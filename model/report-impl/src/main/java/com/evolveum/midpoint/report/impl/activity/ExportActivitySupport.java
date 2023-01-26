/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.activity;

import static com.evolveum.midpoint.util.MiscUtil.or0;

import java.util.Collection;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;
import com.evolveum.midpoint.report.impl.ReportServiceImpl;
import com.evolveum.midpoint.report.impl.controller.ExportedReportDataRow;
import com.evolveum.midpoint.report.impl.controller.ExportedReportHeaderRow;
import com.evolveum.midpoint.report.impl.controller.ReportDataWriter;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ObjectHandler;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Contains common functionality for executions of export report-related activities.
 * This is an experiment - using object composition instead of inheritance.
 */
public class ExportActivitySupport extends ReportActivitySupport {

    private SaveReportFileSupport saveSupport;

    ExportActivitySupport(AbstractActivityRun<?, ?, ?> activityRun, ReportServiceImpl reportService,
            ObjectResolver resolver, AbstractReportWorkDefinition workDefinition) {
        super(activityRun, reportService, resolver, workDefinition);
    }

    void beforeExecution(OperationResult result) throws CommonException, ActivityRunException {
        super.beforeExecution(result);
        setupSaveSupport();
    }

    private void setupSaveSupport() {
        saveSupport = new SaveReportFileSupport(activityRun, getReport(), reportService);
    }

    /**
     * Save exported report to a file.
     */
    void saveReportFile(String aggregatedData,
            ReportDataWriter<? extends ExportedReportDataRow, ? extends ExportedReportHeaderRow> dataWriter,
            OperationResult result) throws CommonException {
        saveSupport.saveReportFile(aggregatedData, dataWriter, result);
    }

    void saveReportFile(ReportDataWriter<? extends ExportedReportDataRow, ? extends ExportedReportHeaderRow> dataWriter,
            OperationResult result) throws CommonException {
        saveSupport.saveReportFile(dataWriter, result);
    }

    /**
     * Search container objects for iterative task.
     */
    public void searchRecordsIteratively(
            Class<? extends Containerable> type,
            ObjectQuery query,
            ObjectHandler<Containerable> handler,
            Collection<SelectorOptions<GetOperationOptions>> options,
            OperationResult result) throws CommonException {
        if (AuditEventRecordType.class.equals(type)) {
            modelAuditService.searchObjectsIterative(
                    query,
                    options,
                    handler::handle,
                    runningTask,
                    result);
        } else if (ObjectType.class.isAssignableFrom(type)) {
            Class<? extends ObjectType> objectType = type.asSubclass(ObjectType.class);
            modelService.searchObjectsIterative(
                    objectType,
                    query,
                    (object, lResult) -> handler.handle(object.asObjectable(), lResult),
                    options,
                    runningTask,
                    result);
        } else {
            // Temporary - until iterative search is available
            SearchResultList<? extends Containerable> values =
                    modelService.searchContainers(type, query, options, runningTask, result);
            values.forEach(value -> handler.handle(value, result));
        }
    }

    /**
     * Count container objects for iterative task.
     * Temporary until will be implemented iterative search for audit records and containerable objects.
     */
    public int countRecords(Class<? extends Containerable> type,
            ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options,
            OperationResult result) throws CommonException {
        if (AuditEventRecordType.class.equals(type)) {
            return modelAuditService.countObjects(query, options, runningTask, result);
        } else if (ObjectType.class.isAssignableFrom(type)) {
            Class<? extends ObjectType> objectType = type.asSubclass(ObjectType.class);
            return or0(modelService.countObjects(objectType, query, options, runningTask, result));
        } else if (Containerable.class.isAssignableFrom(type)) {
            return or0(modelService.countContainers(type, query, options, runningTask, result));
        } else if (Referencable.class.isAssignableFrom(type)) {
            return or0(modelService.countReferences(query, options, runningTask, result));
        } else {
            throw new UnsupportedOperationException("Unsupported object type for report: " + type);
        }
    }
}
