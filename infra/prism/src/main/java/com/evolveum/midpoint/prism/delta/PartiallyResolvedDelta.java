/**
 * Copyright (c) 2013 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2013 [name of copyright owner]
 */
package com.evolveum.midpoint.prism.delta;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;

/**
 * @author semancik
 *
 */
public class PartiallyResolvedDelta<V extends PrismValue> {
	
	private ItemDelta<V> delta;
	private ItemPath residualPath;
	
	public PartiallyResolvedDelta(ItemDelta<V> itemDelta, ItemPath residualPath) {
		super();
		this.delta = itemDelta;
		this.residualPath = residualPath;
	}

	public ItemDelta<V> getDelta() {
		return delta;
	}

	public void setDelta(ItemDelta<V> itemDelta) {
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
		} else if (!residualPath.equals(other.residualPath))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "PartiallyResolvedDelta(item=" + delta + ", residualPath=" + residualPath + ")";
	}

}
