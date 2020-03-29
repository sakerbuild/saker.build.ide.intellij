package saker.build.ide.intellij.impl.properties;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;
import saker.build.ide.support.properties.IDEProjectProperties;

import javax.swing.JComponent;
import java.util.Objects;

public class DaemonConnectionsConfigurable implements Configurable, Configurable.NoScroll {
    private final SakerBuildProjectConfigurable parent;
    private final DaemonConnectionsForm form;

    public DaemonConnectionsConfigurable(SakerBuildProjectConfigurable parent) {
        this.parent = parent;
        this.form = new DaemonConnectionsForm(this);
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Daemon Connections";
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
        IDEProjectProperties currentprops = parent.getCurrentProjectProperties();
        IDEProjectProperties properties = parent.getProperties();

        if (!Objects.equals(currentprops.getExecutionDaemonConnectionName(), properties.getExecutionDaemonConnectionName())) {
            return true;
        }
        if (!Objects.equals(currentprops.getConnections(), properties.getConnections())) {
            return true;
        }
        return false;
    }

    public SakerBuildProjectConfigurable getParent() {
        return parent;
    }

    @Override
    public void apply() throws ConfigurationException {
        parent.apply();
    }
}
