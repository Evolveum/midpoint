/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.impl.lex.json;

import java.io.IOException;

import org.w3c.dom.Node;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ext.DOMSerializer;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

public class DomElementSerializer extends DOMSerializer {

    @Override
    public void serializeWithType(Node value, JsonGenerator jgen, SerializerProvider provider,
            TypeSerializer typeSer) throws IOException, JsonProcessingException {
        // TODO Auto-generated method stub
        serialize(value, jgen, provider);
    }

}
