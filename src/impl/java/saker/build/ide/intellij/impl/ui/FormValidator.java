package saker.build.ide.intellij.impl.ui;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.ui.ComponentValidator;
import com.intellij.openapi.ui.ValidationInfo;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class FormValidator implements Disposable {
    public static final int REQUIRED = 1 << 0;

    private JButton okButton;
    private List<UpdateCallerComponentValidator> validators = new ArrayList<>();

    public FormValidator() {
    }

    public FormValidator(JButton okButton) {
        this.okButton = okButton;
    }

    public void setOkButton(JButton okButton) {
        this.okButton = okButton;
    }

    public void add(JTextField textfield, Supplier<? extends ValidationInfo> validator, int flags) {
        UpdateCallerComponentValidator componentvalidator = new UpdateCallerComponentValidator(textfield, flags);
        Supplier<? extends ValidationInfo> nvalidator = () -> {
            componentvalidator.performedValidation = true;
            return validator.get();
        };

        componentvalidator.withValidator(nvalidator).withFocusValidator(nvalidator)
                .andRegisterOnDocumentListener(textfield).installOn(textfield);
        validators.add(componentvalidator);
    }

    public void add(JTextField textfield, Supplier<? extends ValidationInfo> validator) {
        add(textfield, validator, 0);
    }

    public boolean canOk() {
        for (UpdateCallerComponentValidator validator : validators) {
            if (validator.isRequired() && !validator.performedValidation) {
                return false;
            }
        }
        return true;
    }

    public void revalidateFocusFirstErroneous() {
        revalidate();
        for (UpdateCallerComponentValidator validator : validators) {
            ValidationInfo info = validator.getValidationInfo();
            if (info == null) {
                continue;
            }
            if (!info.okEnabled) {
                info.component.requestFocus();
                return;
            }
            if (info.warning) {
                continue;
            }
            info.component.requestFocus();
            return;
        }
    }

    public void revalidate() {
        for (UpdateCallerComponentValidator validator : validators) {
            validator.revalidate();
        }
    }

    @Override
    public void dispose() {
    }

    private void updateOkButton() {
        if (okButton == null) {
            return;
        }
        for (UpdateCallerComponentValidator validator : validators) {
            ValidationInfo info = validator.getValidationInfo();
            if (info != null && !info.okEnabled) {
                okButton.setEnabled(false);
                return;
            }
        }
        okButton.setEnabled(true);
    }

    private class UpdateCallerComponentValidator extends ComponentValidator {
        protected boolean performedValidation;
        protected int flags;
        protected JComponent component;

        public UpdateCallerComponentValidator(JComponent component, int flags) {
            super(FormValidator.this);
            this.component = component;
            this.flags = flags;
        }

        public boolean isRequired() {
            return (flags & REQUIRED) == REQUIRED;
        }

        @Override
        public void updateInfo(@Nullable ValidationInfo info) {
            super.updateInfo(info);
            updateOkButton();
        }
    }
}
