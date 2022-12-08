/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.sync;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.provisioning.api.SynchronizationEvent;
import com.evolveum.midpoint.provisioning.impl.shadows.ShadowedChange;
import com.evolveum.midpoint.util.DebugUtil;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asPrismObject;

/**
 * TODO
 */
public abstract class SynchronizationEventImpl<AC extends ShadowedChange<?>> implements SynchronizationEvent {

    protected final AC change;

    SynchronizationEventImpl(AC change) {
        this.change = change;

        // This is a temporary measure. We assume that we get fully initialized (successfully or not) change here.
        change.getInitializationState().checkAfterInitialization();
    }

    @Override
    public ResourceObjectShadowChangeDescription getChangeDescription() {
        return change.getShadowChangeDescription();
    }

    @Override
    public int getSequentialNumber() {
        return change.getSequentialNumber();
    }

    @Override
    public Object getCorrelationValue() {
        return change.getPrimaryIdentifierValue();
    }

    @Override
    public boolean isComplete() {
        // Note that the initialization completeness was checked at construction. (Temporarily.)
        return change.getInitializationState().isOk();
    }

    @Override
    public boolean isNotApplicable() {
        return change.getInitializationState().isNotApplicable();
    }

    @Override
    public boolean isError() {
        return change.getInitializationState().isError();
    }

    @Override
    public String getErrorMessage() {
        Throwable e = change.getInitializationState().getExceptionEncountered();
        if (e != null) {
            return e.getClass().getSimpleName() + ": " + e.getMessage();
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return "SynchronizationEventImpl{" +
                "change=" + change +
                '}';
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append(this.getClass().getSimpleName());
        sb.append("\n");
        DebugUtil.debugDumpWithLabelLn(sb, "change", change, indent + 1);
        return sb.toString();
    }

    @Override
    public String getShadowOid() {
        return change.getShadowOid();
    }

    @Override
    public PrismObject<ShadowType> getShadowedObject() {
        return asPrismObject(change.getShadowedObject());
    }

    @Override
    public int compareTo(@NotNull SynchronizationEvent o) {
        return Integer.compare(getSequentialNumber(), o.getSequentialNumber());
    }
}
