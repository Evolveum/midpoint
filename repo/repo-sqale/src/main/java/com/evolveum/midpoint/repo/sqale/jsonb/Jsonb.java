/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.jsonb;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.jetbrains.annotations.NotNull;

/**
 * Type representing JSONB columns in PostgreSQL database as a wrapped string.
 */
public class Jsonb {

    public final String value;

    public Jsonb(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        // Pure JSON, this is critical for JSONB array serialization in PG JDBC driver.
        return value;
    }

    // static stuff for parse/format

    public static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        class BigDecimalSerializer extends JsonSerializer<BigDecimal> {
            @Override
            public void serialize(BigDecimal value, JsonGenerator gen,
                    SerializerProvider serializerProvider) throws IOException {
                gen.writeRawValue(value.toString());
            }
        }

        SimpleModule module = new SimpleModule();
        module.addSerializer(BigDecimal.class, new BigDecimalSerializer());
        MAPPER.registerModule(module);
        MAPPER.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    }

    /** Returns JSONB object from map or null if map is null or empty. */
    public static Jsonb fromMap(Map<String, ?> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }

        return fromObject(map);
    }

    /** Returns JSONB array from list or null if map is null or empty. */
    public static Jsonb fromList(List<Object> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }

        return fromObject(list);
    }

    /** Returns JSONB object for provided parameter, even if null. */
    @NotNull
    private static Jsonb fromObject(Object object) {
        try {
            return new Jsonb(MAPPER.writeValueAsString(object));
        } catch (JsonProcessingException e) {
            throw new JsonbException("Unexpected error while writing JSONB value", e);
        }
    }

    @NotNull
    public static Map<String, Object> toMap(@NotNull Jsonb jsonb) {
        try {
            //noinspection unchecked
            return MAPPER.readValue(jsonb.value, Map.class);
        } catch (JsonProcessingException e) {
            throw new JsonbException("Unexpected error while reading JSONB value", e);
        }
    }

    @NotNull
    public static List<Object> toList(@NotNull Jsonb jsonb) {
        try {
            //noinspection unchecked
            return MAPPER.readValue(jsonb.value, List.class);
        } catch (JsonProcessingException e) {
            throw new JsonbException("Unexpected error while reading JSONB value", e);
        }
    }
}
