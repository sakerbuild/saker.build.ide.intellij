package saker.build.ide.intellij.impl.properties.wizard;

import org.jetbrains.annotations.Nullable;
import saker.build.ide.support.ui.wizard.ClassPathFileChooserSakerWizardPage;

import javax.swing.JComponent;

public class ClassPathFileChooserWizardStep extends SakerWizardPageWizardStep<ClassPathFileChooserSakerWizardPage> {
    private ClassPathFileChooserWizardForm form;

    public ClassPathFileChooserWizardStep(SakerWizardModel model, ClassPathFileChooserSakerWizardPage wizardPage) {
        super(model, wizardPage);
        this.form = new ClassPathFileChooserWizardForm(this);
    }

    @Override
    protected JComponent getComponent() {
        return form.getRootPanel();
    }
    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return form.getArchivePathTextField();
    }
}
