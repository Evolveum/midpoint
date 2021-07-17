/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.activity;

import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.schema.util.task.work.LegacyWorkDefinitionSource;
import com.evolveum.midpoint.schema.util.task.work.WorkDefinitionSource;
import com.evolveum.midpoint.schema.util.task.work.WorkDefinitionWrapper.TypedWorkDefinitionWrapper;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractReportWorkDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import org.jetbrains.annotations.NotNull;

public class AbstractReportWorkDefinition extends AbstractWorkDefinition {

    @NotNull private final ObjectReferenceType reportRef;

    AbstractReportWorkDefinition(WorkDefinitionSource source) throws SchemaException {
        ObjectReferenceType rawReportRef;

        if (source instanceof LegacyWorkDefinitionSource) {
            LegacyWorkDefinitionSource legacy = (LegacyWorkDefinitionSource) source;
            rawReportRef = legacy.getObjectRef();
        } else {
            AbstractReportWorkDefinitionType typedDefinition = (AbstractReportWorkDefinitionType)
                    ((TypedWorkDefinitionWrapper) source).getTypedDefinition();
            rawReportRef = typedDefinition.getReportRef();
        }
        reportRef = MiscUtil.requireNonNull(rawReportRef, () -> "No report definition");
    }

    public @NotNull ObjectReferenceType getReportRef() {
        return reportRef;
    }

    @Override
    protected void debugDumpContent(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabelLn(sb, "reportRef", String.valueOf(reportRef), indent+1);
    }
}
