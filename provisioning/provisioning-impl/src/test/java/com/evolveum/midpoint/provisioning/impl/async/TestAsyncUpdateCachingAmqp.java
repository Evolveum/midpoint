/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.async;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import org.apache.commons.io.IOUtils;
import org.testng.annotations.AfterClass;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 *
 */
public class TestAsyncUpdateCachingAmqp extends TestAsyncUpdateCaching {

	private final EmbeddedBroker embeddedBroker = new EmbeddedBroker();

	private static final String QUEUE_NAME = "testQueue";

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
		embeddedBroker.send(QUEUE_NAME, message);
	}
}
