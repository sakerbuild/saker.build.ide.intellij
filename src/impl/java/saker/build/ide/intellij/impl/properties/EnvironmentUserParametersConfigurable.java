package saker.build.ide.intellij.impl.properties;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;
import saker.build.ide.intellij.impl.IntellijSakerIDEPlugin;
import saker.build.ide.support.SimpleIDEPluginProperties;
import saker.build.ide.support.properties.IDEPluginProperties;
import saker.build.thirdparty.saker.util.ImmutableUtils;

import javax.swing.*;
import java.util.*;

public class EnvironmentUserParametersConfigurable implements Configurable {
    private IntellijSakerIDEPlugin plugin;
    private Set<? extends Map.Entry<String, String>> userParameters = null;

    private EnvironmentUserParametersForm form;

    public EnvironmentUserParametersConfigurable() {
        form = new EnvironmentUserParametersForm() {

            @Override
            protected Map.Entry<String, String> editUserParameter(Map.Entry<String, String> entry) {
                Map.Entry<String, String>[] result = new Map.Entry[] { entry };
                UserParameterEditorDialog dialog = new UserParameterEditorDialog("Edit environment user parameter",
                        form.getParametersPanel()) {
                    @Override
                    protected void onOK() {
                        result[0] = ImmutableUtils
                                .makeImmutableMapEntry(getKeyTextField().getText(), getValueTextField().getText());
                        super.onOK();
                    }

                    @Override
                    protected void onDelete() {
                        result[0] = null;
                        super.onDelete();
                    }
                };
                dialog.setEditValues(entry.getKey(), entry.getValue());
                dialog.setVisible(true);
                return result[0];
            }

            @Override
            protected Map.Entry<String, String> addUserParameter() {
                Map.Entry<String, String>[] result = new Map.Entry[] { null };
                UserParameterEditorDialog dialog = new UserParameterEditorDialog("Add environment user parameter",
                        form.getParametersPanel()) {
                    @Override
                    protected void onOK() {
                        result[0] = ImmutableUtils
                                .makeImmutableMapEntry(getKeyTextField().getText(), getValueTextField().getText());
                        super.onOK();
                    }

                    @Override
                    protected void onDelete() {
                        super.onDelete();
                    }
                };
                dialog.setVisible(true);
                return result[0];
            }
        };

    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Environment User Parameters";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        return form.getRootPanel();
    }

    @Override
    public void reset() {
        plugin = IntellijSakerIDEPlugin.getInstance();
        IDEPluginProperties props = plugin.getIDEPluginProperties();
        if (props != null) {
            this.userParameters = props.getUserParameters();
        }
        if (this.userParameters == null) {
            this.userParameters = Collections.emptySet();
        }
        form.setUserParameters(this.userParameters);
    }

    private Set<Map.Entry<String, String>> getCurrentValues() {
        LinkedHashSet<Map.Entry<String, String>> result = new LinkedHashSet<>(form.getParametersEditPanel().getData());
        return result;
    }

    @Override
    public boolean isModified() {
        return !Objects.equals(this.userParameters, getCurrentValues());
    }

    @Override
    public void apply() throws ConfigurationException {
        Set<Map.Entry<String, String>> vals = getCurrentValues();
        plugin.setIDEPluginProperties(
                SimpleIDEPluginProperties.builder(plugin.getIDEPluginProperties()).setUserParameters(vals).build());
        this.userParameters = vals;
    }

}
