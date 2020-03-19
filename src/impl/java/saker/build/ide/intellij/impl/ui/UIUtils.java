package saker.build.ide.intellij.impl.ui;

import javax.swing.*;

public class UIUtils {
    public static void selectAndFocusAll(JTextField tf) {
        if (tf == null) {
            return;
        }
        tf.requestFocus();
        tf.select(0, tf.getText().length());
    }
}
