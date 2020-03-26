package saker.build.ide.intellij.impl;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import saker.build.ide.intellij.ISakerBuildPluginImpl;
import saker.build.ide.intellij.ImplementationStartArguments;
import saker.build.ide.intellij.SakerBuildPlugin;
import saker.build.ide.intellij.impl.properties.SakerBuildApplicationConfigurable;
import saker.build.ide.support.ExceptionDisplayer;
import saker.build.ide.support.SakerIDEPlugin;
import saker.build.ide.support.SakerIDEProject;
import saker.build.ide.support.properties.IDEPluginProperties;
import saker.build.thirdparty.saker.util.io.IOUtils;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class IntellijSakerIDEPlugin implements Closeable, ExceptionDisplayer, ISakerBuildPluginImpl {
    private final SakerIDEPlugin sakerPlugin;
    private volatile boolean closed = false;

    private final Object projectsLock = new Object();
    private final Map<Project, IntellijSakerIDEProject> projects = new ConcurrentHashMap<>();
    private final Object configurationChangeLock = new Object();

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

        try {
            sakerPlugin.initialize(sakerJarPath, plugindirectory);
            sakerPlugin.start(sakerPlugin.createDaemonLaunchParameters(sakerPlugin.getIDEPluginProperties()));
        } catch (IOException e) {
            displayException(e);
        }
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

    public final void setIDEPluginProperties(IDEPluginProperties properties) {
        synchronized (configurationChangeLock) {
            sakerPlugin.setIDEPluginProperties(properties);
        }

        ProgressManager progmanager = ProgressManager.getInstance();
        progmanager.run(new Task.Backgroundable(null, "Updating saker.build plugin properties", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                sakerPlugin.updateForPluginProperties(properties);
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

}
