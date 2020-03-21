package saker.build.ide.intellij.impl.properties.wizard;

import org.jetbrains.annotations.Nullable;
import saker.build.ide.support.ui.wizard.NestRepositoryVersionSakerWizardPage;

import javax.swing.JComponent;

public class NestRepositoryVersionWizardStep extends SakerWizardPageWizardStep<NestRepositoryVersionSakerWizardPage> {
    private final NestRepositoryVersionWizardForm form;

    public NestRepositoryVersionWizardStep(SakerWizardModel model, NestRepositoryVersionSakerWizardPage wizardPage) {
        super(model, wizardPage);
        this.form = new NestRepositoryVersionWizardForm(this);
    }

    @Override
    protected JComponent getComponent() {
        return form.getRootPanel();
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return form.getVersionTextField();
    }
}
