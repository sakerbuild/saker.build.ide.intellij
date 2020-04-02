package saker.build.ide.intellij.impl.editor;

import saker.build.file.path.SakerPath;
import saker.build.ide.support.ui.ScriptEditorModel;

public class IntellijScriptEditorModel extends ScriptEditorModel {
    private BuildScriptEditorHighlighter owner;

    public IntellijScriptEditorModel() {
    }

    public IntellijScriptEditorModel(SakerPath scriptExecutionPath) {
        super(scriptExecutionPath);
    }

    public BuildScriptEditorHighlighter getOwner() {
        return owner;
    }

    public void setOwner(BuildScriptEditorHighlighter owner) {
        synchronized (inputAccessLock) {
            if (this.owner != null) {
                this.owner.disown(this);
            }
            this.owner = owner;
        }
    }
}
