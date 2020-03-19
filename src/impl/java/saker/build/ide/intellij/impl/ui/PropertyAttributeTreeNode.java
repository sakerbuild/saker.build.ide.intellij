package saker.build.ide.intellij.impl.ui;

import saker.build.thirdparty.saker.util.ObjectUtils;

import javax.swing.tree.TreeNode;
import java.util.Collections;
import java.util.Enumeration;

public class PropertyAttributeTreeNode implements TreeNode {
    private TreeNode parent;
    private String property;
    private String value;
    private Object userData;

    public PropertyAttributeTreeNode(TreeNode parent, String property, String value) {
        this.parent = parent;
        this.property = property;
        this.value = value;
    }

    public void setUserData(Object userData) {
        this.userData = userData;
    }

    public Object getUserData() {
        return userData;
    }

    @Override
    public TreeNode getChildAt(int childIndex) {
        return null;
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public TreeNode getParent() {
        return parent;
    }

    @Override
    public int getIndex(TreeNode node) {
        return 0;
    }

    @Override
    public boolean getAllowsChildren() {
        return false;
    }

    @Override
    public boolean isLeaf() {
        return true;
    }

    @Override
    public Enumeration children() {
        return Collections.emptyEnumeration();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(property);
        sb.append(": ");
        if (ObjectUtils.isNullOrEmpty(value)) {
            sb.append("<no-value>");
        } else {
            sb.append(value);
        }
        return sb.toString();
    }
}
