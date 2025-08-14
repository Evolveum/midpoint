/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.test.smart;

import com.evolveum.midpoint.smart.api.ServiceClient;
import com.evolveum.midpoint.util.exception.SchemaException;

import java.util.Iterator;
import java.util.List;

/**
 * Smart integration service client to be used when there is no real service available.
 */
public class MockServiceClientImpl implements ServiceClient {

    private Object lastRequest;
    private final Iterator<Object> responses;

    public MockServiceClientImpl(Object... responses) {
        this.responses = List.of(responses).iterator();
    }

    @Override
    public <REQ, RESP> RESP invoke(Method method, REQ request, Class<RESP> responseClass) throws SchemaException {
        lastRequest = request;
        //noinspection unchecked
        return (RESP) responses.next();
    }

    public Object getLastRequest() {
        return lastRequest;
    }

    @Override
    public void close() {
    }
}
