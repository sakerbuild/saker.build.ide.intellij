package saker.build.ide.intellij.impl.properties.wizard;

import org.jetbrains.annotations.Nullable;
import saker.build.ide.support.ui.wizard.ClassPathNetworkArchiveSakerWizardPage;

import javax.swing.JComponent;

public class ClassPathNetworkArchiveWizardStep extends SakerWizardPageWizardStep<ClassPathNetworkArchiveSakerWizardPage> {
    private ClassPathNetworkArchiveWizardForm form;

    public ClassPathNetworkArchiveWizardStep(SakerWizardModel model,
            ClassPathNetworkArchiveSakerWizardPage wizardPage) {
        super("Select Network URL", "Specify the URL from where the archive should be loaded for the classpath.", model,
                wizardPage);
        this.form = new ClassPathNetworkArchiveWizardForm(this);
    }

    @Override
    protected JComponent getComponent() {
        return form.getRootPanel();
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return form.getUrlTextField();
    }
}
