package saker.build.ide.intellij.impl.properties;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;
import saker.build.ide.intellij.impl.IntellijSakerIDEProject;
import saker.build.ide.support.properties.DaemonConnectionIDEProperty;
import saker.build.ide.support.properties.IDEProjectProperties;
import saker.build.ide.support.properties.SimpleIDEProjectProperties;
import saker.build.thirdparty.saker.util.ImmutableUtils;

import javax.swing.JComponent;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

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
    }
}
