package saker.build.ide.intellij.impl.properties;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;
import saker.build.ide.intellij.impl.IntellijSakerIDEPlugin;
import saker.build.ide.support.SimpleIDEPluginProperties;
import saker.build.ide.support.properties.IDEPluginProperties;
import saker.build.thirdparty.saker.util.ImmutableUtils;

import javax.swing.*;
import java.util.*;

public class EnvironmentUserParametersConfigurable implements Configurable {
    private final IntellijSakerIDEPlugin plugin;
    private Set<? extends Map.Entry<String, String>> userParameters = null;

    private final UserParametersForm form;

    public EnvironmentUserParametersConfigurable() {
        plugin = IntellijSakerIDEPlugin.getInstance();
        form = new UserParametersForm(ApplicationManager.getApplication());
        form.setUserParameterKind("environment");
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
        IDEPluginProperties props = plugin.getIDEPluginProperties();
        if (props != null) {
            this.userParameters = props.getUserParameters();
        }
        if (this.userParameters == null) {
            this.userParameters = Collections.emptySet();
        }
        form.setUserParameters(this.userParameters);
    }

    private Set<Map.Entry<String, String>> getCurrentValues() {
        return new LinkedHashSet<>(form.getParametersEditPanel().getData());
    }

    @Override
    public boolean isModified() {
        return !Objects.equals(this.userParameters, getCurrentValues());
    }

    @Override
    public void apply() throws ConfigurationException {
        Set<Map.Entry<String, String>> vals = getCurrentValues();
        plugin.setIDEPluginProperties(
                SimpleIDEPluginProperties.builder(plugin.getIDEPluginProperties()).setUserParameters(vals).build());
        this.userParameters = vals;
    }

}
