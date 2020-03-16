package saker.build.ide.intellij.impl;

import com.intellij.openapi.project.Project;
import saker.build.file.path.SakerPath;
import saker.build.ide.intellij.ISakerBuildPluginImpl;
import saker.build.ide.intellij.ImplementationStartArguments;
import saker.build.ide.intellij.SakerBuildPlugin;
import saker.build.ide.support.ExceptionDisplayer;
import saker.build.ide.support.SakerIDEPlugin;
import saker.build.ide.support.SakerIDEProject;
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
        //TODO display exception
        exc.printStackTrace();
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

    public static SakerPath tryParsePath(String path) {
        if (path == null) {
            return null;
        }
        try {
            return SakerPath.valueOf(path);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
