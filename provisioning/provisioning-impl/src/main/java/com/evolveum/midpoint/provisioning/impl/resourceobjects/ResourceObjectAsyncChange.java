/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.ucf.api.UcfAsyncUpdateChange;
import com.evolveum.midpoint.schema.AcknowledgementSink;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugUtil;
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
    protected String toStringExtra() {
        return ", notificationOnly=" + notificationOnly;
    }

    @Override
    protected void debugDumpExtra(StringBuilder sb, int indent) {
        sb.append('\n');
        DebugUtil.debugDumpWithLabel(sb, "notificationOnly", notificationOnly, indent + 1);
    }

    @Override
    public void acknowledge(boolean release, OperationResult result) {
        acknowledgementSink.acknowledge(release, result);
    }

    @Override
    public Trace getLogger() {
        return LOGGER;
    }

    public boolean isNotificationOnly() {
        return notificationOnly;
    }
}
