package saker.build.ide.intellij.properties;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurableProvider;
import org.jetbrains.annotations.Nullable;
import saker.build.ide.intellij.SakerBuildPlugin;

public class SakerBuildApplicationConfigurableProvider extends ConfigurableProvider {
    @Nullable
    @Override
    public Configurable createConfigurable() {
        return SakerBuildPlugin.getPluginImpl().createApplicationConfigurable();
    }
}
