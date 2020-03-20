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

public class PathConfigurationConfigurable implements Configurable {
    private final IntellijSakerIDEProject project;
    private final PathConfigurationForm form;

    private Set<ProviderMountIDEProperty> mounts = Collections.emptySet();
    private String workingDirectoryProperty;
    private String buildDirectoryProperty;
    private String mirrorDirectoryProperty;

    public PathConfigurationConfigurable(SakerBuildProjectConfigurable parent) {
        this.project = parent.getProject();
        this.form = new PathConfigurationForm(project.getProject());
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
        IDEProjectProperties props = project.getIDEProjectProperties();
        form.reset(props);

        this.mounts = form.getMounts();
        this.workingDirectoryProperty = form.getWorkingDirectory();
        this.buildDirectoryProperty = form.getBuildDirectory();
        this.mirrorDirectoryProperty = form.getMirrorDirectory();
    }

    @Override
    public boolean isModified() {
        if (!Objects.equals(this.mounts, form.getMounts())) {
            return true;
        }
        if (!Objects.equals(this.workingDirectoryProperty, form.getWorkingDirectory())) {
            return true;
        }
        if (!Objects.equals(this.buildDirectoryProperty, form.getBuildDirectory())) {
            return true;
        }
        if (!Objects.equals(this.mirrorDirectoryProperty, form.getMirrorDirectory())) {
            return true;
        }
        return false;
    }

    @Override
    public void apply() throws ConfigurationException {
        project.setIDEProjectProperties(SimpleIDEProjectProperties.builder(project.getIDEProjectProperties())
                .setWorkingDirectory(form.getWorkingDirectory()).setBuildDirectory(form.getBuildDirectory())
                .setMirrorDirectory(form.getMirrorDirectory())
                .setMounts(ImmutableUtils.makeImmutableLinkedHashSet(form.getMounts())).build());
    }
}
