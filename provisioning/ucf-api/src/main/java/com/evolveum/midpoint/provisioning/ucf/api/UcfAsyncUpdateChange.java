/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.api;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.util.Collection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.AcknowledgementSink;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttribute;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/** The UCF-level asynchronous update change. */
public class UcfAsyncUpdateChange extends UcfChange implements AcknowledgementSink {

    /**
     * This means that the change is just a notification that a resource object has changed. To know about its state
     * it has to be fetched. For notification-only changes both objectDelta and currentResourceObject have to be null.
     * (And this flag is introduced to distinguish intentional notification-only changes from malformed ones that have
     * both currentResourceObject and objectDelta missing.)
     *
     * TODO consider removal - is this needed any longer? (It looks nice and useful, though.)
     */
    private final boolean notificationOnly;
    private final AcknowledgementSink acknowledgeSink;

    public UcfAsyncUpdateChange(
            int localSequenceNumber,
            @NotNull Object primaryIdentifierRealValue,
            @NotNull ResourceObjectDefinition resourceObjectDefinition,
            @NotNull Collection<ShadowSimpleAttribute<?>> identifiers,
            @Nullable ObjectDelta<ShadowType> objectDelta,
            @Nullable UcfResourceObject currentResourceObject,
            boolean notificationOnly,
            AcknowledgementSink acknowledgeSink) {
        super(localSequenceNumber, primaryIdentifierRealValue, resourceObjectDefinition, identifiers,
                objectDelta, currentResourceObject, UcfErrorState.success());
        this.notificationOnly = notificationOnly;
        this.acknowledgeSink = acknowledgeSink;
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
        acknowledgeSink.acknowledge(release, result);
    }

    @Override
    protected void checkObjectClassDefinitionPresence() {
        stateCheck(resourceObjectDefinition != null, "No object class definition");
    }
}
