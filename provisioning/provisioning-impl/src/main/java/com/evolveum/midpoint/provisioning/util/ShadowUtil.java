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

package com.evolveum.midpoint.provisioning.util;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectIdentificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType.Attributes;
import com.evolveum.midpoint.xml.ns._public.model.model_1.FaultMessage;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author semancik
 */
public class ShadowUtil {

    // TODO: This should be probably moved out to a better place
    //public static final String COMMON_SCHEMA_NAMESPACE = "http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd";
    public static final String COMMON_SCHEMA_OBJECT_ELEMENT_NAME = "object";

    public static final String JAXB_PACKAGE_NAME = "com.evolveum.midpoint.xml.ns._public.common.common_1";

    // It is specific to identity connector
    //public static final String IC_SCHEMA_NAMESPACE = "http://midpoint.evolveum.com/xml/ns/public/resource/idconnector/resource-schema-1.xsd";
    //public static final String IC_CONFIGURATION_NAMESPACE = "http://midpoint.evolveum.com/xml/ns/public/resource/idconnector/configuration-1.xsd";
    /**
     * Prefix for the generated bundle namespaces.
     */
    public static final String IC_BUNDLE_NAMESPACE_PREFIX = "http://midpoint.evolveum.com/xml/ns/resource/idconnector/bundle";

    //public static final QName IC_UID_QNAME = new QName(NS_ICF_RESOURCE, "uid", "ids");

    //public static final QName IC_NAME_QNAME = new QName(NS_ICF_RESOURCE, "name", "ids");
//    public static final QName IC_BUNDLE_NAME = new QName(IC_CONFIGURATION_NAMESPACE, "bundleName", "ids");
//    public static final QName IC_BUNDLE_VERSION = new QName(IC_CONFIGURATION_NAMESPACE, "bundleVersion", "ids");
//    public static final QName IC_CONNECTOR_NAME = new QName(IC_CONFIGURATION_NAMESPACE, "connectorName", "ids");

    public static final String IC_DEFAULT_ACCOUNT_XSD_OBJECTCLASS_LOCAL_NAME = "AccountObjectClass";

    public static final String IC_DEFAULT_GROUP_XSD_OBJECTCLASS_LOCAL_NAME = "GroupObjectClass";
//    public static final QName[] IC_BUNDLE_KEYS = new QName[]{IC_BUNDLE_NAME, IC_BUNDLE_VERSION, IC_CONNECTOR_NAME};

    private static final Trace logger = TraceManager.getTrace(ShadowUtil.class);

    /**
     * Create temporaly XMLDocument just as a factory to create custom XML elements.
     *
     * @return empty XMLDocment element
     *
     * @throws FaultMessage
     */
    public static Document getXmlDocument() {
        return DOMUtil.getDocument();
    }

    public static String getSingleValueString(List<Object> list) {
        // TODO checks and meaningful exceptions
        Object o = list.get(0);
        return (String) o;
    }

    public static String getSingleValueAttributeString(ResourceObjectShadowType.Attributes attrs, QName qname) {
        return getSingleValueAttributeString(attrs.getAny(), qname);
    }

    public static String getSingleValueAttributeString(List<Element> any, QName qname) {
        Element e = getSingleValueAttributeElement(any, qname);
        if (e == null) {
            return null;
        }
        return e.getTextContent();
    }

    public static Element getSingleValueAttributeElement(ResourceObjectShadowType.Attributes attrs, QName qname) {

        return getSingleValueAttributeElement(attrs.getAny(), qname);

    }

    public static Element getSingleValueAttributeElement(List<Element> any, QName qname) {

        // This is quick and dirty code
        // TODO check if this code is correct. And clean it up. A lot.

        Element result = null;

        for (Object o : any) {
            if (o instanceof Element) {
                Element e = (Element) o;
                if (qname.getNamespaceURI().equals(e.getNamespaceURI()) && qname.getLocalPart().equals(e.getLocalName())) {
                    if (result == null) {
                        result = e;
                    } else {
                        throw new IllegalStateException("Multiple values found where single value expected for attribute " + qname);
                    }
                }
            } else {
                // Die miserably. There is something we do not expect.
                throw new IllegalStateException("Unknown type genrated by JAX-B code for ResourceObjectShadow attributes: " + o);
            }
        }

        return result;
    }

    public static void replaceSingleValueAttribute(ResourceObjectShadowType.Attributes attrs, QName qname, String newValue) throws ParserConfigurationException {
        replaceSingleValueAttribute(attrs.getAny(), qname, newValue);
    }

    public static void replaceSingleValueAttribute(List<Element> any, QName qname, String newValue) throws ParserConfigurationException {
        Element e = getXmlDocument().createElementNS(qname.getNamespaceURI(), qname.getLocalPart());
        e.setTextContent(newValue);
        replaceSingleValueAttribute(any, e);
    }

    public static void replaceSingleValueAttribute(List<Element> any, Element newElement) {

        // This is quick and dirty code
        // TODO check if this code is correct. And clean it up. A lot.

        Object found = null;

        for (Object o : any) {
            if (o instanceof Element) {
                Element e = (Element) o;
                if (newElement.getNamespaceURI().equals(e.getNamespaceURI()) && newElement.getLocalName().equals(e.getLocalName())) {
                    if (found == null) {
                        found = o;
                    } else {
                        throw new IllegalStateException("Multiple values found where single value expected for attribute {" + newElement.getNamespaceURI() + "}" + newElement.getLocalName());
                    }
                }
            } else {
                // Die miserably. There is something we do not expect.
                throw new IllegalStateException("Unknown type genrated by JAX-B code for ResourceObjectShadow attributes: " + o);
            }
        }

        any.remove(found);
        any.add(newElement);
    }

    public static void replaceMultiValueAttribute(ResourceObjectShadowType.Attributes attrs, QName qname, Set<String> values) throws ParserConfigurationException {
        replaceMultiValueAttribute(attrs.getAny(), qname, values);
    }

    public static void replaceMultiValueAttribute(List<Element> any, QName qname, Set<String> values) throws ParserConfigurationException {

        Document xmlDocument = getXmlDocument();

        //Element e = xmlDocument.createElementNS(qname.getNamespaceURI(), qname.getLocalPart());

        // This is quick and dirty code
        // TODO check if this code is correct. And clean it up. A lot.

        Set<Object> found = new HashSet<Object>();

        for (Object o : any) {
            if (o instanceof Element) {
                Element e = (Element) o;
                if (qname.getNamespaceURI().equals(e.getNamespaceURI()) && qname.getLocalPart().equals(e.getLocalName())) {
                    found.add(o);
                }
            } else {
                // Die miserably. There is something we do not expect.
                throw new IllegalStateException("Unknown type genrated by JAX-B code for ResourceObjectShadow attributes: " + o);
            }
        }

        for (Object o : found) {
            any.remove(o);
        }

        for (String value : values) {
            Element e = xmlDocument.createElementNS(qname.getNamespaceURI(), qname.getLocalPart());
            e.setTextContent(value);
            any.add(e);
        }

    }

    public static Set<String> getMultiValueAttributeString(ResourceObjectShadowType.Attributes attrs, QName qname) {
        return getMultiValueAttributeString(attrs.getAny(), qname);
    }

    public static Set<String> getMultiValueAttributeString(List<Element> any, QName qname) {
        Set<Element> el = getMultiValueAttributeElement(any, qname);
        Set<String> result = new HashSet<String>();

        for (Element e : el) {
            result.add(e.getTextContent());
        }

        return result;
    }

    public static Set<Element> getMultiValueAttributeElement(List<Element> any, QName qname) {

        // This is quick and dirty code
        // TODO check if this code is correct. And clean it up. A lot.

        Set<Element> result = new HashSet<Element>();

        for (Object o : any) {
            if (o instanceof Element) {
                Element e = (Element) o;
                if (qname.getNamespaceURI().equals(e.getNamespaceURI()) && qname.getLocalPart().equals(e.getLocalName())) {
                    result.add(e);
                }
            } else {
                // Die miserably. There is something we do not expect.
                throw new IllegalStateException("Unknown type genrated by JAX-B code for ResourceObjectShadow attributes: " + o);
            }
        }

        return result;
    }

    //TODO: Find out why was it visible inside the package only (workaround: make it public)
    public static ResourceObjectIdentificationType attributesToIdentification(Attributes attrs) {
        // Just stupind copy for now. There may be more attributes than needed, but
        // we do not care. The code will pick up what it needs.

        ResourceObjectIdentificationType id = new ResourceObjectIdentificationType();

        // This is shallow copy. Is it enough? Do we need deep copy?
        for (Element o : attrs.getAny()) {
            id.getAny().add(o);
        }

        return id;
    }

    /**
     * Copy attributes between to type and remove the old ones.
     *
     * @param to
     * @param from
     */
    public static void changeAttributes(List<Element> to, List<Element> from) {
        to.clear();
        for (Element o : from) {
            to.add(o);
        }
    }

    /**
     * Copy attributes tag between to object.
     * 
     * @param to
     * @param from
     */
    public static void mergeAttributes(Attributes to, Attributes from) {

        // Get list of conflicting attributes first

        Set<QName> toDelete = new HashSet<QName>();

        for (Object o : from.getAny()) {
            Element e = (Element) o;
            toDelete.add(qNameFromElement(e));
        }

        // delete conflicting attributes
        Iterator it = to.getAny().iterator();
        while (it.hasNext()) {
            Element e = (Element) it.next();
            for (QName deleteQName : toDelete) {
                if (isElementQName(e, deleteQName)) {
                    it.remove();
                }
            }
        }

        // now we can freely add the attributes

        for (Object o : from.getAny()) {
            Element e = (Element) o;
            to.getAny().add(e);
        }

    }

    public static QName qNameFromElement(Element e) {
        return new QName(e.getNamespaceURI(), e.getLocalName());
    }

    public static boolean isElementQName(Element e, QName qname) {
        return (e.getNamespaceURI().equals(qname.getNamespaceURI()) && e.getLocalName().equals(qname.getLocalPart()));
    }

    public static Map<String, Set<Element>> indexElementsByName(List<Element> any) {
        return indexElementsByName(any, null);
    }

    /**
     * Collect child elements and index by names.
     * @param any
     * @param namespace
     * @return
     */
    public static Map<String, Set<Element>> indexElementsByName(List<Element> any, String namespace) {
        logger.debug("indexElementsByName begin");
        logger.debug("namespace = {}", namespace);

        Map<String, Set<Element>> index = new HashMap<String, Set<Element>>();

        //index available values (may be multiple values are avaliable)
        for (Object o : any) {
            Element e = (Element) o;
            logger.debug("e = {}", e);
            logger.debug("e.getLocalName = {}", e.getLocalName());
            logger.debug("e.getNamespaceURI() = {}", e.getNamespaceURI());
            if (namespace == null || namespace.equals(e.getNamespaceURI())) {
                String name = e.getLocalName();
                Set<Element> elements = index.get(name);
                if (elements == null) {
                    elements = new HashSet<Element>();
                    index.put(name, elements);
                }
                elements.add(e);
            }
        }
        logger.debug("indexElementsByName end");
        return index;
    }

    public static QName squeezeSingleQname(List<Element> any) {

        String resultNs = null;
        String resultName = null;

        for (Object o : any) {
            if (o instanceof Element) {
                Element e = (Element) o;
                if (resultName == null) {
                    resultNs = e.getNamespaceURI();
                    resultName = e.getLocalName();
                } else if (resultName.equals(e.getLocalName()) && resultNs.equals(e.getNamespaceURI())) {
                    // everything is OK ...
                } else {
                    throw new IllegalStateException("Multiple QNames found where single element was expected");
                }
            } else {
                // Die miserably. There is something we do not expect.
                throw new IllegalStateException("Unknown type genrated by JAX-B code for ResourceObjectShadow attributes: " + o);
            }
        }

        return new QName(resultNs, resultName);
    }

    public static Set<String> squeezeSingleStringValues(List<Element> any) {

        Set<String> result = new HashSet<String>();
        String resultNs = null;
        String resultName = null;

        for (Object o : any) {
            if (o instanceof Element) {
                Element e = (Element) o;
                if (resultName == null) {
                    resultNs = e.getNamespaceURI();
                    resultName = e.getLocalName();
                    result.add(e.getTextContent());
                } else if (resultName.equals(e.getLocalName()) && resultNs.equals(e.getNamespaceURI())) {
                    result.add(e.getTextContent());
                } else {
                    throw new IllegalStateException("Multiple QNames found where single element was expected");
                }
            } else {
                // Die miserably. There is something we do not expect.
                throw new IllegalStateException("Unknown type genrated by JAX-B code for ResourceObjectShadow attributes: " + o);
            }
        }

        return result;
    }
}
