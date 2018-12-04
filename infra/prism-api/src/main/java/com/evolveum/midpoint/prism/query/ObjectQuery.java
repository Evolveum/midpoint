/*
 * Copyright (c) 2010-2018 Evolveum
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
