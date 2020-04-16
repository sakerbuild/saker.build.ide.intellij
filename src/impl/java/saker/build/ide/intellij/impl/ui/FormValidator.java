package saker.build.ide.intellij.impl.ui;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.ui.ComponentValidator;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.DocumentAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import saker.build.ide.intellij.util.PluginCompatUtil;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class FormValidator implements Disposable {
    public static final int REQUIRED = 1 << 0;
    public static final int START_ON_FOCUS_LOST = 1 << 1;

    private static final Method METHOD_COMPONENTVALIDATOR_WITHFOCUSVALIDATOR = PluginCompatUtil
            .getMethod(ComponentValidator.class, "withFocusValidator", Supplier.class);
    private static final Method METHOD_COMPONENTVALIDATOR_WITHVALIDATOR_SUPPLIER = PluginCompatUtil
            .getMethod(ComponentValidator.class, "withValidator", Supplier.class);

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
        UpdateCallerComponentValidator componentvalidator = createUpdateCallerComponentValidator(component, flags);
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

        if (METHOD_COMPONENTVALIDATOR_WITHVALIDATOR_SUPPLIER != null) {
            try {
                METHOD_COMPONENTVALIDATOR_WITHVALIDATOR_SUPPLIER.invoke(componentvalidator, nvalidator);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            componentvalidator.withValidator(val -> {
                val.updateInfo(nvalidator.get());
            });
        }
        if (METHOD_COMPONENTVALIDATOR_WITHFOCUSVALIDATOR != null) {
            try {
                METHOD_COMPONENTVALIDATOR_WITHFOCUSVALIDATOR.invoke(componentvalidator, focusvalidator);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (setup != null) {
            setup.accept(componentvalidator, component);
        }
        componentvalidator.installOn(component);

        validators.add(componentvalidator);
        return this;
    }

    public FormValidator add(JTextField textfield, Supplier<? extends ValidationInfo> validator, int flags) {
        return addForComponent(textfield, validator, flags, (updateCallerComponentValidator, textComponent) -> {
            //copied from ComponentValidator.andRegisterOnDocumentListener for compatibility
            textComponent.getDocument().addDocumentListener(new DocumentAdapter() {
                @Override
                protected void textChanged(@NotNull DocumentEvent e) {
                    // Don't use 'this' to avoid cyclic references.
                    ComponentValidator.getInstance(textComponent).ifPresent(ComponentValidator::revalidate);
                }
            });
        });
    }

    public FormValidator add(JTextField textfield, Supplier<? extends ValidationInfo> validator) {
        return add(textfield, validator, 0);
    }

    public <T> FormValidator add(JComboBox<T> combo, Supplier<? extends ValidationInfo> validator, int flags) {
        return addForComponent(combo, validator, flags, (v, c) -> {
            c.addItemListener(e -> {
                // Don't use 'this' to avoid cyclic references.
                ComponentValidator.getInstance(c).ifPresent(ComponentValidator::revalidate);
            });
        });
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
            ValidationInfo info = validator.getValidationInfoCompat();
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

    public void revalidateComponent(JComponent... components) {
        for (JComponent c : components) {
            for (UpdateCallerComponentValidator validator : validators) {
                if (validator.component == c) {
                    validator.enabled = true;
                    validator.revalidate();
                }
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
            ValidationInfo info = validator.getValidationInfoCompat();
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

    private abstract class UpdateCallerComponentValidator extends ComponentValidator {
        protected boolean performedValidation;
        protected int flags;
        protected JComponent component;
        protected boolean enabled = true;

        private UpdateCallerComponentValidator(JComponent component, int flags) {
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

        public abstract ValidationInfo getValidationInfoCompat();
    }

    private static final Method COMPONENTVALIDATOR_GETVALIDATIONINFO = PluginCompatUtil
            .getMethod(ComponentValidator.class, "getValidationInfo");

    private class NewUpdateCallerComponentValidator extends UpdateCallerComponentValidator {
        private NewUpdateCallerComponentValidator(JComponent component, int flags) {
            super(component, flags);
        }

        @Override
        public ValidationInfo getValidationInfoCompat() {
            try {
                return (ValidationInfo) COMPONENTVALIDATOR_GETVALIDATIONINFO.invoke(this);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e.getCause());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private class OldUpdateCallerComponentValidator extends UpdateCallerComponentValidator {
        private ValidationInfo validationInfo;

        private OldUpdateCallerComponentValidator(JComponent component, int flags) {
            super(component, flags);
        }

        @Override
        public void updateInfo(@Nullable ValidationInfo info) {
            this.validationInfo = info;
            super.updateInfo(info);
        }

        @Override
        public ValidationInfo getValidationInfoCompat() {
            return validationInfo;
        }
    }

    private UpdateCallerComponentValidator createUpdateCallerComponentValidator(JComponent component, int flags) {
        if (COMPONENTVALIDATOR_GETVALIDATIONINFO == null) {
            return new OldUpdateCallerComponentValidator(component, flags);
        }
        return new NewUpdateCallerComponentValidator(component, flags);
    }
}
