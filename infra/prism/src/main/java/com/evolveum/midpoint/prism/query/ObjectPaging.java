/*
 * Copyright (c) 2010-2014 Evolveum
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

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ObjectPaging implements DebugDumpable, Serializable {
	
	private Integer offset;
	private Integer maxSize;
	@NotNull private final List<ObjectOrdering> ordering = new ArrayList<>();
	private String cookie;
	
	protected ObjectPaging() {
	}
	
	ObjectPaging(Integer offset, Integer maxSize) {
		this.offset = offset;
		this.maxSize = maxSize;
	}

	ObjectPaging(ItemPath orderBy, OrderDirection direction) {
		setOrdering(orderBy, direction);
	}

	ObjectPaging(Integer offset, Integer maxSize, ItemPath orderBy, OrderDirection direction) {
		this.offset = offset;
		this.maxSize = maxSize;
		setOrdering(orderBy, direction);
	}
	
	public static ObjectPaging createPaging(Integer offset, Integer maxSize){
		return new ObjectPaging(offset, maxSize);
	}
	
	public static ObjectPaging createPaging(Integer offset, Integer maxSize, QName orderBy, OrderDirection direction) {
		return new ObjectPaging(offset, maxSize, orderBy != null ? new ItemPath(orderBy) : null, direction);
	}

	public static ObjectPaging createPaging(Integer offset, Integer maxSize, ItemPath orderBy, OrderDirection direction) {
		return new ObjectPaging(offset, maxSize, orderBy, direction);
	}
	
	public static ObjectPaging createPaging(Integer offset, Integer maxSize, List<ObjectOrdering> orderings) {
		ObjectPaging paging = new ObjectPaging(offset, maxSize);
		paging.setOrdering(orderings);
		return paging;
	}

	public static ObjectPaging createPaging(ItemPath orderBy, OrderDirection direction) {
		return new ObjectPaging(orderBy, direction);
	}

	public static ObjectPaging createPaging(QName orderBy, OrderDirection direction) {
		return new ObjectPaging(new ItemPath(orderBy), direction);
	}
	
	public static ObjectPaging createEmptyPaging(){
		return new ObjectPaging();
	}

	// TODO rename to getPrimaryOrderingDirection
	public OrderDirection getDirection() {
		ObjectOrdering primary = getPrimaryOrdering();
		return primary != null ? primary.getDirection() : null;
	}

	// TODO rename to getPrimaryOrderingPath
	public ItemPath getOrderBy() {
		ObjectOrdering primary = getPrimaryOrdering();
		return primary != null ? primary.getOrderBy() : null;
	}

	public ObjectOrdering getPrimaryOrdering() {
		if (hasOrdering()) {
			return ordering.get(0);
		} else {
			return null;
		}
	}

	// TODO name?
	public List<ObjectOrdering> getOrderingInstructions() {
		return ordering;
	}

	public boolean hasOrdering() {
		return !ordering.isEmpty();
	}

	public void setOrdering(ItemPath orderBy, OrderDirection direction) {
		this.ordering.clear();
		addOrderingInstruction(orderBy, direction);
	}

	public void addOrderingInstruction(ItemPath orderBy, OrderDirection direction) {
		this.ordering.add(new ObjectOrdering(orderBy, direction));
	}

	public void addOrderingInstruction(QName orderBy, OrderDirection direction) {
		addOrderingInstruction(new ItemPath(orderBy), direction);
	}

	@SuppressWarnings("NullableProblems")
	public void setOrdering(ObjectOrdering... orderings) {
		this.ordering.clear();
		if (orderings != null) {
			this.ordering.addAll(Arrays.asList(orderings));
		}
	}

	public void setOrdering(Collection<ObjectOrdering> orderings) {
		this.ordering.clear();
		if (orderings != null) {
			this.ordering.addAll(orderings);
		}
	}

	public Integer getOffset() {
		return offset;
	}
	
	public void setOffset(Integer offset) {
		this.offset = offset;
	}

	public Integer getMaxSize() {
		return maxSize;
	}
	
	public void setMaxSize(Integer maxSize) {
		this.maxSize = maxSize;
	}
	
	/**
	 * Returns the paging cookie. The paging cookie is used for optimization of paged searches.
	 * The presence of the cookie may allow the data store to correlate queries and associate
	 * them with the same server-side context. This may allow the data store to reuse the same
	 * pre-computed data. We want this as the sorted and paged searches may be quite expensive.
	 * It is expected that the cookie returned from the search will be passed back in the options
	 * when the next page of the same search is requested.
	 * 
	 * It is OK to initialize a search without any cookie. If the datastore utilizes a re-usable
	 * context it will return a cookie in a search response.
	 */
	public String getCookie() {
		return cookie;
	}

	/**
	 * Sets paging cookie. The paging cookie is used for optimization of paged searches.
	 * The presence of the cookie may allow the data store to correlate queries and associate
	 * them with the same server-side context. This may allow the data store to reuse the same
	 * pre-computed data. We want this as the sorted and paged searches may be quite expensive.
	 * It is expected that the cookie returned from the search will be passed back in the options
	 * when the next page of the same search is requested.
	 * 
	 * It is OK to initialize a search without any cookie. If the datastore utilizes a re-usable
	 * context it will return a cookie in a search response.
	 */
	public void setCookie(String cookie) {
		this.cookie = cookie;
	}

	public ObjectPaging clone() {
		ObjectPaging clone = new ObjectPaging();
		copyTo(clone);
		return clone;
	}

	protected void copyTo(ObjectPaging clone) {
		clone.offset = this.offset;
		clone.maxSize = this.maxSize;
		clone.ordering.clear();
		clone.ordering.addAll(this.ordering);
		clone.cookie = this.cookie;
	}

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		sb.append("Paging:");
		if (getOffset() != null) {
			sb.append("\n");
			DebugUtil.indentDebugDump(sb, indent + 1);
			sb.append("Offset: " + getOffset());
		}
		if (getMaxSize() != null) {
			sb.append("\n");
			DebugUtil.indentDebugDump(sb, indent + 1);
			sb.append("Max size: " + getMaxSize());
		}
		if (hasOrdering()) {
			sb.append("\n");
			DebugUtil.indentDebugDump(sb, indent + 1);
			sb.append("Ordering: ").append(ordering);
		}
		if (getCookie() != null) {
			sb.append("\n");
			DebugUtil.indentDebugDump(sb, indent + 1);
			sb.append("Cookie: " + getCookie());
		}
		return sb.toString();
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("PAGING: ");
		if (getOffset() != null){
			sb.append("O: ");
			sb.append(getOffset());
			sb.append(",");
		}
		if (getMaxSize() != null){
			sb.append("M: ");
			sb.append(getMaxSize());
			sb.append(",");
		}
		if (hasOrdering()) {
			sb.append("ORD: ");
			sb.append(ordering);
			sb.append(", ");
		}
		if (getCookie() != null) {
			sb.append("C:");
			sb.append(getCookie());
		}
		
		return sb.toString();
	}

	@SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
	@Override
	public boolean equals(Object o) {
		return equals(o, true);
	}

	public boolean equals(Object o, boolean exact) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		ObjectPaging that = (ObjectPaging) o;

		if (offset != null ? !offset.equals(that.offset) : that.offset != null)
			return false;
		if (maxSize != null ? !maxSize.equals(that.maxSize) : that.maxSize != null)
			return false;
		if (ordering.size() != that.ordering.size()) {
			return false;
		}
		for (int i = 0; i < ordering.size(); i++) {
			ObjectOrdering oo1 = this.ordering.get(i);
			ObjectOrdering oo2 = that.ordering.get(i);
			if (!oo1.equals(oo2, exact)) {
				return false;
			}
		}
		return cookie != null ? cookie.equals(that.cookie) : that.cookie == null;
	}

	@Override
	public int hashCode() {
		int result = offset != null ? offset.hashCode() : 0;
		result = 31 * result + (maxSize != null ? maxSize.hashCode() : 0);
		result = 31 * result + ordering.hashCode();
		result = 31 * result + (cookie != null ? cookie.hashCode() : 0);
		return result;
	}
}
