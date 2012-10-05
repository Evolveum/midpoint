/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.schema;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Element;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.prism.xml.ns._public.query_2.OrderDirectionType;
import com.evolveum.prism.xml.ns._public.query_2.PagingType;

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

	private static Element fillPropertyReference(String resolve) {
		XPathHolder xpath = new XPathHolder(resolve);
		return xpath.toElement(SchemaConstants.NS_C, "property");
	}
}
