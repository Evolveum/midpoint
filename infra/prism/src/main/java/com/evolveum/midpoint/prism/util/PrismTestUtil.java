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

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
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
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Class that statically instantiates the prism contexts and provides convenient static version of the PrismContext
 * and processor classes. 
 * 
 * This is usable for tests. DO NOT use this in the main code. Although it is placed in "main" for convenience,
 * is should only be used in tests.
 *
 * @author semancik
 */
public class PrismTestUtil {

    private static final QName DEFAULT_ELEMENT_NAME = new QName("http://midpoint.evolveum.com/xml/ns/test/whatever-1.xsd", "whatever");

    private static PrismContext prismContext;
    private static PrismContextFactory prismContextFactory;
    
    public static void resetPrismContext(PrismContextFactory newPrismContextFactory) throws SchemaException, SAXException, IOException {
    	setFactory(newPrismContextFactory);
    	resetPrismContext();
    }
    
	public static void setFactory(PrismContextFactory newPrismContextFactory) {
		PrismTestUtil.prismContextFactory = newPrismContextFactory;
	}

	public static void resetPrismContext() throws SchemaException, SAXException, IOException {
		prismContext = createInitializedPrismContext();
	}

	public static PrismContext createPrismContext() throws SchemaException {
    	if (prismContextFactory == null) {
    		throw new IllegalStateException("Cannot create prism context, no prism factory is set");
    	}
        return prismContextFactory.createPrismContext();
    }

    public static PrismContext createInitializedPrismContext() throws SchemaException, SAXException, IOException {
    	PrismContext newPrismContext = createPrismContext();
    	newPrismContext.initialize();
        return newPrismContext;
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
    
    public static <T extends Objectable> PrismObject<T> parseObject(String xmlString) throws SchemaException {
    	return getPrismContext().parseObject(xmlString);
    }
    
    public static <T extends Objectable> PrismObject<T> parseObject(Element element) throws SchemaException {
    	return getPrismContext().parseObject(element);
    }
    
    // ==========================
    // == JAXB
    // ==========================
    
    public static void marshalElementToDom(JAXBElement<?> jaxbElement, Node parentNode) throws JAXBException {
        prismContext.getPrismJaxbProcessor().marshalElementToDom(jaxbElement, parentNode);
    }

    public static <T> JAXBElement<T> unmarshalElement(String xmlString, Class<T> type) throws JAXBException, SchemaException {
        return prismContext.getPrismJaxbProcessor().unmarshalElement(xmlString, type);
    }
    
    public static <T> T unmarshalObject(File file, Class<T> type) throws JAXBException, SchemaException, FileNotFoundException {
    	return prismContext.getPrismJaxbProcessor().unmarshalObject(file, type);
    }
    
    public static <T> T unmarshalObject(String stringXml, Class<T> type) throws JAXBException, SchemaException {
    	return prismContext.getPrismJaxbProcessor().unmarshalObject(stringXml, type);
    }
    
    public static <T> JAXBElement<T> unmarshalElement(File xmlFile, Class<T> type) throws JAXBException, SchemaException, FileNotFoundException {
        return prismContext.getPrismJaxbProcessor().unmarshalElement(xmlFile, type);
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
    
}
