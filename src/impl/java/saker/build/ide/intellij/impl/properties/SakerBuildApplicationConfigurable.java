package saker.build.ide.intellij.impl.properties;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import saker.build.ide.intellij.ExtensionDisablement;
import saker.build.ide.intellij.impl.IntellijSakerIDEPlugin;
import saker.build.ide.support.SimpleIDEPluginProperties;
import saker.build.ide.support.properties.IDEPluginProperties;
import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.build.thirdparty.saker.util.ObjectUtils;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.util.Objects;
import java.util.Set;

public class SakerBuildApplicationConfigurable implements Configurable, Configurable.Composite {
    private final IntellijSakerIDEPlugin plugin;
    private Configurable[] configurables;

    private ApplicationConfigurationForm form;

    private IDEPluginProperties properties;
    private Set<ExtensionDisablement> extensionDisablements;

    private final SimpleIDEPluginProperties.Builder builder;
    private Set<ExtensionDisablement> pendingExtensionDisablements;

    public SakerBuildApplicationConfigurable(IntellijSakerIDEPlugin plugin) {
        this.plugin = plugin;
        this.properties = plugin.getIDEPluginProperties();
        this.builder = SimpleIDEPluginProperties.builder(this.properties);
        this.extensionDisablements = plugin.getExtensionDisablements();
        this.pendingExtensionDisablements = ImmutableUtils.makeImmutableLinkedHashSet(extensionDisablements);

        this.configurables = new Configurable[] { new EnvironmentUserParametersConfigurable(this) };

        this.form = new ApplicationConfigurationForm(this);
    }

    public IDEPluginProperties getCurrentPluginProperties() {
        return builder.buildReuse();
    }

    public IDEPluginProperties getProperties() {
        return properties;
    }

    public SimpleIDEPluginProperties.Builder getBuilder() {
        return builder;
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

    public IntellijSakerIDEPlugin getPlugin() {
        return plugin;
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Saker.build";
    }

    @Override
    public void reset() {
        form.reset();
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        JPanel panel = new JPanel(new GridLayoutManager(1, 1));
        GridConstraints constraints = new GridConstraints();
        constraints.setAnchor(GridConstraints.ANCHOR_NORTHWEST);
        panel.add(new JLabel("See the sub-pages for configuring the saker.build plugin."), constraints);
        return form.getRootPanel();
    }

    @Override
    public boolean isModified() {
        IDEPluginProperties currentprops = getCurrentPluginProperties();
        if (!Objects.equals(currentprops.getExceptionFormat(), properties.getExceptionFormat())) {
            return true;
        }
        if (!Objects.equals(currentprops.getPort(), this.properties.getPort())) {
            return true;
        }
        if (!Objects.equals(currentprops.getActsAsServer(), this.properties.getActsAsServer())) {
            return true;
        }
        return false;
    }

    @Override
    public void apply() throws ConfigurationException {
        IDEPluginProperties properties = getCurrentPluginProperties();
        if (!this.properties.equals(properties)) {
            plugin.setIDEPluginProperties(properties, this.pendingExtensionDisablements);
            this.properties = properties;
        }
    }

    @NotNull
    @Override
    public Configurable[] getConfigurables() {
        return configurables;
    }
}
