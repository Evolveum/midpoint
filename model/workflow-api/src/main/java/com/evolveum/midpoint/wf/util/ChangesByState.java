/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.util;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.ObjectTreeDeltas;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

/**
 * @author mederly
 */
public class ChangesByState<F extends FocusType> implements DebugDumpable {

    @NotNull private final ObjectTreeDeltas<F> applied;
    @NotNull private final ObjectTreeDeltas<F> beingApplied;
    @NotNull private final ObjectTreeDeltas<F> waitingToBeApplied;
    @NotNull private final ObjectTreeDeltas<F> waitingToBeApproved;
    @NotNull private final ObjectTreeDeltas<F> rejected;
    @NotNull private final ObjectTreeDeltas<F> canceled;

    public ChangesByState(PrismContext prismContext) {
        applied = new ObjectTreeDeltas<>(prismContext);
        beingApplied = new ObjectTreeDeltas<>(prismContext);
        waitingToBeApplied = new ObjectTreeDeltas<>(prismContext);
        waitingToBeApproved = new ObjectTreeDeltas<>(prismContext);
        rejected = new ObjectTreeDeltas<>(prismContext);
        canceled = new ObjectTreeDeltas<>(prismContext);
    }

    @NotNull
    public ObjectTreeDeltas<F> getApplied() {
        return applied;
    }

    @NotNull
    public ObjectTreeDeltas<F> getBeingApplied() {
        return beingApplied;
    }

    @NotNull
    public ObjectTreeDeltas<F> getWaitingToBeApplied() {
        return waitingToBeApplied;
    }

    @NotNull
    public ObjectTreeDeltas<F> getWaitingToBeApproved() {
        return waitingToBeApproved;
    }

    @NotNull
    public ObjectTreeDeltas<F> getRejected() {
        return rejected;
    }

    @NotNull
    public ObjectTreeDeltas<F> getCanceled() {
        return canceled;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpWithLabelLn(sb, "Applied", applied, indent);
        DebugUtil.debugDumpWithLabelLn(sb, "Being applied", beingApplied, indent);
        DebugUtil.debugDumpWithLabelLn(sb, "Waiting to be applied", waitingToBeApplied, indent);
        DebugUtil.debugDumpWithLabelLn(sb, "Waiting to be approved", waitingToBeApproved, indent);
        DebugUtil.debugDumpWithLabelLn(sb, "Rejected", rejected, indent);
        DebugUtil.debugDumpWithLabelLn(sb, "Canceled", canceled, indent);
        return sb.toString();
    }
}
