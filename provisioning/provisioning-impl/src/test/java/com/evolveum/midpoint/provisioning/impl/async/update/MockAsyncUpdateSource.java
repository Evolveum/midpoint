/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.impl.async.update;

import com.evolveum.midpoint.provisioning.ucf.api.async.AsyncUpdateMessageListener;
import com.evolveum.midpoint.provisioning.ucf.api.async.PassiveAsyncUpdateSource;
import com.evolveum.midpoint.provisioning.ucf.impl.builtin.async.update.AsyncUpdateConnectorInstance;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AnyDataAsyncUpdateMessageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AsyncUpdateMessageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AsyncUpdateSourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UcfChangeType;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * See also MockAsyncUpdateSource in model-intest.
 */
@SuppressWarnings("unused")
public class MockAsyncUpdateSource implements PassiveAsyncUpdateSource {

    private static final Trace LOGGER = TraceManager.getTrace(MockAsyncUpdateSource.class);

    private final Queue<AsyncUpdateMessageType> messages = new LinkedList<>();

    private final AtomicInteger unacknowledgedMessagesCounter = new AtomicInteger(0);

    static final MockAsyncUpdateSource INSTANCE = new MockAsyncUpdateSource();

    public static MockAsyncUpdateSource create(AsyncUpdateSourceType configuration, AsyncUpdateConnectorInstance connectorInstance) {
        LOGGER.info("create() method called");
        return INSTANCE;
    }

    @Override
    public boolean getNextUpdate(AsyncUpdateMessageListener listener) {
        AsyncUpdateMessageType message = messages.poll();
        if (message != null) {
            listener.onMessage(message, (processed, result) -> {
                unacknowledgedMessagesCounter.decrementAndGet();
            });
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void test(OperationResult parentResult) {
        LOGGER.info("test() method called");
    }

    @Override
    public boolean isOpen() {
        return !messages.isEmpty();
    }

    void prepareMessage(UcfChangeType changeDescription) {
        AnyDataAsyncUpdateMessageType message = new AnyDataAsyncUpdateMessageType();
        message.setData(changeDescription);
        messages.offer(message);
        unacknowledgedMessagesCounter.incrementAndGet();
    }

    public void reset() {
        messages.clear();
        unacknowledgedMessagesCounter.set(0);
    }

    public int getUnacknowledgedMessagesCount() {
        return unacknowledgedMessagesCounter.get();
    }

    @Override
    public void close() {
    }

    @Override
    public String toString() {
        return "MockAsyncUpdateSource{" +
                "messages:" + messages.size() +
                ", unacknowledged:" + unacknowledgedMessagesCounter.get() +
                '}';
    }
}
