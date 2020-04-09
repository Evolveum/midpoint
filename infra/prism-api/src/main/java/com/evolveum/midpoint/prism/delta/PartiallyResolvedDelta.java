/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.delta;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;

import java.util.Objects;

/**
 * @author semancik
 *
 */
public class PartiallyResolvedDelta<V extends PrismValue,D extends ItemDefinition> implements DebugDumpable {

    private ItemDelta<V,D> delta;
    private ItemPath residualPath;

    public PartiallyResolvedDelta(ItemDelta<V,D> itemDelta, ItemPath residualPath) {
        super();
        this.delta = itemDelta;
        this.residualPath = residualPath;
    }

    public ItemDelta<V,D> getDelta() {
        return delta;
    }

    public void setDelta(ItemDelta<V,D> itemDelta) {
        this.delta = itemDelta;
    }

    public ItemPath getResidualPath() {
        return residualPath;
    }

    public void setResidualPath(ItemPath residualPath) {
        this.residualPath = residualPath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PartiallyResolvedDelta<?, ?> that = (PartiallyResolvedDelta<?, ?>) o;
        return Objects.equals(delta, that.delta) && Objects.equals(residualPath, that.residualPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(delta, residualPath);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("PartiallyResolvedDelta\n");
        DebugUtil.debugDumpWithLabel(sb, "delta", delta, indent + 1);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "residual path", residualPath==null?"null":residualPath.toString(), indent + 1);
        return sb.toString();
    }

    @Override
    public String toString() {
        return "PartiallyResolvedDelta(item=" + delta + ", residualPath=" + residualPath + ")";
    }

}
