/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.query;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.query.NotFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.util.exception.SchemaException;


public class NotFilterImpl extends UnaryLogicalFilterImpl implements NotFilter {

	public NotFilterImpl() {
	}

	public NotFilterImpl(ObjectFilter filter) {
		setFilter(filter);
	}

	public static NotFilter createNot(ObjectFilter filter) {
		return new NotFilterImpl(filter);
	}
	
	@SuppressWarnings("CloneDoesntCallSuperClone")
	@Override
	public NotFilterImpl clone() {
		return new NotFilterImpl(getFilter().clone());
	}
	
	@Override
	public NotFilter cloneEmpty() {
		return new NotFilterImpl();
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
