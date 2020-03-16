package saker.build.ide.intellij.impl;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import saker.build.file.path.SakerPath;
import saker.build.ide.intellij.SakerBuildPlugin;
import saker.build.scripting.ScriptParsingFailedException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;

public class TargetsActionGroup extends ActionGroup {
    private static final AnAction[] EMPTY_ANACTION_ARRAY = new AnAction[0];

    public TargetsActionGroup() {
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
        IntellijSakerIDEPlugin ideplugin = IntellijSakerIDEPlugin.getInstance();
        if (ideplugin == null) {
            return EMPTY_ANACTION_ARRAY;
        }
        IntellijSakerIDEProject intellijideproject = ideplugin.getOrCreateProject(project);
        if (intellijideproject == null) {
            return EMPTY_ANACTION_ARRAY;
        }
        List<AnAction> result = new ArrayList<>();
        addTargetsMenu(intellijideproject, result);
        return result.toArray(EMPTY_ANACTION_ARRAY);
    }

    private void addTargetsMenu(IntellijSakerIDEProject intellijideproject, List<AnAction> result) {
        NavigableSet<SakerPath> filepaths = intellijideproject.getTrackedScriptPaths();
        if (filepaths.isEmpty()) {
            result.add(new AnAction("Add new build file") {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    //TODO add new build file
                    System.out.println("TargetsActionGroup.actionPerformed");
                }
            });
        } else {
            SakerPath workingdirpath = intellijideproject.getWorkingDirectoryExecutionPath();
            for (SakerPath buildfilepath : filepaths) {
                SakerPath relativepath = buildfilepath;
                if (workingdirpath != null) {
                    if (buildfilepath.startsWith(workingdirpath)) {
                        relativepath = workingdirpath.relativize(relativepath);
                    }
                }
                result.add(new ActionGroup(relativepath.toString(), true) {
                    @NotNull
                    @Override
                    public AnAction[] getChildren(@Nullable AnActionEvent e) {
                        List<AnAction> targetresult = new ArrayList<>();
                        appendTargetsToBuildFileMenu(targetresult);
                        return targetresult.toArray(EMPTY_ANACTION_ARRAY);
                    }

                    private void appendTargetsToBuildFileMenu(List<AnAction> targetresult) {
                        Set<String> scripttargets;
                        try {
                            scripttargets = intellijideproject.getScriptTargets(buildfilepath);
                        } catch (ScriptParsingFailedException ex) {
                            ex.printStackTrace();
                            targetresult.add(new AnAction("Failed to parse script file") {
                                @Override
                                public void actionPerformed(@NotNull AnActionEvent e) {
                                    //TODO dummy
                                }
                            });
                            return;
                        } catch (IOException ex) {
                            ex.printStackTrace();
                            targetresult.add(new AnAction("Failed to open script file") {
                                @Override
                                public void actionPerformed(@NotNull AnActionEvent e) {
                                    //TODO dummy
                                }
                            });
                            return;
                        }
                        if (scripttargets == null) {
                            targetresult.add(new AnAction("Script is not part of the configuration") {
                                @Override
                                public void actionPerformed(@NotNull AnActionEvent e) {
                                    //TODO dummy
                                }
                            });
                            return;
                        }
                        if (scripttargets.isEmpty()) {
                            targetresult.add(new AnAction("No targets found") {
                                @Override
                                public void actionPerformed(@NotNull AnActionEvent e) {
                                    //TODO dummy
                                }
                            });
                            return;
                        }
                        for (String target : scripttargets) {
                            targetresult.add(new AnAction(target) {
                                @Override
                                public void actionPerformed(@NotNull AnActionEvent e) {
                                    intellijideproject.buildAsync(buildfilepath, target);
                                }
                            });
                        }
                    }
                });
            }
        }
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
//        if (project == null) {
//            e.getPresentation().setVisible(false);
//            return;
//        }
//        ModuleManager modulemanager = ModuleManager.getInstance(project);
//        for (Module m : modulemanager.getModules()) {
//            if (FacetManager.getInstance(m).getFacetByType(SakerBuildFacetType.FACET_TYPE_ID) != null) {
//                e.getPresentation().setVisible(true);
//                return;
//            }
//        }
//        e.getPresentation().setVisible(false);
    }
}
