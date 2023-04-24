/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import jakarta.xml.bind.JAXBElement;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EntryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ParamsType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

public class ParamsTypeUtil {

    public static ParamsType toParamsType(Map<String, Collection<String>> paramsMap){
        Set<Entry<String, Collection<String>>> params = paramsMap.entrySet();
        if (!params.isEmpty()) {
            ParamsType paramsType = new ParamsType();

            for (Entry<String, Collection<String>> entry : params) {
                if (entry.getValue() == null) {
                    paramsType.getEntry().add(createEntryElement(entry.getKey(), null));
                } else {
                    for (String value: entry.getValue()) {
                        paramsType.getEntry().add(createEntryElement(entry.getKey(), value));
                    }
                }
            }
            return paramsType;
        }
        return null;
    }

    private static EntryType createEntryElement(String key, String value) {
        EntryType entryType = new EntryType();
        entryType.setKey(key);
        if (value != null) {
            entryType.setEntryValue(new JAXBElement<>(SchemaConstants.C_PARAM_VALUE, Serializable.class, value));
        }
        return entryType;
    }

    public static Map<String, Serializable> fromParamsType(ParamsType paramsType, PrismContext prismContext) throws SchemaException {
        if (paramsType != null) {
            Map<String, Serializable> params = new HashMap<>();
            for (EntryType entry : paramsType.getEntry()) {
                Serializable realValue;
                if (entry.getEntryValue() != null) {
                    Serializable value = (Serializable) entry.getEntryValue().getValue();
                    if (value instanceof RawType) {
                        RawType raw = (RawType) value;
                        if (raw.isParsed()) {
                            realValue = raw.getAlreadyParsedValue().getRealValue();
                        } else {
                            realValue = raw.guessFormattedValue();
                        }
                    } else {
                        realValue = value;
                    }
                } else {
                    realValue = null;
                }
                params.put(entry.getKey(), realValue);
            }
            return params;
        }
        return null;
    }

    public static Map<String, Collection<String>> fromParamsType(ParamsType paramsType) {
        if (paramsType != null) {
            Map<String, Collection<String>> params = new HashMap<>();
            for (EntryType entry : paramsType.getEntry()) {
                if (entry.getEntryValue() != null) {
                    String newValue = extractString(entry.getEntryValue());
                    Collection<String> values = params.computeIfAbsent(entry.getKey(), k -> new ArrayList<>());
                    values.add(newValue);
                }
            }
            return params;
        }
        return null;
    }

    private static String extractString(JAXBElement<?> jaxbElement) {
        Object value = jaxbElement.getValue();
        if (value instanceof RawType) {
            return ((RawType) value).extractString();
        } else {
            return value.toString();
        }
    }

}
