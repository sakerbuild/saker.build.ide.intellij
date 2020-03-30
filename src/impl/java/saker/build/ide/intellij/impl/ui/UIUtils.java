package saker.build.ide.intellij.impl.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.WindowManager;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.tree.DefaultTreeCellRenderer;

public class UIUtils {
    public static void selectAndFocusAll(JTextField tf) {
        if (tf == null) {
            return;
        }
        tf.requestFocus();
        tf.select(0, tf.getText().length());
    }

    public static void setLocationRelativeTo(JDialog dialog, Project project) {
        if (project == null) {
            return;
        }
        WindowManager wm = WindowManager.getInstance();
        if (wm == null) {
            return;
        }
        JFrame projectframe = wm.getFrame(project);
        if (projectframe != null) {
            dialog.setLocationRelativeTo(projectframe);
            return;
        }
        IdeFrame ideframe = wm.getIdeFrame(project);
        if (ideframe != null) {
            JComponent ideframecomponent = ideframe.getComponent();
            if (ideframecomponent != null) {
                dialog.setLocationRelativeTo(ideframecomponent);
                return;
            }
        }
    }

    public static void setDefaultTreeCellRendererIcon(DefaultTreeCellRenderer renderer, Icon icon){
        renderer.setIcon(icon);
        renderer.setClosedIcon(icon);
        renderer.setLeafIcon(icon);
        renderer.setOpenIcon(icon);
        renderer.setDisabledIcon(icon);
    }
}
