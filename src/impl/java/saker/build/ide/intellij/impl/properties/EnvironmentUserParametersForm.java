package saker.build.ide.intellij.impl.properties;

import com.intellij.ui.AddEditRemovePanel;
import com.intellij.ui.table.JBTable;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.ui.StatusText;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import saker.build.thirdparty.saker.util.ObjectUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public abstract class EnvironmentUserParametersForm {
    private JTabbedPane tabbedPane1;
    private JPanel rootPanel;
    private JPanel parametersPanel;

    private AddEditRemovePanel<Map.Entry<String, String>> parametersEditPanel;

    public EnvironmentUserParametersForm() {
        $$$setupUI$$$();

        parametersEditPanel = new AddEditRemovePanel<Map.Entry<String, String>>(new UserParametersTableModel(),
                Collections.emptyList()) {
            {
                getTable().setShowColumns(true);
                getTable().getTableHeader().setReorderingAllowed(false);
                getEmptyText().clear().setText("No environment user parameters defined.");
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

    public AddEditRemovePanel<Map.Entry<String, String>> getParametersEditPanel() {
        return parametersEditPanel;
    }

    public void setUserParameters(Set<? extends Map.Entry<String, String>> parameters) {
        parametersEditPanel.setData(ObjectUtils.newArrayList(parameters));
    }

    public JPanel getParametersPanel() {
        return parametersPanel;
    }

    protected abstract Map.Entry<String, String> editUserParameter(Map.Entry<String, String> entry);

    protected abstract Map.Entry<String, String> addUserParameter();

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
