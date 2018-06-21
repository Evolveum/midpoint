/**
 * Copyright (c) 2010-2017 Evolveum
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
package com.evolveum.midpoint.schema.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.bind.JAXBElement;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
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

	public static Map<String, Serializable> fromParamsType(ParamsType paramsType, PrismContext prismContext) throws SchemaException{
		if (paramsType != null) {
			Map<String, Serializable> params = new HashMap<>();
			Serializable realValue = null;
			for (EntryType entry : paramsType.getEntry()) {
				if (entry.getEntryValue() != null){

					Serializable value = (Serializable) entry.getEntryValue().getValue();
					if (value instanceof RawType){
						XNode xnode = ((RawType) value).getXnode();
						if (xnode instanceof PrimitiveXNode){
							realValue = ((PrimitiveXNode) xnode).getGuessedFormattedValue();
						}
					} else {
						realValue = value;
					}
				}
				params.put(entry.getKey(), (Serializable) (realValue));
			}

			return params;
		}
		return null;
	}

	public static Map<String, Collection<String>> fromParamsType(ParamsType paramsType) throws SchemaException {
		if (paramsType != null) {
			Map<String, Collection<String>> params = new HashMap<>();
			Serializable realValue = null;
			for (EntryType entry : paramsType.getEntry()) {
				if (entry.getEntryValue() != null) {
					String newValue = extractString(entry.getEntryValue());
					Collection<String> values = params.get(entry.getKey());
					if (values == null) {
						values = new ArrayList<>();
						params.put(entry.getKey(), values);
					}
					values.add(newValue);
				}
			}
			return params;
		}
		return null;
	}

	private static String extractString(JAXBElement<?> jaxbElement) throws SchemaException {
		Object value = jaxbElement.getValue();
		if (value instanceof RawType){
			XNode xnode = ((RawType) value).getXnode();
			if (xnode instanceof PrimitiveXNode) {
				value = ((PrimitiveXNode) xnode).getStringValue();
			}
		}
		return value.toString();
	}

}
