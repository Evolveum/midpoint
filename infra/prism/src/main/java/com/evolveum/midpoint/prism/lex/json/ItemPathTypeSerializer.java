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

import com.evolveum.midpoint.prism.marshaller.ItemPathHolder;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class ItemPathTypeSerializer extends JsonSerializer<ItemPathType> {

	@Override
	public void serialize(@NotNull ItemPathType value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
		ItemPathHolder xpath = new ItemPathHolder(value.getItemPath(), true);
		String path = xpath.getXPathWithDeclarations();
		jgen.writeObject(path);
		
	}
	
	@Override
	public void serializeWithType(@NotNull ItemPathType value, JsonGenerator jgen, SerializerProvider provider,
			TypeSerializer typeSer) throws IOException {
		serialize(value, jgen, provider);
	}
}
