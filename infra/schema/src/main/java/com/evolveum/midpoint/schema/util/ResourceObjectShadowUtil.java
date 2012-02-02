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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.schema.util;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;

/**
 * Methods that would belong to the ResourceObjectShadowType class but cannot go there
 * because of JAXB.
 * 
 * @author Radovan Semancik
 */
public class ResourceObjectShadowUtil {
	
	public static String getResourceOid(ResourceObjectShadowType shadow) {
		if (shadow.getResourceRef() != null) {
			return shadow.getResourceRef().getOid();
		} else if (shadow.getResource() != null) {
			return shadow.getResource().getOid();
		} else {
			return null;
		}
	}
	
	public static String getSingleStringAttributeValue(ResourceObjectShadowType shadow, QName attrName) {
		if (shadow.getAttributes() == null || shadow.getAttributes().getAny() == null ||
				shadow.getAttributes().getAny().isEmpty()) {
			return null;
		}
		String value = null;
		for (Object element : shadow.getAttributes().getAny()) {
			if (attrName.equals(JAXBUtil.getElementQName(element))) {
				if (value != null) {
					throw new IllegalArgumentException("Multiple values for attribute "+attrName+" in "+ObjectTypeUtil.toShortString(shadow)+" while expecting a single value");
				}
				if (element instanceof Element) {
					value = ((Element)element).getTextContent();
				} else {
					// JAXBObject, obviously not a string attribute
					throw new IllegalArgumentException("Found value "+element+" for attribute "+attrName+" in "+ObjectTypeUtil.toShortString(shadow)+" while expecting a string value");
				}
			}
		}
		return value;
	}

	public static List<Object> getAttributeValues(AccountShadowType shadow, QName attrName) {
		if (shadow.getAttributes() == null || shadow.getAttributes().getAny() == null ||
				shadow.getAttributes().getAny().isEmpty()) {
			return null;
		}
		List<Object> values = new ArrayList<Object>();
		for (Object element : shadow.getAttributes().getAny()) {
			if (attrName.equals(JAXBUtil.getElementQName(element))) {
				values.add(element);
			}
		}
		if (values.isEmpty()) {
			return null;
		}
		return values;
	}

	public static String getAttributeStringValue(AccountShadowType shadow, QName attrName) {
		List<Object> values = getAttributeValues(shadow, attrName);
		if (values.isEmpty()) {
			return null;
		}
		if (values.size() > 1) {
			throw new IllegalStateException("More than one value for "+attrName);
		}
		Object value = values.get(0);
		if (!(value instanceof Element)) {
			throw new IllegalStateException("Not an Element value for "+attrName);
		}
		Element element = (Element)value;
		return element.getTextContent();
	}

	
}
