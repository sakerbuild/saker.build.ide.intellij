package saker.build.ide.intellij.impl.properties;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import saker.build.ide.intellij.impl.IntellijSakerIDEProject;
import saker.build.ide.support.properties.IDEProjectProperties;
import saker.build.ide.support.properties.MountPathIDEProperty;
import saker.build.ide.support.properties.SimpleIDEProjectProperties;

import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import java.util.Objects;
import java.util.function.Consumer;

public class SakerBuildProjectConfigurable implements Configurable, Configurable.Composite {
    private final IntellijSakerIDEProject project;
    private final Configurable[] configurables;

    private ProjectConfigurationForm form;

    private IDEProjectProperties properties;
    private final SimpleIDEProjectProperties.Builder builder;

    public SakerBuildProjectConfigurable(IntellijSakerIDEProject project) {
        this.project = project;
        this.properties = project.getIDEProjectProperties();
        this.builder = SimpleIDEProjectProperties.builder(this.properties);

        this.configurables = new Configurable[] { new DaemonConnectionsConfigurable(this),
                new PathConfigurationConfigurable(this),
                new ScriptConfigurationConfigurable(this),
                new TaskRepositoriesConfigurable(this),
                new ExecutionUserParametersConfigureable(this), };
        this.form = new ProjectConfigurationForm(this);
    }

    public IDEProjectProperties getCurrentProjectProperties() {
        return this.builder.buildReuse();
    }

    public IDEProjectProperties getProperties() {
        return properties;
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
        form.reset();
    }

    @Override
    public boolean isModified() {
        IDEProjectProperties currentprops = getCurrentProjectProperties();
        if (currentprops.isRequireTaskIDEConfiguration() != this.properties.isRequireTaskIDEConfiguration()) {
            return true;
        }
        if (currentprops.isBuildTraceEmbedArtifacts() != this.properties.isBuildTraceEmbedArtifacts()) {
            return true;
        }
        if (!Objects.equals(currentprops.getBuildTraceOutput(), this.properties.getBuildTraceOutput())) {
            return true;
        }
        return false;
    }

    @Override
    public void apply() throws ConfigurationException {
        IDEProjectProperties properties = this.builder.buildReuse();
        project.setIDEProjectProperties(properties);
        this.properties = properties;
    }

    public SimpleIDEProjectProperties.Builder getBuilder() {
        return builder;
    }

    public IntellijSakerIDEProject getProject() {
        return project;
    }

    @NotNull
    @Override
    public Configurable[] getConfigurables() {
        return configurables;
    }

    public static void addTextPropertyChangeListener(JTextField textfield, Consumer<? super String> field) {
        textfield.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                field.accept(textfield.getText());
            }
        });
    }
}
