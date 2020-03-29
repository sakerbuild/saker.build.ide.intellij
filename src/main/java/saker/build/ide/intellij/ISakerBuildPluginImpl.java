package saker.build.ide.intellij;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import saker.build.ide.intellij.api.ISakerPlugin;

import java.io.IOException;

public interface ISakerBuildPluginImpl extends ISakerPlugin {
    public void closeProject(Project project) throws IOException;

    public ISakerBuildProjectImpl getOrCreateProject(Project project);

    public void displayException(Throwable exc);

    public Configurable createApplicationConfigurable();
}
