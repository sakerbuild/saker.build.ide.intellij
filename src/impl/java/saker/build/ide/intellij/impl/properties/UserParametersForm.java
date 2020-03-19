package saker.build.ide.intellij.impl.properties;

import com.intellij.ui.AddEditRemovePanel;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import org.jetbrains.annotations.Nullable;
import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.build.thirdparty.saker.util.ObjectUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class UserParametersForm {
    private JTabbedPane tabbedPane1;
    private JPanel rootPanel;
    private JPanel parametersPanel;

    private AddEditRemovePanel<Map.Entry<String, String>> parametersEditPanel;
    private String userParameterKind = "";

    public UserParametersForm() {
        $$$setupUI$$$();

        parametersEditPanel = new AddEditRemovePanel<Map.Entry<String, String>>(new UserParametersTableModel(),
                Collections.emptyList()) {
            {
                getTable().setShowColumns(true);
                getTable().getTableHeader().setReorderingAllowed(false);
            }

            @Nullable
            @Override
            protected Map.Entry<String, String> addItem() {
                return addUserParameter();
            }

            @Override
            protected boolean removeItem(Map.Entry<String, String> o) {
                return true;
            }

            @Nullable
            @Override
            protected Map.Entry<String, String> editItem(Map.Entry<String, String> o) {
                return editUserParameter(o);
            }

        };

        parametersPanel.add(parametersEditPanel,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null,
                        new Dimension(200, 200), null, 0, false));
    }

    public void setUserParameterKind(String kind) {
        if (kind == null) {
            userParameterKind = "";
        } else {
            userParameterKind = kind;
        }
        getParametersEditPanel().getEmptyText().setText("No " + userParameterKind + " user parameters defined");
    }

    public AddEditRemovePanel<Map.Entry<String, String>> getParametersEditPanel() {
        return parametersEditPanel;
    }

    public void setUserParameters(Set<? extends Map.Entry<String, String>> parameters) {
        parametersEditPanel.setData(ObjectUtils.newArrayList(parameters));
    }

    public JPanel getParametersPanel() {
        return parametersPanel;
    }

    protected Map.Entry<String, String> editUserParameter(Map.Entry<String, String> entry) {
        Map.Entry<String, String>[] result = new Map.Entry[] { entry };
        UserParameterEditorDialog dialog = new UserParameterEditorDialog(
                "Edit " + userParameterKind + " user parameter", getParametersPanel()) {
            @Override
            protected void onOK() {
                result[0] = ImmutableUtils
                        .makeImmutableMapEntry(getKeyTextField().getText(), getValueTextField().getText());
                super.onOK();
            }
        };
        Set<String> existingkeys = getKeys();
        existingkeys.remove(entry.getKey());
        dialog.setExistingKeys(existingkeys);
        initDialogMessages(dialog);
        dialog.setEditValues(entry.getKey(), entry.getValue());
        dialog.setVisible(true);
        return result[0];
    }

    protected Map.Entry<String, String> addUserParameter() {
        Map.Entry<String, String>[] result = new Map.Entry[] { null };
        UserParameterEditorDialog dialog = new UserParameterEditorDialog(
                "Add " + userParameterKind + " user " + "parameter", getParametersPanel()) {
            @Override
            protected void onOK() {
                result[0] = ImmutableUtils
                        .makeImmutableMapEntry(getKeyTextField().getText(), getValueTextField().getText());
                super.onOK();
            }
        };
        dialog.setExistingKeys(getKeys());
        initDialogMessages(dialog);
        dialog.setVisible(true);
        return result[0];
    }

    private Set<String> getKeys() {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (Map.Entry<String, String> entry : parametersEditPanel.getData()) {
            result.add(entry.getKey());
        }
        return result;
    }

    private void initDialogMessages(UserParameterEditorDialog dialog) {
        if (userParameterKind.isEmpty()) {
            dialog.getTitleLabel().setText("User parameter");
            dialog.getInfoLabel().setText("Specify an user parameter.");
        } else {
            dialog.getTitleLabel().setText(Character.toUpperCase(userParameterKind.charAt(0)) + userParameterKind
                    .substring(1) + " user parameter");
            StringBuilder sb = new StringBuilder().append("Specify an ").append(userParameterKind)
                    .append(" user parameter for the build.");
            if ("environment".equals(userParameterKind)) {
                sb.append(" (-E option)");
            } else if ("execution".equals(userParameterKind)) {
                sb.append(" (-U option)");
            }
            dialog.getInfoLabel().setText(sb.toString());
        }
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        rootPanel = new JPanel();
        rootPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane1 = new JTabbedPane();
        rootPanel.add(tabbedPane1,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null,
                        new Dimension(200, 200), null, 1, false));
        parametersPanel = new JPanel();
        parametersPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane1.addTab("Parameters", parametersPanel);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane1.addTab("Extensions", panel1);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return rootPanel;
    }

    public JPanel getRootPanel() {
        return rootPanel;
    }

    private static class UserParametersTableModel extends AddEditRemovePanel.TableModel<Map.Entry<String, String>> {
        private static final String[] COLUMN_NAMES = { "Key", "Value" };

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Nullable
        @Override
        public String getColumnName(int columnIndex) {
            return COLUMN_NAMES[columnIndex];
        }

        @Override
        public Object getField(Map.Entry<String, String> o, int columnIndex) {
            return columnIndex == 0 ? o.getKey() : o.getValue();
        }
    }
}
