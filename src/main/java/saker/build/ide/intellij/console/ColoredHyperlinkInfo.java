package saker.build.ide.intellij.console;

import java.awt.Color;

public interface ColoredHyperlinkInfo {

    public default Color getBackgroundColor() {
        return null;
    }
}
