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

package com.evolveum.midpoint.test.util;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.testng.AssertJUnit;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import static org.testng.AssertJUnit.assertEquals;

/**
 * Unit test utilities.
 *
 * @author Radovan Semancik
 */
public class TestUtil {
	
	public static final int MAX_EXCEPTION_MESSAGE_LENGTH = 500;
	
	private static final Trace LOGGER = TraceManager.getTrace(TestUtil.class);

    public static <T> void assertPropertyValueSetEquals(Collection<PrismPropertyValue<T>> actual, T... expected) {
        Set<T> set = new HashSet<T>();
        for (PrismPropertyValue<T> value : actual) {
            set.add(value.getValue());
        }
        assertSetEquals(set, expected);
    }

    public static <T> void assertSetEquals(Collection<T> actual, T... expected) {
        assertSetEquals(null, actual, expected);
    }

    public static <T> void assertSetEquals(String message, Collection<T> actual, T... expected) {
        Set<T> expectedSet = new HashSet<T>();
        expectedSet.addAll(Arrays.asList(expected));
        Set<T> actualSet = new HashSet<T>();
        actualSet.addAll(actual);
        if (message != null) {
            assertEquals(message, expectedSet, actualSet);
        } else {
            assertEquals(expectedSet, actualSet);
        }
    }
    
    public static String getNodeOid(Node node) {
		Node oidNode = null;
		if ((null == node.getAttributes())
				|| (null == (oidNode = node.getAttributes().getNamedItem(
						SchemaConstants.C_OID_ATTRIBUTE.getLocalPart())))
				|| (StringUtils.isEmpty(oidNode.getNodeValue()))) {
			return null;
		}
		String oid = oidNode.getNodeValue();
		return oid;
	}
    
    public static void setAttribute(PrismObject<ShadowType> account, QName attrName, QName typeName, 
			PrismContext prismContext, String value) throws SchemaException {
		PrismContainer<Containerable> attributesContainer = account.findContainer(ShadowType.F_ATTRIBUTES);
		ResourceAttributeDefinition attrDef = new ResourceAttributeDefinition(attrName, attrName, typeName, prismContext);
		ResourceAttribute attribute = attrDef.instantiate();
		attribute.setRealValue(value);
		attributesContainer.add(attribute);
	}

	public static void assertElement(List<Object> elements, QName elementQName, String value) {
		for (Object element: elements) {
			QName thisElementQName = JAXBUtil.getElementQName(element);
			if (elementQName.equals(thisElementQName)) {
				if (element instanceof Element) {
					String thisElementContent = ((Element)element).getTextContent();
					if (value.equals(thisElementContent)) {
						return;
					} else {
						AssertJUnit.fail("Wrong value for element with name "+elementQName+"; expected "+value+"; was "+thisElementContent);
					}
				} else {
					throw new IllegalArgumentException("Unexpected type of element "+elementQName+": "+element.getClass());
				}
			}
		}
		AssertJUnit.fail("No element with name "+elementQName);
	}

	public static void assertExceptionSanity(ObjectAlreadyExistsException e) {
		LOGGER.debug("Excpetion (expected)", e, e);
		System.out.println("Excpetion (expected)");
		System.out.println(ExceptionUtils.getFullStackTrace(e));
		assert !e.getMessage().isEmpty() : "Empty exception message";
		assert e.getMessage().length() < MAX_EXCEPTION_MESSAGE_LENGTH : "Exception message too long ("
				+e.getMessage().length()+" characters): "+e.getMessage();
	}
}
