package saker.build.ide.intellij.impl.editor;

import saker.build.file.path.SakerPath;
import saker.build.ide.support.ui.ScriptEditorModel;

public class IntellijScriptEditorModel extends ScriptEditorModel {
    public IntellijScriptEditorModel() {
    }

    public IntellijScriptEditorModel(SakerPath scriptExecutionPath) {
        super(scriptExecutionPath);
    }
}
