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

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Smart integration service client to be used when there is no real service available.
 */
public class MockServiceClientImpl implements ServiceClient {

    private static final Trace LOGGER = TraceManager.getTrace(MockServiceClientImpl.class);

    private Object lastRequest;
    private final Iterator<Object> responses;
    private final Function<Object, Object> responseFunction;

    public MockServiceClientImpl(Object... responses) {
        this.responses = List.of(responses).iterator();
        this.responseFunction = null;
    }

    public MockServiceClientImpl(List<Object> responses) {
        this.responses = responses.iterator();
        this.responseFunction = null;
    }

    public MockServiceClientImpl(Function<Object, Object> responseFunction) {
        this.responses = null;
        this.responseFunction = responseFunction;
    }

    @Override
    public <REQ, RESP> RESP invoke(Method method, REQ request, Class<RESP> responseClass) throws SchemaException {
        LOGGER.debug("Invoking {} with request:\n{}",
                method, PrismContext.get().jsonSerializer().serializeRealValueContent(request));
        lastRequest = request;

        Object response;
        if (responseFunction != null) {
            response = responseFunction.apply(request);
        } else {
            if (!responses.hasNext()) {
                throw new AssertionError("No more responses available in the mock service client");
            }
            response = responses.next();
        }

        if (response instanceof RuntimeException exception) {
            throw exception;
        }
        //noinspection unchecked
        return (RESP) response;
    }

    @Override
    public <REQ, RESP> CompletableFuture<RESP> invokeAsync(Method method, REQ request, Class<RESP> responseClass) {
        try {
            return CompletableFuture.completedFuture(invoke(method, request, responseClass));
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
    }

    public Object getLastRequest() {
        return lastRequest;
    }

    @Override
    public void close() {
    }
}
