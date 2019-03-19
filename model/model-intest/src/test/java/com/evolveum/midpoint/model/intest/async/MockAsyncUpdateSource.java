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

package com.evolveum.midpoint.model.intest.async;

import com.evolveum.midpoint.provisioning.ucf.api.AsyncUpdateMessageListener;
import com.evolveum.midpoint.provisioning.ucf.api.AsyncUpdateSource;
import com.evolveum.midpoint.provisioning.ucf.api.ListeningActivity;
import com.evolveum.midpoint.provisioning.ucf.impl.builtin.async.AsyncUpdateConnectorInstance;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AnyDataAsyncUpdateMessageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AsyncUpdateMessageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AsyncUpdateSourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UcfChangeType;

import java.util.LinkedList;
import java.util.Queue;

/**
 * See also MockAsyncUpdateSource in provisioning-impl.
 */
@SuppressWarnings("unused")
public class MockAsyncUpdateSource implements AsyncUpdateSource {

	private static final Trace LOGGER = TraceManager.getTrace(MockAsyncUpdateSource.class);

	private final Queue<AsyncUpdateMessageType> messages = new LinkedList<>();

	public static final MockAsyncUpdateSource INSTANCE = new MockAsyncUpdateSource();

	private static class DummyListeningActivityImpl implements ListeningActivity {
		@Override
		public void stop() {
			// no-op
		}
	}

	public static MockAsyncUpdateSource create(AsyncUpdateSourceType configuration, AsyncUpdateConnectorInstance connectorInstance) {
		LOGGER.info("create() method called");
		return INSTANCE;
	}

	@Override
	public ListeningActivity startListening(AsyncUpdateMessageListener listener) throws SchemaException {
		LOGGER.info("startListening() method called");
		for (AsyncUpdateMessageType message : messages) {
			listener.onMessage(message);
		}
		return new DummyListeningActivityImpl();
	}

	@Override
	public void test(OperationResult parentResult) {
		LOGGER.info("test() method called");
	}

	public void prepareMessage(UcfChangeType changeDescription) {
		AnyDataAsyncUpdateMessageType message = new AnyDataAsyncUpdateMessageType();
		message.setData(changeDescription);
		prepareMessage(message);
	}

	public void prepareMessage(AsyncUpdateMessageType message) {
		messages.offer(message);
	}

	public void reset() {
		messages.clear();
	}

	@Override
	public String toString() {
		return "MockAsyncUpdateSource{" +
				"messages:" + messages.size() +
				'}';
	}
}
