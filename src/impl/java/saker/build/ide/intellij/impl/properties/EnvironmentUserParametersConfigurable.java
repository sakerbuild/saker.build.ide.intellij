package saker.build.ide.intellij.impl.properties;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import saker.build.ide.intellij.ExtensionDisablement;
import saker.build.ide.intellij.impl.IntellijSakerIDEPlugin;
import saker.build.ide.support.SimpleIDEPluginProperties;
import saker.build.ide.support.properties.IDEPluginProperties;

import javax.swing.JComponent;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class EnvironmentUserParametersConfigurable implements Configurable, Configurable.NoScroll {

    private Set<? extends Map.Entry<String, String>> userParameters = null;
    private Set<ExtensionDisablement> extensionDisablements = null;

    private final UserParametersForm form;
    private SakerBuildApplicationConfigurable parent;

    public EnvironmentUserParametersConfigurable(SakerBuildApplicationConfigurable parent) {
        this.parent = parent;
        form = new UserParametersForm(this);
        form.setUserParameterKind("environment");
        form.getParametersInfoLabel().setText("The following user parameters are defined for the build environment:");
    }

    public SakerBuildApplicationConfigurable getParent() {
        return parent;
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Environment User Parameters";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        return form.getRootPanel();
    }

    @Override
    public void reset() {
        this.userParameters = null;

        IDEPluginProperties props = parent.getPlugin().getIDEPluginProperties();
        if (props != null) {
            this.userParameters = props.getUserParameters();
        }
        if (this.userParameters == null) {
            this.userParameters = Collections.emptySet();
        }
        this.extensionDisablements = parent.getPlugin().getExtensionDisablements();
        form.setUserParameters(this.userParameters, this.extensionDisablements);
    }

    @NotNull
    private Set<Map.Entry<String, String>> getCurrentValues() {
        return form.getCurrentValues();
    }

    @NotNull
    private Set<ExtensionDisablement> getCurrentExtensionDisablements() {
        return form.getCurrentExtensionDisablements();
    }

    @Override
    public boolean isModified() {
        if (!Objects.equals(this.userParameters, getCurrentValues())) {
            return true;
        }
        if (!Objects.equals(this.extensionDisablements, getCurrentExtensionDisablements())) {
            return true;
        }
        return false;
    }

    @Override
    public void apply() throws ConfigurationException {
        IntellijSakerIDEPlugin plugin = parent.getPlugin();

        Set<Map.Entry<String, String>> vals = getCurrentValues();
        Set<ExtensionDisablement> disablements = getCurrentExtensionDisablements();

        plugin.setIDEPluginProperties(
                SimpleIDEPluginProperties.builder(plugin.getIDEPluginProperties()).setUserParameters(vals).build(),
                disablements);
        this.userParameters = vals;
    }

}
