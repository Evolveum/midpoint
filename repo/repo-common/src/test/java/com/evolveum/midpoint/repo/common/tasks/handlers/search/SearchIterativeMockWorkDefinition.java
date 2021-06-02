/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.tasks.handlers.search;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.ObjectSetSpecificationProvider;
import com.evolveum.midpoint.schema.util.task.WorkDefinitionSource;
import com.evolveum.midpoint.schema.util.task.WorkDefinitionWrapper;
import com.evolveum.midpoint.util.DebugUtil;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSetType;

import javax.xml.namespace.QName;

import static com.evolveum.midpoint.repo.common.tasks.handlers.composite.MockComponentActivityExecution.NS_EXT;

public class SearchIterativeMockWorkDefinition extends AbstractWorkDefinition implements ObjectSetSpecificationProvider {

    private static final ItemName OBJECT_SET_NAME = new ItemName(NS_EXT, "objectSet");
    private static final ItemName MESSAGE_NAME = new ItemName(NS_EXT, "message");

    static final QName WORK_DEFINITION_TYPE_QNAME = new QName(NS_EXT, "SearchIterativeMockDefinitionType");

    private final ObjectSetType objectSet;
    private final String message;

    SearchIterativeMockWorkDefinition(WorkDefinitionSource source) {
        PrismContainerValue<?> pcv = ((WorkDefinitionWrapper.UntypedWorkDefinitionWrapper) source).getUntypedDefinition();
        this.objectSet = pcv.getItemRealValue(OBJECT_SET_NAME, ObjectSetType.class);
        this.message = pcv.getPropertyRealValue(MESSAGE_NAME, String.class);
    }

    public ObjectSetType getObjectSet() {
        return objectSet;
    }

    public String getMessage() {
        return message;
    }

    @Override
    protected void debugDumpContent(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabelLn(sb, "objectSet", objectSet, indent+1);
    }

    @Override
    public ObjectSetType getObjectSetSpecification() {
        return objectSet;
    }
}
