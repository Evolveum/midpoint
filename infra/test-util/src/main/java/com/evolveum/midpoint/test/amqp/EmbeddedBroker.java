/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.test.amqp;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.github.fridujo.rabbitmq.mock.MockConnectionFactory;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class EmbeddedBroker {

    protected static final Trace LOGGER = TraceManager.getTrace(EmbeddedBroker.class);

    private final ConnectionFactory connectionFactory = new MockConnectionFactory();

    public void start() throws Exception {
        System.out.println("Starting the mock broker");
    }

    public void stop() {
        System.out.println("Stopping the mock broker");
    }

    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    public void send(String queueName, String message, Map<String, Object> headers) throws IOException, TimeoutException {
        try (Connection connection = connectionFactory.newConnection();
                Channel channel = connection.createChannel()) {
            channel.basicPublish("", queueName, createProperties(headers), message.getBytes(StandardCharsets.UTF_8));
            LOGGER.trace("Sent '{}'", message);
        }
    }

    public long getMessagesCount(String queueName) throws IOException, TimeoutException {
        try (Connection connection = connectionFactory.newConnection();
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
        try (Connection connection = connectionFactory.newConnection();
                Channel channel = connection.createChannel()) {
            channel.queueDeclare(queueName, true, false, false, Map.of());
        }
    }
}
