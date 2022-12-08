/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import com.evolveum.midpoint.provisioning.impl.shadows.sync.NotApplicableException;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.ucf.api.UcfAsyncUpdateChange;
import com.evolveum.midpoint.schema.AcknowledgementSink;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Asynchronous change represented at the level of resource object converter, i.e. completely processed except
 * for repository (shadow) connection.
 */
public class ResourceObjectAsyncChange extends ResourceObjectChange implements AcknowledgementSink {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceObjectAsyncChange.class);

    private final boolean notificationOnly;

    /** Where to send acknowledgements to. */
    @NotNull private final AcknowledgementSink acknowledgementSink;

    ResourceObjectAsyncChange(@NotNull UcfAsyncUpdateChange ucfAsyncUpdateChange,
            @NotNull ResourceObjectConverter converter, @NotNull ProvisioningContext originalContext) {
        super(ucfAsyncUpdateChange, null, originalContext, converter.getBeans());
        this.notificationOnly = ucfAsyncUpdateChange.isNotificationOnly();
        this.acknowledgementSink = ucfAsyncUpdateChange;
    }

    @Override
    protected void processObjectAndDelta(OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        ResourceObjectConverter converter = beans.resourceObjectConverter;
        if (resourceObject != null) {
            // TODO why not in LS case? Probably because ConnId LS operation takes care of it?
            context.applyAttributesDefinition(resourceObject);
            converter.postProcessResourceObjectRead(context, resourceObject, true, result);
        } else {
            // we will fetch current resource object later; TODO why the difference w.r.t. LS case?
        }

        if (objectDelta != null) {
            // TODO why not in LS case? Probably there's no MODIFY delta there...
            context.applyAttributesDefinition(objectDelta);
        }
    }

    public boolean isNotificationOnly() {
        return notificationOnly;
    }

    @Override
    protected String toStringExtra() {
        return ", notificationOnly=" + notificationOnly;
    }

    @Override
    protected void debugDumpExtra(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabelLn(sb, "notificationOnly", notificationOnly, indent + 1);
    }

    @Override
    public void acknowledge(boolean release, OperationResult result) {
        acknowledgementSink.acknowledge(release, result);
    }

    @Override
    public Trace getLogger() {
        return LOGGER;
    }
}
