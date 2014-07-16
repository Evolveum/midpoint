/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
		} else if (!residualPath.equivalent(other.residualPath))     // TODO: ok?
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "PartiallyResolvedDelta(item=" + delta + ", residualPath=" + residualPath + ")";
	}

}
