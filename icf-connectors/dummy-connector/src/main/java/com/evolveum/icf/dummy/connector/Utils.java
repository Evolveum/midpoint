/*
 * Copyright (c) 2010-201ž Evolveum
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
package com.evolveum.icf.dummy.connector;

import java.util.List;
import java.util.Set;

import org.identityconnectors.framework.common.objects.Attribute;

/**
 *
 * @author lazyman
 * @author Radovan Semancik
 *
 */
public class Utils {

	public static String getMandatoryStringAttribute(Set<Attribute> attributes, String attributeName) {
		String value = getAttributeSingleValue(attributes, attributeName, String.class);
		if (value == null) {
			throw new IllegalArgumentException("No value for mandatory attribute "+attributeName);
		}
		return value;
	}

	@SuppressWarnings("unchecked")
	public static <T> T getAttributeSingleValue(Set<Attribute> attributes, String attributeName, Class<T> type) {
		for (Attribute attr : attributes) {
			if (attributeName.equals(attr.getName())) {
				List<Object> values = attr.getValue();
				if (values == null || values.isEmpty()) {
					return null;
				}
				if (values.size()>1) {
					throw new IllegalArgumentException("Multiple values for single valued attribute "+attributeName);
				}
				if (!(type.isAssignableFrom(values.get(0).getClass()))) {
					throw new IllegalArgumentException("Illegal value type "+values.get(0).getClass().getName()+" for attribute "+attributeName+", expecting type "+type.getClass().getName());
				}
				return (T)values.get(0);
			}
		}
		return null;
	}

    public static void notNull(Object object, String message) {
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void notNullArgument(Object object, String arg) {
        notNull(object, "Argument '" + arg + "' can't be null.");
    }

    public static void notEmpty(String value, String message) {
        notNull(value, message);

        if (value.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void notEmptyArgument(String value, String arg) {
        notEmpty(value, "Argument '" + arg + "' can't be empty.");
    }
}
