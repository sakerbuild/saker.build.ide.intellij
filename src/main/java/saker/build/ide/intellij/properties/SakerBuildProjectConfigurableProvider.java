package saker.build.ide.intellij.properties;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurableProvider;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;
import saker.build.ide.intellij.ISakerBuildPluginImpl;
import saker.build.ide.intellij.ISakerBuildProjectImpl;
import saker.build.ide.intellij.SakerBuildPlugin;

public class SakerBuildProjectConfigurableProvider extends ConfigurableProvider {
    private Project project;

    public SakerBuildProjectConfigurableProvider(Project project) {
        this.project = project;
    }

    @Nullable
    @Override
    public Configurable createConfigurable() {
        ISakerBuildPluginImpl pluginimpl = SakerBuildPlugin.getPluginImpl();
        ISakerBuildProjectImpl ideproject = pluginimpl.getOrCreateProject(project);
        if (ideproject == null) {
            //TODO return an error configurable
            return null;
        }
        return ideproject.getProjectPropertiesConfigurable();
    }

    @Override
    public boolean canCreateConfigurable() {
        return SakerBuildPlugin.isSakerBuildProjectNatureEnabled(project);
    }
}
