/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.async.provisioning;

import org.testng.annotations.Test;

import javax.jms.*;
import javax.naming.InitialContext;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 */
public class TestAsyncProvisioningArtemisJms extends TestAsyncProvisioningArtemis {

    private static final File RESOURCE_ARTEMIS_JMS_FILE = new File(TEST_DIR, "resource-async-provisioning-artemis-jms.xml");

    private static final String INVM_CONNECTION_FACTORY = "invmConnectionFactory";
    private static final String TCP_CONNECTION_FACTORY = "tcpConnectionFactory";

    private static final String EXTERNAL_QUEUE_JMS = "TestQueueExternalJms";
    private static final String EMBEDDED_QUEUE_JMS = "TestQueueEmbeddedJms";

    @Override
    protected File getResourceFile() {
        return RESOURCE_ARTEMIS_JMS_FILE;
    }

    @Override
    protected void testSanity() throws Exception {
        testEmbeddedBrokerSanityJms();
    }

    private void testEmbeddedBrokerSanityJms() throws Exception {
        InitialContext ic = new InitialContext();
        ConnectionFactory cf = (ConnectionFactory) ic.lookup(INVM_CONNECTION_FACTORY);
        Queue testQueue = (Queue) ic.lookup(EMBEDDED_QUEUE_JMS);
        try (Connection connection = cf.createConnection()) {
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageProducer producer = session.createProducer(testQueue);
            MessageConsumer consumer = session.createConsumer(testQueue);

            connection.start();

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

    /** Use only when external broker is started */
    @Test(enabled = EXTERNAL_BROKER_TESTS_ENABLED)
    public void test015ExternalBrokerSanityJms() throws Exception {
        InitialContext ic = new InitialContext();
        ConnectionFactory cf = (ConnectionFactory) ic.lookup(TCP_CONNECTION_FACTORY);
        Queue testQueue = (Queue) ic.lookup(EXTERNAL_QUEUE_JMS);
        Connection connection = cf.createConnection(EXTERNAL_BROKER_LOGIN, EXTERNAL_BROKER_PASSWORD);
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        MessageProducer producer = session.createProducer(testQueue);
        MessageConsumer consumer = session.createConsumer(testQueue);

        connection.start();

        String textSent = "Hello, world.";
        TextMessage message = session.createTextMessage(textSent);
        producer.send(message);

        TextMessage receivedMessage = (TextMessage)consumer.receive();
        String textReceived = receivedMessage.getText();
        displayValue("Message received", textReceived);

        assertThat(textReceived).as("text received").isEqualTo(textSent);

        session.close();
        connection.close();
    }

}
