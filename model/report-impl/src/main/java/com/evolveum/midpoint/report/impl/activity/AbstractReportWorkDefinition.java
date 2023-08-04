/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.activity;

import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.schema.util.task.work.WorkDefinitionBean;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractReportWorkDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportParameterType;

/**
 * Work definition for report export and imports.
 */
public class AbstractReportWorkDefinition extends AbstractWorkDefinition {

    @NotNull private final ObjectReferenceType reportRef;
    private final ReportParameterType reportParams;

    AbstractReportWorkDefinition(@NotNull WorkDefinitionBean source, @NotNull ConfigurationItemOrigin origin)
            throws SchemaException {
        super(origin);
        var typedDefinition = (AbstractReportWorkDefinitionType) source.getBean();
        reportRef = MiscUtil.requireNonNull(typedDefinition.getReportRef(), () -> "No report definition");
        reportParams = typedDefinition.getReportParam();
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
