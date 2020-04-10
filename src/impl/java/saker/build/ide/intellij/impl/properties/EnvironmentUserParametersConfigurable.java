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
import saker.build.ide.support.properties.IDEProjectProperties;

import javax.swing.JComponent;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class EnvironmentUserParametersConfigurable implements Configurable, Configurable.NoScroll {

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
        form.setUserParameters(parent.getProperties().getUserParameters(), parent.getExtensionDisablements());
    }

    @Override
    public boolean isModified() {
        IDEPluginProperties currentprops = parent.getCurrentPluginProperties();
        IDEPluginProperties properties = parent.getProperties();
        if (!Objects.equals(currentprops.getUserParameters(), properties.getUserParameters())) {
            return true;
        }
        if (!Objects.equals(parent.getCurrentExtensionDisablements(), parent.getExtensionDisablements())) {
            return true;
        }
        return false;
    }

    @Override
    public void apply() throws ConfigurationException {
        parent.apply();
    }

}
