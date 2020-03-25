package saker.build.ide.intellij.impl.properties;

import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.CommonActionsPanel;
import com.intellij.ui.DoubleClickListener;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import saker.build.ide.intellij.impl.properties.wizard.ClassPathTypeChooserWizardForm;
import saker.build.ide.intellij.impl.properties.wizard.SakerWizardDialog;
import saker.build.ide.intellij.impl.properties.wizard.SakerWizardModel;
import saker.build.ide.intellij.impl.properties.wizard.SakerWizardPageWizardStep;
import saker.build.ide.intellij.impl.properties.wizard.ServiceEnumeratorWizardForm;
import saker.build.ide.intellij.impl.properties.wizard.WizardStepFactory;
import saker.build.ide.intellij.impl.ui.PropertyAttributeTreeNode;
import saker.build.ide.intellij.impl.ui.PropertyTreeNode;
import saker.build.ide.intellij.impl.ui.RootTreeNode;
import saker.build.ide.support.SakerIDESupportUtils;
import saker.build.ide.support.properties.ClassPathServiceEnumeratorIDEProperty;
import saker.build.ide.support.properties.IDEProjectProperties;
import saker.build.ide.support.properties.RepositoryIDEProperty;
import saker.build.ide.support.ui.wizard.BaseSakerWizardManager;
import saker.build.ide.support.ui.wizard.SakerWizardPage;
import saker.build.runtime.repository.SakerRepositoryFactory;
import saker.build.thirdparty.saker.util.ObjectUtils;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.CardLayout;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class RepositoryConfigurationForm {
    private JPanel rootPanel;
    private JPanel configurationsPanel;

    private final TaskRepositoriesConfigurable configurable;

    private Tree configTree;
    private RootTreeNode<RepositoryPropertyTreeNode> rootTreeNode = new RootTreeNode<>();
    private ToolbarDecorator decorator;

    public RepositoryConfigurationForm(TaskRepositoriesConfigurable configurable) {
        this.configurable = configurable;
        DefaultTreeModel treemodel = new DefaultTreeModel(rootTreeNode, false);
        configTree = new Tree(treemodel);
        configTree.setRootVisible(false);
        //TODO add reset default link
        configTree.getEmptyText().clear().appendText("No repositories defined.");

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
    }

    public void reset(IDEProjectProperties properties) {
        for (RepositoryIDEProperty sc : properties.getRepositories()) {
            RepositoryPropertyTreeNode node = new RepositoryPropertyTreeNode(rootTreeNode);
            node.setProperty(sc);
            rootTreeNode.add(node);
        }
        ((DefaultTreeModel) configTree.getModel()).reload(rootTreeNode);
    }

    public Set<RepositoryIDEProperty> getRepositories() {
        LinkedHashSet<RepositoryIDEProperty> result = new LinkedHashSet<>();
        for (RepositoryPropertyTreeNode n : rootTreeNode.getNodes()) {
            result.add(n.getProperty());
        }
        return result;
    }

    private RepositoryIDEProperty showWizard(RepositoryIDEProperty editproperty, String... editinitialconfigids) {
        SakerWizardModel model = new SakerWizardModel(
                editproperty == null ? "New Repository Configuration" : "Edit Repository Configuration",
                configurable.getParent().getCurrentProjectProperties(),
                configurable.getParent().getProject().getProject());

        BaseSakerWizardManager<SakerWizardPage> wizardmanager = model.getWizardManager();
        wizardmanager
                .setConfiguration(ClassPathTypeChooserWizardForm.WIZARD_CONFIGURATION_WITH_REPOSITORY_CLASSPATH, true);
        wizardmanager
                .setConfiguration(ServiceEnumeratorWizardForm.WIZARD_CONFIGURATION_DEFAULT_SERVICE_LOADER_CLASS_NAME,
                        SakerRepositoryFactory.class.getName());

        if (editproperty != null) {
            SakerIDESupportUtils.editRepositoryConfigurationWithWizardManager(wizardmanager, editproperty);
        }

        SakerWizardPage startwizardpage = SakerIDESupportUtils.createRepositoryConfigurationWizardSteps(wizardmanager);
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
                return (RepositoryIDEProperty) ObjectUtils.getOptional(startwizardpage.finishWizard(null));
            } catch (Exception e) {
                configurable.getParent().getProject().displayException(e);
            }
        }
        return null;
    }

    private void addAction(AnActionButton anActionButton) {
        RepositoryIDEProperty property = showWizard(null);
        if (property != null) {
            RepositoryPropertyTreeNode node = new RepositoryPropertyTreeNode(rootTreeNode);
            node.setProperty(property);
            int idx = rootTreeNode.add(node);
            ((DefaultTreeModel) configTree.getModel()).nodesWereInserted(rootTreeNode, new int[] { idx });
        }
    }

    private void removeAction(AnActionButton anActionButton) {
        TreePath[] selection = configTree.getSelectionPaths();
        if (selection == null) {
            return;
        }
        Set<RepositoryPropertyTreeNode> nodestoremove = new HashSet<>();
        for (TreePath selpath : selection) {
            Object[] path = selpath.getPath();
            if (path.length == 0) {
                continue;
            }
            if (path[path.length - 1] instanceof RepositoryPropertyTreeNode) {
                nodestoremove.add((RepositoryPropertyTreeNode) path[path.length - 1]);
            }
        }
        if (!nodestoremove.isEmpty()) {
            for (RepositoryPropertyTreeNode n : nodestoremove) {
                int idx = rootTreeNode.getIndex(n);
                rootTreeNode.remove(n);
                ((DefaultTreeModel) configTree.getModel())
                        .nodesWereRemoved(rootTreeNode, new int[] { idx }, new Object[] { n });
            }
        }
    }

    private void editAction(AnActionButton anActionButton) {
        performEditAction();
    }

    private void performEditAction() {
        Object selection = configTree.getLastSelectedPathComponent();
        RepositoryPropertyTreeNode propertynode;
        String[] editinitialconfigid = null;
        if (selection instanceof RepositoryPropertyTreeNode) {
            propertynode = (RepositoryPropertyTreeNode) selection;
        } else if (selection instanceof PropertyAttributeTreeNode) {
            propertynode = (RepositoryPropertyTreeNode) ((PropertyAttributeTreeNode) selection).getParent();
            Object userdata = ((PropertyAttributeTreeNode) selection).getUserData();
            if (userdata instanceof String[]) {
                editinitialconfigid = (String[]) userdata;
            } else if (userdata instanceof String) {
                editinitialconfigid = new String[] { (String) userdata };
            }
        } else {
            return;
        }
        RepositoryIDEProperty editedproperty = propertynode.getProperty();
        RepositoryIDEProperty property = showWizard(editedproperty, editinitialconfigid);
        if (property != null) {
            propertynode.setProperty(property);
            DefaultTreeModel model = (DefaultTreeModel) configTree.getModel();
            model.nodeStructureChanged(propertynode);
        }
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
        rootPanel.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        final JLabel label1 = new JLabel();
        label1.setText("The following repositories are used during the build execution to look up build tasks.");
        rootPanel.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        configurationsPanel = new JPanel();
        configurationsPanel.setLayout(new CardLayout(0, 0));
        rootPanel.add(configurationsPanel,
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

    private static final class RepositoryPropertyTreeNode extends PropertyTreeNode<RepositoryIDEProperty> {
        public RepositoryPropertyTreeNode(TreeNode parent) {
            super(parent);
        }

        @Override
        public void setProperty(RepositoryIDEProperty property) {
            this.children = new ArrayList<>();

            PropertyAttributeTreeNode classpath = new PropertyAttributeTreeNode(this, "Classpath",
                    SakerIDESupportUtils.classPathLocationToLabel(property.getClassPathLocation()));
            children.add(classpath);
            classpath.setUserData(SakerIDESupportUtils.WIZARDPAGE_CONFIGURATION_EDIT_INITIAL_CLASSPATH);

            String repoidstr = property.getRepositoryIdentifier();
            if (ObjectUtils.isNullOrEmpty(repoidstr)) {
                repoidstr = "No identifier/auto-generated";
            }
            PropertyAttributeTreeNode identifiernode = new PropertyAttributeTreeNode(this, "Identifier", repoidstr);
            children.add(identifiernode);
            identifiernode.setUserData(SakerIDESupportUtils.WIZARDPAGE_CONFIGURATION_EDIT_INITIAL_REPOSITORYID);

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

            super.setProperty(property);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(ObjectUtils
                    .nullDefault(SakerIDESupportUtils.classPathLocationToLabel(property.getClassPathLocation()), ""));
            String repositoryid = property.getRepositoryIdentifier();
            if (!ObjectUtils.isNullOrEmpty(repositoryid)) {
                sb.append(" @");
                sb.append(repositoryid);
            }
            return sb.toString();
        }
    }

}
