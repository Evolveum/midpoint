/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.activity;

import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.report.api.ReportConstants;
import com.evolveum.midpoint.schema.util.task.work.LegacyWorkDefinitionSource;
import com.evolveum.midpoint.schema.util.task.work.WorkDefinitionSource;
import com.evolveum.midpoint.schema.util.task.work.WorkDefinitionWrapper.TypedWorkDefinitionWrapper;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractReportWorkDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportParameterType;

import org.jetbrains.annotations.NotNull;

/**
 * Work definition for report export and imports.
 */
public class AbstractReportWorkDefinition extends AbstractWorkDefinition {

    @NotNull private final ObjectReferenceType reportRef;
    private final ReportParameterType reportParams;

    AbstractReportWorkDefinition(WorkDefinitionSource source) throws SchemaException {
        ObjectReferenceType rawReportRef;
        ReportParameterType rawReportParams;

        if (source instanceof LegacyWorkDefinitionSource) {
            LegacyWorkDefinitionSource legacy = (LegacyWorkDefinitionSource) source;
            rawReportRef = legacy.getObjectRef();
            rawReportParams =
                    legacy.getExtensionItemRealValue(ReportConstants.REPORT_PARAMS_PROPERTY_NAME, ReportParameterType.class);
        } else {
            AbstractReportWorkDefinitionType typedDefinition = (AbstractReportWorkDefinitionType)
                    ((TypedWorkDefinitionWrapper) source).getTypedDefinition();
            rawReportRef = typedDefinition.getReportRef();
            rawReportParams = typedDefinition.getReportParam();
        }
        reportRef = MiscUtil.requireNonNull(rawReportRef, () -> "No report definition");
        reportParams = rawReportParams;
    }

    public @NotNull ObjectReferenceType getReportRef() {
        return reportRef;
    }

    ReportParameterType getReportParams() {
        return reportParams;
    }

    @Override
    protected void debugDumpContent(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabel(sb, "reportRef", String.valueOf(reportRef), indent+1);
    }
}
