/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.impl.lex.json;

import java.io.IOException;

import com.evolveum.midpoint.prism.impl.marshaller.ItemPathParserTemp;
import org.apache.commons.lang.StringUtils;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

public class ItemPathTypeDeserializer extends JsonDeserializer<ItemPathType>{

    @Override
    public ItemPathType deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        if (jp.getCurrentToken() != JsonToken.VALUE_STRING){
            throw new JsonParseException("Cannot parse path value. Expected that the value will be string but it is: " + jp.getCurrentTokenId(), jp.getCurrentLocation());
        }
        String path = jp.getText();
        if (StringUtils.isBlank(path)){
            throw new IllegalStateException("Error while deserializing path. No path specified.");
        }
        return new ItemPathType(ItemPathParserTemp.parseFromString(path));
    }

}
