/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report.impl.controller.export;

import java.text.SimpleDateFormat;
import java.util.*;
import javax.xml.namespace.QName;

import com.google.common.collect.ImmutableSet;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.model.common.util.DefaultColumnUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.report.impl.ReportServiceImpl;
import com.evolveum.midpoint.report.impl.ReportUtils;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.constants.AuditConstants;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author skublik
 */

public abstract class ExportController {

    private static final Trace LOGGER = TraceManager.getTrace(ExportController.class);

    protected static final String LABEL_COLUMN = "label";
    protected static final String NUMBER_COLUMN = "number";
    protected static final String STATUS_COLUMN = "status";

    private final static Set<String> HEADS_OF_WIDGET =
            ImmutableSet.of(LABEL_COLUMN, NUMBER_COLUMN, STATUS_COLUMN);

    private final ReportServiceImpl reportService;
    private final ExportConfigurationType exportConfiguration;

    public ExportController(ExportConfigurationType exportConfiguration, ReportServiceImpl reportService) {
        this.exportConfiguration = exportConfiguration;
        this.reportService = reportService;
    }

    protected ReportServiceImpl getReportService() {
        return reportService;
    }

    public ExportConfigurationType getExportConfiguration() {
        return exportConfiguration;
    }

    protected static Set<String> getHeadsOfWidget() {
        return HEADS_OF_WIDGET;
    }

    public abstract byte[] processDashboard(DashboardReportEngineConfigurationType dashboardConfig, Task task, OperationResult result) throws Exception;

    public abstract byte[] processCollection(String nameOfReport, ObjectCollectionReportEngineConfigurationType collectionConfig, Task task, OperationResult result) throws Exception;

    protected void recordProgress(Task task, long progress, OperationResult opResult, Trace logger) {
        try {
            task.setProgressImmediate(progress, opResult);
        } catch (ObjectNotFoundException e) {             // these exceptions are of so little probability and harmless, so we just log them and do not report higher
            LoggingUtils.logException(logger, "Couldn't record progress to task {}, probably because the task does not exist anymore", e, task);
        } catch (SchemaException e) {
            LoggingUtils.logException(logger, "Couldn't record progress to task {}, due to unexpected schema exception", e, task);
        }
    }

    public abstract String getTypeSuffix();

    public abstract String getType();

//    protected String getMessage(Enum e) {
//        return getMessage(e.getDeclaringClass().getSimpleName() + '.' + e.name());
//    }

    protected String getMessage(String key) {
        return getMessage(key, null);
    }

    protected String getMessage(String key, Object... params) {
        return getReportService().getLocalizationService().translate(key, params, Locale.getDefault(), key);
    }

    protected String getRealValueAsString(GuiObjectColumnType column, PrismObject<ObjectType> object, ItemPath itemPath,
            ExpressionType expression, Task task, OperationResult result) {
        Item valueObject = object;

        if (itemPath != null && !DefaultColumnUtils.isSpecialColumn(itemPath, object)) {
            Iterator<?> iterator = itemPath.getSegments().iterator();
            while (iterator.hasNext()) {
                Object segment = iterator.next();
                QName name;
                if (segment instanceof QName) {
                    name = (QName) segment;
                } else if (segment instanceof NameItemPathSegment) {
                    name = ((NameItemPathSegment) segment).getName();
                } else {
                    continue;
                }
                if (valueObject == null) {
                    break;
                }
                valueObject = (Item) valueObject.find(ItemPath.create(name));
                if (valueObject instanceof PrismProperty && iterator.hasNext()) {
                    throw new IllegalArgumentException("Found object is PrismProperty, but ItemPath isn't empty");
                }
                if (valueObject instanceof PrismReference) {
                    if (valueObject.isSingleValue()) {
                        Referencable ref = ((PrismReference) valueObject).getRealValue();
                        if (iterator.hasNext()) {
                            valueObject = getObjectFromReference(ref);
                        }
                    } else {
                        if (iterator.hasNext()) {
                            throw new IllegalArgumentException("Found reference object is multivalue, but ItemPath isn't empty");
                        }
                    }

                }
            }
        }
        if (expression != null) {
            return evaluateExpression(expression, valueObject, task, result);
        }
        if (DisplayValueType.NUMBER.equals(column.getDisplayValue())) {
            if (valueObject == null) {
                return "0";
            }
            return String.valueOf(valueObject.getValues().size());
        }
        if (itemPath == null) {
            throw new IllegalArgumentException("Path and expression for column " + column.getName() + " is null");
        }
        if (DefaultColumnUtils.isSpecialColumn(itemPath, object)) {
            return DefaultColumnUtils.processSpecialColumn(itemPath, object, getReportService().getLocalizationService());
        }
        if (valueObject instanceof PrismContainer) {
            throw new IllegalArgumentException("Found object is PrismContainer, but expression is null and should be display real value");
        }
        if (valueObject == null) {
            return "";
        }
        @NotNull List<PrismValue> values = valueObject.getValues();
        StringBuilder sb = new StringBuilder();
        values.forEach(value -> {
            if (!sb.toString().isEmpty()) {
                appendMultivalueDelimiter(sb);
            }
            if (value instanceof PrismPropertyValue) {
                Object realObject = value.getRealValue();
                if (realObject == null) {
                    realObject = "";
                }
                sb.append(ReportUtils.prettyPrintForReport(realObject));
            } else if (value instanceof PrismReferenceValue) {
                sb.append(getObjectNameFromRef(((PrismReferenceValue) value).getRealValue()));
            }
        });
        return sb.toString();
    }

    private String getObjectNameFromRef(Referencable ref) {
        if (ref == null) {
            return "";
        }
        if (ref.getTargetName() != null && ref.getTargetName().getOrig() != null) {
            return ref.getTargetName().getOrig();
        }
        PrismObject object = getObjectFromReference(ref);

        if (object == null) {
            return ref.getOid();
        }

        if (object.getName() == null || object.getName().getOrig() == null) {
            return "";
        }
        return object.getName().getOrig();
    }

    protected abstract void appendMultivalueDelimiter(StringBuilder sb);

    private String evaluateExpression(ExpressionType expression, Item valueObject, Task task, OperationResult result) {
        Object object;
        if (valueObject == null) {
            object = null;
        } else {
            object = valueObject.getRealValue();
        }
        return evaluateExpression(expression, object, task, result);
    }

    private String evaluateExpression(ExpressionType expression, Object valueObject, Task task, OperationResult result) {

        ExpressionVariables variables = new ExpressionVariables();
        if (valueObject == null) {
            variables.put(ExpressionConstants.VAR_OBJECT, null, Object.class);
        } else {
            variables.put(ExpressionConstants.VAR_OBJECT, valueObject, valueObject.getClass());
        }
        Collection<String> values = null;
        try {
            values = ExpressionUtil.evaluateStringExpression(variables, getReportService().getPrismContext(), expression,
                    null, getReportService().getExpressionFactory(), "value for column", task, result);
        } catch (SchemaException | ExpressionEvaluationException | ObjectNotFoundException | CommunicationException
                | ConfigurationException | SecurityViolationException e) {
            LOGGER.error("Couldn't execute expression " + expression, e);
        }
        if (values == null || values.isEmpty()) {
            return "";
        }
        if (values.size() != 1) {
            throw new IllegalArgumentException("Expected collection with one value, but it is " + values);
        }
        return values.iterator().next();
    }


    protected String getColumnLabel(String name, PrismObjectDefinition<ObjectType> objectDefinition, ItemPath path) {
        if (path != null) {
            ItemDefinition def = objectDefinition.findItemDefinition(path);
            if (def == null) {
                throw new IllegalArgumentException("Could'n find item for path " + path);
            }
            String displayName = def.getDisplayName();
            return getMessage(displayName);
        }
        return name;
    }

    protected PrismObject<ObjectType> getObjectFromReference(Referencable ref) {
        Task task = getReportService().getTaskManager().createTaskInstance("Get object");
        Class<ObjectType> type = getReportService().getPrismContext().getSchemaRegistry().determineClassForType(ref.getType());

        if (ref.asReferenceValue().getObject() != null) {
            return ref.asReferenceValue().getObject();
        }

        PrismObject<ObjectType> object = null;
        try {
            object = getReportService().getModelService().getObject(type, ref.getOid(), null, task, task.getResult());
        } catch (Exception e) {
            LOGGER.error("Couldn't get object from objectRef " + ref, e);
        }
        return object;
    }

    protected String getStringValueByAuditColumn(AuditEventRecord record, ItemPath path,
            ExpressionType expression, Task task, OperationResult result) {
        if (expression == null) {
            return getStringValueByAuditColumn(record, path);
        }
        Object object;
        if (path == null) {
            object = record;
        } else {
            object = DefaultColumnUtils.getObjectByAuditColumn(record, path);
        }
        return evaluateExpression(expression, object, task, result);
    }

    private String getStringValueByAuditColumn(AuditEventRecord record, ItemPath path) {
        switch (path.toString()) {
            case AuditConstants.TIME_COLUMN:
                return ReportUtils.prettyPrintForReport(new Date(record.getTimestamp()));
            case AuditConstants.INITIATOR_COLUMN:
                return record.getInitiator() == null ? "" : record.getInitiator().getName().getOrig();
            case AuditConstants.EVENT_STAGE_COLUMN:
                return record.getEventStage() == null ? "" : ReportUtils.prettyPrintForReport(record.getEventStage());
            case AuditConstants.EVENT_TYPE_COLUMN:
                return record.getEventType() == null ? "" : ReportUtils.prettyPrintForReport(record.getEventType());
            case AuditConstants.TARGET_COLUMN:
                return record.getTarget() == null ? "" : getObjectNameFromRef(record.getTarget().getRealValue());
            case AuditConstants.TARGET_OWNER_COLUMN:
                return record.getTargetOwner() == null ? "" : record.getTargetOwner().getName().getOrig();
            case AuditConstants.CHANNEL_COLUMN:
                return record.getChannel() == null ? "" : ReportUtils.prettyPrintForReport(QNameUtil.uriToQName(record.getChannel()));
            case AuditConstants.OUTCOME_COLUMN:
                return record.getOutcome() == null ? "" : ReportUtils.prettyPrintForReport(record.getOutcome());
            case AuditConstants.MESSAGE_COLUMN:
                return record.getMessage() == null ? "" : record.getMessage();
            case AuditConstants.DELTA_COLUMN:
                if (record.getDeltas() == null || record.getDeltas().isEmpty()) {
                    return "";
                }
                StringBuilder sbDelta = new StringBuilder();
                Collection<ObjectDeltaOperation<? extends ObjectType>> deltas = record.getDeltas();
                Iterator<ObjectDeltaOperation<? extends ObjectType>> iterator = deltas.iterator();
                int index = 0;
                while (iterator.hasNext()) {
                    ObjectDeltaOperation delta = iterator.next();
                    sbDelta.append(ReportUtils.printDelta(delta));
                    if ((index + 1) != deltas.size()) {
                        sbDelta.append("\n");
                    }
                    index++;
                }
                return sbDelta.toString();
            case AuditConstants.TASK_OID_COLUMN:
                return record.getTaskOid() == null ? "" : record.getTaskOid();
            case AuditConstants.NODE_IDENTIFIER_COLUMN:
                return record.getNodeIdentifier() == null ? "" : record.getNodeIdentifier();
            case AuditConstants.ATTORNEY_COLUMN:
                return record.getAttorney() == null ? "" : record.getAttorney().getName().getOrig();
            case AuditConstants.RESULT_COLUMN:
                return record.getResult() == null ? "" : record.getResult();
            case AuditConstants.RESOURCE_OID_COLUMN:
                Set<String> resourceOids = record.getResourceOids();
                if (resourceOids == null || resourceOids.isEmpty()) {
                    return "";
                }
                StringBuilder sb = new StringBuilder();
                int i = 1;
                for (String oid : resourceOids) {
                    sb.append(oid);
                    if (i != resourceOids.size()) {
                        sb.append("\n");
                    }
                    i++;
                }
                return sb.toString();
            default:
                if (record.getCustomColumnProperty().containsKey(path.toString())) {
                    return record.getCustomColumnProperty().get(path.toString());
                } else {
                    LOGGER.error("Unknown name of column for AuditReport " + path);
                    throw new IllegalArgumentException("Unknown name of column for AuditReport " + path);
                }
        }
    }

    protected QName resolveTypeQname(CollectionRefSpecificationType collectionRef, CompiledObjectCollectionView compiledCollection) {
        QName type;
        if (collectionRef.getCollectionRef() != null) {
            ObjectCollectionType collection = (ObjectCollectionType) getObjectFromReference(collectionRef.getCollectionRef()).asObjectable();
            if (collection.getAuditSearch() != null) {
                type = AuditEventRecordType.COMPLEX_TYPE;
            } else {
                type = collection.getType();
            }
        } else if (collectionRef.getBaseCollectionRef() != null && collectionRef.getBaseCollectionRef().getCollectionRef() != null) {
            ObjectCollectionType collection = (ObjectCollectionType) getObjectFromReference(collectionRef.getBaseCollectionRef().getCollectionRef()).asObjectable();
            type = collection.getType();
        } else {
            type = compiledCollection.getObjectType();
        }
        if (type == null) {
            LOGGER.error("Couldn't define type for objects");
            throw new IllegalArgumentException("Couldn't define type for objects");
        }
        return type;
    }

    protected Class<ObjectType> resolveType(CollectionRefSpecificationType collectionRef, CompiledObjectCollectionView compiledCollection) {
        QName type = resolveTypeQname(collectionRef, compiledCollection);
        return (Class<ObjectType>) getReportService().getPrismContext().getSchemaRegistry()
                .getCompileTimeClassForObjectType(type);
    }
}
