/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.tasks.handlers.iterative;

import static com.evolveum.midpoint.repo.common.tasks.handlers.composite.MockComponentActivityRun.NS_EXT;
import static com.evolveum.midpoint.util.MiscUtil.or0;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.repo.common.activity.run.buckets.segmentation.content.NumericIntervalBucketUtil.Interval;
import com.evolveum.midpoint.schema.util.task.work.WorkDefinitionBean;

import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskAffectedObjectsType;

import com.google.common.base.MoreObjects;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.util.DebugUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class IterativeMockWorkDefinition extends AbstractWorkDefinition {

    private static final ItemName FROM_NAME = new ItemName(NS_EXT, "from");
    private static final ItemName TO_NAME = new ItemName(NS_EXT, "to");
    private static final ItemName MESSAGE_NAME = new ItemName(NS_EXT, "message");
    private static final ItemName DELAY_NAME = new ItemName(NS_EXT, "delay");

    static final QName WORK_DEFINITION_TYPE_QNAME = new QName(NS_EXT, "IterativeMockDefinitionType");
    static final QName WORK_DEFINITION_ITEM_QNAME = new QName(NS_EXT, "iterativeMock");

    /** Lower bound, inclusive. */
    private final int from;

    /** Upper bound, inclusive. */
    private final int to;

    private final String message;

    private final long delay;

    IterativeMockWorkDefinition(@NotNull WorkDefinitionBean source, @NotNull QName activityTypeName) {
        super(activityTypeName);
        PrismContainerValue<?> pcv = source.getValue();
        this.from = MoreObjects.firstNonNull(pcv.getPropertyRealValue(FROM_NAME, Integer.class), 0);
        this.to = MoreObjects.firstNonNull(pcv.getPropertyRealValue(TO_NAME, Integer.class), from);
        this.message = pcv.getPropertyRealValue(MESSAGE_NAME, String.class);
        this.delay = or0(pcv.getPropertyRealValue(DELAY_NAME, Long.class));
    }

    public int getFrom() {
        return from;
    }

    public int getTo() {
        return to;
    }

    public Interval getInterval() {
        return Interval.of(from, to + 1);
    }

    public String getMessage() {
        return message;
    }

    public long getDelay() {
        return delay;
    }

    @Override
    public @Nullable TaskAffectedObjectsType getAffectedObjects() {
        return null; // not relevant here
    }

    @Override
    protected void debugDumpContent(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabelLn(sb, "from", from, indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, "to", to, indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, "message", message, indent+1);
        DebugUtil.debugDumpWithLabel(sb, "delay", delay, indent+1);
    }
}
