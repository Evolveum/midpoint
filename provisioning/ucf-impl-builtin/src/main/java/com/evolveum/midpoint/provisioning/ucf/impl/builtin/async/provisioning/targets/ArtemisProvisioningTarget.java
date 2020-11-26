/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.impl.builtin.async.provisioning.targets;

import static org.apache.activemq.artemis.api.core.client.ActiveMQClient.DEFAULT_ACK_BATCH_SIZE;

import java.util.Objects;

import com.evolveum.midpoint.provisioning.ucf.api.async.AsyncProvisioningTarget;

import org.apache.activemq.artemis.api.core.client.*;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.provisioning.ucf.api.async.AsyncProvisioningRequest;
import com.evolveum.midpoint.provisioning.ucf.impl.builtin.async.provisioning.AsyncProvisioningConnectorInstance;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArtemisProvisioningTargetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AsyncProvisioningTargetType;

/**
 * Connection to Artemis async provisioning target using Artemis Core API.
 *
 * EXPERIMENTAL. It is better to use JMS target connection instead.
 */
@Experimental
public class ArtemisProvisioningTarget extends AbstractMessagingTarget<ArtemisProvisioningTargetType> {

    private static final Trace LOGGER = TraceManager.getTrace(JmsProvisioningTarget.class);

    private ClientSessionFactory clientSessionFactory;

    private final ThreadLocal<ClientSession> clientSessionThreadLocal = new ThreadLocal<>();
    private final ThreadLocal<ClientProducer> clientProducerThreadLocal = new ThreadLocal<>();

    private ArtemisProvisioningTarget(@NotNull ArtemisProvisioningTargetType configuration, @NotNull AsyncProvisioningConnectorInstance connectorInstance) {
        super(configuration, connectorInstance);
    }

    public static ArtemisProvisioningTarget create(@NotNull AsyncProvisioningTargetType configuration, @NotNull AsyncProvisioningConnectorInstance connectorInstance) {
        return new ArtemisProvisioningTarget((ArtemisProvisioningTargetType) configuration, connectorInstance);
    }

    @Override
    public @NotNull AsyncProvisioningTarget copy() {
        return create(configuration, connectorInstance);
    }

    @Override
    protected void validate() {
        Objects.requireNonNull(configuration.getUrl(), "URI must be specified");
        Objects.requireNonNull(configuration.getAddress(), "address must be specified");
    }

    @Override
    protected void executeTest() throws Exception {
        closeClientProducer();
        closeClientSession();
        getOrCreateClientProducer();
    }

    @Override
    protected String executeSend(AsyncProvisioningRequest request) throws Exception {
        ClientSession session = getOrCreateClientSession();
        ClientProducer producer = getOrCreateClientProducer();

        ClientMessage message = session.createMessage(false);
        message.getBodyBuffer().writeUTF(request.asString());
        producer.send(message);
        return String.valueOf(message.getMessageID());
    }

    @Override
    protected void closeBrokerConnection() {
        clientSessionFactory.close();
    }

    /**
     * Gets current client producer (thread-local) or creates a new one if needed.
     */
    private ClientProducer getOrCreateClientProducer() throws Exception {
        ClientProducer existingProducer = clientProducerThreadLocal.get();
        if (existingProducer != null) {
            return existingProducer;
        }

        ClientSession clientSession = getOrCreateClientSession();
        ClientProducer newClientProducer = clientSession.createProducer(configuration.getAddress());
        clientProducerThreadLocal.set(newClientProducer);
        return newClientProducer;
    }

    /**
     * Gets current client session (thread-local) or creates a new one if needed.
     */
    private ClientSession getOrCreateClientSession() throws Exception {
        ClientSession existingSession = clientSessionThreadLocal.get();
        if (existingSession != null) {
            return existingSession;
        }

        ClientSessionFactory clientSessionFactory = getOrCreateClientSessionFactory();
        ClientSession newSession = clientSessionFactory.createSession(configuration.getUsername(), decrypt(configuration.getPassword()),
                false, true, true, false, DEFAULT_ACK_BATCH_SIZE);
        clientSessionThreadLocal.set(newSession);
        return newSession;
    }

    /**
     * Gets current client session factory, or creates a new one if needed.
     *
     * Must be synchronized to avoid creation of more connections.
     * Perhaps not the best solution (using Future might be better) but good enough for now.
     */
    private synchronized ClientSessionFactory getOrCreateClientSessionFactory() throws Exception {
        if (clientSessionFactory == null) {
            ServerLocator serverLocator = ActiveMQClient.createServerLocator(configuration.getUrl());
            clientSessionFactory = serverLocator.createSessionFactory();
        }
        return clientSessionFactory;
    }

    /**
     * Closes existing client producer and removes it from the thread.
     */
    private void closeClientProducer() {
        ClientProducer existingProducer = clientProducerThreadLocal.get();
        if (existingProducer != null) {
            try {
                existingProducer.close();
            } catch (Throwable t) {
                LoggingUtils.logException(LOGGER, "Couldn't close existing client producer - ignoring this exception", t);
            } finally {
                clientProducerThreadLocal.remove();
            }
        }
    }

    /**
     * Closes existing session and removes it from the thread.
     */
    private void closeClientSession() {
        ClientSession existingSession = clientSessionThreadLocal.get();
        if (existingSession != null) {
            try {
                existingSession.close();
            } catch (Throwable t) {
                LoggingUtils.logException(LOGGER, "Couldn't close existing client session - ignoring this exception", t);
            } finally {
                clientSessionThreadLocal.remove();
            }
        }
    }

    @Override
    public String toString() {
        return "ArtemisProvisioningTarget{" +
                "url='" + configuration.getUrl() +
                "',address='" + configuration.getAddress() +
                "'}";
    }
}
