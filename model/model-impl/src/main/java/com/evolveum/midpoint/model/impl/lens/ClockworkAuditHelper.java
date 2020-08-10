/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens;

import static java.util.Collections.emptyList;

import java.util.Collection;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.model.impl.util.AuditHelper;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Audit-related responsibilities during clockwork processing.
 */
@Component
public class ClockworkAuditHelper {

    private static final Trace LOGGER = TraceManager.getTrace(ClockworkAuditHelper.class);

    @Autowired private PrismContext prismContext;
    @Autowired private AuditHelper auditHelper;
    @Autowired private ExpressionFactory expressionFactory;

    // "overallResult" covers the whole clockwork run
    // while "result" is - most of the time - related to the current clockwork click
    <F extends ObjectType> void audit(LensContext<F> context, AuditEventStage stage, Task task, OperationResult result,
            OperationResult overallResult) throws SchemaException {
        if (context.isLazyAuditRequest()) {
            if (stage == AuditEventStage.REQUEST) {
                // We skip auditing here, we will do it before execution
            } else if (stage == AuditEventStage.EXECUTION) {
                if (context.getUnauditedExecutedDeltas().isEmpty()) {
                    // No deltas, nothing to audit in this wave
                    return;
                }
                if (!context.isRequestAudited()) {
                    auditEvent(context, AuditEventStage.REQUEST, context.getStats().getRequestTimestamp(), false, task, result, overallResult);
                }
                auditEvent(context, stage, null, false, task, result, overallResult);
            }
        } else {
            auditEvent(context, stage, null, false, task, result, overallResult);
        }
    }

    /**
     * Make sure that at least one execution is audited if a request was already audited. We don't want
     * request without execution in the audit logs.
     */
    <F extends ObjectType> void auditFinalExecution(LensContext<F> context, Task task, OperationResult result,
            OperationResult overallResult) throws SchemaException {
        if (context.isRequestAudited() && !context.isExecutionAudited()) {
            auditEvent(context, AuditEventStage.EXECUTION, null, true, task, result, overallResult);
        }
    }

    // "overallResult" covers the whole clockwork run
    // while "result" is - most of the time - related to the current clockwork click
    //
    // We provide "result" here just for completeness - if any of the called methods would like to record to it.
    <F extends ObjectType> void auditEvent(LensContext<F> context, AuditEventStage stage,
            XMLGregorianCalendar timestamp, boolean alwaysAudit, Task task, OperationResult result,
            OperationResult overallResult) throws SchemaException {

        PrismObject<? extends ObjectType> primaryObject;
        ObjectDelta<? extends ObjectType> primaryDelta;
        if (context.getFocusContext() != null) {
            if (context.getFocusContext().getObjectOld() != null) {
                primaryObject = context.getFocusContext().getObjectOld();
            } else {
                primaryObject = context.getFocusContext().getObjectNew();
            }
            primaryDelta = context.getFocusContext().getSummaryDelta();
        } else {
            Collection<LensProjectionContext> projectionContexts = context.getProjectionContexts();
            if (projectionContexts == null || projectionContexts.isEmpty()) {
                throw new IllegalStateException("No focus and no projections in " + context);
            }
            if (projectionContexts.size() > 1) {
                throw new IllegalStateException("No focus and more than one projection in " + context);
            }
            LensProjectionContext projection = projectionContexts.iterator().next();
            if (projection.getObjectOld() != null) {
                primaryObject = projection.getObjectOld();
            } else {
                primaryObject = projection.getObjectNew();
            }
            primaryDelta = projection.getCurrentDelta();
        }

        AuditEventType eventType = determineEventType(primaryDelta);

        AuditEventRecord auditRecord = new AuditEventRecord(eventType, stage);
        auditRecord.setRequestIdentifier(context.getRequestIdentifier());

        boolean recordResourceOids;
        List<SystemConfigurationAuditEventRecordingPropertyType> propertiesToRecord;
        SystemConfigurationType config = context.getSystemConfigurationType();
        if (config != null && config.getAudit() != null && config.getAudit().getEventRecording() != null) {
            SystemConfigurationAuditEventRecordingType eventRecording = config.getAudit().getEventRecording();
            recordResourceOids = Boolean.TRUE.equals(eventRecording.isRecordResourceOids());
            propertiesToRecord = eventRecording.getProperty();
        } else {
            recordResourceOids = false;
            propertiesToRecord = emptyList();
        }

        if (primaryObject != null) {
            auditRecord.setTarget(primaryObject, prismContext);
            if (recordResourceOids) {
                if (primaryObject.getRealValue() instanceof FocusType) {
                    FocusType focus = (FocusType) primaryObject.getRealValue();
                    for (ObjectReferenceType shadowRef : focus.getLinkRef()) {
                        LensProjectionContext projectionContext = context.findProjectionContextByOid(shadowRef.getOid());
                        if (projectionContext != null && StringUtils.isNotBlank(projectionContext.getResourceOid())) {
                            auditRecord.addResourceOid(projectionContext.getResourceOid());
                        }
                    }
                } else if (primaryObject.getRealValue() instanceof ShadowType) {
                    ObjectReferenceType resource = ((ShadowType) primaryObject.getRealValue()).getResourceRef();
                    if (resource != null && resource.getOid() != null) {
                        auditRecord.addResourceOid(resource.getOid());
                    }
                }
            }
        }

        auditRecord.setChannel(context.getChannel());

        // This is a brutal hack -- FIXME: create some "compute in-depth preview" method on operation result
        OperationResult clone = overallResult.clone(2, false);
        for (OperationResult subresult : clone.getSubresults()) {
            subresult.computeStatusIfUnknown();
        }
        clone.computeStatus();

        if (stage == AuditEventStage.REQUEST) {
            Collection<ObjectDeltaOperation<? extends ObjectType>> clonedDeltas = ObjectDeltaOperation.cloneDeltaCollection(context.getPrimaryChanges());
            checkNamesArePresent(clonedDeltas, primaryObject);
            auditRecord.addDeltas(clonedDeltas);
            if (auditRecord.getTargetRef() == null) {
                auditRecord.setTargetRef(ModelImplUtils.determineAuditTargetDeltaOps(clonedDeltas, context.getPrismContext()));
            }
        } else if (stage == AuditEventStage.EXECUTION) {
            auditRecord.setOutcome(clone.getStatus());
            Collection<ObjectDeltaOperation<? extends ObjectType>> unauditedExecutedDeltas = context.getUnauditedExecutedDeltas();
            if (!alwaysAudit && unauditedExecutedDeltas.isEmpty()) {
                // No deltas, nothing to audit in this wave
                return;
            }
            Collection<ObjectDeltaOperation<? extends ObjectType>> clonedDeltas = ObjectDeltaOperation.cloneCollection(unauditedExecutedDeltas);
            checkNamesArePresent(clonedDeltas, primaryObject);
            auditRecord.addDeltas(clonedDeltas);
        } else {
            throw new IllegalStateException("Unknown audit stage " + stage);
        }

        if (timestamp != null) {
            auditRecord.setTimestamp(XmlTypeConverter.toMillis(timestamp));
        }

        addRecordMessage(auditRecord, clone.getMessage());

        for (SystemConfigurationAuditEventRecordingPropertyType property : propertiesToRecord) {
            String name = property.getName();
            if (StringUtils.isBlank(name)) {
                throw new IllegalArgumentException("Name of SystemConfigurationAuditEventRecordingPropertyType is empty or null in " + property);
            }
            ExpressionType expression = property.getExpression();
            if (expression != null) {
                ExpressionVariables variables = new ExpressionVariables();
                variables.put(ExpressionConstants.VAR_TARGET, primaryObject, PrismObject.class);
                variables.put(ExpressionConstants.VAR_AUDIT_RECORD, auditRecord, AuditEventRecord.class);
                String shortDesc = "value for custom column of audit table";
                try {
                    Collection<String> values = ExpressionUtil.evaluateStringExpression(variables, prismContext, expression, context.getPrivilegedExpressionProfile(), expressionFactory, shortDesc, task, result);
                    if (values != null && !values.isEmpty()) {
                        if (values.size() > 1) {
                            throw new IllegalArgumentException("Collection of expression result contains more as one value");
                        }
                        auditRecord.getCustomColumnProperty().put(name, values.iterator().next());
                    }
                } catch (CommonException e) {
                    LOGGER.error("Couldn't evaluate Expression " + expression.toString(), e);
                }
            }
        }

        auditHelper.audit(auditRecord, context.getNameResolver(), task, result);

        if (stage == AuditEventStage.EXECUTION) {
            // We need to clean up so these deltas will not be audited again in next wave
            context.markExecutedDeltasAudited();
            context.setExecutionAudited(true);
        } else {
            assert stage == AuditEventStage.REQUEST;
            context.setRequestAudited(true);
        }
    }

    @NotNull
    private AuditEventType determineEventType(ObjectDelta<? extends ObjectType> primaryDelta) {
        AuditEventType eventType;
        if (primaryDelta == null) {
            eventType = AuditEventType.SYNCHRONIZATION;
        } else if (primaryDelta.isAdd()) {
            eventType = AuditEventType.ADD_OBJECT;
        } else if (primaryDelta.isModify()) {
            eventType = AuditEventType.MODIFY_OBJECT;
        } else if (primaryDelta.isDelete()) {
            eventType = AuditEventType.DELETE_OBJECT;
        } else {
            throw new IllegalStateException("Unknown state of delta " + primaryDelta);
        }
        return eventType;
    }

    private void checkNamesArePresent(Collection<ObjectDeltaOperation<? extends ObjectType>> deltas, PrismObject<? extends ObjectType> primaryObject) {
        if (primaryObject != null) {
            for (ObjectDeltaOperation<? extends ObjectType> delta : deltas) {
                if (delta.getObjectName() == null) {
                    delta.setObjectName(primaryObject.getName());
                }
            }
        }
    }

    /**
     * Adds a message to the record by pulling the messages from individual delta results.
     */
    private void addRecordMessage(AuditEventRecord auditRecord, String message) {
        if (auditRecord.getMessage() != null) {
            return;
        }
        if (!StringUtils.isEmpty(message)) {
            auditRecord.setMessage(message);
            return;
        }
        Collection<ObjectDeltaOperation<? extends ObjectType>> deltas = auditRecord.getDeltas();
        if (deltas.isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (ObjectDeltaOperation<? extends ObjectType> delta : deltas) {
            OperationResult executionResult = delta.getExecutionResult();
            if (executionResult != null) {
                String deltaMessage = executionResult.getMessage();
                if (!StringUtils.isEmpty(deltaMessage)) {
                    if (sb.length() != 0) {
                        sb.append("; ");
                    }
                    sb.append(deltaMessage);
                }
            }
        }
        auditRecord.setMessage(sb.toString());
    }
}
