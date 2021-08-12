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
import com.evolveum.midpoint.report.impl.activity.ClassicDashboardReportExportActivityExecutionSpecifics;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.CommonException;
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
 * Controls the process of exporting dashboard-based reports.
 *
 * Currently the only use of this class is to be a "bridge" between the world of the activity framework
 * (represented mainly by {@link ClassicDashboardReportExportActivityExecutionSpecifics} class) and a set of cooperating
 * classes that implement the report export itself. However, in the future it may be used in other ways,
 * independently of the activity framework.
 *
 * The process is driven by the activity execution that calls the following methods of this class:
 *
 * 1. {@link #initialize(RunningTask, OperationResult)} that sets up the processes (in a particular worker task),
 * 2. {@link #beforeBucketExecution(int, OperationResult)} that starts processing of a given work bucket,
 * 3. {@link #handleDataRecord(int, Containerable, RunningTask, OperationResult)} that processes given prism object,
 * to be aggregated.
 *
 * @param <C> Type of records to be processed.
 */
@Experimental
public class DashboardBasedExportController<C extends Containerable> extends CollectionBasedExportController<C>{

    private static final Trace LOGGER = TraceManager.getTrace(DashboardBasedExportController.class);

    /**
     * Identifier of widget.
     */
    private final String widgetIdentifier;

    public DashboardBasedExportController(@NotNull ReportDataSource<C> dataSource,
                                          @NotNull ReportDataWriter dataWriter,
                                          @NotNull ReportType report,
                                          @NotNull ReportServiceImpl reportService,
                                          @NotNull CompiledObjectCollectionView compiledCollection,
                                          @NotNull String widgetIdentifier) {
        super(dataSource, dataWriter, report, reportService, compiledCollection);
        this.widgetIdentifier = widgetIdentifier;
    }

    /**
     * Prepares the controller for accepting the source data:
     * initializes the data source, determines columns, etc.
     */
    public void initialize(@NotNull RunningTask task, @NotNull OperationResult result)
            throws CommonException {

        columns = MiscSchemaUtil.orderCustomColumns(compiledCollection.getColumns());

        initializeDataSource(task, result);
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

        dataWriter.setHeaderRow(ExportedDashboardReportHeaderRow.fromColumns(headerColumns, widgetIdentifier));
    }

    /**
     * BEWARE: Can be called from multiple threads at once.
     * The resulting rows should be sorted on sequentialNumber.
     */
    public void handleDataRecord(int sequentialNumber, C record, RunningTask workerTask, OperationResult result) {

        VariablesMap variables = new VariablesMap();
        variables.put(ExpressionConstants.VAR_OBJECT, record, recordDefinition);

//        ExpressionType condition = configuration.getCondition();
//        if (condition != null) {
//            try {
//                boolean writeRecord = evaluateCondition(condition, variables, this.reportService.getExpressionFactory(), workerTask, result);
//                if (!writeRecord){
//                    return;
//                }
//            } catch (Exception e) {
//                LOGGER.error("Couldn't evaluate condition for report record " + record);
//                return;
//            }
//        }

        variables.putAll(this.reportService.evaluateSubreportParameters(report.asPrismObject(), variables, workerTask, result));

        ColumnDataConverter<C> columnDataConverter =
                new ColumnDataConverter<>(record, report, variables, reportService, workerTask, result);

        ExportedDashboardReportDataRow dataRow = new ExportedDashboardReportDataRow(sequentialNumber, widgetIdentifier);

        columns.forEach(column ->
                dataRow.addColumn(
                        columnDataConverter.convertColumn(column)));

        dataWriter.appendDataRow(dataRow);
    }
}
