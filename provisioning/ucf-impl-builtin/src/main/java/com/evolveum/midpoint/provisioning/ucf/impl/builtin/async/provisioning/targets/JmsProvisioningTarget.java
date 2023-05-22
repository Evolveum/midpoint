/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.impl.builtin.async.provisioning.targets;

import jakarta.jms.*;
import javax.naming.InitialContext;

import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.provisioning.ucf.api.async.AsyncProvisioningTarget;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AsyncProvisioningTargetType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.provisioning.ucf.api.async.AsyncProvisioningRequest;
import com.evolveum.midpoint.provisioning.ucf.impl.builtin.async.provisioning.AsyncProvisioningConnectorInstance;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.JmsProvisioningTargetType;

import java.util.Objects;

/**
 * Connection to async provisioning target using JMS API.
 */
public class JmsProvisioningTarget extends AbstractMessagingTarget<JmsProvisioningTargetType> {

    private static final Trace LOGGER = TraceManager.getTrace(JmsProvisioningTarget.class);

    /** Connection factory used to create connections. */
    private final ConnectionFactory connectionFactory;

    /** The queue or topic to send the messages to. */
    private final Destination destination;

    /**
     * Connection to the JMS broker. It is created when a message has to be sent or a test is issued.
     * It is closed when we are done (see below).
     * Guarded by "this". Should use {@link #getOrCreateConnection()} to obtain the connection.
     */
    private Connection connection;

    /**
     * Sessions are single-threaded objects. So we cache them per thread.
     */
    private final ThreadLocal<Session> sessionThreadLocal = new ThreadLocal<>();

    /**
     * Producers are single-threaded objects. So we cache them per thread.
     */
    private final ThreadLocal<MessageProducer> producerThreadLocal = new ThreadLocal<>();

    private JmsProvisioningTarget(@NotNull JmsProvisioningTargetType configuration, @NotNull AsyncProvisioningConnectorInstance connectorInstance) {
        super(configuration, connectorInstance);

        try {
            InitialContext ic = new InitialContext();
            this.connectionFactory = (ConnectionFactory) ic.lookup(configuration.getConnectionFactory());
            this.destination = (Destination) ic.lookup(configuration.getDestination());
        } catch (Throwable t) {
            throw new SystemException("Couldn't obtain JNDI objects for " + this + ": " + t.getMessage(), t);
        }
    }

    public static JmsProvisioningTarget create(@NotNull AsyncProvisioningTargetType configuration,
            @NotNull AsyncProvisioningConnectorInstance connectorInstance) {
        return new JmsProvisioningTarget((JmsProvisioningTargetType) configuration, connectorInstance);
    }

    @Override
    public @NotNull AsyncProvisioningTarget copy() {
        return create(configuration, connectorInstance);
    }

    @Override
    protected void validate() {
        Objects.requireNonNull(configuration.getConnectionFactory(), "configurationFactory must be specified");
        Objects.requireNonNull(configuration.getDestination(), "destination must be specified");
    }

    @Override
    protected void executeTest() throws JMSException, EncryptionException {
        closeProducer();
        closeSession();
        getOrCreateProducer();
    }

    @Override
    protected String executeSend(AsyncProvisioningRequest request) throws JMSException, EncryptionException {
        Session session = getOrCreateSession();
        TextMessage message = session.createTextMessage(request.asString());
        MessageProducer producer = getOrCreateProducer();
        producer.send(message);
        return message.getJMSMessageID();
    }

    /**
     * Gets current producer (thread-local) or creates a new one if needed.
     */
    private MessageProducer getOrCreateProducer() throws JMSException, EncryptionException {
        MessageProducer producer = producerThreadLocal.get();
        if (producer != null) {
            return producer;
        }

        Session session = getOrCreateSession();
        MessageProducer newProducer = session.createProducer(destination);
        producerThreadLocal.set(newProducer);
        return newProducer;
    }

    /**
     * Gets current session (thread-local) or creates a new one if needed.
     */
    private Session getOrCreateSession() throws JMSException, EncryptionException {
        Session existingSession = sessionThreadLocal.get();
        if (existingSession != null) {
            return existingSession;
        }

        Connection connection = getOrCreateConnection();
        Session newSession = connection.createSession();
        sessionThreadLocal.set(newSession);
        return newSession;
    }

    /**
     * Gets current connection, or creates a new one if needed.
     *
     * Must be synchronized to avoid creation of more connections.
     * Perhaps not the best solution (using Future might be better) but good enough for now.
     */
    private synchronized Connection getOrCreateConnection() throws JMSException, EncryptionException {
        if (connection == null) {
            connection = connectionFactory.createConnection(configuration.getUsername(), decrypt(configuration.getPassword()));
        }
        return connection;
    }

    /**
     * Closes existing producer and removes it from the thread.
     */
    private void closeProducer() {
        MessageProducer existingProducer = producerThreadLocal.get();
        if (existingProducer != null) {
            try {
                existingProducer.close();
            } catch (Throwable t) {
                LoggingUtils.logException(LOGGER, "Couldn't close existing producer - ignoring this exception", t);
            } finally {
                producerThreadLocal.remove();
            }
        }
    }

    /**
     * Closes existing session and removes it from the thread.
     */
    private void closeSession() {
        Session existingSession = sessionThreadLocal.get();
        if (existingSession != null) {
            try {
                existingSession.close();
            } catch (Throwable t) {
                LoggingUtils.logException(LOGGER, "Couldn't close existing session - ignoring this exception", t);
            } finally {
                sessionThreadLocal.remove();
            }
        }
    }

    @Override
    protected void closeBrokerConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (JMSException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't close JMS connection", e);
            } finally {
                connection = null;
            }
        }
    }

    @Override
    public String toString() {
        return "JmsProvisioningTarget{" +
                "cf='" + configuration.getConnectionFactory() +
                "',dest='" + configuration.getDestination() +
                "'}";
    }
}
