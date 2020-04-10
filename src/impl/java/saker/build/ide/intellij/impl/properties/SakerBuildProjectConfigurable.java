package saker.build.ide.intellij.impl.properties;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.DocumentAdapter;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import saker.build.ide.intellij.ExtensionDisablement;
import saker.build.ide.intellij.impl.IntellijSakerIDEProject;
import saker.build.ide.support.properties.IDEProjectProperties;
import saker.build.ide.support.properties.SimpleIDEProjectProperties;
import saker.build.thirdparty.saker.util.ImmutableUtils;

import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public class SakerBuildProjectConfigurable implements Configurable, Configurable.Composite {
    private final IntellijSakerIDEProject project;
    private final Configurable[] configurables;

    private ProjectConfigurationForm form;

    private IDEProjectProperties properties;
    private Set<ExtensionDisablement> extensionDisablements;

    private final SimpleIDEProjectProperties.Builder builder;
    private Set<ExtensionDisablement> pendingExtensionDisablements;

    public SakerBuildProjectConfigurable(IntellijSakerIDEProject project) {
        this.project = project;
        this.properties = project.getIDEProjectProperties();
        this.builder = SimpleIDEProjectProperties.builder(this.properties);
        this.extensionDisablements = project.getExtensionDisablements();
        this.pendingExtensionDisablements = ImmutableUtils.makeImmutableLinkedHashSet(extensionDisablements);

        this.configurables = new Configurable[] { new DaemonConnectionsConfigurable(this),
                new PathConfigurationConfigurable(this),
                new ScriptConfigurationConfigurable(this),
                new TaskRepositoriesConfigurable(this),
                new ExecutionUserParametersConfigurable(this), };
        this.form = new ProjectConfigurationForm(this);
    }

    public IDEProjectProperties getCurrentProjectProperties() {
        return this.builder.buildReuse();
    }

    public IDEProjectProperties getProperties() {
        return properties;
    }

    public Set<ExtensionDisablement> getExtensionDisablements() {
        return extensionDisablements;
    }

    public Set<ExtensionDisablement> getCurrentExtensionDisablements() {
        return pendingExtensionDisablements;
    }

    public void setExtensionDisablements(Set<ExtensionDisablement> extensionDisablements) {
        this.pendingExtensionDisablements = extensionDisablements;
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
        IDEProjectProperties properties = getCurrentProjectProperties();
        if (!this.properties.equals(properties) || !this.extensionDisablements
                .equals(this.pendingExtensionDisablements)) {
            project.setIDEProjectProperties(properties, this.pendingExtensionDisablements);
            this.properties = properties;
        }
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
