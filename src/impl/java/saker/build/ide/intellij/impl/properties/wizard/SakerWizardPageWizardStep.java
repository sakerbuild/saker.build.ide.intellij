package saker.build.ide.intellij.impl.properties.wizard;

import com.intellij.ui.wizard.WizardNavigationState;
import com.intellij.ui.wizard.WizardStep;
import org.jetbrains.annotations.NotNull;
import saker.build.ide.support.ui.wizard.SakerWizardPage;
import saker.build.ide.support.ui.wizard.WizardPageHistoryLink;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import java.awt.Component;

public abstract class SakerWizardPageWizardStep<WPType extends SakerWizardPage> extends WizardStep<SakerWizardModel> {
    protected final SakerWizardModel model;
    protected SakerWizardPageWizardStep<?> previous;
    protected WPType wizardPage;
    protected WizardNavigationState navigationState;

    protected boolean formComplete = true;

    public SakerWizardPageWizardStep(@NotNull SakerWizardModel model, @NotNull WPType wizardPage) {
        this.model = model;
        this.wizardPage = wizardPage;
    }

    public SakerWizardPageWizardStep(String title, SakerWizardModel model, WPType wizardPage) {
        super(title);
        this.model = model;
        this.wizardPage = wizardPage;
    }

    public SakerWizardPageWizardStep(String title, String explanation, SakerWizardModel model, WPType wizardPage) {
        super(title, explanation);
        this.model = model;
        this.wizardPage = wizardPage;
    }

    public SakerWizardPageWizardStep(String title, String explanation, Icon icon, SakerWizardModel model,
            WPType wizardPage) {
        super(title, explanation, icon);
        this.model = model;
        this.wizardPage = wizardPage;
    }

    public SakerWizardPageWizardStep(String title, String explanation, Icon icon, String helpId, SakerWizardModel model,
            WPType wizardPage) {
        super(title, explanation, icon, helpId);
        this.model = model;
        this.wizardPage = wizardPage;
    }

    @NotNull
    public SakerWizardModel getModel() {
        return model;
    }

    @NotNull
    public WPType getWizardPage() {
        return wizardPage;
    }

    public void setPrevious(SakerWizardPageWizardStep<?> previous) {
        this.previous = previous;
    }

    public SakerWizardPageWizardStep<?> getPrevious() {
        return previous;
    }

    protected abstract JComponent getComponent();

    public boolean isFormComplete() {
        return formComplete;
    }

    public void setFormComplete(boolean formComplete) {
        this.formComplete = formComplete;
    }

    @Override
    public JComponent prepare(WizardNavigationState state) {
        navigationState = state;
        updateButtons();
        return getComponent();
    }

    @Override
    public SakerWizardPageWizardStep<?> onNext(SakerWizardModel model) {
        SakerWizardPage np = wizardPage.getNextPage();
        if (np == null) {
            return null;
        }
        WizardPageHistoryLink historylink = WizardPageHistoryLink.next(getParentHistoryLink(), wizardPage);
        while (true) {
            SakerWizardPage redirected = np.redirectPage(historylink);
            if (redirected == null) {
                break;
            }
            np = redirected;
        }

        return WizardStepFactory.create(model, np, this);
    }

    private WizardPageHistoryLink getParentHistoryLink() {
        if (previous == null) {
            return null;
        }
        return WizardPageHistoryLink.next(previous.getParentHistoryLink(), this.previous.getWizardPage());
    }

    @Override
    public SakerWizardPageWizardStep<?> onPrevious(SakerWizardModel model) {
        return previous;
    }

    protected void updateButtons() {
        if (navigationState == null) {
            return;
        }
        WizardPageHistoryLink parenthistorylink = getParentHistoryLink();
        boolean finishenabled = formComplete && wizardPage.canFinishWizard(parenthistorylink);
        boolean nextenabled = formComplete && wizardPage.getNextPage() != null;

        navigationState.FINISH.setEnabled(finishenabled);
        navigationState.NEXT.setEnabled(nextenabled);
        navigationState.PREVIOUS.setEnabled(previous != null);

        SakerWizardDialog dialog = model.getDialog();
        if (dialog != null) {
            JRootPane rootpane = dialog.getRootPane();

            Action defaction = null;
            if (nextenabled) {
                defaction = navigationState.NEXT;
            } else if (finishenabled) {
                defaction = navigationState.FINISH;
            }
            updateDefaultAction(rootpane, defaction);
        }
    }

    private void updateDefaultAction(JRootPane rootpane, Action defaction) {
        if (defaction == null) {
            return;
        }
        JButton currentdefbutton = rootpane.getDefaultButton();
        if (currentdefbutton != null) {
            Action currentaction = currentdefbutton.getAction();
            if (currentaction == defaction) {
                //the action is already default
                return;
            }
            if (currentaction == navigationState.CANCEL || currentaction == navigationState.PREVIOUS) {
                //just disallow the cancel and previous actions as default. they make no sense.
                rootpane.setDefaultButton(null);
            }
        }
        JButton b = findButtonWithAction(rootpane, defaction);
        if (b != null) {
            rootpane.setDefaultButton(b);
        }
    }

    private static JButton findButtonWithAction(JComponent component, Action action) {
        int len = component.getComponentCount();
        for (int i = 0; i < len; ++i) {
            Component c = component.getComponent(i);
            if (c instanceof JButton) {
                if (((JButton) c).getAction() == action) {
                    return (JButton) c;
                }
            } else if (c instanceof JComponent) {
                JButton subfound = findButtonWithAction((JComponent) c, action);
                if (subfound != null) {
                    return subfound;
                }
            }
        }
        return null;
    }
}
