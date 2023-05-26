/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import java.util.Collection;

import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationContext;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.ucf.api.Operation;
import com.evolveum.midpoint.provisioning.ucf.api.PropertyModificationOperation;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.AuditConfiguration;
import com.evolveum.midpoint.repo.common.AuditHelper;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectDeltaSchemaLevelUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@Component
public class ShadowAuditHelper {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowAuditHelper.class);

    @Autowired private AuditHelper auditHelper;
    @Autowired private PrismContext prismContext;
    @Autowired private SystemObjectCache systemObjectCache;
    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;

    public void auditEvent(@NotNull AuditEventType event, @Nullable ShadowType shadow, @NotNull ProvisioningContext ctx,
            @NotNull OperationResult result) {

        auditEvent(event, shadow, null, ctx, result);
    }

    public void auditEvent(@NotNull AuditEventType event, @Nullable ShadowType shadow, @Nullable Collection<Operation> operationsWave,
            @NotNull ProvisioningContext ctx, @NotNull OperationResult result) {

        Task task = ctx.getTask();

        SystemConfigurationType systemConfiguration;
        try {
            systemConfiguration = systemObjectCache.getSystemConfigurationBean(result);
        } catch (Exception ex) {
            throw new SystemException("Couldn't load system configuration", ex);
        }

        if (!isEnhancedShadowAuditingEnabled(systemConfiguration)) {
            return;
        }

        ProvisioningOperationContext operationContext = ctx.getOperationContext();
        if (operationContext == null) {
            operationContext = new ProvisioningOperationContext();
        }

        AuditEventRecord auditRecord = new AuditEventRecord(event, AuditEventStage.RESOURCE);
        auditRecord.setRequestIdentifier(operationContext.requestIdentifier());
        if (shadow == null && operationContext.shadowRef() != null) {
            ObjectReferenceType shadowRef = operationContext.shadowRef();
            try {
                shadow = repositoryService.getObject(ShadowType.class, shadowRef.getOid(), null, result).asObjectable();
            } catch (Exception ex) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Couldn't get shadow with reference " + shadowRef, ex);
                }
            }
        }

        if (shadow != null) {
            auditRecord.setTargetRef(new ObjectReferenceType()
                    .oid(shadow.getOid())
                    .targetName(shadow.getName())
                    .description(PolyString.getOrig(shadow.getName()))
                    .type(ShadowType.COMPLEX_TYPE).asReferenceValue());
        }
        auditRecord.setTimestamp(Clock.get().currentTimeMillis());
        auditRecord.setChannel(task.getChannel());

        ObjectDeltaOperation<ShadowType> delta = createDelta(event, shadow, operationsWave);
        if (delta != null) {
            auditRecord.addDelta(delta);
        }

        AuditConfiguration auditConfiguration = auditHelper.getAuditConfiguration(systemConfiguration);

        OperationResult clone = auditHelper.cloneResultForAuditEventRecord(result);
        auditHelper.addRecordMessage(auditRecord, clone.getMessage());

        auditRecord.setOutcome(clone.getStatus());

        if (auditConfiguration.isRecordResourceOids()) {
            auditRecord.addResourceOid(ctx.getResourceOid());
        }

        PrismObject<ShadowType> object = shadow != null ? shadow.asPrismObject() : null;

        for (SystemConfigurationAuditEventRecordingPropertyType property : auditConfiguration.getPropertiesToRecord()) {
            auditHelper.evaluateAuditRecordProperty(property, auditRecord, object,
                    operationContext.expressionProfile(), task, result);
        }

        if (auditConfiguration.getEventRecordingExpression() != null) {
            // MID-6839
            auditRecord = auditHelper.evaluateRecordingExpression(auditConfiguration.getEventRecordingExpression(), auditRecord, object,
                    operationContext.expressionProfile(), operationContext.expressionEnvironmentSupplier(), ctx.getTask(), result);
        }

        if (auditRecord == null) {
            return;
        }

        ObjectDeltaSchemaLevelUtil.NameResolver nameResolver = (objectClass, oid) -> repositoryService.getObject(objectClass, oid, null, result).getName();

        auditHelper.audit(auditRecord, nameResolver, ctx.getTask(), result);
    }

    private ObjectDeltaOperation<ShadowType> createDelta(AuditEventType event, ShadowType shadow, Collection<Operation> operations) {
        if (shadow == null) {
            return null;
        }

        PrismObject<ShadowType> object = shadow.asPrismObject();
        if (event == AuditEventType.ADD_OBJECT || event == AuditEventType.DISCOVER_OBJECT) {
            ObjectDelta<ShadowType> delta = object.createAddDelta();

            return new ObjectDeltaOperation<>(delta);
        } else if (event == AuditEventType.DELETE_OBJECT) {
            ObjectDelta<ShadowType> delta = object.createDeleteDelta();

            return new ObjectDeltaOperation<>(delta);
        } else if (event == AuditEventType.MODIFY_OBJECT) {
            ObjectDelta<ShadowType> delta = prismContext
                    .deltaFactory()
                    .object()
                    .createEmptyDelta(ShadowType.class, shadow.getOid(), ChangeType.MODIFY);

            for (Operation operation : operations) {
                if (operation instanceof PropertyModificationOperation) {
                    PropertyModificationOperation<?> propertyOp = (PropertyModificationOperation<?>) operation;
                    PropertyDelta<?> pDelta = propertyOp.getPropertyDelta().clone();
                    delta.addModification(pDelta);
                }
            }

            return new ObjectDeltaOperation<>(delta);
        }

        return null;
    }

    private boolean isEnhancedShadowAuditingEnabled(SystemConfigurationType config) {
        if (config == null || config.getAudit() == null) {
            return false;
        }

        SystemConfigurationAuditEventRecordingType eventRecording = config.getAudit().getEventRecording();
        return BooleanUtils.isTrue(eventRecording.isRecordEnhancedShadowChanges());
    }
}
