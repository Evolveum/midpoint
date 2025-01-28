/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import java.util.Collection;

import com.evolveum.midpoint.prism.Referencable;

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
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import static com.evolveum.midpoint.schema.GetOperationOptions.createReadOnlyCollection;

@Component
public class ShadowAuditHelper {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowAuditHelper.class);

    @Autowired private AuditHelper auditHelper;
    @Autowired private PrismContext prismContext;
    @Autowired private SystemObjectCache systemObjectCache;
    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;

    /** For actions without specific UCF modify operations, like ADD, DELETE, DISCOVER. */
    public void auditEvent(
            @NotNull AuditEventType event, @Nullable ShadowType shadow, @NotNull ProvisioningContext ctx,
            @NotNull OperationResult result) throws SchemaException {

        auditEvent(event, shadow, null, ctx, result);
    }

    /** For actions where we have specific UCF modify operations, like object MODIFY. */
    public void auditEvent(
            @NotNull AuditEventType event,
            @Nullable ShadowType shadow,
            @Nullable Collection<Operation> ucfOperationsExecuted,
            @NotNull ProvisioningContext ctx,
            @NotNull OperationResult result) throws SchemaException {

        Task task = ctx.getTask();
        SystemConfigurationType systemConfiguration = systemObjectCache.getSystemConfigurationBean(result);

        if (!isRecordResourceStageEnabled(systemConfiguration)) {
            return;
        }

        ProvisioningOperationContext operationContext = ctx.getOperationContext();

        AuditEventRecord auditRecord = new AuditEventRecord(event, AuditEventStage.RESOURCE);
        auditRecord.setRequestIdentifier(operationContext.requestIdentifier());
        if (shadow == null) {
            ObjectReferenceType associationShadowRef = ctx.getAssociationShadowRef();
            String associationShadowOid = Referencable.getOid(associationShadowRef);
            if (associationShadowOid != null) {
                try {
                    shadow = repositoryService
                            .getObject(ShadowType.class, associationShadowOid, null, result)
                            .asObjectable();
                } catch (Exception ex) {
                    LOGGER.debug("Couldn't get association shadow with reference {}", associationShadowRef, ex);
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

        OperationResult clone = auditHelper.cloneResultForAuditEventRecord(result);
        auditHelper.addRecordMessage(auditRecord, clone.getMessage());

        auditRecord.setOutcome(clone.getStatus());

        ObjectDeltaOperation<ShadowType> delta = createDelta(event, shadow, ucfOperationsExecuted, clone, ctx);
        if (delta != null) {
            auditRecord.addDelta(delta);
        }

        AuditConfiguration auditConfiguration = auditHelper.getAuditConfiguration(systemConfiguration);

        if (auditConfiguration.isRecordResourceOids()) {
            auditRecord.addResourceOid(ctx.getResourceOid());
        }

        PrismObject<ShadowType> object = shadow != null ? shadow.asPrismObject() : null;

        for (SystemConfigurationAuditEventRecordingPropertyType property : auditConfiguration.getPropertiesToRecord()) {
            auditHelper.evaluateAuditRecordProperty(
                    property, auditRecord, object, operationContext.expressionProfile(), task, result);
        }

        if (auditConfiguration.getEventRecordingExpression() != null) {
            // MID-6839
            auditRecord = auditHelper.evaluateRecordingExpression(
                    auditConfiguration.getEventRecordingExpression(),
                    auditRecord,
                    object,
                    operationContext.expressionProfile(),
                    operationContext.expressionEnvironmentSupplier(),
                    ctx.getTask(),
                    result);
        }

        if (auditRecord == null) {
            return;
        }
        // TODO: We should have better API for name resolution instead of getObject()
        ObjectDeltaSchemaLevelUtil.NameResolver nameResolver =
                (objectClass, oid, lResult) ->
                        // Using read-only options to avoid cloning when fetching from the cache
                        repositoryService
                                .getObject(objectClass, oid, createReadOnlyCollection(), lResult)
                                .getName();

        auditHelper.audit(auditRecord, nameResolver, ctx.getTask(), result);
    }

    private ObjectDeltaOperation<ShadowType> createDelta(
            AuditEventType event,
            ShadowType shadow,
            Collection<Operation> operations,
            OperationResult result,
            @NotNull ProvisioningContext ctx) {
        if (shadow == null) {
            return null;
        }

        ObjectDelta<ShadowType> delta = null;
        PrismObject<ShadowType> object = shadow.asPrismObject();
        if (event == AuditEventType.ADD_OBJECT || event == AuditEventType.DISCOVER_OBJECT) {
            delta = object.createAddDelta();
        } else if (event == AuditEventType.DELETE_OBJECT) {
            delta = object.createDeleteDelta();
        } else if (event == AuditEventType.MODIFY_OBJECT) {
            delta = prismContext
                    .deltaFactory()
                    .object()
                    .createEmptyDelta(ShadowType.class, shadow.getOid(), ChangeType.MODIFY);

            for (Operation operation : operations) {
                if (operation instanceof PropertyModificationOperation<?> propertyOp) {
                    delta.addModification(
                            propertyOp.getPropertyDelta().clone());
                }
            }
        }

        if (delta != null) {
            ObjectDeltaOperation<ShadowType> deltaOperation = new ObjectDeltaOperation<>(delta, result);
            deltaOperation.setObjectName(object.getName());
            deltaOperation.setResourceName(ctx.getResource().getName().toPolyString());
            return deltaOperation;
        }

        return null;
    }

    private boolean isRecordResourceStageEnabled(SystemConfigurationType config) {
        if (config == null || config.getAudit() == null) {
            return true;
        }

        SystemConfigurationAuditEventRecordingType eventRecording = config.getAudit().getEventRecording();
        return eventRecording == null || BooleanUtils.isNotFalse(eventRecording.isRecordResourceStageChanges());
    }
}
