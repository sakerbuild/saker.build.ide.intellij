package saker.build.ide.intellij;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.plugins.cl.PluginClassLoader;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.extensions.ExtensionsArea;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.util.messages.MessageBusConnection;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipFile;

public class SakerBuildPlugin {
    public static final String ID = "saker.build.ide.intellij";
    private static final String NATURE_KEY_NAME = "saker.build.project.nature";
    public static final Key<Boolean> SAKER_BUILD_NATURE_KEY = new Key<>(NATURE_KEY_NAME);
    private static IdeaPluginDescriptor implementationPluginDescriptor;

    private static volatile ISakerBuildPluginImpl pluginImpl;

    public static synchronized void setSakerBuildProjectNatureEnabled(Project project, boolean enabled) {
        if (project.isDisposed()) {
            return;
        }
        project.putUserData(SAKER_BUILD_NATURE_KEY, enabled);
        PropertiesComponent propertiescomponent = PropertiesComponent.getInstance(project);
        propertiescomponent.setValue(NATURE_KEY_NAME, Boolean.toString(enabled));

        if (!enabled) {
            ISakerBuildPluginImpl plugin = SakerBuildPlugin.pluginImpl;
            if (plugin != null) {
                try {
                    plugin.closeProject(project);
                } catch (Exception e) {
                    plugin.displayException(e);
                }
            }
        }
    }

    public static boolean isSakerBuildProjectNatureEnabled(Project project) {
        if (project == null || project.isDisposed()) {
            return false;
        }
        if (project.getBasePath() == null) {
            //we require the project to have a base path we can work with
            return false;
        }
        Boolean naturemarker = project.getUserData(SAKER_BUILD_NATURE_KEY);
        if (naturemarker != null) {
            return naturemarker;
        }
        PropertiesComponent propertiescomponent = PropertiesComponent.getInstance(project);
        String propval = propertiescomponent.getValue(NATURE_KEY_NAME);
        if (propval != null) {
            return Boolean.parseBoolean(propval);
        }

        boolean naturepresent;

        String projectpathstr = project.getBasePath();
        if (projectpathstr == null) {
            naturepresent = false;
        } else {
            Path projectpath = Paths.get(projectpathstr);
            Path configfilepath = projectpath.resolve(".saker.build.ide.project.config");
            if (Files.isRegularFile(configfilepath)) {
                naturepresent = true;
            } else {
                naturepresent = false;
            }
            MessageBusConnection connection = project.getMessageBus().connect();

            connection.subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
                @Override
                public void after(@NotNull List<? extends VFileEvent> events) {
                    for (VFileEvent event : events) {
                        if (configfilepath.equals(Paths.get(event.getPath()))) {
                            boolean configfilepresent = Files.isRegularFile(configfilepath);
                            project.putUserData(SAKER_BUILD_NATURE_KEY, configfilepresent);
                            if (configfilepresent) {
                                propertiescomponent.setValue(NATURE_KEY_NAME, "true");
                            }
                        }
                    }
                }
            });
        }

        project.putUserData(SAKER_BUILD_NATURE_KEY, naturepresent);
        if (naturepresent) {
            propertiescomponent.setValue(NATURE_KEY_NAME, "true");
        }

        return naturepresent;
    }

    public static void init() {
        synchronized (SakerBuildPlugin.class) {
            if (pluginImpl != null) {
                return;
            }
            PluginClassLoader plugincl = (PluginClassLoader) SakerBuildPlugin.class.getClassLoader();

            IdeaPluginDescriptor plugindescriptor = PluginManager.getPlugin(plugincl.getPluginId());

            try {
                Path sakerbuildjarpath = exportEmbeddedJar("saker.build.jar");
                JarFile sbjf = createMultiReleaseJarFile(sakerbuildjarpath);
                JarFile sbide = createMultiReleaseJarFile(exportEmbeddedJar("saker.build-ide.jar"));
                ClassLoader jarcl = new ImplementationClassLoader(Arrays.asList(sbjf, sbide));

                implementationPluginDescriptor = new ForwardingImplementationPluginDescriptor(plugindescriptor, jarcl);

                Class<? extends ISakerBuildPluginImpl> pluginimplclass = Class
                        .forName("saker.build.ide.intellij.impl.IntellijSakerIDEPlugin", false, jarcl)
                        .asSubclass(ISakerBuildPluginImpl.class);
                ISakerBuildPluginImpl plugininstance = pluginimplclass.getConstructor().newInstance();

                ImplementationStartArguments initargs = new ImplementationStartArguments(sakerbuildjarpath,
                        Paths.get(PathManager.getHomePath()));
                pluginimplclass.getMethod("initialize", ImplementationStartArguments.class)
                        .invoke(plugininstance, initargs);
                pluginImpl = plugininstance;
            } catch (Exception e) {
                Logger.getInstance(SakerBuildPlugin.class).error("Failed to initialize saker.build plugin", e);
                pluginImpl = new InitFailedPluginImpl();
            }
        }
    }

    public static EditorHighlighter getEditorHighlighter(Project project, VirtualFile file, EditorColorsScheme colors) {
        VirtualFileSystem fs = file.getFileSystem();
        if (!(fs instanceof LocalFileSystem)) {
            return null;
        }
        ISakerBuildProjectImpl sakerproject = getPluginImpl().getOrCreateProject(project);
        if (!sakerproject.isScriptModellingConfigurationAppliesTo(file.getPath())) {
            return null;
        }
        return sakerproject.getEditorHighlighter(file, colors);
    }

    public static void close() {
        synchronized (SakerBuildPlugin.class) {
            try {
                ISakerBuildPluginImpl plugin = SakerBuildPlugin.pluginImpl;
                if (plugin instanceof Closeable) {
                    try {
                        ((Closeable) plugin).close();
                    } catch (Exception e) {
                        Logger.getInstance(SakerBuildPlugin.class).error("Failed to close saker.build plugin", e);
                    }
                }
            } finally {
                SakerBuildPlugin.pluginImpl = new InitFailedPluginImpl();
            }
        }
    }

    public static ISakerBuildPluginImpl getPluginImpl() {
        return SakerBuildPlugin.pluginImpl;
    }

    private static void registerExtension(IdeaPluginDescriptor implplugindesc, ExtensionsArea rootarea, String id,
            String point, String implattrname, String implclass) {
        Element moduletypeextension = new Element("extension");
        if (id != null) {
            moduletypeextension.setAttribute("id", id);
        }
        moduletypeextension.setAttribute("point", point);
        moduletypeextension.setAttribute(implattrname, implclass);
        rootarea.registerExtension(implplugindesc, moduletypeextension, "com.intellij");
    }

    private static Path exportEmbeddedJar(String jarname) throws IOException {
        URL jarentry = SakerBuildPlugin.class.getClassLoader().getResource(jarname);
        if (jarentry == null) {
            throw new FileNotFoundException("Embedded " + jarname + " not found.");
        }
        URLConnection conn = jarentry.openConnection();
        Path result = Paths.get(PathManager.getHomePath()).resolve(jarname);
        Long existinglast = getLastModifiedTimeOrNull(result);
        try (InputStream is = conn.getInputStream()) {
            long lastmodified = conn.getLastModified();
            if (existinglast == null || existinglast.longValue() != lastmodified) {
                Files.copy(is, result, StandardCopyOption.REPLACE_EXISTING);
                Files.setLastModifiedTime(result, FileTime.fromMillis(lastmodified));
            }
        }
        return result;
    }

    private static Long getLastModifiedTimeOrNull(Path result) {
        try {
            return Files.getLastModifiedTime(result).toMillis();
        } catch (IOException ignored) {
            return null;
        }
    }

    private static JarFile createMultiReleaseJarFile(Path jarpath) throws IOException {
        try {
            Class<?> versionclass = Class.forName("java.lang.Runtime$Version", false, null);
            Constructor<JarFile> constructor = JarFile.class
                    .getConstructor(File.class, boolean.class, int.class, versionclass);
            Method versionmethod = Runtime.class.getMethod("version");
            Object runtimeversion = versionmethod.invoke(null);
            return constructor.newInstance(jarpath.toFile(), true, ZipFile.OPEN_READ, runtimeversion);
        } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            return new JarFile(jarpath.toFile());
        }
    }

    public static void displayException(Throwable exc) {
        //TODO some better display
        exc.printStackTrace();
    }

    public static <T> List<ContributedExtensionConfiguration<T>> applyExtensionDisablements(
            List<? extends ContributedExtensionConfiguration<? extends T>> extensions,
            Set<ExtensionDisablement> disablements) {
        ArrayList<ContributedExtensionConfiguration<T>> result = new ArrayList<>();
        for (ContributedExtensionConfiguration<? extends T> ext : extensions) {
            ExtensionDisablement testdisablement = new ExtensionDisablement(ext.getContributedExtension());
            boolean enabled = !disablements.contains(testdisablement);
            if (enabled != ext.isEnabled()) {
                result.add(new ContributedExtensionConfiguration<>(ext.getContributor(), ext.getContributedExtension(),
                        enabled));
            } else {
                result.add(new ContributedExtensionConfiguration<>(ext.getContributor(), ext.getContributedExtension(),
                        ext.isEnabled()));
            }
        }
        return result;
    }

    public static Set<ExtensionDisablement> getExtensionDisablements(
            Collection<? extends ContributedExtensionConfiguration<?>> contributedextensions) {
        if (contributedextensions == null || contributedextensions.isEmpty()) {
            return Collections.emptySet();
        }
        HashSet<ExtensionDisablement> result = new HashSet<>();
        for (ContributedExtensionConfiguration<?> ext : contributedextensions) {
            if (!ext.isEnabled()) {
                result.add(new ExtensionDisablement(ext.getContributedExtension()));
            }
        }
        return result;
    }

    private static class InitFailedPluginImpl implements ISakerBuildPluginImpl {
        @Override
        public void closeProject(Project project) throws IOException {
        }

        @Override
        public ISakerBuildProjectImpl getOrCreateProject(Project project) {
            return null;
        }

        @Override
        public void displayException(Throwable exc) {
            SakerBuildPlugin.displayException(exc);
        }

        @Override
        public Configurable createApplicationConfigurable() {
            return null;
        }
    }
}
