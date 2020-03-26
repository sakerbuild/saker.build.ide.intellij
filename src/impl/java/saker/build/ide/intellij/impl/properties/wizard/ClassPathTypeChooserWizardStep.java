package saker.build.ide.intellij.impl.properties.wizard;

import org.jetbrains.annotations.Nullable;
import saker.build.ide.support.ui.wizard.ClassPathTypeChooserSakerWizardPage;

import javax.swing.JComponent;

public class ClassPathTypeChooserWizardStep extends SakerWizardPageWizardStep<ClassPathTypeChooserSakerWizardPage> {
    private ClassPathTypeChooserWizardForm form;

    public ClassPathTypeChooserWizardStep(SakerWizardModel model, ClassPathTypeChooserSakerWizardPage wizardPage) {
        super("Choose Classpath Type", "Select the classpath type you want to configure.", model, wizardPage);
        this.form = new ClassPathTypeChooserWizardForm(this);
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
