/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.tasks.handlers.composite;

import static com.evolveum.midpoint.repo.common.tasks.handlers.composite.MockComponentActivityRun.NS_EXT;
import static com.evolveum.midpoint.util.MiscUtil.or0;

import java.util.Objects;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskAffectedObjectsType;

public class CompositeMockWorkDefinition extends AbstractWorkDefinition {

    private static final ItemName MESSAGE_NAME = new ItemName(NS_EXT, "message");
    private static final ItemName DELAY_NAME = new ItemName(NS_EXT, "delay");
    private static final ItemName STEPS_NAME = new ItemName(NS_EXT, "steps");
    private static final ItemName OPENING_NAME = new ItemName(NS_EXT, "opening");
    private static final ItemName CLOSING_NAME = new ItemName(NS_EXT, "closing");

    static final QName WORK_DEFINITION_TYPE_QNAME = new QName(NS_EXT, "CompositeMockDefinitionType");
    static final QName WORK_DEFINITION_ITEM_QNAME = new QName(NS_EXT, "compositeMock");

    private final String message;
    private final long delay;
    private final int steps;
    private final Boolean opening;
    private final Boolean closing;

    CompositeMockWorkDefinition(@NotNull WorkDefinitionFactory.WorkDefinitionInfo info) {
        super(info);
        PrismContainerValue<?> pcv = info.source().getValue();
        this.message = pcv.getPropertyRealValue(MESSAGE_NAME, String.class);
        this.delay = or0(pcv.getPropertyRealValue(DELAY_NAME, Long.class));
        this.steps = Objects.requireNonNullElse(pcv.getPropertyRealValue(STEPS_NAME, Integer.class), 1);
        this.opening = pcv.getPropertyRealValue(OPENING_NAME, Boolean.class);
        this.closing = pcv.getPropertyRealValue(CLOSING_NAME, Boolean.class);
    }

    public String getMessage() {
        return message;
    }

    public long getDelay() {
        return delay;
    }

    public int getSteps() {
        return steps;
    }

    private Boolean isOpening() {
        return opening;
    }

    private Boolean isClosing() {
        return closing;
    }

    boolean isOpeningEnabled() {
        return !Boolean.FALSE.equals(isOpening());
    }

    boolean isClosingEnabled() {
        return !Boolean.FALSE.equals(isClosing());
    }

    @Override
    public @Nullable TaskAffectedObjectsType getAffectedObjects() {
        return null; // not relevant here
    }

    @Override
    public void debugDumpContent(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabelLn(sb, "message", message, indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, "delay", delay, indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, "steps", steps, indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, "opening", opening, indent+1);
        DebugUtil.debugDumpWithLabel(sb, "closing", closing, indent+1);
    }
}
