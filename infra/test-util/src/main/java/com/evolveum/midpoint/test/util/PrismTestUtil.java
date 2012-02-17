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

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;

/**
 * Class that statically instantiates the contexts for JAXB parsing.
 * This is usable for tests. Do not use in ordinary (production) code.
 *
 * @author semancik
 */
public class PrismTestUtil {

    private static final QName DEFAULT_ELEMENT_NAME = new QName("http://midpoint.evolveum.com/xml/ns/test/whatever-1.xsd", "whatever");

    private static PrismContext prismContext;
    
    public static PrismContext createPrismContext() throws SchemaException {
    	MidPointPrismContextFactory factory = new MidPointPrismContextFactory();
        return factory.createPrismContext();
    }

    public static PrismContext createInitializedPrismContext() throws SchemaException, SAXException, IOException {
    	MidPointPrismContextFactory factory = new MidPointPrismContextFactory();
        return factory.createInitializedPrismContext();
    }
    
    public static PrismContext getPrismContext() {
    	return prismContext;
    }
    
    public static SchemaRegistry getSchemaRegistry() {
    	return prismContext.getSchemaRegistry();
    }
    
    // ==========================
    // == parsing
    // ==========================
    
    public static <T extends Objectable> PrismObject<T> parseObject(File file) throws SchemaException {
    	return getPrismContext().getPrismDomProcessor().parseObject(file);
    }
    
    // ==========================
    // == JAXB
    // ==========================
    
    public static void marshalElementToDom(JAXBElement<?> jaxbElement, Node parentNode) throws JAXBException {
        prismContext.getPrismJaxbProcessor().marshalElementToDom(jaxbElement, parentNode);
    }

    public static <T> JAXBElement<T> unmarshalElement(String xmlString) throws JAXBException {
        return prismContext.getPrismJaxbProcessor().unmarshalElement(xmlString);
    }

    // Compatibility
    public static <T> JAXBElement<T> unmarshalElement(String xmlString, Class<T> type) throws JAXBException {
        return prismContext.getPrismJaxbProcessor().unmarshalElement(xmlString);
    }
    
    public static <T> T unmarshalObject(File file) throws JAXBException {
    	return prismContext.getPrismJaxbProcessor().unmarshalObject(file);
    }

    public static <T> JAXBElement<T> unmarshalElement(File xmlFile) throws JAXBException {
        return prismContext.getPrismJaxbProcessor().unmarshalElement(xmlFile);
    }

    // Compatibility
    public static <T> JAXBElement<T> unmarshalElement(File xmlFile, Class<T> type) throws JAXBException {
        return prismContext.getPrismJaxbProcessor().unmarshalElement(xmlFile);
    }
    
    public static <T> Element marshalObjectToDom(T jaxbObject, QName elementQName, Document doc) throws JAXBException {
    	return prismContext.getPrismJaxbProcessor().marshalObjectToDom(jaxbObject, elementQName, doc);
    }
    
    public static Element toDomElement(Object element) throws JAXBException {
    	return prismContext.getPrismJaxbProcessor().toDomElement(element);
    }
    
    public static Element toDomElement(Object jaxbElement, Document doc) throws JAXBException {
    	return prismContext.getPrismJaxbProcessor().toDomElement(jaxbElement, doc);
    }
    
    public static Element toDomElement(Object jaxbElement, Document doc, boolean adopt, boolean clone, boolean deep) throws JAXBException {
    	return prismContext.getPrismJaxbProcessor().toDomElement(jaxbElement, doc, adopt, clone, deep);
    }

    public static String marshalToString(Objectable objectable) throws JAXBException {
        return prismContext.getPrismJaxbProcessor().marshalToString(objectable);
    }

    public static String marshalElementToString(JAXBElement<?> jaxbElement) throws JAXBException {
        return prismContext.getPrismJaxbProcessor().marshalElementToString(jaxbElement);
    }
    
    // Works both on JAXB and DOM elements
    public static String marshalElementToString(Object element) throws JAXBException {
        return prismContext.getPrismJaxbProcessor().marshalElementToString(element);
    }

    // Compatibility
    public static String marshalWrap(Object jaxbObject) throws JAXBException {
        JAXBElement<Object> jaxbElement = new JAXBElement<Object>(DEFAULT_ELEMENT_NAME, (Class) jaxbObject.getClass(), jaxbObject);
        return marshalElementToString(jaxbElement);
    }
    
    // ==========================
    // == init
    // ==========================

    static {
        try {
            prismContext = createInitializedPrismContext();
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }
}
