/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
