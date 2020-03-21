package saker.build.ide.intellij.impl.properties;

import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.CommonActionsPanel;
import com.intellij.ui.DoubleClickListener;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.uiDesigner.core.GridLayoutManager;
import saker.build.ide.intellij.impl.properties.wizard.ClassPathTypeChooserWizardStep;
import saker.build.ide.intellij.impl.properties.wizard.RepositoryServiceEnumeratorSakerWizardPage;
import saker.build.ide.intellij.impl.properties.wizard.SakerWizardDialog;
import saker.build.ide.intellij.impl.properties.wizard.SakerWizardModel;
import saker.build.ide.intellij.impl.properties.wizard.ScriptServiceEnumeratorSakerWizardPage;
import saker.build.ide.intellij.impl.ui.PropertyAttributeTreeNode;
import saker.build.ide.intellij.impl.ui.PropertyTreeNode;
import saker.build.ide.intellij.impl.ui.RootTreeNode;
import saker.build.ide.support.SakerIDESupportUtils;
import saker.build.ide.support.properties.ClassPathLocationIDEProperty;
import saker.build.ide.support.properties.ClassPathServiceEnumeratorIDEProperty;
import saker.build.ide.support.properties.IDEProjectProperties;
import saker.build.ide.support.properties.RepositoryIDEProperty;
import saker.build.ide.support.properties.ScriptConfigurationIDEProperty;
import saker.build.ide.support.ui.wizard.AbstractSakerWizardPage;
import saker.build.ide.support.ui.wizard.ClassPathTypeChooserSakerWizardPage;
import saker.build.ide.support.ui.wizard.RepositoryIdentifierSakerWizardPage;
import saker.build.ide.support.ui.wizard.SakerWizardManager;
import saker.build.ide.support.ui.wizard.SakerWizardPage;
import saker.build.ide.support.ui.wizard.ServiceEnumeratorRedirectingSakerWizardPage;
import saker.build.thirdparty.saker.util.ObjectUtils;

import javax.swing.JComponent;
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
        rootPanel.add(decorator.disableUpDownActions().setToolbarPosition(ActionToolbarPosition.RIGHT)
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

    private void addAction(AnActionButton anActionButton) {
        //TODO add
        SakerWizardModel model = new SakerWizardModel("New Repository Configuration",
                configurable.getParent().getCurrentProjectProperties(),
                configurable.getParent().getProject().getProject());
        ClassPathTypeChooserSakerWizardPage cppage = model.getWizardPage(ClassPathTypeChooserSakerWizardPage.class);
        ServiceEnumeratorRedirectingSakerWizardPage serviceenumeratorpage = model
                .getWizardPage(RepositoryServiceEnumeratorRedirectingSakerWizardPage.class);
        cppage.setNextPage(serviceenumeratorpage);
        serviceenumeratorpage.setNextPage(model.getWizardPage(RepositoryIdentifierSakerWizardPage.class));

        ClassPathTypeChooserWizardStep cpwizardstep = new ClassPathTypeChooserWizardStep(model, cppage);
        cpwizardstep.setClassPathTypes(true, false);

        model.add(cpwizardstep);

        SakerWizardDialog dialog = new SakerWizardDialog(rootPanel, true, model);
        model.setDialog(dialog);
        dialog.setModal(true);

        if (dialog.showAndGet()) {
            try {
                RepositoryIDEProperty property = (RepositoryIDEProperty) ObjectUtils.getOptional(cppage.finishWizard());
                RepositoryPropertyTreeNode node = new RepositoryPropertyTreeNode(rootTreeNode);
                node.setProperty(property);
                int idx = rootTreeNode.add(node);
                ((DefaultTreeModel) configTree.getModel()).nodesWereInserted(rootTreeNode, new int[] { idx });
            } catch (Exception e) {
                e.printStackTrace();
            }
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
        if (selection instanceof RepositoryPropertyTreeNode) {
            propertynode = (RepositoryPropertyTreeNode) selection;
        } else {
            return;
        }
        RepositoryIDEProperty editedproperty = propertynode.getProperty();
        //TODO edit
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
        rootPanel.setLayout(new CardLayout(0, 0));
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

            String repoidstr = property.getRepositoryIdentifier();
            if (ObjectUtils.isNullOrEmpty(repoidstr)) {
                repoidstr = "No identifier/auto-generated";
            }
            PropertyAttributeTreeNode identifiernode = new PropertyAttributeTreeNode(this, "Identifier", repoidstr);
            children.add(identifiernode);

            ClassPathServiceEnumeratorIDEProperty serviceenumeratorproperty = property.getServiceEnumerator();
            if (serviceenumeratorproperty != null) {
                PropertyAttributeTreeNode serviceenumerator = new PropertyAttributeTreeNode(this,
                        SakerIDESupportUtils.serviceEnumeratorToTitleLabel(serviceenumeratorproperty),
                        SakerIDESupportUtils.serviceEnumeratorToLabel(serviceenumeratorproperty));
                children.add(serviceenumerator);
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

    public static class RepositoryServiceEnumeratorRedirectingSakerWizardPage extends ServiceEnumeratorRedirectingSakerWizardPage {
        public RepositoryServiceEnumeratorRedirectingSakerWizardPage(
                SakerWizardManager<SakerWizardPage> wizardManager) {
            super(wizardManager);
        }

        @Override
        protected AbstractSakerWizardPage redirect() {
            return wizardManager.getWizardPage(RepositoryServiceEnumeratorSakerWizardPage.class);
        }
    }
}
