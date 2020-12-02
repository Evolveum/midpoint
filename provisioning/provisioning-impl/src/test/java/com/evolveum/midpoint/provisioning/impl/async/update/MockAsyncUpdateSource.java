/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.async.update;

import com.evolveum.midpoint.provisioning.ucf.api.async.AsyncUpdateMessageListener;
import com.evolveum.midpoint.provisioning.ucf.api.async.PassiveAsyncUpdateSource;
import com.evolveum.midpoint.provisioning.ucf.impl.builtin.async.update.AsyncUpdateConnectorInstance;
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
 * See also MockAsyncUpdateSource in model-intest.
 */
@SuppressWarnings("unused")
public class MockAsyncUpdateSource implements PassiveAsyncUpdateSource {

    private static final Trace LOGGER = TraceManager.getTrace(MockAsyncUpdateSource.class);

    private final Queue<AsyncUpdateMessageType> messages = new LinkedList<>();

    static final MockAsyncUpdateSource INSTANCE = new MockAsyncUpdateSource();

    public static MockAsyncUpdateSource create(AsyncUpdateSourceType configuration, AsyncUpdateConnectorInstance connectorInstance) {
        LOGGER.info("create() method called");
        return INSTANCE;
    }

    @Override
    public boolean getNextUpdate(AsyncUpdateMessageListener listener) throws SchemaException {
        AsyncUpdateMessageType message = messages.poll();
        if (message != null) {
            listener.onMessage(message);
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
    }

    public void reset() {
        messages.clear();
    }

    @Override
    public void close() {
    }

    @Override
    public String toString() {
        return "MockAsyncUpdateSource{" +
                "messages:" + messages.size() +
                '}';
    }
}
