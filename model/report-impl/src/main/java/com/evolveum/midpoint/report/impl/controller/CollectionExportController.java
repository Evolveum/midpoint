/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report.impl.controller;

import static com.evolveum.midpoint.report.impl.controller.GenericSupport.evaluateCondition;
import static com.evolveum.midpoint.util.MiscUtil.configNonNull;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.SubreportUseType.*;

import static java.util.Objects.requireNonNull;

import static com.evolveum.midpoint.report.impl.controller.GenericSupport.getHeaderColumns;

import java.util.*;
import java.util.stream.Collectors;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.report.impl.ReportBeans;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.model.common.util.DefaultColumnUtils;
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

import org.jetbrains.annotations.Nullable;

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

    private static final Trace LOGGER = TraceManager.getTrace(CollectionExportController.class);

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

        new DataRecordEvaluation(
                sequentialNumber, record, variables, workerTask)
                .evaluate(result);
    }

    /** Called from within {@link DataRecordEvaluation} to write computed report rows. */
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

    /**
     * Evaluates a single data record - gradually evaluating subreports in embedded or joined mode.
     */
    class DataRecordEvaluation {

        private final int sequentialNumber;
        @NotNull private final R record;
        @NotNull private final VariablesMap variables;
        @NotNull private final RunningTask workerTask;
        @NotNull private final List<SubreportParameterType> subReportDefinitionsSorted;

        DataRecordEvaluation(
                int sequentialNumber,
                @NotNull R record,
                @NotNull VariablesMap variables,
                @NotNull RunningTask workerTask) {

            this.sequentialNumber = sequentialNumber;
            this.record = record;
            this.variables = variables;
            this.workerTask = workerTask;
            this.subReportDefinitionsSorted =
                    configuration.getSubreport().stream()
                            .sorted(Comparator.comparingInt(s -> ObjectUtils.defaultIfNull(s.getOrder(), Integer.MAX_VALUE)))
                            .collect(Collectors.toList());
        }

        public void evaluate(OperationResult result) throws ConfigurationException {
            if (conditionHolds(result)) {
                evaluateFromSubreport(0, result);
            } else {
                LOGGER.trace("Condition excludes processing of #{}:{}", sequentialNumber, record);
            }
        }

        private void evaluateFromSubreport(int index, OperationResult result) throws ConfigurationException {
            if (index == subReportDefinitionsSorted.size()) {
                convertAndWriteRow(sequentialNumber, record, variables, workerTask, result);
                return;
            }
            SubreportParameterType subreportDef = subReportDefinitionsSorted.get(index);
            String subReportName = configNonNull(subreportDef.getName(), () -> "Unnamed subreport definition: " + subreportDef);
            @Nullable TypedValue<?> subReportResultTyped =
                    getSingleTypedValue(
                            ReportBeans.get().reportService.evaluateSubreport(
                                    report.asPrismObject(), variables, subreportDef, workerTask, result));
            SubreportUseType use = Objects.requireNonNullElse(subreportDef.getUse(), EMBEDDED);
            if (use == EMBEDDED) {
                variables.put(subReportName, subReportResultTyped);
                evaluateFromSubreport(index + 1, result);
                variables.remove(subReportName);
            } else {
                stateCheck(
                        use == INNER_JOIN || use == LEFT_JOIN,
                        "Unsupported use value for %s: %s", subReportName, use);
                List<?> subReportValues = getAsList(subReportResultTyped);
                for (Object subReportValue : subReportValues) {
                    variables.put(
                            subReportName,
                            TypedValue.of(getRealValue(subReportValue), Object.class));
                    evaluateFromSubreport(index + 1, result);
                    variables.remove(subReportName);
                }
                if (subReportValues.isEmpty() && use == LEFT_JOIN) {
                    // Null is the best alternative to represent "no element" generated from the joined subreport.
                    variables.put(subReportName, null, Object.class);
                    evaluateFromSubreport(index + 1, result);
                    variables.remove(subReportName);
                }
            }
        }

        // Quite a hackery, for now. Should be reconsidered some day.
        private Object getRealValue(Object value) {
            if (value instanceof Item) {
                return ((Item<?, ?>) value).getRealValue();
            } else if (value instanceof PrismValue) {
                return ((PrismValue) value).getRealValue();
            } else {
                return value;
            }
        }

        private @Nullable TypedValue<?> getSingleTypedValue(@NotNull VariablesMap map) {
            if (map.isEmpty()) {
                return null;
            }
            if (map.size() > 1) {
                throw new IllegalStateException("Expected zero or single entry, got more: " + map);
            }
            return map.values().iterator().next();
        }

        private @NotNull List<?> getAsList(@Nullable TypedValue<?> typedValue) {
            Object value = typedValue != null ? typedValue.getValue() : null;
            if (value instanceof Collection) {
                return List.copyOf((Collection<?>) value);
            } else if (value != null) {
                return List.of(value);
            } else {
                return List.of();
            }
        }

        private boolean conditionHolds(OperationResult result) {
            ExpressionType condition = configuration.getCondition();
            if (condition == null) {
                return true;
            }
            try {
                return evaluateCondition(condition, variables, ReportBeans.get().expressionFactory, workerTask, result);
            } catch (Exception e) {
                LoggingUtils.logException(LOGGER, "Couldn't evaluate condition for report record {} in {}", e, record, report);
                return false;
            }
        }
    }
}
