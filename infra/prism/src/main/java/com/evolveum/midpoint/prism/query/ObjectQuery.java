/*
 * Copyright (c) 2010-2013 Evolveum
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
	
	public static <T extends Objectable> boolean match(PrismObject<T> object, ObjectFilter filter, MatchingRuleRegistry matchingRuleRegistry) throws SchemaException{
		return filter.match(object, matchingRuleRegistry);
//		return false;
	}
	
	public ObjectQuery clone() {
		ObjectQuery clone = new ObjectQuery();
		if (this.filter != null) {
			clone.filter = this.filter.clone();
		}
		if (this.paging != null) {
			clone.paging = this.paging.clone();
		}
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
			sb.append("filter: null");
		} else {
			sb.append("filter:");
			sb.append("\n");
			sb.append(filter.debugDump(indent + 1));
		}
		
		sb.append("\n");
		DebugUtil.indentDebugDump(sb, indent);
		if (paging == null) {
			sb.append("paging: null");
		} else {
			sb.append("paging: ").append(paging.debugDump(0));
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
		return sb.toString();
	}

}
