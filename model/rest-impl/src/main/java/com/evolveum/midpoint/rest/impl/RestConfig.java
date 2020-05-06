/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.rest.impl;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismSerializer;
import com.evolveum.midpoint.prism.SerializationOptions;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Spring configuration for MVC-based REST service. TODO: experimental as of early 2020
 * This prepares needed (de)serializers and also drives the package scan for REST controllers.
 */
@ComponentScan
@EnableWebMvc
public class RestConfig extends WebMvcConfigurationSupport {

    @Bean
    public MappingJackson2HttpMessageConverter myConverter(ObjectMapper objectMapper) {
        return new MappingJackson2HttpMessageConverter(objectMapper);
    }

    @Bean
    public ObjectMapper jsonObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        SimpleModule module = new SimpleModule();
        module.addSerializer(PrismObject.class, prismObjectJsonSerializer());
        objectMapper.registerModule(module);

        return objectMapper;
    }

    @SuppressWarnings("rawtypes") // no <?>, otherwise the line module.addSerializer doesn't compile
    private JsonSerializer<PrismObject> prismObjectJsonSerializer() {
        return new JsonSerializer<PrismObject>() {
            @Override
            public void serialize(PrismObject value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                PrismSerializer<String> prismSerializer = value.getPrismContext().jsonSerializer()
                        .options(SerializationOptions.createSerializeReferenceNames());
                try {
                    // TODO: this is just one instance of from MidpointAbstractProvider#writeTo - how much more do we need?
                    String rawJson = prismSerializer.serialize(value);
                    gen.writeRaw(rawJson);
                } catch (SchemaException e) {
                    throw new IOException("JSON serialization problems", e);
                }
            }
        };
    }
}
