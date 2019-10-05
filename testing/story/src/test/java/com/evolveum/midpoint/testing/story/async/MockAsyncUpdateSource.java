/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story.async;

import com.evolveum.midpoint.provisioning.ucf.api.AsyncUpdateMessageListener;
import com.evolveum.midpoint.provisioning.ucf.api.AsyncUpdateSource;
import com.evolveum.midpoint.provisioning.ucf.api.ListeningActivity;
import com.evolveum.midpoint.provisioning.ucf.impl.builtin.async.AsyncUpdateConnectorInstance;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.util.LinkedList;
import java.util.Queue;

/**
 * See also MockAsyncUpdateSource in model-intest and provisioning-impl.
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

		@Override
		public AsyncUpdateListeningActivityInformationType getInformation() {
			return new AsyncUpdateListeningActivityInformationType()
					.status(AsyncUpdateListeningActivityStatusType.ALIVE);
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
