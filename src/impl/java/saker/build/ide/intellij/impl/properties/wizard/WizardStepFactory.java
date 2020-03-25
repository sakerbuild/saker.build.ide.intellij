package saker.build.ide.intellij.impl.properties.wizard;

import saker.build.ide.support.ui.wizard.ClassPathFileChooserSakerWizardPage;
import saker.build.ide.support.ui.wizard.ClassPathNetworkArchiveSakerWizardPage;
import saker.build.ide.support.ui.wizard.ClassPathServiceEnumeratorSakerWizardPage;
import saker.build.ide.support.ui.wizard.ClassPathTypeChooserSakerWizardPage;
import saker.build.ide.support.ui.wizard.NestRepositoryVersionSakerWizardPage;
import saker.build.ide.support.ui.wizard.RepositoryIdentifierSakerWizardPage;
import saker.build.ide.support.ui.wizard.SakerWizardPage;
import saker.build.ide.support.ui.wizard.ScriptConfigurationSakerWizardPage;

public class WizardStepFactory {

    public static SakerWizardPageWizardStep<?> create(SakerWizardModel model, SakerWizardPage wizardpage,
            SakerWizardPageWizardStep<?> previous) {
        SakerWizardPageWizardStep<?> result;
        if (wizardpage instanceof ClassPathTypeChooserSakerWizardPage) {
            result = new ClassPathTypeChooserWizardStep(model, (ClassPathTypeChooserSakerWizardPage) wizardpage);
        } else if (wizardpage instanceof ClassPathFileChooserSakerWizardPage) {
            result = new ClassPathFileChooserWizardStep(model, (ClassPathFileChooserSakerWizardPage) wizardpage);
        } else if (wizardpage instanceof ClassPathNetworkArchiveSakerWizardPage) {
            result = new ClassPathNetworkArchiveWizardStep(model, (ClassPathNetworkArchiveSakerWizardPage) wizardpage);
        } else if (wizardpage instanceof NestRepositoryVersionSakerWizardPage) {
            result = new NestRepositoryVersionWizardStep(model, (NestRepositoryVersionSakerWizardPage) wizardpage);
        } else if (wizardpage instanceof ClassPathServiceEnumeratorSakerWizardPage) {
            result = new ServiceEnumeratorWizardStep(model, (ClassPathServiceEnumeratorSakerWizardPage) wizardpage);
        } else if (wizardpage instanceof ScriptConfigurationSakerWizardPage) {
            result = new ScriptConfigurationWizardStep(model, (ScriptConfigurationSakerWizardPage) wizardpage);
        } else if (wizardpage instanceof RepositoryIdentifierSakerWizardPage) {
            result = new RepositoryIdentifierWizardStep(model, (RepositoryIdentifierSakerWizardPage) wizardpage);
        } else {
            throw new UnsupportedOperationException(wizardpage.getClass().getName());
        }
        result.setPrevious(previous);
        return result;
    }
}
