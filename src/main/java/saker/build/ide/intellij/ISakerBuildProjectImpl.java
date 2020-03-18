package saker.build.ide.intellij;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;

public interface ISakerBuildProjectImpl {
    public Project getProject();

    public void buildAsync();

    public Configurable getProjectPropertiesConfigurable();
}
