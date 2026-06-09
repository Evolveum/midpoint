/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.impl.async.update;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.amqp.ArtemisTestSupport;
import com.evolveum.midpoint.test.amqp.EmbeddedBroker;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.provisioning.ucf.api.ListeningActivity;
import com.evolveum.midpoint.provisioning.ucf.api.async.ActiveAsyncUpdateSource;
import com.evolveum.midpoint.provisioning.ucf.api.async.AsyncUpdateMessageListener;
import com.evolveum.midpoint.provisioning.ucf.impl.builtin.async.update.AsyncUpdateConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.impl.builtin.async.update.sources.Amqp091AsyncUpdateSource;
import com.evolveum.midpoint.xml.ns._public.common.common_3.Amqp091MessageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.Amqp091SourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AsyncUpdateSourceType;

import jakarta.jms.JMSException;
import org.apache.commons.io.IOUtils;
import org.testng.annotations.AfterClass;

import javax.naming.NamingException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static com.evolveum.midpoint.test.amqp.ArtemisTestSupport.acknowledge;

import static org.assertj.core.api.Assertions.assertThat;

public class TestAsyncUpdateCachingAmqp extends TestAsyncUpdateCaching {

    private static final File RESOURCE_ASYNC_CACHING_AMQP_FILE = new File(TEST_DIR, "resource-async-caching-amqp.xml");

    private static final String QUEUE_NAME = "testQueue";

    private final EmbeddedBroker embeddedBroker = new EmbeddedBroker();

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        embeddedBroker.start();
        super.initSystem(initTask, initResult);
        embeddedBroker.createQueue(QUEUE_NAME);
    }

    @Override
    protected File getResourceFile() {
        return RESOURCE_ASYNC_CACHING_AMQP_FILE;
    }

    @AfterClass
    public void stop() throws Exception {
        embeddedBroker.stop();
    }

    @Override
    void prepareMessage(File messageFile) throws IOException, TimeoutException, JMSException, NamingException {
        String message = messageFile != null ?
                String.join("\n", IOUtils.readLines(new FileReader(messageFile)))
                : "";
        Map<String, Object> headers = new HashMap<>();
        headers.put(Amqp091AsyncUpdateSource.HEADER_LAST_MESSAGE, true);
        embeddedBroker.send(QUEUE_NAME, message, headers);
    }

    @Override
    void assertNoUnacknowledgedMessages() {
        try {
            assertThat(embeddedBroker.getMessagesCount(QUEUE_NAME))
                    .as("unacknowledged message count")
                    .isEqualTo(0);
        } catch (JMSException | NamingException e) {
            throw new SystemException(e);
        }
    }
    @SuppressWarnings("unused")
    public static class ArtemisAsyncUpdateTestSource implements ActiveAsyncUpdateSource {

        private final Amqp091SourceType sourceConfiguration;

        private ArtemisAsyncUpdateTestSource(Amqp091SourceType sourceConfiguration) {
            this.sourceConfiguration = sourceConfiguration;
        }

        public static ArtemisAsyncUpdateTestSource create(
                AsyncUpdateSourceType configuration, AsyncUpdateConnectorInstance connectorInstance) {
            if (!(configuration instanceof Amqp091SourceType)) {
                throw new IllegalArgumentException("Expected " + Amqp091SourceType.class.getName() + " but got "
                        + configuration.getClass().getName());
            }
            return new ArtemisAsyncUpdateTestSource((Amqp091SourceType) configuration);
        }

        @Override
        public ListeningActivity startListening(AsyncUpdateMessageListener listener) {
            ArtemisTestSupport.ListenerRuntime runtime = ArtemisTestSupport.startListener(
                    sourceConfiguration.getQueue(),
                    (message, session, last) -> listener.onMessage(
                            new Amqp091MessageType()
                                    .sourceName(sourceConfiguration.getName())
                                    .body(ArtemisTestSupport.readBody(message)),
                            (processed, result) -> acknowledge(message, session, processed)));
            return new ListeningActivity() {
                @Override
                public void stop() {
                    runtime.stop();
                }

                @Override
                public boolean isAlive() {
                    return runtime.isAlive();
                }
            };
        }

        @Override
        public void test(OperationResult parentResult) {
            ArtemisTestSupport.testQueueAccessible(
                    getClass().getName() + ".test",
                    sourceConfiguration.getName(),
                    sourceConfiguration.getQueue(),
                    parentResult);
        }

        @Override
        public void close() {
            // No source-level resources to close. Listener lifecycle is managed by ListeningActivity.
        }
    }
}
