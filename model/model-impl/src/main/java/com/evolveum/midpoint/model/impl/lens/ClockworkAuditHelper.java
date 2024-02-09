/*
 * Copyright (C) 2020-2021 Evolveum and contributors
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
import org.springframework.beans.factory.annotation.Qualifier;
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
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Audit-related responsibilities during clockwork processing.
 */
@Component
public class ClockworkAuditHelper {

    private static final Trace LOGGER = TraceManager.getTrace(ClockworkAuditHelper.class);

    private static final String OP_EVALUATE_AUDIT_RECORD_PROPERTY =
            ClockworkAuditHelper.class.getName() + ".evaluateAuditRecordProperty";

    @Autowired private PrismContext prismContext;
    @Autowired private AuditHelper auditHelper;
    @Autowired private ExpressionFactory expressionFactory;
    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;

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
            OperationResult overallResult) {
        if (context.isRequestAudited() && !context.isExecutionAudited()) {
            auditEvent(context, AuditEventStage.EXECUTION, null, true, task, result, overallResult);
        }
    }

    // "overallResult" covers the whole clockwork run
    // while "result" is - most of the time - related to the current clockwork click
    //
    // We provide "result" here just for completeness - if any of the called methods would like to record to it.
    <F extends ObjectType> void auditEvent(
            LensContext<F> context, AuditEventStage stage, XMLGregorianCalendar timestamp,
            boolean alwaysAudit, Task task, OperationResult result, OperationResult overallResult) {

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
            if (projectionContexts.isEmpty()) {
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
            // TODO couldn't we determine primary object from object ADD delta? See e.g. TestModelServiceContract.test120.
            primaryDelta = projection.getCurrentDelta();
        }

        AuditEventType eventType = determineEventType(primaryDelta);

        AuditEventRecord auditRecord = new AuditEventRecord(eventType, stage);
        auditRecord.setRequestIdentifier(context.getRequestIdentifier());

        boolean recordResourceOids;
        List<SystemConfigurationAuditEventRecordingPropertyType> propertiesToRecord;
        ExpressionType eventRecordingExpression = null;

        SystemConfigurationType config = context.getSystemConfigurationBean();
        if (config != null && config.getAudit() != null && config.getAudit().getEventRecording() != null) {
            SystemConfigurationAuditEventRecordingType eventRecording = config.getAudit().getEventRecording();
            recordResourceOids = Boolean.TRUE.equals(eventRecording.isRecordResourceOids());
            propertiesToRecord = eventRecording.getProperty();
            eventRecordingExpression = eventRecording.getExpression();
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
                auditRecord.setTargetRef(ModelImplUtils.determineAuditTargetDeltaOps(clonedDeltas));
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
            evaluateAuditRecordProperty(property, auditRecord, primaryObject, context, task, result);
        }

        if (eventRecordingExpression != null) {
            // MID-6839
            auditRecord = auditHelper.evaluateRecordingExpression(eventRecordingExpression,
                    auditRecord, primaryObject, context, task, result);
        }

        if (auditRecord != null) {
            auditHelper.audit(auditRecord, context.getNameResolver(), task, result);
        }

        if (stage == AuditEventStage.EXECUTION) {
            // We need to clean up so these deltas will not be audited again in next wave
            context.markExecutedDeltasAudited();
            context.setExecutionAudited(true);
        } else {
            assert stage == AuditEventStage.REQUEST;
            context.setRequestAudited(true);
        }
    }

    private <F extends ObjectType> void evaluateAuditRecordProperty(SystemConfigurationAuditEventRecordingPropertyType propertyDef,
            AuditEventRecord auditRecord, PrismObject<? extends ObjectType> primaryObject, LensContext<F> context, Task task,
            OperationResult parentResult) {
        String name = propertyDef.getName();
        OperationResult result = parentResult.subresult(OP_EVALUATE_AUDIT_RECORD_PROPERTY)
                .addParam("name", name)
                .setMinor()
                .build();
        try {
            if (StringUtils.isBlank(name)) {
                throw new IllegalArgumentException("Name of SystemConfigurationAuditEventRecordingPropertyType is empty or null in " + propertyDef);
            }
            if (!targetSelectorMatches(propertyDef.getTargetSelector(), primaryObject)) {
                result.recordNotApplicable();
                return;
            }
            ExpressionType expression = propertyDef.getExpression();
            if (expression != null) {
                VariablesMap variables = new VariablesMap();
                variables.put(ExpressionConstants.VAR_TARGET, primaryObject, PrismObject.class);
                variables.put(ExpressionConstants.VAR_AUDIT_RECORD, auditRecord, AuditEventRecord.class);
                String shortDesc = "value for custom column of audit table";
                Collection<String> values = ExpressionUtil.evaluateStringExpression(variables, prismContext, expression,
                        context.getPrivilegedExpressionProfile(), expressionFactory, shortDesc, task, result);
                if (values == null || values.isEmpty()) {
                    // nothing to do
                } else if (values.size() == 1) {
                    auditRecord.getCustomColumnProperty().put(name, values.iterator().next());
                } else {
                    throw new IllegalArgumentException("Collection of expression result contains more than one value");
                }
            }
        } catch (Throwable t) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't evaluate audit record property expression {}", t, name);
            // Intentionally not throwing the exception. The error is marked as partial.
            // (It would be better to mark it as fatal and to derive overall result as partial, but we aren't that far yet.)
            result.recordPartialError(t);
        } finally {
            result.recordSuccessIfUnknown();
        }
    }

    private boolean targetSelectorMatches(List<ObjectSelectorType> targetSelectors,
            PrismObject<? extends ObjectType> primaryObject) throws CommunicationException, ObjectNotFoundException,
            SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        if (targetSelectors.isEmpty()) {
            return true;
        }
        for (ObjectSelectorType targetSelector : targetSelectors) {
            if (repositoryService.selectorMatches(targetSelector, primaryObject, null, LOGGER, "target selector")) {
                return true;
            }
        }
        LOGGER.debug("No selector matches for {}", primaryObject);
        return false;
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

    /**
     * The request was denied before its execution was even started.
     * This is a special case.
     *
     * @param opResult The (closed) result of the operation that failed
     * @param execResult The (open) result under which the auditing should be done
     */
    public void auditRequestDenied(
            @NotNull LensContext<? extends ObjectType> context,
            @NotNull Task task,
            @NotNull OperationResult opResult,
            @NotNull OperationResult execResult) {
        // We do not care much about the event type here. At least not now.
        AuditEventRecord auditRecord = new AuditEventRecord(AuditEventType.MODIFY_OBJECT, AuditEventStage.REQUEST);
        auditRecord.setRequestIdentifier(context.getRequestIdentifier());
        auditRecord.setChannel(context.getChannel());
        auditRecord.setOutcome(opResult.getStatus());
        auditRecord.setMessage(opResult.getMessage());
        auditHelper.audit(auditRecord, context.getNameResolver(), task, execResult);
    }
}
