/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import java.util.Collection;

import com.evolveum.midpoint.provisioning.ucf.api.UcfResourceObject;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.util.ErrorState;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttribute;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
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
            @NotNull ResourceObjectDefinition objectDefinition,
            @NotNull Collection<ShadowSimpleAttribute<?>> identifiers,
            UcfResourceObject rawResourceObject,
            ObjectDelta<ShadowType> objectDelta,
            ProvisioningContext ctx) {
        super(
                localSequenceNumber,
                primaryIdentifierRealValue,
                objectDefinition,
                identifiers,
                rawResourceObject,
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
