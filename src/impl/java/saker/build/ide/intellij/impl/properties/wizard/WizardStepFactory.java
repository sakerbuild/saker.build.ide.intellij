package saker.build.ide.intellij.impl.properties.wizard;

import com.intellij.ui.wizard.WizardStep;
import saker.build.ide.support.ui.wizard.ClassPathFileChooserSakerWizardPage;
import saker.build.ide.support.ui.wizard.ClassPathNetworkArchiveSakerWizardPage;
import saker.build.ide.support.ui.wizard.ClassPathServiceEnumeratorSakerWizardPage;
import saker.build.ide.support.ui.wizard.ClassPathTypeChooserSakerWizardPage;
import saker.build.ide.support.ui.wizard.NestRepositoryVersionSakerWizardPage;
import saker.build.ide.support.ui.wizard.RepositoryIdentifierSakerWizardPage;
import saker.build.ide.support.ui.wizard.SakerWizardPage;
import saker.build.ide.support.ui.wizard.ScriptConfigurationSakerWizardPage;
import saker.build.runtime.repository.SakerRepositoryFactory;
import saker.build.scripting.ScriptAccessProvider;

public class WizardStepFactory {

    public static WizardStep<? extends SakerWizardModel> create(SakerWizardModel model, SakerWizardPage wizardpage,
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
        } else if (wizardpage instanceof ScriptServiceEnumeratorSakerWizardPage) {
            result = new ServiceEnumeratorWizardStep(model, (ClassPathServiceEnumeratorSakerWizardPage) wizardpage,
                    ScriptAccessProvider.class.getName());
        } else if (wizardpage instanceof RepositoryServiceEnumeratorSakerWizardPage) {
            result = new ServiceEnumeratorWizardStep(model, (ClassPathServiceEnumeratorSakerWizardPage) wizardpage,
                    SakerRepositoryFactory.class.getName());
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
