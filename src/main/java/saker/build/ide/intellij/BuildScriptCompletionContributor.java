package saker.build.ide.intellij;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionInitializationContext;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import org.jetbrains.annotations.NotNull;

public class BuildScriptCompletionContributor extends CompletionContributor {

    public BuildScriptCompletionContributor() {
    }

    @Override
    public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
        Editor editor = parameters.getEditor();
        if (editor instanceof EditorEx) {
            EditorHighlighter highlighter = ((EditorEx) editor).getHighlighter();
            if (highlighter instanceof IBuildScriptEditorHighlighter) {
                ((IBuildScriptEditorHighlighter) highlighter).performCompletion(parameters, result);
                return;
            }
        }
        super.fillCompletionVariants(parameters, result);
    }

    @Override
    public void beforeCompletion(@NotNull CompletionInitializationContext context) {
        super.beforeCompletion(context);
        //https://intellij-support.jetbrains.com/hc/en-us/community/posts/206752355-The-dreaded-IntellijIdeaRulezzz-string
        context.setDummyIdentifier("");
    }

}
