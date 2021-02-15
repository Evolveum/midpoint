/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import java.util.Collection;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.util.ProcessingState;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * TODO
 */
public class ExternalResourceObjectChange extends ResourceObjectChange {

    private static final Trace LOGGER = TraceManager.getTrace(ExternalResourceObjectChange.class);

    public ExternalResourceObjectChange(int localSequenceNumber,
            @NotNull Object primaryIdentifierRealValue, @NotNull Collection<ResourceAttribute<?>> identifiers,
            PrismObject<ShadowType> resourceObject, ObjectDelta<ShadowType> objectDelta,
            ProvisioningContext ctx, ResourceObjectConverter resourceObjectConverter) {
        super(localSequenceNumber, primaryIdentifierRealValue, identifiers, resourceObject, objectDelta,
                ProcessingState.success(), ctx, resourceObjectConverter.getLocalBeans());
    }

    @Override
    protected String toStringExtra() {
        return "";
    }

    @Override
    protected void debugDumpExtra(StringBuilder sb, int indent) {
    }

    @Override
    public void initializeInternal(Task task, OperationResult result) throws SchemaException, ObjectNotFoundException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        updateRefinedObjectClass();
        freezeIdentifiers();
    }

    @Override
    public Trace getLogger() {
        return LOGGER;
    }
}
