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

package com.evolveum.midpoint.provisioning.impl.async;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.qpid.server.SystemLauncher;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class EmbeddedBroker {

	private final SystemLauncher broker = new SystemLauncher();

	void start() throws Exception {
		System.out.println("Starting the broker");
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("type", "Memory");
		attributes.put("initialConfigurationLocation", findResourcePath("async/qpid-config.json"));
		broker.startup(attributes);
	}

	private String findResourcePath(String fileName) {
		return EmbeddedBroker.class.getClassLoader().getResource(fileName).toExternalForm();
	}

	void stop() {
		System.out.println("Stopping the broker");
		broker.shutdown();
	}

	void send(String queueName, String message) throws IOException, TimeoutException {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost("localhost");
		try (Connection connection = factory.newConnection();
				Channel channel = connection.createChannel()) {
			channel.basicPublish("", queueName, null, message.getBytes(StandardCharsets.UTF_8));
			System.out.println("Sent '" + message + "'");
		}
	}

	public void createQueue(String queueName) throws IOException, TimeoutException {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost("localhost");
		try (Connection connection = factory.newConnection();
				Channel channel = connection.createChannel()) {
			channel.queueDeclare(queueName, true, false, false, new HashMap<>());
		}
	}
}