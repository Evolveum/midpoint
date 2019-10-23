/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.impl.xnode;

import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.DebugUtil;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Map;

public class RootXNodeImpl extends XNodeImpl implements RootXNode {

    @NotNull private QName rootElementName;
    private XNodeImpl subnode;

    public RootXNodeImpl(@NotNull QName rootElementName) {
        this.rootElementName = rootElementName;
    }

    public RootXNodeImpl(@NotNull QName rootElementName, XNode subnode) {
        this.rootElementName = rootElementName;
        this.subnode = (XNodeImpl) subnode;
    }

    public RootXNodeImpl(@NotNull Map.Entry<QName, XNodeImpl> entry) {
        Validate.notNull(entry.getKey());
        this.rootElementName = entry.getKey();
        this.subnode = entry.getValue();
    }

    // TODO consider if this is clean enough... The whole concept of root node (as child of XNode) has to be thought out
    @Override
    public QName getTypeQName() {
        if (typeQName != null) {
            return typeQName;
        } else if (subnode != null) {
            return subnode.getTypeQName();
        } else {
            return null;
        }
    }

    @Override
    public boolean isExplicitTypeDeclaration() {
        if (super.isExplicitTypeDeclaration()) {
            return true;
        } else {
            return subnode != null && subnode.isExplicitTypeDeclaration();
        }
    }

    @NotNull
    public QName getRootElementName() {
        return rootElementName;
    }

    public void setRootElementName(@NotNull QName rootElementName) {
        this.rootElementName = rootElementName;
    }

    public XNodeImpl getSubnode() {
        return subnode;
    }

    public void setSubnode(XNodeImpl subnode) {
        this.subnode = subnode;
    }

    @Override
    public boolean isEmpty() {
        return (subnode == null);
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
        if (subnode != null) {
            subnode.accept(visitor);
        }
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("ROOT ").append(rootElementName);
        String dumpSuffix = dumpSuffix();
        if (dumpSuffix != null) {
            sb.append(dumpSuffix);
        }
        if (subnode == null) {
            sb.append(": null");
        } else {
            sb.append("\n");
            sb.append(subnode.debugDump(indent + 1));
        }
        return sb.toString();
    }

    @Override
    public String getDesc() {
        return "root";
    }

    @Override
    public String toString() {
        return "XNode(root:"+subnode+")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RootXNodeImpl rootXNode = (RootXNodeImpl) o;

        if (rootElementName != null ? !rootElementName.equals(rootXNode.rootElementName) : rootXNode.rootElementName != null) {
            return false;
        }
        if (subnode != null ? !subnode.equals(rootXNode.subnode) : rootXNode.subnode != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = rootElementName != null ? rootElementName.hashCode() : 0;
        result = 31 * result + (subnode != null ? subnode.hashCode() : 0);
        return result;
    }

    public MapXNodeImpl toMapXNode() {
        MapXNodeImpl map = new MapXNodeImpl();
        map.put(rootElementName, subnode);
        if (subnode.getTypeQName() == null) {
            subnode.setTypeQName(getTypeQName());
        }
        return map;
    }

    @Override
    public RootXNodeImpl toRootXNode() {
        return this;
    }
}
