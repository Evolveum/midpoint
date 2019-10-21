/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * @author mederly
 */
public class TreeNode<T> implements DebugDumpable {

    private final List<TreeNode<T>> children = new ArrayList<>();
    private TreeNode<T> parent;
    private T userObject;

    public TreeNode() {
    }

    public TreeNode(T o) {
        userObject = o;
    }

    public List<TreeNode<T>> getChildren() {
        return children;
    }

    public TreeNode<T> getParent() {
        return parent;
    }

    public void add(TreeNode<T> newChild) {
        if (children.contains(newChild)) {
            return;
        }
        if (newChild.parent != null) {
            newChild.parent.remove(newChild);
        }
        children.add(newChild);
        newChild.parent = this;
    }

    private void remove(TreeNode<T> child) {
        children.remove(child);
        child.parent = null;
    }

    public T getUserObject() {
        return userObject;
    }

    @SuppressWarnings("unused")
    public void setUserObject(T userObject) {
        this.userObject = userObject;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(DebugUtil.debugDump(userObject, indent));
        for (TreeNode<T> child : children) {
            sb.append("\n");
            sb.append(child.debugDump(indent + 1));
        }
        return sb.toString();
    }

    public <N> TreeNode<N> transform(Function<T, N> transformation) {
        TreeNode<N> rv = new TreeNode<>(transformation.apply(userObject));
        for (TreeNode<T> child : children) {
            rv.add(child.transform(transformation));
        }
        return rv;
    }

    public void acceptDepthFirst(TreeNodeVisitor<T> visitor) {
        visitor.visit(this);
        children.forEach(child -> child.acceptDepthFirst(visitor));
    }

    public int getDepth() {
        return parent != null ? parent.getDepth() + 1 : 0;
    }
}
