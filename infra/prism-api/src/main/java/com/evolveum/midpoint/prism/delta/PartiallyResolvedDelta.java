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
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((delta == null) ? 0 : delta.hashCode());
		result = prime * result + ((residualPath == null) ? 0 : residualPath.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PartiallyResolvedDelta other = (PartiallyResolvedDelta) obj;
		if (delta == null) {
			if (other.delta != null)
				return false;
		} else if (!delta.equals(other.delta))
			return false;
		if (residualPath == null) {
			if (other.residualPath != null)
				return false;
		} else if (!residualPath.equivalent(other.residualPath))     // TODO: ok?
			return false;
		return true;
	}

	@Override
	public String debugDump() {
		return debugDump(0);
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
