/**
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
package com.evolveum.midpoint.prism.util;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.w3c.dom.Node;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.dom.PrismDomProcessor;
import com.evolveum.midpoint.prism.xml.PrismJaxbProcessor;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Set of prism-related asserts.
 * 
 * DO NOT use this in the main code. Although it is placed in "main" for convenience, is should only be used in tests.
 * 
 * @author Radovan Semancik
 *
 */
public class PrismAsserts {
	
	// VALUE asserts
	
	public static void assertPropertyValues(String message, Collection expected, Collection<PrismPropertyValue<Object>> results) {
		assertEquals(message, expected.size(), results.size());

        Set<Object> values = new HashSet<Object>();
        for (PrismPropertyValue result : results) {
            values.add(result.getValue());
        }
        assertEquals(message, expected, values);

//        Object[] array = expected.toArray();
//        PropertyValue[] array1 = new PropertyValue[results.size()];
//        results.toArray(array1);
//
//        for (int i=0;i<expected.size();i++) {
//            assertEquals(array[i], array1[i].getValue());
//        }
    }
	
	public static void assertReferenceValue(PrismReference ref, String oid) {
		for (PrismReferenceValue val: ref.getValues()) {
			if (oid.equals(val.getOid())) {
				return;
			}
		}
		fail("Oid "+oid+" not found in reference "+ref);
	}
	
	// DEFINITION asserts
	
	public static void assertDefinition(Item item, QName type, int minOccurs, int maxOccurs) {
		ItemDefinition definition = item.getDefinition();
		assertDefinition(definition, item.getName(), type, minOccurs, maxOccurs);
	}
		
	public static void assertPropertyDefinition(PrismContainerDefinition containerDef, QName propertyName, QName type, int minOccurs, int maxOccurs) {
		PrismPropertyDefinition definition = containerDef.findPropertyDefinition(propertyName);
		assertDefinition(definition, propertyName, type, minOccurs, maxOccurs);
	}
	
	public static void assertDefinition(ItemDefinition definition, QName itemName, QName type, int minOccurs, int maxOccurs) {
		assertNotNull("No definition for "+itemName, definition);
		assertEquals("Wrong definition type for "+itemName, type, definition.getTypeName());
		assertEquals("Wrong definition minOccurs for "+itemName, minOccurs, definition.getMinOccurs());
		assertEquals("Wrong definition maxOccurs for "+itemName, maxOccurs, definition.getMaxOccurs());
	}
	
	// DELTA asserts
	
	public static void assertPropertyReplace(ObjectDelta<?> userDelta, QName propertyName, Object... expectedValues) {
		PropertyDelta propertyDelta = userDelta.getPropertyDelta(propertyName);
		assertNotNull("Property delta for "+propertyName+" not found",propertyDelta);
		assertSet(propertyName, propertyDelta.getValuesToReplace(), expectedValues);
	}

	public static void assertPropertyAdd(ObjectDelta<?> userDelta, QName propertyName, Object... expectedValues) {
		PropertyDelta propertyDelta = userDelta.getPropertyDelta(propertyName);
		assertNotNull("Property delta for "+propertyName+" not found",propertyDelta);
		assertSet(propertyName, propertyDelta.getValuesToAdd(), expectedValues);
	}
	
	public static void assertPropertyDelete(ObjectDelta<?> userDelta, QName propertyName, Object... expectedValues) {
		PropertyDelta propertyDelta = userDelta.getPropertyDelta(propertyName);
		assertNotNull("Property delta for "+propertyName+" not found",propertyDelta);
		assertSet(propertyName, propertyDelta.getValuesToDelete(), expectedValues);
	}

	public static void assertPropertyReplace(ObjectDelta<?> userDelta, PropertyPath propertyPath, Object... expectedValues) {
		PropertyDelta propertyDelta = userDelta.getPropertyDelta(propertyPath);
		assertNotNull("Property delta for "+propertyPath+" not found",propertyDelta);
		assertSet(propertyPath.last().getName(), propertyDelta.getValuesToReplace(), expectedValues);
	}

	public static void assertPropertyAdd(ObjectDelta<?> userDelta, PropertyPath propertyPath, Object... expectedValues) {
		PropertyDelta propertyDelta = userDelta.getPropertyDelta(propertyPath);
		assertNotNull("Property delta for "+propertyPath+" not found",propertyDelta);
		assertSet(propertyPath.last().getName(), propertyDelta.getValuesToAdd(), expectedValues);
	}
	
	public static void assertPropertyDelete(ObjectDelta<?> userDelta, PropertyPath propertyPath, Object... expectedValues) {
		PropertyDelta propertyDelta = userDelta.getPropertyDelta(propertyPath);
		assertNotNull("Property delta for "+propertyPath+" not found",propertyDelta);
		assertSet(propertyPath.last().getName(), propertyDelta.getValuesToDelete(), expectedValues);
	}
	
	public static ContainerDelta<?> assertContainerAdd(ObjectDelta<?> userDelta, PropertyPath propertyPath) {
		ContainerDelta<?> delta = userDelta.getContainerDelta(propertyPath);
		assertNotNull("Container delta for "+propertyPath+" not found",delta);
		assert !delta.isEmpty() : "Container delta for "+propertyPath+" is empty";
		return delta;
	}
	
	// OBJECT asserts
	
	public static void assertElementsEquals(Object expected, Object actual) throws SchemaException {
		assertEquals(elementToPrism(expected), elementToPrism(actual));
    }
	
	public static void assertEquals(File fileNewXml, String objectString) throws SchemaException {
		assertEquals(toPrism(fileNewXml), toPrism(objectString));
    }
	
	public static void assertEquals(Objectable expected, Objectable actual) throws SchemaException {
		assertEquals(actual.asPrismObject(), actual.asPrismObject());
    }
	
	public static void assertEquals(File fileNewXml, Objectable objectable) throws SchemaException {
		assertEquals(toPrism(fileNewXml), objectable.asPrismObject());
    }

	public static void assertEquals(PrismObject<?> prism1, PrismObject<?> prism2) {
		if (prism1 == null) {
			fail("Left prism is null");
		}
		if (prism2 == null) {
			fail("Right prism is null");
		}
		assertEquals(null, prism1, prism2);
	}
	
	public static void assertEquals(String message, PrismObject<?> prism1, PrismObject<?> prism2) {
		if (prism1 == null) {
			fail(message + ": Left prism is null");
		}
		if (prism2 == null) {
			fail(message + ": Right prism is null");
		}
		assertEquals(message, prism1, prism2);
	}

	private static void assertSet(QName propertyName, Collection<PrismPropertyValue<?>> valuesFromDelta, Object[] expectedValues) {
		assertEquals("Wrong number of values",expectedValues.length, valuesFromDelta.size());
		for (PrismPropertyValue<?> valueToReplace: valuesFromDelta) {
			boolean found = false;
			for (Object value: expectedValues) {
				if (value.equals(valueToReplace.getValue())) {
					found = true;
				}
			}
			if (!found) {
				fail("Unexpected value "+valueToReplace+" in delta for "+propertyName);
			}
		}
	}
	
	private static PrismObject<?> toPrism(String objectString) throws SchemaException {
		return getDomProcessor().parseObject(objectString);
	}

	private static PrismObject<?> toPrism(File objectFile) throws SchemaException {
		return getDomProcessor().parseObject(objectFile);
	}
	
	private static PrismObject<?> toPrism(Node domNode) throws SchemaException {
		return getDomProcessor().parseObject(domNode);
	}


	private static PrismObject<?> elementToPrism(Object element) throws SchemaException {
		if (element instanceof Node) {
			return toPrism((Node)element);
		} else if (element instanceof JAXBElement<?>) {
			JAXBElement<?> jaxbElement = (JAXBElement)element;
			Object value = jaxbElement.getValue();
			if (value instanceof Objectable) {
				return ((Objectable)value).asPrismObject();
			} else {
				throw new IllegalArgumentException("Unknown JAXB element value "+value);
			}
		} else {
			throw new IllegalArgumentException("Unknown element type "+element);
		}
	}

	private static PrismDomProcessor getDomProcessor() {
		return PrismTestUtil.getPrismContext().getPrismDomProcessor();
	}

	private static PrismJaxbProcessor getJaxbProcessor() {
		return PrismTestUtil.getPrismContext().getPrismJaxbProcessor();
	}
	
	// Local version of JUnit assers to avoid pulling JUnit dependecy to main
	
	static void assertNotNull(String string, Object object) {
		assert object != null : string;
	}
	
	static void assertEquals(String message, Object expected, Object actual) {
		assert expected.equals(actual) : message + ": expected "+expected+", was "+actual;
	}
	
	static void fail(String message) {
		assert false: message;
	}

}
