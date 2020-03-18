package saker.build.ide.intellij.impl.properties;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;
import saker.build.ide.intellij.impl.IntellijSakerIDEProject;

import javax.swing.*;

public class TaskRepositoriesConfigurable implements Configurable {
    private final IntellijSakerIDEProject project;

    public TaskRepositoriesConfigurable(IntellijSakerIDEProject project) {
        this.project = project;
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Task Repositories";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        //TODO
        return new JLabel("TODO " + getDisplayName());
    }

    @Override
    public void reset() {

    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void apply() throws ConfigurationException {

    }
}
