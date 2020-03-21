package saker.build.ide.intellij.impl.properties.wizard;

import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.DocumentAdapter;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.jetbrains.annotations.NotNull;
import saker.build.file.path.SakerPath;
import saker.build.ide.intellij.impl.properties.MountPathDialog;
import saker.build.ide.support.SakerIDEProject;
import saker.build.ide.support.SakerIDESupportUtils;
import saker.build.ide.support.properties.IDEProjectProperties;
import saker.build.ide.support.ui.FileSystemEndpointSelector;
import saker.build.thirdparty.saker.util.ObjectUtils;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import java.awt.Insets;

public class ClassPathFileChooserWizardForm {
    private ClassPathFileChooserWizardStep wizardStep;
    private JPanel rootPanel;
    private JComboBox<String> fileSystemEndpointComboBox;
    private TextFieldWithBrowseButton archivePathTextField;

    private FileSystemEndpointSelector endpointSelector;
    private IDEProjectProperties projectProperties;

    public ClassPathFileChooserWizardForm(ClassPathFileChooserWizardStep wizardstep) {
        wizardStep = wizardstep;
        this.projectProperties = wizardstep.getModel().getProjectProperties();

        fileSystemEndpointComboBox.addItemListener(e -> {
            //TODO auto-convert between local and project relative
            endpointSelector.setSelectedIndex(fileSystemEndpointComboBox.getSelectedIndex());
            //TODO revalidate path
            updateWizardStep();
        });

        resetEndpointSelector(SakerIDEProject.MOUNT_ENDPOINT_PROJECT_RELATIVE);
        archivePathTextField.addActionListener(e -> {
            MountPathDialog.showFileChooser(wizardstep.getModel().getProject(), archivePathTextField, rootPanel,
                    endpointSelector, fileSystemEndpointComboBox);
        });
        archivePathTextField.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                updateWizardStep();
            }
        });
        updateWizardStep();
    }

    private void updateWizardStep() {
        wizardStep.getWizardPage().setFile(endpointSelector.getSelectedEndpointName(),
                SakerIDESupportUtils.normalizePath(archivePathTextField.getText()));
        wizardStep.setFormComplete(isFormComplete());
        wizardStep.updateButtons();
    }

    private boolean isFormComplete() {
        try {
            SakerPath path = SakerPath.valueOf(archivePathTextField.getText());
            if (path.getFileName() == null) {
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void resetEndpointSelector(String endpoint) {
        endpointSelector = new FileSystemEndpointSelector(this.projectProperties, endpoint);
        fileSystemEndpointComboBox.setModel(
                new DefaultComboBoxModel<>(endpointSelector.getLabels().toArray(ObjectUtils.EMPTY_STRING_ARRAY)));
        fileSystemEndpointComboBox.setSelectedIndex(endpointSelector.getSelectedIndex());
    }

    public JTextField getArchivePathTextField() {
        return archivePathTextField.getTextField();
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
        rootPanel.setLayout(new GridLayoutManager(3, 2, new Insets(0, 0, 0, 0), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        rootPanel.add(panel1, new GridConstraints(0, 0, 2, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0,
                false));
        final JLabel label1 = new JLabel();
        label1.setText("Archive path:");
        panel1.add(label1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        archivePathTextField = new TextFieldWithBrowseButton();
        panel1.add(archivePathTextField,
                new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
                        0, false));
        final JLabel label2 = new JLabel();
        label2.setText("File system endpoint:");
        panel1.add(label2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        fileSystemEndpointComboBox = new JComboBox();
        panel1.add(fileSystemEndpointComboBox,
                new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0,
                        false));
        final Spacer spacer1 = new Spacer();
        rootPanel.add(spacer1,
                new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1,
                        GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return rootPanel;
    }

}