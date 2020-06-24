/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.lex.json.writer;

import com.evolveum.midpoint.prism.SerializationContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.StringWriter;

/**
 * TODO
 */
class JsonWritingContext extends WritingContext<JsonGenerator> {

    JsonWritingContext(SerializationContext prismSerializationContext) {
        super(prismSerializationContext);
    }

    JsonGenerator createJacksonGenerator(StringWriter out) {
        try {
            JsonFactory factory = new JsonFactory();
            JsonGenerator generator = factory.createGenerator(out);
            generator.setPrettyPrinter(new DefaultPrettyPrinter());
            generator.setCodec(configureMapperForSerialization());
            return generator;
        } catch (IOException ex) {
            throw new SystemException("Couldn't create Jackson generator for JSON: " + ex.getMessage(), ex);
        }
    }

    private ObjectMapper configureMapperForSerialization() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.registerModule(createSerializerModule());
        mapper.setAnnotationIntrospector(new JaxbAnnotationIntrospector());
        return mapper;
    }

    private Module createSerializerModule() {
        SimpleModule module = new SimpleModule("MidpointModule", new Version(0, 0, 0, "aa"));
        module.addSerializer(QName.class, new QNameSerializer());
        module.addSerializer(PolyString.class, new PolyStringSerializer());
        module.addSerializer(ItemPath.class, new ItemPathSerializer());
        module.addSerializer(ItemPathType.class, new ItemPathTypeSerializer());
        module.addSerializer(XMLGregorianCalendar.class, new XmlGregorianCalendarSerializer());
//        module.addSerializer(Element.class, new DomElementJsonSerializer());
//        module.addSerializer(JAXBElement.class, new JaxbElementSerializer());
        return module;
    }

    protected boolean supportsInlineTypes() {
        return false;
    }

    @Override
    void writeInlineType(QName typeName) {
        throw new UnsupportedOperationException();
    }

    @Override
    void resetInlineTypeIfPossible() {
    }

    @Override
    boolean supportsMultipleDocuments() {
        return false;
    }

    @Override
    void newDocument() {
        throw new UnsupportedOperationException();
    }
}
