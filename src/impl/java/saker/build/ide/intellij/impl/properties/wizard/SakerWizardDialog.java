package saker.build.ide.intellij.impl.properties.wizard;

import com.intellij.ui.wizard.WizardDialog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import javax.swing.JButton;
import java.awt.Component;

public class SakerWizardDialog extends WizardDialog<SakerWizardModel> {
    public SakerWizardDialog(Component parent, boolean canBeParent, SakerWizardModel model) {
        super(parent, canBeParent, model);
        this.setModal(true);
        model.setDialog(this);
    }

    @Nullable
    @Override
    public JButton getButton(@NotNull Action action) {
        return super.getButton(action);
    }
}
