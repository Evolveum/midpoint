/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.lex.json;

import java.io.IOException;
import java.io.StringWriter;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.impl.lex.json.yaml.MidpointYAMLFactory;
import com.evolveum.midpoint.prism.impl.lex.json.yaml.MidpointYAMLGenerator;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

public class YamlWriter extends AbstractWriter {

    public YAMLGenerator createJacksonGenerator(StringWriter out) throws SchemaException{
        try {
            MidpointYAMLFactory factory = new MidpointYAMLFactory();
            MidpointYAMLGenerator generator = (MidpointYAMLGenerator) factory.createGenerator(out);
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
//        mapper.enableDefaultTyping(DefaultTyping.NON_CONCRETE_AND_ARRAYS, As.EXISTING_PROPERTY);
//        mapper.configure(SerializationFeaCture.);
//        mapper.setSerializationInclusion(Include.NON_NULL);
        mapper.registerModule(createSerializerModule());

        mapper.disableDefaultTyping();

//        mapper.enableDefaultTyping(DefaultTyping.NON_CONCRETE_AND_ARRAYS);
//        mapper.enableDefaultTyping(DefaultTyping.NON_FINAL, As.EXISTING_PROPERTY);
//        //mapper.enableDefaultTyping(DefaultTyping.NON_FINAL, As.EXTERNAL_PROPERTY);
//        mapper.enableDefaultTyping(DefaultTyping.NON_FINAL, As.PROPERTY);

        return mapper;
    }

    private Module createSerializerModule(){
        SimpleModule module = new SimpleModule("MidpointModule", new Version(0, 0, 0, "aa"));
        module.addSerializer(QName.class, new QNameSerializer());
        module.addSerializer(PolyString.class, new PolyStringSerializer());
        module.addSerializer(ItemPath.class, new ItemPathSerializer());
        module.addSerializer(ItemPathType.class, new ItemPathTypeSerializer());
//        module.addSerializer(JAXBElement.class, new JaxbElementSerializer());
        module.addSerializer(XMLGregorianCalendar.class, new XmlGregorianCalendarSerializer());
        module.addSerializer(Element.class, new DomElementSerializer());
        return module;
    }

    @Override
    protected boolean supportsInlineTypes() {
        return true;
    }

    @Override
    protected void writeInlineType(QName typeName, JsonSerializationContext ctx) throws IOException {
        ctx.generator.writeTypeId(QNameUtil.qNameToUri(typeName, false, '/'));
    }

    @Override
    protected void resetInlineTypeIfPossible(JsonSerializationContext ctx) {
        ((MidpointYAMLGenerator) ctx.generator).resetTypeId();                    // brutal hack
    }
}
