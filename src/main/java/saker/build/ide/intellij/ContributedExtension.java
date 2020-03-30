package saker.build.ide.intellij;

import com.intellij.openapi.extensions.PluginId;

public interface ContributedExtension {
    public String getId();

    public String getDisplayName();

    public PluginId getPluginId();

    public String getImplementationClass();
}
