/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.OrFilter;
import com.evolveum.midpoint.util.exception.SchemaException;

public class OrFilterImpl extends NaryLogicalFilterImpl implements OrFilter {

	public OrFilterImpl(List<ObjectFilter> condition) {
		super(condition);
	}

	public static OrFilter createOr(ObjectFilter... conditions){
		List<ObjectFilter> filters = new ArrayList<>();
		Collections.addAll(filters, conditions);
		return new OrFilterImpl(filters);
	}
	
	public static OrFilter createOr(List<ObjectFilter> conditions){	
		return new OrFilterImpl(conditions);
	}
	
	@SuppressWarnings("CloneDoesntCallSuperClone")
	@Override
	public OrFilterImpl clone() {
		return new OrFilterImpl(getClonedConditions());
	}
	
	@Override
	public OrFilterImpl cloneEmpty() {
		return new OrFilterImpl(new ArrayList<>());
	}

	@Override
	protected String getDebugDumpOperationName() {
		return "OR";
	}

	@Override
	public boolean match(PrismContainerValue value, MatchingRuleRegistry matchingRuleRegistry) throws SchemaException {
		for (ObjectFilter filter : getConditions()){
			if (filter.match(value, matchingRuleRegistry)){
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean equals(Object obj, boolean exact) {
		return super.equals(obj, exact) && obj instanceof OrFilter;
	}

}
