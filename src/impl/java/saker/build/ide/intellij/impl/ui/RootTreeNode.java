package saker.build.ide.intellij.impl.ui;

import javax.swing.tree.TreeNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class RootTreeNode<TN extends TreeNode> implements TreeNode {
    private List<TN> nodes = new ArrayList<>();

    public void clear() {
        nodes.clear();
    }

    public int add(TN node) {
        int idx = nodes.size();
        nodes.add(node);
        return idx;
    }

    public void remove(TreeNode node) {
        nodes.remove(node);
    }

    public List<TN> getNodes() {
        return nodes;
    }

    @Override
    public TN getChildAt(int childIndex) {
        return nodes.get(childIndex);
    }

    @Override
    public int getChildCount() {
        return nodes.size();
    }

    @Override
    public TreeNode getParent() {
        return null;
    }

    @Override
    public int getIndex(TreeNode node) {
        return nodes.indexOf(node);
    }

    @Override
    public boolean getAllowsChildren() {
        return !nodes.isEmpty();
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public Enumeration children() {
        return Collections.enumeration(nodes);
    }
}
