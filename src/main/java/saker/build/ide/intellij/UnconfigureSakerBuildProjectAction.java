package saker.build.ide.intellij;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class UnconfigureSakerBuildProjectAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        SakerBuildPlugin.setSakerBuildProjectNatureEnabled(project, false);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        Project project = e.getProject();
        if (project == null) {
            e.getPresentation().setVisible(false);
            return;
        }
        if (!SakerBuildPlugin.isSakerBuildProjectNatureEnabled(project)) {
            e.getPresentation().setVisible(false);
            return;
        }
        e.getPresentation().setEnabledAndVisible(true);
    }
}
