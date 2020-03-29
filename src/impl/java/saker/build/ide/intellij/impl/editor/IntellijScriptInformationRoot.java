package saker.build.ide.intellij.impl.editor;

import saker.build.ide.support.ui.BaseScriptInformationRoot;
import saker.build.scripting.model.PartitionedTextContent;
import saker.build.scripting.model.ScriptCompletionProposal;
import saker.build.scripting.model.ScriptTokenInformation;
import saker.build.scripting.model.TextPartition;

public class IntellijScriptInformationRoot extends BaseScriptInformationRoot<IntellijScriptInformationEntry> {
    protected IntellijScriptInformationRoot() {
    }

    @Override
    protected IntellijScriptInformationEntry createInformationEntry(TextPartition partition) {
        return new IntellijScriptInformationEntry(partition);
    }

    public static IntellijScriptInformationRoot create(ScriptTokenInformation tokeninfo) {
        IntellijScriptInformationRoot result = new IntellijScriptInformationRoot();
        result.init(tokeninfo);
        return result;
    }

    public static IntellijScriptInformationRoot create(PartitionedTextContent textcontent) {
        IntellijScriptInformationRoot result = new IntellijScriptInformationRoot();
        result.init(textcontent);
        return result;
    }

    public static IntellijScriptInformationRoot create(ScriptCompletionProposal proposal) {
        IntellijScriptInformationRoot result = new IntellijScriptInformationRoot();
        result.init(proposal.getInformation(), proposal.getSchemaIdentifier(), proposal.getSchemaMetaData());
        return result;
    }
}
