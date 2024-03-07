/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.controller;

import com.evolveum.midpoint.report.impl.activity.ReportDataCreationActivityRun;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.report.impl.ReportServiceImpl;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Controls the process of exporting collection-based reports.
 *
 * Currently the only use of this class is to be a "bridge" between the world of the activity framework
 * (represented mainly by {@link ReportDataCreationActivityRun} class) and a set of cooperating
 * classes that implement the report export itself. However, in the future it may be used in other ways,
 * independently of the activity framework.
 *
 * The process is driven by the activity execution that calls the following methods of this class:
 *
 * 1. {@link #initialize(RunningTask, OperationResult)} that sets up the processes (in a particular worker task),
 * 2. {@link #beforeBucketExecution(int, OperationResult)} that starts processing of a given work bucket,
 * 3. {@link #handleDataRecord(int, Containerable, RunningTask, OperationResult)} that processes given prism object,
 * 4. {@link #afterBucketExecution(int, RunningTask, OperationResult)} that wraps up processing of a bucket, storing partial results
 * to be aggregated in the follow-up activity.
 *
 * @param <C> Type of records to be processed.
 */
@Experimental
public class CollectionDistributedExportController<C extends Containerable> extends CollectionExportController<C> {

    private static final Trace LOGGER = TraceManager.getTrace(CollectionDistributedExportController.class);

    /**
     * Reference to the global (aggregated) report data object.
     *
     * Currently always present. But in the future we may provide simplified version of the process that executes
     * in a single bucket, not needing aggregated report data object.
     */
    @NotNull private final ObjectReferenceType globalReportDataRef;

    public CollectionDistributedExportController(@NotNull ReportDataSource<C> dataSource,
            @NotNull ReportDataWriter<ExportedReportDataRow, ExportedReportHeaderRow> dataWriter,
            @NotNull ReportType report,
            @NotNull ObjectReferenceType globalReportDataRef,
            @NotNull ReportServiceImpl reportService,
            @NotNull CompiledObjectCollectionView compiledCollection,
            ReportParameterType reportParameters) {

        super(dataSource, dataWriter, report, reportService, compiledCollection, reportParameters);

        this.globalReportDataRef = globalReportDataRef;
    }

    /**
     * Called after bucket of data is executed, i.e. after all the data from current bucket were passed to
     * {@link #handleDataRecord(int, Containerable, RunningTask, OperationResult)} method.
     *
     * We have to store the data into partial report data object in the repository, to be aggregated into final
     * report afterwards.
     */
    public void afterBucketExecution(int bucketNumber, @NotNull RunningTask runningTask, OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException {

        if (!runningTask.canRun()) {
            LOGGER.warn("Not storing the (partial) resulting report for bucket #{}, as the activity is being suspended: {}",
                    bucketNumber, report);
            return;
        }

        String data = dataWriter.getStringData();
        dataWriter.reset();

        LOGGER.debug("Bucket {} is complete ({} chars in report). Let's create the partial report data object:\n{}",
                bucketNumber, data.length(), data);

        // Note that we include [oid] in the object name to allow a poor man searching over the children.
        // It's until parentRef is properly indexed in the repository.
        // We also make the name sortable by padding the number with zeros: until we can sort on the sequential number.
        String name = String.format("Partial report data for %s (%08d)", globalReportDataRef.getOid(), bucketNumber);

        ReportDataType partialReportData = new ReportDataType()
                .name(name)
                .reportRef(ObjectTypeUtil.createObjectRef(report))
                .parentRef(globalReportDataRef.clone())
                .sequentialNumber(bucketNumber)
                .data(data);
        repositoryService.addObject(partialReportData.asPrismObject(), null, result);
    }
}
