/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.async;

import com.evolveum.midpoint.provisioning.ucf.impl.builtin.async.sources.Amqp091AsyncUpdateSource;
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
            channel.basicPublish("", queueName, createProperties(), message.getBytes(StandardCharsets.UTF_8));
            System.out.println("Sent '" + message + "'");
        }
    }

    @NotNull
    private AMQP.BasicProperties createProperties() {
        Map<String, Object> headers = new HashMap<>();
        headers.put(Amqp091AsyncUpdateSource.HEADER_LAST_MESSAGE, true);
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
