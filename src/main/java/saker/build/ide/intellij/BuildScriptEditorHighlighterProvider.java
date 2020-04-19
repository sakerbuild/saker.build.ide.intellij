package saker.build.ide.intellij;

import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.ex.util.EmptyEditorHighlighter;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.fileTypes.EditorHighlighterProvider;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BuildScriptEditorHighlighterProvider implements EditorHighlighterProvider, DumbAware {
    @Override
    public EditorHighlighter getEditorHighlighter(@Nullable Project project, @NotNull FileType fileType,
            @Nullable VirtualFile virtualFile, @NotNull EditorColorsScheme colors) {
        if (fileType != BuildScriptFileType.INSTANCE || virtualFile == null) {
            return null;
        }
        if (!SakerBuildPlugin.isSakerBuildProjectNatureEnabled(project)) {
            return new EmptyEditorHighlighter(null);
        }
        return SakerBuildPlugin.getEditorHighlighter(project, virtualFile, colors);
    }
}
