package saker.build.ide.intellij.impl.properties;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.ui.AddEditRemovePanel;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.AnActionButtonRunnable;
import com.intellij.ui.CommonActionsPanel;
import com.intellij.ui.DoubleClickListener;
import com.intellij.ui.JBDefaultTreeCellRenderer;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.table.JBTable;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.intellij.util.IconUtil;
import org.jdesktop.swingx.JXBusyLabel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import saker.build.ide.intellij.ContributedExtensionConfiguration;
import saker.build.ide.intellij.ExtensionDisablement;
import saker.build.ide.intellij.SakerBuildPlugin;
import saker.build.ide.intellij.UserParameterContributorExtension;
import saker.build.ide.intellij.extension.params.IEnvironmentUserParameterContributor;
import saker.build.ide.intellij.extension.params.IExecutionUserParameterContributor;
import saker.build.ide.intellij.impl.IntellijSakerIDEPlugin;
import saker.build.ide.intellij.impl.IntellijSakerIDEProject;
import saker.build.ide.intellij.impl.ui.PropertyAttributeTreeNode;
import saker.build.ide.intellij.impl.ui.PropertyTreeNode;
import saker.build.ide.intellij.impl.ui.RootTreeNode;
import saker.build.ide.intellij.impl.ui.TreeModelAdapter;
import saker.build.ide.intellij.impl.ui.UIUtils;
import saker.build.ide.support.SakerIDEPlugin;
import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.build.thirdparty.saker.util.ObjectUtils;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreeSelectionModel;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;

public class UserParametersForm {
    private JTabbedPane tabbedPane;
    private JPanel rootPanel;
    private JPanel parametersPanel;
    private JBLabel parametersInfoLabel;
    private JPanel extensionsPanel;
    private JXBusyLabel contributorsCalculatingBusyLabel;

    //    private AddEditRemovePanel<UserParameterEntry> parametersEditPanel;
    private String userParameterKind = "";

    private JBTable parametersTable;
    private ToolbarDecorator parametersDecorator;
    private ParametersTableModel parametersTableModel = new ParametersTableModel();

    private Tree extensionsTree;
    private ToolbarDecorator extensionsDecorator;
    private ToolbarDecorator.ElementActionButton disableExtensionButton;
    private ToolbarDecorator.ElementActionButton enableExtensionButton;

    private RootTreeNode<ExtensionPropertyTreeNode> extensionsRootNode = new RootTreeNode<>();

    private BiFunction<Map<String, String>, List<? extends ContributedExtensionConfiguration<?>>, Map<String, String>> extensionApplier = (p, ed) -> p;

    private Project project;

    private final Object busyIndicatorAccessLock = new Object();
    private int busyCounter = 0;

    private UserParametersForm() {
        $$$setupUI$$$();

        parametersTable = new JBTable(parametersTableModel);
        DefaultTableCellRenderer genrenderer = new DefaultTableCellRenderer();
        genrenderer.setIcon(IconUtil.scale(AllIcons.Plugins.PluginLogo, parametersTable, 0.4f));
        genrenderer.setToolTipText("User parameter overridden by extension");
        DefaultTableCellRenderer defaultrenderer = new DefaultTableCellRenderer();
        parametersTable.getColumnModel().getColumn(0).setCellRenderer(new TableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                if (value instanceof GeneratedKey) {
                    return genrenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                }
                return defaultrenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
        });
        parametersDecorator = ToolbarDecorator.createDecorator(parametersTable)
                .setAddAction(new AnActionButtonRunnable() {
                    @Override
                    public void run(AnActionButton anActionButton) {
                        UserParameterEntry entry = addUserParameter();
                        if (entry != null) {
                            parametersTableModel.add(entry);
                        }
                    }
                }).setRemoveAction(new AnActionButtonRunnable() {
                    @Override
                    public void run(AnActionButton anActionButton) {
                        int[] selected = parametersTable.getSelectedRows();
                        parametersTableModel.remove(selected);
                    }
                }).setEditAction(new AnActionButtonRunnable() {
                    @Override
                    public void run(AnActionButton anActionButton) {
                        performEdit();
                    }
                }).setEditActionUpdater(e -> isEditEnabled()).setRemoveActionUpdater(e -> isRemoveEnabled());
        parametersPanel.add(parametersDecorator.createPanel());
        parametersTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                AnActionButton editbutton = parametersDecorator.getActionsPanel()
                        .getAnActionButton(CommonActionsPanel.Buttons.EDIT);
                editbutton
                        .update(AnActionEvent.createFromAnAction(editbutton, null, "none", DataContext.EMPTY_CONTEXT));
                AnActionButton removebutton = parametersDecorator.getActionsPanel()
                        .getAnActionButton(CommonActionsPanel.Buttons.REMOVE);
                removebutton.update(AnActionEvent
                        .createFromAnAction(removebutton, null, "none", DataContext.EMPTY_CONTEXT));
            }
        });

        new DoubleClickListener() {
            @Override
            protected boolean onDoubleClick(MouseEvent event) {
                performEdit();
                return true;
            }
        }.installOn(parametersTable);

        JLabel label = (JLabel) tabbedPane.getTabComponentAt(1);
        label.setIcon(IconUtil.scale(AllIcons.Plugins.PluginLogo, label, 0.4f));

        DefaultTreeModel treeModel = new DefaultTreeModel(extensionsRootNode, false);
        extensionsTree = new Tree(treeModel);
        DefaultTreeCellRenderer defaulttreecellrenderer = new JBDefaultTreeCellRenderer();
        DefaultTreeCellRenderer disabledtreecellrenderer = new JBDefaultTreeCellRenderer();
        DefaultTreeCellRenderer enabledtreecellrenderer = new JBDefaultTreeCellRenderer();

        UIUtils.setDefaultTreeCellRendererIcon(defaulttreecellrenderer, null);
        UIUtils.setDefaultTreeCellRendererIcon(enabledtreecellrenderer,
                IconUtil.scale(AllIcons.Plugins.PluginLogo, extensionsTree, 0.4f));
        UIUtils.setDefaultTreeCellRendererIcon(disabledtreecellrenderer, AllIcons.RunConfigurations.ShowIgnored);
        extensionsTree.setCellRenderer(new TreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
                    boolean leaf, int row, boolean hasFocus) {
                if (value instanceof ExtensionPropertyTreeNode) {
                    if (((ExtensionPropertyTreeNode) value).getProperty().isEnabled()) {
                        return enabledtreecellrenderer
                                .getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
                    }
                    return disabledtreecellrenderer
                            .getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
                }
                return defaulttreecellrenderer
                        .getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
            }
        });
        extensionsTree.setRootVisible(false);
        extensionsTree.getEmptyText().clear().appendText("No user parameter contributor extensions installed.");
        extensionsTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        extensionsDecorator = ToolbarDecorator.createDecorator(extensionsTree).disableUpDownActions()
                .setToolbarPosition(ActionToolbarPosition.RIGHT);

        disableExtensionButton = new ToolbarDecorator.ElementActionButton(null, "Disable extension",
                disabledtreecellrenderer.getIcon()) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                Object sel = extensionsTree.getLastSelectedPathComponent();
                if (!(sel instanceof ExtensionPropertyTreeNode)) {
                    return;
                }
                ExtensionPropertyTreeNode propnode = (ExtensionPropertyTreeNode) sel;
                propnode.setProperty(propnode.getProperty().setEnabled(false));
                ((DefaultTreeModel) extensionsTree.getModel()).nodeStructureChanged(propnode);
                updateExtensionEnablementButtons();
                parametersTableModel.updateExtensions();
            }
        };
        enableExtensionButton = new ToolbarDecorator.ElementActionButton(null, "Enable extension",
                AllIcons.Actions.SetDefault) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                Object sel = extensionsTree.getLastSelectedPathComponent();
                if (!(sel instanceof ExtensionPropertyTreeNode)) {
                    return;
                }
                ExtensionPropertyTreeNode propnode = (ExtensionPropertyTreeNode) sel;
                propnode.setProperty(propnode.getProperty().setEnabled(true));
                ((DefaultTreeModel) extensionsTree.getModel()).nodeStructureChanged(propnode);
                updateExtensionEnablementButtons();
                parametersTableModel.updateExtensions();
            }
        };
        extensionsTree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                updateExtensionEnablementButtons();
            }
        });
        enableExtensionButton.setEnabled(false);
        disableExtensionButton.setEnabled(false);
        extensionsDecorator.addExtraAction(enableExtensionButton);
        extensionsDecorator.addExtraAction(disableExtensionButton);
        extensionsPanel.add(extensionsDecorator.createPanel());
    }

    private void updateExtensionEnablementButtons() {
        Object sel = extensionsTree.getLastSelectedPathComponent();
        if (!(sel instanceof ExtensionPropertyTreeNode)) {
            disableExtensionButton.setEnabled(false);
            enableExtensionButton.setEnabled(false);
            return;
        }
        boolean extensionenabled = ((ExtensionPropertyTreeNode) sel).getProperty().isEnabled();
        disableExtensionButton.setEnabled(extensionenabled);
        enableExtensionButton.setEnabled(!extensionenabled);
    }

    private boolean isRemoveEnabled() {
        if (parametersTable.getSelectionModel().isSelectionEmpty()) {
            return false;
        }
        if (parametersTableModel.getEntryAtRow(parametersTable.getSelectedRow()).extensionModified) {
            return false;
        }
        return true;
    }

    private boolean isEditEnabled() {
        if (parametersTable.getSelectionModel().isSelectionEmpty()) {
            return false;
        }
        if (parametersTable.getSelectedRows().length != 1) {
            return false;
        }
        if (parametersTableModel.getEntryAtRow(parametersTable.getSelectedRow()).extensionModified) {
            return false;
        }
        return true;
    }

    private void performEdit() {
        int row = parametersTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        UserParameterEntry entry = parametersTableModel.getEntryAtRow(row);
        if (entry.extensionModified) {
            return;
        }
        UserParameterEntry edited = editUserParameter(entry);
        if (edited != null) {
            parametersTableModel.setDataAtIndex(row, edited);
        }
    }

    public UserParametersForm(ExecutionUserParametersConfigurable configurable) {
        this();
        this.project = configurable.getParent().getProject().getProject();
        parametersTableModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                configurable.getParent().getBuilder().setUserParameters(getCurrentValues());
            }
        });
        extensionsTree.getModel().addTreeModelListener(new TreeModelAdapter() {
            @Override
            protected void update(TreeModelEvent e) {
                configurable.getParent().setExtensionDisablements(getCurrentExtensionDisablements());
            }
        });

        IntellijSakerIDEProject project = configurable.getParent().getProject();
        List<ContributedExtensionConfiguration<IExecutionUserParameterContributor>> extensions = project
                .getExecutionParameterContributors();
        extensionApplier = (params, currentextensions) -> {
            return project.getUserParametersWithContributors(params,
                    (List<? extends ContributedExtensionConfiguration<? extends IExecutionUserParameterContributor>>) currentextensions,
                    new EmptyProgressIndicator());
        };

        setExtensionsTreeContents(extensions);
    }

    public UserParametersForm(EnvironmentUserParametersConfigurable configurable) {
        this();
        parametersTableModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                configurable.getParent().getBuilder().setUserParameters(getCurrentValues());
            }
        });
        extensionsTree.getModel().addTreeModelListener(new TreeModelAdapter() {
            @Override
            protected void update(TreeModelEvent e) {
                configurable.getParent().setExtensionDisablements(getCurrentExtensionDisablements());
            }
        });

        IntellijSakerIDEPlugin plugin = configurable.getParent().getPlugin();
        List<ContributedExtensionConfiguration<IEnvironmentUserParameterContributor>> extensions = plugin
                .getEnvironmentParameterContributors();
        extensionApplier = (params, currentextensions) -> {
            return plugin.getUserParametersWithContributors(params,
                    (List<? extends ContributedExtensionConfiguration<? extends IEnvironmentUserParameterContributor>>) currentextensions,
                    new EmptyProgressIndicator());
        };

        setExtensionsTreeContents(extensions);
    }

    private static class GeneratedKey {
        private final UserParameterEntry entry;

        public GeneratedKey(UserParameterEntry entry) {
            this.entry = entry;
        }

        @Override
        public String toString() {
            return entry.getKey();
        }
    }

    private class ParametersTableModel extends AbstractTableModel {

        private List<UserParameterEntry> data = new ArrayList<>();
        private List<UserParameterEntry> generatedParams = new ArrayList<>();

        public List<UserParameterEntry> getData() {
            return data;
        }

        private void recalculateGeneratedParams(List<UserParameterEntry> input) {
            List<ContributedExtensionConfiguration<?>> extensions = getCurrentExtensions();
            if (ObjectUtils.isNullOrEmpty(extensions)) {
                return;
            }
            List<UserParameterEntry> datainput = ImmutableUtils.makeImmutableList(input);
            Set<Map.Entry<String, String>> entries = new LinkedHashSet<>();
            for (UserParameterEntry entry : input) {
                entries.add(entry.toEntry());
            }
            Map<String, String> userentries = SakerIDEPlugin.entrySetToMap(entries);

            synchronized (busyIndicatorAccessLock) {
                ++busyCounter;
                contributorsCalculatingBusyLabel.setBusy(true);
                contributorsCalculatingBusyLabel.setVisible(true);
            }

            ProgressManager progmanager = ProgressManager.getInstance();
            progmanager.run(new Task.Backgroundable(project, "Calculating user parameters", true) {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    try {
                        Map<String, String> currententries = extensionApplier.apply(userentries, extensions);
                        List<UserParameterEntry> result = new ArrayList<>();
                        for (Map.Entry<String, String> entry : currententries.entrySet()) {
                            if (Objects.equals(userentries.get(entry.getKey()), entry.getValue())) {
                                //the contributors haven't changed the given entry.
                                continue;
                            }
                            result.add(new UserParameterEntry(entry.getKey(), entry.getValue(), true));
                        }
                        SwingUtilities.invokeLater(() -> {
                            if (!Objects.equals(ParametersTableModel.this.data, datainput) || !extensions
                                    .equals(getCurrentExtensions())) {
                                //calculated output not applicable, model was modified
                                return;
                            }
                            ParametersTableModel.this.generatedParams = result;
                            int size = input.size();
                            fireTableRowsInserted(size, size);
                        });
                    } finally {
                        SwingUtilities.invokeLater(() -> {
                            synchronized (busyIndicatorAccessLock) {
                                boolean busy = --busyCounter > 0;
                                contributorsCalculatingBusyLabel.setBusy(busy);
                                contributorsCalculatingBusyLabel.setVisible(busy);
                            }
                        });
                    }
                }
            });
        }

        public void setData(List<UserParameterEntry> data) {
            this.data = data;
            this.generatedParams = Collections.emptyList();
            fireTableDataChanged();
            recalculateGeneratedParams(data);
        }

        @Override
        public int getRowCount() {
            return data.size() + generatedParams.size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            UserParameterEntry elem = getEntryAtRow(rowIndex);
            if (columnIndex == 0) {
                if (elem.extensionModified) {
                    return new GeneratedKey(elem);
                }
                return elem.getKey();
            }
            return elem.getValue();
        }

        @Override
        public String getColumnName(int column) {
            return COLUMN_NAMES[column];
        }

        public UserParameterEntry getEntryAtRow(int rowIndex) {
            UserParameterEntry elem;
            int size = data.size();
            if (rowIndex < size) {
                elem = data.get(rowIndex);
            } else {
                elem = generatedParams.get(rowIndex - size);
            }
            return elem;
        }

        public void add(UserParameterEntry entry) {
            this.data.add(entry);
            int size = data.size();
            clearNotifyGeneratedParams(size);
            fireTableRowsInserted(size, size);
            recalculateGeneratedParams(data);
        }

        public void remove(int[] indices) {
            if (ObjectUtils.isNullOrEmpty(indices)) {
                return;
            }
            indices = indices.clone();

            Arrays.sort(indices);
            int size = data.size();
            if (indices[0] >= size) {
                return;
            }
            clearNotifyGeneratedParams(size);

            for (int i = indices.length - 1; i >= 0; i--) {
                int idx = indices[i];
                data.remove(idx);
            }
            recalculateGeneratedParams(data);
        }

        public void setDataAtIndex(int idx, UserParameterEntry edited) {
            int size = data.size();
            clearNotifyGeneratedParams(size);
            data.set(idx, edited);
            fireTableRowsUpdated(idx, idx);
            recalculateGeneratedParams(data);
        }

        private void clearNotifyGeneratedParams(int size) {
            if (!generatedParams.isEmpty()) {
                int gensize = generatedParams.size();
                generatedParams = Collections.emptyList();
                fireTableRowsDeleted(size, size + gensize);
            }
        }

        public void updateExtensions() {
            int size = data.size();
            clearNotifyGeneratedParams(size);
            recalculateGeneratedParams(data);
        }
    }

    private void setExtensionsTreeContents(List<? extends ContributedExtensionConfiguration<?>> extensions) {
        extensionsRootNode.clear();

        for (ContributedExtensionConfiguration<?> ext : extensions) {
            ExtensionPropertyTreeNode node = new ExtensionPropertyTreeNode(extensionsRootNode);
            node.setProperty(ext);
            extensionsRootNode.add(node);
        }
        ((DefaultTreeModel) extensionsTree.getModel()).reload(extensionsRootNode);
    }

    private List<ContributedExtensionConfiguration<?>> getCurrentExtensions() {
        List<ContributedExtensionConfiguration<?>> result = new ArrayList<>();
        for (ExtensionPropertyTreeNode n : extensionsRootNode.getNodes()) {
            result.add(n.getProperty());
        }

        return result;
    }

    public Set<ExtensionDisablement> getCurrentExtensionDisablements() {
        return SakerBuildPlugin.getExtensionDisablements(getCurrentExtensions());
    }

    private List<UserParameterEntry> getCurrentExtensionEntries() {
        Set<Map.Entry<String, String>> values = getCurrentValues();
        Map<String, String> userentries = SakerIDEPlugin.entrySetToMap(values);
        Map<String, String> currententries = extensionApplier.apply(userentries, getCurrentExtensions());
        List<UserParameterEntry> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : currententries.entrySet()) {
            if (Objects.equals(userentries.get(entry.getKey()), entry.getValue())) {
                //the contributors haven't changed the given entry.
                continue;
            }
            result.add(new UserParameterEntry(entry.getKey(), entry.getValue(), true));
        }
        return result;
    }

    @NotNull
    public Set<Map.Entry<String, String>> getCurrentValues() {
        List<UserParameterEntry> data = parametersTableModel.getData();
        LinkedHashSet<Map.Entry<String, String>> result = new LinkedHashSet<>();
        for (UserParameterEntry entry : data) {
            if (!entry.extensionModified) {
                result.add(entry.toEntry());
            }
        }

        return result;
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
        parametersTable.getEmptyText().setText("No " + userParameterKind + " user parameters defined.");
    }

    public void setUserParameters(Set<? extends Map.Entry<String, String>> parameters,
            Set<ExtensionDisablement> extensionDisablements) {
        ArrayList<UserParameterEntry> data = ObjectUtils.newArrayList();
        if (parameters != null) {
            for (Map.Entry<String, String> entry : parameters) {
                data.add(UserParameterEntry.valueOf(entry));
            }
        }

        List<ContributedExtensionConfiguration<Object>> nextensions = SakerBuildPlugin
                .applyExtensionDisablements(getCurrentExtensions(), extensionDisablements);
        setExtensionsTreeContents(nextensions);
        parametersTableModel.setData(data);
    }

    public JPanel getParametersPanel() {
        return parametersPanel;
    }

    protected UserParameterEntry editUserParameter(UserParameterEntry entry) {
        if (entry == null || entry.extensionModified) {
            return null;
        }
        UserParameterEditorDialog dialog = new UserParameterEditorDialog(
                "Edit " + userParameterKind + " user parameter", getParametersPanel());
        Set<String> existingkeys = getKeys();
        existingkeys.remove(entry.getKey());
        dialog.setExistingKeys(existingkeys);
        initDialogMessages(dialog);
        dialog.setEntry(entry.toEntry());
        dialog.setVisible(true);
        return UserParameterEntry.valueOf(dialog.getEntry());
    }

    protected UserParameterEntry addUserParameter() {
        UserParameterEditorDialog dialog = new UserParameterEditorDialog(
                "Add " + userParameterKind + " user " + "parameter", getParametersPanel());
        dialog.setExistingKeys(getKeys());
        initDialogMessages(dialog);
        dialog.setVisible(true);
        return UserParameterEntry.valueOf(dialog.getEntry());
    }

    private Set<String> getKeys() {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (UserParameterEntry entry : parametersTableModel.getData()) {
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
        tabbedPane.setVisible(true);
        rootPanel.add(tabbedPane,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null,
                        new Dimension(200, 200), null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane.addTab("Parameters", panel1);
        parametersInfoLabel = new JBLabel();
        panel1.add(parametersInfoLabel,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(-1, 30),
                        new Dimension(-1, 30), null, 0, false));
        parametersPanel = new JPanel();
        parametersPanel.setLayout(new CardLayout(0, 0));
        panel1.add(parametersPanel,
                new GridConstraints(1, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
                        0, false));
        contributorsCalculatingBusyLabel = new JXBusyLabel();
        contributorsCalculatingBusyLabel.setToolTipText("Calculating user parameters...");
        contributorsCalculatingBusyLabel.setVisible(false);
        panel1.add(contributorsCalculatingBusyLabel,
                new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null,
                        new Dimension(15, 15), null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1,
                new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane.addTab("Extensions", panel2);
        final JBLabel jBLabel1 = new JBLabel();
        jBLabel1.setText("The following extension contribute to user parameters:");
        panel2.add(jBLabel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(-1, 30),
                new Dimension(-1, 30), null, 0, false));
        extensionsPanel = new JPanel();
        extensionsPanel.setLayout(new CardLayout(0, 0));
        panel2.add(extensionsPanel,
                new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
                        0, false));
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

    public static class KeyValueTableModel extends AddEditRemovePanel.TableModel<UserParameterEntry> {
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
        public Object getField(UserParameterEntry o, int columnIndex) {
            return columnIndex == 0 ? o.getKey() : o.getValue();
        }
    }

    public static class UserParameterEntry {
        public final String key;
        public final String value;

        public final boolean extensionModified;

        public UserParameterEntry(String key, String value) {
            this.key = key;
            this.value = value;
            this.extensionModified = false;
        }

        public UserParameterEntry(String key, String value, boolean extensionModified) {
            this.key = key;
            this.value = value;
            this.extensionModified = extensionModified;
        }

        public static UserParameterEntry valueOf(Map.Entry<String, String> entry) {
            if (entry == null) {
                return null;
            }
            return new UserParameterEntry(entry.getKey(), entry.getValue());
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }

        public Map.Entry<String, String> toEntry() {
            return ImmutableUtils.makeImmutableMapEntry(key, value);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            UserParameterEntry that = (UserParameterEntry) o;
            return extensionModified == that.extensionModified && Objects.equals(key, that.key) && Objects
                    .equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, value, extensionModified);
        }
    }

    private static class ExtensionPropertyTreeNode extends PropertyTreeNode<ContributedExtensionConfiguration<?>> {
        public ExtensionPropertyTreeNode(TreeNode parent) {
            super(parent);
        }

        @Override
        public void setProperty(ContributedExtensionConfiguration<?> property) {
            this.children = new ArrayList<>();

            UserParameterContributorExtension ext = property.getContributedExtension();
            String id = ext.getId();
            PluginId pluginId = ext.getPluginId();
            String implementationClass = ext.getImplementationClass();
            children.add(new PropertyAttributeTreeNode(this, "ID", id));
            children.add(new PropertyAttributeTreeNode(this, "Plugin ID", Objects.toString(pluginId, null)));
            children.add(new PropertyAttributeTreeNode(this, "Class", implementationClass));

            super.setProperty(property);
        }

        @Override
        public String toString() {
            final String result;
            UserParameterContributorExtension ext = property.getContributedExtension();
            String displayname = ext.getDisplayName();
            if (!ObjectUtils.isNullOrEmpty(displayname)) {
                result = displayname;
            } else {
                String id = ext.getId();
                if (!ObjectUtils.isNullOrEmpty(id)) {
                    result = id;
                } else {
                    String implclass = ext.getImplementationClass();
                    if (!ObjectUtils.isNullOrEmpty(implclass)) {
                        result = implclass;
                    } else {
                        result = ext.toString();
                    }
                }
            }
            if (!property.isEnabled()) {
                return result + " (disabled)";
            }
            return result;
        }
    }
}
