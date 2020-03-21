package saker.build.ide.intellij.impl.properties.wizard;

import com.intellij.ui.DoubleClickListener;
import saker.build.ide.support.ui.wizard.ClassPathTypeChooserSakerWizardPage;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListModel;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.MouseEvent;

public class ClassPathTypeChooserWizardForm {
    private JList<String> typeList;
    private JPanel rootPanel;
    private ClassPathTypeChooserWizardStep wizardStep;

    public ClassPathTypeChooserWizardForm(ClassPathTypeChooserWizardStep wizardstep) {
        wizardStep = wizardstep;
        typeList.setModel(createItems(false, false));
        typeList.addListSelectionListener(e -> {
            String selected = typeList.getSelectedValue();
            ClassPathTypeChooserSakerWizardPage page = wizardStep.getWizardPage();
            if (selected == null) {
                page.unselect();
            } else {
                switch (selected) {
                    case ClassPathTypeChooserSakerWizardPage.LABEL_JAVA_ARCHIVE: {
                        page.selectJavaArchive();
                        break;
                    }
                    case ClassPathTypeChooserSakerWizardPage.LABEL_NETWORK_ARCHIVE_HTTP: {
                        page.selectNetworkArchive();
                        break;
                    }
                    case ClassPathTypeChooserSakerWizardPage.LABEL_NEST_REPOSITORY_CLASS_PATH: {
                        page.selectNestRepository();
                        break;
                    }
                    case ClassPathTypeChooserSakerWizardPage.LABEL_SAKER_SCRIPT_CLASS_PATH: {
                        page.selectSakerScript();
                        break;
                    }
                }
            }
            wizardstep.updateButtons();
        });
        new DoubleClickListener() {
            @Override
            protected boolean onDoubleClick(MouseEvent event) {
                int index = typeList.locationToIndex(event.getPoint());
                if (index >= 0) {
                    if (wizardStep.getWizardPage().getNextPage() != null) {
                        wizardStep.getModel().next();
                    }
                    return true;
                }
                return false;
            }
        }.installOn(typeList);
    }

    public void setClassPathTypes(boolean repositorycp, boolean scriptcp) {
        typeList.setModel(createItems(repositorycp, scriptcp));
    }

    private static ListModel<String> createItems(boolean repositorycp, boolean scriptcp) {
        DefaultListModel<String> result = new DefaultListModel<>();
        result.addElement(ClassPathTypeChooserSakerWizardPage.LABEL_JAVA_ARCHIVE);
        result.addElement(ClassPathTypeChooserSakerWizardPage.LABEL_NETWORK_ARCHIVE_HTTP);
        if (repositorycp) {
            result.addElement(ClassPathTypeChooserSakerWizardPage.LABEL_NEST_REPOSITORY_CLASS_PATH);
        }
        if (scriptcp) {
            result.addElement(ClassPathTypeChooserSakerWizardPage.LABEL_SAKER_SCRIPT_CLASS_PATH);
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
        rootPanel.setLayout(new CardLayout(0, 0));
        typeList = new JList();
        typeList.setMinimumSize(new Dimension(0, 300));
        typeList.setSelectionMode(0);
        typeList.setVisibleRowCount(12);
        rootPanel.add(typeList, "Card1");
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return rootPanel;
    }

}
