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

import com.evolveum.midpoint.xml.ns._public.common.common_1.OrderDirectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceType;
import com.evolveum.midpoint.xml.schema.SchemaConstants;
import com.evolveum.midpoint.xml.schema.XPathType;

/**
 * 
 * @author lazyman
 * 
 */
public abstract class PagingTypeFactory {
	
	public static PagingType createListAllPaging(OrderDirectionType order, String orderBy) {
		return createPaging(0, order, orderBy);
	}

	public static PagingType createPaging(int offset, OrderDirectionType order, String orderBy) {
		return createPaging(offset, Integer.MAX_VALUE, order, orderBy);
	}

	public static PagingType createPaging(int offset, int maxSize, OrderDirectionType order, String orderBy) {
		PagingType paging = new PagingType();
		PropertyReferenceType propertyReferenceType = fillPropertyReference(orderBy);
		paging.setOrderBy(propertyReferenceType);
		paging.setOffset(offset);
		paging.setMaxSize(Integer.MAX_VALUE);
		paging.setOrderDirection(order);

		return paging;
	}

	private static PropertyReferenceType fillPropertyReference(String resolve) {
		PropertyReferenceType property = new PropertyReferenceType();
		XPathType xpath = new XPathType(getPropertyName(resolve));
		property.setProperty(xpath.toElement(SchemaConstants.NS_C, "property"));
		return property;
	}

	private static String getPropertyName(String name) {
		if (null == name) {
			return "";
		}
		return StringUtils.lowerCase(name);
	}
}
