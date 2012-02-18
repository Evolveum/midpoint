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

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.JAXBElement;

import org.testng.AssertJUnit;
import org.w3c.dom.Node;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
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
	
	public static void assertElementsEquals(Object element1, Object element2) throws SchemaException {
		assertEquals(elementToPrism(element1), elementToPrism(element2));
    }
	
	public static void assertEquals(File fileNewXml, String objectString) throws SchemaException {
		assertEquals(toPrism(fileNewXml), toPrism(objectString));
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
