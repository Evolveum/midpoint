/*
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

package com.evolveum.midpoint.prism.lex.json;

import com.evolveum.midpoint.prism.marshaller.XNodeProcessorEvaluationMode;
import com.evolveum.midpoint.prism.util.JavaTypeConverter;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.prism.xnode.ValueParser;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.util.Map;

public class JsonNullValueParser<T> implements ValueParser<T> {

	public JsonNullValueParser() {
	}

	@Override
	public T parse(QName typeName, XNodeProcessorEvaluationMode mode) throws SchemaException {
		Class clazz = XsdTypeMapper.toJavaType(typeName);
		if (clazz == null) {
			throw new SchemaException("Unsupported type " + typeName);
		}
		return (T) JavaTypeConverter.convert(clazz, "");
	}

	@Override
	public boolean isEmpty() {
		return true;
	}

	@Override
	public String getStringValue() {
		return "";
	}

	@Override
	public String toString() {
		return "JsonNullValueParser";
	}

    @Override
    public Map<String, String> getPotentiallyRelevantNamespaces() {
        return null;                // TODO implement
    }

    Element asDomElement() throws IOException {
		return null;
	}
}
