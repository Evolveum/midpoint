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
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.common;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.util.DOMUtil;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.apache.commons.lang.Validate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author semancik
 */
public class QueryUtil {

    public static Element createTypeFilter(Document doc, String uri) {
        Validate.notNull(doc);
        Validate.notNull(uri);
        Validate.notEmpty(uri);

        Element type = doc.createElementNS(SchemaConstants.C_FILTER_TYPE.getNamespaceURI(), SchemaConstants.C_FILTER_TYPE.getLocalPart());
        type.setAttributeNS(SchemaConstants.C_FILTER_TYPE_URI.getNamespaceURI(), SchemaConstants.C_FILTER_TYPE_URI.getLocalPart(), uri);
        return type;
    }

    /**
     * Creates "equal" filter segment for multi-valued properties based on DOM representation.
     * 
     * @param doc
     * @param xpath property container xpath. may be null.
     * @param values
     * @return "equal" filter segment (as DOM)
     * @throws JAXBException 
     */
    public static Element createEqualFilter(Document doc, XPathHolder xpath, List<? extends Object> values) throws SchemaException {
        Validate.notNull(doc);
        Validate.notNull(values);
        Validate.notEmpty(values);

        Element equal = doc.createElementNS(SchemaConstants.C_FILTER_EQUAL.getNamespaceURI(), SchemaConstants.C_FILTER_EQUAL.getLocalPart());
        Element value = doc.createElementNS(SchemaConstants.C_FILTER_VALUE.getNamespaceURI(), SchemaConstants.C_FILTER_VALUE.getLocalPart());
        for (Object val : values) {
        	Element domElement;
			try {
				domElement = JAXBUtil.toDomElement(val);
			} catch (JAXBException e) {
				throw new SchemaException("Unexpected JAXB problem while creating search filer for value "+val,e);
			}
            value.appendChild(doc.importNode(domElement, true));
        }
        if (xpath != null) {
            Element path = xpath.toElement(SchemaConstants.C_FILTER_PATH, doc);
            equal.appendChild(doc.importNode(path, true));
        }
        equal.appendChild(doc.importNode(value, true));
        return equal;
    }

    /**
     * Creates "equal" filter segment for single-valued properties based on DOM representation.
     * 
     * @param doc
     * @param xpath property container xpath. may be null.
     * @param value
     * @return "equal" filter segment (as DOM)
     * @throws JAXBException 
     */
    public static Element createEqualFilter(Document doc, XPathHolder xpath, Object value) throws SchemaException {
        Validate.notNull(doc);
        Validate.notNull(value);

        List<Object> values = new ArrayList<Object>();
        values.add(value);
        return createEqualFilter(doc, xpath, values);
    }
    
    /**
     * Creates "equal" filter segment for single-valued properties with string content.
     * 
     * @param doc
     * @param xpath property container xpath. may be null.
     * @param value
     * @return "equal" filter segment (as DOM)
     * @throws JAXBException 
     */
    public static Element createEqualFilter(Document doc, XPathHolder xpath, QName properyName, String value) throws SchemaException {
        Validate.notNull(doc);
        Validate.notNull(properyName);
        Validate.notNull(value);

        Element element = doc.createElementNS(properyName.getNamespaceURI(), properyName.getLocalPart());
        element.setTextContent(value);
        List<Element> values = new ArrayList<Element>();
        values.add(element);
        return createEqualFilter(doc, xpath, values);
    }
    
    /**
     * Creates "equal" filter segment for single-valued properties with QName content.
     * 
     * @param doc
     * @param xpath property container xpath. may be null.
     * @param value
     * @return "equal" filter segment (as DOM)
     * @throws JAXBException 
     */
    public static Element createEqualFilter(Document doc, XPathHolder xpath, QName properyName, QName value) throws SchemaException {
        Validate.notNull(doc);
        Validate.notNull(properyName);
        Validate.notNull(value);

        Element element = doc.createElementNS(properyName.getNamespaceURI(), properyName.getLocalPart());
        DOMUtil.setQNameValue(element, value);
        List<Element> values = new ArrayList<Element>();
        values.add(element);
        return createEqualFilter(doc, xpath, values);
    }
    
    /**
     * Creates "equal" filter for object reference.
     * 
     * @param doc
     * @param xpath property container xpath. may be null.
     * @param propertyName name of the reference property (e.g. "resourceRef")
     * @param oid OID of the referenced object
     * @return "equal" filter segment (as DOM)
     * @throws JAXBException 
     */
    public static Element createEqualRefFilter(Document doc, XPathHolder xpath, QName propertyName, String oid) throws SchemaException {
    	Element value = doc.createElementNS(propertyName.getNamespaceURI(), propertyName.getLocalPart());
    	value.setAttributeNS(SchemaConstants.C_OID_ATTRIBUTE.getNamespaceURI(), SchemaConstants.C_OID_ATTRIBUTE.getLocalPart(), oid);
    	return createEqualFilter(doc,xpath,value);
    }

    public static Element createAndFilter(Document doc, Element el1, Element el2) {
        Validate.notNull(doc);
        Validate.notNull(el1);
        Validate.notNull(el2);

        Element and = doc.createElementNS(SchemaConstants.C_FILTER_AND.getNamespaceURI(), SchemaConstants.C_FILTER_AND.getLocalPart());
        and.appendChild(el1);
        and.appendChild(el2);
        return and;
    }

    public static Element createAndFilter(Document doc, Element el1, Element el2, Element el3) {
        Validate.notNull(doc);
        Validate.notNull(el1);
        Validate.notNull(el2);
        Validate.notNull(el3);

        Element and = doc.createElementNS(SchemaConstants.C_FILTER_AND.getNamespaceURI(), SchemaConstants.C_FILTER_AND.getLocalPart());
        and.appendChild(el1);
        and.appendChild(el2);
        and.appendChild(el3);
        return and;
    }
}
