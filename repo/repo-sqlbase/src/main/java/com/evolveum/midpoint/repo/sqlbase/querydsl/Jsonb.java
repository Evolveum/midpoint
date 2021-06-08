/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase.querydsl;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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
        return "JSONB " + value;
    }

    // static stuff for parse/format

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static Jsonb from(Map<String, Object> map) throws IOException {
        if (map == null || map.isEmpty()) {
            return null;
        }

        return new Jsonb(MAPPER.writeValueAsString(map));
    }

    public static Map<String, Object> toMap(Jsonb jsonb) throws JsonProcessingException {
        //noinspection unchecked
        return MAPPER.readValue(jsonb.value, Map.class);
    }
}
