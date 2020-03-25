package saker.build.ide.intellij.impl.properties.wizard;

import com.intellij.ui.DoubleClickListener;
import saker.build.ide.support.ui.wizard.BaseSakerWizardManager;
import saker.build.ide.support.ui.wizard.ClassPathTypeChooserSakerWizardPage;
import saker.build.ide.support.ui.wizard.SakerWizardPage;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.MouseEvent;

public class ClassPathTypeChooserWizardForm {
    public static final String WIZARD_CONFIGURATION_WITH_SCRIPT_CLASSPATH = "classpath.type.chooser.with.script";
    public static final String WIZARD_CONFIGURATION_WITH_REPOSITORY_CLASSPATH = "classpath.type.chooser.with.repository";

    private JList<String> typeList;
    private JPanel rootPanel;
    private ClassPathTypeChooserWizardStep wizardStep;

    public ClassPathTypeChooserWizardForm(ClassPathTypeChooserWizardStep wizardstep) {
        wizardStep = wizardstep;
        ClassPathTypeChooserSakerWizardPage wizardpage = wizardstep.getWizardPage();
        BaseSakerWizardManager<SakerWizardPage> wizardmanager = wizardstep.getModel().getWizardManager();
        DefaultListModel<String> items = createItems(
                Boolean.TRUE.equals(wizardmanager.getConfiguration(WIZARD_CONFIGURATION_WITH_REPOSITORY_CLASSPATH)),
                Boolean.TRUE.equals(wizardmanager.getConfiguration(WIZARD_CONFIGURATION_WITH_SCRIPT_CLASSPATH)));
        typeList.setModel(items);
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
        switch (wizardpage.getSelected()) {
            case ClassPathTypeChooserSakerWizardPage.SELECTED_JAVA_ARCHIVE: {
                typeList.setSelectedIndex(items.indexOf(ClassPathTypeChooserSakerWizardPage.LABEL_JAVA_ARCHIVE));
                break;
            }
            case ClassPathTypeChooserSakerWizardPage.SELECTED_NETWORK_ARCHIVE: {
                typeList.setSelectedIndex(
                        items.indexOf(ClassPathTypeChooserSakerWizardPage.LABEL_NETWORK_ARCHIVE_HTTP));
                break;
            }
            case ClassPathTypeChooserSakerWizardPage.SELECTED_NEST_REPOSITORY: {
                typeList.setSelectedIndex(
                        items.indexOf(ClassPathTypeChooserSakerWizardPage.LABEL_NEST_REPOSITORY_CLASS_PATH));
                break;
            }
            case ClassPathTypeChooserSakerWizardPage.SELECTED_SAKERSCRIPT: {
                typeList.setSelectedIndex(
                        items.indexOf(ClassPathTypeChooserSakerWizardPage.LABEL_SAKER_SCRIPT_CLASS_PATH));
                break;
            }
        }
    }

    public JList<String> getTypeList() {
        return typeList;
    }

    private static DefaultListModel<String> createItems(boolean repositorycp, boolean scriptcp) {
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
        rootPanel.setMinimumSize(new Dimension(400, 400));
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
