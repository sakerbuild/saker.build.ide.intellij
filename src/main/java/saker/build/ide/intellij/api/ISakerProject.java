package saker.build.ide.intellij.api;

import com.intellij.openapi.project.Project;

public interface ISakerProject {
    public Project getProject();

    public ISakerPlugin getPlugin();

    public String executionPathToProjectRelativePath(String executionpath);

    //argument may be absolute or relative too
    public String projectPathToExecutionPath(String path);
}
