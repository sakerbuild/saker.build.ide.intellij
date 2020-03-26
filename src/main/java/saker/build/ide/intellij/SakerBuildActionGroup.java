package saker.build.ide.intellij;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SakerBuildActionGroup extends ActionGroup {
    public static final AnAction[] EMPTY_ANACTION_ARRAY = new AnAction[0];

    public SakerBuildActionGroup() {
        super("Saker.build", false);
    }

    @Override
    public boolean hideIfNoVisibleChildren() {
        return true;
    }

    @NotNull
    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
        if (e == null) {
            return EMPTY_ANACTION_ARRAY;
        }
        Project project = e.getProject();
        if (project == null) {
            return EMPTY_ANACTION_ARRAY;
        }
        ISakerBuildPluginImpl pluginimpl = SakerBuildPlugin.getPluginImpl();
        if (pluginimpl == null) {
            return EMPTY_ANACTION_ARRAY;
        }
        ISakerBuildProjectImpl intellijideproject = pluginimpl.getOrCreateProject(project);
        if (intellijideproject == null) {
            return EMPTY_ANACTION_ARRAY;
        }
        List<AnAction> result = new ArrayList<>();
        intellijideproject.addSakerBuildTargetsMenuActions(result);
        return result.toArray(EMPTY_ANACTION_ARRAY);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        Project project = e.getProject();
        if (SakerBuildPlugin.isSakerBuildProjectNatureEnabled(project)) {
            e.getPresentation().setEnabledAndVisible(true);
            return;
        }
        e.getPresentation().setVisible(false);
    }
}
