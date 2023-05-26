/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.async.provisioning;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.Enumeration;
import jakarta.jms.*;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.messaging.JsonAsyncProvisioningRequest;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.jms.client.ActiveMQMessage;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

/**
 * Tests with real Artemis broker.
 *
 * We use embedded one, but we can (temporarily) switch to real external broker, if needed.
 *
 * Using JMS to access the broker from the tests.
 * On the sending (midPoint) side, either JMS or Core API is used.
 */
public abstract class TestAsyncProvisioningArtemis extends TestAsyncProvisioning {

    protected static class BrokerAccess {
        private final String connectionFactory;
        private final String username;
        private final String password;

        private BrokerAccess(String connectionFactory, String username, String password) {
            this.connectionFactory = connectionFactory;
            this.username = username;
            this.password = password;
        }
    }

    private static final BrokerAccess EMBEDDED = new BrokerAccess("invmConnectionFactory", null, null);
    @SuppressWarnings("unused")
    private static final BrokerAccess EXTERNAL = new BrokerAccess("tcpConnectionFactory", "admin", "secret");

    private static final BrokerAccess BROKER = EMBEDDED;

    private static final String SANITY_TEST_QUEUE = "SanityTestQueue";
    private static final String PROVISIONING_QUEUE = "ProvisioningQueue";

    protected EmbeddedActiveMQ embeddedBroker;

    private Connection connection;
    private MessageConsumer consumer;
    private QueueBrowser browser;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        startEmbeddedBroker();
        connectToTestBroker();
    }

    private void connectToTestBroker() throws NamingException, JMSException {
        InitialContext ic = new InitialContext();
        ConnectionFactory cf = (ConnectionFactory) ic.lookup(BROKER.connectionFactory);
        Queue provisioningQueue = (Queue) ic.lookup(PROVISIONING_QUEUE);
        connection = cf.createConnection(BROKER.username, BROKER.password);
        connection.start();

        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        consumer = session.createConsumer(provisioningQueue);
        browser = session.createBrowser(provisioningQueue);
    }

    @AfterClass
    public void shutdown() throws Exception {
        if (connection != null) {
            connection.close();
        }
        stopEmbeddedBroker();
    }

    /**
     * We restart a broker. The targets should reconnect seamlessly.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void test600Reconnect() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        stopEmbeddedBroker();

        startEmbeddedBroker();
        connectToTestBroker();

        PrismObject<ShadowType> jim = createShadow(resource, "jim");

        when();
        provisioningService.addObject(jim, null, null, task, result);

        then();
        assertSuccessOrInProgress(result);

        dumpRequests();
        String req = getRequest();
        JsonAsyncProvisioningRequest jsonRequest = JsonAsyncProvisioningRequest.from(req);
        assertThat(jsonRequest.getOperation()).isEqualTo("add");
        assertThat(jsonRequest.getObjectClass()).isEqualTo(getAccountObjectClassName());
        assertThat(jsonRequest.getAttributes()).containsOnlyKeys(icfsUid(), icfsName());
        assertThat((Collection<Object>) jsonRequest.getAttributes().get(icfsUid())).containsExactly("jim");
        assertThat((Collection<Object>) jsonRequest.getAttributes().get(icfsName())).containsExactly("jim");
    }

    private void startEmbeddedBroker() throws Exception {
        embeddedBroker = new EmbeddedActiveMQ();
        embeddedBroker.start();
    }

    private void stopEmbeddedBroker() throws Exception {
        embeddedBroker.stop();
    }

    @Override
    protected String getRequest() throws JMSException {
        Message msg = consumer.receiveNoWait();
        displayValue("Message received", msg);
        if (msg instanceof TextMessage) {
            String text = ((TextMessage) msg).getText();
            displayValue("Message " + msg.getJMSMessageID(), text);
            return text;
        } else if (msg instanceof ActiveMQMessage) {
            ClientMessage coreMessage = ((ActiveMQMessage) msg).getCoreMessage();
            String text = coreMessage.getBodyBuffer().readUTF();
            displayValue("Message " + coreMessage.getMessageID(), text);
            return text;
        } else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void dumpRequests() throws JMSException {
        // Looks like the browser does not see messages "seen" by the consumer (although not received yet)
        Enumeration<TextMessage> enumeration = browser.getEnumeration();
        StringBuilder sb = new StringBuilder();
        while (enumeration.hasMoreElements()) {
            TextMessage message = enumeration.nextElement();
            sb.append(message.getJMSMessageID()).append(" -> ").append(message.getText()).append("\n");
        }
        displayValue("Messages", sb.toString());
    }

    @Override
    protected void clearRequests() throws JMSException {
        while (consumer.receiveNoWait() != null) {
            // No processing needed
        }
    }

    @Override
    protected void testSanityExtra() throws Exception {
        testBrokerSanity();
        dumpRequests();
    }

    private void testBrokerSanity() throws Exception {
        InitialContext ic = new InitialContext();
        ConnectionFactory cf = (ConnectionFactory) ic.lookup(BROKER.connectionFactory);
        Queue queue = (Queue) ic.lookup(SANITY_TEST_QUEUE);
        try (Connection connection = cf.createConnection(BROKER.username, BROKER.password)) {
            connection.start();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageProducer producer = session.createProducer(queue);
            MessageConsumer consumer = session.createConsumer(queue);

            String textSent = "Hello, world.";
            TextMessage message = session.createTextMessage(textSent);
            producer.send(message);

            TextMessage receivedMessage = (TextMessage) consumer.receive();
            String textReceived = receivedMessage.getText();
            displayValue("Message received", textReceived);

            assertThat(textReceived).as("text received").isEqualTo(textSent);
            session.close();
        }
    }
}
