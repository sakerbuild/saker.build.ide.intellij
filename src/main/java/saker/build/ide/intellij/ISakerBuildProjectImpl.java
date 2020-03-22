package saker.build.ide.intellij;

import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

public interface ISakerBuildProjectImpl {
    public Project getProject();

    public void buildAsync();

    public Configurable getProjectPropertiesConfigurable();

    public boolean isScriptModellingConfigurationAppliesTo(String localfilepath);

    public EditorHighlighter getEditorHighlighter(VirtualFile file, EditorColorsScheme colors);
}
