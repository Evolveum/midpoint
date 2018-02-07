/**
 * Copyright (c) 2017-2018 Evolveum
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
package com.evolveum.midpoint.security.enforcer.impl;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ItemFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Helper class to SecurityEnforcer, used to evaluate query item authorizations.
 *
 * @author semancik
 */
public class QueryAutzItemPaths extends AutzItemPaths {

	private static final Trace LOGGER = TraceManager.getTrace(QueryAutzItemPaths.class);

	private List<ItemPath> requiredItems = new ArrayList<>();

	public List<ItemPath> getRequiredItems() {
		return requiredItems;
	}

	public void addRequiredItem(ItemPath path) {
		requiredItems.add(path);
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
		if (isAllItems()) {
			return unsatisfiedItems;
		}
		for (ItemPath requiredItem: requiredItems) {
			if (ItemPath.containsEquivalent(getIncludedItems(), requiredItem)) {
				// allowed
				continue;
			}
			if (!getExcludedItems().isEmpty() && !ItemPath.containsEquivalent(getExcludedItems(), requiredItem)) {
				// not notAllowed = allowed
				continue;
			}
			unsatisfiedItems.add(requiredItem);
		}
		return unsatisfiedItems;
	}
	
	@Override
	public void shortDump(StringBuilder sb) {
		sb.append("required: ");
		dumpItems(sb, requiredItems);
		sb.append("; ");
		super.shortDump(sb);
	}
	
}
