package saker.build.ide.intellij.impl.properties;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurableProvider;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class SakerBuildApplicationConfigurableProvider extends ConfigurableProvider {
    @Nullable
    @Override
    public Configurable createConfigurable() {
        return new Configurable() {
            @Nls(capitalization = Nls.Capitalization.Title)
            @Override
            public String getDisplayName() {
                return "Hello settings";
            }

            @Nullable
            @Override
            public JComponent createComponent() {
                return new JLabel("Saker.build app configurations");
            }

            @Override
            public boolean isModified() {
                return false;
            }

            @Override
            public void apply() throws ConfigurationException {

            }
        };
    }
}
