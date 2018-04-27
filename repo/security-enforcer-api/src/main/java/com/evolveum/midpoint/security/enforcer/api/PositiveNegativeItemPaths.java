/**
 * Copyright (c) 2018 Evolveum
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
package com.evolveum.midpoint.security.enforcer.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPath.CompareResult;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.ShortDumpable;

/**
 * @author semancik
 *
 */
public class PositiveNegativeItemPaths implements ShortDumpable {
	
	private List<ItemPath> includedItems = new ArrayList<>();
	private List<ItemPath> excludedItems = new ArrayList<>();
	private boolean allItems = false;
	
	public boolean isAllItems() {
		return allItems;
	}
	
	protected List<ItemPath> getIncludedItems() {
		return includedItems;
	}

	protected List<ItemPath> getExcludedItems() {
		return excludedItems;
	}

	public void collectItemPaths(Collection<ItemPath> newIncludedItems, Collection<ItemPath> newExcludedItems) {
		if (allItems) {
			return;
		}
		if (newIncludedItems.isEmpty() && newExcludedItems.isEmpty()) {
			allItems = true;
			return;
		}
		for (ItemPath newIncludedItem: newIncludedItems) {
			// TODO: better merging, consider subpaths
			includedItems.add(newIncludedItem);
		}
		if (excludedItems.isEmpty()) {
			excludedItems.addAll(newExcludedItems);
		} else {
			// Merging exceptItem is in fact intersection operation, not addition.
			// But we need to carefully consider subpaths.
			List<ItemPath> newItems = new ArrayList<>();
			Iterator<ItemPath> iterator = excludedItems.iterator();
			while (iterator.hasNext()) {
				ItemPath excludedItem = iterator.next();
				ItemPath replacementItem = null;
				boolean keep = false;
				for (ItemPath newExcludedItem: newExcludedItems) {
					CompareResult result = newExcludedItem.compareComplex(excludedItem);
					if (result == CompareResult.SUBPATH || result == CompareResult.EQUIVALENT) {
						// match, keep excludedItem in the list
						keep = true;
						break;
					}
					if (result == CompareResult.SUPERPATH) {
						// replace excludedItem with a more specific item
						replacementItem = newExcludedItem;
					}
				}
				if (!keep) {
					iterator.remove();
					if (replacementItem != null) {
						newItems.add(replacementItem);
					}
				}
			}
			excludedItems.addAll(newItems);
			if (excludedItems.isEmpty()) {
				allItems = true;
			}
		}
	}
	
	public boolean isApplicable(ItemPath nameOnlyItemPath) {
		if (allItems) {
			return true;
		}
		for (ItemPath includedItem: includedItems) {
			if (includedItem.isSubPathOrEquivalent(nameOnlyItemPath)) {
				return true;
			}
		}
		if (excludedItems.isEmpty()) {
			return false;
		}
		for (ItemPath excludedItem: excludedItems) {
			CompareResult result = excludedItem.compareComplex(nameOnlyItemPath);
			// This is tricky. We really want to exclude all related paths:
			// subpaths, superpaths and (obviously) the item itself
			// Exclusion of subpaths are quite obtious.
			// But we also need to exclude superpaths. If we don't to this
			// then we efficiently grant access to the superpath element which
			// will also apply to this element - and that would include it.
	        if (result != CompareResult.NO_RELATION) {
				return false;
			}
		}
		return true;
	}
	

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.getClass().getSimpleName()).append("(");;
		shortDump(sb);
		sb.append(")");
		return sb.toString();
	}

	@Override
	public void shortDump(StringBuilder sb) {
		if (allItems) {
			sb.append("[all]");
		} else {
			if (includedItems.isEmpty() && excludedItems.isEmpty()) {
				sb.append("[none]");
			} else {
				sb.append("included: ");
				dumpItems(sb, includedItems);
				sb.append("; excluded: ");
				dumpItems(sb, excludedItems);
			}
		}
	}

	protected void dumpItems(StringBuilder sb, List<ItemPath> items) {
		if (items.isEmpty()) {
			sb.append("[none]");
		} else {
			Iterator<ItemPath> iterator = items.iterator();
			while (iterator.hasNext()) {
				sb.append(PrettyPrinter.prettyPrint(iterator.next()));
				if (iterator.hasNext()) {
					sb.append(", ");
				}
			}
		}
	}

}
