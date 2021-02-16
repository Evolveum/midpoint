/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.api;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.AcknowledgementSink;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import static java.util.Collections.emptyList;

/**
 * TODO
 */
public class UcfAsyncUpdateChange extends UcfChange implements AcknowledgementSink {

    /**
     * This means that the change is just a notification that a resource object has changed. To know about its state
     * it has to be fetched. For notification-only changes both objectDelta and currentResourceObject have to be null.
     * (And this flag is introduced to distinguish intentional notification-only changes from malformed ones that have
     * both currentResourceObject and objectDelta missing.)
     */
    private final boolean notificationOnly;
    private final AcknowledgementSink acknowledgeSink;

    public UcfAsyncUpdateChange(int localSequenceNumber, @NotNull Object primaryIdentifierRealValue,
            ObjectClassComplexTypeDefinition objectClassDefinition,
            @NotNull Collection<ResourceAttribute<?>> identifiers,
            ObjectDelta<ShadowType> objectDelta, PrismObject<ShadowType> currentResourceObject,
            boolean notificationOnly, AcknowledgementSink acknowledgeSink) {
        super(localSequenceNumber, primaryIdentifierRealValue, objectClassDefinition, identifiers,
                objectDelta, currentResourceObject, UcfErrorState.success());
        this.notificationOnly = notificationOnly;
        this.acknowledgeSink = acknowledgeSink;
    }

    public UcfAsyncUpdateChange(int localSequenceNumber, UcfErrorState errorState, AcknowledgementSink acknowledgeSink) {
        super(localSequenceNumber, null, null, emptyList(),
                null, null, errorState);
        this.notificationOnly = false;
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
        if (errorState.isSuccess()) {
            stateCheck(objectClassDefinition != null, "No object class definition");
        }
    }
}
