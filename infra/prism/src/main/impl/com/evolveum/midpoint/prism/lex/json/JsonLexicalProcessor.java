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

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import org.jetbrains.annotations.NotNull;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

public class JsonLexicalProcessor extends AbstractJsonLexicalProcessor {

	public JsonLexicalProcessor(@NotNull SchemaRegistry schemaRegistry) {
		super(schemaRegistry);
	}

	@Override
	public boolean canRead(@NotNull File file) throws IOException {
		return file.getName().endsWith(".json");
	}

	@Override
	public boolean canRead(@NotNull String dataString) {
		return dataString.startsWith("{");
	}

    @Override
    protected com.fasterxml.jackson.core.JsonParser createJacksonParser(InputStream stream) throws SchemaException, IOException {
        JsonFactory factory = new JsonFactory();
        try {
            return factory.createParser(stream);
        } catch (IOException e) {
            throw e;
        }
    }

	public JsonGenerator createJacksonGenerator(StringWriter out) throws SchemaException{
		return createJsonGenerator(out);
	}
	private JsonGenerator createJsonGenerator(StringWriter out) throws SchemaException{
		try {
			JsonFactory factory = new JsonFactory();
			JsonGenerator generator = factory.createGenerator(out);
			generator.setPrettyPrinter(new DefaultPrettyPrinter());
			generator.setCodec(configureMapperForSerialization());

			return generator;
		} catch (IOException ex){
			throw new SchemaException("Schema error during serializing to JSON.", ex);
		}

	}

	private ObjectMapper configureMapperForSerialization(){
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
		mapper.setSerializationInclusion(Include.NON_NULL);
		mapper.registerModule(createSerializerModule());
		mapper.setAnnotationIntrospector(new JaxbAnnotationIntrospector());
		return mapper;
	}

	private Module createSerializerModule(){
		SimpleModule module = new SimpleModule("MidpointModule", new Version(0, 0, 0, "aa"));
		module.addSerializer(QName.class, new QNameSerializer());
		module.addSerializer(PolyString.class, new PolyStringSerializer());
		module.addSerializer(ItemPath.class, new ItemPathSerializer());
		module.addSerializer(ItemPathType.class, new ItemPathTypeSerializer());
		module.addSerializer(XMLGregorianCalendar.class, new XmlGregorianCalendarSerializer());
//		module.addSerializer(Element.class, new DomElementJsonSerializer());
//		module.addSerializer(JAXBElement.class, new JaxbElementSerializer());
		return module;
	}

	@Override
	protected QName tagToTypeName(Object tid, AbstractJsonLexicalProcessor.JsonParsingContext ctx) {
		return null;
	}

	@Override
	protected boolean supportsInlineTypes() {
		return false;
	}

	@Override
	protected void writeInlineType(QName typeName, JsonSerializationContext ctx) throws IOException {
		throw new IllegalStateException("JSON cannot write type information using tags.");
	}
}
