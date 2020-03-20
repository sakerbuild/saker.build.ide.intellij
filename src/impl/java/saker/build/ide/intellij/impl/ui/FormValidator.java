package saker.build.ide.intellij.impl.ui;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.ui.ComponentValidator;
import com.intellij.openapi.ui.ValidationInfo;
import org.jetbrains.annotations.Nullable;
import saker.build.thirdparty.saker.util.function.Functionals;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class FormValidator implements Disposable {
    public static final int REQUIRED = 1 << 0;
    public static final int START_ON_FOCUS_LOST = 1 << 1;

    private JButton okButton;
    private List<UpdateCallerComponentValidator> validators = new ArrayList<>();

    public FormValidator() {
    }

    public FormValidator(JButton okButton) {
        this.okButton = okButton;
    }

    public FormValidator setOkButton(JButton okButton) {
        this.okButton = okButton;
        return this;
    }

    private <T extends JComponent> FormValidator addForComponent(T component,
            Supplier<? extends ValidationInfo> validator, int flags,
            BiConsumer<? super UpdateCallerComponentValidator, ? super T> setup) {
        UpdateCallerComponentValidator componentvalidator = new UpdateCallerComponentValidator(component, flags);
        ValidationPerformedFlaggerValidator nvalidator = new ValidationPerformedFlaggerValidator(componentvalidator,
                validator);

        Supplier<? extends ValidationInfo> focusvalidator = nvalidator;
        if ((flags & START_ON_FOCUS_LOST) == START_ON_FOCUS_LOST) {
            componentvalidator.enabled = false;
            focusvalidator = () -> {
                componentvalidator.enabled = true;
                return nvalidator.get();
            };
        }

        componentvalidator.withValidator(nvalidator).withFocusValidator(focusvalidator);

        if (setup != null) {
            setup.accept(componentvalidator, component);
        }

        validators.add(componentvalidator);
        return this;
    }

    public FormValidator add(JTextField textfield, Supplier<? extends ValidationInfo> validator, int flags) {
        return addForComponent(textfield, validator, flags,
                UpdateCallerComponentValidator::andRegisterOnDocumentListener);
    }

    public FormValidator add(JTextField textfield, Supplier<? extends ValidationInfo> validator) {
        return add(textfield, validator, 0);
    }

    public <T> FormValidator add(JComboBox<T> combo, Supplier<? extends ValidationInfo> validator, int flags) {
        return addForComponent(combo, validator, flags, Functionals.nullBiConsumer());
    }

    public <T> FormValidator add(JComboBox<T> combo, Supplier<? extends ValidationInfo> validator) {
        return add(combo, validator, 0);
    }

    public boolean canPerformOkRevalidateRefocus() {
        for (UpdateCallerComponentValidator validator : validators) {
            if (!validator.performedValidation) {
                validator.enabled = true;
                validator.revalidate();
            }
            ValidationInfo info = validator.getValidationInfo();
            if (info != null && !info.okEnabled) {
                info.component.requestFocus();
                return false;
            }
        }
        return true;
    }

    public void revalidate() {
        for (UpdateCallerComponentValidator validator : validators) {
            validator.enabled = true;
            validator.revalidate();
        }
    }

    public void revalidateComponent(JComponent component) {
        for (UpdateCallerComponentValidator validator : validators) {
            if (validator.component == component) {
                validator.enabled = true;
                validator.revalidate();
            }
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

    private static class ValidationPerformedFlaggerValidator implements Supplier<ValidationInfo> {
        private final UpdateCallerComponentValidator componentvalidator;
        private final Supplier<? extends ValidationInfo> validator;

        public ValidationPerformedFlaggerValidator(UpdateCallerComponentValidator componentvalidator,
                Supplier<? extends ValidationInfo> validator) {
            this.componentvalidator = componentvalidator;
            this.validator = validator;
        }

        @Override
        public ValidationInfo get() {
            if (!componentvalidator.enabled) {
                return null;
            }
            componentvalidator.performedValidation = true;
            return validator.get();
        }
    }

    private class UpdateCallerComponentValidator extends ComponentValidator {
        protected boolean performedValidation;
        protected int flags;
        protected JComponent component;
        protected boolean enabled = true;

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
