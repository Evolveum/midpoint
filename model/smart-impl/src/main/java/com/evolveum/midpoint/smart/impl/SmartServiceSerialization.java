/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.smart.impl;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Shared serialization helper for Smart service requests.
 *
 * Used by both the transport client and auditing decorator to ensure that the request
 * sent to the Smart service is serialized in the same way as the request recorded in audit.
 */
class SmartServiceSerialization {

    private SmartServiceSerialization() {
    }

    static String serializeRequest(Object request) throws SchemaException {
        return PrismContext.get().jsonSerializer().serializeRealValueContent(request);
    }
}
