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

import com.evolveum.prism.xml.ns._public.query_3.OrderDirectionType;
import com.evolveum.prism.xml.ns._public.query_3.PagingType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;



public class PagingConvertor {
	
	public static ObjectPaging createObjectPaging(PagingType pagingType){
		if (pagingType == null) {
			return null;
		}
        if (pagingType.getOrderBy() != null && pagingType.getGroupBy() != null) {
            return ObjectPaging.createPaging(pagingType.getOffset(), pagingType.getMaxSize(),
                    pagingType.getOrderBy().getItemPath(), toOrderDirection(pagingType.getOrderDirection()), pagingType.getGroupBy().getItemPath());
        }

		if (pagingType.getOrderBy() != null) {
			return ObjectPaging.createPaging(pagingType.getOffset(), pagingType.getMaxSize(),
					pagingType.getOrderBy().getItemPath(), toOrderDirection(pagingType.getOrderDirection()));

		} if (pagingType.getGroupBy() != null) {
            return ObjectPaging.createPaging(pagingType.getGroupBy().getItemPath());

        } else {
			return ObjectPaging.createPaging(pagingType.getOffset(), pagingType.getMaxSize());
		}
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
	
	public static PagingType createPagingType(ObjectPaging paging){
		if (paging == null){
			return null;
		}
		PagingType pagingType = new PagingType();
		pagingType
				.setOrderDirection(toOrderDirectionType(paging.getDirection()));
		pagingType.setMaxSize(paging.getMaxSize());
		pagingType.setOffset(paging.getOffset());
		if (paging.getOrderBy() != null) {
			pagingType.setOrderBy(new ItemPathType(paging.getOrderBy()));
		}
        if (paging.getGroupBy() != null) {
            pagingType.setGroupBy(new ItemPathType(paging.getGroupBy()));
        }
		return pagingType;
	}
	
	private static OrderDirectionType toOrderDirectionType(OrderDirection direction){
		if (direction == null){
			return null;
		}
		
		if (OrderDirection.ASCENDING == direction){
			return OrderDirectionType.ASCENDING;
		}
		if (OrderDirection.DESCENDING == direction){
			return OrderDirectionType.DESCENDING;
		}
		return null;
	}
}
