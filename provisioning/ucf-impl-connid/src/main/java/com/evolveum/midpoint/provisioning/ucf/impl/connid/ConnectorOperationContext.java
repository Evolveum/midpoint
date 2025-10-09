/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.provisioning.ucf.api.SchemaAwareUcfExecutionContext;
import com.evolveum.midpoint.schema.processor.CompleteResourceSchema;

/** Context for any non-trivial connector operation. */
record ConnectorOperationContext(
        @NotNull ConnectorContext connectorContext,
        @NotNull SchemaAwareUcfExecutionContext ucfExecutionContext) {

    @NotNull CompleteResourceSchema getResourceSchemaRequired() {
        return ucfExecutionContext.getResourceSchema();
    }
}
