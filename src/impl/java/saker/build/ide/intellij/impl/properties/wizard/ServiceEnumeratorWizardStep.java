package saker.build.ide.intellij.impl.properties.wizard;

import org.jetbrains.annotations.Nullable;
import saker.build.ide.support.ui.wizard.ClassPathServiceEnumeratorSakerWizardPage;

import javax.swing.JComponent;
import javax.swing.JRadioButton;

public class ServiceEnumeratorWizardStep extends SakerWizardPageWizardStep<ClassPathServiceEnumeratorSakerWizardPage> {
    private ServiceEnumeratorWizardForm form;

    public ServiceEnumeratorWizardStep(SakerWizardModel model, ClassPathServiceEnumeratorSakerWizardPage wizardPage) {
        super("Service Loading Configuration", "Specify how the service provider should be loaded.", model, wizardPage);
        this.form = new ServiceEnumeratorWizardForm(this);
    }

    @Override
    protected JComponent getComponent() {
        return form.getRootPanel();
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        JRadioButton rb = form.getSelectRadioButton();
        if (rb != null) {
            return rb;
        }
        return super.getPreferredFocusedComponent();
    }
}
