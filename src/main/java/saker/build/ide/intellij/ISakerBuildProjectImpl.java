package saker.build.ide.intellij;

import com.intellij.openapi.options.Configurable;

public interface ISakerBuildProjectImpl {
    public void buildAsync();

    public Configurable getProjectPropertiesConfigurable();
}
