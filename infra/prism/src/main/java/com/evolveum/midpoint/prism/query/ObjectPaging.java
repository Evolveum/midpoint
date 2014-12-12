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

import java.io.Serializable;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;

public class ObjectPaging implements DebugDumpable, Serializable {
	
	private Integer offset;
	private Integer maxSize;
	private QName orderBy;
	private OrderDirection direction;
	private String cookie;
	
	ObjectPaging() {
		// TODO Auto-generated constructor stub
	}
	
	ObjectPaging(Integer offset, Integer maxSize){
		this.offset = offset;
		this.maxSize = maxSize;
	}
	
	ObjectPaging(Integer offset, Integer maxSize, QName orderBy, OrderDirection direction){
		this.offset = offset;
		this.maxSize = maxSize;
		this.orderBy = orderBy;
		this.direction = direction;
	}
	
	public static ObjectPaging createPaging(Integer offset, Integer maxSize){
		return new ObjectPaging(offset, maxSize);
	}
	
	public static ObjectPaging createPaging(Integer offset, Integer maxSize, QName orderBy, OrderDirection direction){
		return new ObjectPaging(offset, maxSize, orderBy, direction);
	}
	
	public static ObjectPaging createPaging(Integer offset, Integer maxSize, String orderBy, String namespace, OrderDirection direction){
		return new ObjectPaging(offset, maxSize, new QName(namespace, orderBy), direction);
	}
	
	public static ObjectPaging createEmptyPaging(){
		return new ObjectPaging();
	}
	
	public OrderDirection getDirection() {
		return direction;
	}
	public void setDirection(OrderDirection direction) {
		this.direction = direction;
	}
	
	public Integer getOffset() {
		return offset;
	}
	
	public void setOffset(Integer offset) {
		this.offset = offset;
	}
	
	public QName getOrderBy() {
		return orderBy;
	}
	
	public void setOrderBy(QName orderBy) {
		this.orderBy = orderBy;
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
		ObjectPaging clone = new ObjectPaging(offset, maxSize, orderBy, direction);
		clone.cookie = this.cookie;
		return clone;
	}

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		sb.append("PAGING:");
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
		if (getOrderBy() != null) {
			sb.append("\n");
			DebugUtil.indentDebugDump(sb, indent + 1);
			sb.append("Order by: " + getOrderBy().toString());
		}
		if (getDirection() != null) {
			sb.append("\n");
			DebugUtil.indentDebugDump(sb, indent + 1);
			sb.append("Order direction: " + getDirection());
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
		if (this == null){
			sb.append("null");
			return sb.toString();
		}
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
		if (getOrderBy() != null){
			sb.append("BY: ");
			sb.append(getOrderBy().getLocalPart());
			sb.append(", ");
		}
		if (getDirection() != null){
			sb.append("D:");
			sb.append(getDirection());
			sb.append(", ");
		}
		if (getCookie() != null) {
			sb.append("C:");
			sb.append(getCookie());
		}
		
		return sb.toString();
	}

}
