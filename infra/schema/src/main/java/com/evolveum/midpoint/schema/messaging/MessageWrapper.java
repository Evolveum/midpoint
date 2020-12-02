/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.messaging;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.Amqp091MessageAttributesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.Amqp091MessageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AsyncUpdateMessageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.JmsTextMessageType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Provides basic message-manipulation methods to be used in scripts (typically, async update transformation scripts).
 */
@Experimental
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

    public @NotNull AsyncUpdateMessageType getMessage() {
        return message;
    }

    /**
     * Returns message body as text. Assumes UTF-8 encoding for binary messages.
     */
    public String getBodyAsText() {
        if (message instanceof Amqp091MessageType) {
            return new String(((Amqp091MessageType) message).getBody(), StandardCharsets.UTF_8);
        } else if (message instanceof JmsTextMessageType) {
            return ((JmsTextMessageType) message).getText();
        } else {
            throw new UnsupportedOperationException("Unsupported message: " + MiscUtil.getClass(message));
        }
    }

    /**
     * Returns message body as byte array. Assumes UTF-8 encoding for text messages.
     */
    public byte[] getBodyAsBytes() {
        if (message instanceof Amqp091MessageType) {
            return ((Amqp091MessageType) message).getBody();
        } else if (message instanceof JmsTextMessageType) {
            return ((JmsTextMessageType) message).getText().getBytes(StandardCharsets.UTF_8);
        } else {
            throw new UnsupportedOperationException("Unsupported message: " + MiscUtil.getClass(message));
        }
    }

    /**
     * Returns message body as map. Assumes plain JSON representation.
     */
    public Map<String, Object> getBodyAsMap() throws IOException {
        String json = getBodyAsText();
        return MAPPER.readValue(json, MAP_TYPE);
    }

    /**
     * Returns message body as {@link JsonAsyncProvisioningRequest}. Assumes plain JSON representation.
     */
    public JsonAsyncProvisioningRequest getBodyAsAsyncProvisioningRequest() throws JsonProcessingException {
        return JsonAsyncProvisioningRequest.from(getBodyAsText());
    }

    /**
     * Returns message body as prism item. Assumes prism representation.
     */
    public Item<?,?> getBodyAsPrismItem(String language) throws SchemaException {
        String text = getBodyAsText();
        return prismContext.parserFor(text).language(language).parseItem();
    }

    /**
     * Returns AMQP 0.9.1 attributes of the message.
     * TODO decide what to do with this method
     */
    public Amqp091MessageAttributesType getAmqp091Attributes() {
        return ((Amqp091MessageType) message).getAttributes();
    }
}
