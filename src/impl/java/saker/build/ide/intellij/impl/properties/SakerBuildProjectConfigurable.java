package saker.build.ide.intellij.impl.properties;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import saker.build.ide.intellij.impl.IntellijSakerIDEProject;

import javax.swing.*;

public class SakerBuildProjectConfigurable implements Configurable, Configurable.Composite {
    private IntellijSakerIDEProject project;

    public SakerBuildProjectConfigurable(IntellijSakerIDEProject project) {
        this.project = project;
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Saker.build Project Settings";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        return new JLabel("Project configurable");
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void apply() throws ConfigurationException {

    }

    @NotNull
    @Override
    public Configurable[] getConfigurables() {
        return new Configurable[] { new DaemonConnectionsConfigurable(project),
                new PathConfigurationConfigurable(project),
                new ScriptConfigurationConfigurable(project),
                new TaskRepositoriesConfigurable(project),
                new ExecutionUserParametersConfigureable(project), };
    }
}
