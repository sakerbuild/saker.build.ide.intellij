package saker.build.ide.intellij.impl.ui;

import com.intellij.navigation.ItemPresentation;
import com.intellij.ui.CheckboxTree;
import com.intellij.ui.CheckboxTreeBase;
import com.intellij.ui.CheckedTreeNode;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBLabel;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import saker.build.ide.intellij.extension.ideconfig.IIDEProjectConfigurationEntry;
import saker.build.ide.intellij.extension.ideconfig.IIDEProjectConfigurationRootEntry;
import saker.build.ide.intellij.impl.IntellijSakerIDEProject;
import saker.build.thirdparty.saker.util.ObjectUtils;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class IDEConfigurationSelectorDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JBLabel infoLabel;
    private CheckboxTree configTree;
    private JScrollPane treeScrollPane;
    private IntellijSakerIDEProject project;

    private ConfigurationEntryCheckedTreeNode rootTreeNode = new ConfigurationEntryCheckedTreeNode();

    private boolean ok;

    public IDEConfigurationSelectorDialog(IntellijSakerIDEProject project,
            List<IIDEProjectConfigurationRootEntry> rootentries, String configurationName) {
        this.project = project;
        $$$setupUI$$$();
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        setTitle("Apply IDE configuration" + (configurationName == null ? "" : " - " + configurationName));
        infoLabel.setText("Select the project configuration modifications to apply to project: " + project.getProject()
                .getName());

        buttonOK.addActionListener(e -> onOK());

        buttonCancel.addActionListener(e -> onCancel());

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        contentPane.registerKeyboardAction(e -> onCancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        for (IIDEProjectConfigurationEntry entry : rootentries) {
            if (entry == null) {
                continue;
            }
            ConfigurationEntryCheckedTreeNode node = new ConfigurationEntryCheckedTreeNode();
            node.setProperty(entry);
            rootTreeNode.add(node);
        }

        DefaultTreeModel treeModel = new DefaultTreeModel(rootTreeNode, false);
        configTree.setBackground(JBColor.background());
        configTree.setModel(treeModel);
        configTree.setRootVisible(false);
        configTree.getEmptyText().setText("No IDE configurations applicable.");
        treeScrollPane.setBackground(JBColor.background());

        pack();
        UIUtils.setLocationRelativeTo(this, project.getProject());
        setMinimumSize(getSize());
    }

    public boolean isOk() {
        return ok;
    }

    private void onOK() {
        ok = true;
        dispose();
    }

    private void onCancel() {
        dispose();
    }

    private void createUIComponents() {
        configTree = new CheckboxTree(new CheckboxTree.CheckboxTreeCellRenderer() {
            @Override
            public void customizeRenderer(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf,
                    int row, boolean hasFocus) {
                ColoredTreeCellRenderer component = getTextRenderer();
                component.clear();
                component.setIcon(null);
                if (value instanceof ConfigurationEntryCheckedTreeNode) {
                    ConfigurationEntryCheckedTreeNode node = (ConfigurationEntryCheckedTreeNode) value;
                    IIDEProjectConfigurationEntry property = node.getProperty();
                    if (property != null) {
                        ItemPresentation presentation = property.getLabelPresentation();
                        if (presentation != null) {
                            component.append(Objects.toString(presentation.getPresentableText(), ""));
                            String locationstr = presentation.getLocationString();
                            if (locationstr != null) {
                                component.append(" - " + locationstr, SimpleTextAttributes.GRAYED_ATTRIBUTES);
                            }
                            component.setIcon(presentation.getIcon(false));
                        } else {
                            component.append(Objects.toString(property.getLabel(), ""));
                        }
                    } else {
                        component.append(Objects.toString(value, ""));
                    }
                } else {
                    component.append(Objects.toString(value, ""));
                }
            }
        }, null, new CheckboxTreeBase.CheckPolicy(true, true, true, true));
    }

    private static class ConfigurationEntryCheckedTreeNode extends CheckedTreeNode {
        private IIDEProjectConfigurationEntry property;

        public void setProperty(IIDEProjectConfigurationEntry property) {
            this.property = property;
            this.setChecked(property.isSelected());

            IIDEProjectConfigurationEntry[] subentries = property.getSubEntries();
            if (!ObjectUtils.isNullOrEmpty(subentries)) {
                for (IIDEProjectConfigurationEntry entry : subentries) {
                    if (entry == null) {
                        continue;
                    }
                    ConfigurationEntryCheckedTreeNode cnode = new ConfigurationEntryCheckedTreeNode();
                    cnode.setProperty(entry);
                    this.add(cnode);
                }

            }
        }

        public IIDEProjectConfigurationEntry getProperty() {
            return property;
        }

        @Override
        public void setChecked(boolean checked) {
            super.setChecked(checked);
            if (property != null) {
                property.setSelected(checked);
            }
        }
    }

    private static class ConfigurationEntryTreeNode extends PropertyTreeNode<IIDEProjectConfigurationEntry> {

        public ConfigurationEntryTreeNode(TreeNode parent) {
            super(parent);
        }

        @Override
        public void setProperty(IIDEProjectConfigurationEntry property) {
            super.setProperty(property);
            IIDEProjectConfigurationEntry[] subentries = property.getSubEntries();
            this.children = new ArrayList<>();
            if (!ObjectUtils.isNullOrEmpty(subentries)) {
                for (IIDEProjectConfigurationEntry entry : subentries) {
                    if (entry == null) {
                        continue;
                    }
                    ConfigurationEntryTreeNode cnode = new ConfigurationEntryTreeNode(this);
                    cnode.setProperty(entry);
                    this.children.add(cnode);
                }

            }
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
        contentPane = new JPanel();
        contentPane.setLayout(new GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));
        contentPane.setMinimumSize(new Dimension(400, 400));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1,
                new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null,
                        null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1, true, false));
        panel1.add(panel2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0,
                false));
        buttonOK = new JButton();
        buttonOK.setText("OK");
        panel2.add(buttonOK,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonCancel = new JButton();
        buttonCancel.setText("Cancel");
        panel2.add(buttonCancel,
                new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel3,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
                        0, false));
        infoLabel = new JBLabel();
        panel3.add(infoLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0,
                false));
        treeScrollPane = new JScrollPane();
        panel3.add(treeScrollPane,
                new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                        new Dimension(-1, 300), null, null, 1, false));
        configTree.setOpaque(true);
        treeScrollPane.setViewportView(configTree);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }

}
