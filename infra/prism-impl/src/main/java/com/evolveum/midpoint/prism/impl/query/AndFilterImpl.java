/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.query;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.util.exception.SchemaException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AndFilterImpl extends NaryLogicalFilterImpl implements AndFilter {

	public AndFilterImpl(List<ObjectFilter> condition) {
		super(condition);
	}
	
	public static AndFilter createAnd(ObjectFilter... conditions){
		List<ObjectFilter> filters = new ArrayList<>(conditions.length);
		Collections.addAll(filters, conditions);
		return new AndFilterImpl(filters);
	}
	
	public static AndFilter createAnd(List<ObjectFilter> conditions){
		return new AndFilterImpl(conditions);
	}
	
	@SuppressWarnings("CloneDoesntCallSuperClone")
	@Override
	public AndFilterImpl clone() {
		return new AndFilterImpl(getClonedConditions());
	}
	
	@Override
	public AndFilterImpl cloneEmpty() {
		return new AndFilterImpl(new ArrayList<>());
	}
	
	@Override
	protected String getDebugDumpOperationName() {
		return "AND";
	}
	
	@Override
	public boolean match(PrismContainerValue value, MatchingRuleRegistry matchingRuleRegistry) throws SchemaException {
		for (ObjectFilter filter : getConditions()) {
			if (!filter.match(value, matchingRuleRegistry)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean equals(Object obj, boolean exact) {
		return super.equals(obj, exact) && obj instanceof AndFilter;
	}
}
