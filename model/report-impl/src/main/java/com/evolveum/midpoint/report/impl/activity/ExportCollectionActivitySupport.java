/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.activity;

import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.report.impl.ReportServiceImpl;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommonException;

/**
 * Contains common functionality for executions of collection export report-related activities.
 * This is an experiment - using object composition instead of inheritance.
 */
class ExportCollectionActivitySupport extends ExportActivitySupport {

    ExportCollectionActivitySupport(AbstractActivityRun<?, ?, ?> activityRun, ReportServiceImpl reportService,
            ObjectResolver resolver, AbstractReportWorkDefinition workDefinition) {
        super(activityRun, reportService, resolver, workDefinition);
    }

    @Override
    public void stateCheck(OperationResult result) throws CommonException {
        MiscUtil.stateCheck(report.getObjectCollection() != null, "Only collection-based reports are supported here");
        super.stateCheck(result);
    }
}
