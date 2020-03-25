package saker.build.ide.intellij.impl.properties.wizard;

import com.intellij.ui.AddEditRemovePanel;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBTextField;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import saker.build.file.path.WildcardPath;
import saker.build.ide.intellij.impl.properties.UserParameterEditorDialog;
import saker.build.ide.intellij.impl.properties.UserParametersForm;
import saker.build.ide.intellij.impl.ui.SakerPropertyPageAddEditRemovePanel;
import saker.build.ide.support.ui.wizard.ScriptConfigurationSakerWizardPage;
import saker.build.thirdparty.saker.util.ObjectUtils;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.Dimension;
import java.awt.Insets;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class ScriptConfigurationWizardForm {
    private JPanel rootPanel;
    private JPanel optionsPanel;
    private JBTextField scriptFilesWildcardTextField;

    private ScriptConfigurationWizardStep wizardStep;
    private AddEditRemovePanel<Map.Entry<String, String>> optionsEditPanel;

    public ScriptConfigurationWizardForm(ScriptConfigurationWizardStep wizardstep) {
        wizardStep = wizardstep;
        ScriptConfigurationSakerWizardPage wizardpage = wizardstep.getWizardPage();
        Set<? extends Map.Entry<String, String>> options = wizardpage.getScriptOptions();
        String wildcard = ObjectUtils.nullDefault(wizardpage.getScriptsWildcard(), "");

        optionsEditPanel = new SakerPropertyPageAddEditRemovePanel<Map.Entry<String, String>>(
                new UserParametersForm.KeyValueTableModel(), ObjectUtils.newArrayList(options)) {
            @Nullable
            @Override
            protected Map.Entry<String, String> addItem() {
                UserParameterEditorDialog dialog = createDialog();
                dialog.setExistingKeys(getKeys());
                dialog.setVisible(true);

                return dialog.getEntry();
            }

            @Nullable
            @Override
            protected Map.Entry<String, String> editItem(Map.Entry<String, String> o) {
                UserParameterEditorDialog dialog = createDialog();
                dialog.setEntry(o);
                Set<String> keys = getKeys();
                keys.remove(o.getKey());
                dialog.setExistingKeys(keys);
                dialog.setVisible(true);

                return dialog.getEntry();
            }

            @NotNull
            private UserParameterEditorDialog createDialog() {
                UserParameterEditorDialog dialog = new UserParameterEditorDialog("Script Language Option", this);
                dialog.getTitleLabel().setText("Script Language Option");
                dialog.getInfoLabel().setText("Enter a script option for the language parser (-SO option).");
                dialog.setExistingKeyMessage("Option already exists for key.");
                dialog.setEmptyKeyMessage("Option key should not be empty.");
                return dialog;
            }
        };
        optionsEditPanel.getTable().getModel().addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                updateWizardStep();
            }
        });

        optionsPanel.add(optionsEditPanel,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW | GridConstraints.SIZEPOLICY_WANT_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null,
                        new Dimension(200, 200), null, 0, false));

        optionsEditPanel.getEmptyText().clear().appendText("No script options.");
        scriptFilesWildcardTextField.getEmptyText().clear().appendText("Script wildcard pattern");

        scriptFilesWildcardTextField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                updateWizardStep();
            }
        });

        scriptFilesWildcardTextField.setText(wildcard);

        updateWizardStep();
    }

    public JBTextField getScriptFilesWildcardTextField() {
        return scriptFilesWildcardTextField;
    }

    private Set<String> getKeys() {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (Map.Entry<String, String> entry : optionsEditPanel.getData()) {
            result.add(entry.getKey());
        }
        return result;
    }

    private void updateWizardStep() {
        wizardStep.getWizardPage().setData(scriptFilesWildcardTextField.getText(), getScriptOptions());
        wizardStep.setFormComplete(isFormComplete());
        wizardStep.updateButtons();
    }

    private Set<? extends Map.Entry<String, String>> getScriptOptions() {
        return new LinkedHashSet<>(optionsEditPanel.getData());
    }

    private boolean isFormComplete() {
        try {
            String wildcardtext = scriptFilesWildcardTextField.getText();
            if (wildcardtext.isEmpty()) {
                return false;
            }
            WildcardPath.valueOf(wildcardtext);
            return true;
        } catch (Exception e) {
            return false;
        }
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
        rootPanel.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        rootPanel.setMinimumSize(new Dimension(400, 400));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        rootPanel.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0,
                false));
        final JLabel label1 = new JLabel();
        label1.setText("Script files:");
        panel1.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        scriptFilesWildcardTextField = new JBTextField();
        panel1.add(scriptFilesWildcardTextField,
                new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
                        0, false));
        optionsPanel = new JPanel();
        optionsPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        rootPanel.add(optionsPanel,
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
