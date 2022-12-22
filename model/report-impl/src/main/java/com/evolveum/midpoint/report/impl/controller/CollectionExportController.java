/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.controller;

import static java.util.Objects.requireNonNull;

import static com.evolveum.midpoint.report.impl.controller.GenericSupport.evaluateCondition;
import static com.evolveum.midpoint.report.impl.controller.GenericSupport.getHeaderColumns;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.model.common.util.DefaultColumnUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.report.impl.ReportServiceImpl;
import com.evolveum.midpoint.report.impl.activity.ClassicCollectionReportExportActivityRun;
import com.evolveum.midpoint.schema.GetOperationOptions;
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
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

/**
 * Controls the process of exporting collection-based reports.
 * </p>
 * Currently, the only use of this class is to be a "bridge" between the world of the activity framework
 * (represented mainly by {@link ClassicCollectionReportExportActivityRun} class) and a set of cooperating
 * classes that implement the report export itself. However, in the future it may be used in other ways,
 * independently of the activity framework.
 * </p>
 * The process is driven by the activity execution that calls the following methods of this class:
 * </p>
 * 1. {@link #initialize(RunningTask, OperationResult)} that sets up the processes (in a particular worker task),
 * 2. {@link #beforeBucketExecution(int, OperationResult)} that starts processing of a given work bucket,
 * 3. {@link #handleDataRecord(int, Containerable, RunningTask, OperationResult)} that processes given prism object,
 * to be aggregated.
 *
 * @param <C> Type of records to be processed.
 */
@Experimental
public class CollectionExportController<C extends Containerable> implements ExportController<C> {

    private static final Trace LOGGER = TraceManager.getTrace(CollectionExportController.class);

    /**
     * Data source for the report. It is initialized from within this class. But the actual feeding
     * of data from the source to this class is ensured out of band, "behind the scenes". For example,
     * if activity framework is used, then it itself feeds the data to this controller.
     */
    @NotNull private final ReportDataSource<C> dataSource;

    /**
     * Definition of records that are processed. Initialized along with the data source.
     */
    protected PrismContainerDefinition<C> recordDefinition;

    /**
     * Data writer for the report. Produces e.g. CSV or HTML data.
     */
    @NotNull protected final ReportDataWriter<ExportedReportDataRow, ExportedReportHeaderRow> dataWriter;

    /** The report of which an export is being done. */
    @NotNull protected final ReportType report;

    /** The report parameters. */
    protected ReportParameterType reportParameters;

    /** Configuration of the report export, taken from the report. */
    @NotNull private final ObjectCollectionReportEngineConfigurationType configuration;

    /** Compiled final collection from more collections and archetypes related to object type. */
    @NotNull protected final CompiledObjectCollectionView compiledCollection;

    /**
     * Columns for the report.
     */
    protected List<GuiObjectColumnType> columns;

    /**
     * Values of report parameters.
     */
    protected VariablesMap parameters;

    // Useful Spring beans
    protected final ReportServiceImpl reportService;
    protected final PrismContext prismContext;
    protected final SchemaRegistry schemaRegistry;
    protected final SchemaService schemaService;
    protected final ModelInteractionService modelInteractionService;
    protected final RepositoryService repositoryService;
    protected final LocalizationService localizationService;

    public CollectionExportController(@NotNull ReportDataSource<C> dataSource,
            @NotNull ReportDataWriter<ExportedReportDataRow, ExportedReportHeaderRow> dataWriter,
            @NotNull ReportType report,
            @NotNull ReportServiceImpl reportService,
            @NotNull CompiledObjectCollectionView compiledCollection,
            ReportParameterType reportParameters) {

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
        this.reportParameters = reportParameters;
    }

    /**
     * Prepares the controller for accepting the source data:
     * initializes the data source, determines columns, etc.
     */
    public void initialize(@NotNull RunningTask task, @NotNull OperationResult result)
            throws CommonException {

        columns = MiscSchemaUtil.orderCustomColumns(compiledCollection.getColumns());

        initializeParameters(configuration.getParameter()); // must come before data source initialization
        initializeDataSource(task, result);
    }

    protected void initializeDataSource(RunningTask task, OperationResult result) throws CommonException {

        Class<Containerable> type = reportService.resolveTypeForReport(compiledCollection);
        Collection<SelectorOptions<GetOperationOptions>> defaultOptions = DefaultColumnUtils.createOption(type, schemaService);

        ModelInteractionService.SearchSpec<C> searchSpec = modelInteractionService.getSearchSpecificationFromCollection(
                compiledCollection, compiledCollection.getContainerType(), defaultOptions, parameters, task, result);

        recordDefinition = requireNonNull(
                schemaRegistry.findContainerDefinitionByCompileTimeClass(searchSpec.type),
                () -> "No definition for " + searchSpec.type + " found");

        dataSource.initialize(searchSpec.type, searchSpec.query, searchSpec.options);
    }

    protected void initializeParameters(List<SearchFilterParameterType> parametersDefinitions) {
        VariablesMap parameters = new VariablesMap();
        if (reportParameters != null) {
            PrismContainerValue<ReportParameterType> reportParamsValue = reportParameters.asPrismContainerValue();
            @NotNull Collection<Item<?, ?>> items = reportParamsValue.getItems();
            for (Item<?, ?> item : items) {
                String paramName = item.getPath().lastName().getLocalPart();
                Object value = null;
                if (!item.getRealValues().isEmpty()) {
                    value = item.getRealValue();
                }
                if (item.getRealValue() instanceof RawType) {
                    try {
                        ObjectReferenceType parsedRealValue = ((RawType) item.getRealValue()).getParsedRealValue(ObjectReferenceType.class);
                        parameters.put(paramName, new TypedValue<>(parsedRealValue, ObjectReferenceType.class));
                    } catch (SchemaException e) {
                        LOGGER.error("Couldn't parse ObjectReferenceType from raw type. " + item.getRealValue());
                    }
                } else {
                    if (item.getRealValue() != null) {
                        parameters.put(paramName, new TypedValue<>(value, item.getRealValue().getClass()));
                    }
                }
            }
        }

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
    public void handleDataRecord(int sequentialNumber, C record, RunningTask workerTask, OperationResult result) {

        VariablesMap variables = new VariablesMap();
        variables.putAll(parameters);
        variables.put(ExpressionConstants.VAR_OBJECT, record, recordDefinition);

        ExpressionType condition = configuration.getCondition();
        if (condition != null) {
            try {
                boolean writeRecord = evaluateCondition(condition, variables, this.reportService.getExpressionFactory(), workerTask, result);
                if (!writeRecord) {
                    return;
                }
            } catch (Exception e) {
                LOGGER.error("Couldn't evaluate condition for report record " + record);
                return;
            }
        }

        // handle subreport parameters that have asRow=true, we'll create "new virtual rows")
        List<SubreportParameterType> paramsAsRow = getSubreports(true);
        if (paramsAsRow.isEmpty()) {
            processSingleDataRecord(sequentialNumber, record, variables, workerTask, result);
            return;
        }

        boolean rowsCreated = handleSubreportParameters(sequentialNumber, record, paramsAsRow, variables, workerTask, result);

        if (!rowsCreated) {
            processSingleDataRecord(sequentialNumber, record, variables, workerTask, result);
        }
    }

    private <T> List<T> tail(List<T> list) {
        if (list == null) {
            return null;
        }

        if (list.size() == 1) {
            return Collections.emptyList();
        }

        return new ArrayList<>(list.subList(1, list.size()));
    }

    private boolean handleSubreportParameters(int sequentialNumber, C record, List<SubreportParameterType> params, VariablesMap variables, RunningTask task, OperationResult result) {
        SubreportParameterType param = params.stream().findFirst().orElse(null);
        if (param == null) {
            return false;
        }

        VariablesMap map = reportService.evaluateSubreportParameter(report.asPrismObject(), variables, param, task, result);
        if (map.isEmpty()) {
            return false;
        }

        TypedValue value = map.get(param.getName());
        if (value == null || value.getValue() == null) {
            return false;
        }

        Object obj = value.getValue();

        VariablesMap vars = new VariablesMap();
        vars.putAll(variables);

        List rows = new ArrayList();
        if (obj instanceof Collection) {
            rows.addAll((Collection) obj);
        } else {
            rows.add(obj);
        }

        if (rows.isEmpty()) {
            return false;
        }

        boolean rowsCreated = false;

        for (Object row : rows) {
            if (row instanceof Item) {
                row = ((Item<?, ?>) row).getRealValue();
            } else if (row instanceof PrismValue) {
                row = ((PrismValue) row).getRealValue();
            }

            vars.put(param.getName(), row, row.getClass());

            if (params.size() == 1) {
                vars.putAll(evaluateSimpleSubreportParameters(vars, task, result));

                convertAndWriteRow(sequentialNumber, record, vars, task, result);

                rowsCreated = rowsCreated | true;
            } else {
                rowsCreated = rowsCreated | handleSubreportParameters(sequentialNumber, record, tail(params), vars, task, result);
            }
        }

        return rowsCreated;
    }

    private VariablesMap evaluateSimpleSubreportParameters(VariablesMap variables, RunningTask task, OperationResult result) {
        VariablesMap resultMap = new VariablesMap();

        List<SubreportParameterType> params = getSubreports(false);
        for (SubreportParameterType param : params) {
            VariablesMap allVars = new VariablesMap();
            allVars.putAll(variables);
            allVars.putAll(resultMap);

            resultMap.putAll(reportService.evaluateSubreportParameter(report.asPrismObject(), allVars, param, task, result));
        }

        return resultMap;
    }

    private void convertAndWriteRow(int sequentialNumber, C record, VariablesMap variables, RunningTask workerTask, OperationResult result) {
        ColumnDataConverter<C> columnDataConverter =
                new ColumnDataConverter<>(record, report, variables, reportService, workerTask, result);

        ExportedReportDataRow dataRow = new ExportedReportDataRow(sequentialNumber);

        columns.forEach(column ->
                dataRow.addColumn(
                        columnDataConverter.convertColumn(column)));

        dataWriter.appendDataRow(dataRow);
    }

    private void processSingleDataRecord(int sequentialNumber, C record, VariablesMap variables, RunningTask workerTask, OperationResult result) {
        variables.putAll(this.reportService.evaluateSubreportParameters(report.asPrismObject(), variables, workerTask, result));

        convertAndWriteRow(sequentialNumber, record, variables, workerTask, result);
    }

    private List<SubreportParameterType> getSubreports(boolean asRow) {
        ObjectCollectionReportEngineConfigurationType collection = report.getObjectCollection();
        if (collection == null || collection.getSubreport().isEmpty()) {
            return new ArrayList();
        }

        Collection<SubreportParameterType> subreports = collection.getSubreport();
        List<SubreportParameterType> paramsAsRow = subreports.stream().filter(s -> asRow ? BooleanUtils.isTrue(s.isAsRow()) : BooleanUtils.isNotTrue(s.isAsRow())).collect(Collectors.toList());
        paramsAsRow.sort(Comparator.comparingInt(s -> ObjectUtils.defaultIfNull(s.getOrder(), Integer.MAX_VALUE)));

        return paramsAsRow;
    }
}
