/*
 * Copyright (c) 2013 Evolveum
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
package com.evolveum.midpoint.prism.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.Date;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.apache.commons.lang.time.DateUtils;

import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Generic universal type converter. It is supposed to covert anything to anything as long
 * as there is any way to convert it. This means converting string containing a decimal representation
 * of a number to int, PolyString to string, etc. This is supposed to work in a fashion similar to
 * many scripting languages (e.g. perl) where the programmer does not really care about the type
 * and the type conversion is done automatically.
 * 
 * @author Radovan Semancik
 */
public class JavaTypeConverter {
	
    final static String[] DATE_FORMATS = {
    	"yyyy-MM-dd'T'HH:mm:ss.SSSX",
    	"yyyy-MM-dd'T'HH:mm:ssz",
    	"EEE, dd MMM yyyy HH:mm:ss z",
    	"EEE, dd MMM yyyy HH:mm zzzz",
    	"EEE MMM dd HH:mm:ss z yyyy",
    	"yyyy-MM-dd'T'HH:mm:ssZ",
    	"yyyy-MM-dd'T'HH:mm:ss.SSSzzzz",
    	"yyyy-MM-dd'T'HH:mm:sszzzz",
    	"yyyy-MM-dd'T'HH:mm:ss z",
    	"yyyy-MM-dd'T'HH:mm:ss",
    	"yyyy-MM-dd'T'HHmmss.SSSz",
    	"yyyy-MM-dd"
    };
	
	public static <T> T convert(Class<T> expectedType, Object rawValue) {
		if (rawValue == null || expectedType.isInstance(rawValue)) {
			return (T)rawValue;
		}
		if (rawValue instanceof PrismPropertyValue<?>) {
			rawValue = ((PrismPropertyValue<?>)rawValue).getValue();
		}
		// This really needs to be checked twice
		if (rawValue == null || expectedType.isInstance(rawValue)) {
			return (T)rawValue;
		}
		
		// Primitive types
		
		// boolean
		if (expectedType == boolean.class && rawValue instanceof Boolean) {
			return (T) ((Boolean)rawValue);
		}
		if (expectedType == Boolean.class && rawValue instanceof String) {
			return (T) (Boolean)Boolean.parseBoolean(((String)rawValue));
		}
		if (expectedType == Boolean.class && rawValue instanceof PolyString) {
			return (T) (Boolean)Boolean.parseBoolean(((PolyString)rawValue).toString());
		}
		if (expectedType == boolean.class && rawValue instanceof String) {
			return (T) (Boolean)Boolean.parseBoolean(((String)rawValue));
		}
		if (expectedType == boolean.class && rawValue instanceof PolyString) {
			return (T) (Boolean)Boolean.parseBoolean(((PolyString)rawValue).toString());
		}
		if (expectedType == String.class && rawValue instanceof Boolean) {
			return (T) rawValue.toString();
		}
		
		// int
		if (expectedType == int.class && rawValue instanceof Integer) {
			return (T)((Integer)rawValue);
		}
		if (expectedType == Integer.class && rawValue instanceof String) {
			return (T) (Integer)Integer.parseInt(((String)rawValue));
		}
		if (expectedType == int.class && rawValue instanceof String) {
			return (T) (Integer)Integer.parseInt(((String)rawValue));
		}
		if (expectedType == String.class && rawValue instanceof Integer) {
			return (T) rawValue.toString();
		}
		
		if (expectedType == long.class && rawValue instanceof Long) {
			return (T)((Long)rawValue);
		}
		if (expectedType == Long.class && rawValue instanceof String) {
			return (T) (Long)Long.parseLong(((String)rawValue));
		}
		if (expectedType == long.class && rawValue instanceof String) {
			return (T) (Long)Long.parseLong(((String)rawValue));
		}
		if (expectedType == String.class && rawValue instanceof Long) {
			return (T) rawValue.toString();
		}
				
		if (expectedType == float.class && rawValue instanceof Float) {
			return (T)((Float)rawValue);
		}
		if (expectedType == Float.class && rawValue instanceof String) {
			return (T) (Float)Float.parseFloat(((String)rawValue));
		}
		if (expectedType == float.class && rawValue instanceof String) {
			return (T) (Float)Float.parseFloat(((String)rawValue));
		}
		if (expectedType == String.class && rawValue instanceof Float) {
			return (T) rawValue.toString();
		}
		
		if (expectedType == double.class && rawValue instanceof Double) {
			return (T)((Double)rawValue);
		}
		if (expectedType == Double.class && rawValue instanceof String) {
			return (T) (Double)Double.parseDouble(((String)rawValue));
		}
		if (expectedType == double.class && rawValue instanceof String) {
			return (T) (Double)Double.parseDouble(((String)rawValue));
		}
		if (expectedType == String.class && rawValue instanceof Float) {
			return (T) rawValue.toString();
		}
		
		if (expectedType == byte.class && rawValue instanceof Byte) {
			return (T)((Byte)rawValue);
		}
		if (expectedType == Byte.class && rawValue instanceof String) {
			return (T) (Byte)Byte.parseByte(((String)rawValue));
		}
		if (expectedType == byte.class && rawValue instanceof String) {
			return (T) (Byte)Byte.parseByte(((String)rawValue));
		}
		if (expectedType == String.class && rawValue instanceof Byte) {
			return (T) rawValue.toString();
		}

		if (expectedType == BigInteger.class && rawValue instanceof String) {
			return (T) new BigInteger(((String)rawValue));
		}
		if (expectedType == String.class && rawValue instanceof BigInteger) {
			return (T) ((BigInteger)rawValue).toString();
		}
		
		if (expectedType == BigDecimal.class && rawValue instanceof String) {
			return (T) new BigDecimal(((String)rawValue));
		}
		if (expectedType == String.class && rawValue instanceof BigDecimal) {
			return (T) ((BigDecimal)rawValue).toString();
		}
		
		if (expectedType == PolyString.class && rawValue instanceof String) {
			return (T) new PolyString((String)rawValue);
		}
		if (expectedType == PolyStringType.class && rawValue instanceof String) {
			PolyStringType polyStringType = new PolyStringType();
			polyStringType.setOrig((String)rawValue);
			return (T) polyStringType;
		}
		if (expectedType == String.class && rawValue instanceof PolyString) {
			return (T)((PolyString)rawValue).getOrig();
		}
		if (expectedType == String.class && rawValue instanceof PolyStringType) {
			return (T)((PolyStringType)rawValue).getOrig();
		}
		if (expectedType == PolyString.class && rawValue instanceof PolyStringType) {
			return (T) ((PolyStringType)rawValue).toPolyString();
		}
		if (expectedType == PolyString.class && rawValue instanceof Integer) {
			return (T) new PolyString(((Integer)rawValue).toString());
		}
		if (expectedType == PolyStringType.class && rawValue instanceof PolyString) {
			PolyStringType polyStringType = new PolyStringType((PolyString)rawValue);
			return (T) polyStringType;
		}
		if (expectedType == PolyStringType.class && rawValue instanceof Integer) {
			PolyStringType polyStringType = new PolyStringType(((Integer)rawValue).toString());
			return (T) polyStringType;
		}
		
		// Date and time
		if (expectedType == XMLGregorianCalendar.class && rawValue instanceof Long) {
			XMLGregorianCalendar xmlCalType = XmlTypeConverter.createXMLGregorianCalendar((Long)rawValue);
			return (T) xmlCalType;
		}
		if (expectedType == XMLGregorianCalendar.class && rawValue instanceof String) {
			XMLGregorianCalendar xmlCalType = magicDateTimeParse((String)rawValue);
			return (T) xmlCalType;
		}
		if (expectedType == String.class && rawValue instanceof XMLGregorianCalendar) {
			return (T) ((XMLGregorianCalendar)rawValue).toXMLFormat();
		}
		if (expectedType == Long.class && rawValue instanceof XMLGregorianCalendar) {
			return (T) (Long) XmlTypeConverter.toMillis((XMLGregorianCalendar)rawValue);
		}
		
		// XML Enums (JAXB)
		if (expectedType.isEnum() && expectedType.getAnnotation(XmlEnum.class) != null && rawValue instanceof String) {
			return XmlTypeConverter.toXmlEnum(expectedType, (String)rawValue);
		}
		if (expectedType == String.class && rawValue.getClass().isEnum() && rawValue.getClass().getAnnotation(XmlEnum.class) != null) {
			return (T) XmlTypeConverter.fromXmlEnum(rawValue);
		}
		
		// Java Enums
		if (expectedType.isEnum() && rawValue instanceof String) {
			return (T) Enum.valueOf((Class<Enum>)expectedType, (String)rawValue);
		}
		if (expectedType == String.class && rawValue.getClass().isEnum()) {
			return (T) rawValue.toString();
		}
		
		//QName
		if (expectedType == QName.class && rawValue instanceof QName){
			return (T) rawValue;
		}
		if (expectedType == QName.class && rawValue instanceof String){
			return (T) QNameUtil.uriToQName((String)rawValue);
		}
		
		throw new IllegalArgumentException("Expected "+expectedType+" type, but got "+rawValue.getClass());
	}

	private static XMLGregorianCalendar magicDateTimeParse(String stringDate) {
		try {
			return XmlTypeConverter.createXMLGregorianCalendar(stringDate);
		} catch (IllegalArgumentException e) {
			// No XML format. This is still quite OK. It we will try other formats ...
		}
		Date date;
		try {
			date = DateUtils.parseDate(stringDate, DATE_FORMATS);
		} catch (ParseException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
		return XmlTypeConverter.createXMLGregorianCalendar(date);
	}

}
