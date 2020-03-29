package saker.build.ide.intellij.impl.dialog;

import com.intellij.openapi.project.Project;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.DoubleClickListener;
import com.intellij.ui.components.JBList;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import saker.build.file.path.SakerPath;
import saker.build.ide.intellij.impl.ui.UIUtils;
import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.build.thirdparty.saker.util.StringUtils;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

public class BuildTargetChooserDialog extends JDialog {
    private static final SakerPath DEFAULT_BUILD_FILE_PATH = SakerPath.valueOf("saker.build");

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JBList<BuildTargetItem> targetsList;

    private BuildTargetItem selectedItem;

    public BuildTargetChooserDialog(Project project, List<? extends BuildTargetItem> items) {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        setTitle("Choose Build Target");

        buttonOK.addActionListener(e -> onOK());

        buttonCancel.addActionListener(e -> onCancel());

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        targetsList.getEmptyText().clear().appendText("No build targets found.");
        targetsList.setModel(new CollectionListModel<>(items));

        if (!items.isEmpty()) {
            ArrayList<? extends BuildTargetItem> sellist = new ArrayList<>(items);
            sellist.sort((l, r) -> {
                if (DEFAULT_BUILD_FILE_PATH.equals(l.getDisplayPath())) {
                    if (DEFAULT_BUILD_FILE_PATH.equals(r.getDisplayPath())) {
                        if ("build".equals(l.getTarget())) {
                            return -1;
                        }
                        if ("build".equals(r.getTarget())) {
                            return 1;
                        }
                        return StringUtils.compareStringsNullFirst(l.getTarget(), r.getTarget());
                    }
                    return -1;
                }
                if (DEFAULT_BUILD_FILE_PATH.equals(r.getDisplayPath())) {
                    return 1;
                }
                int cmp = ObjectUtils.compareNullsFirst(l.getDisplayPath(), r.getDisplayPath());
                if (cmp != 0) {
                    return cmp;
                }
                if ("build".equals(l.getTarget())) {
                    return -1;
                }
                if ("build".equals(r.getTarget())) {
                    return 1;
                }
                return StringUtils.compareStringsNullFirst(l.getTarget(), r.getTarget());
            });
            targetsList.setSelectedIndex(items.indexOf(sellist.get(0)));
        } else {
            buttonOK.setEnabled(false);
        }

        new DoubleClickListener() {
            @Override
            protected boolean onDoubleClick(MouseEvent event) {
                onOK();
                return true;
            }
        }.installOn(targetsList);

        pack();
        UIUtils.setLocationRelativeTo(this, project);
        setMinimumSize(getSize());
    }

    public BuildTargetItem getSelectedItem() {
        return selectedItem;
    }

    private void onOK() {
        selectedItem = targetsList.getSelectedValue();
        dispose();
    }

    private void onCancel() {
        dispose();
    }

    public static class BuildTargetItem {
        private SakerPath scriptPath;
        private String target;
        private SakerPath displayPath;

        public BuildTargetItem(SakerPath scriptPath, String target, SakerPath displayPath) {
            this.scriptPath = scriptPath;
            this.target = target;
            this.displayPath = displayPath;
        }

        public SakerPath getScriptPath() {
            return scriptPath;
        }

        public String getTarget() {
            return target;
        }

        public SakerPath getDisplayPath() {
            return displayPath;
        }

        @Override
        public String toString() {
            return target + "@" + displayPath;
        }
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        contentPane = new JPanel();
        contentPane.setLayout(new GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));
        contentPane.setMinimumSize(new Dimension(300, 300));
        contentPane.setPreferredSize(new Dimension(300, 300));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1,
                new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null,
                        null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1, true, false));
        panel1.add(panel2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0,
                false));
        buttonOK = new JButton();
        buttonOK.setText("OK");
        panel2.add(buttonOK,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonCancel = new JButton();
        buttonCancel.setText("Cancel");
        panel2.add(buttonCancel,
                new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel3,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
                        0, false));
        targetsList = new JBList();
        targetsList.setSelectionMode(0);
        panel3.add(targetsList,
                new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
                        0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Choose a build target to run:");
        panel3.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }

}
