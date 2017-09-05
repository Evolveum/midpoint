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
package com.evolveum.midpoint.prism.xml;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Maintains mapping of XSD types (qnames) and Java types (classes)
 *
 * @author Radovan Semancik
 */
public class XsdTypeMapper {

    public static final String BOOLEAN_XML_VALUE_TRUE = "true";
    public static final String BOOLEAN_XML_VALUE_FALSE = "false";

    private static final Map<Class, QName> javaToXsdTypeMap = new HashMap<>();
    private static final Map<QName, Class> xsdToJavaTypeMap = new HashMap<>();
    private static final Map<Class, QName> javaToXsdTypeMapExt = new HashMap<>();
    private static final Map<QName, Class> xsdToJavaTypeMapExt = new HashMap<>();

    private static final Trace LOGGER = TraceManager.getTrace(XsdTypeMapper.class);
	private static final String MULTIPLICITY_UNBOUNDED = "unbounded";

    private static void initTypeMap() throws IOException, ClassNotFoundException {
        addMapping(String.class, DOMUtil.XSD_STRING, true);
        addMapping(char.class, DOMUtil.XSD_STRING, false);
        addMapping(File.class, DOMUtil.XSD_STRING, false);
        addMapping(int.class, DOMUtil.XSD_INT, true);
        addMapping(Integer.class, DOMUtil.XSD_INT, false);
        addMapping(BigInteger.class, DOMUtil.XSD_INTEGER, true);
        addMapping(BigDecimal.class, DOMUtil.XSD_DECIMAL, true);
        addMapping(double.class, DOMUtil.XSD_DOUBLE, true);
        addMapping(Double.class, DOMUtil.XSD_DOUBLE, false);
        addMapping(float.class, DOMUtil.XSD_FLOAT, true);
        addMapping(Float.class, DOMUtil.XSD_FLOAT, false);
        //maybe this is not a great idea
        addMapping(long.class, DOMUtil.XSD_LONG, true);
        addMapping(Long.class, DOMUtil.XSD_LONG, false);
        addMapping(short.class, DOMUtil.XSD_SHORT, true);
        addMapping(Short.class, DOMUtil.XSD_SHORT, false);
        addMapping(byte.class, DOMUtil.XSD_BYTE, true);
        addMapping(Byte.class, DOMUtil.XSD_BYTE, false);
        //great idea end
        addMapping(boolean.class, DOMUtil.XSD_BOOLEAN, true);
        addMapping(Boolean.class, DOMUtil.XSD_BOOLEAN, false);
        addMapping(byte[].class, DOMUtil.XSD_BASE64BINARY, true);
        addMapping(GregorianCalendar.class, DOMUtil.XSD_DATETIME, true);
        addMapping(XMLGregorianCalendar.class, DOMUtil.XSD_DATETIME, true);
        addMapping(Duration.class, DOMUtil.XSD_DURATION, true);

        addMapping(ItemPathType.class, ItemPathType.COMPLEX_TYPE, true);
        addMapping(ItemPath.class, ItemPathType.COMPLEX_TYPE, false);
        addMapping(QName.class, DOMUtil.XSD_QNAME, true);

        addMapping(PolyString.class, PrismConstants.POLYSTRING_TYPE_QNAME, true);
        addMappingExt(ItemPathType.class, ItemPathType.COMPLEX_TYPE, true);				// TODO remove

        xsdToJavaTypeMap.put(DOMUtil.XSD_ANYURI, String.class);
    }

    private static void addMapping(Class javaClass, QName xsdType, boolean both) {
        LOGGER.trace("Adding XSD type mapping {} {} {} ", javaClass, both ? "<->" : " ->", xsdType);
        javaToXsdTypeMap.put(javaClass, xsdType);
        if (both) {
            xsdToJavaTypeMap.put(xsdType, javaClass);
        }
    }

    private static void addMappingExt(Class javaClass, QName xsdType, boolean both) {
        LOGGER.trace("Adding 'ext' XSD type mapping {} {} {} ", javaClass, both ? "<->" : " ->", xsdType);
        javaToXsdTypeMapExt.put(javaClass, xsdType);
        if (both) {
            xsdToJavaTypeMapExt.put(xsdType, javaClass);
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

    public static QName determineQNameWithNs(QName xsdType){
    	if (StringUtils.isNotBlank(xsdType.getNamespaceURI())){
    		return xsdType;
    	}
    	Set<QName> keys = xsdToJavaTypeMap.keySet();
    	for (Iterator<QName> iterator = keys.iterator(); iterator.hasNext();){
    		QName key = iterator.next();
    		if (QNameUtil.match(key, xsdType)){
    			return key;
    		}
    	}
    	return null;
    }

    public static <T> Class<T> getXsdToJavaMapping(QName xsdType) {
    	Class clazz = xsdToJavaTypeMap.get(xsdType);
    	if (clazz == null){
    		Set<QName> keys = xsdToJavaTypeMap.keySet();
    		for (Iterator<QName> iterator = keys.iterator(); iterator.hasNext();){
    			QName key = iterator.next();
    			if (QNameUtil.match(key, xsdType)){
    				return xsdToJavaTypeMap.get(key);
    			}
    		}
    	}
    	return xsdToJavaTypeMap.get(xsdType);
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

    @Nullable
    public static <T> Class<T> toJavaType(@NotNull QName xsdType) {
		//noinspection ConstantConditions
		return toJavaType(xsdToJavaTypeMap, xsdType, true);
    }

    @Nullable
    public static <T> Class<T> toJavaTypeIfKnown(@NotNull QName xsdType) {
        return toJavaType(xsdToJavaTypeMap, xsdType, false);
    }

    // experimental feature - covers all the classes
    public static <T> Class<T> toJavaTypeIfKnownExt(@NotNull QName xsdType) {
        Class<T> cls = toJavaType(xsdToJavaTypeMap, xsdType, false);
        if (cls != null) {
            return cls;
        } else {
            return toJavaType(xsdToJavaTypeMapExt, xsdType, false);
        }
    }

    @Nullable
    private static <T> Class<T> toJavaType(Map<QName, Class> map, @NotNull QName xsdType, boolean errorIfNoMapping) {
        Class<?> javaType = map.get(xsdType);
		if (javaType == null && StringUtils.isEmpty(xsdType.getNamespaceURI())) {
			// TODO check uniqueness w.r.t. other types...
			for (Map.Entry<QName,Class> entry : xsdToJavaTypeMap.entrySet()) {
				if (QNameUtil.match(entry.getKey(), xsdType)) {
					javaType = entry.getValue();
					break;
				}
			}
		}
        if (javaType == null) {
            if (errorIfNoMapping && xsdType.getNamespaceURI().equals(XMLConstants.W3C_XML_SCHEMA_NS_URI)) {
                throw new IllegalArgumentException("No type mapping for XSD type " + xsdType);
            } else {
                return null;
            }
        }
        @SuppressWarnings("unchecked")
        Class<T> typedClass = (Class<T>) javaType;
        return typedClass;
    }

    public static String multiplicityToString(Integer integer) {
		if (integer == null) {
			return null;
		}
		if (integer < 0) {
			return MULTIPLICITY_UNBOUNDED;
		}
		return integer.toString();
	}

	public static Integer multiplicityToInteger(String string) {
		if (string == null || StringUtils.isEmpty(string)) {
			return null;
		}
		if (MULTIPLICITY_UNBOUNDED.equals(string)) {
			return -1;
		}
		return Integer.valueOf(string);
	}

	public static boolean isMatchingMultiplicity(int number, int min, int max) {
		if (min >= 0 && number < min) {
			return false;
		}
		if (max >= 0 && number > max) {
			return false;
		}
		return true;
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
