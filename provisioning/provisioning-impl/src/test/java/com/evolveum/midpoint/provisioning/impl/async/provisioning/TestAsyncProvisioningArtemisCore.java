/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.async.provisioning;

import org.apache.activemq.artemis.api.core.ActiveMQQueueExistsException;
import org.apache.activemq.artemis.api.core.QueueConfiguration;
import org.apache.activemq.artemis.api.core.client.*;
import org.testng.annotations.Test;

import java.io.File;

import static org.apache.activemq.artemis.api.core.client.ActiveMQClient.DEFAULT_ACK_BATCH_SIZE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 */
public class TestAsyncProvisioningArtemisCore extends TestAsyncProvisioningArtemis {

    private static final File RESOURCE_ARTEMIS_CORE_FILE = new File(TEST_DIR, "resource-async-provisioning-artemis-core.xml");

    private static final String EXTERNAL_QUEUE_CORE = "TestQueueExternalCore";
    private static final String EMBEDDED_QUEUE_CORE = "TestQueueEmbeddedCore";

    @Override
    protected File getResourceFile() {
        return RESOURCE_ARTEMIS_CORE_FILE;
    }

    @Override
    protected void testSanity() throws Exception {
        testEmbeddedBrokerSanity();
    }

    private void testEmbeddedBrokerSanity() throws Exception {
        ServerLocator serverLocator =  ActiveMQClient.createServerLocator("vm://0");
        ClientSessionFactory factory =  serverLocator.createSessionFactory();
        try (ClientSession session = factory.createSession()) {
            session.createQueue(new QueueConfiguration(EMBEDDED_QUEUE_CORE));

            ClientProducer producer = session.createProducer(EMBEDDED_QUEUE_CORE);
            ClientMessage message = session.createMessage(false);
            String bodySent = "Hello";
            message.writeBodyBufferString(bodySent);
            producer.send(message);

            session.start();
            ClientConsumer consumer = session.createConsumer(EMBEDDED_QUEUE_CORE);
            ClientMessage msgReceived = consumer.receive();
            String bodyReceived = msgReceived.getBodyBuffer().readString();
            displayValue("message received", bodyReceived);
            assertThat(bodyReceived).as("body received").isEqualTo(bodySent);
        }
    }

    /** Use only when external broker is started */
    @Test(enabled = EXTERNAL_BROKER_TESTS_ENABLED)
    public void test010ExternalBrokerSanity() throws Exception {
        ServerLocator serverLocator =  ActiveMQClient.createServerLocator(EXTERNAL_BROKER_URL);
        ClientSessionFactory factory =  serverLocator.createSessionFactory();
        try (ClientSession session = factory.createSession(EXTERNAL_BROKER_LOGIN, EXTERNAL_BROKER_PASSWORD, false,
                true, true, false, DEFAULT_ACK_BATCH_SIZE)) {

            try {
                session.createQueue(new QueueConfiguration(EXTERNAL_QUEUE_CORE));
            } catch (ActiveMQQueueExistsException e) {
                System.out.println("Queue " + EXTERNAL_QUEUE_CORE + " already exists");
            }
            ClientProducer producer = session.createProducer(EXTERNAL_QUEUE_CORE);
            ClientMessage message = session.createMessage(false);
            String bodySent = "Hello";
            message.writeBodyBufferString(bodySent);
            producer.send(message);

            session.start();
            ClientConsumer consumer = session.createConsumer(EXTERNAL_QUEUE_CORE);
            ClientMessage msgReceived = consumer.receive();
            String bodyReceived = msgReceived.getBodyBuffer().readString();
            displayValue("message received", bodyReceived);
            assertThat(bodyReceived).as("body received").isEqualTo(bodySent);
        }
    }

}
