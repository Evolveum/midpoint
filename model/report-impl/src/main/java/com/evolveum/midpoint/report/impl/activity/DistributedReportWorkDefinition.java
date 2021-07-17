/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.activity;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.common.activity.definition.ObjectSetSpecificationProvider;
import com.evolveum.midpoint.schema.util.task.work.ObjectSetUtil;
import com.evolveum.midpoint.schema.util.task.work.WorkDefinitionSource;
import com.evolveum.midpoint.schema.util.task.work.WorkDefinitionWrapper.TypedWorkDefinitionWrapper;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DistributedReportExportWorkDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class DistributedReportWorkDefinition extends AbstractReportWorkDefinition implements ObjectSetSpecificationProvider {

    @NotNull private final ObjectSetType objects;

    DistributedReportWorkDefinition(WorkDefinitionSource source) throws SchemaException {
        super(source);
        stateCheck(source instanceof TypedWorkDefinitionWrapper, "Legacy source is not supported here");
        DistributedReportExportWorkDefinitionType typedDefinition = (DistributedReportExportWorkDefinitionType)
                ((TypedWorkDefinitionWrapper) source).getTypedDefinition();
        objects = typedDefinition.getObjects() != null ? typedDefinition.getObjects() : new ObjectSetType(PrismContext.get());
        ObjectSetUtil.applyDefaultObjectType(objects, ObjectType.COMPLEX_TYPE);
    }

    @Override
    public @NotNull ObjectSetType getObjectSetSpecification() {
        return objects;
    }

    @Override
    protected void debugDumpContent(StringBuilder sb, int indent) {
        super.debugDumpContent(sb, indent);
        DebugUtil.debugDumpWithLabelLn(sb, "objects", objects, indent+1);
    }
}
