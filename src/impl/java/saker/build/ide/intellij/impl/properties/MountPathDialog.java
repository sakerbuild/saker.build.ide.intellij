package saker.build.ide.intellij.impl.properties;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBTextField;
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

import javax.swing.*;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

public class MountPathDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JBTextField executionRootTextField;
    private JComboBox<String> fileSystemEndpointComboBox;
    private TextFieldWithBrowseButton mountedPathTextField;

    private ProviderMountIDEProperty property;

    private FileSystemEndpointSelector endpointSelector;
    private Iterable<? extends DaemonConnectionIDEProperty> connections;

    private Disposable myDisposable = new DummyDisposable();

    private Set<String> existingRoots = Collections.emptySet();

    public MountPathDialog(String title, JComponent relative, Project project,
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

        executionRootTextField.getEmptyText().clear().appendText("Execution root");

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(e -> onCancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        mountedPathTextField.addActionListener(e -> {
            showFileChooser(project);
        });
        //TODO enable button if local
        mountedPathTextField.setButtonEnabled(false);
        mountedPathTextField.getButton().setEnabled(false);

        pack();
        setMinimumSize(getSize());

        resetEndpointSelector(SakerIDEProject.MOUNT_ENDPOINT_PROJECT_RELATIVE);
        JTextField mountpathtextfield = mountedPathTextField.getTextField();
        fileSystemEndpointComboBox.addItemListener(e -> {
            //TODO auto-convert between local and project relative
            endpointSelector.setSelectedIndex(fileSystemEndpointComboBox.getSelectedIndex());
            validator.revalidateComponent(mountpathtextfield);
        });

        validator.add(executionRootTextField, () -> {
            String text = executionRootTextField.getText();
            if (text.isEmpty()) {
                return new ValidationInfo("Please specify an execution root for the mounted path.",
                        executionRootTextField);
            }
            String normalizedroot;
            try {
                normalizedroot = SakerPath.normalizeRoot(text);
            } catch (Exception e) {
                return new ValidationInfo("Root should have the format: [a-z]+: or be the / root.",
                        executionRootTextField);
            }
            if (existingRoots.contains(text) || existingRoots.contains(normalizedroot)) {
                return new ValidationInfo("The specified root is already mounted: " + normalizedroot,
                        executionRootTextField);
            }
            return null;
        }, FormValidator.REQUIRED | FormValidator.START_ON_FOCUS_LOST).add(mountpathtextfield, () -> {
            String text = mountpathtextfield.getText();
            SakerPath path;
            try {
                path = SakerPath.valueOf(text);
            } catch (Exception e) {
                return new ValidationInfo("Invalid path format. (" + e.getMessage() + ")", mountpathtextfield);
            }

            if (SakerIDEProject.MOUNT_ENDPOINT_PROJECT_RELATIVE.equals(endpointSelector.getSelectedEndpointName())) {
                if (path.isRelative()) {
                    //ok.
                    if (!path.isForwardRelative()) {
                        return new ValidationInfo(
                                "Project relative mount path should be forward relative. (Shouldn't start with ../)",
                                mountpathtextfield);
                    }
                } else {
                    if (!SakerPath.ROOT_SLASH.equals(path.getRoot())) {
                        return new ValidationInfo(
                                "Project relative mount path should be forward relative or have the /" + " root.",
                                mountpathtextfield);
                    }
                }
            } else {
                if (!path.isAbsolute()) {
                    return new ValidationInfo("Mounted path should be absolute.", mountpathtextfield);
                }
            }
            return null;
        }, FormValidator.REQUIRED | FormValidator.START_ON_FOCUS_LOST);
    }

    public void setExistingRoots(Set<String> existingRoots) {
        this.existingRoots = new TreeSet<>(existingRoots);
        //add the normalized forms as well
        for (String r : existingRoots) {
            try {
                this.existingRoots.add(SakerPath.normalizeRoot(r));
            } catch (IllegalArgumentException e) {
                //the root may have an illegal format
            }
        }

    }

    private void resetEndpointSelector(String endpoint) {
        endpointSelector = new FileSystemEndpointSelector(this.connections, endpoint);
        fileSystemEndpointComboBox.setModel(
                new DefaultComboBoxModel<>(endpointSelector.getLabels().toArray(ObjectUtils.EMPTY_STRING_ARRAY)));
        fileSystemEndpointComboBox.setSelectedIndex(endpointSelector.getSelectedIndex());
    }

    public ProviderMountIDEProperty getProperty() {
        return property;
    }

    public void setEditProperty(ProviderMountIDEProperty property) {
        this.executionRootTextField
                .setText(ObjectUtils.nullDefault(SakerIDESupportUtils.tryNormalizePathRoot(property.getRoot()), ""));
        this.mountedPathTextField.setText(ObjectUtils.nullDefault(property.getMountPath(), ""));
        resetEndpointSelector(property.getMountClientName());
        buttonOK.setEnabled(true);
    }

    private void showFileChooser(Project project) {
        String endpointname = endpointSelector.getSelectedEndpointName();
        if (endpointname == null) {
            //TODO info
            return;
        }
        switch (endpointname) {
            case SakerIDEProject.MOUNT_ENDPOINT_PROJECT_RELATIVE: {
                VirtualFile root = null;
                VirtualFile basedir = project.getBaseDir();
                SakerPath projectbasepath;
                VirtualFile toSelect = null;
                if (basedir != null) {
                    VirtualFile parent = basedir.getParent();
                    if (parent == null) {
                        root = basedir;
                    } else {
                        root = parent;
                    }
                    projectbasepath = SakerPath.valueOf(basedir.getPath());

                    try {
                        SakerPath currentpath = SakerPath.valueOf(mountedPathTextField.getText());
                        if (SakerPath.ROOT_SLASH.equals(currentpath.getRoot())) {
                            currentpath = currentpath.replaceRoot(null);
                        }
                        if (currentpath.isRelative()) {
                            toSelect = VfsUtil.findRelativeFile(basedir, currentpath.getNameArray());
                        }
                        // else it has an unrecognized root. cant set
                    } catch (Exception e) {
                        //XXX display exception?
                    }
                } else {
                    projectbasepath = null;
                }
                if (toSelect == null) {
                    //try against the local file system
                    try {
                        toSelect = LocalFileSystem.getInstance().findFileByPath(mountedPathTextField.getText());
                    } catch (Exception e) {
                        //XXX display exception?
                    }
                }
                FileChooserDescriptor chooserdescriptor = new FileChooserDescriptor(false, true, false, false, false,
                        false) {
                    @Override
                    public boolean isFileVisible(VirtualFile f, boolean showHiddenFiles) {
                        try {
                            SakerPath fp = SakerPath.valueOf(f.getPath());
                            return fp.startsWith(projectbasepath) && super.isFileVisible(f, showHiddenFiles);
                        } catch (Exception e) {
                            //XXX display exception?
                        }
                        return super.isFileVisible(f, showHiddenFiles);
                    }
                };
                if (root != null) {
                    chooserdescriptor.setRoots(root);
                }
                VirtualFile f = FileChooser.chooseFile(chooserdescriptor, contentPane, project, toSelect);
                if (f != null) {
                    SakerPath chosenspath = SakerPath.valueOf(f.getPath());
                    if (projectbasepath == null || !chosenspath.startsWith(projectbasepath)) {
                        int idx = endpointSelector.selectEndpoint(SakerIDEProject.MOUNT_ENDPOINT_LOCAL_FILESYSTEM);
                        fileSystemEndpointComboBox.setSelectedIndex(idx);
                        mountedPathTextField.setText(chosenspath.toString());
                    } else {
                        SakerPath relpath = projectbasepath.relativize(chosenspath).replaceRoot(SakerPath.ROOT_SLASH);
                        mountedPathTextField.setText(relpath.toString());
                    }
                }
                break;
            }
            case SakerIDEProject.MOUNT_ENDPOINT_LOCAL_FILESYSTEM: {
                FileChooserDescriptor chooserdescriptor = new FileChooserDescriptor(false, true, false, false, false,
                        false);
                VirtualFile toSelect = null;

                try {
                    toSelect = LocalFileSystem.getInstance().findFileByPath(mountedPathTextField.getText());
                } catch (Exception e) {
                    //XXX display exception?
                }
                VirtualFile f = FileChooser.chooseFile(chooserdescriptor, contentPane, null, toSelect);
                if (f != null) {
                    mountedPathTextField.setText(f.getPath());
                }
                break;
            }
            default: {
                //TODO info
                break;
            }
        }
    }

    private void onOK() {
        property = new ProviderMountIDEProperty(executionRootTextField.getText(),
                new MountPathIDEProperty(endpointSelector.getSelectedEndpointName(), mountedPathTextField.getText()));
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
        panel3.setLayout(new GridLayoutManager(3, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel3,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        new Dimension(350, -1), null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Execution root:");
        panel3.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("File system endpoint:");
        panel3.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Mounted path:");
        panel3.add(label3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        executionRootTextField = new JBTextField();
        panel3.add(executionRootTextField,
                new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null,
                        new Dimension(150, -1), null, 0, false));
        mountedPathTextField = new TextFieldWithBrowseButton();
        panel3.add(mountedPathTextField,
                new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null,
                        null, 0, false));
        fileSystemEndpointComboBox = new JComboBox();
        panel3.add(fileSystemEndpointComboBox,
                new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0,
                        false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }

}
