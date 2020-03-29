package saker.build.ide.intellij.impl.properties;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.AddEditRemovePanel;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.components.JBTextField;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import saker.build.file.path.SakerPath;
import saker.build.ide.intellij.impl.ui.SakerPropertyPageAddEditRemovePanel;
import saker.build.ide.support.SakerIDEProject;
import saker.build.ide.support.SakerIDESupportUtils;
import saker.build.ide.support.properties.DaemonConnectionIDEProperty;
import saker.build.ide.support.properties.IDEProjectProperties;
import saker.build.ide.support.properties.ProviderMountIDEProperty;
import saker.build.ide.support.properties.SimpleIDEProjectProperties;
import saker.build.ide.support.ui.FileSystemEndpointSelector;
import saker.build.thirdparty.saker.util.ObjectUtils;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class PathConfigurationForm {
    private JPanel rootPanel;
    private JBTextField workingDirectoryTextField;
    private JBTextField buildDirectoryTextField;
    private JBTextField mirrorDirectoryTextField;
    private JPanel tablePanel;
    private JPanel pathsPanel;

    private Disposable myDisposable = Disposer.newDisposable();

    private AddEditRemovePanel<ProviderMountIDEProperty> mountsEditPanel;
    private PathConfigurationConfigurable configurable;

    public PathConfigurationForm(PathConfigurationConfigurable configurable) {
        this.configurable = configurable;

        workingDirectoryTextField.getEmptyText().clear().appendText("Execution path");
        buildDirectoryTextField.getEmptyText().clear().appendText("Execution path");
        mirrorDirectoryTextField.getEmptyText().clear().appendText("Execution daemon local path (empty for default)");

        pathsPanel.setBorder(IdeBorderFactory.createTitledBorder("Special paths", true));
        tablePanel.setBorder(IdeBorderFactory.createTitledBorder("Execution roots", false));

        MountsTableModel tablemodel = new MountsTableModel();
        mountsEditPanel = new SakerPropertyPageAddEditRemovePanel<ProviderMountIDEProperty>(tablemodel) {
            @Nullable
            @Override
            protected ProviderMountIDEProperty addItem() {
                return PathConfigurationForm.this.addItem();
            }

            @Nullable
            @Override
            protected ProviderMountIDEProperty editItem(ProviderMountIDEProperty o) {
                return PathConfigurationForm.this.editItem(o);
            }
        };

        tablePanel.add(mountsEditPanel);
        mountsEditPanel.getEmptyText().clear().appendText("No mounted paths.");

        mountsEditPanel.getTable().getModel().addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                configurable.getParent().getBuilder().setMounts(getMounts());
            }
        });

        SimpleIDEProjectProperties.Builder propertiesbuilder = configurable.getParent().getBuilder();
        SakerBuildProjectConfigurable
                .addTextPropertyChangeListener(workingDirectoryTextField, propertiesbuilder::setWorkingDirectory);
        SakerBuildProjectConfigurable
                .addTextPropertyChangeListener(buildDirectoryTextField, propertiesbuilder::setBuildDirectory);
        SakerBuildProjectConfigurable
                .addTextPropertyChangeListener(mirrorDirectoryTextField, propertiesbuilder::setMirrorDirectory);
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
        MountPathDialog dialog = new MountPathDialog("Mount Path", tablePanel,
                configurable.getParent().getProject().getProject(),
                configurable.getParent().getCurrentProjectProperties().getConnections());
        dialog.setExistingRoots(getRoots());
        dialog.setVisible(true);
        return dialog.getProperty();
    }

    private ProviderMountIDEProperty editItem(ProviderMountIDEProperty o) {
        MountPathDialog dialog = new MountPathDialog("Edit Mount Path", tablePanel,
                configurable.getParent().getProject().getProject(),
                configurable.getParent().getCurrentProjectProperties().getConnections());
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

    public void reset() {
        IDEProjectProperties props = configurable.getParent().getProperties();
        mountsEditPanel.setData(ObjectUtils.newArrayList(props.getMounts()));
        workingDirectoryTextField
                .setText(ObjectUtils.nullDefault(SakerIDESupportUtils.normalizePath(props.getWorkingDirectory()), ""));
        buildDirectoryTextField
                .setText(ObjectUtils.nullDefault(SakerIDESupportUtils.normalizePath(props.getBuildDirectory()), ""));
        mirrorDirectoryTextField
                .setText(ObjectUtils.nullDefault(SakerIDESupportUtils.normalizePath(props.getMirrorDirectory()), ""));

        configurable.getParent().getBuilder().setMounts(getMounts());
    }

    public void dispose() {
        Disposer.dispose(myDisposable);
    }

    public Set<ProviderMountIDEProperty> getMounts() {
        return new LinkedHashSet<>(mountsEditPanel.getData());
    }

    public JPanel getRootPanel() {
        return rootPanel;
    }

    private static final String[] COLUMN_NAMES = { "Execution root", "File system endpoint", "Mounted path" };

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
                    DaemonConnectionIDEProperty connprop = SakerIDESupportUtils.getConnectionPropertyWithName(
                            configurable.getParent().getCurrentProjectProperties().getConnections(), mountnamestr);
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
        pathsPanel = new JPanel();
        pathsPanel.setLayout(new GridLayoutManager(3, 2, new Insets(0, 0, 0, 0), -1, -1));
        rootPanel.add(pathsPanel,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
                        0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Working directory:");
        pathsPanel.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Build directory:");
        pathsPanel.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Mirror directory:");
        pathsPanel.add(label3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        workingDirectoryTextField = new JBTextField();
        pathsPanel.add(workingDirectoryTextField,
                new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null,
                        new Dimension(150, -1), null, 0, false));
        buildDirectoryTextField = new JBTextField();
        pathsPanel.add(buildDirectoryTextField,
                new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null,
                        new Dimension(150, -1), null, 0, false));
        mirrorDirectoryTextField = new JBTextField();
        pathsPanel.add(mirrorDirectoryTextField,
                new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null,
                        new Dimension(150, -1), null, 0, false));
        tablePanel = new JPanel();
        tablePanel.setLayout(new CardLayout(0, 0));
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
