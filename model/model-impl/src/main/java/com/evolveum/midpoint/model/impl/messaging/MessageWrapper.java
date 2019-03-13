/*
 * Copyright (c) 2010-2019 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 *
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
