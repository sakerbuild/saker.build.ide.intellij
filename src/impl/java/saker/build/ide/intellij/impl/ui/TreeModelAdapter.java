package saker.build.ide.intellij.impl.ui;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;

public abstract class TreeModelAdapter implements TreeModelListener {
    protected abstract void update(TreeModelEvent e);

    @Override
    public void treeNodesChanged(TreeModelEvent e) {
        update(e);
    }

    @Override
    public void treeNodesInserted(TreeModelEvent e) {
        update(e);
    }

    @Override
    public void treeNodesRemoved(TreeModelEvent e) {
        update(e);
    }

    @Override
    public void treeStructureChanged(TreeModelEvent e) {
        update(e);
    }
}
