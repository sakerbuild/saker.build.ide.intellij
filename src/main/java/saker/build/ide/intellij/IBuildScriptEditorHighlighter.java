package saker.build.ide.intellij;

import com.intellij.ide.structureView.StructureView;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.psi.PsiFile;

public interface IBuildScriptEditorHighlighter {
    public String getDocumentationAtOffset(int offset);

    public StructureView createStructureView(PsiFile psiFile, FileEditor fileEditor);
}
