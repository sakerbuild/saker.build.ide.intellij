package saker.build.ide.intellij.impl.properties.wizard;

import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBTextField;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.jetbrains.annotations.NotNull;
import saker.build.ide.support.properties.BuiltinScriptingLanguageServiceEnumeratorIDEProperty;
import saker.build.ide.support.properties.ClassPathServiceEnumeratorIDEProperty;
import saker.build.ide.support.properties.NamedClassClassPathServiceEnumeratorIDEProperty;
import saker.build.ide.support.properties.NestRepositoryFactoryServiceEnumeratorIDEProperty;
import saker.build.ide.support.properties.ServiceLoaderClassPathEnumeratorIDEProperty;
import saker.build.ide.support.ui.wizard.ClassPathServiceEnumeratorSakerWizardPage;
import saker.build.thirdparty.saker.util.ObjectUtils;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.event.DocumentEvent;
import java.awt.Dimension;
import java.awt.Insets;
import java.util.Objects;

public class ServiceEnumeratorWizardForm {
    public static final String WIZARD_CONFIGURATION_DEFAULT_SERVICE_LOADER_CLASS_NAME = "service.enumerator.default.service.loader.class.name";

    private JPanel rootPanel;
    private JRadioButton serviceLoaderRadioButton;
    private JRadioButton classNameRadioButton;
    private JBTextField serviceLoaderTextField;
    private JBTextField classNameTextField;

    private ServiceEnumeratorWizardStep wizardStep;

    public ServiceEnumeratorWizardForm(ServiceEnumeratorWizardStep wizardstep) {
        wizardStep = wizardstep;
        ClassPathServiceEnumeratorSakerWizardPage wizardpage = wizardstep.getWizardPage();
        ClassPathServiceEnumeratorIDEProperty property = wizardpage.getProperty();

        ButtonGroup buttongroup = new ButtonGroup();
        buttongroup.add(serviceLoaderRadioButton);
        buttongroup.add(classNameRadioButton);

        serviceLoaderRadioButton.addChangeListener(e -> {
            serviceLoaderTextField.setEnabled(serviceLoaderRadioButton.isSelected());
            updateWizardStep();
        });
        classNameRadioButton.addChangeListener(e -> {
            classNameTextField.setEnabled(classNameRadioButton.isSelected());
            updateWizardStep();
        });

        serviceLoaderTextField.setText(Objects.toString(wizardstep.getModel().getWizardManager()
                .getConfiguration(WIZARD_CONFIGURATION_DEFAULT_SERVICE_LOADER_CLASS_NAME), ""));

        DocumentAdapter documentadapter = new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                updateWizardStep();
            }
        };
        serviceLoaderTextField.getDocument().addDocumentListener(documentadapter);
        classNameTextField.getDocument().addDocumentListener(documentadapter);
        if (property != null) {
            property.accept(new ClassPathServiceEnumeratorIDEProperty.Visitor<Void, Void>() {
                @Override
                public Void visit(ServiceLoaderClassPathEnumeratorIDEProperty property, Void param) {
                    serviceLoaderTextField.setText(ObjectUtils.nullDefault(property.getServiceClass(), ""));
                    serviceLoaderRadioButton.setSelected(true);
                    return null;
                }

                @Override
                public Void visit(NamedClassClassPathServiceEnumeratorIDEProperty property, Void param) {
                    classNameTextField.setText(ObjectUtils.nullDefault(property.getClassName(), ""));
                    classNameRadioButton.setSelected(true);
                    return null;
                }

                @Override
                public Void visit(BuiltinScriptingLanguageServiceEnumeratorIDEProperty property, Void param) {
                    return null;
                }

                @Override
                public Void visit(NestRepositoryFactoryServiceEnumeratorIDEProperty property, Void param) {
                    return null;
                }
            }, null);
        }

        updateWizardStep();
    }

    public JRadioButton getSelectRadioButton() {
        if (serviceLoaderRadioButton.isSelected()) {
            return serviceLoaderRadioButton;
        }
        if (classNameRadioButton.isSelected()) {
            return classNameRadioButton;
        }
        return null;
    }

    private void updateWizardStep() {
        if (serviceLoaderRadioButton.isSelected()) {
            wizardStep.getWizardPage().setServiceLoader(serviceLoaderTextField.getText());
        } else if (classNameRadioButton.isSelected()) {
            wizardStep.getWizardPage().setNamedClass(classNameTextField.getText());
        } else {
            wizardStep.getWizardPage().unselect();
        }
        wizardStep.setFormComplete(isFormComplete());
        wizardStep.updateButtons();
    }

    private boolean isFormComplete() {
        if (serviceLoaderRadioButton.isSelected()) {
            if (serviceLoaderTextField.getText().isEmpty()) {
                return false;
            }
            return true;
        }
        if (classNameRadioButton.isSelected()) {
            if (classNameTextField.getText().isEmpty()) {
                return false;
            }
            return true;
        }
        return false;
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
        panel1.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        rootPanel.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0,
                false));
        serviceLoaderRadioButton = new JRadioButton();
        serviceLoaderRadioButton.setSelected(true);
        serviceLoaderRadioButton.setText("Service loader:");
        panel1.add(serviceLoaderRadioButton,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        classNameRadioButton = new JRadioButton();
        classNameRadioButton.setText("Class name:");
        panel1.add(classNameRadioButton,
                new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        serviceLoaderTextField = new JBTextField();
        panel1.add(serviceLoaderTextField,
                new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
                        0, false));
        classNameTextField = new JBTextField();
        classNameTextField.setEnabled(false);
        panel1.add(classNameTextField,
                new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
                        0, false));
        final Spacer spacer1 = new Spacer();
        rootPanel.add(spacer1,
                new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1,
                        GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return rootPanel;
    }

}
