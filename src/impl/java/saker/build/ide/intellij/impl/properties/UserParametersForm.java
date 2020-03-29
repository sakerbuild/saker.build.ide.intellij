package saker.build.ide.intellij.impl.properties;

import com.intellij.icons.AllIcons;
import com.intellij.ui.AddEditRemovePanel;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.IconUtil;
import org.jetbrains.annotations.Nullable;
import saker.build.ide.intellij.impl.ui.SakerPropertyPageAddEditRemovePanel;
import saker.build.thirdparty.saker.util.ObjectUtils;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class UserParametersForm {
    private JTabbedPane tabbedPane;
    private JPanel rootPanel;
    private JPanel parametersPanel;
    private JBLabel parametersInfoLabel;

    private AddEditRemovePanel<Map.Entry<String, String>> parametersEditPanel;
    private String userParameterKind = "";

    private UserParametersForm() {
        $$$setupUI$$$();
        parametersEditPanel = new SakerPropertyPageAddEditRemovePanel<Map.Entry<String, String>>(
                new KeyValueTableModel()) {
            @Nullable
            @Override
            protected Map.Entry<String, String> addItem() {
                return addUserParameter();
            }

            @Nullable
            @Override
            protected Map.Entry<String, String> editItem(Map.Entry<String, String> o) {
                return editUserParameter(o);
            }
        };

        parametersPanel.add(parametersEditPanel);

        JLabel label = (JLabel) tabbedPane.getTabComponentAt(1);
        label.setIcon(IconUtil.scale(AllIcons.Plugins.PluginLogo, label, 0.4f));
    }

    public UserParametersForm(ExecutionUserParametersConfigureable configurable) {
        this();
        parametersEditPanel.getTable().getModel().addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                configurable.getParent().getBuilder().setUserParameters(getCurrentValues());
            }
        });
    }

    public UserParametersForm(EnvironmentUserParametersConfigurable configurable) {
        this();
    }

    public Set<Map.Entry<String, String>> getCurrentValues() {
        return new LinkedHashSet<>(parametersEditPanel.getData());
    }

    public JLabel getParametersInfoLabel() {
        return parametersInfoLabel;
    }

    public void setUserParameterKind(String kind) {
        if (kind == null) {
            userParameterKind = "";
        } else {
            userParameterKind = kind;
        }
        getParametersEditPanel().getEmptyText().setText("No " + userParameterKind + " user parameters defined.");
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
        UserParameterEditorDialog dialog = new UserParameterEditorDialog(
                "Edit " + userParameterKind + " user parameter", getParametersPanel());
        Set<String> existingkeys = getKeys();
        existingkeys.remove(entry.getKey());
        dialog.setExistingKeys(existingkeys);
        initDialogMessages(dialog);
        dialog.setEntry(entry);
        dialog.setVisible(true);
        return dialog.getEntry();
    }

    protected Map.Entry<String, String> addUserParameter() {
        UserParameterEditorDialog dialog = new UserParameterEditorDialog(
                "Add " + userParameterKind + " user " + "parameter", getParametersPanel());
        dialog.setExistingKeys(getKeys());
        initDialogMessages(dialog);
        dialog.setVisible(true);
        return dialog.getEntry();
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
        createUIComponents();
        rootPanel = new JPanel();
        rootPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        rootPanel.add(tabbedPane,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null,
                        new Dimension(200, 200), null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane.addTab("Parameters", panel1);
        parametersInfoLabel = new JBLabel();
        panel1.add(parametersInfoLabel,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0,
                        false));
        parametersPanel = new JPanel();
        parametersPanel.setLayout(new CardLayout(0, 0));
        panel1.add(parametersPanel,
                new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
                        0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane.addTab("Extensions", panel2);
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

    private static final String[] COLUMN_NAMES = { "Key", "Value" };

    private void createUIComponents() {
        tabbedPane = new JBTabbedPane();
    }

    public static class KeyValueTableModel extends AddEditRemovePanel.TableModel<Map.Entry<String, String>> {

        @Override
        public int getColumnCount() {
            return COLUMN_NAMES.length;
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
