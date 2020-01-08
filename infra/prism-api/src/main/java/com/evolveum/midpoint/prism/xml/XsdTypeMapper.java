/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.xml;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.path.UniformItemPath;
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

    private static final Map<Class<?>, QName> JAVA_TO_XSD_TYPE_MAP = new HashMap<>();
    private static final Map<QName, Class<?>> XSD_TO_JAVA_TYPE_MAP = new HashMap<>();
    private static final Map<String, QName> XSD_TYPE_QUALIFICATION_MAP = new HashMap<>();
    private static final Map<Class<?>, QName> JAVA_TO_XSD_TYPE_MAP_EXT = new HashMap<>();
    private static final Map<QName, Class<?>> XSD_TO_JAVA_TYPE_MAP_EXT = new HashMap<>();

    private static final Trace LOGGER = TraceManager.getTrace(XsdTypeMapper.class);
    private static final String MULTIPLICITY_UNBOUNDED = "unbounded";

    private static void initTypeMap() {
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
        addMapping(GregorianCalendar.class, DOMUtil.XSD_DATETIME, false);
        addMapping(XMLGregorianCalendar.class, DOMUtil.XSD_DATETIME, true);
        addMapping(ZonedDateTime.class, DOMUtil.XSD_DATETIME, false);
        addMapping(Duration.class, DOMUtil.XSD_DURATION, true);

        addMapping(ItemPathType.class, ItemPathType.COMPLEX_TYPE, true);
        addMapping(UniformItemPath.class, ItemPathType.COMPLEX_TYPE, false);
        addMapping(ItemPath.class, ItemPathType.COMPLEX_TYPE, false);
        addMapping(QName.class, DOMUtil.XSD_QNAME, true);

        addMapping(PolyString.class, PrismConstants.POLYSTRING_TYPE_QNAME, true);
        addMapping(RawType.class,  DOMUtil.XSD_STRING, false);
        addMappingExt(ItemPathType.class, ItemPathType.COMPLEX_TYPE, true);                // TODO remove

        addXsdToJavaMapping(DOMUtil.XSD_ANYURI, String.class);
    }

    private static void addMapping(Class javaClass, QName xsdType, boolean alsoXsdToJava) {
        LOGGER.trace("Adding XSD type mapping {} {} {} ", javaClass, alsoXsdToJava ? "<->" : " ->", xsdType);
        JAVA_TO_XSD_TYPE_MAP.put(javaClass, xsdType);
        if (alsoXsdToJava) {
            addXsdToJavaMapping(xsdType, javaClass);
        }
    }

    private static void addXsdToJavaMapping(QName xsdType, Class javaClass) {
        XSD_TO_JAVA_TYPE_MAP.put(xsdType, javaClass);
        XSD_TO_JAVA_TYPE_MAP.put(QNameUtil.nullNamespace(xsdType), javaClass);
        XSD_TYPE_QUALIFICATION_MAP.put(xsdType.getLocalPart(), xsdType);
    }

    private static void addMappingExt(Class javaClass, QName xsdType, boolean alsoXsdToJava) {
        LOGGER.trace("Adding 'ext' XSD type mapping {} {} {} ", javaClass, alsoXsdToJava ? "<->" : " ->", xsdType);
        JAVA_TO_XSD_TYPE_MAP_EXT.put(javaClass, xsdType);
        if (alsoXsdToJava) {
            XSD_TO_JAVA_TYPE_MAP_EXT.put(xsdType, javaClass);
            XSD_TO_JAVA_TYPE_MAP_EXT.put(QNameUtil.nullNamespace(xsdType), javaClass);
        }
    }

    @NotNull
    public static QName toXsdType(Class javaClass) {
        QName xsdType = getJavaToXsdMapping(javaClass);
        if (xsdType != null) {
            return xsdType;
        } else {
            throw new IllegalArgumentException("No XSD mapping for Java type " + javaClass.getCanonicalName());
        }
    }

    public static QName getJavaToXsdMapping(Class<?> type) {
        QName typeName = JAVA_TO_XSD_TYPE_MAP.get(type);
        if (typeName != null) {
            return typeName;
        } else {
            Class<?> superType = type.getSuperclass();
            if (superType != null) {
                return getJavaToXsdMapping(superType);
            } else {
                return null;
            }
        }
    }

    public static QName determineQNameWithNs(QName xsdType) {
        if (QNameUtil.isQualified(xsdType)) {
            return xsdType;
        } else {
            return XSD_TYPE_QUALIFICATION_MAP.get(xsdType.getLocalPart());
        }
    }

    public static <T> Class<T> getXsdToJavaMapping(QName xsdType) {
        //noinspection unchecked
        return (Class<T>) XSD_TO_JAVA_TYPE_MAP.get(xsdType);
    }

    /**
     * Returns the class in the type mapping.
     * The class supplied by the caller may be a subclass of what we have in the map.
     * This returns the class that in the mapping.
     */
    public static Class<?> getTypeFromClass(Class<?> clazz) {
        if (JAVA_TO_XSD_TYPE_MAP.containsKey(clazz)) {
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
        return toJavaType(XSD_TO_JAVA_TYPE_MAP, xsdType, true);
    }

    @Nullable
    public static <T> Class<T> toJavaTypeIfKnown(@NotNull QName xsdType) {
        return toJavaType(XSD_TO_JAVA_TYPE_MAP, xsdType, false);
    }

    // experimental feature - covers all the classes
    public static <T> Class<T> toJavaTypeIfKnownExt(@NotNull QName xsdType) {
        Class<T> cls = toJavaType(XSD_TO_JAVA_TYPE_MAP, xsdType, false);
        if (cls != null) {
            return cls;
        } else {
            return toJavaType(XSD_TO_JAVA_TYPE_MAP_EXT, xsdType, false);
        }
    }

    @Nullable
    private static <T> Class<T> toJavaType(Map<QName, Class<?>> map, @NotNull QName xsdType, boolean errorIfNoMapping) {
        Class<?> javaType = map.get(xsdType);
        if (javaType != null) {
            //noinspection unchecked
            return (Class<T>) javaType;
        } else if (errorIfNoMapping && xsdType.getNamespaceURI().equals(XMLConstants.W3C_XML_SCHEMA_NS_URI)) {
            throw new IllegalArgumentException("No type mapping for XSD type " + xsdType);
        } else {
            return null;
        }
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
