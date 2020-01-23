/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.impl.lex.json;

import java.io.IOException;

import com.evolveum.midpoint.prism.impl.marshaller.ItemPathSerializerTemp;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

public class ItemPathSerializer extends JsonSerializer<ItemPath> {

    @Override
    public void serialize(ItemPath value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeObject(ItemPathSerializerTemp.serializeWithForcedDeclarations(value));
    }

    @Override
    public void serializeWithType(ItemPath value, JsonGenerator jgen, SerializerProvider provider, TypeSerializer typeSer)
            throws IOException {
        serialize(value, jgen, provider);
    }

}
