/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.messaging;

import com.evolveum.midpoint.xml.ns._public.common.common_3.Amqp091MessageAttributesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.Amqp091MessageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AsyncUpdateMessageType;
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

	private static final TypeReference<?> MAP_TYPE = new MapTypeReference();
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static class MapTypeReference extends TypeReference<Map<String, Object>> {
	}

	public MessageWrapper(@NotNull AsyncUpdateMessageType message) {
		this.message = message;
	}

	public Amqp091MessageAttributesType getAttributes() {
		return ((Amqp091MessageType) message).getAttributes();
	}

	public byte[] getBody() {
		return ((Amqp091MessageType) message).getBody();
	}

	public String getText() {
		return new String(((Amqp091MessageType) message).getBody(), StandardCharsets.UTF_8);
	}

	public AsyncUpdateMessageType getOriginalMessage() {
		return message;
	}

	public Map<String, Object> getBodyAsMap() throws IOException {
		String json = getText();
		return MAPPER.readValue(json, MAP_TYPE);
	}
}
