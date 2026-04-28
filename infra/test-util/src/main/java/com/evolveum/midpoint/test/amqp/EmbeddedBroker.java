/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.test.amqp;

import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;

import jakarta.jms.BytesMessage;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageProducer;
import jakarta.jms.Queue;
import jakarta.jms.QueueBrowser;
import jakarta.jms.Session;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.activemq.artemis.api.core.QueueConfiguration;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;

/**
 * This broker is used only by async-update tests.
 * Artemis is an actively maintained embeddable broker, but its AMQP support is AMQP 1.0.
 * The production Amqp091AsyncUpdateSource remains based on RabbitMQ's com.rabbitmq.client
 * AMQP 0-9-1 API. These tests exercise the async-update processing flow without requiring
 * Docker or an external RabbitMQ broker; they are not intended to verify RabbitMQ protocol
 * compatibility.
 */

public class EmbeddedBroker {

    private static final String LAST_MESSAGE_HEADER = "X-LastMessage";
    private static final String CONNECTION_FACTORY_NAME = "invmConnectionFactory";
    private static final String CONNECTION_FACTORY_URL = "vm://0";
    public static final String LAST_MESSAGE_PROPERTY = "XLastMessage";

    private EmbeddedActiveMQ embeddedBroker;

    public void start() throws Exception {
        embeddedBroker = new EmbeddedActiveMQ();
        embeddedBroker.setConfiguration(new ConfigurationImpl()
                .setPersistenceEnabled(false)
                .setSecurityEnabled(false)
                .addAcceptorConfiguration("in-vm", CONNECTION_FACTORY_URL));
        embeddedBroker.start();
    }

    public void stop() throws Exception {
        if (embeddedBroker != null) {
            embeddedBroker.stop();
            embeddedBroker = null;
        }
    }

    public void createQueue(String queueName) throws Exception {
        embeddedBroker.getActiveMQServer().createQueue(
                QueueConfiguration.of(queueName)
                        .setRoutingType(RoutingType.ANYCAST)
                        .setDurable(true));
    }

    public void send(String queueName, String message, Map<String, Object> headers) throws JMSException, NamingException {
        try (Connection connection = createConnection()) {
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            try {
                Queue queue = lookupQueue(queueName);
                MessageProducer producer = session.createProducer(queue);
                BytesMessage jmsMessage = session.createBytesMessage();
                jmsMessage.writeBytes(message.getBytes(StandardCharsets.UTF_8));
                for (Map.Entry<String, Object> header : headers.entrySet()) {
                    jmsMessage.setObjectProperty(toJmsPropertyName(header.getKey()), header.getValue());
                }
                producer.send(jmsMessage);
            } finally {
                session.close();
            }
        }
    }

    public long getMessagesCount(String queueName) throws JMSException, NamingException {
        try (Connection connection = createConnection()) {
            connection.start();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            try {
                QueueBrowser browser = session.createBrowser(lookupQueue(queueName));
                try {
                    long count = 0;
                    @SuppressWarnings("unchecked")
                    Enumeration<Message> messages = browser.getEnumeration();
                    while (messages.hasMoreElements()) {
                        messages.nextElement();
                        count++;
                    }
                    return count;
                } finally {
                    browser.close();
                }
            } finally {
                session.close();
            }
        }
    }

    public static Connection createConnection() throws NamingException, JMSException {
        return lookupConnectionFactory().createConnection();
    }

    public static Queue lookupQueue(String queueName) throws NamingException {
        return (Queue) createContext(queueName).lookup(queueName);
    }

    private static ConnectionFactory lookupConnectionFactory() throws NamingException {
        return (ConnectionFactory) createContext(null).lookup(CONNECTION_FACTORY_NAME);
    }

    private static InitialContext createContext(String queueName) throws NamingException {
        Hashtable<String, String> environment = new Hashtable<>();
        environment.put(Context.INITIAL_CONTEXT_FACTORY, "org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory");
        environment.put("connectionFactory." + CONNECTION_FACTORY_NAME, CONNECTION_FACTORY_URL);
        if (queueName != null) {
            environment.put("queue." + queueName, queueName);
        }
        return new InitialContext(environment);
    }

    private static String toJmsPropertyName(String propertyName) {
        if (LAST_MESSAGE_HEADER.equals(propertyName)) {
            return LAST_MESSAGE_PROPERTY;
        }
        return propertyName;
    }
}
