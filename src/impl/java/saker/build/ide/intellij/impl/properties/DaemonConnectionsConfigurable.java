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
    private final IntellijSakerIDEProject project;
    private final DaemonConnectionsForm form;

    private String executionDaemonName;
    private Set<DaemonConnectionIDEProperty> daemonConnections = Collections.emptySet();

    public DaemonConnectionsConfigurable(SakerBuildProjectConfigurable parent) {
        this.project = parent.getProject();
        form = new DaemonConnectionsForm();
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
        IDEProjectProperties props = project.getIDEProjectProperties();
        form.reset(props);

        this.executionDaemonName = form.getExecutionDaemonName();
        this.daemonConnections = form.getDaemonConnections();
    }

    @Override
    public boolean isModified() {
        if (!Objects.equals(this.executionDaemonName, form.getExecutionDaemonName())) {
            return true;
        }
        if (!daemonConnections.equals(form.getDaemonConnections())) {
            return true;
        }
        return false;
    }

    @Override
    public void apply() throws ConfigurationException {
        Set<DaemonConnectionIDEProperty> connections = form.getDaemonConnections();
        String execdaemonname = form.getExecutionDaemonName();
        project.setIDEProjectProperties(SimpleIDEProjectProperties.builder(project.getIDEProjectProperties())
                .setConnections(ImmutableUtils.makeImmutableLinkedHashSet((connections)))
                .setExecutionDaemonConnectionName(execdaemonname).build());
    }
}
