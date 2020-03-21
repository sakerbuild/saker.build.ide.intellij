package saker.build.ide.intellij.impl.properties.wizard;

import org.jetbrains.annotations.Nullable;
import saker.build.ide.support.ui.wizard.ClassPathTypeChooserSakerWizardPage;

import javax.swing.JComponent;

public class ClassPathTypeChooserWizardStep extends SakerWizardPageWizardStep<ClassPathTypeChooserSakerWizardPage> {
    private ClassPathTypeChooserWizardForm form;

    public ClassPathTypeChooserWizardStep(SakerWizardModel model, ClassPathTypeChooserSakerWizardPage wizardPage) {
        super(model, wizardPage);
        this.form = new ClassPathTypeChooserWizardForm(this);
    }

    public void setClassPathTypes(boolean repositorycp, boolean scriptcp) {
        form.setClassPathTypes(repositorycp, scriptcp);
    }

    @Override
    protected JComponent getComponent() {
        return form.getRootPanel();
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return form.getTypeList();
    }
}
