package saker.build.ide.intellij;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionInitializationContext;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;

public class BuildScriptCompletionContributor extends CompletionContributor implements DumbAware {

    public BuildScriptCompletionContributor() {
    }

    @Override
    public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
        if (parameters.isAutoPopup()) {
            //don't show for auto-popup as it may be generally annoying as intellij can invoke it on random tokens?
            return;
        }
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
        if (!"".equals(context.getDummyIdentifier())) {
            //there was an exception logged during testing, so we check if it has been alerady set:
            //java.lang.Throwable: Changing the dummy identifier twice, already changed by saker.build.ide.intellij.BuildScriptCompletionContributor@42d25149
            //	at com.intellij.openapi.diagnostic.Logger.error(Logger.java:145)
            //	at com.intellij.codeInsight.completion.CompletionInitializationUtil$1.setDummyIdentifier(CompletionInitializationUtil.java:72)
            //	at com.intellij.codeInsight.completion.WordCompletionContributor.beforeCompletion(WordCompletionContributor.java:48)
            //	at com.intellij.codeInsight.completion.CompletionInitializationUtil.runContributorsBeforeCompletion(CompletionInitializationUtil.java:80)
            //	at com.intellij.codeInsight.completion.CompletionInitializationUtil.lambda$createCompletionInitializationContext$0(CompletionInitializationUtil.java:54)
            //	at com.intellij.openapi.application.impl.ApplicationImpl.runWriteActionWithClass(ApplicationImpl.java:873)
            //	at com.intellij.openapi.application.impl.ApplicationImpl.runWriteAction(ApplicationImpl.java:899)
            //	at com.intellij.openapi.application.WriteAction.compute(WriteAction.java:113)
            //	at com.intellij.codeInsight.completion.CompletionInitializationUtil.createCompletionInitializationContext(CompletionInitializationUtil.java:44)
            context.setDummyIdentifier("");
        }
    }

}
