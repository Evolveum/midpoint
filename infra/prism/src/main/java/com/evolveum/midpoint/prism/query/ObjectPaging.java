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

import java.io.Serializable;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.Dumpable;

public class ObjectPaging implements Dumpable, DebugDumpable, Serializable{
	
	private Integer offset;
	private Integer maxSize;
	private QName orderBy;
	private OrderDirection direction;

	
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
	
	public ObjectPaging clone() {
		return new ObjectPaging(offset, maxSize, orderBy, direction);
	}

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		sb.append("PAGING: \n");
		DebugUtil.indentDebugDump(sb, indent + 1);
		if (getOffset() != null) {
			sb.append("Offset: " + getOffset());
			sb.append("\n");
		}
		if (getMaxSize() != null) {
			sb.append("Max size: " + getMaxSize());
			sb.append("\n");
		}
		if (getOrderBy() != null) {
			sb.append("Order by: " + getOrderBy().toString());
			sb.append("\n");
		}
		if (getDirection() != null) {
			sb.append("Order direction: " + getDirection());
			sb.append("\n");
		}
		return sb.toString();
	}

	@Override
	public String dump() {
		return debugDump(0);
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
		}
		
		return sb.toString();
	}

}
