package saker.build.ide.intellij.impl.properties;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;
import saker.build.ide.intellij.impl.IntellijSakerIDEProject;
import saker.build.ide.support.properties.IDEProjectProperties;
import saker.build.ide.support.properties.SimpleIDEProjectProperties;
import saker.build.thirdparty.saker.util.ImmutableUtils;

import javax.swing.*;
import java.util.*;

public class ExecutionUserParametersConfigureable implements Configurable {
    private final IntellijSakerIDEProject project;
    private Set<? extends Map.Entry<String, String>> userParameters = null;

    private final UserParametersForm form;

    public ExecutionUserParametersConfigureable(IntellijSakerIDEProject project) {
        this.project = project;

        form = new UserParametersForm();
        form.setUserParameterKind("execution");
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
