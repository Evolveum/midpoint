/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import static java.util.Collections.emptyList;

import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.ResourceObjectConverter;
import com.evolveum.midpoint.provisioning.impl.sync.SkipProcessingException;
import com.evolveum.midpoint.provisioning.ucf.api.UcfAsyncUpdateChange;
import com.evolveum.midpoint.schema.AcknowledgementSink;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.jetbrains.annotations.NotNull;

/**
 * Asynchronous change represented at the level of resource object converter, i.e. completely processed except
 * for repository (shadow) connection.
 */
public class ResourceObjectAsyncChange extends ResourceObjectChange implements AcknowledgementSink {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceObjectAsyncChange.class);

    private final boolean notificationOnly;

    /** Where to send acknowledgements to. */
    @NotNull private final AcknowledgementSink acknowledgementSink;

    public ResourceObjectAsyncChange(@NotNull UcfAsyncUpdateChange ucfAsyncUpdateChange) {
        super(ucfAsyncUpdateChange);
        this.notificationOnly = ucfAsyncUpdateChange.isNotificationOnly();
        this.acknowledgementSink = ucfAsyncUpdateChange;
    }

    public ResourceObjectAsyncChange(int localSequenceNumber, @NotNull Throwable throwable, @NotNull AcknowledgementSink acknowledgementSink) {
        super(localSequenceNumber, null, emptyList(), null, null);
        this.notificationOnly = false;
        this.acknowledgementSink = acknowledgementSink;
        setSkipFurtherProcessing(throwable);
    }

    public void preprocess(ResourceObjectConverter converter, ProvisioningContext originalCtx, Task listenerTask,
            OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            ExpressionEvaluationException, SecurityViolationException, SkipProcessingException {

        processingState.checkSkipProcessing();

        LOGGER.trace("Change before pre-processing:\n{}", debugDumpLazily());

        determineProvisioningContext(originalCtx, listenerTask);

        setResourceRefIfMissing(context.getResourceOid()); // TODO why not in other kinds of changes (LS, EXT)?

        postProcessResourceObjectIfAny(converter, result);

        LOGGER.trace("Pre-processed change\n:{}", debugDumpLazily());
    }

    private void postProcessResourceObjectIfAny(ResourceObjectConverter converter, OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            ExpressionEvaluationException, SecurityViolationException {
        if (resourceObject != null) {
            // TODO
            //  1. why not in LS case? Probably because ConnId LS operation takes care of it?
            //  2. why not also for objectDelta?
            converter.getShadowCaretaker().applyAttributesDefinition(context, resourceObject);
            converter.postProcessResourceObjectRead(context, resourceObject, true, result);
        } else {
            // we will fetch current resource object later; TODO why the difference w.r.t. LS case?
        }

        if (objectDelta != null) {
            converter.getShadowCaretaker().applyAttributesDefinition(context, objectDelta);
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
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "notificationOnly", notificationOnly, indent + 1);
    }

    @Override
    public void acknowledge(boolean release, OperationResult result) {
        acknowledgementSink.acknowledge(release, result);
    }
}
