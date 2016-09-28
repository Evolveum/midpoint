/*
 * Copyright (c) 2010-2016 Evolveum
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

package com.evolveum.midpoint.prism.parser.json;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;

public class JsonParser extends AbstractParser {
	
	@Override
	public boolean canParse(File file) throws IOException {
		if (file == null) {
			return false;
		}
		return file.getName().endsWith(".json");
	}

	@Override
	public boolean canParse(String dataString) {
		if (dataString == null) {
			return false;
		}
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

    @Override
	protected com.fasterxml.jackson.core.JsonParser createJacksonParser(String dataString) throws SchemaException {
		JsonFactory factory = new JsonFactory();
		try {
			return factory.createParser(dataString);
		} catch (IOException e) {
			throw new SchemaException("Cannot create JSON parser: " + e.getMessage(), e);
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
//		module.addSerializer(Element.class, new DomElementJsonSerializer());
//		module.addSerializer(JAXBElement.class, new JaxbElementSerializer());
		return module;
	}

	@Override
	protected boolean serializeExplicitType(PrimitiveXNode primitive, QName explicitType, JsonGenerator generator) throws JsonGenerationException, IOException {
		if (explicitType != null) {
				generator.writeStartObject();
				generator.writeStringField(TYPE_DEFINITION, QNameUtil.qNameToUri(primitive.getTypeQName()));
				generator.writeObjectField(VALUE_FIELD, primitive.getValue());
				generator.writeEndObject();
				return true;
			}
		return false;
	}

	@Override
	protected void writeExplicitType(QName explicitType, JsonGenerator generator) throws JsonProcessingException, IOException {
		generator.writeObjectField("@type", explicitType);
	}

	
	

}
