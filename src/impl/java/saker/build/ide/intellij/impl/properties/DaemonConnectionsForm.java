package saker.build.ide.intellij.impl.properties;

import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.CommonActionsPanel;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import saker.build.ide.intellij.impl.ui.PropertyAttributeTreeNode;
import saker.build.ide.intellij.impl.ui.PropertyTreeNode;
import saker.build.ide.intellij.impl.ui.RootTreeNode;
import saker.build.ide.support.properties.DaemonConnectionIDEProperty;
import saker.build.ide.support.properties.IDEProjectProperties;
import saker.build.ide.support.ui.ExecutionDaemonSelector;
import saker.build.thirdparty.saker.util.ObjectUtils;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.*;

public class DaemonConnectionsForm {
    private JPanel panel1;
    private JComboBox<String> executionDaemonComboBox;
    private JPanel treeContainer;
    private JPanel rootPanel;

    private JTree configTree;
    private ExecutionDaemonSelector daemonSelector = new ExecutionDaemonSelector(null);
    private RootTreeNode<DaemonPropertyTreeNode> rootTreeNode = new RootTreeNode<>();
    private ToolbarDecorator decorator;

    public DaemonConnectionsForm() {
        DefaultTreeModel treemodel = new DefaultTreeModel(rootTreeNode, false);
        configTree = new JTree(treemodel);
        configTree.setRootVisible(false);

        DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) configTree.getCellRenderer();
        renderer.setClosedIcon(null);
        renderer.setIcon(null);
        renderer.setLeafIcon(null);
        renderer.setOpenIcon(null);
        renderer.setDisabledIcon(null);

        decorator = ToolbarDecorator.createDecorator(configTree);
        treeContainer.add(decorator.disableUpDownActions().setToolbarPosition(ActionToolbarPosition.RIGHT)
                .setAddAction(this::addAction).setRemoveAction(this::removeAction).setEditAction(this::editAction)
                .createPanel());

        configTree.addTreeSelectionListener(e -> {
            decorator.getActionsPanel()
                    .setEnabled(CommonActionsPanel.Buttons.EDIT, configTree.getSelectionPath() != null);
        });

        decorator.getActionsPanel().setEnabled(CommonActionsPanel.Buttons.EDIT, configTree.getSelectionPath() != null);

        updateDaemonComboBox();
        executionDaemonComboBox.addItemListener(e -> {
            daemonSelector.setExecutionDaemonIndex(executionDaemonComboBox.getSelectedIndex());
        });
    }

    public void dispose() {
    }

    public void reset(IDEProjectProperties properties) {
        daemonSelector.reset(properties);
        for (DaemonConnectionIDEProperty connection : properties.getConnections()) {
            DaemonPropertyTreeNode node = new DaemonPropertyTreeNode(rootTreeNode);
            node.setProperty(connection);
            rootTreeNode.add(node);
        }
        ((DefaultTreeModel) configTree.getModel()).reload(rootTreeNode);

        updateDaemonComboBox();
    }

    private void updateDaemonComboBox() {
        executionDaemonComboBox.setModel(
                new DefaultComboBoxModel<>(daemonSelector.getLabels().toArray(ObjectUtils.EMPTY_STRING_ARRAY)));
        executionDaemonComboBox.setSelectedIndex(daemonSelector.getExecutionDaemonIndex());
    }

    public String getExecutionDaemonName() {
        return daemonSelector.getSelectedExecutionDaemonName();
    }

    public Set<DaemonConnectionIDEProperty> getDaemonConnections() {
        LinkedHashSet<DaemonConnectionIDEProperty> result = new LinkedHashSet<>();
        for (DaemonPropertyTreeNode n : rootTreeNode.getNodes()) {
            result.add(n.getProperty());
        }
        return result;
    }

    private void addAction(AnActionButton button) {
        DaemonConnectionEditorDialog dialog = new DaemonConnectionEditorDialog("Add Daemon Connection", configTree);
        dialog.setVisible(true);
        DaemonConnectionIDEProperty property = dialog.getDaemonConnectionIDEProperty();
        if (property != null) {
            DaemonPropertyTreeNode node = new DaemonPropertyTreeNode(rootTreeNode);
            node.setProperty(property);
            int idx = rootTreeNode.add(node);
            ((DefaultTreeModel) configTree.getModel()).nodesWereInserted(rootTreeNode, new int[] { idx });

            resetDaemonSelector();
        }
    }

    private void removeAction(AnActionButton button) {
        TreePath[] selection = configTree.getSelectionPaths();
        if (selection == null) {
            return;
        }
        Set<DaemonPropertyTreeNode> nodestoremove = new HashSet<>();
        for (TreePath selpath : selection) {
            Object[] path = selpath.getPath();
            if (path.length == 0) {
                continue;
            }
            if (path[path.length - 1] instanceof DaemonPropertyTreeNode) {
                nodestoremove.add((DaemonPropertyTreeNode) path[path.length - 1]);
            }
        }
        if (!nodestoremove.isEmpty()) {
            for (DaemonPropertyTreeNode n : nodestoremove) {
                int idx = rootTreeNode.getIndex(n);
                rootTreeNode.remove(n);
                ((DefaultTreeModel) configTree.getModel())
                        .nodesWereRemoved(rootTreeNode, new int[] { idx }, new Object[] { n });
            }
            resetDaemonSelector();
        }
    }

    private void editAction(AnActionButton button) {
        Object selection = configTree.getLastSelectedPathComponent();
        if (!(selection instanceof DaemonPropertyTreeNode)) {
            return;
        }
        DaemonConnectionEditorDialog dialog = new DaemonConnectionEditorDialog("Edit Daemon Connection", configTree);
        DaemonPropertyTreeNode propertynode = (DaemonPropertyTreeNode) selection;
        DaemonConnectionIDEProperty editedproperty = propertynode.getProperty();
        dialog.setEditProperty(editedproperty);
        dialog.setVisible(true);
        DaemonConnectionIDEProperty property = dialog.getDaemonConnectionIDEProperty();
        String execdaemonname = getExecutionDaemonName();
        if (property != null) {
            propertynode.setProperty(property);
            DefaultTreeModel model = (DefaultTreeModel) configTree.getModel();
            model.nodeStructureChanged(propertynode);
            if (execdaemonname != null && execdaemonname.equals(editedproperty.getConnectionName())) {
                //rename the execution daemon reference as well
                resetDaemonSelectorWithExecutionDaemonName(property.getConnectionName());
            } else {
                resetDaemonSelector();
            }
        }
    }

    private void resetDaemonSelector() {
        String execdaemonname = getExecutionDaemonName();
        resetDaemonSelectorWithExecutionDaemonName(execdaemonname);
    }

    private void resetDaemonSelectorWithExecutionDaemonName(String execdaemonname) {
        daemonSelector.reset(getDaemonConnections(), execdaemonname);
        updateDaemonComboBox();
    }

    public JPanel getRootPanel() {
        return rootPanel;
    }

    private static final class DaemonPropertyTreeNode extends PropertyTreeNode<DaemonConnectionIDEProperty> {
        public DaemonPropertyTreeNode(TreeNode parent) {
            super(parent);
        }

        @Override
        public void setProperty(DaemonConnectionIDEProperty property) {
            super.setProperty(property);
            this.children = Arrays
                    .asList(new PropertyAttributeTreeNode(this, "Connection name", property.getConnectionName()),
                            new PropertyAttributeTreeNode(this, "Address", property.getNetAddress()),
                            new PropertyAttributeTreeNode(this, "Use as cluster",
                                    Boolean.toString(property.isUseAsCluster())));
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (!ObjectUtils.isNullOrEmpty(property.getConnectionName())) {
                sb.append(property.getConnectionName());
            }
            if (!ObjectUtils.isNullOrEmpty(property.getNetAddress())) {
                sb.append(" @");
                sb.append(property.getNetAddress());
            }
            if (sb.length() == 0) {
                return "<unknown>";
            }
            return sb.toString();
        }
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
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
        rootPanel.setLayout(new GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        treeContainer = new JPanel();
        treeContainer.setLayout(new CardLayout(0, 0));
        rootPanel.add(treeContainer,
                new GridConstraints(0, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
                        0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Execution daemon:");
        rootPanel.add(label1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        executionDaemonComboBox = new JComboBox();
        rootPanel.add(executionDaemonComboBox,
                new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0,
                        false));
        final Spacer spacer1 = new Spacer();
        rootPanel.add(spacer1,
                new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return rootPanel;
    }

}
