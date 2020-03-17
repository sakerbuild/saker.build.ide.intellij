package saker.build.ide.intellij.impl.properties;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;
import saker.build.ide.intellij.impl.IntellijSakerIDEPlugin;
import saker.build.ide.support.SimpleIDEPluginProperties;
import saker.build.ide.support.properties.IDEPluginProperties;
import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.build.thirdparty.saker.util.ObjectUtils;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;

public class EnvironmentUserParametersConfigurable implements Configurable {
    private IntellijSakerIDEPlugin plugin;
    private Set<? extends Map.Entry<String, String>> userParameters = null;

    private EnvironmentUserParametersForm form;

    public EnvironmentUserParametersConfigurable() {
        form = new EnvironmentUserParametersForm();
        JTable table = form.getUserParametersTable();
        table.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent mouseEvent) {
                if (mouseEvent.getClickCount() == 2) {
                    int rowidx = table.getSelectedRow();
                    if (rowidx != -1) {
                        DefaultTableModel model = (DefaultTableModel) table.getModel();
                        UserParameterEditorDialog dialog = new UserParameterEditorDialog(
                                "Edit environment user parameter", table) {
                            @Override
                            protected void onOK() {
                                model.setValueAt(getKeyTextField().getText(), rowidx, 0);
                                model.setValueAt(getValueTextField().getText(), rowidx, 1);
                                super.onOK();
                                model.fireTableDataChanged();
                            }

                            @Override
                            protected void onDelete() {
                                model.removeRow(rowidx);
                                super.onDelete();
                                model.fireTableDataChanged();
                            }
                        };
                        dialog.setEditValues((String) model.getValueAt(rowidx, 0),
                                (String) model.getValueAt(rowidx, 1));
                        int colidx = table.getSelectedColumn();
                        JTextField editedtf;
                        if (colidx == 1) {
                            editedtf = dialog.getValueTextField();
                        } else {
                            editedtf = dialog.getKeyTextField();
                        }
                        editedtf.select(0, editedtf.getText().length());
                        editedtf.requestFocus();
                        dialog.setVisible(true);
                    }
                }
            }
        });
        form.getAddButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                UserParameterEditorDialog dialog = new UserParameterEditorDialog("Add environment user parameter",
                        table) {
                    @Override
                    protected void onOK() {
                        DefaultTableModel model = (DefaultTableModel) table.getModel();
                        model.addRow(new String[] { getKeyTextField().getText(), getValueTextField().getText() });
                        super.onOK();
                        model.fireTableDataChanged();
                    }
                };
                dialog.setVisible(true);
            }
        });

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
        JTable table = form.getUserParametersTable();

        DefaultTableModel model = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        model.addColumn("Key");
        model.addColumn("Value");
        for (Map.Entry<String, String> entry : this.userParameters) {
            model.addRow(new String[] { entry.getKey(), entry.getValue() });
        }
        table.setModel(model);

    }

    private Set<Map.Entry<String, String>> getCurrentValues() {
        JTable table = form.getUserParametersTable();
        TableModel model = table.getModel();
        int rc = model.getRowCount();
        LinkedHashSet<Map.Entry<String, String>> result = new LinkedHashSet<>();
        for (int i = 0; i < rc; ++i) {
            String key = (String) model.getValueAt(i, 0);
            String value = (String) model.getValueAt(i, 1);
            result.add(ImmutableUtils.makeImmutableMapEntry(key, value));
        }
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
