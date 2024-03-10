/*
 * Copyright (C) 2010-2023 Evolveum and contributors
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

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

    void beforeRun(OperationResult result) throws CommonException, ActivityRunException {
        super.beforeRun(result);
        setupSaveSupport();
    }

    private void setupSaveSupport() {
        saveSupport = new SaveReportFileSupport(activityRun, getReport(), reportService);
    }

    /**
     * Saves the data in the simple case: just dumping content of `dataWriter` to the output file.
     */
    void saveSimpleReportData(
            ReportDataWriter<? extends ExportedReportDataRow, ? extends ExportedReportHeaderRow> dataWriter,
            OperationResult result) throws CommonException {
        saveSupport.saveSimpleReportData(dataWriter, result);
    }

    /**
     * Save exported report to a file. This is the variant for distributed reports that assumes we have the
     * aggregated data as a String, plus pre-existing (empty) aggregated {@link ReportDataType} object.
     */
    void saveAggregatedReportData(
            @NotNull String aggregatedData,
            @NotNull ReportDataWriter<? extends ExportedReportDataRow, ? extends ExportedReportHeaderRow> completingDataWriter,
            @NotNull ObjectReferenceType aggregatedDataRef,
            @NotNull OperationResult result) throws CommonException {
        saveSupport.saveAggregatedReportData(aggregatedData, completingDataWriter, aggregatedDataRef, result);
    }

    /**
     * Search container objects for iterative task.
     */
    public <T> void searchRecordsIteratively(
            Class<T> type,
            ObjectQuery query,
            ObjectHandler<T> handler,
            Collection<SelectorOptions<GetOperationOptions>> options,
            OperationResult result) throws CommonException {
        if (AuditEventRecordType.class.equals(type)) {
            //noinspection unchecked
            modelAuditService.searchObjectsIterative(
                    query,
                    options,
                    ((ObjectHandler<Containerable>) handler)::handle,
                    runningTask,
                    result);
        } else if (ObjectType.class.isAssignableFrom(type)) {
            Class<? extends ObjectType> objectType = type.asSubclass(ObjectType.class);
            //noinspection unchecked
            modelService.searchObjectsIterative(
                    objectType,
                    query,
                    (object, lResult) -> ((ObjectHandler<Containerable>) handler).handle(object.asObjectable(), lResult),
                    options,
                    runningTask,
                    result);
        } else if (modelService.isSupportedByRepository(SimulationResultType.class) && Containerable.class.isAssignableFrom(type)) {
            Class<? extends Containerable> containerableType = type.asSubclass(Containerable.class);
            modelService.searchContainersIterative(
                    containerableType,
                    query,
                    ((ObjectHandler<Containerable>) handler)::handle,
                    options,
                    runningTask,
                    result
            );
        } else if (Containerable.class.isAssignableFrom(type)) {
            // TODO: Temporary - until iterative search is available
            Class<? extends Containerable> containerableType = type.asSubclass(Containerable.class);
            SearchResultList<? extends Containerable> values =
                    modelService.searchContainers(containerableType, query, options, runningTask, result);
            //noinspection unchecked
            values.forEach(value -> ((ObjectHandler<Containerable>) handler).handle(value, result));




        } else if (Referencable.class.isAssignableFrom(type)) {
            //noinspection unchecked
            modelService.searchReferencesIterative(query,
                    (value, lResult) -> ((ObjectHandler<ObjectReferenceType>) handler).handle(value, lResult),
                    options, runningTask, result);
        } else {
            throw new UnsupportedOperationException("Unsupported object type for report: " + type);
        }
    }

    /**
     * Count container objects for iterative task.
     * Temporary until will be implemented iterative search for audit records and containerable objects.
     */
    int countRecords(Class<?> type,
            ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options,
            OperationResult result) throws CommonException {
        if (AuditEventRecordType.class.equals(type)) {
            return modelAuditService.countObjects(query, options, runningTask, result);
        } else if (ObjectType.class.isAssignableFrom(type)) {
            Class<? extends ObjectType> objectType = type.asSubclass(ObjectType.class);
            return or0(modelService.countObjects(objectType, query, options, runningTask, result));
        } else if (Containerable.class.isAssignableFrom(type)) {
            //noinspection unchecked
            return or0(modelService.countContainers(((Class<? extends Containerable>) type), query, options, runningTask, result));
        } else if (Referencable.class.isAssignableFrom(type)) {
            return or0(modelService.countReferences(query, options, runningTask, result));
        } else {
            throw new UnsupportedOperationException("Unsupported object type for report: " + type);
        }
    }
}
