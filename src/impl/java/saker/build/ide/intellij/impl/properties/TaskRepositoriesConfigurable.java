package saker.build.ide.intellij.impl.properties;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;
import saker.build.ide.intellij.impl.IntellijSakerIDEProject;
import saker.build.ide.support.properties.IDEProjectProperties;
import saker.build.ide.support.properties.RepositoryIDEProperty;
import saker.build.ide.support.properties.SimpleIDEProjectProperties;

import javax.swing.JComponent;
import java.util.Objects;
import java.util.Set;

public class TaskRepositoriesConfigurable implements Configurable, Configurable.NoScroll {
    private final SakerBuildProjectConfigurable parent;
    private RepositoryConfigurationForm form;

    public TaskRepositoriesConfigurable(SakerBuildProjectConfigurable parent) {
        this.parent = parent;
        form = new RepositoryConfigurationForm(this);
    }

    public SakerBuildProjectConfigurable getParent() {
        return parent;
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Task Repositories";
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

        if (!Objects.equals(currentprops.getRepositories(), properties.getRepositories())) {
            return true;
        }
        return false;
    }

    @Override
    public void apply() throws ConfigurationException {
    }
}
