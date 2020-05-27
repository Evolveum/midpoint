/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report.impl.controller.export;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.model.api.interaction.DashboardWidget;
import com.evolveum.midpoint.model.api.util.DashboardUtils;
import com.evolveum.midpoint.model.api.util.DefaultColumnUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.report.impl.ReportServiceImpl;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.*;

/**
 * @author skublik
 */

public class CsvExporterController extends ExportController {

    private static final Trace LOGGER = TraceManager.getTrace(CsvExporterController.class);

    public CsvExporterController(ExportConfigurationType exportConfiguration, ReportServiceImpl reportService) {
        super(exportConfiguration, reportService);
    }

    @Override
    public byte[] processDashboard(DashboardReportEngineConfigurationType dashboardConfig, Task task, OperationResult result) throws Exception {
        ObjectReferenceType ref = dashboardConfig.getDashboardRef();
        Class<ObjectType> type = getReportService().getPrismContext().getSchemaRegistry().determineClassForType(ref.getType());
        DashboardType dashboard = (DashboardType) getReportService().getModelService()
                .getObject(type, ref.getOid(), null, task, result)
                .asObjectable();

        CSVFormat csvFormat = createCsvFormat();
        if (Boolean.TRUE.equals(isCreateHeader())) {
            String[] arrayHeader = new String[getHeadsOfWidget().size()];
            arrayHeader = getHeadsOfWidget().toArray(arrayHeader);
            csvFormat = csvFormat.withHeader(arrayHeader)
                    .withSkipHeaderRecord(false);
        } else {
            csvFormat = csvFormat.withSkipHeaderRecord(true);
        }

        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            CSVPrinter printer = new CSVPrinter(new OutputStreamWriter(output, getEncoding()), csvFormat);

            long i = 0;
            task.setExpectedTotal((long) dashboard.getWidget().size());
            recordProgress(task, i, result, LOGGER);

            for (DashboardWidgetType widget : dashboard.getWidget()) {
                DashboardWidget widgetData = getReportService().getDashboardService().createWidgetData(widget, task, result);
                printer.printRecord(createTableRow(widgetData));
            }

            printer.flush();

            return output.toByteArray();
        } catch (IOException e) {
            LOGGER.error("Couldn't create CSVPrinter", e);
        }

        return null;
    }

    private Object[] createTableRow(DashboardWidget data) {
        List<String> items = new ArrayList<>();
        getHeadsOfWidget().forEach(header ->
        {
            if (header.equals(LABEL_COLUMN)) {
                items.add(data.getLabel());
            }
            if (header.equals(NUMBER_COLUMN)) {
                items.add(data.getNumberMessage());
            }
            if (header.equals(STATUS_COLUMN)) {
                String color = "";
                if (data.getDisplay() != null && StringUtils.isNoneBlank(data.getDisplay().getColor())) {
                    color = data.getDisplay().getColor();
                }
                items.add(color);
            }
        });
        return items.toArray();
    }

    @Override
    public byte[] processCollection(String nameOfReport, ObjectCollectionReportEngineConfigurationType collectionConfig, Task task, OperationResult result) throws Exception {
        CollectionRefSpecificationType collectionRefSpecification = collectionConfig.getCollection();
        ObjectReferenceType ref = collectionRefSpecification.getCollectionRef();
        ObjectCollectionType collection = null;
        if (ref != null) {
            Class<ObjectType> type = getReportService().getPrismContext().getSchemaRegistry().determineClassForType(ref.getType());
            collection = (ObjectCollectionType) getReportService().getModelService()
                    .getObject(type, ref.getOid(), null, task, result)
                    .asObjectable();
        }

        CompiledObjectCollectionView compiledCollection = new CompiledObjectCollectionView();
        if (!Boolean.TRUE.equals(collectionConfig.isUseOnlyReportView())) {
            if (collection != null) {
                getReportService().getModelInteractionService().applyView(compiledCollection, collection.getDefaultView());
            } else if (collectionRefSpecification.getBaseCollectionRef() != null
                    && collectionRefSpecification.getBaseCollectionRef().getCollectionRef() != null) {
                ObjectCollectionType baseCollection = (ObjectCollectionType) getObjectFromReference(collectionRefSpecification.getBaseCollectionRef().getCollectionRef()).asObjectable();
                getReportService().getModelInteractionService().applyView(compiledCollection, baseCollection.getDefaultView());
            }
        }

        GuiObjectListViewType reportView = collectionConfig.getView();
        if (reportView != null) {
            getReportService().getModelInteractionService().applyView(compiledCollection, reportView);
        }

        byte[] csvFile;
        boolean isAuditCollection = collection != null && collection.getAuditSearch() != null ? true : false;
        if (!isAuditCollection) {
            csvFile = createTableBoxForObjectView(collectionRefSpecification, compiledCollection, task, result);
        } else {
            csvFile = createTableBoxForAuditView(collection, compiledCollection, task, result);
        }


        return csvFile;
    }

    private byte[] createTableBoxForAuditView(ObjectCollectionType collection, CompiledObjectCollectionView compiledCollection, Task task, OperationResult result) {
        Map<String, Object> parameters = new HashMap<>();
        String query = DashboardUtils
                .getQueryForListRecords(DashboardUtils.createQuery(collection, parameters, false, getReportService().getClock()));
        List<AuditEventRecord> auditRecords = getReportService().getAuditService().listRecords(query, parameters, result);
        if (auditRecords == null) {
            auditRecords = new ArrayList<>();
        }

        if (compiledCollection.getColumns().isEmpty()) {
            getReportService().getModelInteractionService().applyView(compiledCollection, DefaultColumnUtils.getDefaultAuditEventsView());
        }
        List<String> headers = new ArrayList<>();
        List<List<String>> records = new ArrayList<>();

        List<GuiObjectColumnType> columns = MiscSchemaUtil.orderCustomColumns(compiledCollection.getColumns());

        columns.forEach(column -> {
            Validate.notNull(column.getName(), "Name of column is null");

            DisplayType columnDisplay = column.getDisplay();
            String label;
            if(columnDisplay != null && columnDisplay.getLabel() != null) {
                label = getMessage(columnDisplay.getLabel().getOrig());
            } else {
                label = column.getName();
            }
            headers.add(label);

        });

        int i = 0;
        task.setExpectedTotal((long) auditRecords.size());
        recordProgress(task, i, result, LOGGER);

        for (AuditEventRecord auditRecord : auditRecords) {
            List<String> items = new ArrayList<>();
            columns.forEach(column -> {
                ExpressionType expression = column.getExpression();
                ItemPath path = column.getPath() == null ? null : column.getPath().getItemPath();
                items.add(getStringValueByAuditColumn(auditRecord, path, expression, task, result));
            });
            records.add(items);
            i++;
            recordProgress(task, i, result, LOGGER);
        }

        CSVFormat csvFormat = createCsvFormat();
        if (Boolean.TRUE.equals(isCreateHeader())) {
            String[] arrayHeader = new String[headers.size()];
            arrayHeader = headers.toArray(arrayHeader);
            csvFormat = csvFormat.withHeader(arrayHeader)
                    .withSkipHeaderRecord(false);
        } else {
            csvFormat = csvFormat.withSkipHeaderRecord(true);
        }
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            CSVPrinter printer = new CSVPrinter(new OutputStreamWriter(output, getEncoding()), csvFormat);
            for (List<String> record : records) {
                printer.printRecord(record.toArray());
            }
            printer.flush();

            return output.toByteArray();
        } catch (IOException e) {
            LOGGER.error("Couldn't create CSVPrinter", e);
        }
        return null;
    }

    private byte[] createTableBoxForObjectView(CollectionRefSpecificationType collection, CompiledObjectCollectionView compiledCollection, Task task, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {

        Class<ObjectType> type = resolveType(collection, compiledCollection);
        Collection<SelectorOptions<GetOperationOptions>> options = DefaultColumnUtils.createOption(type, getReportService().getSchemaHelper());
        List<PrismObject<ObjectType>> values = getReportService().getDashboardService().searchObjectFromCollection(collection, compiledCollection.getObjectType(), options, task, result);
        if (values.isEmpty()) {
            values = new ArrayList<>();
        }


        PrismObjectDefinition<ObjectType> def = getReportService().getPrismContext().getSchemaRegistry().findItemDefinitionsByCompileTimeClass(type, PrismObjectDefinition.class).get(0);

        if (compiledCollection.getColumns().isEmpty()) {
            getReportService().getModelInteractionService().applyView(compiledCollection, DefaultColumnUtils.getDefaultView(type));
        }
        List<String> headers = new ArrayList<>();
        List<List<String>> records = new ArrayList<>();

        List<GuiObjectColumnType> columns = MiscSchemaUtil.orderCustomColumns(compiledCollection.getColumns());

        columns.forEach(column -> {
            Validate.notNull(column.getName(), "Name of column is null");
            ItemPath path = column.getPath() == null ? null : column.getPath().getItemPath();

            DisplayType columnDisplay = column.getDisplay();
            String label;
            if(columnDisplay != null && columnDisplay.getLabel() != null) {
                label = getMessage(columnDisplay.getLabel().getOrig());
            } else  {

                label = getColumnLabel(column.getName(), def, path);
            }
            headers.add(label);

        });

        int i = 0;
        task.setExpectedTotal((long) values.size());
        recordProgress(task, i, result, LOGGER);

        for (PrismObject<ObjectType> value : values) {

            List<String> items = new ArrayList<>();
            columns.forEach(column -> {
                ItemPath path = column.getPath() == null ? null : column.getPath().getItemPath();
                ExpressionType expression = column.getExpression();
                items.add(getRealValueAsString(column, value, path, expression, task, result));
            });
            records.add(items);
            i++;
            recordProgress(task, i, result, LOGGER);
        }

        CSVFormat csvFormat = createCsvFormat();
        if (Boolean.TRUE.equals(isCreateHeader())) {
            String[] arrayHeader = new String[headers.size()];
            arrayHeader = headers.toArray(arrayHeader);
            csvFormat = csvFormat.withHeader(arrayHeader)
                            .withSkipHeaderRecord(false);
        } else {
            csvFormat = csvFormat.withSkipHeaderRecord(true);
        }

        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            CSVPrinter printer = new CSVPrinter(new OutputStreamWriter(output, getEncoding()), csvFormat);
            for (List<String> record : records) {
                printer.printRecord(record.toArray());
            }
            printer.flush();

            return output.toByteArray();
        } catch (IOException e) {
            LOGGER.error("Couldn't create CSVPrinter", e);
        }
        return null;
    }

    @Override
    protected String getRealValueAsString(GuiObjectColumnType column, PrismObject<ObjectType> object, ItemPath itemPath, ExpressionType expression, Task task, OperationResult result) {
        String value = super.getRealValueAsString(column, object, itemPath, expression, task, result);
        value = removeNewLine(value);
        return value;
    }

    protected String getStringValueByAuditColumn(AuditEventRecord record, ItemPath path,
            ExpressionType expression, Task task, OperationResult result) {
        String value = super.getStringValueByAuditColumn(record, path, expression, task, result);
        value = removeNewLine(value);
        return value;
    }

    private String removeNewLine(String value) {
        if (value.contains("\n\t")) {
            value = value.replace("\n\t", " ");
        }
        if (value.contains("\n")) {
            value = value.replace("\n", " ");
        }
        return value;
    }

    public CSVFormat createCsvFormat() {
        return CSVFormat.newFormat(toCharacter(getFieldDelimiter()))
                .withAllowDuplicateHeaderNames(true)
                .withAllowMissingColumnNames(false)
                .withEscape(toCharacter(getEscape()))
                .withIgnoreEmptyLines(true)
                .withIgnoreHeaderCase(false)
                .withIgnoreSurroundingSpaces(true)
                .withQuote(toCharacter(getQuote()))
                .withQuoteMode(QuoteMode.valueOf(getQuoteMode().name()))
                .withRecordSeparator(getRecordSeparator())
                .withTrailingDelimiter(isTrailingDelimiter())
                .withTrim(isTrim());
    }

    private String getEncoding() {
        return getCsvConfiguration().getEncoding() == null ? "utf-8" : getCsvConfiguration().getEncoding();
    }

    private boolean isCreateHeader() {
        return getCsvConfiguration().isCreateHeader() == null ? true : getCsvConfiguration().isCreateHeader();
    }

    private boolean isTrim() {
        return getCsvConfiguration().isTrim() == null ? false : getCsvConfiguration().isTrim();
    }

    private boolean isTrailingDelimiter() {
        return getCsvConfiguration().isTrailingDelimiter() == null ? false : getCsvConfiguration().isTrailingDelimiter();
    }

    private String getRecordSeparator() {
        return getCsvConfiguration().getRecordSeparator() == null ? "\r\n" : getCsvConfiguration().getRecordSeparator();
    }

    private QuoteModeType getQuoteMode() {
        return getCsvConfiguration().getQuoteMode() == null ? QuoteModeType.NON_NUMERIC : getCsvConfiguration().getQuoteMode();
    }

    private String getQuote() {
        return getCsvConfiguration().getQuote() == null ? "\"" : getCsvConfiguration().getQuote();
    }

    private String getEscape() {
        return getCsvConfiguration().getEscape() == null ? "\\" : getCsvConfiguration().getEscape();
    }

    private String getFieldDelimiter() {
        return getCsvConfiguration().getFieldDelimiter() == null ? ";" : getCsvConfiguration().getFieldDelimiter();
    }

    private CsvExportType getCsvConfiguration() {
        if (getExportConfiguration().getCsv() == null) {
            return new CsvExportType();
        }
        return getExportConfiguration().getCsv();
    }

    public static Character toCharacter(String value) {
        if (value == null) {
            return null;
        }

        if (value.length() != 1) {
            throw new IllegalArgumentException("Can't cast to character of " + value + ", illegal string size: "
                    + value.length() + ", should be 1");
        }

        return value.charAt(0);
    }


    @Override
    public String getTypeSuffix() {
        return ".csv";
    }

    @Override
    public String getType() {
        return "CSV";
    }

    @Override
    protected void appendMultivalueDelimiter(StringBuilder sb) {
        String delimiter = getCsvConfiguration().getMultivalueDelimiter() == null ? "," : getCsvConfiguration().getMultivalueDelimiter();
        sb.append(delimiter);
    }
}
