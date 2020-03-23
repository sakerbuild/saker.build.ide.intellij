package saker.build.ide.intellij.impl.editor;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionInitializationContext;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import org.jetbrains.annotations.NotNull;
import saker.build.ide.intellij.DocumentationHolder;
import saker.build.scripting.model.CompletionProposalEdit;
import saker.build.scripting.model.CompletionProposalEditKind;
import saker.build.scripting.model.InsertCompletionProposalEdit;
import saker.build.scripting.model.ScriptCompletionProposal;
import saker.build.scripting.model.ScriptSyntaxModel;
import saker.build.thirdparty.saker.util.ObjectUtils;

import java.util.List;

public class BuildScriptCompletionContributor extends CompletionContributor {

    public BuildScriptCompletionContributor() {
    }

    @Override
    public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
        Editor editor = parameters.getEditor();
        if (editor instanceof EditorEx) {
            EditorHighlighter highlighter = ((EditorEx) editor).getHighlighter();
            if (highlighter instanceof BuildScriptEditorHighlighter) {
                performCompletion(parameters, result, (BuildScriptEditorHighlighter) highlighter);
                return;
            }
        }
        super.fillCompletionVariants(parameters, result);
    }

    private void performCompletion(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result,
            BuildScriptEditorHighlighter highlighter) {
        ScriptSyntaxModel model = highlighter.getModel();
        if (model == null) {
            return;
        }
        List<? extends ScriptCompletionProposal> proposals = model.getCompletionProposals(parameters.getOffset());
        for (ScriptCompletionProposal proposal : proposals) {
            addIntellijProposal(parameters, result, proposal);
        }

    }

    private void addIntellijProposal(CompletionParameters parameters, @NotNull CompletionResultSet result,
            ScriptCompletionProposal proposal) {
        List<? extends CompletionProposalEdit> changes = proposal.getTextChanges();
        if (ObjectUtils.isNullOrEmpty(changes)) {
            return;
        }
        //TODO support complex edits
        for (CompletionProposalEdit c : changes) {
            String ckind = c.getKind();
            if (!CompletionProposalEditKind.INSERT.equalsIgnoreCase(ckind)) {
                //non insert proposals not yet supported
                return;
            }
        }
        int count = changes.size();
        if (count != 1) {
            return;
        }
        for (int i = 0; i < count; i++) {
            CompletionProposalEdit c = changes.get(i);
            for (int j = i + 1; j < count; j++) {
                CompletionProposalEdit c2 = changes.get(j);
                if (CompletionProposalEdit.overlaps(c, c2)) {
                    System.err.println("Overlaps: " + c + " and " + c2);
                    //XXX display info?
                    //invalid proposal
                    return;
                }
            }
        }
        InsertCompletionProposalEdit edit = (InsertCompletionProposalEdit) changes.get(0);
        LookupElementBuilder builder = LookupElementBuilder.create(new DocumentationHolder() {
            @Override
            public String getDocumentation() {
                return BuildScriptEditorHighlighter.generateDocumentation(proposal.getInformation());
            }
        }, edit.getText());
        builder = builder.withPresentableText(proposal.getDisplayString());
        if (!ObjectUtils.isNullOrEmpty(proposal.getDisplayRelation())) {
            builder = builder.withTailText(" : " + proposal.getDisplayRelation());
        }
        if (!ObjectUtils.isNullOrEmpty(proposal.getDisplayType())) {
            builder = builder.withTypeText(proposal.getDisplayType());
        }
        builder = builder.withInsertHandler(new InsertHandler<LookupElement>() {
            @Override
            public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement item) {
                int editoffset = edit.getOffset();
                context.getDocument().replaceString(editoffset, context.getSelectionEndOffset(),
                        ObjectUtils.nullDefault(edit.getText(), ""));
                context.getEditor().getCaretModel().moveToOffset(proposal.getSelectionOffset());
                context.commitDocument();
            }
        });
        result.addElement(builder);
    }

    @Override
    public void beforeCompletion(@NotNull CompletionInitializationContext context) {
        super.beforeCompletion(context);
        //https://intellij-support.jetbrains.com/hc/en-us/community/posts/206752355-The-dreaded-IntellijIdeaRulezzz-string
        context.setDummyIdentifier("");
        Editor editor = context.getEditor();
        if (editor instanceof EditorEx) {
            EditorHighlighter highlighter = ((EditorEx) editor).getHighlighter();
            System.out.println("BuildScriptCompletionContributor.beforeCompletion " + highlighter);
        }
    }

}
