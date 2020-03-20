package saker.build.ide.intellij.impl.properties;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.AddEditRemovePanel;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import org.jetbrains.annotations.Nullable;
import saker.build.file.path.SakerPath;
import saker.build.ide.intellij.impl.ui.DummyDisposable;
import saker.build.ide.support.SakerIDEProject;
import saker.build.ide.support.SakerIDESupportUtils;
import saker.build.ide.support.properties.DaemonConnectionIDEProperty;
import saker.build.ide.support.properties.IDEProjectProperties;
import saker.build.ide.support.properties.ProviderMountIDEProperty;
import saker.build.ide.support.ui.FileSystemEndpointSelector;
import saker.build.thirdparty.saker.util.ObjectUtils;

import javax.swing.*;
import java.awt.Dimension;
import java.awt.Insets;
import java.util.*;

public class PathConfigurationForm {
    private JPanel rootPanel;
    private JTextField workingDirectoryTextField;
    private JTextField buildDirectoryTextField;
    private JTextField mirrorDirectoryTextField;
    private JPanel tablePanel;

    private Disposable myDisposable = new DummyDisposable();

    private Project project;
    private AddEditRemovePanel<ProviderMountIDEProperty> mountsEditPanel;
    private Iterable<? extends DaemonConnectionIDEProperty> daemonConnections = Collections.emptySet();

    public PathConfigurationForm(Project project) {
        this.project = project;
        mountsEditPanel = new AddEditRemovePanel<ProviderMountIDEProperty>(new MountsTableModel(), new ArrayList<>()) {
            {
                getTable().setShowColumns(true);
                getTable().getTableHeader().setReorderingAllowed(false);
            }

            @Nullable
            @Override
            protected ProviderMountIDEProperty addItem() {
                return PathConfigurationForm.this.addItem();
            }

            @Override
            protected boolean removeItem(ProviderMountIDEProperty o) {
                return true;
            }

            @Nullable
            @Override
            protected ProviderMountIDEProperty editItem(ProviderMountIDEProperty o) {
                return PathConfigurationForm.this.editItem(o);
            }
        };

        tablePanel.add(mountsEditPanel,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW | GridConstraints.SIZEPOLICY_WANT_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null,
                        new Dimension(200, 200), null, 0, false));
    }

    private Set<String> getRoots() {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (ProviderMountIDEProperty m : getMounts()) {
            String r = SakerIDESupportUtils.tryNormalizePathRoot(m.getRoot());
            if (ObjectUtils.isNullOrEmpty(r)) {
                continue;
            }
            result.add(r);
        }

        return result;
    }

    private ProviderMountIDEProperty addItem() {
        MountPathDialog dialog = new MountPathDialog("Mount Path", tablePanel, project, daemonConnections);
        dialog.setExistingRoots(getRoots());
        dialog.setVisible(true);
        return dialog.getProperty();
    }

    private ProviderMountIDEProperty editItem(ProviderMountIDEProperty o) {
        MountPathDialog dialog = new MountPathDialog("Edit Mount Path", tablePanel, project, daemonConnections);
        Set<String> existingroots = getRoots();
        existingroots.remove(SakerIDESupportUtils.tryNormalizePathRoot(o.getRoot()));
        dialog.setExistingRoots(existingroots);
        dialog.setEditProperty(o);
        dialog.setVisible(true);
        return ObjectUtils.nullDefault(dialog.getProperty(), o);
    }

    public void setMounts(List<? extends ProviderMountIDEProperty> properties) {
        mountsEditPanel.setData(ObjectUtils.newArrayList(properties));
    }

    public void reset(IDEProjectProperties props) {
        if (props != null) {
            mountsEditPanel.setData(ObjectUtils.newArrayList(props.getMounts()));
            workingDirectoryTextField.setText(
                    ObjectUtils.nullDefault(SakerIDESupportUtils.normalizePath(props.getWorkingDirectory()), ""));
            buildDirectoryTextField.setText(
                    ObjectUtils.nullDefault(SakerIDESupportUtils.normalizePath(props.getBuildDirectory()), ""));
            mirrorDirectoryTextField.setText(
                    ObjectUtils.nullDefault(SakerIDESupportUtils.normalizePath(props.getMirrorDirectory()), ""));
            daemonConnections = props.getConnections();
        }
    }

    public void dispose() {
        Disposer.dispose(myDisposable);
    }

    public Set<ProviderMountIDEProperty> getMounts() {
        return new LinkedHashSet<>(mountsEditPanel.getData());
    }

    public String getWorkingDirectory() {
        return SakerIDESupportUtils.normalizePath(workingDirectoryTextField.getText());
    }

    public String getBuildDirectory() {
        return SakerIDESupportUtils.normalizePath(buildDirectoryTextField.getText());
    }

    public String getMirrorDirectory() {
        return SakerIDESupportUtils.normalizePath(mirrorDirectoryTextField.getText());
    }

    public JPanel getRootPanel() {
        return rootPanel;
    }

    private static final String[] COLUMN_NAMES = { "Mount root", "File system endpoint", "Mounted path" };

    private class MountsTableModel extends AddEditRemovePanel.TableModel<ProviderMountIDEProperty> {

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
        public Object getField(ProviderMountIDEProperty o, int columnIndex) {
            switch (columnIndex) {
                case 0: {
                    return SakerIDESupportUtils.tryNormalizePathRoot(o.getRoot());
                }
                case 1: {
                    String mountnamestr = o.getMountClientName();
                    DaemonConnectionIDEProperty connprop = SakerIDESupportUtils
                            .getConnectionPropertyWithName(daemonConnections, mountnamestr);
                    if (connprop != null) {
                        if (ObjectUtils.isNullOrEmpty(connprop.getNetAddress())) {
                            return connprop.getConnectionName();
                        }
                        return ObjectUtils.nullDefault(connprop.getConnectionName(), "") + " @" + connprop
                                .getNetAddress();
                    }
                    if (SakerIDEProject.MOUNT_ENDPOINT_PROJECT_RELATIVE.equals(mountnamestr)) {
                        return FileSystemEndpointSelector.LABEL_PROJECT_RELATIVE;
                    }
                    if (SakerIDEProject.MOUNT_ENDPOINT_LOCAL_FILESYSTEM.equals(mountnamestr)) {
                        return FileSystemEndpointSelector.LABEL_LOCAL_FILE_SYSTEM;
                    }
                    return mountnamestr;
                }
                case 2: {
                    String mountpathstr = SakerIDESupportUtils.normalizePath(o.getMountPath());
                    try {
                        SakerPath path = SakerPath.valueOf(mountpathstr);
                        String mountnamestr = o.getMountClientName();
                        if (SakerIDEProject.MOUNT_ENDPOINT_PROJECT_RELATIVE.equals(mountnamestr) && path
                                .isForwardRelative()) {
                            path = path.replaceRoot(SakerPath.ROOT_SLASH);
                        }
                        return path.toString();
                    } catch (Exception e) {
                        return mountpathstr;
                    }
                }
                default: {
                    throw new UnsupportedOperationException(columnIndex + "");
                }
            }
        }
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
        rootPanel.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(3, 2, new Insets(5, 5, 5, 5), -1, -1));
        rootPanel.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0,
                false));
        panel1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Special paths"));
        final JLabel label1 = new JLabel();
        label1.setText("Working directory:");
        panel1.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Build directory:");
        panel1.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Mirror directory:");
        panel1.add(label3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        workingDirectoryTextField = new JTextField();
        panel1.add(workingDirectoryTextField,
                new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null,
                        new Dimension(150, -1), null, 0, false));
        buildDirectoryTextField = new JTextField();
        panel1.add(buildDirectoryTextField,
                new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null,
                        new Dimension(150, -1), null, 0, false));
        mirrorDirectoryTextField = new JTextField();
        panel1.add(mirrorDirectoryTextField,
                new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null,
                        new Dimension(150, -1), null, 0, false));
        tablePanel = new JPanel();
        tablePanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        rootPanel.add(tablePanel,
                new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null,
                        0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return rootPanel;
    }

}
