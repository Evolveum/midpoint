/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.async.update;

import com.evolveum.midpoint.provisioning.ucf.impl.builtin.async.update.sources.Amqp091AsyncUpdateSource;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.amqp.EmbeddedBroker;
import org.apache.commons.io.IOUtils;
import org.testng.annotations.AfterClass;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class TestAsyncUpdateCachingAmqp extends TestAsyncUpdateCaching {

    private static final File RESOURCE_ASYNC_CACHING_AMQP_FILE = new File(TEST_DIR, "resource-async-caching-amqp.xml");

    private static final String QUEUE_NAME = "testQueue";

    private final EmbeddedBroker embeddedBroker = new EmbeddedBroker();

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        embeddedBroker.start();
        embeddedBroker.createQueue(QUEUE_NAME);
    }

    @Override
    protected File getResourceFile() {
        return RESOURCE_ASYNC_CACHING_AMQP_FILE;
    }

    @AfterClass
    public void stop() {
        embeddedBroker.stop();
    }

    @Override
    void prepareMessage(File messageFile) throws IOException, TimeoutException {
        String message = String.join("\n", IOUtils.readLines(new FileReader(messageFile)));
        Map<String, Object> headers = new HashMap<>();
        headers.put(Amqp091AsyncUpdateSource.HEADER_LAST_MESSAGE, true);
        embeddedBroker.send(QUEUE_NAME, message, headers);
    }
}
