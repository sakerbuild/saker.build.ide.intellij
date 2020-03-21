package saker.build.ide.intellij.impl.properties;

import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.ui.AddEditRemovePanel;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.CommonActionsPanel;
import com.intellij.ui.DoubleClickListener;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import saker.build.ide.intellij.impl.properties.wizard.ClassPathTypeChooserWizardStep;
import saker.build.ide.intellij.impl.properties.wizard.SakerWizardDialog;
import saker.build.ide.intellij.impl.properties.wizard.SakerWizardModel;
import saker.build.ide.intellij.impl.properties.wizard.ScriptServiceEnumeratorSakerWizardPage;
import saker.build.ide.intellij.impl.ui.PropertyAttributeTreeNode;
import saker.build.ide.intellij.impl.ui.PropertyTreeNode;
import saker.build.ide.intellij.impl.ui.RootTreeNode;
import saker.build.ide.intellij.impl.ui.SakerPropertyPageAddEditRemovePanel;
import saker.build.ide.support.SakerIDESupportUtils;
import saker.build.ide.support.properties.ClassPathLocationIDEProperty;
import saker.build.ide.support.properties.ClassPathServiceEnumeratorIDEProperty;
import saker.build.ide.support.properties.IDEProjectProperties;
import saker.build.ide.support.properties.ScriptConfigurationIDEProperty;
import saker.build.ide.support.ui.wizard.AbstractSakerWizardPage;
import saker.build.ide.support.ui.wizard.ClassPathTypeChooserSakerWizardPage;
import saker.build.ide.support.ui.wizard.SakerWizardManager;
import saker.build.ide.support.ui.wizard.SakerWizardPage;
import saker.build.ide.support.ui.wizard.ScriptConfigurationSakerWizardPage;
import saker.build.ide.support.ui.wizard.ServiceEnumeratorRedirectingSakerWizardPage;
import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.build.thirdparty.saker.util.ObjectUtils;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Insets;
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
        //TODO add reset default link
        configTree.getEmptyText().clear().appendText("No script configurations defined.");

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
                //don't edit on root daemon connection node, as it opens and closes
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

        modellingExclusionsPanel.add(exclusionsEditPanel,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null,
                        new Dimension(200, 200), null, 0, false));
    }

    private void addAction(AnActionButton button) {
        SakerWizardModel model = new SakerWizardModel("Script configuration",
                configurable.getParent().getCurrentProjectProperties(),
                configurable.getParent().getProject().getProject());
        ClassPathTypeChooserSakerWizardPage cppage = model.getWizardPage(ClassPathTypeChooserSakerWizardPage.class);
        ServiceEnumeratorRedirectingSakerWizardPage serviceenumeratorpage = model
                .getWizardPage(ScriptServiceEnumeratorRedirectingSakerWizardPage.class);
        cppage.setNextPage(serviceenumeratorpage);
        serviceenumeratorpage.setNextPage(model.getWizardPage(ScriptConfigurationSakerWizardPage.class));

        ClassPathTypeChooserWizardStep cpwizardstep = new ClassPathTypeChooserWizardStep(model, cppage);
        cpwizardstep.setClassPathTypes(false, true);

        model.add(cpwizardstep);

        SakerWizardDialog dialog = new SakerWizardDialog(configurationsPanel, true, model);
        model.setDialog(dialog);
        dialog.setModal(true);

        if (dialog.showAndGet()) {
            try {
                ScriptConfigurationIDEProperty property = (ScriptConfigurationIDEProperty) ObjectUtils
                        .getOptional(cppage.finishWizard());
                ScriptPropertyTreeNode node = new ScriptPropertyTreeNode(rootTreeNode);
                node.setProperty(property);
                int idx = rootTreeNode.add(node);
                ((DefaultTreeModel) configTree.getModel()).nodesWereInserted(rootTreeNode, new int[] { idx });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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
        if (selection instanceof ScriptPropertyTreeNode) {
            propertynode = (ScriptPropertyTreeNode) selection;
        } else {
            return;
        }
        ScriptConfigurationIDEProperty editedproperty = propertynode.getProperty();
        //TODO implement
    }

    public void reset(IDEProjectProperties properties) {
        exclusionsEditPanel.setData(new ArrayList<>(properties.getScriptModellingExclusions()));

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
        configurationsPanel = new JPanel();
        configurationsPanel.setLayout(new CardLayout(0, 0));
        tabbedPane1.addTab("Configurations", configurationsPanel);
        ideModellingPanel = new JPanel();
        ideModellingPanel.setLayout(new GridLayoutManager(2, 1, new Insets(5, 5, 5, 5), -1, -1));
        tabbedPane1.addTab("IDE modelling", ideModellingPanel);
        ideModellingPanel.setBorder(
                BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Modelling exclusions"));
        final JLabel label1 = new JLabel();
        label1.setText("The build scripts matching the specified wildcards won't be part of the script modelling.");
        ideModellingPanel.add(label1,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0,
                        false));
        modellingExclusionsPanel = new JPanel();
        modellingExclusionsPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
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
            PropertyAttributeTreeNode classpath = new PropertyAttributeTreeNode(this, "Classpath",
                    SakerIDESupportUtils.classPathLocationToLabel(property.getClassPathLocation()));
            children.add(classpath);
            ClassPathServiceEnumeratorIDEProperty serviceenumeratorproperty = property.getServiceEnumerator();
            if (serviceenumeratorproperty != null) {
                PropertyAttributeTreeNode serviceenumerator = new PropertyAttributeTreeNode(this,
                        SakerIDESupportUtils.serviceEnumeratorToTitleLabel(serviceenumeratorproperty),
                        SakerIDESupportUtils.serviceEnumeratorToLabel(serviceenumeratorproperty));
                children.add(serviceenumerator);
            }
            PropertyAttributeTreeNode optionsnode = new PropertyAttributeTreeNode(this, "Script options",
                    getScriptOptionsNodeLabel(property));
            children.add(optionsnode);

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

    public static class ScriptServiceEnumeratorRedirectingSakerWizardPage extends ServiceEnumeratorRedirectingSakerWizardPage {
        public ScriptServiceEnumeratorRedirectingSakerWizardPage(SakerWizardManager<SakerWizardPage> wizardManager) {
            super(wizardManager);
        }

        @Override
        protected AbstractSakerWizardPage redirect() {
            return wizardManager.getWizardPage(ScriptServiceEnumeratorSakerWizardPage.class);
        }
    }
}
