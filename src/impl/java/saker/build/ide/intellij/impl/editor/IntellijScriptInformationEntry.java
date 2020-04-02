package saker.build.ide.intellij.impl.editor;

import saker.build.ide.intellij.extension.script.information.IScriptInformationEntry;
import saker.build.ide.support.ui.BaseScriptInformationEntry;
import saker.build.scripting.model.FormattedTextContent;
import saker.build.scripting.model.TextPartition;

public class IntellijScriptInformationEntry extends BaseScriptInformationEntry implements IScriptInformationEntry {
    private String iconSource;

    public IntellijScriptInformationEntry(TextPartition partition) {
        super(partition);
    }

    public IntellijScriptInformationEntry(String title, String subTitle, FormattedTextContent content) {
        super(title, subTitle, content);
    }

//    @Override
//    public void setIconSource(String sourceurl) {
//        if (sourceurl == null) {
//            this.iconSource = null;
//            return;
//        }
//        if (sourceurl.indexOf('\"') >= 0) {
//            throw new IllegalArgumentException("Cannot contain quotes: " + sourceurl);
//        }
//        this.iconSource = sourceurl;
//    }

    public String getIconSource() {
        return iconSource;
    }
}
