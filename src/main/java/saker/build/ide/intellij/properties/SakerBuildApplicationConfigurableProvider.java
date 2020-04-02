package saker.build.ide.intellij.properties;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurableProvider;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.Nullable;
import saker.build.ide.intellij.ISakerBuildPluginImpl;
import saker.build.ide.intellij.SakerBuildPlugin;

public class SakerBuildApplicationConfigurableProvider extends ConfigurableProvider implements DumbAware {
    @Nullable
    @Override
    public Configurable createConfigurable() {
        ISakerBuildPluginImpl pluginimpl = SakerBuildPlugin.getPluginImpl();
        return pluginimpl.createApplicationConfigurable();
    }

    @Override
    public boolean canCreateConfigurable() {
        return SakerBuildPlugin.getPluginImpl() != null;
    }
}
