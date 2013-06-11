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

import javax.xml.bind.annotation.XmlEnum;

import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;

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
		if (expectedType == boolean.class && rawValue instanceof Boolean) {
			return (T) ((Boolean)rawValue);
		}
		if (expectedType == int.class && rawValue instanceof Integer) {
			return (T)((Integer)rawValue);
		}
		if (expectedType == long.class && rawValue instanceof Long) {
			return (T)((Long)rawValue);
		}
		if (expectedType == float.class && rawValue instanceof Float) {
			return (T)((Float)rawValue);
		}
		if (expectedType == double.class && rawValue instanceof Double) {
			return (T)((Double)rawValue);
		}
		if (expectedType == byte.class && rawValue instanceof Byte) {
			return (T)((Byte)rawValue);
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
		
		throw new IllegalArgumentException("Expected "+expectedType+" type, but got "+rawValue.getClass());
	}

}
