/*
 * Copyright (C) 2020-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import java.util.Collection;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.util.MiscUtil;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.model.common.expression.ModelExpressionEnvironment;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.common.AuditConfiguration;
import com.evolveum.midpoint.repo.common.AuditHelper;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
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

    @Autowired private AuditHelper auditHelper;

    // "overallResult" covers the whole clockwork run
    // while "result" is - most of the time - related to the current clockwork click
    <F extends ObjectType> void audit(
            LensContext<F> context, AuditEventStage stage, Task task, OperationResult result, OperationResult overallResult)
            throws SchemaException {
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

        if (!task.isExecutionFullyPersistent()) {
            // Or, should we record the simulation deltas here? It is better done at the end, because we have all deltas there,
            // so we can have one aggregated delta per object.
            LOGGER.trace("No persistent execution, no auditing");
            return;
        }

        var focusContext = context.getFocusContext();

        PrismObject<? extends ObjectType> primaryObject;
        String primaryObjectDefaultOid; // OID may be missing in objects, as they may be immutable (but it's in the context)
        ObjectDelta<? extends ObjectType> primaryDelta;
        if (focusContext != null) {
            primaryObjectDefaultOid = focusContext.getOid();
            if (focusContext.getObjectOld() != null) {
                primaryObject = focusContext.getObjectOld();
            } else {
                primaryObject = focusContext.getObjectNew();
            }
            primaryDelta = focusContext.getSummaryDelta();
        } else {
            LensProjectionContext projection =
                    MiscUtil.extractSingletonRequired(
                            context.getProjectionContexts(),
                            () -> new IllegalStateException("No focus and more than one projection in " + context),
                            () -> new IllegalStateException("No focus and no projections in " + context));
            if (projection.getObjectOld() != null) {
                primaryObject = projection.getObjectOld();
            } else {
                primaryObject = projection.getObjectNew();
            }
            primaryObjectDefaultOid = projection.getOid();
            // TODO couldn't we determine primary object from object ADD delta? See e.g. TestModelServiceContract.test120.
            primaryDelta = projection.getCurrentDelta();
        }

        AuditEventType eventType = determineEventType(primaryDelta);

        AuditEventRecord auditRecord = new AuditEventRecord(eventType, stage);
        auditRecord.setRequestIdentifier(context.getRequestIdentifier());

        SystemConfigurationType config = context.getSystemConfigurationBean();
        AuditConfiguration auditConfiguration = auditHelper.getAuditConfiguration(config);

        if (primaryObject != null) {
            auditRecord.setTarget(primaryObject, primaryObjectDefaultOid);
            if (auditConfiguration.isRecordResourceOids()) {
                recordResourceOids(auditRecord, primaryObject.getRealValue(), context);
            }
        }

        auditRecord.setChannel(context.getChannel());

        OperationResult clone = auditHelper.cloneResultForAuditEventRecord(overallResult);

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

        auditHelper.addRecordMessage(auditRecord, clone.getMessage());

        for (SystemConfigurationAuditEventRecordingPropertyType property : auditConfiguration.getPropertiesToRecord()) {
            auditHelper.evaluateAuditRecordProperty(
                    property, auditRecord, primaryObject,
                    context.getPrivilegedExpressionProfile(), task, result);
        }

        if (auditConfiguration.getEventRecordingExpression() != null) {
            // MID-6839
            auditRecord = auditHelper.evaluateRecordingExpression(
                    auditConfiguration.getEventRecordingExpression(), auditRecord, primaryObject,
                    context.getPrivilegedExpressionProfile(),
                    (sTask, sResult) -> new ModelExpressionEnvironment<>(context, null, sTask, sResult),
                    task, result);
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

    private <F extends ObjectType> void recordResourceOids(
            AuditEventRecord auditRecord, ObjectType primaryObject, LensContext<F> context) {
        if (primaryObject instanceof FocusType) {
            FocusType focus = (FocusType) primaryObject;
            for (ObjectReferenceType linkRef : focus.getLinkRef()) {
                String shadowOid = linkRef.getOid();
                if (shadowOid != null) {
                    for (LensProjectionContext projCtx : context.findProjectionContextsByOid(shadowOid)) {
                        String resourceOid = projCtx.getResourceOid();
                        if (resourceOid != null) {
                            auditRecord.addResourceOid(resourceOid);
                        }
                    }
                }
            }
        } else if (primaryObject instanceof ShadowType) {
            ObjectReferenceType resource = ((ShadowType) primaryObject).getResourceRef();
            if (resource != null && resource.getOid() != null) {
                auditRecord.addResourceOid(resource.getOid());
            }
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
        Preconditions.checkArgument(opResult.isClosed());
        Preconditions.checkArgument(!execResult.isClosed());
        // We do not care much about the event type here. At least not now.
        AuditEventRecord auditRecord = new AuditEventRecord(AuditEventType.MODIFY_OBJECT, AuditEventStage.REQUEST);
        auditRecord.setRequestIdentifier(context.getRequestIdentifier());
        auditRecord.setChannel(context.getChannel());
        auditRecord.setOutcome(opResult.getStatus());
        auditRecord.setMessage(opResult.getMessage());
        auditHelper.audit(auditRecord, context.getNameResolver(), task, execResult);
    }
}
