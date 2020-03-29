package saker.build.ide.intellij;

import com.intellij.openapi.extensions.PluginDescriptor;

public interface ContributedExtension {
    public String getId();

    public String getDisplayName();

    public PluginDescriptor getPluginDescriptor();

}
