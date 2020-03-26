package saker.build.ide.intellij.impl.properties.wizard;

import org.jetbrains.annotations.Nullable;
import saker.build.ide.support.ui.wizard.RepositoryIdentifierSakerWizardPage;

import javax.swing.JComponent;

public class RepositoryIdentifierWizardStep extends SakerWizardPageWizardStep<RepositoryIdentifierSakerWizardPage> {
    private RepositoryIdentifierWizardForm form;

    public RepositoryIdentifierWizardStep(SakerWizardModel model, RepositoryIdentifierSakerWizardPage wizardPage) {
        super("Choose Repository Identifier",
                "The specified identifier can be used to reference the repository from build scripts.", model,
                wizardPage);
        form = new RepositoryIdentifierWizardForm(this);
    }

    @Override
    protected JComponent getComponent() {
        return form.getRootPanel();
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return form.getIdentifierTextField();
    }
}
