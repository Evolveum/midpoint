/*
 * Copyright (c) 2010-2015 Evolveum
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
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

import java.io.Serializable;

public class ObjectQuery implements DebugDumpable, Serializable {

	private ObjectFilter filter;
	private ObjectPaging paging;
	private boolean allowPartialResults = false;

	private boolean useNewQueryInterpreter = true;

	public ObjectFilter getFilter() {
		return filter;
	}

	public void setFilter(ObjectFilter filter) {
		this.filter = filter;
	}

	public void setPaging(ObjectPaging paging) {
		this.paging = paging;
	}

	public ObjectPaging getPaging() {
		return paging;
	}

	public boolean isAllowPartialResults() {
		return allowPartialResults;
	}

	public void setAllowPartialResults(boolean allowPartialResults) {
		this.allowPartialResults = allowPartialResults;
	}

	public boolean isUseNewQueryInterpreter() {
		return useNewQueryInterpreter;
	}

	public void setUseNewQueryInterpreter(boolean useNewQueryInterpreter) {
		this.useNewQueryInterpreter = useNewQueryInterpreter;
	}

	public static ObjectQuery createObjectQuery(ObjectFilter filter) {
		ObjectQuery query = new ObjectQuery();
		query.setFilter(filter);
		return query;
	}
	
	public static ObjectQuery createObjectQuery(XNode condition, ObjectFilter filter) {
		ObjectQuery query = new ObjectQuery();
		query.setFilter(filter);
		return query;
	}

	public static ObjectQuery createObjectQuery(ObjectPaging paging) {
		ObjectQuery query = new ObjectQuery();
		query.setPaging(paging);
		return query;
	}

	public static ObjectQuery createObjectQuery(ObjectFilter filter, ObjectPaging paging) {
		ObjectQuery query = new ObjectQuery();
		query.setFilter(filter);
		query.setPaging(paging);
		return query;
	}

	// although we do our best to match even incomplete relations (null, unqualified), ultimately
	// it is the client's responsibility to ensure relations in object and filter are normalized (namely: null -> org:default)
	public static <T extends Objectable> boolean match(PrismObject<T> object, ObjectFilter filter, MatchingRuleRegistry matchingRuleRegistry) throws SchemaException{
		return filter.match(object.getValue(), matchingRuleRegistry);
	}

	// although we do our best to match even incomplete relations (null, unqualified), ultimately
	// it is the client's responsibility to ensure relations in object and filter are normalized (namely: null -> org:default)
	public static boolean match(Containerable object, ObjectFilter filter, MatchingRuleRegistry matchingRuleRegistry) throws SchemaException{
		return filter.match(object.asPrismContainerValue(), matchingRuleRegistry);
	}
	
	@Override
    public ObjectQuery clone() {
		ObjectQuery clone = cloneEmpty();
		if (this.filter != null) {
			clone.filter = this.filter.clone();
		}
		return clone;
	}
	
	public ObjectQuery cloneEmpty() {
		ObjectQuery clone = new ObjectQuery();
		if (this.paging != null) {
			clone.paging = this.paging.clone();
		}
		if (this.allowPartialResults) {
			clone.allowPartialResults = true;
		}
		clone.useNewQueryInterpreter = this.useNewQueryInterpreter;
		return clone;
	}	

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		
		DebugUtil.indentDebugDump(sb, indent);
		if (filter == null) {
			sb.append("Filter: null");
		} else {
			sb.append("Filter:");
			sb.append("\n");
			sb.append(filter.debugDump(indent + 1));
		}
		
		sb.append("\n");
		DebugUtil.indentDebugDump(sb, indent);
		if (paging == null) {
			sb.append("Paging: null");
		} else {
			sb.append(paging.debugDump(0));
		}
		
		if (allowPartialResults) {
			sb.append("\n");
			DebugUtil.indentDebugDump(sb, indent);
			sb.append("Allow partial results");
		}
		
		return sb.toString();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Q{");
		if (filter != null) {
			sb.append(filter.toString());
			sb.append(",");
		} else {
			sb.append("null filter");
		}
		if (paging != null) {
			sb.append(paging.toString());
			sb.append(",");
		} else {
			sb.append("null paging");
		}
		if (allowPartialResults) {
			sb.append(",partial");
		}
		return sb.toString();
	}

	public void addFilter(ObjectFilter objectFilter) {
		if (objectFilter == null || objectFilter instanceof AllFilter) {
			// nothing to do
		} else if (filter == null || filter instanceof AllFilter) {
			setFilter(objectFilter);
		} else {
			setFilter(AndFilter.createAnd(objectFilter, filter));
		}
	}

	// use when offset/maxSize is expected
	public Integer getOffset() {
		if (paging == null) {
			return null;
		}
		if (paging.getCookie() != null) {
			throw new UnsupportedOperationException("Paging cookie is not supported here.");
		}
		return paging.getOffset();
	}

	// use when offset/maxSize is expected
	public Integer getMaxSize() {
		if (paging == null) {
			return null;
		}
		if (paging.getCookie() != null) {
			throw new UnsupportedOperationException("Paging cookie is not supported here.");
		}
		return paging.getMaxSize();
	}

	@SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
	public boolean equals(Object o) {
		return equals(o, true);
	}

	public boolean equivalent(Object o) {
		return equals(o, false);
	}

	public boolean equals(Object o, boolean exact) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		ObjectQuery that = (ObjectQuery) o;

		if (allowPartialResults != that.allowPartialResults)
			return false;
		if (useNewQueryInterpreter != that.useNewQueryInterpreter)
			return false;
		if (filter != null ? !filter.equals(that.filter, exact) : that.filter != null)
			return false;
		return paging != null ? paging.equals(that.paging, exact) : that.paging == null;

	}

	@Override
	public int hashCode() {
		int result = filter != null ? filter.hashCode() : 0;
		result = 31 * result + (paging != null ? paging.hashCode() : 0);
		result = 31 * result + (allowPartialResults ? 1 : 0);
		result = 31 * result + (useNewQueryInterpreter ? 1 : 0);
		return result;
	}
}
