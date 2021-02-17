/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.lex.json.writer;

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
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.SerializationContext;
import com.evolveum.midpoint.prism.impl.lex.json.yaml.MidpointYAMLFactory;
import com.evolveum.midpoint.prism.impl.lex.json.yaml.MidpointYAMLGenerator;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SystemException;

public class YamlWritingContext extends WritingContext<MidpointYAMLGenerator> {

    YamlWritingContext(@Nullable SerializationContext prismSerializationContext) {
        super(prismSerializationContext);
    }

    @Override
    MidpointYAMLGenerator createJacksonGenerator(StringWriter out) {
        try {
            MidpointYAMLFactory factory = new MidpointYAMLFactory();
            MidpointYAMLGenerator generator = (MidpointYAMLGenerator) factory.createGenerator(out);
            generator.setPrettyPrinter(new DefaultPrettyPrinter());
            generator.setCodec(configureMapperForSerialization());
            return generator;
        } catch (IOException ex) {
            throw new SystemException("Couldn't create Jackson generator for YAML: " + ex.getMessage(), ex);
        }
    }

    private ObjectMapper configureMapperForSerialization() {
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
    protected void writeInlineType(QName typeName) throws IOException {
        generator.writeTypeId(QNameUtil.qNameToUri(typeName, false, '/'));
    }

    @Override
    protected void resetInlineTypeIfPossible() {
        generator.resetTypeId(); // brutal hack
    }

    @Override
    boolean supportsMultipleDocuments() {
        return true;
    }

    @Override
    void newDocument() throws IOException {
        generator.newDocument();
    }
}
