package saker.build.ide.intellij.impl.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.DoubleClickListener;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import saker.build.ide.intellij.impl.IntellijSakerIDEProject;
import saker.build.ide.intellij.impl.properties.DaemonConnectionsConfigurable;
import saker.build.ide.intellij.impl.properties.ExecutionUserParametersConfigureable;
import saker.build.ide.intellij.impl.properties.PathConfigurationConfigurable;
import saker.build.ide.intellij.impl.properties.SakerBuildProjectConfigurable;
import saker.build.ide.intellij.impl.properties.ScriptConfigurationConfigurable;
import saker.build.ide.intellij.impl.properties.TaskRepositoriesConfigurable;
import saker.build.ide.support.SakerIDEProject;
import saker.build.ide.support.SakerIDESupportUtils;
import saker.build.ide.support.properties.PropertiesValidationErrorResult;
import saker.build.ide.support.properties.PropertiesValidationException;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Set;

public class ProjectPropertiesValidationDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JList<ValidationItem> errorsList;

    private IntellijSakerIDEProject project;

    public ProjectPropertiesValidationDialog(IntellijSakerIDEProject project,
            Set<PropertiesValidationErrorResult> errors) {
        this.project = project;
        setContentPane(this.contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        setTitle("Invalid Project Properties");

        buttonOK.addActionListener(e -> onOK());

        buttonCancel.addActionListener(e -> onCancel());

        DefaultListModel<ValidationItem> model = new DefaultListModel<>();
        for (PropertiesValidationErrorResult e : errors) {
            model.addElement(new ValidationItem(e));
        }
        errorsList.setModel(model);
        errorsList.addListSelectionListener(e -> buttonOK.setEnabled(errorsList.getSelectedValue() != null));
        if (model.size() > 0) {
            errorsList.setSelectedIndex(0);
        }

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        this.contentPane.registerKeyboardAction(e -> onCancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        new DoubleClickListener() {
            @Override
            protected boolean onDoubleClick(MouseEvent event) {
                int index = errorsList.locationToIndex(event.getPoint());
                if (index >= 0) {
                    onOK();
                    return true;
                }
                return false;
            }
        }.installOn(errorsList);

        pack();
        UIUtils.setLocationRelativeTo(this, project.getProject());
        setMinimumSize(getSize());
    }



    private static Class<? extends Configurable> getConfigurableClass(PropertiesValidationErrorResult err) {
        Class<? extends Configurable> pageclass;
        String type = err.errorType;
        if (type.startsWith(SakerIDEProject.NS_BUILD_DIRECTORY)) {
            pageclass = PathConfigurationConfigurable.class;
        } else if (type.startsWith(SakerIDEProject.NS_DAEMON_CONNECTION)) {
            pageclass = DaemonConnectionsConfigurable.class;
        } else if (type.startsWith(SakerIDEProject.NS_EXECUTION_DAEMON_NAME)) {
            pageclass = DaemonConnectionsConfigurable.class;
        } else if (type.startsWith(SakerIDEProject.NS_MIRROR_DIRECTORY)) {
            pageclass = PathConfigurationConfigurable.class;
        } else if (type.startsWith(SakerIDEProject.NS_PROVIDER_MOUNT)) {
            pageclass = PathConfigurationConfigurable.class;
        } else if (type.startsWith(SakerIDEProject.NS_REPOSITORY_CONFIGURATION)) {
            pageclass = TaskRepositoriesConfigurable.class;
        } else if (type.startsWith(SakerIDEProject.NS_SCRIPT_CONFIGURATION)) {
            pageclass = ScriptConfigurationConfigurable.class;
        } else if (type.startsWith(SakerIDEProject.NS_USER_PARAMETERS)) {
            pageclass = ExecutionUserParametersConfigureable.class;
        } else if (type.startsWith(SakerIDEProject.NS_WORKING_DIRECTORY)) {
            pageclass = PathConfigurationConfigurable.class;
        } else if (type.startsWith(SakerIDEProject.NS_SCRIPT_MODELLING_EXCLUSION)) {
            pageclass = ScriptConfigurationConfigurable.class;
        } else if (type.startsWith(SakerIDEProject.NS_BUILD_TRACE_OUT)) {
            pageclass = SakerBuildProjectConfigurable.class;
        } else {
            pageclass = SakerBuildProjectConfigurable.class;
        }
        return pageclass;
    }

    private void onOK() {
        dispose();
        ValidationItem selected = errorsList.getSelectedValue();
        if (selected != null) {
            Project project = this.project.getProject();
            PropertiesValidationErrorResult validationerror = selected.error;
            showSettingsForValidationError(project, validationerror);
        }
    }

    public static void showSettingsForValidationError(Project project,
            PropertiesValidationErrorResult validationerror) {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, getConfigurableClass(validationerror));
    }

    private void onCancel() {
        dispose();
    }

    private static class ValidationItem {
        protected PropertiesValidationErrorResult error;

        public ValidationItem(PropertiesValidationErrorResult error) {
            this.error = error;
        }

        @Override
        public String toString() {
            return SakerIDESupportUtils.createValidationErrorMessage(error);
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
        contentPane = new JPanel();
        contentPane.setLayout(new GridLayoutManager(3, 1, new Insets(10, 10, 10, 10), -1, -1));
        contentPane.setMinimumSize(new Dimension(400, 400));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1,
                new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
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
        buttonOK.setText("Edit");
        panel2.add(buttonOK,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonCancel = new JButton();
        buttonCancel.setText("Cancel ");
        panel2.add(buttonCancel,
                new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel3,
                new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        new Dimension(-1, 300), null, null, 0, false));
        errorsList = new JList();
        errorsList.setSelectionMode(0);
        panel3.add(errorsList, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50),
                null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("The following errors were detected in the saker.build project configuration:");
        contentPane.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }

}
