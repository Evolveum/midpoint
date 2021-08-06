/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.activity;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.activity.execution.AbstractActivityExecution;
import com.evolveum.midpoint.repo.common.task.PlainIterativeActivityExecution;
import com.evolveum.midpoint.repo.common.task.SearchBasedActivityExecution;
import com.evolveum.midpoint.report.impl.ReportServiceImpl;
import com.evolveum.midpoint.report.impl.controller.fileformat.ReportDataWriter;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Contains common functionality for executions of report-related activities.
 * This is an experiment - using object composition instead of inheritance.
 */
class ExportActivitySupport extends ReportActivitySupport{

    private SaveReportFileSupport saveSupport;

    ExportActivitySupport(AbstractActivityExecution<?, ?, ?> activityExecution, ReportServiceImpl reportService,
            ObjectResolver resolver, AbstractReportWorkDefinition workDefinition) {
        super(activityExecution, reportService, resolver, workDefinition);
    }

    void beforeExecution(OperationResult result) throws CommonException, ActivityExecutionException {
        super.beforeExecution(result);
        setupSaveSupport(result);
    }

    private void setupSaveSupport(OperationResult result) throws CommonException {
        saveSupport = new SaveReportFileSupport(report, runningTask, getCompiledCollectionView(result), reportService);
        saveSupport.initializeExecution(result);
    }

    /**
     * Save exported report to a file.
     */
    public void saveReportFile(String aggregatedData, ReportDataWriter dataWriter, OperationResult result) throws CommonException {
        saveSupport.saveReportFile(aggregatedData, dataWriter, result);
    }

    public void saveReportFile(ReportDataWriter dataWriter, OperationResult result) throws CommonException {
        saveSupport.saveReportFile(dataWriter.getStringData(), dataWriter, result);
    }

    /**
     * Search container objects for iterative task.
     * Temporary until will be implemented iterative search for audit records and containerable objects.
     */
    public List<? extends Containerable> searchRecords(Class<? extends Containerable> type,
            ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options,
            OperationResult result) throws CommonException {
        if (AuditEventRecordType.class.equals(type)) {
            @NotNull SearchResultList<AuditEventRecordType> auditRecords = auditService.searchObjects(query, options, result);
            return auditRecords.getList();
        } else if (ObjectType.class.isAssignableFrom(type)) {
            SearchResultList<PrismObject<ObjectType>> results = modelService.searchObjects((Class<ObjectType>) type, query, options, runningTask, result);
            List list = new ArrayList();
            results.forEach(object -> list.add(object.asObjectable()));
            return list;
        } else {
            SearchResultList<? extends Containerable> containers = modelService.searchContainers(type, query, options, runningTask, result);
            return containers.getList();
        }
    }
}
