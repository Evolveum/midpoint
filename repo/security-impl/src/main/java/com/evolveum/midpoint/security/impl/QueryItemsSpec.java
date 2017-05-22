/**
 * Copyright (c) 2017 Evolveum
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
package com.evolveum.midpoint.security.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ItemFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * Helper class to SecurityEnforcer, used to evaluate query item authorizations.
 * 
 * @author semancik
 */
public class QueryItemsSpec {
	
	private static final Trace LOGGER = TraceManager.getTrace(QueryItemsSpec.class);
	
	private List<ItemPath> requiredItems = new ArrayList<>();
	private List<ItemPath> allowedItems = new ArrayList<>();
	private boolean allItemsAllowed = false;
	
	public List<ItemPath> getRequiredItems() {
		return requiredItems;
	}
	
	public void addRequiredItem(ItemPath path) {
		requiredItems.add(path);
	}
	
	public List<ItemPath> getAllowedItems() {
		return allowedItems;
	}
	
	public void addAllowedItem(ItemPath path) {
		allowedItems.add(path);
	}

	public void addAllowedItems(Authorization autz) {
		if (autz.getItem().isEmpty()) {
			allItemsAllowed = true;
		} else {
			for (ItemPathType itemPathType: autz.getItem()) {
				addAllowedItem(itemPathType.getItemPath());
			}
		}
	}
	
	public void addRequiredItems(ObjectFilter filter) {
		filter.accept(visitable -> {
			if (visitable instanceof ItemFilter) {
				requiredItems.add(((ItemFilter)visitable).getFullPath());
			}
		});
	}

	
	public List<ItemPath> evaluateUnsatisfierItems() {
		List<ItemPath> unsatisfiedItems = new ArrayList<>();
		if (!allItemsAllowed) {
			for (ItemPath requiredItem: requiredItems) {
				if (!ItemPath.containsEquivalent(allowedItems, requiredItem)) {
					unsatisfiedItems.add(requiredItem);
				}
			}
		}
		return unsatisfiedItems;
	}

	public Object shortDump() {
		StringBuilder sb = new StringBuilder();
		sb.append("required: ");
		dumpItems(sb, requiredItems);
		sb.append("; allowed: ");
		if (allItemsAllowed) {
			sb.append("[all]");
		} else {
			dumpItems(sb, allowedItems);
		}
		return sb.toString();
	}

	private void dumpItems(StringBuilder sb, List<ItemPath> items) {
		if (items.isEmpty()) {
			sb.append("[none]");
		} else {
			Iterator<ItemPath> iterator = items.iterator();
			while (iterator.hasNext()) {
				sb.append(PrettyPrinter.prettyPrint(iterator.next()));
				if (iterator.hasNext()) {
					sb.append(",");
				}
			}
		}
	}
	
}
