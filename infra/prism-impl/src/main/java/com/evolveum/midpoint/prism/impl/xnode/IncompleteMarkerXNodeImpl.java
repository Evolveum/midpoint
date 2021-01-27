/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.xnode;

import com.evolveum.midpoint.prism.PrismNamespaceContext;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.xnode.IncompleteMarkerXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.DebugUtil;

/**
 *
 */
public class IncompleteMarkerXNodeImpl extends XNodeImpl implements IncompleteMarkerXNode {

    @Deprecated
    public IncompleteMarkerXNodeImpl() {
        super();
    }

    public IncompleteMarkerXNodeImpl(PrismNamespaceContext local) {
        super(local);
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public String getDesc() {
        return "incomplete";
    }

    @Override
    public void accept(Visitor<XNode> visitor) {
        visitor.visit(this);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("Incomplete");
        return sb.toString();
    }
}
