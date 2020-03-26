package saker.build.ide.intellij.impl.properties;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;
import saker.build.ide.intellij.impl.IntellijSakerIDEProject;
import saker.build.ide.support.properties.IDEProjectProperties;
import saker.build.ide.support.properties.SimpleIDEProjectProperties;

import javax.swing.JComponent;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ExecutionUserParametersConfigureable implements Configurable, Configurable.NoScroll {
    private final IntellijSakerIDEProject project;
    private Set<? extends Map.Entry<String, String>> userParameters = null;

    private final UserParametersForm form;

    public ExecutionUserParametersConfigureable(SakerBuildProjectConfigurable parent) {
        this.project = parent.getProject();

        form = new UserParametersForm();
        form.setUserParameterKind("execution");
        form.getParametersInfoLabel().setText("The following user parameters are defined for the build execution.");
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "User Parameters";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        return form.getRootPanel();
    }

    @Override
    public void reset() {
        this.userParameters = null;
        IDEProjectProperties props = project.getIDEProjectProperties();
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
        project.setIDEProjectProperties(SimpleIDEProjectProperties.builder(project.getIDEProjectProperties())
                .setUserParameters(getCurrentValues()).build());
    }
}
