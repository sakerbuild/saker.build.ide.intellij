package saker.build.ide.intellij.impl.properties;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import saker.build.ide.intellij.impl.IntellijSakerIDEProject;
import saker.build.ide.support.properties.IDEProjectProperties;
import saker.build.ide.support.properties.MountPathIDEProperty;
import saker.build.ide.support.properties.SimpleIDEProjectProperties;

import javax.swing.JComponent;
import javax.swing.JLabel;
import java.util.Objects;

public class SakerBuildProjectConfigurable implements Configurable, Configurable.Composite {
    private final IntellijSakerIDEProject project;
    private final Configurable[] configurables;

    private ProjectConfigurationForm form;

    private boolean embedBuildTraceArtifacts;
    private boolean requireIDEConfiguration;
    private MountPathIDEProperty buildTraceOutput;

    public SakerBuildProjectConfigurable(IntellijSakerIDEProject project) {
        this.project = project;
        this.configurables = new Configurable[] { new DaemonConnectionsConfigurable(this),
                new PathConfigurationConfigurable(this),
                new ScriptConfigurationConfigurable(this),
                new TaskRepositoriesConfigurable(this),
                new ExecutionUserParametersConfigureable(this), };
        this.form = new ProjectConfigurationForm(this);
    }

    public IDEProjectProperties getCurrentProjectProperties() {
        return project.getIDEProjectProperties();
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Saker.build Project Settings";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        return form.getRootPanel();
    }

    @Override
    public void disposeUIResources() {
        form.dispose();
    }

    @Override
    public void reset() {
        form.reset(getCurrentProjectProperties());

        embedBuildTraceArtifacts = form.isEmbedBuildTraceArtifacts();
        requireIDEConfiguration = form.isRequireIDEConfiguration();
        buildTraceOutput = form.getBuildTraceOutput();
    }

    @Override
    public boolean isModified() {
        if (this.embedBuildTraceArtifacts != form.isEmbedBuildTraceArtifacts()) {
            return true;
        }
        if (this.requireIDEConfiguration != form.isRequireIDEConfiguration()) {
            return true;
        }
        if (!Objects.equals(this.buildTraceOutput, form.getBuildTraceOutput())) {
            return true;
        }
        return false;
    }

    @Override
    public void apply() throws ConfigurationException {
        project.setIDEProjectProperties(SimpleIDEProjectProperties.builder(project.getIDEProjectProperties())
                .setRequireTaskIDEConfiguration(form.isRequireIDEConfiguration())
                .setBuildTraceOutput(form.getBuildTraceOutput())
                .setBuildTraceEmbedArtifacts(form.isEmbedBuildTraceArtifacts()).build());
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
