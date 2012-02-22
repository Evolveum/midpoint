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
package com.evolveum.midpoint.test.util;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.testng.AssertJUnit;
import org.w3c.dom.Node;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.dom.PrismDomProcessor;
import com.evolveum.midpoint.prism.xml.PrismJaxbProcessor;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
public class PrismAsserts {
	
	public static void assertPropertyValues(Collection expected, Collection<PrismPropertyValue<Object>> results) {
		AssertJUnit.assertEquals(expected.size(), results.size());

        Set<Object> values = new HashSet<Object>();
        for (PrismPropertyValue result : results) {
            values.add(result.getValue());
        }
        AssertJUnit.assertEquals(expected, values);

//        Object[] array = expected.toArray();
//        PropertyValue[] array1 = new PropertyValue[results.size()];
//        results.toArray(array1);
//
//        for (int i=0;i<expected.size();i++) {
//            assertEquals(array[i], array1[i].getValue());
//        }
    }
	
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
			AssertJUnit.fail("Left prism is null");
		}
		if (prism2 == null) {
			AssertJUnit.fail("Right prism is null");
		}
		AssertJUnit.assertEquals(prism1, prism2);
	}

	public static void assertEquals(String message, PrismObject<?> prism1, PrismObject<?> prism2) {
		if (prism1 == null) {
			AssertJUnit.fail(message + ": Left prism is null");
		}
		if (prism2 == null) {
			AssertJUnit.fail(message + ": Right prism is null");
		}
		AssertJUnit.assertEquals(message, prism1, prism2);
	}
	
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

	private static void assertSet(QName propertyName, Collection<PrismPropertyValue<?>> valuesFromDelta, Object[] expectedValues) {
		AssertJUnit.assertEquals("Wrong number of values",expectedValues.length, valuesFromDelta.size());
		for (PrismPropertyValue<?> valueToReplace: valuesFromDelta) {
			boolean found = false;
			for (Object value: expectedValues) {
				if (value.equals(valueToReplace.getValue())) {
					found = true;
				}
			}
			if (!found) {
				AssertJUnit.fail("Unexpected value "+valueToReplace+" in delta for "+propertyName);
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

}
