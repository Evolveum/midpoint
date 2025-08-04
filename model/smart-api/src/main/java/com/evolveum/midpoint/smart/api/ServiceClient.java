/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.api;

import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Generic interface for a client that communicates with a remote service.
 * Can point to a real remote service or to a mock for testing purposes.
 */
public interface ServiceClient extends AutoCloseable {

    /** Invokes the specified method on the remote microservice (or on its substitution) with the given request. */
    <REQ, RESP> RESP invoke(Method method, REQ request, Class<RESP> responseClass) throws SchemaException;

    @Override
    void close();

    enum Method {
        SUGGEST_OBJECT_TYPES, SUGGEST_FOCUS_TYPE, MATCH_SCHEMA, SUGGEST_MAPPING;
    }
}
