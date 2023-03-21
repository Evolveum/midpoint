/*
 * Copyright (C) 2022-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.page;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Page;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.core.util.lang.WicketObjects;
import org.apache.wicket.markup.html.debug.PageView;
import org.apache.wicket.util.io.IClusterable;
import org.apache.wicket.util.string.Strings;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Viliam Repan (lazyman).
 */
public class PageStructureDump {

    private boolean showBehaviors;

    private boolean showStatefulAndParentsOnly;

    public void dumpStructure(Page page, Writer writer) throws IOException {
        writer.write("Path;Tree;Stateless;Render time (ms);Size;Type;Model Object\n");
        writer.flush();

        buildTree(page, null, writer, 0);
    }

    public void dumpStructure(TreeNode node, Writer writer, int depth) throws IOException {
        dumpStructure(node, writer, depth, true);
    }

    public void dumpStructure(TreeNode node, Writer writer, int depth, boolean children) throws IOException {
        writer.write(node.getPath());
        writer.write(";");
        writer.write(StringUtils.repeat('*', depth));
        writer.write(";");
        writer.write(Boolean.toString(node.isStateless()));
        writer.write(";");
        writer.write(node.getRenderTime());
        writer.write(";");
        writer.write(Long.toString(node.getSize()));
        writer.write(";");
        writer.write(node.getModel());
        writer.write(";\n");

        writer.flush();

        if (children) {
            for (TreeNode child : node.children) {
                dumpStructure(child, writer, depth + 1);
            }
        }

        writer.flush();
    }

    private TreeNode buildTree(Component node, TreeNode parent, Writer writer, int depth) throws IOException {
        TreeNode treeNode = new TreeNode(node, parent);
        dumpStructure(treeNode, writer, depth, false);

        List<TreeNode> children = treeNode.children;

        // Add its behaviors
        if (showBehaviors) {
            for (Behavior behavior : node.getBehaviors()) {
                if (!showStatefulAndParentsOnly || !behavior.getStatelessHint(node)) {
                    TreeNode tn = new TreeNode(behavior, treeNode);
                    dumpStructure(tn, writer, depth + 1, false);
//                    children.add(tn);
                }
            }
        }

        // Add its children
        if (node instanceof MarkupContainer) {
            MarkupContainer container = (MarkupContainer) node;
            for (Component child : container) {
                buildTree(child, treeNode, writer, depth + 1);
            }
        }

        // Sort the children list, putting behaviors first
//        Collections.sort(children, new Comparator<TreeNode>() {
//
//            @Override
//            public int compare(TreeNode o1, TreeNode o2) {
//                if (o1.node instanceof Component) {
//                    if (o2.node instanceof Component) {
//                        return o1.getPath().compareTo((o2).getPath());
//                    } else {
//                        return 1;
//                    }
//                } else {
//                    if (o2.node instanceof Component) {
//                        return -1;
//                    } else {
//                        return o1.getPath().compareTo((o2).getPath());
//                    }
//                }
//            }
//        });
//
//        // Add this node to its parent if
//        // -it has children or
//        // -it is stateful or
//        // -stateless components are visible
//        if (parent != null &&
//                (!showStatefulAndParentsOnly || treeNode.hasChildren() || !node.isStateless())) {
//            parent.children.add(treeNode);
//        }
        return treeNode;
    }

    private static class TreeNode {
        public IClusterable node;
        public TreeNode parent;
        public List<TreeNode> children;

        public TreeNode(IClusterable node, TreeNode parent) {
            this.node = node;
            this.parent = parent;
            children = new ArrayList<>();
            if (!(node instanceof Component) && !(node instanceof Behavior)) {
                throw new IllegalArgumentException("Only accepts Components and Behaviors");
            }
        }

        public boolean hasChildren() {
            return !children.isEmpty();
        }

        /**
         * @return list of indexes to navigate from the root of the tree to this node (e.g. the path
         * to the node).
         */
        public List<Integer> getPathIndexes() {
            List<Integer> path = new ArrayList<>();
            TreeNode nextChild = this;
            TreeNode parent;
            while ((parent = nextChild.parent) != null) {
                int indexOf = parent.children.indexOf(nextChild);
                if (indexOf < 0) {throw new AssertionError("Child not found in parent");}
                path.add(indexOf);
                nextChild = parent;
            }
            Collections.reverse(path);
            return path;
        }

        public String getPath() {
            if (node instanceof Component) {
                return ((Component) node).getPath();
            } else {
                Behavior behavior = (Behavior) node;
                Component parent = (Component) this.parent.node;
                String parentPath = parent.getPath();
                int indexOf = parent.getBehaviors().indexOf(behavior);
                return parentPath + Component.PATH_SEPARATOR + "Behavior_" + indexOf;
            }
        }

        public String getRenderTime() {
            if (node instanceof Component) {
                Long renderDuration = ((Component) node).getMetaData(PageView.RENDER_KEY);
                if (renderDuration != null) {
                    return renderDuration.toString();
                }
            }
            return "n/a";
        }

        public long getSize() {
            if (node instanceof Component) {
                long size = ((Component) node).getSizeInBytes();
                return size;
            } else {
                long size = WicketObjects.sizeof(node);
                return size;
            }
        }

        public String getType() {
            // anonymous class? Get the parent's class name
            String type = node.getClass().getName();
            if (type.indexOf("$") > 0) {
                type = node.getClass().getSuperclass().getName();
            }
            return type;
        }

        public String getModel() {
            if (node instanceof Component) {
                String model;
                try {
                    model = ((Component) node).getDefaultModelObjectAsString();
                } catch (Exception e) {
                    model = e.getMessage();
                }
                return model;
            }
            return null;
        }

        public boolean isStateless() {
            if (node instanceof Page) {
                return ((Page) node).isPageStateless();
            } else if (node instanceof Component) {
                return ((Component) node).isStateless();
            } else {
                Behavior behavior = (Behavior) node;
                Component parent = (Component) this.parent.node;
                return behavior.getStatelessHint(parent);
            }
        }

        @Override
        public String toString() {
            if (node instanceof Page) {
                // Last component of getType() i.e. almost the same as getClass().getSimpleName();
                String type = getType();
                type = Strings.lastPathComponent(type, '.');
                return type;
            } else if (node instanceof Component) {
                return ((Component) node).getId();
            } else {
                // Last component of getType() i.e. almost the same as getClass().getSimpleName();
                String type = getType();
                type = Strings.lastPathComponent(type, '.');
                return type;
            }
        }
    }
}
