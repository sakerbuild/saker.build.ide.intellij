package saker.build.ide.intellij.impl.editor;

import saker.build.ide.intellij.extension.script.information.IScriptInformationDesigner;
import saker.build.ide.intellij.extension.script.information.IScriptInformationRoot;

import java.util.List;

public final class MultiScriptInformationDesigner implements IScriptInformationDesigner {
    private final List<IScriptInformationDesigner> designers;

    public MultiScriptInformationDesigner(List<IScriptInformationDesigner> designers) {
        this.designers = designers;
    }

    @Override
    public void process(IScriptInformationRoot informationroot) {
        for (IScriptInformationDesigner designer : designers) {
            designer.process(informationroot);
        }
    }

}
