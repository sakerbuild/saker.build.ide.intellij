package saker.build.ide.intellij.impl;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import saker.build.daemon.DaemonLaunchParameters;
import saker.build.ide.intellij.ContributedExtensionConfiguration;
import saker.build.ide.intellij.ExtensionDisablement;
import saker.build.ide.intellij.ISakerBuildPluginImpl;
import saker.build.ide.intellij.ImplementationStartArguments;
import saker.build.ide.intellij.SakerBuildPlugin;
import saker.build.ide.intellij.extension.params.EnvironmentUserParameterContributorProviderExtensionPointBean;
import saker.build.ide.intellij.extension.params.IEnvironmentUserParameterContributor;
import saker.build.ide.intellij.extension.params.UserParameterModification;
import saker.build.ide.intellij.extension.script.information.IScriptInformationDesigner;
import saker.build.ide.intellij.extension.script.information.ScriptInformationDesignerExtensionPointBean;
import saker.build.ide.intellij.extension.script.outline.IScriptOutlineDesigner;
import saker.build.ide.intellij.extension.script.outline.ScriptOutlineDesignerExtensionPointBean;
import saker.build.ide.intellij.extension.script.proposal.IScriptProposalDesigner;
import saker.build.ide.intellij.extension.script.proposal.ScriptProposalDesignerExtensionPointBean;
import saker.build.ide.intellij.impl.editor.MultiScriptInformationDesigner;
import saker.build.ide.intellij.impl.editor.MultiScriptOutlineDesigner;
import saker.build.ide.intellij.impl.editor.MultiScriptProposalDesigner;
import saker.build.ide.intellij.impl.properties.SakerBuildApplicationConfigurable;
import saker.build.ide.intellij.util.PluginCompatUtil;
import saker.build.ide.support.ExceptionDisplayer;
import saker.build.ide.support.SakerIDEPlugin;
import saker.build.ide.support.SakerIDEProject;
import saker.build.ide.support.SimpleIDEPluginProperties;
import saker.build.ide.support.persist.StructuredArrayObjectInput;
import saker.build.ide.support.persist.StructuredArrayObjectOutput;
import saker.build.ide.support.persist.StructuredObjectInput;
import saker.build.ide.support.persist.StructuredObjectOutput;
import saker.build.ide.support.persist.XMLStructuredReader;
import saker.build.ide.support.persist.XMLStructuredWriter;
import saker.build.ide.support.properties.IDEPluginProperties;
import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.build.thirdparty.saker.util.io.IOUtils;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

public class IntellijSakerIDEPlugin implements Closeable, ExceptionDisplayer, ISakerBuildPluginImpl {
    private static final String CONFIG_FILE_ROOT_OBJECT_NAME = "saker.build.ide.intellij.plugin.config";
    private static final String IDE_PLUGIN_PROPERTIES_FILE_NAME = "." + CONFIG_FILE_ROOT_OBJECT_NAME;

    private final SakerIDEPlugin sakerPlugin;
    private volatile boolean closed = false;

    private final Object projectsLock = new Object();
    private final Map<Project, IntellijSakerIDEProject> projects = new ConcurrentHashMap<>();
    private final Object configurationChangeLock = new Object();

    private Path pluginConfigurationFilePath;

    private List<ContributedExtensionConfiguration<IEnvironmentUserParameterContributor>> environmentParameterContributors = Collections
            .emptyList();

    public IntellijSakerIDEPlugin() {
        sakerPlugin = new SakerIDEPlugin();
    }

    public void initialize(ImplementationStartArguments args) {
        Path sakerJarPath = args.sakerJarPath;
        Path plugindirectory = args.pluginDirectory;

        sakerPlugin.addExceptionDisplayer(this);

        this.pluginConfigurationFilePath = plugindirectory.resolve(IDE_PLUGIN_PROPERTIES_FILE_NAME);

        Set<ExtensionDisablement> extensiondisablements = new HashSet<>();

        try (InputStream in = Files.newInputStream(pluginConfigurationFilePath)) {
            XMLStructuredReader reader = new XMLStructuredReader(in);
            try (StructuredObjectInput configurationobj = reader.readObject(CONFIG_FILE_ROOT_OBJECT_NAME)) {
                readExtensionDisablements(configurationobj, extensiondisablements);
            }
        } catch (NoSuchFileException e) {
        } catch (IOException e) {
            displayException(e);
        }

        environmentParameterContributors = new ArrayList<>();

        for (EnvironmentUserParameterContributorProviderExtensionPointBean extbean : PluginCompatUtil
                .getExtensionList(EnvironmentUserParameterContributorProviderExtensionPointBean.EP_NAME)) {
            if (extbean.getId() == null) {
                Logger.getInstance(IntellijSakerIDEPlugin.class)
                        .warn("No id attribute specified for extension: " + extbean);
                continue;
            }
            boolean enabled = !ExtensionDisablement
                    .isDisabled(extensiondisablements, extbean.getPluginId(), extbean.getId());

            try {
                IEnvironmentUserParameterContributor contributor = extbean.createContributor();
                environmentParameterContributors
                        .add(new ContributedExtensionConfiguration<>(contributor, extbean, enabled));
            } catch (Exception e) {
                Logger.getInstance(IntellijSakerIDEPlugin.class)
                        .error("Failed to instantiate extension: " + extbean, e);
            }
        }
        environmentParameterContributors = ImmutableUtils.unmodifiableList(environmentParameterContributors);

        try {
            sakerPlugin.initialize(sakerJarPath, plugindirectory);

            //non cancellable
            ProgressManager.getInstance().run(new Task.Backgroundable(null, "Initlaizing saker.build plugin", false) {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    //this section should be cancellable as it is part of the initialization process of the plugin
                    IDEPluginProperties pluginprops = getIDEPluginPropertiesWithEnvironmentParameterContributions(
                            sakerPlugin.getIDEPluginProperties(), indicator);
                    DaemonLaunchParameters daemonLaunchParameters = sakerPlugin
                            .createDaemonLaunchParameters(pluginprops);
                    try {
                        sakerPlugin.start(daemonLaunchParameters);
                    } catch (IOException e) {
                        displayException(e);
                    }
                }
            });
        } catch (IOException e) {
            displayException(e);
        }
    }

    public List<ContributedExtensionConfiguration<IEnvironmentUserParameterContributor>> getEnvironmentParameterContributors() {
        return environmentParameterContributors;
    }

    public IScriptInformationDesigner getScriptInformationDesignerForSchemaIdentifier(String schemaid,
            @Nullable Project project) {
        List<IScriptInformationDesigner> designers = new ArrayList<>();
        for (ScriptInformationDesignerExtensionPointBean ext : PluginCompatUtil
                .getExtensionList(ScriptInformationDesignerExtensionPointBean.EP_NAME)) {
            String configschemaid = ext.getSchemaId();
            if (configschemaid != null && !configschemaid.equals(schemaid)) {
                continue;
            }
            try {
                IScriptInformationDesigner designer = ext.createContributor(project);
                designers.add(designer);
            } catch (Exception e) {
                displayException(e);
                continue;
            }
        }
        if (designers.isEmpty()) {
            return null;
        }
        if (designers.size() == 1) {
            return designers.get(0);
        }
        return new MultiScriptInformationDesigner(designers);
    }

    public IScriptOutlineDesigner getScriptOutlineDesignerForSchemaIdentifier(String schemaid,
            @Nullable Project project) {
        List<IScriptOutlineDesigner> designers = new ArrayList<>();
        for (ScriptOutlineDesignerExtensionPointBean ext : PluginCompatUtil
                .getExtensionList(ScriptOutlineDesignerExtensionPointBean.EP_NAME)) {
            String configschemaid = ext.getSchemaId();
            if (configschemaid != null && !configschemaid.equals(schemaid)) {
                //can't use
                continue;
            }
            try {
                IScriptOutlineDesigner designer = ext.createContributor(project);
                designers.add(designer);
            } catch (Exception e) {
                displayException(e);
                continue;
            }
        }

        if (designers.isEmpty()) {
            return null;
        }
        if (designers.size() == 1) {
            return designers.get(0);
        }
        return new MultiScriptOutlineDesigner(designers);
    }

    public IScriptProposalDesigner getScriptProposalDesignerForSchemaIdentifiers(Set<String> schemaidentifiers,
            @Nullable Project project) {
        Objects.requireNonNull(schemaidentifiers, "schema identifiers");
        List<IScriptProposalDesigner> designers = new ArrayList<>();
        for (ScriptProposalDesignerExtensionPointBean ext : PluginCompatUtil
                .getExtensionList(ScriptProposalDesignerExtensionPointBean.EP_NAME)) {
            String configschemaid = ext.getSchemaId();
            if (configschemaid != null && !schemaidentifiers.contains(configschemaid)) {
                continue;
            }
            try {
                IScriptProposalDesigner designer = ext.createContributor(project);
                designers.add(designer);
            } catch (Exception e) {
                displayException(e);
                continue;
            }
        }

        if (designers.isEmpty()) {
            return null;
        }
        if (designers.size() == 1) {
            return designers.get(0);
        }
        return new MultiScriptProposalDesigner(designers);
    }

    @Override
    public void closeProject(Project project) throws IOException {
        synchronized (projectsLock) {
            IntellijSakerIDEProject intellijproject = projects.remove(project);
            try {
                if (intellijproject != null) {
                    intellijproject.close();
                }
            } finally {
                sakerPlugin.closeProject(project);
            }
        }
    }

    @Override
    public IntellijSakerIDEProject getOrCreateProject(Project project) {
        if (project == null) {
            return null;
        }
        synchronized (projectsLock) {
            if (closed) {
                throw new IllegalStateException("closed");
            }
            IntellijSakerIDEProject intellijproject = projects.get(project);
            if (intellijproject != null) {
                return intellijproject;
            }
            if (!SakerBuildPlugin.isSakerBuildProjectNatureEnabled(project)) {
                return null;
            }
            SakerIDEProject sakerproject = sakerPlugin.getOrCreateProject(project);
            intellijproject = new IntellijSakerIDEProject(this, sakerproject, project);
            projects.put(project, intellijproject);
            intellijproject.initialize();
            return intellijproject;
        }
    }

    public final IDEPluginProperties getIDEPluginProperties() {
        return sakerPlugin.getIDEPluginProperties();
    }

    @Deprecated
    public final void setIDEPluginProperties(IDEPluginProperties properties) {
        this.setIDEPluginProperties(properties, getExtensionDisablements());
    }

    public final void setIDEPluginProperties(IDEPluginProperties properties,
            Set<ExtensionDisablement> extensionDisablements) {
        synchronized (configurationChangeLock) {
            sakerPlugin.setIDEPluginProperties(properties);
            Set<ExtensionDisablement> prevdisablements = getExtensionDisablements();

            this.environmentParameterContributors = SakerBuildPlugin
                    .applyExtensionDisablements(this.environmentParameterContributors, extensionDisablements);
            if (!prevdisablements.equals(extensionDisablements)) {
                try {
                    writePluginConfigurationFile(extensionDisablements);
                } catch (IOException e) {
                    displayException(e);
                }
            }
        }
        ProgressManager progmanager = ProgressManager.getInstance();
        progmanager.run(new Task.Backgroundable(null, "Updating saker.build plugin properties", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                sakerPlugin.updateForPluginProperties(getIDEPluginPropertiesWithEnvironmentParameterContributions(
                        sakerPlugin.getIDEPluginProperties(), indicator));
            }
        });
    }

    @Deprecated
    public final void setIDEPluginProperties(IDEPluginProperties properties,
            List<? extends ContributedExtensionConfiguration<IEnvironmentUserParameterContributor>> environmentParameterContributors) {
        synchronized (configurationChangeLock) {
            sakerPlugin.setIDEPluginProperties(properties);
            Set<ExtensionDisablement> prevdisablements = getExtensionDisablements();

            this.environmentParameterContributors = ImmutableUtils.makeImmutableList(environmentParameterContributors);
            Set<ExtensionDisablement> currentdisablements = getExtensionDisablements();
            if (!prevdisablements.equals(currentdisablements)) {
                try {
                    writePluginConfigurationFile(currentdisablements);
                } catch (IOException e) {
                    displayException(e);
                }
            }
        }
        ProgressManager progmanager = ProgressManager.getInstance();
        progmanager.run(new Task.Backgroundable(null, "Updating saker.build plugin properties", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                sakerPlugin.updateForPluginProperties(
                        getIDEPluginPropertiesWithEnvironmentParameterContributions(properties, indicator));
            }
        });
    }

    @NotNull
    public Set<ExtensionDisablement> getExtensionDisablements() {
        return SakerBuildPlugin.getExtensionDisablements(this.environmentParameterContributors);
    }

    private IDEPluginProperties getIDEPluginPropertiesWithEnvironmentParameterContributions(
            IDEPluginProperties properties, ProgressIndicator monitor) {
        if (environmentParameterContributors.isEmpty()) {
            return properties;
        }
        if (monitor == null) {
            monitor = new EmptyProgressIndicator();
        }
        SimpleIDEPluginProperties.Builder builder = SimpleIDEPluginProperties.builder(properties);
        Map<String, String> propertiesuserparams = SakerIDEPlugin.entrySetToMap(properties.getUserParameters());
        List<ContributedExtensionConfiguration<IEnvironmentUserParameterContributor>> contributors = environmentParameterContributors;

        NavigableMap<String, String> userparammap = getUserParametersWithContributors(propertiesuserparams,
                contributors, monitor);
        builder.setUserParameters(userparammap.entrySet());
        return builder.build();
    }

    private void writePluginConfigurationFile(Iterable<? extends ExtensionDisablement> disablements) throws
            IOException {
        try (OutputStream os = Files.newOutputStream(pluginConfigurationFilePath)) {
            try (XMLStructuredWriter writer = new XMLStructuredWriter(os)) {
                try (StructuredObjectOutput configurationobj = writer.writeObject(CONFIG_FILE_ROOT_OBJECT_NAME)) {
                    writeExtensionDisablements(configurationobj, disablements);
                }
            }
        }
    }

    public static <E> NavigableMap<String, String> getUserParametersWithContributors(Map<String, String> userparameters,
            List<? extends ContributedExtensionConfiguration<? extends E>> contributors,
            ExceptionDisplayer excdisplayer, ProgressIndicator monitor,
            BiFunction<? super E, ? super Map<String, String>, ? extends Set<UserParameterModification>> applier) {
        NavigableMap<String, String> userparamworkmap = ObjectUtils.newTreeMap(userparameters);
        NavigableMap<String, String> unmodifiableuserparammap = ImmutableUtils
                .unmodifiableNavigableMap(userparamworkmap);
        contributor_loop:
        for (ContributedExtensionConfiguration<? extends E> contributor : contributors) {
            if (!contributor.isEnabled()) {
                continue;
            }
            if (monitor != null && monitor.isCanceled()) {
                throw new ProcessCanceledException();
            }
            try {
                Set<UserParameterModification> modifications = applier
                        .apply(contributor.getContributor(), unmodifiableuserparammap);
                if (ObjectUtils.isNullOrEmpty(modifications)) {
                    continue;
                }
                Set<String> keys = new TreeSet<>();
                for (UserParameterModification mod : modifications) {
                    if (!keys.add(mod.getKey())) {
                        excdisplayer.displayException(new IllegalArgumentException(
                                "Multiple user parameter modification for key: " + mod.getKey() + " by " + contributor
                                        .getContributedExtension()));
                        continue contributor_loop;
                    }
                }
                for (UserParameterModification mod : modifications) {
                    mod.apply(userparamworkmap);
                }
            } catch (ProcessCanceledException e) {
                throw e;
            } catch (Exception e) {
                if (monitor != null && monitor.isCanceled()) {
                    throw new ProcessCanceledException();
                }
                excdisplayer.displayException(e);
            }
        }
        return userparamworkmap;
    }

    public NavigableMap<String, String> getUserParametersWithContributors(Map<String, String> userparameters,
            List<? extends ContributedExtensionConfiguration<? extends IEnvironmentUserParameterContributor>> contributors,
            ProgressIndicator monitor) {
        return getUserParametersWithContributors(userparameters, contributors, this, monitor, (ext, userparams) -> {
            return ext.contribute(this, userparams, monitor);
        });
    }

    public void reloadPluginEnvironment() {
        ProgressManager.getInstance().run(new Task.Backgroundable(null, "Reloading plugin environment", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    synchronized (configurationChangeLock) {
                        DaemonLaunchParameters launchparams = sakerPlugin.createDaemonLaunchParameters(
                                getIDEPluginPropertiesWithEnvironmentParameterContributions(
                                        sakerPlugin.getIDEPluginProperties(), indicator));
                        if (indicator.isCanceled()) {
                            return;
                        }
                        sakerPlugin.forceReloadPluginDaemon(launchparams);
                    }
                } catch (Exception e) {
                    displayException(e);
                }
            }
        });
    }

    public final void addPluginResourceListener(SakerIDEPlugin.PluginResourceListener listener) {
        sakerPlugin.addPluginResourceListener(listener);
    }

    public final void removePluginResourceListener(SakerIDEPlugin.PluginResourceListener listener) {
        sakerPlugin.removePluginResourceListener(listener);
    }

    @Override
    public void close() throws IOException {
        closed = true;
        IOException exc = null;
        List<ContributedExtensionConfiguration<IEnvironmentUserParameterContributor>> envparamcontributors = environmentParameterContributors;
        if (!ObjectUtils.isNullOrEmpty(envparamcontributors)) {
            this.environmentParameterContributors = Collections.emptyList();
            for (ContributedExtensionConfiguration<IEnvironmentUserParameterContributor> contributor : envparamcontributors) {
                try {
                    IEnvironmentUserParameterContributor paramcontributor = contributor.getContributor();
                    if (paramcontributor != null) {
                        paramcontributor.dispose();
                    }
                } catch (Exception e) {
                    //catch just in case
                    exc = IOUtils.addExc(exc, e);
                }
            }
        }
        try {
            closeProjects();
        } catch (IOException e) {
            exc = IOUtils.addExc(exc, e);
        }
        exc = IOUtils.closeExc(exc, sakerPlugin);
        IOUtils.throwExc(exc);
    }

    @Override
    public void displayException(Throwable exc) {
        SakerBuildPlugin.displayException(exc);
    }

    @Override
    public Configurable createApplicationConfigurable() {
        return new SakerBuildApplicationConfigurable(this);
    }

    private void closeProjects() throws IOException {
        List<IntellijSakerIDEProject> copiedprojects;
        synchronized (projectsLock) {
            copiedprojects = new ArrayList<>(projects.values());
            projects.clear();
        }
        IOException exc = null;
        for (IntellijSakerIDEProject p : copiedprojects) {
            try {
                p.close();
            } catch (IOException e) {
                exc = IOUtils.addExc(exc, e);
            }
        }
        IOUtils.throwExc(exc);
    }

    public static void readExtensionDisablements(StructuredObjectInput configurationobj,
            Set<ExtensionDisablement> extensiondisablements) throws IOException {
        try (StructuredArrayObjectInput disablementsarray = configurationobj.readArray("extension_disablements")) {
            if (disablementsarray != null) {
                int len = disablementsarray.length();
                for (int i = 0; i < len; i++) {
                    StructuredObjectInput obj = disablementsarray.readObject();
                    String pluginid = obj.readString("plugin_id");

                    if (ObjectUtils.isNullOrEmpty(pluginid)) {
                        continue;
                    }
                    String extensionid = obj.readString("extension_id");
                    if (ObjectUtils.isNullOrEmpty(extensionid)) {
                        continue;
                    }
                    extensiondisablements.add(new ExtensionDisablement(PluginId.getId(pluginid), extensionid));
                }
            }
        }
    }

    public static void writeExtensionDisablements(StructuredObjectOutput configurationobjout,
            Iterable<? extends ExtensionDisablement> disablements) throws IOException {
        if (ObjectUtils.isNullOrEmpty(disablements)) {
            return;
        }
        try (StructuredArrayObjectOutput out = configurationobjout.writeArray("extension_disablements")) {
            for (ExtensionDisablement disablement : disablements) {
                try (StructuredObjectOutput obj = out.writeObject()) {
                    obj.writeField("plugin_id", disablement.getPluginId().getIdString());
                    obj.writeField("extension_id", disablement.getExtensionId());
                }
            }
        }
    }

}
