package saker.build.ide.intellij;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.ide.structureView.StructureView;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

public interface IBuildScriptEditorHighlighter {
    public String getDocumentationAtOffset(int offset);

    public StructureView createStructureView(PsiFile psiFile, FileEditor fileEditor);

    public void performCompletion(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result);
}
