package saker.build.ide.intellij;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class RunBuildAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        ISakerBuildPluginImpl plugin = SakerBuildPlugin.getPluginImpl();
        ISakerBuildProjectImpl ideproject = plugin.getOrCreateProject(project);
        if (ideproject == null) {
            return;
        }
        ideproject.buildAsync();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        Project project = e.getProject();
        if (project == null) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }
        if (!SakerBuildPlugin.isSakerBuildProjectNatureEnabled(project)) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }
        e.getPresentation().setEnabledAndVisible(true);
    }
}
