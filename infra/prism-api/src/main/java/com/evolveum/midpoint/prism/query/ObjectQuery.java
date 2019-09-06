/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.query;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.exception.SchemaException;

import java.io.Serializable;

/**
 *
 */
public interface ObjectQuery extends DebugDumpable, Serializable {

	ObjectFilter getFilter();

	void setFilter(ObjectFilter filter);

	void setPaging(ObjectPaging paging);

	ObjectPaging getPaging();

	boolean isAllowPartialResults();

	void setAllowPartialResults(boolean allowPartialResults);

	ObjectQuery clone();

	ObjectQuery cloneEmpty();

	void addFilter(ObjectFilter objectFilter);

	// use when offset/maxSize is expected
	Integer getOffset();

	// use when offset/maxSize is expected
	Integer getMaxSize();

	boolean equivalent(Object o);

	boolean equals(Object o, boolean exact);

	// TODO decide what to do with these static methods

	// although we do our best to match even incomplete relations (null, unqualified), ultimately
	// it is the client's responsibility to ensure relations in object and filter are normalized (namely: null -> org:default)
	static <T extends Objectable> boolean match(
			PrismObject<T> object, ObjectFilter filter, MatchingRuleRegistry matchingRuleRegistry) throws SchemaException {
		return filter.match(object.getValue(), matchingRuleRegistry);
	}

	// although we do our best to match even incomplete relations (null, unqualified), ultimately
	// it is the client's responsibility to ensure relations in object and filter are normalized (namely: null -> org:default)
	static boolean match(Containerable object, ObjectFilter filter, MatchingRuleRegistry matchingRuleRegistry) throws SchemaException{
		return filter.match(object.asPrismContainerValue(), matchingRuleRegistry);
	}



}
