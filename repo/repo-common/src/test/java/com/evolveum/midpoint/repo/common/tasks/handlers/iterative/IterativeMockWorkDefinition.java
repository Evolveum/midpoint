/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.tasks.handlers.iterative;

import static com.evolveum.midpoint.repo.common.tasks.handlers.composite.MockComponentActivityExecution.NS_EXT;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.util.task.WorkDefinitionWrapper;

import com.google.common.base.MoreObjects;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.schema.util.task.WorkDefinitionSource;
import com.evolveum.midpoint.util.DebugUtil;

public class IterativeMockWorkDefinition extends AbstractWorkDefinition {

    private static final ItemName FROM_NAME = new ItemName(NS_EXT, "from");
    private static final ItemName TO_NAME = new ItemName(NS_EXT, "to");
    private static final ItemName MESSAGE_NAME = new ItemName(NS_EXT, "message");

    static final QName WORK_DEFINITION_TYPE_QNAME = new QName(NS_EXT, "IterativeMockDefinitionType");

    private final int from;
    private final int to;
    private final String message;

    IterativeMockWorkDefinition(WorkDefinitionSource source) {
        PrismContainerValue<?> pcv = ((WorkDefinitionWrapper.UntypedWorkDefinitionWrapper) source).getUntypedDefinition();
        this.from = MoreObjects.firstNonNull(pcv.getPropertyRealValue(FROM_NAME, Integer.class), 0);
        this.to = MoreObjects.firstNonNull(pcv.getPropertyRealValue(TO_NAME, Integer.class), from);
        this.message = pcv.getPropertyRealValue(MESSAGE_NAME, String.class);
    }

    public int getFrom() {
        return from;
    }

    public int getTo() {
        return to;
    }

    public String getMessage() {
        return message;
    }

    @Override
    protected void debugDumpContent(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabelLn(sb, "message", message, indent+1);
    }
}
