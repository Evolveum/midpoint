/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.sync;

import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.provisioning.api.SynchronizationEvent;
import com.evolveum.midpoint.provisioning.impl.adoption.AdoptedChange;
import com.evolveum.midpoint.provisioning.util.ProcessingState;
import com.evolveum.midpoint.util.DebugUtil;

/**
 * TODO
 */
public abstract class SynchronizationEventImpl<AC extends AdoptedChange<?>> implements SynchronizationEvent {

    protected final AC change;

    SynchronizationEventImpl(AC change) {
        this.change = change;
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
        return change.isPreprocessed();
    }

    @Override
    public boolean isSkip() {
        ProcessingState processingState = change.getProcessingState();
        return processingState.isSkipFurtherProcessing() && processingState.getExceptionEncountered() == null;
    }

    @Override
    public boolean isError() {
        ProcessingState processingState = change.getProcessingState();
        return processingState.getExceptionEncountered() != null;
    }

    @Override
    public String getErrorMessage() {
        Throwable e = change.getProcessingState().getExceptionEncountered();
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
}
