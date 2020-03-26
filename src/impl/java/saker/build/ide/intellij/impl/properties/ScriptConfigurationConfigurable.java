package saker.build.ide.intellij.impl.properties;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;
import saker.build.ide.intellij.impl.IntellijSakerIDEProject;
import saker.build.ide.support.properties.ScriptConfigurationIDEProperty;
import saker.build.ide.support.properties.SimpleIDEProjectProperties;

import javax.swing.JComponent;
import java.util.Objects;
import java.util.Set;

public class ScriptConfigurationConfigurable implements Configurable, Configurable.NoScroll {
    private final SakerBuildProjectConfigurable parent;
    private final ScriptConfigurationForm form;

    private Set<String> exclusionWildcards;
    private Set<ScriptConfigurationIDEProperty> scriptConfigurations;

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
        form.reset(parent.getProject().getIDEProjectProperties());

        exclusionWildcards = form.getExclusionWildcards();
        scriptConfigurations = form.getScriptConfiguration();
    }

    @Override
    public boolean isModified() {
        if (!Objects.equals(exclusionWildcards, form.getExclusionWildcards())) {
            return true;
        }
        if (!Objects.equals(scriptConfigurations, form.getScriptConfiguration())) {
            return true;
        }
        return false;
    }

    @Override
    public void apply() throws ConfigurationException {
        IntellijSakerIDEProject project = parent.getProject();
        project.setIDEProjectProperties(SimpleIDEProjectProperties.builder(project.getIDEProjectProperties())
                .setScriptConfigurations(form.getScriptConfiguration())
                .setScriptModellingExclusions(form.getExclusionWildcards()).build());
    }
}
