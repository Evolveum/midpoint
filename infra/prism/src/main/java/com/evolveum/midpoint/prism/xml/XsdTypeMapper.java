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
 */
package com.evolveum.midpoint.prism.xml;

import com.evolveum.midpoint.util.ClassPathUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.codec.binary.Base64;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Maintains mapping of XSD types (qnames) and Java types (classes)
 *
 * @author Radovan Semancik
 */
public class XsdTypeMapper {

    public static final String BOOLEAN_XML_VALUE_TRUE = "true";
    public static final String BOOLEAN_XML_VALUE_FALSE = "false";

    private static Map<Class, QName> javaToXsdTypeMap;
    private static Map<QName, Class> xsdToJavaTypeMap;

    private static final Trace LOGGER = TraceManager.getTrace(XsdTypeMapper.class);

    private static void initTypeMap() throws IOException, ClassNotFoundException {

        javaToXsdTypeMap = new HashMap<Class, QName>();
        xsdToJavaTypeMap = new HashMap<QName, Class>();
        addMapping(String.class, DOMUtil.XSD_STRING, true);
        addMapping(char.class, DOMUtil.XSD_STRING, false);
        addMapping(File.class, DOMUtil.XSD_STRING, false);
        addMapping(int.class, DOMUtil.XSD_INTEGER, true);
        addMapping(Integer.class, DOMUtil.XSD_INTEGER, false);
        addMapping(long.class, DOMUtil.XSD_INTEGER, false);
        addMapping(Long.class, DOMUtil.XSD_INTEGER, false);
        addMapping(boolean.class, DOMUtil.XSD_BOOLEAN, true);
        addMapping(Boolean.class, DOMUtil.XSD_BOOLEAN, false);
        addMapping(byte[].class, DOMUtil.XSD_BASE64BINARY, true);
        addMapping(GregorianCalendar.class, DOMUtil.XSD_DATETIME, true);
        addMapping(XMLGregorianCalendar.class, DOMUtil.XSD_DATETIME, true);
        addMapping(QName.class, DOMUtil.XSD_QNAME, true);

    }

    private static void addMapping(Class javaClass, QName xsdType, boolean both) {
        LOGGER.trace("Adding XSD type mapping {} {} {} ", new Object[]{javaClass, both ? "<->" : " ->",
                xsdType});
        javaToXsdTypeMap.put(javaClass, xsdType);
        if (both) {
            xsdToJavaTypeMap.put(xsdType, javaClass);
        }
    }

    public static QName toXsdType(Class javaClass) {
        QName xsdType = getJavaToXsdMapping(javaClass);
        if (xsdType == null) {
            throw new IllegalArgumentException("No XSD mapping for Java type " + javaClass.getCanonicalName());
        }
        return xsdType;
    }
    
    public static QName getJavaToXsdMapping(Class<?> type) {
        if (javaToXsdTypeMap.containsKey(type)) {
            return javaToXsdTypeMap.get(type);
        }
        Class<?> superType = type.getSuperclass();
        if (superType != null) {
            return getJavaToXsdMapping(superType);
        }
        return null;
    }

    /**
     * Returns the class in the type mapping.
     * The class supplied by the caller may be a subclass of what we have in the map.
     * This returns the class that in the mapping.
     */
    public static Class<?> getTypeFromClass(Class<?> clazz) {
        if (javaToXsdTypeMap.containsKey(clazz)) {
            return clazz;
        }
        Class<?> superClazz = clazz.getSuperclass();
        if (superClazz != null) {
            return getTypeFromClass(superClazz);
        }
        return null;
    }
    

    public static Class toJavaType(QName xsdType) {
        Class javaType = xsdToJavaTypeMap.get(xsdType);
        if (javaType == null) {
            if (xsdType.getNamespaceURI().equals(XMLConstants.W3C_XML_SCHEMA_NS_URI)) {
                throw new IllegalArgumentException("No type mapping for XSD type " + xsdType);
            } else {
                return Element.class;
            }
        }
        return javaType;
    }

    static {
        try {
            initTypeMap();
        } catch (Exception e) {
            LOGGER.error("Cannot initialize XSD type mapping: " + e.getMessage(), e);
            throw new IllegalStateException("Cannot initialize XSD type mapping: " + e.getMessage(), e);
        }
    }

}
