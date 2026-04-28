/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.test.amqp;

import jakarta.jms.BytesMessage;
import jakarta.jms.Connection;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageListener;
import jakarta.jms.Queue;
import jakarta.jms.Session;

import javax.naming.NamingException;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SystemException;

public final class ArtemisTestSupport {

    private ArtemisTestSupport() {
    }

    public static void testQueueAccessible(
            String operationName, String sourceName, String queueName, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(operationName);
        result.addParam("sourceName", sourceName);
        try (Connection connection = EmbeddedBroker.createConnection()) {
            Queue queue = EmbeddedBroker.lookupQueue(queueName);
            if (queue == null) {
                throw new SystemException("Queue lookup returned null for " + queueName);
            }
            result.recordSuccess();
        } catch (JMSException | NamingException e) {
            result.recordFatalError("Couldn't connect to embedded Artemis queue: " + e.getMessage(), e);
            throw new SystemException("Couldn't connect to embedded Artemis queue: " + e.getMessage(), e);
        }
    }

    public static ListenerRuntime startListener(String queueName, JmsMessageHandler handler) {
        return new ListenerRuntime(queueName, handler);
    }

    public static byte[] readBody(Message message) throws JMSException {
        if (!(message instanceof BytesMessage)) {
            throw new SystemException("Expected BytesMessage but got " + message.getClass().getName());
        }
        BytesMessage bytesMessage = (BytesMessage) message;
        int length = Math.toIntExact(bytesMessage.getBodyLength());
        byte[] body = new byte[length];
        bytesMessage.readBytes(body);
        return body;
    }

    public static void acknowledge(Message message, Session session, boolean processed) {
        try {
            if (processed) {
                message.acknowledge();
            } else {
                session.recover();
            }
        } catch (JMSException e) {
            throw new SystemException("Couldn't acknowledge embedded Artemis message", e);
        }
    }

    @FunctionalInterface
    public interface JmsMessageHandler {
        void onMessage(Message message, Session session, boolean last) throws JMSException;
    }

    public static final class ListenerRuntime implements MessageListener {

        private final JmsMessageHandler handler;

        private volatile boolean alive = true;

        private Connection connection;
        private Session session;
        private MessageConsumer consumer;

        private ListenerRuntime(String queueName, JmsMessageHandler handler) {
            this.handler = handler;
            try {
                connection = EmbeddedBroker.createConnection();
                session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
                Queue queue = EmbeddedBroker.lookupQueue(queueName);
                consumer = session.createConsumer(queue);
                consumer.setMessageListener(this);
                connection.start();
            } catch (JMSException | NamingException e) {
                closeQuietly();
                throw new SystemException("Couldn't start embedded Artemis listener: " + e.getMessage(), e);
            }
        }

        @Override
        public void onMessage(Message message) {
            if (!alive) {
                return;
            }
            try {
                boolean last = message.propertyExists(EmbeddedBroker.LAST_MESSAGE_PROPERTY)
                        && Boolean.TRUE.equals(message.getObjectProperty(EmbeddedBroker.LAST_MESSAGE_PROPERTY));
                handler.onMessage(message, session, last);
                if (last) {
                    stop();
                }
            } catch (JMSException | RuntimeException e) {
                stop();
                throw new SystemException("Couldn't process embedded Artemis message: " + e.getMessage(), e);
            }
        }

        public void stop() {
            alive = false;
            closeQuietly();
        }

        public boolean isAlive() {
            return alive;
        }

        private void closeQuietly() {
            try {
                if (consumer != null) {
                    consumer.close();
                }
            } catch (JMSException ignored) {
            }
            try {
                if (session != null) {
                    session.close();
                }
            } catch (JMSException ignored) {
            }
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (JMSException ignored) {
            }
            consumer = null;
            session = null;
            connection = null;
        }

        public void acknowledge(Message message, Session session, boolean processed) {
            ArtemisTestSupport.acknowledge(message, session, processed);
            if (!processed) {
                throw new SystemException("Message processing failed");
            }
        }
    }
}
