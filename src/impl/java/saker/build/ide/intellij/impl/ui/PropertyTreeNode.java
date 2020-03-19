package saker.build.ide.intellij.impl.ui;

import javax.swing.tree.TreeNode;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class PropertyTreeNode<PT> implements TreeNode {
    private TreeNode parent;
    protected PT property;

    protected List<TreeNode> children;

    public PropertyTreeNode(TreeNode parent) {
        this.parent = parent;
    }

    public void setProperty(PT property) {
        this.property = property;
    }

    public PT getProperty() {
        return property;
    }

    @Override
    public TreeNode getChildAt(int childIndex) {
        return children.get(childIndex);
    }

    @Override
    public int getChildCount() {
        return children.size();
    }

    @Override
    public TreeNode getParent() {
        return parent;
    }

    @Override
    public int getIndex(TreeNode node) {
        return children.indexOf(node);
    }

    @Override
    public boolean getAllowsChildren() {
        return true;
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public Enumeration<TreeNode> children() {
        return Collections.enumeration(children);
    }

}
