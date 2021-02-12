/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.xml;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.codec.binary.Base64;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.TestOnly;
import org.w3c.dom.Element;

import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.datatype.*;
import javax.xml.namespace.QName;
import java.io.File;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Simple implementation that converts XSD primitive types to Java (and vice
 * versa).
 * <p>
 * It convert type names (xsd types to java classes) and also the values.
 * <p>
 * The implementation is very simple now. In fact just a bunch of ifs. We don't
 * need much more now. If more complex thing will be needed, we will extend the
 * implementation later.
 *
 * TODO clean this up as it is now part of prism-api!
 *
 * @author Radovan Semancik
 */
public class XmlTypeConverter {

    private static DatatypeFactory datatypeFactory = null;

    private static final Trace LOGGER = TraceManager.getTrace(XmlTypeConverter.class);

    private static DatatypeFactory getDatatypeFactory() {
        if (datatypeFactory == null) {
            try {
                datatypeFactory = DatatypeFactory.newInstance();
            } catch (DatatypeConfigurationException ex) {
                throw new IllegalStateException("Cannot construct DatatypeFactory: " + ex.getMessage(), ex);
            }
        }
        return datatypeFactory;
    }

    // do NOT use from the outside of prism
    public static boolean canConvert(Class<?> clazz) {
        return XsdTypeMapper.getJavaToXsdMapping(clazz) != null;
    }

    // do NOT use from the outside of prism
    public static boolean canConvert(QName xsdType) {
        return XsdTypeMapper.getXsdToJavaMapping(xsdType) != null;
    }

    // TODO consider moving this to better place
    public static boolean isMatchingType(Class<?> expectedClass, Class<?> actualClass) {
        if (expectedClass.isAssignableFrom(actualClass)) {
            return true;
        }
        if (isMatchingType(expectedClass, actualClass, int.class, Integer.class)) {
            return true;
        }
        if (isMatchingType(expectedClass, actualClass, long.class, Long.class)) {
            return true;
        }
        if (isMatchingType(expectedClass, actualClass, boolean.class, Boolean.class)) {
            return true;
        }
        if (isMatchingType(expectedClass, actualClass, byte.class, Byte.class)) {
            return true;
        }
        if (isMatchingType(expectedClass, actualClass, char.class, Character.class)) {
            return true;
        }
        if (isMatchingType(expectedClass, actualClass, float.class, Float.class)) {
            return true;
        }
        if (isMatchingType(expectedClass, actualClass, double.class, Double.class)) {
            return true;
        }
        return false;
    }

    private static boolean isMatchingType(Class<?> expectedClass, Class<?> actualClass, Class<?> lowerClass, Class<?> upperClass) {
        if (lowerClass.isAssignableFrom(expectedClass) && upperClass.isAssignableFrom(actualClass)) {
            return true;
        }
        if (lowerClass.isAssignableFrom(actualClass) && upperClass.isAssignableFrom(expectedClass)) {
            return true;
        }
        return false;
    }

    public static XMLGregorianCalendar createXMLGregorianCalendar() {
        return createXMLGregorianCalendar(System.currentTimeMillis());
    }

    public static XMLGregorianCalendar createXMLGregorianCalendar(Long timeInMillis) {
        if (timeInMillis == null) {
            return null;
        }
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTimeInMillis(timeInMillis);
        return createXMLGregorianCalendar(gregorianCalendar);
    }

    @Contract("null -> null")
    public static XMLGregorianCalendar createXMLGregorianCalendar(Date date) {
        if (date == null) {
            return null;
        }
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTime(date);
        return createXMLGregorianCalendar(gregorianCalendar);
    }

    public static XMLGregorianCalendar createXMLGregorianCalendar(String string) {
        // We need to make gregorian calendar roundtrip to make sure time zone is included

        return createXMLGregorianCalendar(getDatatypeFactory().newXMLGregorianCalendar(string));
    }

    public static XMLGregorianCalendar createXMLGregorianCalendarFromIso8601(String iso8601string) {
        return createXMLGregorianCalendar(ZonedDateTime.parse(iso8601string));
    }

    public static XMLGregorianCalendar createXMLGregorianCalendar(GregorianCalendar cal) {
        return getDatatypeFactory().newXMLGregorianCalendar(cal);
    }

    public static XMLGregorianCalendar createXMLGregorianCalendar(ZonedDateTime zdt) {
        return createXMLGregorianCalendar(GregorianCalendar.from(zdt));
    }

    public static ZonedDateTime toZonedDateTime(XMLGregorianCalendar xcal) {
        return xcal.toGregorianCalendar().toZonedDateTime();
    }

    // in some environments, XMLGregorianCalendar.clone does not work
    @Contract("null -> null; !null -> !null")
    public static XMLGregorianCalendar createXMLGregorianCalendar(XMLGregorianCalendar cal) {
        if (cal == null) {
            return null;
        }
        return createXMLGregorianCalendar(cal.toGregorianCalendar()); // TODO find a better way
    }

    @TestOnly
    public static XMLGregorianCalendar createXMLGregorianCalendar(int year, int month, int day, int hour, int minute,
            int second, int millisecond, int timezone) {
        return getDatatypeFactory().newXMLGregorianCalendar(year, month, day, hour, minute, second, millisecond, timezone);
    }

    @TestOnly
    public static XMLGregorianCalendar createXMLGregorianCalendar(int year, int month, int day, int hour, int minute,
            int second) {
        return getDatatypeFactory().newXMLGregorianCalendar(year, month, day, hour, minute, second, 0, 0);
    }

    public static long toMillis(XMLGregorianCalendar xmlCal) {
        return xmlCal != null ? xmlCal.toGregorianCalendar().getTimeInMillis() : 0;
    }

    public static Long toMillisNullable(XMLGregorianCalendar xmlCal) {
        return xmlCal != null ? xmlCal.toGregorianCalendar().getTimeInMillis() : null;
    }

    public static Date toDate(XMLGregorianCalendar xmlCal) {
        return xmlCal != null ? new Date(xmlCal.toGregorianCalendar().getTimeInMillis()) : null;
    }

    public static XMLGregorianCalendar fromNow(String timeSpec) {
        return fromNow(XmlTypeConverter.createDuration(timeSpec));
    }

    public static XMLGregorianCalendar fromNow(Duration duration) {
        return fromNow(System.currentTimeMillis(), duration);
    }

    public static XMLGregorianCalendar fromNow(long now, Duration duration) {
        XMLGregorianCalendar rv = createXMLGregorianCalendar(now);
        rv.add(duration);
        return rv;
    }

    public static XMLGregorianCalendar fromNow(long now, String timeSpec) {
        return fromNow(now, createDuration(timeSpec));
    }

    public static long toMillis(Duration duration) {
        long now = System.currentTimeMillis();
        return toMillis(fromNow(now, duration)) - now;
    }

    public static Duration createDuration(long durationInMilliSeconds) {
        return getDatatypeFactory().newDuration(durationInMilliSeconds);
    }

    public static Duration createDuration(Duration duration) {
        return getDatatypeFactory().newDuration(duration.getSign() >= 0,
                toBigInteger(duration.getField(DatatypeConstants.YEARS)),
                toBigInteger(duration.getField(DatatypeConstants.MONTHS)),
                toBigInteger(duration.getField(DatatypeConstants.DAYS)),
                toBigInteger(duration.getField(DatatypeConstants.HOURS)),
                toBigInteger(duration.getField(DatatypeConstants.MINUTES)),
                toBigDecimal(duration.getField(DatatypeConstants.SECONDS)));
    }

    public static boolean isZero(Duration duration) {
        if (duration == null) {
            return true;
        }
        return duration.getSign() == 0;
    }


    // to be used from within createDuration only (for general use it should be rewritten!!)
    private static BigDecimal toBigDecimal(Number number) {
        if (number instanceof BigDecimal) {
            return (BigDecimal) number;                     // this is the most probable case as seconds are stored as BigDecimal
        } else if (number instanceof BigInteger) {
            return BigDecimal.valueOf(number.longValue());
        } else if (number != null) {
            return new BigDecimal(number.toString());       // hack ... see https://stackoverflow.com/questions/16216248/convert-java-number-to-bigdecimal-best-way
        } else {
            return null;
        }
    }

    // to be used from within createDuration only (for general use it should be rewritten!!)
    private static BigInteger toBigInteger(Number number) {
        if (number instanceof BigInteger) {
            return (BigInteger) number;                     // this is the most probable case as fields are stored as BigIntegers
        } else if (number != null) {
            return BigInteger.valueOf(number.longValue());
        } else {
            return null;
        }
    }

    public static Duration createDuration(String lexicalRepresentation) {
        return lexicalRepresentation != null ? getDatatypeFactory().newDuration(lexicalRepresentation) : null;
    }

    public static Duration createDuration(boolean isPositive, int years, int months, int days, int hours, int minutes, int seconds) {
        return getDatatypeFactory().newDuration(isPositive, years, months, days, hours, minutes, seconds);
    }

    public static <T> T toXmlEnum(Class<T> expectedType, String stringValue) {
        if (stringValue == null) {
            return null;
        }
        for (T enumConstant: expectedType.getEnumConstants()) {
            Field field;
            try {
                field = expectedType.getField(((Enum)enumConstant).name());
            } catch (SecurityException e) {
                throw new IllegalArgumentException("Error getting field from '"+enumConstant+"' in "+expectedType, e);
            } catch (NoSuchFieldException e) {
                throw new IllegalArgumentException("Error getting field from '"+enumConstant+"' in "+expectedType, e);
            }
            XmlEnumValue annotation = field.getAnnotation(XmlEnumValue.class);
            if (annotation.value().equals(stringValue)) {
                return enumConstant;
            }
        }
        throw new IllegalArgumentException("No enum value '"+stringValue+"' in "+expectedType);
    }

    public static <T> String fromXmlEnum(T enumValue) {
        if (enumValue == null) {
            return null;
        }
        String fieldName = ((Enum)enumValue).name();
        Field field;
        try {
            field = enumValue.getClass().getField(fieldName);
        } catch (SecurityException | NoSuchFieldException e) {
            throw new IllegalArgumentException("Error getting field from "+enumValue, e);
        }
        XmlEnumValue annotation = field.getAnnotation(XmlEnumValue.class);
        return annotation.value();
    }

    public static XMLGregorianCalendar addDuration(XMLGregorianCalendar now, Duration duration) {
        XMLGregorianCalendar later = createXMLGregorianCalendar(toMillis(now));
        later.add(duration);
        return later;
    }

    public static XMLGregorianCalendar addDuration(XMLGregorianCalendar now, String duration) {
        XMLGregorianCalendar later = createXMLGregorianCalendar(toMillis(now));
        later.add(createDuration(duration));
        return later;
    }

    public static XMLGregorianCalendar addMillis(XMLGregorianCalendar now, long duration) {
        return createXMLGregorianCalendar(toMillis(now) + duration);
    }

    public static int compare(XMLGregorianCalendar o1, XMLGregorianCalendar o2) {
        if (o1 == null && o2 == null) {
            return 0;
        } else if (o1 == null) {
            return -1;
        } else if (o2 == null) {
            return 1;
        } else {
            return o1.compare(o2);
        }
    }

    public static int compareMillis(XMLGregorianCalendar o1, XMLGregorianCalendar o2) {
        if (o1 == null && o2 == null) {
            return 0;
        } else if (o1 == null) {
            return -1;
        } else if (o2 == null) {
            return 1;
        } else {
            return Long.compare(o1.toGregorianCalendar().getTimeInMillis(), o2.toGregorianCalendar().getTimeInMillis());
        }
    }

    public static boolean isBeforeNow(XMLGregorianCalendar time) {
        return toMillis(time) < System.currentTimeMillis();
    }

    public static boolean isAfterInterval(XMLGregorianCalendar reference, Duration interval, XMLGregorianCalendar now) {
        XMLGregorianCalendar endOfInterval = addDuration(reference, interval);
        return endOfInterval.compare(now) == DatatypeConstants.LESSER;
    }

    public static Duration longerDuration(Duration a, Duration b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        if (a.compare(b) == DatatypeConstants.GREATER) {
            return a;
        } else {
            return b;
        }
    }

    public static XMLGregorianCalendar laterTimestamp(XMLGregorianCalendar a, XMLGregorianCalendar b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        if (a.compare(b) == DatatypeConstants.GREATER) {
            return a;
        } else {
            return b;
        }
    }

    public static boolean isFresher(XMLGregorianCalendar theTimestamp, XMLGregorianCalendar refTimestamp) {
        if (theTimestamp == null) {
            return false;
        }
        if (refTimestamp == null) {
            return true;
        }
        return theTimestamp.compare(refTimestamp) == DatatypeConstants.GREATER;
    }

    public static Object toJavaValue(Element xmlElement, QName type) throws SchemaException {
        return toJavaValue(xmlElement, XsdTypeMapper.getXsdToJavaMapping(type));
    }

    public static <T> T toJavaValue(String stringContent, QName typeQName) {
        Class<T> javaClass = XsdTypeMapper.getXsdToJavaMapping(typeQName);
        return toJavaValue(stringContent, javaClass, false);
    }

    public static <T> T toJavaValue(String stringContent, Map<String, String> namespaces, QName typeQName) {
        return toJavaValue(stringContent, namespaces, XsdTypeMapper.getXsdToJavaMapping(typeQName));
    }

    public static <T> T toJavaValue(Element xmlElement, Class<T> type) throws SchemaException {
        if (type.equals(Element.class)) {
            return (T) xmlElement;
        } else if (type.equals(QName.class)) {
            return (T) DOMUtil.getQNameValue(xmlElement);
        } else if (PolyString.class.isAssignableFrom(type)) {
            return (T) polyStringToJava(xmlElement);
        } else {
            return toJavaValuePlain(xmlElement.getTextContent(), type, () -> " (element " + DOMUtil.getQName(xmlElement) + ")");
        }
    }

    public static <T> T toJavaValue(String textContent, Map<String, String> namespaces, Class<T> type) {
        if (type.equals(Element.class)) {
            throw new UnsupportedOperationException("Cannot convert text to Element: " + textContent);
        } else if (type.equals(QName.class)) {
            return (T) DOMUtil.getQNameValue(textContent, namespaces);
        } else if (PolyString.class.isAssignableFrom(type)) {
            return (T) new PolyString(textContent);
        } else {
            return toJavaValuePlain(textContent, type, () -> "");
        }
    }

    private static <T> T toJavaValuePlain(String textContent, Class<T> type, Supplier<String> contextSupplier) {
        if (textContent == null) {
            return null;
        }
        T javaValue = toJavaValue(textContent, type);
        if (javaValue == null) {
            throw new IllegalArgumentException("Unknown type for conversion: " + type + contextSupplier.get());
        }
        return javaValue;
    }

    public static <T> T toJavaValue(String stringContent, Class<T> type) {
        return toJavaValue(stringContent, type, false);
    }

    @SuppressWarnings("unchecked")
    public static <T> T toJavaValue(String stringContent, Class<T> type, boolean exceptionOnUnknown) {
        if (type.equals(String.class)) {
            return (T) stringContent;
        } else if (type.equals(char.class)) {
            return (T) (new Character(stringContent.charAt(0)));
        } else if (type.equals(File.class)) {
            return (T) new File(stringContent);
        } else if (type.equals(Integer.class)) {
            return (T) Integer.valueOf(stringContent);
        } else if (type.equals(int.class)) {
            return (T) Integer.valueOf(stringContent);
        } else if (type.equals(Short.class) || type.equals(short.class)) {
            return (T) Short.valueOf(stringContent);
        } else if (type.equals(Long.class)) {
            return (T) Long.valueOf(stringContent);
        } else if (type.equals(long.class)) {
            return (T) Long.valueOf(stringContent);
        } else if (type.equals(Byte.class)) {
            return (T) Byte.valueOf(stringContent);
        } else if (type.equals(byte.class)) {
            return (T) Byte.valueOf(stringContent);
        } else if (type.equals(float.class)) {
            return (T) Float.valueOf(stringContent);
        } else if (type.equals(Float.class)) {
            return (T) Float.valueOf(stringContent);
        } else if (type.equals(double.class)) {
            return (T) Double.valueOf(stringContent);
        } else if (type.equals(Double.class)) {
            return (T) Double.valueOf(stringContent);
        } else if (type.equals(BigInteger.class)) {
            return (T) new BigInteger(stringContent);
        } else if (type.equals(BigDecimal.class)) {
            return (T) new BigDecimal(stringContent);
        } else if (type.equals(byte[].class)) {
            byte[] decodedData = Base64.decodeBase64(stringContent);
            return (T) decodedData;
        } else if (type.equals(boolean.class) || Boolean.class.isAssignableFrom(type)) {
            // TODO: maybe we will need more intelligent conversion, e.g. to trim spaces, case insensitive, etc.
            return (T) Boolean.valueOf(stringContent);
        } else if (type.equals(GregorianCalendar.class)) {
            return (T) getDatatypeFactory().newXMLGregorianCalendar(stringContent).toGregorianCalendar();
        } else if (XMLGregorianCalendar.class.isAssignableFrom(type)) {
            // MID-6361: We need to make XmlGregorian - Gregorian - XmlGregorian roundtrip to make sure time zone is includded
            return (T) createXMLGregorianCalendar(stringContent);
        } else if (type.equals(ZonedDateTime.class)) {
            return (T) ZonedDateTime.parse(stringContent);
        } else if (Duration.class.isAssignableFrom(type)) {
            return (T) getDatatypeFactory().newDuration(stringContent);
        } else if (type.equals(PolyString.class)) {
            return (T) new PolyString(stringContent);
        } else if (type.equals(UniformItemPath.class) || type.equals(ItemPath.class)) {
            throw new UnsupportedOperationException("Path conversion not supported yet");
        } else {
            if (exceptionOnUnknown) {
                throw new IllegalArgumentException("Unknown conversion type "+type);
            } else {
                return null;
            }
        }
    }

    /**
     * Parse PolyString from DOM element.
     */
    private static PolyString polyStringToJava(Element polyStringElement) throws SchemaException {
        List<Element> children = DOMUtil.listChildElements(polyStringElement);
        Element origElement = DOMUtil.getNamedElement(children, PrismConstants.POLYSTRING_ELEMENT_ORIG_QNAME);
        if (origElement == null) {
            // Check for a special syntactic short-cut. If the there are no child elements use the text content of the
            // element as the value of orig
            if (children.isEmpty()) {
                String orig = polyStringElement.getTextContent();
                return new PolyString(orig);
            } else {
                throw new SchemaException("Missing element "+PrismConstants.POLYSTRING_ELEMENT_ORIG_QNAME+" in polystring element "+
                        DOMUtil.getQName(polyStringElement));
            }
        } else {
            String orig = origElement.getTextContent();
            Element normElement = DOMUtil.getNamedElement(children, PrismConstants.POLYSTRING_ELEMENT_NORM_QNAME);
            String norm = normElement != null ? normElement.getTextContent() : null;
            return new PolyString(orig, norm);
        }
    }

}
