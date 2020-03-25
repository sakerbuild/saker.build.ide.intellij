package saker.build.ide.intellij.impl.properties.wizard;

import com.intellij.openapi.project.Project;
import com.intellij.ui.wizard.WizardModel;
import saker.build.ide.support.properties.IDEProjectProperties;
import saker.build.ide.support.ui.wizard.BaseSakerWizardManager;
import saker.build.ide.support.ui.wizard.SakerWizardPage;

public class SakerWizardModel extends WizardModel {
    private IDEProjectProperties projectProperties;
    private Project project;
    private BaseSakerWizardManager<SakerWizardPage> wizardManager = new BaseSakerWizardManager<>();

    protected SakerWizardDialog dialog;

    public SakerWizardModel(String title, IDEProjectProperties projectProperties, Project project) {
        super(title);
        this.projectProperties = projectProperties;
        this.project = project;
    }

    public <T extends SakerWizardPage> T getWizardPage(Class<T> pageclass) {
        return wizardManager.getWizardPage(pageclass);
    }

    public BaseSakerWizardManager<SakerWizardPage> getWizardManager() {
        return wizardManager;
    }

    public SakerWizardDialog getDialog() {
        return dialog;
    }

    public void setDialog(SakerWizardDialog dialog) {
        this.dialog = dialog;
    }

    public void setProjectProperties(IDEProjectProperties projectProperties) {
        this.projectProperties = projectProperties;
    }

    public IDEProjectProperties getProjectProperties() {
        return projectProperties;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public Project getProject() {
        return project;
    }

    public void navigateToWizardPageOrEnd(SakerWizardPage page) {
        if (page == null) {
            return;
        }
        while (true) {
            SakerWizardPageWizardStep<?> cs = (SakerWizardPageWizardStep<?>) getCurrentStep();
            if (cs.getWizardPage() == page) {
                break;
            }
            if (getCurrentNavigationState().NEXT.isEnabled()) {
                next();
            } else {
                //start with the last page
                break;
            }
        }
    }
}
