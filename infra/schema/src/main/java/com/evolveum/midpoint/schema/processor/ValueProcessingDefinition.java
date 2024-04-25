/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.io.Serializable;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.DebugDumpable;

/**
 * Complex processing of an attribute or association value.
 *
 * On inbound side it involves the _correlation_ to a corresponding focus item value followed by
 * invoking the _synchronization reaction_ related to the correlation result.
 */
public class ValueProcessingDefinition
        implements DebugDumpable, Serializable {

    @NotNull private final ResourceObjectInboundDefinition inboundDefinition;

    public ValueProcessingDefinition(@NotNull ResourceObjectInboundDefinition inboundDefinition) {
        this.inboundDefinition = inboundDefinition;
    }

    @Override
    public String debugDump(int indent) {
        return inboundDefinition.debugDump(indent);
    }

    public boolean needsInboundProcessing() {
        return inboundDefinition.hasAnyInbounds();
    }

    public @NotNull ResourceObjectInboundDefinition getInboundDefinition() {
        return inboundDefinition;
    }
}
