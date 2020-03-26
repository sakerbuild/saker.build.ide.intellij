package saker.build.ide.intellij;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.List;

public interface ISakerBuildProjectImpl {
    public Project getProject();

    public void buildAsync();

    public Configurable getProjectPropertiesConfigurable();

    public boolean isScriptModellingConfigurationAppliesTo(String localfilepath);

    public EditorHighlighter getEditorHighlighter(VirtualFile file, EditorColorsScheme colors);

    public void addSakerBuildTargetsMenuActions(List<AnAction> actions);
}
