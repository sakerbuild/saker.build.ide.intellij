package saker.build.ide.intellij;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;

import java.io.IOException;

public interface ISakerBuildPluginImpl {
    public void closeProject(Project project) throws IOException;

    public ISakerBuildProjectImpl getOrCreateProject(Project project);

    public void displayException(Throwable exc);

    public Configurable createApplicationConfigurable();
}
