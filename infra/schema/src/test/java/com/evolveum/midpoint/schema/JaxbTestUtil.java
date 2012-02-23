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
package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;

import org.w3c.dom.Node;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import java.io.File;

/**
 * Class that statically instantiates the contexts for JAXB parsing.
 * This is usable for tests. Do not use in ordinary (production) code.
 *
 * @author semancik
 */
public class JaxbTestUtil {

    private static final QName DEFAULT_ELEMENT_NAME = new QName("http://midpoint.evolveum.com/xml/ns/test/whatever-1.xsd", "whatever");

    private static PrismContext prismContext;

    public static void marshalElementToDom(JAXBElement<?> jaxbElement, Node parentNode) throws JAXBException {
        prismContext.getPrismJaxbProcessor().marshalElementToDom(jaxbElement, parentNode);
    }

    public static <T> JAXBElement<T> unmarshalElement(String xmlString, Class<T> type) throws JAXBException {
        return prismContext.getPrismJaxbProcessor().unmarshalElement(xmlString, type);
    }

    public static <T> JAXBElement<T> unmarshalElement(File xmlFile, Class<T> type) throws JAXBException {
        return prismContext.getPrismJaxbProcessor().unmarshalElement(xmlFile, type);
    }
    
    public static <T> T unmarshalObject(String stringXml, Class<T> type) throws JAXBException {
    	return prismContext.getPrismJaxbProcessor().unmarshalObject(stringXml, type);
    }
    
    public static <T> T unmarshalObject(File file, Class<T> type) throws JAXBException {
    	return prismContext.getPrismJaxbProcessor().unmarshalObject(file, type);
    }

    public static String marshalToString(Objectable objectable) throws JAXBException {
        return prismContext.getPrismJaxbProcessor().marshalToString(objectable);
    }

    public static String marshalElementToString(JAXBElement<?> jaxbElement) throws JAXBException {
        return prismContext.getPrismJaxbProcessor().marshalElementToString(jaxbElement);
    }

    // Compatibility
    public static String marshalWrap(Object jaxbObject) throws JAXBException {
        JAXBElement<Object> jaxbElement = new JAXBElement<Object>(DEFAULT_ELEMENT_NAME, (Class) jaxbObject.getClass(), jaxbObject);
        return marshalElementToString(jaxbElement);
    }
    
    public static PrismContext getPrismContext() {
    	return prismContext;
    }

    public static void initialize() {
        MidPointPrismContextFactory factory = new MidPointPrismContextFactory();
        try {
            prismContext = factory.createPrismContext();
        } catch (SchemaException e) {
            throw new SystemException(e);
        }
    }

    static {
        initialize();
    }
}
