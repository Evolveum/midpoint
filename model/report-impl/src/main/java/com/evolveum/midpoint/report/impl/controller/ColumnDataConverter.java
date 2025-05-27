/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report.impl.controller;

import static com.evolveum.midpoint.report.impl.controller.CommonHtmlSupport.*;
import static com.evolveum.midpoint.util.MiscUtil.argCheck;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.schema.processor.ShadowAssociationValue;
import com.evolveum.midpoint.util.annotation.Experimental;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.interaction.DashboardWidget;
import com.evolveum.midpoint.model.common.util.DefaultColumnUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.report.impl.ReportServiceImpl;
import com.evolveum.midpoint.report.impl.ReportUtils;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Converts record ({@link Containerable}, {@link Referencable} or later POJO) to a semi-formatted row
 * ({@link ExportedReportDataRow} - basically, a string representation) according to individual columns specifications.
 *
 * Responsibilities:
 *
 * 1. resolves paths, including following the references (if any),
 * 2. evaluates expressions,
 * 3. formats (pretty-prints) the values.
 *
 * Instantiated for each individual record.
 */
class ColumnDataConverter<C> {

    private static final Trace LOGGER = TraceManager.getTrace(ColumnDataConverter.class);

    @NotNull private final C record;
    @NotNull private final ReportType report;
    @NotNull private final VariablesMap parameters;
    @NotNull private final ReportServiceImpl reportService;
    @NotNull private final RunningTask task;
    @NotNull private final OperationResult result;

    ColumnDataConverter(@NotNull C record, @NotNull ReportType report, @NotNull VariablesMap parameters,
            @NotNull ReportServiceImpl reportService, @NotNull RunningTask task, @NotNull OperationResult result) {
        this.record = record;
        this.report = report;
        this.parameters = parameters;
        this.reportService = reportService;
        this.task = task;
        this.result = result;
    }

    ColumnDataConverter(@NotNull C record, @NotNull ReportType report, @NotNull ReportServiceImpl reportService,
            @NotNull RunningTask task, @NotNull OperationResult result) {
        this(record, report, new VariablesMap(), reportService, task, result);
    }

    List<String> convertWidgetColumn(@NotNull String header, @Nullable Function<String, String> processValueForStatus) throws CommonException {
        DashboardWidget data = reportService.getDashboardService().createWidgetData((DashboardWidgetType) record, false, task, result);
        if (header.equals(LABEL_COLUMN)) {
            return Collections.singletonList(data.getLabel(reportService.getLocalizationService()));
        }
        if (header.equals(NUMBER_COLUMN)) {
            return Collections.singletonList(data.getNumberMessage());
        }
        if (header.equals(STATUS_COLUMN)) {
            List<String> values = new ArrayList<>();
            if (data.getDisplay() != null && StringUtils.isNoneBlank(data.getDisplay().getColor())) {
                if (processValueForStatus == null) {
                    values.add(data.getDisplay().getColor());
                } else {
                    values.add(processValueForStatus.apply(data.getDisplay().getColor()));
                }
            }
            return values;
        }
        throw new IllegalArgumentException("Unsupported column header " + header + " for widget column");
    }

    List<String> convertColumn(@NotNull GuiObjectColumnType column) {

        ItemPath itemPath = column.getPath() == null ? null : column.getPath().getItemPath();
        ExpressionType expression = column.getExport() != null ? column.getExport().getExpression() : null;

        argCheck(itemPath != null || expression != null,
                "Neither path nor expression for column %s is specified", column.getName());

        Collection<? extends PrismValue> dataValues;
        if (itemPath != null && !DefaultColumnUtils.isSpecialColumn(itemPath, record)) {
            dataValues = resolvePath(itemPath);
        } else if (record instanceof Containerable) {
            dataValues = List.of(((Containerable) record).asPrismContainerValue());
        } else if (record instanceof Referencable) {
            dataValues = List.of(((Referencable) record).asReferenceValue());
        } else {
            throw new IllegalArgumentException("Unsupported type '" + record.getClass() + "' of record: " + record);
        }

        if (expression != null) {
            dataValues = evaluateExportExpressionOverPrismValues(expression, dataValues, column);
        }

        if (DisplayValueType.NUMBER.equals(column.getDisplayValue())) {
            return List.of(String.valueOf(dataValues.size()));
        }
        if (DefaultColumnUtils.isSpecialColumn(itemPath, record)) {
            return MiscUtil.singletonOrEmptyList(
                    DefaultColumnUtils.processSpecialColumn(itemPath, record, reportService.getLocalizationService()));
        }
        return prettyPrintValues(dataValues);
    }

    /**
     * Resolves the path for the record.
     *
     * @return List of values of the item found. (Or empty list of nothing was found.)
     */
    private @NotNull List<? extends PrismValue> resolvePath(ItemPath itemPath) {
        if (itemPath.equivalent(PrismConstants.T_ID)) {
            return getIdentifier();
        }
        Item<?, ?> currentItem = null;
        Iterator<?> iterator = itemPath.getSegments().iterator();
        while (iterator.hasNext()) {
            ItemName name = ItemPath.toNameOrNull(iterator.next());
            if (name == null) {
                continue;
            }
            if (currentItem == null) {
                if (record instanceof Containerable) {
                    currentItem = ((Containerable) record).asPrismContainerValue().findItem(name);
// TODO: Not implemented yet, would we use paths with @/<targetItem> and ../<ownerItem> here?
//  } else if (record instanceof Referencable) {
//                    currentItem = ((Referencable) record).asReferenceValue().findItem(name);
                } else {
                    throw new IllegalArgumentException("Unsupported type '" + record.getClass() + "' of record: " + record);
                }
            } else {
                currentItem = (Item<?, ?>) currentItem.find(name);
            }
            if (currentItem == null) {
                break;
            }
            if (currentItem instanceof PrismProperty) {
                stateCheck(!iterator.hasNext(), "Cannot continue resolving path in prism property: %s", currentItem);
            } else if (currentItem instanceof PrismReference) {
                if (currentItem.isSingleValue()) {
                    Referencable ref = ((PrismReference) currentItem).getRealValue();
                    if (ref != null && iterator.hasNext()) {
                        currentItem = reportService.getObjectFromReference(ref, task, result);
                    }
                } else {
                    stateCheck(!iterator.hasNext(), "Cannot continue resolving path in multivalued reference: %s", currentItem);
                }
            }
        }
        return currentItem != null ? currentItem.getValues() : List.of();
    }

    @Experimental
    private List<? extends PrismValue> getIdentifier() {
        if (record instanceof Objectable) {
            return toPropertyValues(((Objectable) record).getOid());
        } else if (record instanceof Containerable) {
            return toPropertyValues(((Containerable) record).asPrismContainerValue().getId());
        } else {
            return List.of();
        }
    }

    private List<? extends PrismValue> toPropertyValues(Object realValue) {
        if (realValue == null) {
            return List.of();
        } else {
            return List.of(
                    PrismContext.get().itemFactory().createPropertyValue(realValue));
        }
    }

    private List<String> prettyPrintValues(Collection<? extends PrismValue> values) {
        return values.stream()
                .map(this::prettyPrintValue)
                .collect(Collectors.toList());
    }

    // TODO clean up this mess, and integrate with ValueDisplayUtil / PrettyPrinter / notification text formatter etc
    private String prettyPrintValue(PrismValue value) {
        if (value instanceof PrismPropertyValue) {
            Object realValue = ((PrismPropertyValue<?>) value).getRealValue();
            if (realValue == null) {
                return "";
            }
            // TODO why do we load the whole lookup table here, and not only the requested row (by key)?
            //  Also, we probably could implement some caching for small lookup tables, couldn't we?
            PrismObject<LookupTableType> lookupTable = null;
            try {
                lookupTable = loadLookupTable((PrismPropertyValue<?>) value);
            } catch (SchemaException | ObjectNotFoundException e) {
                LOGGER.error("Couldn't load lookupTable. Message: {}", e.getMessage());
            }
            if (lookupTable != null) {
                String lookupTableKey = realValue.toString();
                LookupTableType lookupTableObject = lookupTable.asObjectable();
                String rowLabel = "";
                for (LookupTableRowType lookupTableRow : lookupTableObject.getRow()) {
                    if (lookupTableRow.getKey().equals(lookupTableKey)) {
                        return lookupTableRow.getLabel() != null ? lookupTableRow.getLabel().getOrig() : lookupTableRow.getValue();
                    }
                }
                return rowLabel;
            }
            if (realValue instanceof Collection) {
                throw new IllegalStateException("Collection in a prism property? " + value);
            } else if (realValue instanceof Enum) {
                return ReportUtils.prettyPrintForReport((Enum<?>) realValue);
            } else if (realValue instanceof XMLGregorianCalendar) {
                return ReportUtils.prettyPrintForReport((XMLGregorianCalendar) realValue);
            } else if (realValue instanceof ObjectDeltaOperationType) {
                try {
                    return ReportUtils.printDelta(
                            DeltaConvertor.createObjectDeltaOperation((ObjectDeltaOperationType) realValue, PrismContext.get()));
                } catch (SchemaException e) {
                    LOGGER.error("Couldn't convert delta from ObjectDeltaOperationType to ObjectDeltaOperation {}", realValue);
                    return "";
                }
            }
            if (realValue instanceof ObjectType) {
                return ReportUtils.prettyPrintForReport((ObjectType) realValue, reportService.getLocalizationService());
            } else {
                return ReportUtils.prettyPrintForReport(realValue);
            }
        } else if (value instanceof PrismReferenceValue) {
            return getObjectNameFromRef(value.getRealValue());
        }

        if (value instanceof PrismObjectValue<?>) {
            Objectable realValue = ((PrismObjectValue<?>) value).asObjectable();
            if (realValue instanceof ObjectType) {
                return ReportUtils.prettyPrintForReport((ObjectType) realValue, reportService.getLocalizationService());
            }
        }

        if (value instanceof PrismContainerValue<?> pcv) {
            if (pcv instanceof ShadowAssociationValue shadowAssociationValue) {
                return prettyPrintValue(shadowAssociationValue);
            }
            if (pcv.getCompileTimeClass() != null) {
                Object realValue = pcv.getRealValue();
                if (realValue instanceof AssignmentType assignmentValue) {
                    return prettyPrintValue(assignmentValue);
                }
            }
        }

        return ReportUtils.prettyPrintForReport(value);
    }

    private String prettyPrintValue(@NotNull AssignmentType assignment) {
        List<String> segments = new ArrayList<>();
        segments.add("->");
        ObjectReferenceType targetRef = assignment.getTargetRef();
        if (targetRef != null) {
            segments.add(getObjectNameFromRef(targetRef));
        }
        ConstructionType construction = assignment.getConstruction();
        if (construction != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(getObjectNameFromRef(construction.getResourceRef()));
            sb.append(":");
            sb.append(
                    ReportUtils.prettyPrintForReport(
                            Objects.requireNonNullElse(construction.getKind(), ShadowKindType.ACCOUNT)));
            String intent = construction.getIntent();
            if (intent != null) {
                sb.append("/");
                sb.append(intent);
            }
            segments.add(sb.toString());
        }
        ActivationType activation = assignment.getActivation();
        if (activation != null) {
            XMLGregorianCalendar from = activation.getValidFrom();
            XMLGregorianCalendar to = activation.getValidTo();
            ActivationStatusType status = activation.getAdministrativeStatus();
            if (from != null || to != null) {
                segments.add(
                        "("+ReportUtils.prettyPrintForReport(from) + "-" + ReportUtils.prettyPrintForReport(to) + ")");
            }
            if (status != null) {
                segments.add(ReportUtils.prettyPrintForReport(status));
            }
        }
        Long id = assignment.getId();
        if (id != null) {
            segments.add("[" + id + "]");
        }
        // TODO other parts of the assignment?
        return String.join(" ", segments);
    }

    private String prettyPrintValue(@NotNull ShadowAssociationValue associationValue) {
        List<String> segments = new ArrayList<>();
        ObjectReferenceType shadowRef = associationValue.getSingleObjectRefRelaxed();
        if (shadowRef != null) {
            String name = getObjectNameFromRef(shadowRef);
            if (StringUtils.isNotEmpty(name)) {
                segments.add(name);
            }
            var shadow = shadowRef.getObject();
            if (segments.isEmpty() && shadow != null) {
                var attributesContainer = shadow.findContainer(ShadowType.F_ATTRIBUTES);
                if (attributesContainer != null) {
                    // HACK HACK HACK - what if there are all attributes, not only the identifiers?
                    // At this point we have no object definition, so we cannot select the identifiers. But maybe we could try!
                    for (Item<?, ?> item : attributesContainer.getValue().getItems()) {
                        for (Object realValue : item.getRealValues()) {
                            segments.add(String.valueOf(realValue));
                        }
                    }
                }
            }
        }
        return String.join(" ", segments);
    }

    private String getObjectNameFromRef(Referencable ref) {
        if (ref == null) {
            return "";
        }

        PolyStringType targetName = ref.getTargetName();
        if (targetName != null && targetName.getOrig() != null) {
            return targetName.getOrig();
        }
        PrismObject<?> object = reportService.getObjectFromReference(ref, task, result);

        if (object == null) {
            return ref.getOid();
        }

        if (object.getName() == null || object.getName().getOrig() == null) {
            return "";
        }

        return object.getName().getOrig();
    }

    private Collection<? extends PrismValue> evaluateExportExpressionOverPrismValues(@NotNull ExpressionType expression,
            @NotNull Collection<? extends PrismValue> prismValues, @NotNull GuiObjectColumnType column) {
        Object input;
        if (prismValues.isEmpty()) {
            input = null;
        } else if (prismValues.size() == 1) {
            input = prismValues.iterator().next().getRealValue();
        } else {
            input = prismValues.stream()
                    .filter(Objects::nonNull)
                    .map(PrismValue::getRealValue)
                    .collect(Collectors.toList());
        }
        return evaluateExportExpressionOverRealValues(expression, input, column);
    }

    private Collection<? extends PrismValue> evaluateExportExpressionOverRealValues(
            ExpressionType expression, Object input, @NotNull GuiObjectColumnType column) {
        VariablesMap variables = new VariablesMap();
        variables.putAll(parameters);
        if (input == null) {
            variables.put(ExpressionConstants.VAR_INPUT, null, Object.class);
        } else {
            variables.put(ExpressionConstants.VAR_INPUT, input, input.getClass());
        }

        variables.put(ExpressionConstants.VAR_LOCALE, Locale.getDefault(), Locale.class);

        try {
            return reportService.evaluateScript(report.asPrismObject(), expression, variables,
                    "value for column '" + column.getName() + "' (export)", task, result);
        } catch (Exception e) {
            LOGGER.error("Couldn't execute expression " + expression, e);
            return List.of();
        }
    }

    private PrismObject<LookupTableType> loadLookupTable(PrismPropertyValue<?> value)
            throws SchemaException, ObjectNotFoundException {
        String lookupTableOid = getValueEnumerationRefOid(value);
        if (lookupTableOid == null) {
            return null;
        }
        Collection<SelectorOptions<GetOperationOptions>> options =
                reportService.getSchemaService().getOperationOptionsBuilder()
                        .item(LookupTableType.F_ROW)
                        .retrieveQuery()
                        .asc(LookupTableRowType.F_LABEL)
                        .end()
                        .build();
        return reportService.getRepositoryService().getObject(
                LookupTableType.class, lookupTableOid, options, result);
    }

    private String getValueEnumerationRefOid(PrismPropertyValue<?> value) {
        if (value == null) {
            return null;
        }
        if (value.getParent() == null) {
            return null;
        }
        Itemable item = value.getParent();
        ItemDefinition<?> def = item.getDefinition();
        if (def == null) {
            return null;
        }

        PrismReferenceValue valueEnumerationRef = def.getValueEnumerationRef();
        if (valueEnumerationRef == null) {
            return null;
        }

        return valueEnumerationRef.getOid();
    }
}
