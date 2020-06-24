/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.lex.json.writer;

import com.evolveum.midpoint.prism.impl.marshaller.ItemPathSerializerTemp;
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
        jgen.writeObject(ItemPathSerializerTemp.serializeWithForcedDeclarations(value));

    }

    @Override
    public void serializeWithType(@NotNull ItemPathType value, JsonGenerator jgen, SerializerProvider provider,
            TypeSerializer typeSer) throws IOException {
        serialize(value, jgen, provider);
    }
}
