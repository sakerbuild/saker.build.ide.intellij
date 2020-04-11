package saker.build.ide.intellij;

import com.intellij.lang.documentation.DocumentationProviderEx;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Image;
import java.util.List;

public class BuildScriptDocumentationProvider extends DocumentationProviderEx implements DumbAware {
    private static final Key<DocumentationHolder> KEY_DOCUMENTATION_HOLDER = Key.create("SAKER_DOCUMENTATION_HOLDER");

    @Nullable
    @Override
    public PsiElement getCustomDocumentationElement(@NotNull Editor editor, @NotNull PsiFile file,
            @Nullable PsiElement contextElement) {
        if (contextElement instanceof InvocationOffsetHolder && editor instanceof EditorEx) {
            EditorHighlighter highlighter = ((EditorEx) editor).getHighlighter();
            if (highlighter instanceof IBuildScriptEditorHighlighter) {
                int offset = ((InvocationOffsetHolder) contextElement).getInvocationOffset();
                IBuildScriptEditorHighlighter buildScriptEditorHighlighter = (IBuildScriptEditorHighlighter) highlighter;
                contextElement.putUserData(KEY_DOCUMENTATION_HOLDER, new DocumentationHolder() {
                    @Override
                    public String getDocumentation() {
                        return buildScriptEditorHighlighter.getDocumentationAtOffset(offset);
                    }
                });
                return contextElement;
            }
        }
        return super.getCustomDocumentationElement(editor, file, contextElement);
    }

    @Nullable
    @Override
    public Image getLocalImageForElement(@NotNull PsiElement element, @NotNull String imageSpec) {
        return super.getLocalImageForElement(element, imageSpec);
    }

    @Nullable
    @Override
    public String getQuickNavigateInfo(PsiElement element, PsiElement originalElement) {
        return null;
    }

    @Nullable
    @Override
    public List<String> getUrlFor(PsiElement element, PsiElement originalElement) {
        return null;
    }

    @Nullable
    @Override
    public String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
        DocumentationHolder docholder = element.getUserData(KEY_DOCUMENTATION_HOLDER);
        if (docholder != null) {
            return docholder.getDocumentation();
        }
        return null;
    }


    @Nullable
    //no @Override, as the method was introduced as a newer API
    public String generateHoverDoc(@NotNull PsiElement element, @Nullable PsiElement originalElement) {
        return generateDoc(element, originalElement);
    }

    @Nullable
    @Override
    public PsiElement getDocumentationElementForLookupItem(PsiManager psiManager, Object object, PsiElement element) {
        if (object instanceof DocumentationHolder) {
            element.putUserData(KEY_DOCUMENTATION_HOLDER, (DocumentationHolder) object);
            return element;
        }
        return null;
    }

    @Nullable
    @Override
    public PsiElement getDocumentationElementForLink(PsiManager psiManager, String link, PsiElement context) {
        return null;
    }
}
