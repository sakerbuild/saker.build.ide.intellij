package saker.build.ide.intellij.impl.properties;

import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.ui.AddEditRemovePanel;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.CommonActionsPanel;
import com.intellij.ui.DoubleClickListener;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.ui.tree.TreeModelAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import saker.build.ide.intellij.impl.properties.wizard.ClassPathTypeChooserWizardForm;
import saker.build.ide.intellij.impl.properties.wizard.SakerWizardDialog;
import saker.build.ide.intellij.impl.properties.wizard.SakerWizardModel;
import saker.build.ide.intellij.impl.properties.wizard.SakerWizardPageWizardStep;
import saker.build.ide.intellij.impl.properties.wizard.ServiceEnumeratorWizardForm;
import saker.build.ide.intellij.impl.properties.wizard.WizardStepFactory;
import saker.build.ide.intellij.impl.ui.PropertyAttributeTreeNode;
import saker.build.ide.intellij.impl.ui.PropertyTreeNode;
import saker.build.ide.intellij.impl.ui.RootTreeNode;
import saker.build.ide.intellij.impl.ui.SakerPropertyPageAddEditRemovePanel;
import saker.build.ide.support.SakerIDESupportUtils;
import saker.build.ide.support.properties.ClassPathLocationIDEProperty;
import saker.build.ide.support.properties.ClassPathServiceEnumeratorIDEProperty;
import saker.build.ide.support.properties.IDEProjectProperties;
import saker.build.ide.support.properties.RepositoryIDEProperty;
import saker.build.ide.support.properties.ScriptConfigurationIDEProperty;
import saker.build.ide.support.properties.SimpleIDEProjectProperties;
import saker.build.ide.support.ui.wizard.BaseSakerWizardManager;
import saker.build.ide.support.ui.wizard.SakerWizardPage;
import saker.build.scripting.ScriptAccessProvider;
import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.build.thirdparty.saker.util.ObjectUtils;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class ScriptConfigurationForm {
    private JPanel rootPanel;
    private JTabbedPane tabbedPane1;
    private JPanel configurationsPanel;
    private JPanel ideModellingPanel;
    private JPanel modellingExclusionsPanel;

    private final ScriptConfigurationConfigurable configurable;

    private Tree configTree;
    private RootTreeNode<ScriptPropertyTreeNode> rootTreeNode = new RootTreeNode<>();
    private AddEditRemovePanel<String> exclusionsEditPanel;
    private ToolbarDecorator decorator;

    public ScriptConfigurationForm(ScriptConfigurationConfigurable configurable) {
        this.configurable = configurable;
        DefaultTreeModel treemodel = new DefaultTreeModel(rootTreeNode, false);
        configTree = new Tree(treemodel);
        configTree.setRootVisible(false);
        configTree.getEmptyText().clear().appendText("No script configurations defined.");

        ideModellingPanel.setBorder(IdeBorderFactory.createTitledBorder("Modelling exclusions", false));

        decorator = ToolbarDecorator.createDecorator(configTree);
        configurationsPanel.add(decorator.disableUpDownActions().setToolbarPosition(ActionToolbarPosition.RIGHT)
                .setAddAction(this::addAction).setRemoveAction(this::removeAction).setEditAction(this::editAction)
                .createPanel());

        configTree.addTreeSelectionListener(e -> {
            decorator.getActionsPanel()
                    .setEnabled(CommonActionsPanel.Buttons.EDIT, configTree.getSelectionPath() != null);
        });
        new DoubleClickListener() {
            @Override
            protected boolean onDoubleClick(MouseEvent event) {
                Object selection = configTree.getLastSelectedPathComponent();
                if (selection instanceof PropertyAttributeTreeNode) {
                    performEditAction();
                    return true;
                }
                //don't edit on root node, as it opens and closes
                return false;
            }
        }.installOn(configTree);
        decorator.getActionsPanel().setEnabled(CommonActionsPanel.Buttons.EDIT, configTree.getSelectionPath() != null);

        exclusionsEditPanel = new SakerPropertyPageAddEditRemovePanel<String>(new ExclusionsTableModel()) {
            @Nullable
            @Override
            protected String addItem() {
                ScriptModellingExclusionDialog dialog = new ScriptModellingExclusionDialog(
                        "Add Script Modelling Exclusion", this);
                dialog.setVisible(true);
                return dialog.getExclusionWildcard();
            }

            @Nullable
            @Override
            protected String editItem(String o) {
                ScriptModellingExclusionDialog dialog = new ScriptModellingExclusionDialog(
                        "Edit Script Modelling Exclusion", this);
                dialog.setEdit(o);
                dialog.setVisible(true);
                return dialog.getExclusionWildcard();
            }
        };
        exclusionsEditPanel.getEmptyText().clear().appendText("No script modelling exclusions defined.");
        exclusionsEditPanel.getTable().getModel().addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                configurable.getParent().getBuilder().setScriptModellingExclusions(getExclusionWildcards());
            }
        });
        configTree.getModel().addTreeModelListener(new TreeModelAdapter() {
            @Override
            protected void process(@NotNull TreeModelEvent event, @NotNull EventType type) {
                configurable.getParent().getBuilder().setScriptConfigurations(getScriptConfiguration());
            }
        });

        modellingExclusionsPanel.add(exclusionsEditPanel);
    }

    private ScriptConfigurationIDEProperty showWizard(ScriptConfigurationIDEProperty editproperty,
            String... editinitialconfigids) {
        SakerWizardModel model = new SakerWizardModel(
                editproperty == null ? "New Script Configuration" : "Edit Script Configuration",
                configurable.getParent().getCurrentProjectProperties(),
                configurable.getParent().getProject().getProject());

        BaseSakerWizardManager<SakerWizardPage> wizardmanager = model.getWizardManager();
        wizardmanager.setConfiguration(ClassPathTypeChooserWizardForm.WIZARD_CONFIGURATION_WITH_SCRIPT_CLASSPATH, true);
        wizardmanager
                .setConfiguration(ServiceEnumeratorWizardForm.WIZARD_CONFIGURATION_DEFAULT_SERVICE_LOADER_CLASS_NAME,
                        ScriptAccessProvider.class.getName());

        if (editproperty != null) {
            SakerIDESupportUtils.editScriptConfigurationWithWizardManager(wizardmanager, editproperty);
        }

        SakerWizardPage startwizardpage = SakerIDESupportUtils.createScriptConfigurationWizardSteps(wizardmanager);
        SakerWizardPage initialpage = null;
        if (editinitialconfigids != null) {
            initialpage = SakerIDESupportUtils
                    .findEditorInitialPageWithPriorities(startwizardpage, editinitialconfigids);
        }
        SakerWizardPageWizardStep<?> startstep = WizardStepFactory.create(model, startwizardpage, null);
        model.add(startstep);
        model.navigateToWizardPageOrEnd(initialpage);

        SakerWizardDialog dialog = new SakerWizardDialog(rootPanel, true, model);

        if (dialog.showAndGet()) {
            try {
                return (ScriptConfigurationIDEProperty) ObjectUtils.getOptional(startwizardpage.finishWizard(null));
            } catch (Exception e) {
                configurable.getParent().getProject().displayException(e);
            }
        }
        return null;
    }

    private void addAction(AnActionButton button) {
        ScriptConfigurationIDEProperty property = showWizard(null, (String[]) null);
        if (property != null) {
            addProperty(property);
        }
    }

    private void addProperty(ScriptConfigurationIDEProperty property) {
        ScriptPropertyTreeNode node = new ScriptPropertyTreeNode(rootTreeNode);
        node.setProperty(property);
        int idx = rootTreeNode.add(node);
        ((DefaultTreeModel) configTree.getModel()).nodesWereInserted(rootTreeNode, new int[] { idx });
    }

    private void removeAction(AnActionButton button) {
        TreePath[] selection = configTree.getSelectionPaths();
        if (selection == null) {
            return;
        }
        Set<ScriptPropertyTreeNode> nodestoremove = new HashSet<>();
        for (TreePath selpath : selection) {
            Object[] path = selpath.getPath();
            if (path.length == 0) {
                continue;
            }
            if (path[path.length - 1] instanceof ScriptPropertyTreeNode) {
                nodestoremove.add((ScriptPropertyTreeNode) path[path.length - 1]);
            }
        }
        if (!nodestoremove.isEmpty()) {
            for (ScriptPropertyTreeNode n : nodestoremove) {
                int idx = rootTreeNode.getIndex(n);
                rootTreeNode.remove(n);
                ((DefaultTreeModel) configTree.getModel())
                        .nodesWereRemoved(rootTreeNode, new int[] { idx }, new Object[] { n });
            }
        }
    }

    private void editAction(AnActionButton button) {
        performEditAction();
    }

    private void performEditAction() {
        Object selection = configTree.getLastSelectedPathComponent();
        ScriptPropertyTreeNode propertynode;
        String[] editinitialconfigid = null;
        if (selection instanceof ScriptPropertyTreeNode) {
            propertynode = (ScriptPropertyTreeNode) selection;
        } else if (selection instanceof PropertyAttributeTreeNode) {
            propertynode = (ScriptPropertyTreeNode) ((PropertyAttributeTreeNode) selection).getParent();
            Object userdata = ((PropertyAttributeTreeNode) selection).getUserData();
            if (userdata instanceof String[]) {
                editinitialconfigid = (String[]) userdata;
            } else if (userdata instanceof String) {
                editinitialconfigid = new String[] { (String) userdata };
            }
        } else {
            return;
        }
        ScriptConfigurationIDEProperty editedproperty = propertynode.getProperty();
        ScriptConfigurationIDEProperty property = showWizard(editedproperty, editinitialconfigid);
        if (property != null) {
            propertynode.setProperty(property);
            DefaultTreeModel model = (DefaultTreeModel) configTree.getModel();
            model.nodeStructureChanged(propertynode);
        }
    }

    public void reset() {
        IDEProjectProperties properties = configurable.getParent().getProperties();
        exclusionsEditPanel.setData(new ArrayList<>(properties.getScriptModellingExclusions()));

        rootTreeNode.clear();
        for (ScriptConfigurationIDEProperty sc : properties.getScriptConfigurations()) {
            ScriptPropertyTreeNode node = new ScriptPropertyTreeNode(rootTreeNode);
            node.setProperty(sc);
            rootTreeNode.add(node);
        }
        ((DefaultTreeModel) configTree.getModel()).reload(rootTreeNode);
    }

    public Set<String> getExclusionWildcards() {
        return ImmutableUtils.makeImmutableNavigableSet(exclusionsEditPanel.getData());
    }

    public Set<ScriptConfigurationIDEProperty> getScriptConfiguration() {
        LinkedHashSet<ScriptConfigurationIDEProperty> result = new LinkedHashSet<>();
        for (ScriptPropertyTreeNode n : rootTreeNode.getNodes()) {
            result.add(n.getProperty());
        }
        return result;
    }

    public JPanel getRootPanel() {
        return rootPanel;
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
        rootPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane1 = new JTabbedPane();
        rootPanel.add(tabbedPane1,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null,
                        new Dimension(200, 200), null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(2, 1, new Insets(5, 5, 5, 5), -1, -1));
        tabbedPane1.addTab("Configurations", panel1);
        final JLabel label1 = new JLabel();
        label1.setText("The following language definitions are applied to the build scripts.");
        panel1.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        configurationsPanel = new JPanel();
        configurationsPanel.setLayout(new CardLayout(0, 0));
        panel1.add(configurationsPanel,
                new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
                        0, false));
        ideModellingPanel = new JPanel();
        ideModellingPanel.setLayout(new GridLayoutManager(2, 1, new Insets(5, 5, 5, 5), -1, -1));
        tabbedPane1.addTab("IDE modelling", ideModellingPanel);
        final JLabel label2 = new JLabel();
        label2.setText("The build scripts matching the specified wildcards won't be part of the script modelling.");
        ideModellingPanel.add(label2,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0,
                        false));
        modellingExclusionsPanel = new JPanel();
        modellingExclusionsPanel.setLayout(new CardLayout(0, 0));
        ideModellingPanel.add(modellingExclusionsPanel,
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

    private static final String[] EXCLUSIONS_COLUMN_NAMES = { "Exclusion wildcard" };

    private static class ExclusionsTableModel extends AddEditRemovePanel.TableModel<String> {
        @Override
        public int getColumnCount() {
            return EXCLUSIONS_COLUMN_NAMES.length;
        }

        @Nullable
        @Override
        public String getColumnName(int columnIndex) {
            return EXCLUSIONS_COLUMN_NAMES[columnIndex];
        }

        @Override
        public Object getField(String o, int columnIndex) {
            return o;
        }
    }

    @NotNull
    private static String getScriptOptionsNodeLabel(ScriptConfigurationIDEProperty property) {
        String optionsstr;
        Set<? extends Map.Entry<String, String>> scriptoptions = property.getScriptOptions();
        if (ObjectUtils.isNullOrEmpty(scriptoptions)) {
            optionsstr = "No options defined.";
        } else {
            int size = scriptoptions.size();
            switch (size) {
                case 0: {
                    optionsstr = "No options defined.";
                }
                case 1: {
                    optionsstr = "1 option defined.";
                }
                default: {
                    optionsstr = size + " options defined.";
                }
            }
        }
        return optionsstr;
    }

    private static final class ScriptPropertyTreeNode extends PropertyTreeNode<ScriptConfigurationIDEProperty> {
        public ScriptPropertyTreeNode(TreeNode parent) {
            super(parent);
        }

        @Override
        public void setProperty(ScriptConfigurationIDEProperty property) {
            this.children = new ArrayList<>();

            PropertyAttributeTreeNode affected = new PropertyAttributeTreeNode(this, "Affected scripts",
                    property.getScriptsWildcard());
            children.add(affected);
            affected.setUserData(SakerIDESupportUtils.WIZARDPAGE_CONFIGURATION_EDIT_INITIAL_SCRIPTCONFIG);

            PropertyAttributeTreeNode classpath = new PropertyAttributeTreeNode(this, "Classpath",
                    SakerIDESupportUtils.classPathLocationToLabel(property.getClassPathLocation()));
            children.add(classpath);
            classpath.setUserData(SakerIDESupportUtils.WIZARDPAGE_CONFIGURATION_EDIT_INITIAL_CLASSPATH);

            ClassPathServiceEnumeratorIDEProperty serviceenumeratorproperty = property.getServiceEnumerator();
            if (serviceenumeratorproperty != null) {
                PropertyAttributeTreeNode serviceenumerator = new PropertyAttributeTreeNode(this,
                        SakerIDESupportUtils.serviceEnumeratorToTitleLabel(serviceenumeratorproperty),
                        SakerIDESupportUtils.serviceEnumeratorToLabel(serviceenumeratorproperty));
                children.add(serviceenumerator);
                serviceenumerator.setUserData(
                        new String[] { SakerIDESupportUtils.WIZARDPAGE_CONFIGURATION_EDIT_INITIAL_SERVICE,
                                SakerIDESupportUtils.WIZARDPAGE_CONFIGURATION_EDIT_INITIAL_CLASSPATH });
            }

            PropertyAttributeTreeNode optionsnode = new PropertyAttributeTreeNode(this, "Script options",
                    getScriptOptionsNodeLabel(property));
            children.add(optionsnode);
            optionsnode.setUserData(SakerIDESupportUtils.WIZARDPAGE_CONFIGURATION_EDIT_INITIAL_SCRIPTCONFIG);

            super.setProperty(property);
        }

        @Override
        public String toString() {
            ClassPathLocationIDEProperty cplocation = property.getClassPathLocation();
            StringBuilder sb = new StringBuilder();
            sb.append(ObjectUtils.nullDefault(property.getScriptsWildcard(), ""));
            if (cplocation != null) {
                sb.append(" - ");
                sb.append(SakerIDESupportUtils.classPathLocationToLabel(cplocation));
            }
            return sb.toString();
        }
    }

}
