package saker.build.ide.intellij.impl.properties;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import saker.build.ide.intellij.impl.IntellijSakerIDEProject;

import javax.swing.JComponent;
import javax.swing.JLabel;

public class SakerBuildProjectConfigurable implements Configurable, Configurable.Composite {
    private final IntellijSakerIDEProject project;
    private final Configurable[] configurables;

    public SakerBuildProjectConfigurable(IntellijSakerIDEProject project) {
        this.project = project;
        this.configurables = new Configurable[] { new DaemonConnectionsConfigurable(this),
                new PathConfigurationConfigurable(this),
                new ScriptConfigurationConfigurable(this),
                new TaskRepositoriesConfigurable(this),
                new ExecutionUserParametersConfigureable(this), };
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
    public void disposeUIResources() {
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void apply() throws ConfigurationException {
    }

    public IntellijSakerIDEProject getProject() {
        return project;
    }

    @NotNull
    @Override
    public Configurable[] getConfigurables() {
        return configurables;
    }
}
