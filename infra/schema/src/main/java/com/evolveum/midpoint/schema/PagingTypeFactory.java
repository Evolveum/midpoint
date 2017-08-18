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

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.prism.marshaller.ItemPathHolder;
import com.evolveum.prism.xml.ns._public.query_3.OrderDirectionType;
import com.evolveum.prism.xml.ns._public.query_3.PagingType;

/**
 *
 * @author lazyman
 *
 */
public abstract class PagingTypeFactory {

	public static PagingType createListAllPaging() {
		return createListAllPaging(OrderDirectionType.ASCENDING, "name");
	}

	public static PagingType createListAllPaging(OrderDirectionType order, String orderBy) {
		return createPaging(0, order, orderBy);
	}

	public static PagingType createPaging(int offset, OrderDirectionType order, String orderBy) {
		return createPaging(offset, Integer.MAX_VALUE, order, orderBy);
	}

	public static PagingType createPaging(int offset, int maxSize, OrderDirectionType order, String orderBy) {
		PagingType paging = new PagingType();
		paging.setOffset(offset);
		paging.setMaxSize(maxSize);

        if (order != null && StringUtils.isNotEmpty(orderBy)) {
            paging.setOrderBy(fillPropertyReference(orderBy));
            paging.setOrderDirection(order);
        }

		return paging;
	}

	private static ItemPathType fillPropertyReference(String resolve) {
		ItemPathHolder xpath = new ItemPathHolder(resolve);
		return new ItemPathType(xpath.toItemPath());
	}
}
