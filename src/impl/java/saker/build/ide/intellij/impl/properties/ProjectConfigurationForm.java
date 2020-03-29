package saker.build.ide.intellij.impl.properties;

import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import saker.build.ide.support.SakerIDEProject;
import saker.build.ide.support.SakerIDESupportUtils;
import saker.build.ide.support.properties.IDEProjectProperties;
import saker.build.ide.support.properties.MountPathIDEProperty;
import saker.build.thirdparty.saker.util.ObjectUtils;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public class ProjectConfigurationForm {
    private JPanel rootPanel;
    private JPanel ideConfigurationPanel;
    private JPanel buildTracePanel;
    private JCheckBox generateIDEConfigurationFromCheckBox;
    private JCheckBox embedOutputArtifactsCheckBox;
    private TextFieldWithBrowseButton buildTraceOutputTextField;

    private SakerBuildProjectConfigurable configurable;

    public ProjectConfigurationForm(SakerBuildProjectConfigurable configurable) {
        this.configurable = configurable;

        ideConfigurationPanel.setBorder(IdeBorderFactory.createTitledBorder("IDE configuration", true));
        buildTracePanel.setBorder(IdeBorderFactory.createTitledBorder("Build trace", true));

        generateIDEConfigurationFromCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                configurable.getBuilder()
                        .setRequireTaskIDEConfiguration(generateIDEConfigurationFromCheckBox.isSelected());
            }
        });
        embedOutputArtifactsCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                configurable.getBuilder().setBuildTraceEmbedArtifacts(embedOutputArtifactsCheckBox.isSelected());
            }
        });

        buildTraceOutputTextField.addActionListener(e -> {
            SelectBuildTraceOutputDialog dialog = new SelectBuildTraceOutputDialog("Build Trace Output", rootPanel,
                    configurable.getProject().getProject(),
                    configurable.getCurrentProjectProperties().getConnections());
            dialog.setEditProperty(configurable.getCurrentProjectProperties().getBuildTraceOutput());
            dialog.setVisible(true);
            MountPathIDEProperty prop = dialog.getProperty();
            updateOutputTextField(prop);

            configurable.getBuilder().setBuildTraceOutput(prop);
        });
        buildTraceOutputTextField.getTextField().setEditable(false);
    }

    public void reset() {
        IDEProjectProperties properties = configurable.getProperties();
        generateIDEConfigurationFromCheckBox.setSelected(properties.isRequireTaskIDEConfiguration());
        embedOutputArtifactsCheckBox.setSelected(properties.isBuildTraceEmbedArtifacts());

        MountPathIDEProperty btout = properties.getBuildTraceOutput();
        if (btout != null) {
            updateOutputTextField(btout);
        }
    }

    private void updateOutputTextField(MountPathIDEProperty btout) {
        if (btout == null) {
            buildTraceOutputTextField.setText("");
            return;
        }
        String clientname = btout.getMountClientName();
        if (SakerIDEProject.MOUNT_ENDPOINT_PROJECT_RELATIVE.equals(clientname)) {
            buildTraceOutputTextField.setText(
                    ObjectUtils.nullDefault(SakerIDESupportUtils.normalizePath(btout.getMountPath()), "<missing>"));
        } else {
            buildTraceOutputTextField.setText(ObjectUtils.nullDefault(clientname, "") + " : " + ObjectUtils
                    .nullDefault(btout.getMountPath(), "<missing>"));
        }
    }

    public JPanel getRootPanel() {
        return rootPanel;
    }

    public void dispose() {
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
        label1.setText("Saker.build project settings are available on the sub-pages.");
        rootPanel.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        rootPanel.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0,
                false));
        ideConfigurationPanel = new JPanel();
        ideConfigurationPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(ideConfigurationPanel,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
                        0, false));
        generateIDEConfigurationFromCheckBox = new JCheckBox();
        generateIDEConfigurationFromCheckBox.setText("Generate IDE configuration from build tasks");
        ideConfigurationPanel.add(generateIDEConfigurationFromCheckBox,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buildTracePanel = new JPanel();
        buildTracePanel.setLayout(new GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(buildTracePanel,
                new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
                        0, false));
        embedOutputArtifactsCheckBox = new JCheckBox();
        embedOutputArtifactsCheckBox.setText("Embed output artifacts in build trace");
        buildTracePanel.add(embedOutputArtifactsCheckBox,
                new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Build trace output:");
        buildTracePanel.add(label2,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0,
                        false));
        buildTraceOutputTextField = new TextFieldWithBrowseButton();
        buildTracePanel.add(buildTraceOutputTextField,
                new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
                        0, false));
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
