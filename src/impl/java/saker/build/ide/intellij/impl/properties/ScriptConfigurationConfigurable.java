package saker.build.ide.intellij.impl.properties;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;
import saker.build.ide.intellij.impl.IntellijSakerIDEProject;
import saker.build.ide.support.properties.IDEProjectProperties;
import saker.build.ide.support.properties.ScriptConfigurationIDEProperty;
import saker.build.ide.support.properties.SimpleIDEProjectProperties;

import javax.swing.JComponent;
import java.util.Objects;
import java.util.Set;

public class ScriptConfigurationConfigurable implements Configurable, Configurable.NoScroll {
    private final SakerBuildProjectConfigurable parent;
    private final ScriptConfigurationForm form;

    public ScriptConfigurationConfigurable(SakerBuildProjectConfigurable parent) {
        this.parent = parent;
        this.form = new ScriptConfigurationForm(this);
    }

    public SakerBuildProjectConfigurable getParent() {
        return parent;
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Script Configuration";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        return form.getRootPanel();
    }

    @Override
    public void reset() {
        form.reset();
    }

    @Override
    public boolean isModified() {
        IDEProjectProperties currentprops = parent.getCurrentProjectProperties();
        IDEProjectProperties properties = parent.getProperties();

        if (!Objects.equals(currentprops.getScriptModellingExclusions(), properties.getScriptModellingExclusions())) {
            return true;
        }
        if (!Objects.equals(currentprops.getScriptConfigurations(), properties.getScriptConfigurations())) {
            return true;
        }
        return false;
    }

    @Override
    public void apply() throws ConfigurationException {
    }
}
