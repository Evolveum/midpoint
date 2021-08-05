/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.controller.fileformat;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.model.common.util.DefaultColumnUtils;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.report.impl.ReportServiceImpl;
import com.evolveum.midpoint.report.impl.activity.ReportDataCreationActivityExecution;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.report.impl.controller.fileformat.GenericSupport.evaluateCondition;
import static com.evolveum.midpoint.report.impl.controller.fileformat.GenericSupport.getHeaderColumns;

import static java.util.Objects.requireNonNull;

/**
 * Controls the process of exporting collection-based reports.
 *
 * Currently the only use of this class is to be a "bridge" between the world of the activity framework
 * (represented mainly by {@link ReportDataCreationActivityExecution} class) and a set of cooperating
 * classes that implement the report export itself. However, in the future it may be used in other ways,
 * independently of the activity framework.
 *
 * The process is driven by the activity execution that calls the following methods of this class:
 *
 * 1. {@link #initialize(RunningTask, OperationResult)} that sets up the processes (in a particular worker task),
 * 2. {@link #beforeBucketExecution(int, OperationResult)} that starts processing of a given work bucket,
 * 3. {@link #handleDataRecord(int, Containerable, RunningTask, OperationResult)} that processes given prism object,
 * 4. {@link #afterBucketExecution(int, OperationResult)} that wraps up processing of a bucket, storing partial results
 * to be aggregated in the follow-up activity.
 *
 * @param <C> Type of records to be processed.
 */
@Experimental
public class CollectionBasedDistributedExportController<C extends Containerable> extends CollectionBasedExportController<C> {

    private static final Trace LOGGER = TraceManager.getTrace(CollectionBasedDistributedExportController.class);

    /**
     * Reference to the global (aggregated) report data object.
     *
     * Currently always present. But in the future we may provide simplified version of the process that executes
     * in a single bucket, not needing aggregated report data object.
     */
    @NotNull private final ObjectReferenceType globalReportDataRef;

    /** Configuration of the report export, taken from the report. */
    @NotNull private final ObjectCollectionReportEngineConfigurationType configuration;

    /**
     * Columns for the report.
     */
    private List<GuiObjectColumnType> columns;

    /**
     * Values of report parameters.
     *
     * TODO Currently filled-in from the task extension. But this is to be changed to the work definition.
     */
    private VariablesMap parameters;

    public CollectionBasedDistributedExportController(@NotNull ReportDataSource<C> dataSource,
                                                      @NotNull ReportDataWriter dataWriter,
                                                      @NotNull ReportType report,
                                                      @NotNull ObjectReferenceType globalReportDataRef,
                                                      @NotNull ReportServiceImpl reportService,
                                                      @NotNull CompiledObjectCollectionView compiledCollection) {

        super(dataSource, dataWriter, report, reportService, compiledCollection);

        this.globalReportDataRef = globalReportDataRef;
        this.configuration = report.getObjectCollection();
    }


    /**
     * Called after bucket of data is executed, i.e. after all the data from current bucket were passed to
     * {@link #handleDataRecord(int, Containerable, RunningTask, OperationResult)} method.
     *
     * We have to store the data into partial report data object in the repository, to be aggregated into final
     * report afterwards.
     */
    public void afterBucketExecution(int bucketNumber, OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException {
        String data = dataWriter.getStringData();
        dataWriter.reset();

        LOGGER.info("Bucket {} is complete ({} chars in report). Let's create the partial report data object:\n{}",
                bucketNumber, data.length(), data); // todo debug

        // Note that we include [oid] in the object name to allow a poor man searching over the children.
        // It's until parentRef is properly indexed in the repository.
        // We also make the name sortable by padding the number with zeros: until we can sort on the sequential number.
        String name = String.format("Partial report data for [%s] (%08d)", globalReportDataRef.getOid(), bucketNumber);

        ReportDataType partialReportData = new ReportDataType(prismContext)
                .name(name)
                .reportRef(ObjectTypeUtil.createObjectRef(report, prismContext))
                .parentRef(globalReportDataRef.clone())
                .sequentialNumber(bucketNumber)
                .data(data);
        repositoryService.addObject(partialReportData.asPrismObject(), null, result);
    }
}
