/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.test.smart;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.smart.api.ServiceClient;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import java.util.Iterator;
import java.util.List;

/**
 * Smart integration service client to be used when there is no real service available.
 */
public class MockServiceClientImpl implements ServiceClient {

    private static final Trace LOGGER = TraceManager.getTrace(MockServiceClientImpl.class);

    private Object lastRequest;
    private final Iterator<Object> responses;

    public MockServiceClientImpl(Object... responses) {
        this.responses = List.of(responses).iterator();
    }

    @Override
    public <REQ, RESP> RESP invoke(Method method, REQ request, Class<RESP> responseClass) throws SchemaException {
        LOGGER.debug("Invoking {} with request:\n{}",
                method, PrismContext.get().jsonSerializer().serializeRealValueContent(request));
        lastRequest = request;
        if (!responses.hasNext()) {
            throw new AssertionError("No more responses available in the mock service client");
        }
        var response = responses.next();
        if (response instanceof RuntimeException exception) {
            throw exception;
        }
        //noinspection unchecked
        return (RESP) response;
    }

    public Object getLastRequest() {
        return lastRequest;
    }

    @Override
    public void close() {
    }
}
