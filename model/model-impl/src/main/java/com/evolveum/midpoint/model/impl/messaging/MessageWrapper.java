/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.messaging;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.Amqp091MessageAttributesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.Amqp091MessageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AsyncUpdateMessageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.JmsTextMessageType;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 *  TODO move somewhere else or rewrite
 */
public class MessageWrapper {

    @NotNull private final AsyncUpdateMessageType message;
    @NotNull private final PrismContext prismContext;

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new MapTypeReference();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static class MapTypeReference extends TypeReference<Map<String, Object>> {
    }

    public MessageWrapper(@NotNull AsyncUpdateMessageType message, @NotNull PrismContext prismContext) {
        this.message = message;
        this.prismContext = prismContext;
    }

    public Amqp091MessageAttributesType getAttributes() {
        return ((Amqp091MessageType) message).getAttributes();
    }

    public byte[] getBody() {
        return ((Amqp091MessageType) message).getBody();
    }

    public String getText() {
        if (message instanceof Amqp091MessageType) {
            return new String(((Amqp091MessageType) message).getBody(), StandardCharsets.UTF_8);
        } else if (message instanceof JmsTextMessageType) {
            return ((JmsTextMessageType) message).getText();
        } else {
            throw new UnsupportedOperationException("Unsupported message: " + MiscUtil.getClass(message));
        }
    }

    public AsyncUpdateMessageType getOriginalMessage() {
        return message;
    }

    public Map<String, Object> getBodyAsMap() throws IOException {
        String json = getText();
        return MAPPER.readValue(json, MAP_TYPE);
    }

    public Item<?,?> getBodyAsPrismItem(String language) throws SchemaException {
        String text = getText();
        return prismContext.parserFor(text).language(language).parseItem();
    }
}
