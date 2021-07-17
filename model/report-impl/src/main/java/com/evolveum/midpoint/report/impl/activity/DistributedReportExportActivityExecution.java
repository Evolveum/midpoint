/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.activity;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.execution.AbstractCompositeActivityExecution;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportExportWorkStateType;

/**
 * There's nothing special to do here.
 */
class DistributedReportExportActivityExecution
        extends AbstractCompositeActivityExecution
        <DistributedReportWorkDefinition,
                DistributedReportExportActivityHandler,
                ReportExportWorkStateType> {

    DistributedReportExportActivityExecution(
            @NotNull ExecutionInstantiationContext<DistributedReportWorkDefinition, DistributedReportExportActivityHandler> context) {
        super(context);
    }
}
