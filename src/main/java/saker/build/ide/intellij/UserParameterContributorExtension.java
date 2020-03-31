package saker.build.ide.intellij;

import com.intellij.openapi.extensions.PluginId;

public interface UserParameterContributorExtension {
    public String getId();

    public String getDisplayName();

    public PluginId getPluginId();

    public String getImplementationClass();
}
