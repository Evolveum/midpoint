/*
 * Copyright (c) 2010-2014 Evolveum
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
package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.path.ItemPath;

/**
 * @author semancik
 *
 */
public class PartiallyResolvedItem<V extends PrismValue,D extends ItemDefinition> {

	private Item<V,D> item;
	private ItemPath residualPath;

	public PartiallyResolvedItem(Item<V,D> item, ItemPath residualPath) {
		super();
		this.item = item;
		this.residualPath = residualPath;
	}

	public Item<V,D> getItem() {
		return item;
	}

	public void setItem(Item<V,D> item) {
		this.item = item;
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
		result = prime * result + ((item == null) ? 0 : item.hashCode());
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
		PartiallyResolvedItem other = (PartiallyResolvedItem) obj;
		if (item == null) {
			if (other.item != null)
				return false;
		} else if (!item.equals(other.item))
			return false;
		if (residualPath == null) {
			if (other.residualPath != null)
				return false;
		} else if (!residualPath.equivalent(other.residualPath))        // TODO: ok?
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "PartiallyResolvedItem(item=" + item + ", residualPath=" + residualPath + ")";
	}

}
