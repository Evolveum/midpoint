/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report.impl.controller;

import static java.util.Objects.requireNonNull;

import static com.evolveum.midpoint.report.impl.controller.GenericSupport.getHeaderColumns;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.model.common.util.DefaultColumnUtils;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.report.impl.ReportServiceImpl;
import com.evolveum.midpoint.report.impl.activity.ClassicCollectionReportExportActivityRun;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

/**
 * Controls the process of exporting collection-based reports.
 *
 * Currently, the only use of this class is to be a "bridge" between the world of the activity framework
 * (represented mainly by {@link ClassicCollectionReportExportActivityRun} class) and a set of cooperating
 * classes that implement the report export itself. However, in the future it may be used in other ways,
 * independently of the activity framework.
 *
 * The process is driven by the activity execution that calls the following methods of this class:
 *
 * 1. {@link #initialize(RunningTask, OperationResult)} that sets up the processes (in a particular worker task),
 * 2. {@link #beforeBucketExecution(int, OperationResult)} that starts processing of a given work bucket,
 * 3. {@link #handleDataRecord(int, Object, RunningTask, OperationResult)} that processes given prism object,
 * to be aggregated.
 *
 * @param <R> Type of records to be processed.
 */
@Experimental
public class CollectionExportController<R> implements ExportController<R> {

    /**
     * Data source for the report. It is initialized from within this class. But the actual feeding
     * of data from the source to this class is ensured out of band, "behind the scenes". For example,
     * if activity framework is used, then it itself feeds the data to this controller.
     */
    @NotNull private final ReportDataSource<R> dataSource;

    /**
     * Definition of records that are processed. Initialized along with the data source.
     */
    ItemDefinition<?> recordDefinition;

    /**
     * Data writer for the report. Produces e.g. CSV or HTML data.
     */
    @NotNull final ReportDataWriter<ExportedReportDataRow, ExportedReportHeaderRow> dataWriter;

    /** The report of which an export is being done. */
    @NotNull protected final ReportType report;

    /** Values of report parameters for the current execution in the form of {@link ReportParameterType} bean. */
    private final ReportParameterType parametersValuesBean;

    /** The {@link #parametersValuesBean} converted into the form of {@link VariablesMap}, usable in scripts. */
    VariablesMap parametersValuesMap;

    /** Configuration of the report export, taken from the report. */
    @NotNull private final ObjectCollectionReportEngineConfigurationType configuration;

    /** Compiled final collection from more collections and archetypes related to object type. */
    @NotNull final CompiledObjectCollectionView compiledCollection;

    /** Definitions of columns for the report. */
    protected List<GuiObjectColumnType> columns;

    // Useful Spring beans
    protected final ReportServiceImpl reportService;
    protected final PrismContext prismContext;
    private final SchemaRegistry schemaRegistry;
    protected final SchemaService schemaService;
    protected final ModelInteractionService modelInteractionService;
    protected final RepositoryService repositoryService;
    protected final LocalizationService localizationService;

    public CollectionExportController(@NotNull ReportDataSource<R> dataSource,
            @NotNull ReportDataWriter<ExportedReportDataRow, ExportedReportHeaderRow> dataWriter,
            @NotNull ReportType report,
            @NotNull ReportServiceImpl reportService,
            @NotNull CompiledObjectCollectionView compiledCollection,
            ReportParameterType parametersValuesBean) {

        this.dataSource = dataSource;
        this.dataWriter = dataWriter;
        this.report = report;
        this.configuration = report.getObjectCollection();
        this.reportService = reportService;
        this.prismContext = reportService.getPrismContext();
        this.schemaRegistry = reportService.getPrismContext().getSchemaRegistry();
        this.schemaService = reportService.getSchemaService();
        this.modelInteractionService = reportService.getModelInteractionService();
        this.repositoryService = reportService.getRepositoryService();
        this.localizationService = reportService.getLocalizationService();
        this.compiledCollection = compiledCollection;
        this.parametersValuesBean = parametersValuesBean;
    }

    /**
     * Prepares the controller for accepting the source data:
     * initializes the data source, determines columns, etc.
     */
    public void initialize(@NotNull RunningTask task, @NotNull OperationResult result)
            throws CommonException {

        columns = MiscSchemaUtil.orderCustomColumns(compiledCollection.getColumns());

        initializeParametersValuesMap(); // must come before data source initialization
        initializeDataSource(task, result);
    }

    void initializeDataSource(RunningTask task, OperationResult result) throws CommonException {

        Class<?> type = reportService.resolveTypeForReport(compiledCollection);
        Collection<SelectorOptions<GetOperationOptions>> defaultOptions = DefaultColumnUtils.createOption(type, schemaService);

        ModelInteractionService.SearchSpec<R> searchSpec = modelInteractionService.getSearchSpecificationFromCollection(
                compiledCollection, compiledCollection.getContainerType(), defaultOptions, parametersValuesMap, task, result);

        recordDefinition = requireNonNull(
                Referencable.class.isAssignableFrom(searchSpec.type)
                        ? schemaRegistry.findReferenceDefinitionByElementName(SchemaConstantsGenerated.C_OBJECT_REF)
                        : schemaRegistry.findItemDefinitionByCompileTimeClass(searchSpec.type, ItemDefinition.class),
                () -> "No definition for " + searchSpec.type + " found");

        dataSource.initialize(searchSpec.type, searchSpec.query, searchSpec.options);
    }

    private void initializeParametersValuesMap() throws SchemaException {
        VariablesMap parameterValuesMap = new VariablesMap();
        if (parametersValuesBean != null) {
            //noinspection unchecked
            @NotNull Collection<Item<?, ?>> paramItems = parametersValuesBean.asPrismContainerValue().getItems();
            for (Item<?, ?> paramItem : paramItems) {
                if (paramItem.hasAnyValue()) {
                    String paramName = paramItem.getPath().lastName().getLocalPart();
                    parameterValuesMap.put(paramName, getRealValuesAsTypedValue(paramItem));
                }
            }
        }

        initializeMissingParametersToNull(parameterValuesMap);
        this.parametersValuesMap = parameterValuesMap;
    }

    /**
     * FIXME This functionality should be provided in some common place.
     *  (But beware of the specifics like .get(0) and ObjectReferenceType.class.)
     */
    private TypedValue<?> getRealValuesAsTypedValue(Item<?, ?> item) throws SchemaException {
        ItemDefinition<?> itemDef = item.getDefinition();
        Class<?> itemClass = itemDef != null ? itemDef.getTypeClass() : null;

        List<Object> parsedRealValues = new ArrayList<>();
        for (Object realValue : item.getRealValues()) {
            if (realValue instanceof RawType) {
                // Originally, here it was ObjectReferenceType as hardcoded value. So we keep it here just as the default.
                Class<?> classToParse = Objects.requireNonNullElse(itemClass, ObjectReferenceType.class);
                parsedRealValues.add(
                        ((RawType) realValue).getParsedRealValue(classToParse));
            } else {
                parsedRealValues.add(realValue);
            }
        }
        assert !parsedRealValues.isEmpty();
        Class<?> valueClass = itemClass != null ? itemClass : parsedRealValues.get(0).getClass();
        if (parsedRealValues.size() == 1) {
            return new TypedValue<>(parsedRealValues.get(0), itemDef, valueClass);
        } else {
            return new TypedValue<>(parsedRealValues, itemDef, valueClass);
        }
    }

    private void initializeMissingParametersToNull(VariablesMap parameters) {
        for (SearchFilterParameterType parameterDefinition : configuration.getParameter()) {
            if (!parameters.containsKey(parameterDefinition.getName())) {
                Class<?> clazz = schemaRegistry.determineClassForType(parameterDefinition.getType());
                parameters.put(parameterDefinition.getName(), null, clazz);
            }
        }
    }

    /**
     * Called before bucket of data is executed, i.e. before data start flowing to
     * {@link #handleDataRecord(int, Object, RunningTask, OperationResult)} method.
     * </p>
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
    public void handleDataRecord(int sequentialNumber, R record, RunningTask workerTask, OperationResult result)
            throws ConfigurationException {

        VariablesMap variables = new VariablesMap();
        variables.putAll(parametersValuesMap);
        variables.put(ExpressionConstants.VAR_OBJECT, record, recordDefinition);

        new DataRecordEvaluation<>(
                sequentialNumber, record, variables, workerTask, report, configuration, this::convertAndWriteRow)
                .evaluate(result);
    }

    private void convertAndWriteRow(int sequentialNumber, R record,
            VariablesMap variables, RunningTask workerTask, OperationResult result) {
        ColumnDataConverter<R> columnDataConverter =
                new ColumnDataConverter<>(record, report, variables, reportService, workerTask, result);

        ExportedReportDataRow dataRow = new ExportedReportDataRow(sequentialNumber);

        columns.forEach(column ->
                dataRow.addColumn(
                        columnDataConverter.convertColumn(column)));

        dataWriter.appendDataRow(dataRow);
    }

    interface RowEmitter<R> {
        void emit(int sequentialNumber, R record,
                VariablesMap variables, RunningTask workerTask, OperationResult result);
    }
}
