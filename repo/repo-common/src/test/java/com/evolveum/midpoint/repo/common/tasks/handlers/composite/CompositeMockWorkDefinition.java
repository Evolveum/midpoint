/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.tasks.handlers.composite;

import static com.evolveum.midpoint.repo.common.tasks.handlers.composite.MockComponentActivityExecution.NS_EXT;
import static com.evolveum.midpoint.schema.util.task.WorkDefinitionWrapper.UntypedWorkDefinitionWrapper.getPcv;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.DebugUtil;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.repo.common.task.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.schema.util.task.WorkDefinitionSource;

public class CompositeMockWorkDefinition extends AbstractWorkDefinition {

    private static final ItemName IDENTIFIER_NAME = new ItemName(NS_EXT, "identifier");
    private static final ItemName DELAY_NAME = new ItemName(NS_EXT, "delay");
    private static final ItemName STEPS_NAME = new ItemName(NS_EXT, "steps");
    private static final ItemName OPENING_NAME = new ItemName(NS_EXT, "opening");
    private static final ItemName CLOSING_NAME = new ItemName(NS_EXT, "closing");

    static final QName WORK_DEFINITION_TYPE_QNAME = new QName(NS_EXT, "CompositeMockDefinitionType");

    private final String identifier;
    private final long delay;
    private final int steps;
    private final Boolean opening;
    private final Boolean closing;

    public CompositeMockWorkDefinition(WorkDefinitionSource source) {
        PrismContainerValue<?> pcv = getPcv(source);
        this.identifier = pcv != null ? pcv.getPropertyRealValue(IDENTIFIER_NAME, String.class) : null;
        this.delay = pcv != null ? pcv.getPropertyRealValue(DELAY_NAME, Long.class) : 0;
        this.steps = pcv != null ? pcv.getPropertyRealValue(STEPS_NAME, Integer.class) : 1;
        this.opening = pcv != null ? pcv.getPropertyRealValue(OPENING_NAME, Boolean.class) : null;
        this.closing = pcv != null ? pcv.getPropertyRealValue(CLOSING_NAME, Boolean.class) : null;
    }

    public String getIdentifier() {
        return identifier;
    }

    public long getDelay() {
        return delay;
    }

    public int getSteps() {
        return steps;
    }

    public Boolean isOpening() {
        return opening;
    }

    public Boolean isClosing() {
        return closing;
    }

    @Override
    public @NotNull QName getType() {
        return WORK_DEFINITION_TYPE_QNAME;
    }

    @Override
    public void debugDumpContent(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabelLn(sb, "identifier", identifier, indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, "delay", delay, indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, "steps", steps, indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, "opening", opening, indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, "closing", closing, indent+1);
    }
}
