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
import com.evolveum.midpoint.model.common.util.DefaultColumnUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.report.impl.ReportServiceImpl;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.csv.*;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * @author skublik
 */

public class CsvController extends FileFormatController {

    private static final Trace LOGGER = TraceManager.getTrace(CsvController.class);

    public CsvController(FileFormatConfigurationType fileFormatConfiguration, ReportServiceImpl reportService) {
        super(fileFormatConfiguration, reportService);
    }

    @Override
    public byte[] processDashboard(DashboardReportEngineConfigurationType dashboardConfig, Task task, OperationResult result) throws Exception {
        ObjectReferenceType ref = dashboardConfig.getDashboardRef();
        Class<ObjectType> type = getReportService().getPrismContext().getSchemaRegistry().determineClassForType(ref.getType());
        DashboardType dashboard = (DashboardType) getReportService().getModelService()
                .getObject(type, ref.getOid(), null, task, result)
                .asObjectable();

        CSVFormat csvFormat = createCsvFormat();
        if (Boolean.TRUE.equals(isHeader())) {
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

        CompiledObjectCollectionView compiledCollection = createCompiledView(collectionConfig, collection);

        byte[] csvFile;
        boolean isAuditCollection = collection != null && collection.getAuditSearch() != null;
        if (!isAuditCollection) {
            csvFile = createTableBoxForObjectView(collectionRefSpecification, compiledCollection,
                    collectionConfig.getCondition(), task, result);
        } else {
            csvFile = createTableForAuditView(collectionRefSpecification, compiledCollection, collectionConfig.getCondition(), task, result);
        }


        return csvFile;
    }

    private CompiledObjectCollectionView createCompiledView(ObjectCollectionReportEngineConfigurationType collectionConfig, Task task, OperationResult result) throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        CollectionRefSpecificationType collectionRefSpecification = collectionConfig.getCollection();
        ObjectReferenceType ref = null;
        if (collectionRefSpecification != null) {
            ref = collectionRefSpecification.getCollectionRef();
        }
        ObjectCollectionType collection = null;
        if (ref != null) {
            Class<ObjectType> type = getReportService().getPrismContext().getSchemaRegistry().determineClassForType(ref.getType());
            collection = (ObjectCollectionType) getReportService().getModelService()
                    .getObject(type, ref.getOid(), null, task, result)
                    .asObjectable();
        }
        CompiledObjectCollectionView compiledCollection = createCompiledView(collectionConfig, collection);
        if (compiledCollection.getColumns().isEmpty()) {
            Class<ObjectType> type = resolveType(collectionRefSpecification, compiledCollection);
            getReportService().getModelInteractionService().applyView(compiledCollection, DefaultColumnUtils.getDefaultView(type));
        }
        return compiledCollection;
    }

    private CompiledObjectCollectionView createCompiledView (ObjectCollectionReportEngineConfigurationType collectionConfig, ObjectCollectionType collection){
        CollectionRefSpecificationType collectionRefSpecification = collectionConfig.getCollection();
        CompiledObjectCollectionView compiledCollection = new CompiledObjectCollectionView();
        if (!Boolean.TRUE.equals(collectionConfig.isUseOnlyReportView())) {
            if (collection != null) {
                getReportService().getModelInteractionService().applyView(compiledCollection, collection.getDefaultView());
            } else if (collectionRefSpecification != null && collectionRefSpecification.getBaseCollectionRef() != null
                    && collectionRefSpecification.getBaseCollectionRef().getCollectionRef() != null) {
                ObjectCollectionType baseCollection = (ObjectCollectionType) getObjectFromReference(collectionRefSpecification.getBaseCollectionRef().getCollectionRef()).asObjectable();
                getReportService().getModelInteractionService().applyView(compiledCollection, baseCollection.getDefaultView());
            }
        }

        GuiObjectListViewType reportView = collectionConfig.getView();
        if (reportView != null) {
            getReportService().getModelInteractionService().applyView(compiledCollection, reportView);
        }
        return compiledCollection;
    }

    private byte[] createTableForAuditView(CollectionRefSpecificationType collectionRef, CompiledObjectCollectionView compiledCollection,
            ExpressionType condition, Task task, OperationResult result) throws CommunicationException, ObjectNotFoundException, SchemaException,
            SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        List<AuditEventRecord> auditRecords = getReportService().getDashboardService().searchObjectFromCollection(collectionRef, condition, task, result);

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
                ExpressionType expression = column.getExport() != null ? column.getExport().getExpression() : null;
                ItemPath path = column.getPath() == null ? null : column.getPath().getItemPath();
                items.add(getStringValueByAuditColumn(auditRecord, path, expression, task, result));
            });
            records.add(items);
            i++;
            recordProgress(task, i, result, LOGGER);
        }

        CSVFormat csvFormat = createCsvFormat();
        if (Boolean.TRUE.equals(isHeader())) {
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

    private byte[] createTableBoxForObjectView(CollectionRefSpecificationType collection, CompiledObjectCollectionView compiledCollection, ExpressionType condition, Task task, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {

        Class<ObjectType> type = resolveType(collection, compiledCollection);
        Collection<SelectorOptions<GetOperationOptions>> options = DefaultColumnUtils.createOption(type, getReportService().getSchemaHelper());
        List<PrismObject<ObjectType>> values = getReportService().getDashboardService().searchObjectFromCollection(collection, compiledCollection.getObjectType(), options, condition, task, result);
        if (values.isEmpty()) {
            values = new ArrayList<>();
        }


        PrismObjectDefinition<ObjectType> def = getReportService().getPrismContext().getSchemaRegistry().findItemDefinitionByCompileTimeClass(type, PrismObjectDefinition.class);

        if (compiledCollection.getColumns().isEmpty()) {
            getReportService().getModelInteractionService().applyView(compiledCollection, DefaultColumnUtils.getDefaultView(type));
        }
        List<String> headers = new ArrayList<>();
        List<List<String>> records = new ArrayList<>();

        List<GuiObjectColumnType> columns = MiscSchemaUtil.orderCustomColumns(compiledCollection.getColumns());

        columns.forEach(column -> {
            Validate.notNull(column.getName(), "Name of column is null");
            String label = getColumnLabel(column, def);
            headers.add(label);
        });

        int i = 0;
        task.setExpectedTotal((long) values.size());
        recordProgress(task, i, result, LOGGER);

        for (PrismObject<ObjectType> value : values) {

            List<String> items = new ArrayList<>();
            columns.forEach(column -> {
                ItemPath path = column.getPath() == null ? null : column.getPath().getItemPath();
                ExpressionType expression = column.getExport() != null ? column.getExport().getExpression() : null;
                items.add(getRealValueAsString(column, value, path, expression, task, result));
            });
            records.add(items);
            i++;
            recordProgress(task, i, result, LOGGER);
        }

        CSVFormat csvFormat = createCsvFormat();
        if (Boolean.TRUE.equals(isHeader())) {
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

    @Override
    public void importCollectionReport(ReportType report, PrismContainerValue containerValue, RunningTask task, OperationResult result) {
        if (report.getObjectCollection() != null) {
            ObjectCollectionReportEngineConfigurationType collectionConfig = report.getObjectCollection();
            CompiledObjectCollectionView compiledCollection;
            try {
                compiledCollection = createCompiledView(collectionConfig, task, result);
            } catch (Exception e) {
                LOGGER.error("Couldn't define compiled collection for report", e);
                return;
            }

            if (compiledCollection != null) {
                Class<ObjectType> type = compiledCollection.getTargetClass();
                if (type == null) {
                    String message = "Definition of type in view is null";
                    LOGGER.error(message);
                    result.recordFatalError(message, new IllegalArgumentException(message));
                    return;
                }
                ImportOptionsType importOption = report.getBehavior().getImportOptions();
                try {
                    PrismContainerDefinition<ObjectType> def = getReportService().getPrismContext().getSchemaRegistry().findContainerDefinitionByCompileTimeClass(type);
                    @NotNull PrismContainer object = def.instantiate();
                    PrismContainerValue value = object.createNewValue();
                    List<GuiObjectColumnType> columns = compiledCollection.getColumns();
                    for (Item item : (Collection<Item>) containerValue.getItems()) {
                        for (GuiObjectColumnType column : columns) {
                            String columnName = getColumnLabel(column, def);
                            if (item.getElementName().getLocalPart().equals(columnName)) {
                                Item newItem = object.getDefinition().findItemDefinition(column.getPath().getItemPath()).instantiate();
                                if (newItem instanceof PrismProperty) {
                                    String stringValue = (String) item.getRealValue();
                                    if (StringUtils.isEmpty(stringValue)) {
                                        continue;
                                    }
                                    Object parsedObject;
                                    try {
                                        parsedObject = parseRealValueFromString((PrismPropertyDefinition) newItem.getDefinition(), stringValue);
                                    } catch (SchemaException e) {
                                        LOGGER.error("Couldn't parse value from " + stringValue + "for item " + newItem.getDefinition(), e);
                                        continue;
                                    }
                                    PrismPropertyValue newValue = getReportService().getPrismContext().itemFactory().createPropertyValue(parsedObject);
                                    ((PrismProperty)newItem).addValue(newValue);
                                }
                                value.add(newItem);
                            }
                        }
                    }
                    getReportService().getModelService().importObject((PrismObject) object, importOption, task, result);
                    return;
                } catch (SchemaException e) {
                    String message = "Couldn't instantiate object of type " + type;
                    LOGGER.error(message);
                    result.recordFatalError(message, new IllegalArgumentException(message));
                    return;
                }
            } else {
                String message = "View is null";
                LOGGER.error(message);
                result.recordFatalError(message, new IllegalArgumentException(message));
                return;
            }
        } else {
            String message = "CollectionRefSpecification is null";
            LOGGER.error(message);
            result.recordFatalError(message, new IllegalArgumentException(message));
            return;
        }
    }

    private Object parseRealValueFromString(PrismPropertyDefinition def, String value) throws SchemaException {
        String embeeded = "<a>" + StringEscapeUtils.escapeXml(value) + "</a>";
        Object parsed = getReportService().getPrismContext().parserFor(embeeded).xml().definition(def).parseRealValue();
        return parsed;
    }

    public PrismContainer createContainerFromFile(ReportType report, ReportDataType reportData, Task task, OperationResult result) throws IOException {
        ObjectCollectionReportEngineConfigurationType collectionEngineConf = report.getObjectCollection();
        if (collectionEngineConf == null) {
            throw new IllegalArgumentException("Report of 'import' direction support only object collection engine."
                    + " Please define ObjectCollectionReportEngineConfigurationType in report type.");
        }
        CompiledObjectCollectionView compiledCollection;
        try {
            compiledCollection = createCompiledView(collectionEngineConf, task, result);
        } catch (Exception e) {
            LOGGER.error("Couldn't define compiled collection for report", e);
            return null;
        }
        List<GuiObjectColumnType> columns = MiscSchemaUtil.orderCustomColumns(compiledCollection.getColumns());
        Class<ObjectType> type = compiledCollection.getTargetClass();
        if (type == null) {
            throw new IllegalArgumentException("Couldn't define type of imported objects");
        }
        PrismObjectDefinition<ObjectType> def = getReportService().getPrismContext().getSchemaRegistry().findItemDefinitionByCompileTimeClass(type, PrismObjectDefinition.class);
        List<String> headers = new ArrayList<>();
        columns.forEach(column -> {
            Validate.notNull(column.getName(), "Name of column is null");
            String label = getColumnLabel(column, def);
            headers.add(label);
        });
        Reader reader = Files.newBufferedReader(Paths.get(reportData.getFilePath()));
        CSVFormat csvFormat = createCsvFormat();
        if (Boolean.TRUE.equals(isHeader())) {
            String[] arrayHeader = new String[headers.size()];
            arrayHeader = headers.toArray(arrayHeader);
            csvFormat = csvFormat.withHeader(arrayHeader)
                    .withSkipHeaderRecord(true);
        } else {
            csvFormat = csvFormat.withSkipHeaderRecord(false);
        }
        CSVParser csvParser = new CSVParser(reader, csvFormat);
        ItemFactory itemFactory = getReportService().getPrismContext().itemFactory();
        PrismContainer container = itemFactory.createContainer(new QName("Object"));

        for (CSVRecord csvRecord : csvParser) {
            PrismContainerValue containerValue = container.createNewValue();
            for (String name : headers) {
                PrismProperty property = itemFactory.createProperty(new QName(name));
                Object value;
                if (isHeader()) {
                    value = csvRecord.get(name);
                } else {
                    value = csvRecord.get(headers.indexOf(name));
                }
                property.getValues().add(itemFactory.createPropertyValue(value));
                try {
                    containerValue.add(property);
                } catch (SchemaException e) {
                    LOGGER.error("Couldn't add property " + property, e);
                }
            }
        }
        return container;
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

    private boolean isHeader() {
        return getCsvConfiguration().isHeader() == null ? true : getCsvConfiguration().isHeader();
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
        if (getFileFormatConfiguration().getCsv() == null) {
            return new CsvExportType();
        }
        return getFileFormatConfiguration().getCsv();
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
