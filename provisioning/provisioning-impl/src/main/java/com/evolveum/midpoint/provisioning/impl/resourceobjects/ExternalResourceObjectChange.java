/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import java.util.Collection;

import com.evolveum.midpoint.provisioning.util.ErrorState;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.schema.processor.ResourceObjectClassDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * A resource object change coming from external sources. This is a sibling of live sync and async update changes.
 */
public class ExternalResourceObjectChange extends ResourceObjectChange {

    private static final Trace LOGGER = TraceManager.getTrace(ExternalResourceObjectChange.class);

    public ExternalResourceObjectChange(
            int localSequenceNumber,
            @NotNull Object primaryIdentifierRealValue,
            ResourceObjectClassDefinition objectClassDefinition,
            @NotNull Collection<ResourceAttribute<?>> identifiers,
            ResourceObject resourceObject,
            ObjectDelta<ShadowType> objectDelta,
            ProvisioningContext ctx) {
        super(
                localSequenceNumber,
                primaryIdentifierRealValue,
                objectClassDefinition,
                identifiers,
                resourceObject,
                objectDelta,
                ErrorState.ok(),
                ctx);
    }

    @Override
    protected String toStringExtra() {
        return "";
    }

    @Override
    protected void debugDumpExtra(StringBuilder sb, int indent) {
    }

    @Override
    public Trace getLogger() {
        return LOGGER;
    }
}
