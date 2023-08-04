/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.activity;

import static com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory.WorkDefinitionInfo;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractReportWorkDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportParameterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskAffectedObjectsType;

/**
 * Work definition for report export and imports.
 */
public class AbstractReportWorkDefinition extends AbstractWorkDefinition {

    @NotNull private final ObjectReferenceType reportRef;
    private final ReportParameterType reportParams;

    AbstractReportWorkDefinition(@NotNull WorkDefinitionInfo info) throws ConfigurationException {
        super(info);
        var typedDefinition = (AbstractReportWorkDefinitionType) info.getBean();
        reportRef = MiscUtil.configNonNull(typedDefinition.getReportRef(), () -> "No report definition");
        reportParams = typedDefinition.getReportParam();
    }

    public @NotNull ObjectReferenceType getReportRef() {
        return reportRef;
    }

    ReportParameterType getReportParams() {
        return reportParams;
    }

    @Override
    public @Nullable TaskAffectedObjectsType getAffectedObjects() {
        // Not supported for reports yet; it cannot be determined from the work definition alone, without analyzing the report.
        return null;
    }

    @Override
    protected void debugDumpContent(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabel(sb, "reportRef", String.valueOf(reportRef), indent+1);
    }
}
