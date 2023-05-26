/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.impl.builtin.async.update.sources;

import static jakarta.jms.Session.CLIENT_ACKNOWLEDGE;

import java.util.Enumeration;
import jakarta.jms.*;
import javax.naming.InitialContext;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.provisioning.ucf.api.ListeningActivity;
import com.evolveum.midpoint.provisioning.ucf.api.async.ActiveAsyncUpdateSource;
import com.evolveum.midpoint.provisioning.ucf.api.async.AsyncUpdateMessageListener;
import com.evolveum.midpoint.provisioning.ucf.impl.builtin.async.update.AsyncUpdateConnectorInstance;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AsyncUpdateSourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.JmsMessageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.JmsSourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.JmsTextMessageType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * Async Update source for JMS API.
 *
 * An experimental implementation. Very primitive; suitable basically for testing and demonstration purposes.
 * In particular, it does NOT work with multiple worker threads.
 */
@Experimental
public class JmsAsyncUpdateSource implements ActiveAsyncUpdateSource {

    public static final String HEADER_LAST_MESSAGE = "X-LastMessage";

    private static final Trace LOGGER = TraceManager.getTrace(JmsAsyncUpdateSource.class);

    @NotNull private final JmsSourceType configuration;
    @NotNull private final AsyncUpdateConnectorInstance connectorInstance;
    @NotNull private final ConnectionFactory connectionFactory;
    @NotNull private final Destination destination;

    private JmsAsyncUpdateSource(@NotNull JmsSourceType configuration, @NotNull AsyncUpdateConnectorInstance connectorInstance) {
        this.configuration = configuration;
        this.connectorInstance = connectorInstance;

        try {
            InitialContext ic = new InitialContext();
            this.connectionFactory = (ConnectionFactory) ic.lookup(configuration.getConnectionFactory());
            this.destination = (Destination) ic.lookup(configuration.getDestination());
        } catch (Throwable t) {
            throw new SystemException("Couldn't obtain JNDI objects for " + this + ": " + t.getMessage(), t);
        }
    }

    private class ListeningActivityImpl implements ListeningActivity {

        // the following items are initialized only once; in the constructor
        private Connection connection;

        private volatile boolean closed;

        @Override
        public boolean isAlive() {
            return !closed;
        }

        private ListeningActivityImpl(AsyncUpdateMessageListener listener) {
            try {
                connection = connectionFactory.createConnection(configuration.getUsername(), decrypt(configuration.getPassword()));
                Session session = connection.createSession(false, CLIENT_ACKNOWLEDGE);
                MessageConsumer consumer = session.createConsumer(destination);
                consumer.setMessageListener(message -> {
                    try {
                        listener.onMessage(createAsyncUpdateMessage(message), (release, result) -> {
                            // TODO This has to be done in the correct thread!!!
                            if (release) {
                                try {
                                    message.acknowledge();
                                } catch (JMSException e) {
                                    LoggingUtils.logUnexpectedException(LOGGER, "Message could not be acknowledged", e);
                                }
                            } else {
                                LOGGER.debug("Message processing was not successful. Message will not be acknowledged.");
                            }
                        });
                    } catch (JMSException e) {
                        throw new SystemException("Couldn't process JMS message: " + e.getMessage(), e);
                    }
                });
                connection.setExceptionListener(exception -> {
                    LOGGER.warn("JMS exception detected: {}", exception.getMessage(), exception);
                    stop();
                });
                connection.start();
                LOGGER.info("Opened consumer {}", consumer);
            } catch (Throwable t) {
                silentlyCloseActiveConnection();
                throw new SystemException("Couldn't start listening on " + listener + ": " + t.getMessage(), t);
            }
        }

        @Override
        public void stop() {
            silentlyCloseActiveConnection();
        }

        @Override
        public String toString() {
            return "JMS-ListeningActivityImpl{" +
                    "connection=" + connection +
                    ", destination='" + destination + '\'' +
                    '}';
        }

        private void silentlyCloseActiveConnection() {
            try {
                if (connection != null) {
                    LOGGER.info("Closing {}", connection); // todo debug
                    connection.close();
                    LOGGER.info("Closed {}", connection);
                }
            } catch (Throwable t) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't close active connection {}", t, connection);
            }
            connection = null;
            closed = true;
        }
    }

    private JmsMessageType createAsyncUpdateMessage(Message message) throws JMSException {
        if (message instanceof TextMessage) {
            // todo headers and properties
            return new JmsTextMessageType()
                    .sourceName(configuration.getName())
                    .text(((TextMessage) message).getText());
        } else {
            // todo
            throw new UnsupportedOperationException("Unsupported JMS message type: " + message.getClass().getName());
        }
    }

    public static JmsAsyncUpdateSource create(AsyncUpdateSourceType configuration, AsyncUpdateConnectorInstance connectorInstance) {
        if (!(configuration instanceof JmsSourceType)) {
            throw new IllegalArgumentException("JMS source requires " + JmsSourceType.class.getName() + " but got " +
                    configuration.getClass().getName());
        }
        return new JmsAsyncUpdateSource((JmsSourceType) configuration, connectorInstance);
    }

    @Override
    public ListeningActivity startListening(AsyncUpdateMessageListener listener) {
        return new ListeningActivityImpl(listener);
    }

    @Override
    public void test(OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(getClass().getName() + ".test");
        result.addParam("sourceName", configuration.getName());

        try (Connection connection = connectionFactory.createConnection(configuration.getUsername(), decrypt(configuration.getPassword()))) {
            Session session = connection.createSession(false, CLIENT_ACKNOWLEDGE);
            if (destination instanceof Queue) {
                QueueBrowser browser = session.createBrowser((Queue) destination);
                Enumeration<?> enumeration = browser.getEnumeration();
                int count = 0;
                while (enumeration.hasMoreElements() && count < 10) {
                    enumeration.nextElement();
                    count++;
                }
                LOGGER.info("# of messages in {}: {}{}", ((Queue) destination).getQueueName(), count,
                        (enumeration.hasMoreElements() ? "+":""));
            }
        } catch (Throwable t) {
            result.recordFatalError(t);
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public void close() {
        // activities were already stopped, there is nothing really to do here
    }

    private String decrypt(ProtectedStringType protectedString) throws EncryptionException {
        if (protectedString != null) {
            Protector protector = connectorInstance.getPrismContext().getDefaultProtector();
            return protector.decryptString(protectedString);
        } else {
            return null;
        }
    }
}
