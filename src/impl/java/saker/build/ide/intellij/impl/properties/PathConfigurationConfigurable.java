package saker.build.ide.intellij.impl.properties;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;
import saker.build.ide.intellij.impl.IntellijSakerIDEProject;
import saker.build.ide.support.properties.IDEProjectProperties;
import saker.build.ide.support.properties.ProviderMountIDEProperty;
import saker.build.ide.support.properties.SimpleIDEProjectProperties;
import saker.build.thirdparty.saker.util.ImmutableUtils;

import javax.swing.JComponent;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public class PathConfigurationConfigurable implements Configurable, Configurable.NoScroll {
    private final IntellijSakerIDEProject project;
    private final SakerBuildProjectConfigurable parent;
    private final PathConfigurationForm form;

    public PathConfigurationConfigurable(SakerBuildProjectConfigurable parent) {
        this.project = parent.getProject();
        this.parent = parent;
        this.form = new PathConfigurationForm(this);
    }

    public SakerBuildProjectConfigurable getParent() {
        return parent;
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Path Configuration";
    }

    @Override
    public void disposeUIResources() {
        form.dispose();
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        return form.getRootPanel();
    }

    @Override
    public void reset() {
        form.reset();
    }

    @Override
    public boolean isModified() {
        IDEProjectProperties currentprops = parent.getCurrentProjectProperties();
        IDEProjectProperties properties = parent.getProperties();

        if (!Objects.equals(currentprops.getMounts(), properties.getMounts())) {
            return true;
        }
        if (!Objects.equals(currentprops.getWorkingDirectory(), properties.getWorkingDirectory())) {
            return true;
        }
        if (!Objects.equals(currentprops.getBuildDirectory(), properties.getBuildDirectory())) {
            return true;
        }
        if (!Objects.equals(currentprops.getMirrorDirectory(), properties.getMirrorDirectory())) {
            return true;
        }
        return false;
    }

    @Override
    public void apply() throws ConfigurationException {
        parent.apply();
    }
}
