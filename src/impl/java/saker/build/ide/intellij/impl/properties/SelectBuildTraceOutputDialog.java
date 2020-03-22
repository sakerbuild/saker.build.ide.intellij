package saker.build.ide.intellij.impl.properties;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.Disposer;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import saker.build.file.path.SakerPath;
import saker.build.ide.intellij.impl.ui.DummyDisposable;
import saker.build.ide.intellij.impl.ui.FormValidator;
import saker.build.ide.support.SakerIDEProject;
import saker.build.ide.support.SakerIDESupportUtils;
import saker.build.ide.support.properties.DaemonConnectionIDEProperty;
import saker.build.ide.support.properties.MountPathIDEProperty;
import saker.build.ide.support.properties.ProviderMountIDEProperty;
import saker.build.ide.support.ui.FileSystemEndpointSelector;
import saker.build.thirdparty.saker.util.ObjectUtils;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class SelectBuildTraceOutputDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JComboBox<String> fileSystemEndpointComboBox;
    private TextFieldWithBrowseButton outputTextField;
    private JButton unsetButton;

    private FileSystemEndpointSelector endpointSelector;
    private Iterable<? extends DaemonConnectionIDEProperty> connections;

    private Disposable myDisposable = new DummyDisposable();
    private MountPathIDEProperty property;

    public SelectBuildTraceOutputDialog(String title, JComponent relative, Project project,
            Iterable<? extends DaemonConnectionIDEProperty> connections) {
        this.connections = connections;

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        setTitle(title);
        setLocationRelativeTo(relative);

        FormValidator validator = new FormValidator(buttonOK);

        buttonOK.setEnabled(false);
        buttonOK.addActionListener(e -> {
            if (!validator.canPerformOkRevalidateRefocus()) {
                return;
            }
            onOK();
        });

        buttonCancel.addActionListener(e -> onCancel());

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(e -> onCancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        outputTextField.addActionListener(e -> {
            showFileChooser(project);
        });

        pack();
        setMinimumSize(getSize());

        resetEndpointSelector(SakerIDEProject.MOUNT_ENDPOINT_PROJECT_RELATIVE);
        JTextField outputtextfield = outputTextField.getTextField();
        fileSystemEndpointComboBox.addItemListener(e -> {
            //TODO auto-convert between local and project relative
            endpointSelector.setSelectedIndex(fileSystemEndpointComboBox.getSelectedIndex());
            validator.revalidateComponent(outputtextfield);
        });

        unsetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SelectBuildTraceOutputDialog.this.property = null;
                dispose();
            }
        });

        validator.add(outputtextfield, () -> {
            String str = outputtextfield.getText();
            if (ObjectUtils.isNullOrEmpty(str)) {
                return new ValidationInfo("Please specify an output path.", outputtextfield);
            }
            try {
                SakerPath path = SakerPath.valueOf(str);
                if (path.getFileName() == null) {
                    return new ValidationInfo("No output file name specified.", outputtextfield);
                }
                if (SakerIDEProject.MOUNT_ENDPOINT_PROJECT_RELATIVE
                        .equals(endpointSelector.getSelectedEndpointName())) {
                    if (path.isRelative()) {
                        //ok.
                        if (!path.isForwardRelative()) {
                            return new ValidationInfo(
                                    "Project relative output path should be forward relative. (Shouldn't start with ../)",
                                    outputtextfield);
                        }
                    } else {
                        if (!SakerPath.ROOT_SLASH.equals(path.getRoot())) {
                            return new ValidationInfo(
                                    "Project relative output path should be forward relative or have the / root.",
                                    outputtextfield);
                        }
                    }
                } else {
                    if (!path.isAbsolute()) {
                        return new ValidationInfo("Output path should be absolute.", outputtextfield);
                    }
                }
            } catch (Exception e) {
                return new ValidationInfo("Invalid output path format. (" + e.getMessage() + ")", outputtextfield);
            }
            return null;
        }, FormValidator.REQUIRED | FormValidator.START_ON_FOCUS_LOST);
    }

    public MountPathIDEProperty getProperty() {
        return property;
    }

    public void setEditProperty(MountPathIDEProperty property) {
        this.property = property;
        if (property == null) {
            unsetButton.setVisible(false);
            this.outputTextField.setText("");
            resetEndpointSelector(SakerIDEProject.MOUNT_ENDPOINT_PROJECT_RELATIVE);
            buttonOK.setEnabled(false);
        } else {
            unsetButton.setVisible(true);
            this.outputTextField
                    .setText(ObjectUtils.nullDefault(SakerIDESupportUtils.normalizePath(property.getMountPath()), ""));
            resetEndpointSelector(property.getMountClientName());
            buttonOK.setEnabled(true);
        }
    }

    private void resetEndpointSelector(String endpoint) {
        endpointSelector = new FileSystemEndpointSelector(this.connections, endpoint);
        fileSystemEndpointComboBox.setModel(
                new DefaultComboBoxModel<>(endpointSelector.getLabels().toArray(ObjectUtils.EMPTY_STRING_ARRAY)));
        fileSystemEndpointComboBox.setSelectedIndex(endpointSelector.getSelectedIndex());
    }

    private void showFileChooser(Project project) {
        showFileChooser(project, this.outputTextField);
    }

    private void showFileChooser(Project project, TextFieldWithBrowseButton resulttextfield) {
        MountPathDialog.showFileChooser(project, resulttextfield, this.contentPane, this.endpointSelector,
                this.fileSystemEndpointComboBox);
    }

    private void onOK() {
        property = new MountPathIDEProperty(endpointSelector.getSelectedEndpointName(),
                SakerIDESupportUtils.normalizePath(outputTextField.getText()));
        dispose();
    }

    private void onCancel() {
        dispose();
    }

    @Override
    public void dispose() {
        Disposer.dispose(myDisposable);
        super.dispose();
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
        contentPane = new JPanel();
        contentPane.setLayout(new GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));
        contentPane.setMinimumSize(new Dimension(400, 136));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1,
                new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null,
                        null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1,
                new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1, true, false));
        panel1.add(panel2, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
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
        unsetButton = new JButton();
        unsetButton.setText("Unset");
        unsetButton.setVisible(false);
        panel1.add(unsetButton,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel3,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        new Dimension(350, -1), null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("File system endpoint:");
        panel3.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        fileSystemEndpointComboBox = new JComboBox();
        panel3.add(fileSystemEndpointComboBox,
                new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0,
                        false));
        final JLabel label2 = new JLabel();
        label2.setText("Output path:");
        panel3.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        outputTextField = new TextFieldWithBrowseButton();
        panel3.add(outputTextField,
                new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
                        0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }

}
