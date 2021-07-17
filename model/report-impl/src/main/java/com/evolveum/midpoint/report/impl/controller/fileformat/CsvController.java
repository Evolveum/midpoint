/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report.impl.controller.fileformat;

import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.model.api.interaction.DashboardWidget;
import com.evolveum.midpoint.model.common.util.DefaultColumnUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.report.impl.ReportServiceImpl;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

/**
 * Legacy code that deals with both exporting (dashboard- and collection-style) and importing reports.
 * Specialized to CSV files.
 *
 * TODO split into smaller, more specific classes
 *
 * @author skublik
 */
public class CsvController extends FileFormatController {

    private static final Trace LOGGER = TraceManager.getTrace(CsvController.class);

    @NotNull private final CommonCsvSupport support;

    public CsvController(FileFormatConfigurationType fileFormatConfiguration, ReportType report, ReportServiceImpl reportService) {
        super(fileFormatConfiguration, report, reportService);
        support = new CommonCsvSupport(fileFormatConfiguration);
    }

    @Override
    public byte[] processDashboard(DashboardReportEngineConfigurationType dashboardConfig, RunningTask task, OperationResult result) throws Exception {
        ObjectReferenceType ref = dashboardConfig.getDashboardRef();
        Class<ObjectType> type = getReportService().getPrismContext().getSchemaRegistry().determineClassForType(ref.getType());
        DashboardType dashboard = (DashboardType) getReportService().getModelService()
                .getObject(type, ref.getOid(), null, task, result)
                .asObjectable();

        CSVFormat csvFormat = support.createCsvFormat();
        if (support.isHeader()) {
            String[] arrayHeader = new String[getHeadsOfWidget().size()];
            arrayHeader = getHeadsOfWidget().toArray(arrayHeader);
            csvFormat = csvFormat.withHeader(arrayHeader)
                    .withSkipHeaderRecord(false);
        } else {
            csvFormat = csvFormat.withSkipHeaderRecord(true);
        }

        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            CSVPrinter printer = new CSVPrinter(new OutputStreamWriter(output, support.getEncoding()), csvFormat);

            long i = 1;
            task.setExpectedTotal((long) dashboard.getWidget().size());
            task.flushPendingModifications(result);

            for (DashboardWidgetType widget : dashboard.getWidget()) {
                recordProgress(task, i, result, LOGGER);
                DashboardWidget widgetData = dashboardService.createWidgetData(widget, task, result);
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
    public byte[] processCollection(String nameOfReport, ObjectCollectionReportEngineConfigurationType collectionConfig,
            RunningTask task, OperationResult result) throws CommonException {
        initializeParameters(collectionConfig.getParameter(), task);
        CompiledObjectCollectionView compiledCollection =
                reportService.createCompiledView(collectionConfig, true, task, result);

        return createTableBox(collectionConfig.getCollection(), compiledCollection,
                    collectionConfig.getCondition(), collectionConfig.getSubreport(), result, task);
    }

    private byte[] createTableBox(CollectionRefSpecificationType collection, CompiledObjectCollectionView compiledCollection,
            ExpressionType condition, List<SubreportParameterType> subreports, OperationResult result, RunningTask task)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {

        Class<Containerable> type = reportService.resolveTypeForReport(collection, compiledCollection);
        Collection<SelectorOptions<GetOperationOptions>> options = DefaultColumnUtils.createOption(type, schemaService);
        PrismContainerDefinition<Containerable> def = schemaRegistry.findItemDefinitionByCompileTimeClass(type, PrismContainerDefinition.class);

        List<String> headers = new ArrayList<>();
        List<List<String>> records = new ArrayList<>();

        List<GuiObjectColumnType> columns = MiscSchemaUtil.orderCustomColumns(compiledCollection.getColumns());
        columns.forEach(column -> {
            Validate.notNull(column.getName(), "Name of column is null");
            String label = getColumnLabel(column, def);
            headers.add(label);
        });

        AtomicInteger index = new AtomicInteger(1);
        Predicate<PrismContainer> handler = (value) -> {
            if (!task.canRun()) {
                return false;
            }
            recordProgress(task, index.getAndIncrement(), result, LOGGER);
            boolean writeRecord = true;
            if (condition != null) {
                try {
                    writeRecord = evaluateCondition(condition, value, task, result);
                } catch (Exception e) {
                    LOGGER.error("Couldn't evaluate condition for report record.");
                    return false;
                }
            }

            if (writeRecord) {
                List<String> items = new ArrayList<>();
                evaluateSubreportParameters(subreports, value, task);
                columns.forEach(column -> {
                    ItemPath path = column.getPath() == null ? null : column.getPath().getItemPath();
                    ExpressionType expression = column.getExport() != null ? column.getExport().getExpression() : null;
                    items.add(getRealValueAsString(column, value, path, expression, task, result));
                });
                records.add(items);
            }
            cleanUpVariables();
            return true;
        };
        searchObjectFromCollection(collection, compiledCollection.getContainerType(), handler,
                options, task, result, true);

        CSVFormat csvFormat = support.createCsvFormat();
        if (support.isHeader()) {
            String[] arrayHeader = new String[headers.size()];
            arrayHeader = headers.toArray(arrayHeader);
            csvFormat = csvFormat.withHeader(arrayHeader)
                    .withSkipHeaderRecord(false);
        } else {
            csvFormat = csvFormat.withSkipHeaderRecord(true);
        }

        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            CSVPrinter printer = new CSVPrinter(new OutputStreamWriter(output, support.getEncoding()), csvFormat);
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
    protected String getRealValueAsString(GuiObjectColumnType column, PrismContainer<? extends Containerable> object, ItemPath itemPath, ExpressionType expression, Task task, OperationResult result) {
        String value = super.getRealValueAsString(column, object, itemPath, expression, task, result);
        value = removeNewLine(value);

        return value;
    }

    @Override
    public void importCollectionReport(ReportType report, VariablesMap variables, RunningTask task, OperationResult result) {
        if (report.getObjectCollection() != null) {
            ObjectCollectionReportEngineConfigurationType collectionConfig = report.getObjectCollection();
            CompiledObjectCollectionView compiledCollection;
            try {
                compiledCollection = getReportService().createCompiledView(collectionConfig, true, task, result);
            } catch (Exception e) {
                LOGGER.error("Couldn't define compiled collection for report", e);
                return;
            }

            if (compiledCollection != null) {
                Class<ObjectType> type = compiledCollection.getTargetClass(getReportService().getPrismContext());
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
                    for (String name : variables.keySet()) {
                        boolean isFound = false;
                        for (GuiObjectColumnType column : columns) {
                            if (DisplayValueType.NUMBER.equals(column.getDisplayValue()) || isFound) {
                                continue;
                            }
                            String columnName = getColumnLabel(column, def);
                            if (name.equals(columnName)) {
                                isFound = true;
                                if (column.getPath() != null) {
                                    ItemPath path = column.getPath().getItemPath();
                                    ItemDefinition newItemDefinition = object.getDefinition().findItemDefinition(path);
                                    Item newItem = null;
                                    Object objectFromExpression = null;
                                    TypedValue typedValue = variables.get(name);
                                    if (column.getImport() != null && column.getImport().getExpression() != null) {
                                        if (newItemDefinition.isSingleValue()) {
                                            objectFromExpression = evaluateImportExpression(column.getImport().getExpression(),
                                                    (String) typedValue.getValue(), task, result);
                                        } else {
                                            objectFromExpression = evaluateImportExpression(column.getImport().getExpression(),
                                                    getImportStringValues(typedValue.getValue(), false), task, result);
                                        }
                                        if (objectFromExpression == null) {
                                            continue;
                                        }
                                    }
                                    if (newItemDefinition instanceof PrismPropertyDefinition) {
                                        newItem = object.findOrCreateProperty(path);
                                        processPropertyFromImportReport(objectFromExpression, typedValue, newItem, result);
                                    } else if (newItemDefinition instanceof PrismReferenceDefinition) {
                                        newItem = object.findOrCreateReference(path);
                                        processReferenceFromImportReport(objectFromExpression, typedValue, newItem, type, task, result);
                                    } else if (newItemDefinition instanceof PrismContainerDefinition) {
                                        newItem = object.findOrCreateContainer(path);
                                        processContainerFromImportReport(objectFromExpression, newItem, column.getPath(), result);
                                    }
                                } else {
                                    String message = "Path of column is null, skipping column " + columnName;
                                    LOGGER.error(message);
                                    result.recordPartialError(message);
                                    continue;
                                }
                            }
                        }
                    }
                    getReportService().getModelService().importObject((PrismObject) object, importOption, task, result);
                } catch (SchemaException e) {
                    String message = "Couldn't instantiate object of type " + type;
                    LOGGER.error(message);
                    result.recordPartialError(message, new IllegalArgumentException(message));
                }
            } else {
                String message = "View is null";
                LOGGER.error(message);
                result.recordFatalError(message, new IllegalArgumentException(message));
            }
        } else {
            String message = "CollectionRefSpecification is null";
            LOGGER.error(message);
            result.recordFatalError(message, new IllegalArgumentException(message));
        }
    }

    private void processContainerFromImportReport(Object objectFromExpression, Item newItem, ItemPathType path, OperationResult result) {
        if (objectFromExpression != null) {
            Collection realValues = new ArrayList();
            if (Collection.class.isAssignableFrom(objectFromExpression.getClass())) {
                realValues.addAll((Collection) objectFromExpression);
            } else {
                realValues.add(objectFromExpression);
            }
            for (Object realValue : realValues) {
                if (realValue != null) {
                    PrismContainerValue newValue;
                    if (realValue instanceof Containerable) {
                        newValue = ((Containerable) realValue).asPrismContainerValue();
                    } else {
                        String message = "Couldn't create new container value from " + realValue + "; expect Containerable type";
                        LOGGER.error(message);
                        result.recordPartialError(message);
                        continue;
                    }
                    try {
                        ((PrismContainer) newItem).add(newValue);
                    } catch (SchemaException e) {
                        String message = "Couldn't add new container value to item " + newItem;
                        LOGGER.error(message);
                        result.recordPartialError(message);
                        continue;
                    }
                }
            }
        } else {
            String message = "Found unexpected type PrismContainer from path " + path + " for import column ";
            LOGGER.error(message);
            result.recordPartialError(message);
        }
    }

    private void processReferenceFromImportReport(Object objectFromExpression, TypedValue typedValue, Item newItem, Class type, Task task, OperationResult result) {
        if (objectFromExpression != null) {
            Collection realValues = new ArrayList();
            if (Collection.class.isAssignableFrom(objectFromExpression.getClass())) {
                realValues.addAll((Collection) objectFromExpression);
            } else {
                realValues.add(objectFromExpression);
            }
            for (Object realValue : realValues) {
                if (realValue != null) {
                    PrismReferenceValue newValue;
                    if (realValue instanceof PrismObject) {
                        newValue = getReportService().getPrismContext().itemFactory().createReferenceValue((PrismObject) realValue);
                    } else if (realValue instanceof Referencable) {
                        newValue = ((Referencable) realValue).asReferenceValue();
                    } else {
                        String message = "Couldn't create new reference value from " + realValue + "; expect PrismObject or Referencable type";
                        LOGGER.error(message);
                        result.recordPartialError(message);
                        continue;
                    }
                    try {
                        ((PrismReference) newItem).add(newValue);
                    } catch (SchemaException e) {
                        String message = "Couldn't add new reference value to item " + newItem;
                        LOGGER.error(message);
                        result.recordPartialError(message);
                        continue;
                    }
                }
            }
        } else {
            ArrayList<String> stringValues = getImportStringValues(typedValue.getValue(), newItem.isSingleValue());
            for (String stringValue : stringValues) {

                QName targetType = ((PrismReference) newItem).getDefinition().getTargetTypeName();
                Class<ObjectType> targetTypeClass;
                if (targetType == null) {
                    targetTypeClass = ObjectType.class;
                } else {
                    targetTypeClass = (Class<ObjectType>) schemaRegistry.getCompileTimeClassForObjectType(targetType);
                }
                ObjectQuery query = getReportService().getPrismContext().queryFor(targetTypeClass).item(ObjectType.F_NAME).eq(stringValue).build();
                SearchResultList<PrismObject<ObjectType>> list = null;
                try {
                    list = getReportService().getModelService().searchObjects(type, query, null, task, result);
                } catch (Exception e) {
                    String message = "Couldn't search object of type " + targetTypeClass + " by name " + stringValue;
                    LOGGER.error(message, e);
                    result.recordPartialError(message, e);
                    continue;
                }
                if (list != null) {
                    if (list.size() > 1) {
                        String message = "Expected one search object of type  " + targetTypeClass + " by name " + stringValue
                                + "but found " + list.size() + ": " + list;
                        LOGGER.error(message);
                        result.recordPartialError(message);
                        continue;
                    }
                    PrismReferenceValue newValue = getReportService().getPrismContext().itemFactory().createReferenceValue(list.get(0));
                    try {
                        ((PrismReference) newItem).add(newValue);
                    } catch (SchemaException e) {
                        String message = "Couldn't add new reference value to item " + newItem;
                        LOGGER.error(message);
                        result.recordPartialError(message);
                        continue;
                    }
                }
            }
        }
    }

    private void processPropertyFromImportReport(Object objectFromExpression, TypedValue typedValue, Item newItem, OperationResult result) {
        if (objectFromExpression != null) {
            Collection realValues = new ArrayList();
            if (Collection.class.isAssignableFrom(objectFromExpression.getClass())) {
                realValues.addAll((Collection) objectFromExpression);
            } else {
                realValues.add(objectFromExpression);
            }
            for (Object realValue : realValues) {
                if (realValue != null) {
                    PrismPropertyValue newValue = getReportService().getPrismContext().itemFactory().createPropertyValue(realValue);
                    ((PrismProperty) newItem).addValue(newValue);
                }
            }
        } else {
            ArrayList<String> stringValues = getImportStringValues(typedValue.getValue(), newItem.isSingleValue());
            for (String stringValue : stringValues) {

                if (StringUtils.isEmpty(stringValue)) {
                    continue;
                }
                Object parsedObject;
                try {
                    parsedObject = parseRealValueFromString((PrismPropertyDefinition) newItem.getDefinition(), stringValue);
                } catch (SchemaException e) {
                    String message = "Couldn't parse value from " + stringValue + "for item " + newItem.getDefinition();
                    LOGGER.error(message, e);
                    result.recordPartialError(message, e);
                    continue;
                }
                PrismPropertyValue newValue = getReportService().getPrismContext().itemFactory().createPropertyValue(parsedObject);
                ((PrismProperty) newItem).addValue(newValue);
            }
        }
    }

    private ArrayList<String> getImportStringValues(Object realValue, boolean isSingleValue) {
        ArrayList<String> stringValues = new ArrayList();
        if (isSingleValue || realValue == null) {
            stringValues.add((String) realValue);
        } else {
            if (realValue instanceof String) {
                stringValues.add((String) realValue);
            } else {
                stringValues.addAll((List<String>) realValue);
            }
        }
        return stringValues;
    }

    private Object parseRealValueFromString(PrismPropertyDefinition def, String value) throws SchemaException {
        String embeeded = "<a>" + StringEscapeUtils.escapeXml(value) + "</a>";
        Object parsed = getReportService().getPrismContext().parserFor(embeeded).xml().definition(def).parseRealValue();
        return parsed;
    }

    public List<VariablesMap> createVariablesFromFile(ReportType report, ReportDataType reportData, boolean useImportScript, Task task, OperationResult result) throws IOException {
        ObjectCollectionReportEngineConfigurationType collectionEngineConf = report.getObjectCollection();
        if (collectionEngineConf == null && !useImportScript) {
            throw new IllegalArgumentException("Report of 'import' direction without import script support only object collection engine."
                    + " Please define ObjectCollectionReportEngineConfigurationType in report type.");
        }
        CompiledObjectCollectionView compiledCollection = null;
        try {
            if (collectionEngineConf != null) {
                compiledCollection = getReportService().createCompiledView(collectionEngineConf, !useImportScript, task, result);
            }
        } catch (Exception e) {
            LOGGER.error("Couldn't define compiled collection for report", e);
            return null;
        }
        List<String> headers = new ArrayList<>();
        Reader reader = Files.newBufferedReader(Paths.get(reportData.getFilePath()));
        CSVFormat csvFormat = support.createCsvFormat();
        if (compiledCollection != null) {
            List<GuiObjectColumnType> columns = MiscSchemaUtil.orderCustomColumns(compiledCollection.getColumns());
            Class<ObjectType> type = compiledCollection.getTargetClass(getReportService().getPrismContext());
            if (type == null) {
                throw new IllegalArgumentException("Couldn't define type of imported objects");
            }
            PrismObjectDefinition<ObjectType> def = getReportService().getPrismContext().getSchemaRegistry().findItemDefinitionByCompileTimeClass(type, PrismObjectDefinition.class);
            for (GuiObjectColumnType column : columns) {
                Validate.notNull(column.getName(), "Name of column is null");
                String label = getColumnLabel(column, def);
                headers.add(label);
            }
        } else {
            csvFormat = csvFormat.withFirstRecordAsHeader();
        }
        if (support.isHeader()) {
            if (!headers.isEmpty()) {
                String[] arrayHeader = new String[headers.size()];
                arrayHeader = headers.toArray(arrayHeader);
                csvFormat = csvFormat.withHeader(arrayHeader);
            }
            csvFormat = csvFormat.withSkipHeaderRecord(true);
        } else {
            if (headers.isEmpty()) {
                throw new IllegalArgumentException("Couldn't find headers please "
                        + "define them via view element or write them to csv file and set "
                        + "header element in file format configuration to true.");
            }
            csvFormat = csvFormat.withSkipHeaderRecord(false);
        }
        CSVParser csvParser = new CSVParser(reader, csvFormat);
        if (headers.isEmpty()) {
            headers = csvParser.getHeaderNames();
        }
        List<VariablesMap> listOfVariables = new ArrayList();
        for (CSVRecord csvRecord : csvParser) {
            VariablesMap variables = new VariablesMap();
            for (String name : headers) {
                String value;
                if (support.isHeader()) {
                    value = csvRecord.get(name);
                } else {
                    value = csvRecord.get(headers.indexOf(name));
                }
                if (value != null && value.isEmpty()) {
                    value = null;
                }
                if (value != null && value.contains(getMultivalueDelimiter())) {
                    String[] realValues = value.split(getMultivalueDelimiter());
                    variables.put(name, Arrays.asList(realValues), List.class);
                } else {
                    variables.put(name, value, String.class);
                }
            }
            listOfVariables.add(variables);
        }
        return listOfVariables;
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


    private CsvFileFormatType getCsvConfiguration() {
        if (getFileFormatConfiguration().getCsv() == null) {
            return new CsvFileFormatType();
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
        String delimiter = getMultivalueDelimiter();
        sb.append(delimiter);
    }

    protected String getMultivalueDelimiter() {
        return getCsvConfiguration().getMultivalueDelimiter() == null ? "," : getCsvConfiguration().getMultivalueDelimiter();
    }

}
