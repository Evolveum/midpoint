/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.controller.fileformat;

import java.util.*;
import java.util.stream.Collectors;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.report.impl.activity.ReportDataCreationActivityExecution;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.model.common.util.DefaultColumnUtils;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.report.impl.ReportServiceImpl;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import static com.evolveum.midpoint.report.impl.controller.fileformat.GenericSupport.*;

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
 * @param <C> Type of records to be processed. TODO reconsider if it's OK to have a parameterized type like this
 */
@Experimental
public class CollectionBasedExportController<C extends Containerable> {

    private static final Trace LOGGER = TraceManager.getTrace(CollectionBasedExportController.class);

    /**
     * Data source for the report. It is initialized from within this class. But the actual feeding
     * of data from the source to this class is ensured out of band, "behind the scenes". For example,
     * if activity framework is used, then it itself feeds the data to this controller.
     */
    @NotNull private final ReportDataSource<C> dataSource;

    /**
     * Definition of records that are processed. Initialized along with the data source.
     */
    private PrismContainerDefinition<C> recordDefinition;

    /**
     * Data writer for the report. Produces e.g. CSV or HTML data.
     */
    @NotNull private final ReportDataWriter dataWriter;

    /** The report of which an export is being done. */
    @NotNull private final ReportType report;

    /**
     * Reference to the global (aggregated) report data object.
     *
     * Currently always present. But in the future we may provide simplified version of the process that executes
     * in a single bucket, not needing aggregated report data object.
     */
    @NotNull private final ObjectReferenceType globalReportDataRef;

    /** Configuration of the report export, taken from the report. */
    @NotNull private final ObjectCollectionReportEngineConfigurationType configuration;

    /** Compiled final collection from more collections and archetypes related to object type. */
    @NotNull private final CompiledObjectCollectionView compiledCollection;

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

    // Useful Spring beans
    private final ReportServiceImpl reportService;
    private final PrismContext prismContext;
    private final SchemaRegistry schemaRegistry;
    private final SchemaService schemaService;
    private final ModelInteractionService modelInteractionService;
    private final RepositoryService repositoryService;
    private final LocalizationService localizationService;

    public CollectionBasedExportController(@NotNull ReportDataSource<C> dataSource,
            @NotNull ReportDataWriter dataWriter,
            @NotNull ReportType report,
            @NotNull ObjectReferenceType globalReportDataRef,
            @NotNull ReportServiceImpl reportService,
            @NotNull CompiledObjectCollectionView compiledCollection) {

        this.dataSource = dataSource;
        this.dataWriter = dataWriter;
        this.report = report;
        this.globalReportDataRef = globalReportDataRef;
        this.configuration = report.getObjectCollection();
        this.reportService = reportService;
        this.prismContext = reportService.getPrismContext();
        this.schemaRegistry = reportService.getPrismContext().getSchemaRegistry();
        this.schemaService = reportService.getSchemaService();
        this.modelInteractionService = reportService.getModelInteractionService();
        this.repositoryService = reportService.getRepositoryService();
        this.localizationService = reportService.getLocalizationService();
        this.compiledCollection = compiledCollection;
    }

    /**
     * Prepares the controller for accepting the source data:
     * initializes the data source, determines columns, etc.
     */
    public void initialize(@NotNull RunningTask task, @NotNull OperationResult result)
            throws CommonException {

        columns = MiscSchemaUtil.orderCustomColumns(compiledCollection.getColumns());

        initializeParameters(configuration.getParameter(), task); // must come before data source initialization
        initializeDataSource(task, result);
    }

    private void initializeDataSource(RunningTask task, OperationResult result) throws CommonException {

        CollectionRefSpecificationType collectionConfig = configuration.getCollection();

        Class<Containerable> type = reportService.resolveTypeForReport(collectionConfig, compiledCollection);
        Collection<SelectorOptions<GetOperationOptions>> defaultOptions = DefaultColumnUtils.createOption(type, schemaService);

        ModelInteractionService.SearchSpec<C> searchSpec = modelInteractionService.getSearchSpecificationFromCollection(
                collectionConfig, compiledCollection.getContainerType(), defaultOptions, parameters, task, result);

        recordDefinition = requireNonNull(
                schemaRegistry.findContainerDefinitionByCompileTimeClass(searchSpec.type),
                () -> "No definition for " + searchSpec.type + " found");

        dataSource.initialize(searchSpec.type, searchSpec.query, searchSpec.options);
    }

    private void initializeParameters(List<SearchFilterParameterType> parametersDefinitions, Task task) {
        VariablesMap parameters = reportService.getParameters(task);
        initializeMissingParametersToNull(parameters, parametersDefinitions);
        this.parameters = parameters;
    }

    private void initializeMissingParametersToNull(VariablesMap parameters, List<SearchFilterParameterType> parametersDefinitions) {
        for (SearchFilterParameterType parameterDefinition : parametersDefinitions) {
            if (!parameters.containsKey(parameterDefinition.getName())) {
                Class<?> clazz = schemaRegistry.determineClassForType(parameterDefinition.getType());
                parameters.put(parameterDefinition.getName(), null, clazz);
            }
        }
    }

    /**
     * Called before bucket of data is executed, i.e. before data start flowing to
     * {@link #handleDataRecord(int, Containerable, RunningTask, OperationResult)} method.
     *
     * We have to prepare for collecting the data.
     */
    public void beforeBucketExecution(int sequentialNumber, @SuppressWarnings("unused") OperationResult result) {
        if (sequentialNumber == 1 && dataWriter.shouldWriteHeader()) {
            setHeaderRow();
        }
    }

    private void setHeaderRow() {
        List<ExportedReportHeaderColumn> headerColumns = columns.stream()
                .map(column -> {
                    Validate.notNull(column.getName(), "Name of column is null");
                    return getHeaderColumns(column, recordDefinition, localizationService);
                })
                .collect(Collectors.toList());

        dataWriter.setHeaderRow(ExportedReportHeaderRow.fromColumns(headerColumns));
    }

    /**
     * BEWARE: Can be called from multiple threads at once.
     * The resulting rows should be sorted on sequentialNumber.
     */
    public void handleDataRecord(int sequentialNumber, C record, RunningTask workerTask, OperationResult result) {

        VariablesMap variables = new VariablesMap();
        variables.putAll(parameters);
        variables.put(ExpressionConstants.VAR_OBJECT, record, recordDefinition);

        ExpressionType condition = configuration.getCondition();
        if (condition != null) {
            try {
                boolean writeRecord = evaluateCondition(condition, variables, this.reportService.getExpressionFactory(), workerTask, result);
                if (!writeRecord){
                    return;
                }
            } catch (Exception e) {
                LOGGER.error("Couldn't evaluate condition for report record " + record);
                return;
            }
        }

        variables.putAll(this.reportService.evaluateSubreportParameters(report.asPrismObject(), variables, workerTask, result));

        ColumnDataConverter<C> columnDataConverter =
                new ColumnDataConverter<>(record, report, variables, reportService, workerTask, result);

        ExportedReportDataRow dataRow = new ExportedReportDataRow(sequentialNumber);

        columns.forEach(column ->
                dataRow.addColumn(
                        columnDataConverter.convertColumn(column)));

        dataWriter.appendDataRow(dataRow);
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
