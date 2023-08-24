/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report.impl.controller;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.schema.config.ExecuteScriptConfigItem;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.text.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.report.impl.ReportServiceImpl;
import com.evolveum.midpoint.report.impl.activity.InputReportLine;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * Controls the process of importing collection-based reports.
 */
@Experimental
public class ImportController {

    private static final Trace LOGGER = TraceManager.getTrace(ImportController.class);

    /** The report of which an export is being done. */
    @NotNull private final ReportType report;

    /** Compiled final collection from more collections and archetypes related to object type. */
    private final CompiledObjectCollectionView compiledCollection;

    /** Import script of report. */
    private final ExecuteScriptConfigItem script;

    /**
     * Columns for the report.
     */
    private List<GuiObjectColumnType> columns;

    @NotNull private final CommonCsvSupport support;

    // Useful Spring beans
    private final ReportServiceImpl reportService;
    private final SchemaRegistry schemaRegistry;
    private final LocalizationService localizationService;

    public ImportController(
            @NotNull ReportType report,
            @NotNull ReportServiceImpl reportService,
            CompiledObjectCollectionView compiledCollection) {

        this.report = report;
        this.reportService = reportService;
        this.schemaRegistry = reportService.getPrismContext().getSchemaRegistry();
        this.localizationService = reportService.getLocalizationService();
        this.compiledCollection = compiledCollection;
        ReportBehaviorType behavior = report.getBehavior();
        ExecuteScriptType importScript = behavior != null ? behavior.getImportScript() : null;
        if (importScript != null) {
            // We cannot use "embedded" origin for property real values, as they have no notion of the parent.
            // Hence, we have to start with the origin "behavior" (containerable), and derive our origin from it.
            ConfigurationItemOrigin origin =
                    ConfigurationItemOrigin.embedded(behavior)
                            .child(ReportBehaviorType.F_IMPORT_SCRIPT);
            this.script = ExecuteScriptConfigItem.of(importScript, origin);
        } else {
            this.script = null;
        }
        this.support = new CommonCsvSupport(report.getFileFormat());
    }

    /**
     * Prepares the controller for accepting the source data:
     * initializes the data source, determines columns, etc.
     */
    public void initialize()
            throws CommonException {
        if (compiledCollection != null) {
            columns = MiscSchemaUtil.orderCustomColumns(compiledCollection.getColumns());
        }
    }

    /**
     * BEWARE: Can be called from multiple threads at once.
     */
    public void handleDataRecord(InputReportLine line, RunningTask workerTask, OperationResult result) throws CommonException {
        if (compiledCollection != null) {
            Class<ObjectType> type = compiledCollection.getTargetClass();
            if (type == null) {
                String message = "Definition of type in view is null";
                LOGGER.error(message);
                result.recordFatalError(message, new IllegalArgumentException(message));
                return;
            }
            ImportOptionsType importOption = report.getBehavior().getImportOptions();
            PrismContainerDefinition<ObjectType> def = reportService.getPrismContext().getSchemaRegistry().findContainerDefinitionByCompileTimeClass(type);
            PrismContainer<ObjectType> object = def.instantiate();
            List<GuiObjectColumnType> columns = compiledCollection.getColumns();
            @NotNull VariablesMap variables = line.getVariables();
            for (String name : variables.keySet()) {
                boolean isFound = false;
                for (GuiObjectColumnType column : columns) {
                    if (DisplayValueType.NUMBER.equals(column.getDisplayValue()) || isFound) {
                        continue;
                    }
                    String columnName = GenericSupport.getLabel(column, def, localizationService);
                    if (name.equals(columnName)) {
                        isFound = true;
                        if (column.getPath() != null) {
                            ItemPath path = column.getPath().getItemPath();
                            ItemDefinition<? extends Item<?, ?>> newItemDefinition = object.getDefinition().findItemDefinition(path);
                            Item<?, ?> newItem;
                            Object objectFromExpression = null;
                            TypedValue<?> typedValue = variables.get(name);
                            if (column.getImport() != null && column.getImport().getExpression() != null) {
                                if (newItemDefinition.isSingleValue()) {
                                    objectFromExpression = evaluateImportExpression(column.getImport().getExpression(),
                                            typedValue, workerTask, result);
                                } else {
                                    objectFromExpression = evaluateImportExpression(column.getImport().getExpression(),
                                            typedValue, workerTask, result);
                                }
                                if (objectFromExpression == null) {
                                    continue;
                                }
                            }
                            if (newItemDefinition instanceof PrismPropertyDefinition) {
                                newItem = object.findOrCreateProperty(path);
                                processPropertyFromImportReport(
                                        objectFromExpression,
                                        typedValue,
                                        (Item<PrismPropertyValue<?>, PrismPropertyDefinition<?>>) newItem,
                                        result);
                            } else if (newItemDefinition instanceof PrismReferenceDefinition) {
                                newItem = object.findOrCreateReference(path);
                                processReferenceFromImportReport(
                                        objectFromExpression,
                                        typedValue,
                                        (Item<PrismReferenceValue, PrismReferenceDefinition>) newItem,
                                        type,
                                        workerTask,
                                        result);
                            } else if (newItemDefinition instanceof PrismContainerDefinition) {
                                newItem = object.findOrCreateContainer(path);
                                processContainerFromImportReport(
                                        objectFromExpression,
                                        (Item<PrismContainerValue<?>, PrismContainerDefinition<?>>) newItem,
                                        column.getPath(),
                                        result);
                            }
                        } else {
                            String message = "Path of column is null, skipping column " + columnName;
                            LOGGER.error(message);
                            result.recordPartialError(message);
                            break;
                        }
                    }
                }
            }
            reportService.getModelService().importObject((PrismObject<ObjectType>) object, importOption, workerTask, result);
        } else {
            evaluateImportScript(line, workerTask, result);
        }
    }

    private void evaluateImportScript(InputReportLine line, RunningTask task, OperationResult result) throws CommonException {
        reportService.getBulkActionsService().executeBulkAction(
                script, line.getVariables(), false, task, result);
    }

    private Object evaluateImportExpression(ExpressionType expression, TypedValue<?> typedValue, Task task, OperationResult result) {
        Object value = null;
        try {
            VariablesMap variables = new VariablesMap();
            variables.put(ExpressionConstants.VAR_INPUT, typedValue.getValue(), String.class);
            value = reportService.evaluateScript(report.asPrismObject(), expression, variables, "value for column (import)", task, result);
        } catch (Exception e) {
            LOGGER.error("Couldn't execute expression " + expression, e);
        }
        return value;
    }

    private void processContainerFromImportReport(Object objectFromExpression,
            Item<PrismContainerValue<?>, PrismContainerDefinition<?>> newItem, ItemPathType path, OperationResult result) {
        if (objectFromExpression != null) {
            Collection<Object> realValues = new ArrayList<>();
            exportRealValuesFromObjectFromExpression(objectFromExpression, realValues);
            for (Object realValue : realValues) {
                if (realValue != null) {
                    PrismContainerValue<?> newValue;
                    if (realValue instanceof Containerable) {
                        newValue = ((Containerable) realValue).asPrismContainerValue();
                    } else {
                        String message = "Couldn't create new container value from " + realValue + "; expect Containerable type";
                        LOGGER.error(message);
                        result.recordPartialError(message);
                        continue;
                    }
                    try {
                        newItem.add(newValue);
                    } catch (SchemaException e) {
                        String message = "Couldn't add new container value to item " + newItem;
                        LOGGER.error(message);
                        result.recordPartialError(message);
                        break;
                    }
                }
            }
        } else {
            String message = "Found unexpected type PrismContainer from path " + path + " for import column ";
            LOGGER.error(message);
            result.recordPartialError(message);
        }
    }

    private void processReferenceFromImportReport(Object objectFromExpression, TypedValue<?> typedValue,
            Item<PrismReferenceValue, PrismReferenceDefinition> newItem,
            Class<ObjectType> type, Task task, OperationResult result) {
        if (objectFromExpression != null) {
            Collection<Object> realValues = new ArrayList<>();
            exportRealValuesFromObjectFromExpression(objectFromExpression, realValues);
            for (Object realValue : realValues) {
                if (realValue != null) {
                    PrismReferenceValue newValue;
                    if (realValue instanceof PrismObject) {
                        newValue = reportService.getPrismContext().itemFactory()
                                .createReferenceValue((PrismObject<?>) realValue);
                    } else if (realValue instanceof Referencable) {
                        newValue = ((Referencable) realValue).asReferenceValue();
                    } else {
                        String message = "Couldn't create new reference value from " + realValue
                                + "; expect PrismObject or Referencable type";
                        LOGGER.error(message);
                        result.recordPartialError(message);
                        continue;
                    }
                    try {
                        newItem.add(newValue);
                    } catch (SchemaException e) {
                        String message = "Couldn't add new reference value to item " + newItem;
                        LOGGER.error(message);
                        result.recordPartialError(message);
                        break;
                    }
                }
            }
        } else {
            ArrayList<String> stringValues = getImportStringValues(typedValue.getValue(), newItem.isSingleValue());
            for (String stringValue : stringValues) {

                QName targetType = newItem.getDefinition().getTargetTypeName();
                Class<? extends Containerable> targetTypeClass;
                if (targetType == null) {
                    targetTypeClass = ObjectType.class;
                } else {
                    targetTypeClass = schemaRegistry.getCompileTimeClassForObjectType(targetType);
                }
                ObjectQuery query = reportService.getPrismContext().queryFor(targetTypeClass).item(ObjectType.F_NAME).eq(stringValue).build();
                SearchResultList<PrismObject<ObjectType>> list;
                try {
                    list = reportService.getModelService().searchObjects(type, query, null, task, result);
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
                    PrismReferenceValue newValue = reportService.getPrismContext().itemFactory().createReferenceValue(list.get(0));
                    try {
                        newItem.add(newValue);
                    } catch (SchemaException e) {
                        String message = "Couldn't add new reference value to item " + newItem;
                        LOGGER.error(message);
                        result.recordPartialError(message);
                        break;
                    }
                }
            }
        }
    }

    private void processPropertyFromImportReport(Object objectFromExpression, TypedValue<?> typedValue,
            Item<PrismPropertyValue<?>, PrismPropertyDefinition<?>> newItem, OperationResult result) {
        if (objectFromExpression != null) {
            Collection<Object> realValues = new ArrayList<>();
            exportRealValuesFromObjectFromExpression(objectFromExpression, realValues);
            for (Object realValue : realValues) {
                if (realValue != null) {
                    PrismPropertyValue<?> newValue = reportService.getPrismContext().itemFactory().createPropertyValue(realValue);
                    //noinspection unchecked,rawtypes
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
                    parsedObject = parseRealValueFromString(newItem.getDefinition(), stringValue);
                } catch (SchemaException e) {
                    String message = "Couldn't parse value from " + stringValue + "for item " + newItem.getDefinition();
                    LOGGER.error(message, e);
                    result.recordPartialError(message, e);
                    continue;
                }
                PrismPropertyValue<?> newValue = reportService.getPrismContext().itemFactory().createPropertyValue(parsedObject);
                //noinspection unchecked,rawtypes
                ((PrismProperty) newItem).addValue(newValue);
            }
        }
    }

    private void exportRealValuesFromObjectFromExpression(Object objectFromExpression, Collection<Object> realValues) {
        Collection<Object> collection;
        if (objectFromExpression instanceof Collection) {
            //noinspection unchecked
            collection = (Collection<Object>) objectFromExpression;
        } else {
            collection = Collections.singletonList(objectFromExpression);
        }

        collection.forEach(value -> {
            if (value instanceof PrismValue) {
                realValues.add(((PrismValue) value).getRealValue());
            } else {
                realValues.add(value);
            }
        });
    }

    private ArrayList<String> getImportStringValues(Object realValue, boolean isSingleValue) {
        ArrayList<String> stringValues = new ArrayList<>();
        if (isSingleValue || realValue == null) {
            stringValues.add((String) realValue);
        } else {
            if (realValue instanceof String) {
                stringValues.add((String) realValue);
            } else {
                //noinspection unchecked
                stringValues.addAll((List<String>) realValue);
            }
        }
        return stringValues;
    }

    private Object parseRealValueFromString(PrismPropertyDefinition<?> def, String value) throws SchemaException {
        String embedded = "<a>" + StringEscapeUtils.escapeHtml4(value) + "</a>";
        return reportService.getPrismContext().parserFor(embedded).xml().definition(def).parseRealValue();
    }

    public List<VariablesMap> parseColumnsAsVariablesFromFile(ReportDataType reportData)
            throws IOException {
        List<String> headers = new ArrayList<>();
        Reader reader = Files.newBufferedReader(Paths.get(reportData.getFilePath()));
        CSVFormat csvFormat = support.createCsvFormat();
        if (compiledCollection != null) {
            Class<ObjectType> type = compiledCollection.getTargetClass();
            if (type == null) {
                throw new IllegalArgumentException("Couldn't define type of imported objects");
            }
            PrismObjectDefinition<?> def = reportService.getPrismContext().getSchemaRegistry().findItemDefinitionByCompileTimeClass(
                    type, PrismObjectDefinition.class);
            for (GuiObjectColumnType column : columns) {
                Validate.notNull(column.getName(), "Name of column is null");
                String label = GenericSupport.getLabel(column, def, localizationService);
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

        List<VariablesMap> variablesMaps = new ArrayList<>();
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
                if (value != null && value.contains(support.getMultivalueDelimiter())) {
                    String[] realValues = value.split(support.getMultivalueDelimiter());
                    variables.put(name, Arrays.asList(realValues), String.class);
                } else {
                    variables.put(name, value, String.class);
                }
            }
            variablesMaps.add(variables);
        }
        return variablesMaps;
    }
}
