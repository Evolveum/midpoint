/*
 * Copyright (c) 2010-2019 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
