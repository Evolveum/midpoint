/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.activity;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory.WorkDefinitionInfo;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClassicReportImportWorkDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * Work definition for report import activity.
 */
public class ClassicReportImportWorkDefinition extends AbstractReportWorkDefinition {

    @NotNull private final ObjectReferenceType reportDataRef;

    ClassicReportImportWorkDefinition(@NotNull WorkDefinitionInfo info) throws ConfigurationException {
        super(info);
        var typedDefinition = (ClassicReportImportWorkDefinitionType) info.getBean();
        reportDataRef = MiscUtil.configNonNull(typedDefinition.getReportDataRef(), () -> "No report data object specified");
    }

    @NotNull ObjectReferenceType getReportDataRef() {
        return reportDataRef;
    }

    @Override
    protected void debugDumpContent(StringBuilder sb, int indent) {
        super.debugDumpContent(sb, indent);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "reportDataRef", String.valueOf(reportDataRef), indent + 1);
    }
}
