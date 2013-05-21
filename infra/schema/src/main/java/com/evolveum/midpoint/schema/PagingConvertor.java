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

package com.evolveum.midpoint.schema;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.prism.xml.ns._public.query_2.OrderDirectionType;
import com.evolveum.prism.xml.ns._public.query_2.PagingType;



public class PagingConvertor {
	
	public static ObjectPaging createObjectPaging(PagingType pagingType){
		if (pagingType == null){
			return null;
		}
		
		QName orderBy = null;
		if (pagingType.getOrderBy() != null){
			XPathHolder xpath = new XPathHolder(pagingType.getOrderBy());
			orderBy = ItemPath.getName(xpath.toItemPath().first());
		}
		
		return ObjectPaging.createPaging(pagingType.getOffset(), pagingType.getMaxSize(), orderBy, toOrderDirection(pagingType.getOrderDirection()));
		
	}

	
	private static OrderDirection toOrderDirection(OrderDirectionType directionType){
		if (directionType == null){
			return null;
		}
		
		if (OrderDirectionType.ASCENDING == directionType){
			return OrderDirection.ASCENDING;
		}
		if (OrderDirectionType.DESCENDING == directionType){
			return OrderDirection.DESCENDING;
		}
		return null;
	}
}
