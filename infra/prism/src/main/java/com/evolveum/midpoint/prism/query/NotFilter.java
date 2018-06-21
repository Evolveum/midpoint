/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.prism.query;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.util.exception.SchemaException;


public class NotFilter extends UnaryLogicalFilter {

	public NotFilter() {
	}

	public NotFilter(ObjectFilter filter) {
		setFilter(filter);
	}

	public static NotFilter createNot(ObjectFilter filter) {
		return new NotFilter(filter);
	}
	
	@SuppressWarnings("CloneDoesntCallSuperClone")
	@Override
	public NotFilter clone() {
		return new NotFilter(getFilter().clone());
	}
	
	@Override
	public NotFilter cloneEmpty() {
		return new NotFilter();
	}

	@Override
	public boolean match(PrismContainerValue value, MatchingRuleRegistry matchingRuleRegistry) throws SchemaException {
		return !getFilter().match(value, matchingRuleRegistry);
	}

	@Override
	public boolean equals(Object obj, boolean exact) {
		return super.equals(obj, exact) && obj instanceof NotFilter;
	}
	
	@Override
	protected String getDebugDumpOperationName() {
		return "NOT";
	}

}
