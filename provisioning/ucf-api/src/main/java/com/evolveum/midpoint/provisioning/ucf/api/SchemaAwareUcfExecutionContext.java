/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.api;

import com.evolveum.midpoint.schema.processor.CompleteResourceSchema;

import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.jetbrains.annotations.NotNull;

/**
 * {@link UcfExecutionContext} with resource schema.
 *
 * It exists because some operations need to know the resource schema, but others don't.
 */
public class SchemaAwareUcfExecutionContext extends UcfExecutionContext {

    /** Resource schema: provided here, as it's needed at some places in the connector. */
    @NotNull private final CompleteResourceSchema resourceSchema;

    public SchemaAwareUcfExecutionContext(
            @NotNull LightweightIdentifierGenerator lightweightIdentifierGenerator,
            @NotNull ResourceType resource,
            @NotNull CompleteResourceSchema resourceSchema,
            @NotNull Task task) {
        super(lightweightIdentifierGenerator, resource, task);
        this.resourceSchema = resourceSchema;
    }

    public @NotNull CompleteResourceSchema getResourceSchema() {
        return resourceSchema;
    }
}
