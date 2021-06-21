/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test.amqp;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.qpid.server.SystemLauncher;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class EmbeddedBroker {

    protected static final Trace LOGGER = TraceManager.getTrace(EmbeddedBroker.class);

    private static final String DEFAULT_CONFIG_RESOURCE_PATH = "amqp/default-qpid-config.json";

    private final SystemLauncher broker = new SystemLauncher();

    public void start() throws Exception {
        start(DEFAULT_CONFIG_RESOURCE_PATH);
    }

    public void start(String configResourcePath) throws Exception {
        System.out.println("Starting the broker");
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("type", "Memory");
        attributes.put("initialConfigurationLocation", findResourcePath(configResourcePath));
        broker.startup(attributes);
    }

    private String findResourcePath(String fileName) {
        return EmbeddedBroker.class.getClassLoader().getResource(fileName).toExternalForm();
    }

    public void stop() {
        System.out.println("Stopping the broker");
        broker.shutdown();
    }

    public void send(String queueName, String message, Map<String, Object> headers) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        try (Connection connection = factory.newConnection();
                Channel channel = connection.createChannel()) {
            channel.basicPublish("", queueName, createProperties(headers), message.getBytes(StandardCharsets.UTF_8));
            LOGGER.trace("Sent '{}'", message);
        }
    }

    public long getMessagesCount(String queueName) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        try (Connection connection = factory.newConnection();
                Channel channel = connection.createChannel()) {
            return channel.messageCount(queueName);
        }
    }

    @NotNull
    private AMQP.BasicProperties createProperties(Map<String, Object> headers) {
        return new AMQP.BasicProperties()
                .builder()
                .headers(headers)
                .build();
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
