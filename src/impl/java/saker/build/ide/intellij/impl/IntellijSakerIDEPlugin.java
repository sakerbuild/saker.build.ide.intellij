package saker.build.ide.intellij.impl;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import saker.build.ide.intellij.ContributedExtension;
import saker.build.ide.intellij.ContributedExtensionConfiguration;
import saker.build.ide.intellij.ExtensionDisablement;
import saker.build.ide.intellij.ISakerBuildPluginImpl;
import saker.build.ide.intellij.ImplementationStartArguments;
import saker.build.ide.intellij.SakerBuildPlugin;
import saker.build.ide.intellij.extension.params.EnvironmentUserParameterContributorProviderExtensionPointBean;
import saker.build.ide.intellij.extension.params.IEnvironmentUserParameterContributor;
import saker.build.ide.intellij.extension.params.UserParameterModification;
import saker.build.ide.intellij.impl.properties.SakerBuildApplicationConfigurable;
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
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

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

    public static IntellijSakerIDEPlugin getInstance() {
        return (IntellijSakerIDEPlugin) SakerBuildPlugin.getPluginImpl();
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

        for (EnvironmentUserParameterContributorProviderExtensionPointBean extbean : EnvironmentUserParameterContributorProviderExtensionPointBean.EP_NAME
                .getExtensionList()) {
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
            sakerPlugin.start(sakerPlugin.createDaemonLaunchParameters(sakerPlugin.getIDEPluginProperties()));
        } catch (IOException e) {
            displayException(e);
        }
    }

    public List<ContributedExtensionConfiguration<IEnvironmentUserParameterContributor>> getEnvironmentParameterContributors() {
        return environmentParameterContributors;
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
        this.setIDEPluginProperties(properties, environmentParameterContributors);
    }

    public final void setIDEPluginProperties(IDEPluginProperties properties,
            List<? extends ContributedExtensionConfiguration<IEnvironmentUserParameterContributor>> environmentParameterContributors) {
        synchronized (configurationChangeLock) {
            sakerPlugin.setIDEPluginProperties(properties);
            Set<ExtensionDisablement> prevdisablements = getExtensionDisablements(
                    this.environmentParameterContributors);
            this.environmentParameterContributors = ImmutableUtils.makeImmutableList(environmentParameterContributors);
            Set<ExtensionDisablement> currentdisablements = getExtensionDisablements(
                    this.environmentParameterContributors);
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

    private IDEPluginProperties getIDEPluginPropertiesWithEnvironmentParameterContributions(
            IDEPluginProperties properties, ProgressIndicator monitor) {
        if (environmentParameterContributors.isEmpty()) {
            return properties;
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

    public NavigableMap<String, String> getUserParametersWithContributors(Map<String, String> userparameters,
            List<? extends ContributedExtensionConfiguration<? extends IEnvironmentUserParameterContributor>> contributors,
            ProgressIndicator monitor) {
        NavigableMap<String, String> userparamworkmap = ObjectUtils.newTreeMap(userparameters);
        NavigableMap<String, String> unmodifiableuserparammap = ImmutableUtils
                .unmodifiableNavigableMap(userparamworkmap);
        contributor_loop:
        for (ContributedExtensionConfiguration<? extends IEnvironmentUserParameterContributor> contributor : contributors) {
            if (!contributor.isEnabled()) {
                continue;
            }
            if (monitor != null && monitor.isCanceled()) {
                throw new ProcessCanceledException();
            }
            try {
                Set<UserParameterModification> modifications = contributor.getContributor()
                        .contribute(this, unmodifiableuserparammap, monitor);
                if (ObjectUtils.isNullOrEmpty(modifications)) {
                    continue;
                }
                Set<String> keys = new TreeSet<>();
                for (UserParameterModification mod : modifications) {
                    if (!keys.add(mod.getKey())) {
                        displayException(new IllegalArgumentException(
                                "Multiple environment user parameter modification for key: " + mod.getKey()));
                        continue contributor_loop;
                    }
                }
                for (UserParameterModification mod : modifications) {
                    mod.apply(userparamworkmap);
                }
            } catch (Exception e) {
                if (monitor != null && monitor.isCanceled()) {
                    throw new ProcessCanceledException();
                }
                displayException(e);
            }
        }
        return userparamworkmap;
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
        return new SakerBuildApplicationConfigurable();
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

    public static Set<ExtensionDisablement> getExtensionDisablements(
            Iterable<? extends ContributedExtensionConfiguration<?>> contributedextensions) {
        if (ObjectUtils.isNullOrEmpty(contributedextensions)) {
            return Collections.emptySet();
        }
        HashSet<ExtensionDisablement> result = new HashSet<>();
        for (ContributedExtensionConfiguration<?> ext : contributedextensions) {
            if (!ext.isEnabled()) {
                ContributedExtension contributedextension = ext.getContributedExtension();
                result.add(new ExtensionDisablement(contributedextension.getPluginDescriptor().getPluginId(),
                        contributedextension.getId()));
            }
        }
        return result;
    }

}
