package saker.build.ide.intellij.impl.properties.wizard;

import saker.build.ide.support.ui.wizard.ClassPathServiceEnumeratorSakerWizardPage;

import javax.swing.JComponent;

public class ServiceEnumeratorWizardStep extends SakerWizardPageWizardStep<ClassPathServiceEnumeratorSakerWizardPage> {
    private String defaultServiceClassName;
    private ServiceEnumeratorWizardForm form;

    public ServiceEnumeratorWizardStep(SakerWizardModel model, ClassPathServiceEnumeratorSakerWizardPage wizardPage,
            String defaultServiceClassName) {
        super(model, wizardPage);
        this.defaultServiceClassName = defaultServiceClassName;
        this.form = new ServiceEnumeratorWizardForm(this);
    }

    public String getDefaultServiceClassName() {
        return defaultServiceClassName;
    }

    @Override
    protected JComponent getComponent() {
        return form.getRootPanel();
    }
}
