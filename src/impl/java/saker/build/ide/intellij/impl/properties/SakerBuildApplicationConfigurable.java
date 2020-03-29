package saker.build.ide.intellij.impl.properties;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class SakerBuildApplicationConfigurable implements Configurable, Configurable.Composite {
    private Configurable[] configurables;

    public SakerBuildApplicationConfigurable() {
        this.configurables = new Configurable[] { new EnvironmentUserParametersConfigurable() };
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Saker.build";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        JPanel panel = new JPanel(new GridLayoutManager(1, 1));
        GridConstraints constraints = new GridConstraints();
        constraints.setAnchor(GridConstraints.ANCHOR_NORTHWEST);
        panel.add(new JLabel("See the sub-pages for configuring the saker.build plugin."), constraints);
        return panel;
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void apply() throws ConfigurationException {
    }

    @NotNull
    @Override
    public Configurable[] getConfigurables() {
        return configurables;
    }
}
