package saker.build.ide.intellij.impl.properties;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;
import saker.build.ide.intellij.impl.IntellijSakerIDEProject;
import saker.build.ide.support.properties.DaemonConnectionIDEProperty;
import saker.build.ide.support.properties.IDEProjectProperties;
import saker.build.thirdparty.saker.util.ObjectUtils;

import javax.swing.*;
import java.util.ArrayList;

public class DaemonConnectionsConfigurable implements Configurable {
    private final IntellijSakerIDEProject project;
    private final DaemonConnectionsForm form;

    //    private List<TreePropertyItem<DaemonConnectionIDEProperty>> connectionItems = new ArrayList<>();
    private String executionDaemonName;

    public DaemonConnectionsConfigurable(IntellijSakerIDEProject project) {
        this.project = project;
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
    public void reset() {
        this.executionDaemonName = null;

        IDEProjectProperties props = project.getIDEProjectProperties();
        if (props != null) {
            this.executionDaemonName = props.getExecutionDaemonConnectionName();
            if (ObjectUtils.isNullOrEmpty(executionDaemonName)) {
                //nullize out empty string
                executionDaemonName = null;
            }
        }
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void apply() throws ConfigurationException {

    }
}
