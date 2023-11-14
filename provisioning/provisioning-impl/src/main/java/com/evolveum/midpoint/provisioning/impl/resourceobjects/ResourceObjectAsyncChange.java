/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

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

    /** See {@link UcfAsyncUpdateChange#notificationOnly}. TODO consider removal. */
    private final boolean notificationOnly;

    /** Where to send acknowledgements to. */
    @NotNull private final AcknowledgementSink acknowledgementSink;

    ResourceObjectAsyncChange(@NotNull UcfAsyncUpdateChange ucfAsyncUpdateChange, @NotNull ProvisioningContext originalContext) {
        super(ucfAsyncUpdateChange, originalContext);
        this.notificationOnly = ucfAsyncUpdateChange.isNotificationOnly();
        this.acknowledgementSink = ucfAsyncUpdateChange;
    }

    @Override
    protected void processObjectAndDelta(OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        if (resourceObject != null) {
            // TODO why not in LS case? Probably because ConnId LS operation takes care of it?
            effectiveCtx.applyAttributesDefinition(resourceObject.getPrismObject());
            b.resourceObjectConverter.postProcessResourceObjectRead(effectiveCtx, resourceObject, true, result);
        } else {
            // we will fetch current resource object later; TODO why the difference w.r.t. LS case?
        }

        if (objectDelta != null) {
            // TODO why not in LS case? Probably there's no MODIFY delta there...
            effectiveCtx.applyAttributesDefinition(objectDelta);
        }
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
