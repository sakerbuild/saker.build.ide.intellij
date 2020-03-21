package saker.build.ide.intellij.impl.properties.wizard;

import org.jetbrains.annotations.Nullable;
import saker.build.ide.support.ui.wizard.ScriptConfigurationSakerWizardPage;

import javax.swing.JComponent;

public class ScriptConfigurationWizardStep extends SakerWizardPageWizardStep<ScriptConfigurationSakerWizardPage> {
    private ScriptConfigurationWizardForm form;

    public ScriptConfigurationWizardStep(SakerWizardModel model, ScriptConfigurationSakerWizardPage wizardPage) {
        super(model, wizardPage);
        form = new ScriptConfigurationWizardForm(this);
    }

    @Override
    protected JComponent getComponent() {
        return form.getRootPanel();
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return form.getScriptFilesWildcardTextField();
    }
}
