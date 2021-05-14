/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.tasks.handlers.simple;

import static com.evolveum.midpoint.repo.common.tasks.handlers.composite.CompositeMockSubActivityExecution.NS_EXT;
import static com.evolveum.midpoint.schema.util.task.WorkDefinitionWrapper.UntypedWorkDefinitionWrapper.getPcv;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.repo.common.task.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.task.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.schema.util.task.WorkDefinitionSource;
import com.evolveum.midpoint.util.DebugUtil;

public class SimpleMockWorkDefinition extends AbstractWorkDefinition {

    private static final ItemName MESSAGE_NAME = new ItemName(NS_EXT, "message");

    static final QName WORK_DEFINITION_TYPE_QNAME = new QName(NS_EXT, "SimpleMockDefinitionType");

    private final String message;

    private SimpleMockWorkDefinition(WorkDefinitionSource source) {
        PrismContainerValue<?> pcv = getPcv(source);
        this.message = pcv != null ? pcv.getPropertyRealValue(MESSAGE_NAME, String.class) : null;
    }

    static WorkDefinitionFactory.WorkDefinitionSupplier supplier() {
        return SimpleMockWorkDefinition::new;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public @NotNull QName getType() {
        return WORK_DEFINITION_TYPE_QNAME;
    }

    @Override
    protected void debugDumpContent(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabelLn(sb, "message", message, indent+1);
    }
}
