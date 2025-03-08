/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.controller;

import static com.evolveum.midpoint.model.api.ModelService.EXECUTE_CHANGE;
import static com.evolveum.midpoint.model.impl.controller.ModelController.OP_REEVALUATE_SEARCH_FILTERS;
import static com.evolveum.midpoint.prism.polystring.PolyString.toPolyString;
import static com.evolveum.midpoint.schema.GetOperationOptions.readOnly;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asPrismObject;

import java.util.ArrayList;
import java.util.Collection;

import com.evolveum.midpoint.model.common.expression.ModelExpressionEnvironment;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationContext;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.prism.xml.ns._public.types_3.EvaluationTimeType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.repo.common.AuditHelper;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.security.enforcer.api.AuthorizationParameters;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Helper class to execute the "raw" variant of {@link ModelController#executeChanges(Collection, ModelExecuteOptions, Task,
 * OperationResult)} operation.
 */
class RawChangesExecutor {

    private static final Trace LOGGER = TraceManager.getTrace(RawChangesExecutor.class);

    /** We need this inner operation result to properly audit the execution result. */
    private static final String OP_EXECUTE = RawChangesExecutor.class.getName() + ".execute";

    private final AuditHelper auditHelper = ModelBeans.get().auditHelper;
    private final ProvisioningService provisioningService = ModelBeans.get().provisioningService;
    private final SecurityEnforcer securityEnforcer = ModelBeans.get().securityEnforcer;
    private final TaskManager taskManager = ModelBeans.get().taskManager;
    private final RepositoryService cacheRepositoryService = ModelBeans.get().cacheRepositoryService;

    @NotNull private final Collection<ObjectDelta<? extends ObjectType>> requestDeltas;
    @NotNull private final Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas = new ArrayList<>();
    @Nullable private final ModelExecuteOptions options;
    @NotNull private final Task task;
    @NotNull private final String requestIdentifier = ModelImplUtils.generateRequestIdentifier();
    @Nullable private final SystemConfigurationType systemConfiguration;

    /** What targetRef to use in the audit records. */
    @Nullable private final PrismReferenceValue auditTargetRef;

    RawChangesExecutor(
            @NotNull Collection<ObjectDelta<? extends ObjectType>> requestDeltas,
            @Nullable ModelExecuteOptions options,
            @NotNull Task task,
            @NotNull OperationResult result) throws SchemaException {
        this.requestDeltas = requestDeltas;
        this.options = options;
        this.task = task;
        this.systemConfiguration = ModelBeans.get().systemObjectCache.getSystemConfigurationBean(result);
        this.auditTargetRef = ModelImplUtils.determineAuditTarget(requestDeltas);
    }

    public Collection<ObjectDeltaOperation<? extends ObjectType>> execute(OperationResult parentResult)
            throws ExpressionEvaluationException, PolicyViolationException, SecurityViolationException, SchemaException,
            ObjectNotFoundException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException {

        task.assertPersistentExecution("Raw operation execution is not supported in non-persistent execution mode");

        auditRequest(parentResult);

        OperationResult result = parentResult.createSubresult(OP_EXECUTE);
        try {
            for (ObjectDelta<? extends ObjectType> delta : requestDeltas) {
                executeChangeRaw(delta, result);
            }
            return executedDeltas;
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
            result.cleanup();

            // Here we have two contradicting requirements: The status must be taken from closed operation result,
            // and the result used to do the auditing must be open. Hence, a inner result must have been created
            // above (parentResult -> result).
            auditExecution(result.getStatus(), parentResult);
        }
    }

    private void auditRequest(OperationResult result) {
        AuditEventRecord originalRecord =
                createAuditEventRecordRaw(AuditEventStage.REQUEST, ObjectDeltaOperation.cloneDeltaCollection(requestDeltas));
        processAndAuditTheRecord(originalRecord, result);
    }

    private void auditExecution(OperationResultStatus status, OperationResult result) {
        AuditEventRecord originalRecord = createAuditEventRecordRaw(AuditEventStage.EXECUTION, executedDeltas);
        originalRecord.setTimestamp(System.currentTimeMillis());
        originalRecord.setOutcome(status);
        processAndAuditTheRecord(originalRecord, result);
    }

    private void processAndAuditTheRecord(AuditEventRecord originalRecord, OperationResult result) {
        ExpressionType auditEventRecordingExpression = getAuditEventRecordingExpression(); // MID-6839
        AuditEventRecord processedRecord;
        if (auditEventRecordingExpression != null) {
            processedRecord = auditHelper.evaluateRecordingExpression(
                    auditEventRecordingExpression, originalRecord, null, null,
                    (sTask, sResult) -> new ModelExpressionEnvironment<>(null, null, sTask, sResult), task, result);
        } else {
            processedRecord = originalRecord;
        }
        if (processedRecord != null) {
            auditHelper.audit(processedRecord, null, task, result);
        }
    }

    private ExpressionType getAuditEventRecordingExpression() {
        if (systemConfiguration == null) {
            return null;
        }
        SystemConfigurationAuditType audit = systemConfiguration.getAudit();
        if (audit == null) {
            return null;
        }
        SystemConfigurationAuditEventRecordingType eventRecording = audit.getEventRecording();
        if (eventRecording == null) {
            return null;
        }
        return eventRecording.getExpression();
    }

    private AuditEventRecord createAuditEventRecordRaw(
            AuditEventStage stage, Collection<ObjectDeltaOperation<? extends ObjectType>> deltas) {
        AuditEventRecord auditRecord = new AuditEventRecord(AuditEventType.EXECUTE_CHANGES_RAW, stage);
        auditRecord.setRequestIdentifier(requestIdentifier);
        auditRecord.setTargetRef(auditTargetRef);
        auditRecord.addDeltas(deltas);
        return auditRecord;
    }

    private void executeChangeRaw(ObjectDelta<? extends ObjectType> delta, OperationResult parentResult)
            throws CommunicationException, ObjectNotFoundException, ObjectAlreadyExistsException, PolicyViolationException,
            SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        OperationResult result = parentResult.createSubresult(EXECUTE_CHANGE);
        ObjectType objectToDetermineDetailsForAudit = null;
        try {
            applyDefinitionsIfNeeded(delta, result);
            objectToDetermineDetailsForAudit = executeChangeRawInternal(delta, options, task, result);
        } catch (Throwable t) {
            ModelImplUtils.recordException(result, t);
            throw t;
        } finally { // to have a record with the failed delta as well
            result.close();
            executedDeltas.add(
                    prepareObjectDeltaOperation(delta, objectToDetermineDetailsForAudit, parentResult, result));
        }
    }

    private ObjectDeltaOperation<? extends ObjectType> prepareObjectDeltaOperation(ObjectDelta<? extends ObjectType> delta,
            ObjectType objectToDetermineDetailsForAudit, OperationResult parentResult, OperationResult executionResult) {
        ObjectDeltaOperation<? extends ObjectType> odoToAudit = new ObjectDeltaOperation<>(delta, executionResult);
        if (objectToDetermineDetailsForAudit != null) {
            odoToAudit.setObjectName(toPolyString(objectToDetermineDetailsForAudit.getName()));
            if (objectToDetermineDetailsForAudit instanceof ShadowType shadow) {
                odoToAudit.setResourceOid(ShadowUtil.getResourceOid(shadow));
                odoToAudit.setResourceName(getResourceName(shadow, parentResult));
                odoToAudit.setShadowKind(ShadowUtil.getKind(shadow));
                odoToAudit.setShadowIntent(ShadowUtil.getIntent(shadow));
            }
        }
        return odoToAudit;
    }

    private PolyString getResourceName(ShadowType shadow, OperationResult result) {
        var ref = shadow.getResourceRef();
        if (ref == null) {
            return null;
        }
        var targetName = ref.asReferenceValue().getTargetName();
        if (targetName == null && ref.getOid() != null) {
            try {
                var resource = cacheRepositoryService.getObject(ResourceType.class, ref.getOid(),
                        GetOperationOptionsBuilder.create().readOnly().allowNotFound().build(), result);
                targetName = resource.getName();
            } catch (ObjectNotFoundException | SchemaException e) {
                LOGGER.debug("Problem reading resource {} while getting name for audit record", ref.getOid(), e);
            }
        }
        return targetName;
    }

    private ObjectType executeChangeRawInternal(
            ObjectDelta<? extends ObjectType> delta, ModelExecuteOptions options, Task task, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, ObjectAlreadyExistsException, SchemaException,
            SecurityViolationException, ConfigurationException, ExpressionEvaluationException, PolicyViolationException {
        final boolean preAuthorized = ModelExecuteOptions.isPreAuthorized(options);
        if (delta.isAdd()) {
            return executeAddDeltaRaw(delta, preAuthorized, options, task, result);
        } else if (delta.isDelete()) {
            return executeDeleteDeltaRaw(delta, preAuthorized, task, result);
        } else if (delta.isModify()) {
            return executeModifyDeltaRaw(delta, preAuthorized, options, task, result);
        } else {
            throw new IllegalArgumentException("Wrong delta type " + delta.getChangeType() + " in " + delta);
        }
    }

    private ObjectType executeAddDeltaRaw(ObjectDelta<? extends ObjectType> delta,
            boolean preAuthorized, ModelExecuteOptions options, Task task, OperationResult result1)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException, ObjectAlreadyExistsException {
        RepoAddOptions repoOptions = new RepoAddOptions();
        if (ModelExecuteOptions.isNoCrypt(options)) {
            repoOptions.setAllowUnencryptedValues(true);
        }
        if (ModelExecuteOptions.isOverwrite(options)) {
            repoOptions.setOverwrite(true);
        }
        PrismObject<? extends ObjectType> objectToAdd = delta.getObjectToAdd();
        if (!preAuthorized) {
            securityEnforcer.authorize(
                    ModelAuthorizationAction.RAW_OPERATION.getUrl(),
                    null, AuthorizationParameters.Builder.buildObjectAdd(objectToAdd), task, result1);
            securityEnforcer.authorize(
                    ModelAuthorizationAction.ADD.getUrl(),
                    null, AuthorizationParameters.Builder.buildObjectAdd(objectToAdd), task, result1);
        }
        String oid;
        try {
            if (objectToAdd.canRepresent(TaskType.class)) {
                //noinspection unchecked
                oid = taskManager.addTask((PrismObject<TaskType>) objectToAdd, result1);
            } else {
                oid = cacheRepositoryService.addObject(objectToAdd, repoOptions, result1);
            }
            task.recordObjectActionExecuted(objectToAdd, null, oid, ChangeType.ADD, task.getChannel(), null);
        } catch (Throwable t) {
            task.recordObjectActionExecuted(objectToAdd, null, null, ChangeType.ADD, task.getChannel(), t);
            throw t;
        }
        delta.setOid(oid);
        return objectToAdd.asObjectable();
    }

    private <T extends ObjectType> T executeDeleteDeltaRaw(
            ObjectDelta<T> delta, boolean preAuthorized, Task task, OperationResult result)
            throws PolicyViolationException, CommunicationException, ObjectNotFoundException, SchemaException,
            SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        QNameUtil.setTemporarilyTolerateUndeclaredPrefixes(true);  // MID-2218
        Class<T> clazz = delta.getObjectTypeClass();
        String oid = delta.getOid();
        T objectToDetermineDetailsForAudit = null;
        try {
            T existingObject;
            try {
                existingObject = cacheRepositoryService
                        .getObject(clazz, oid, readOnly(), result)
                        .asObjectable();
                objectToDetermineDetailsForAudit = existingObject;
            } catch (Throwable t) {
                if (!securityEnforcer.isAuthorizedAll(task, result)) {
                    throw t;
                } else {
                    existingObject = PrismContext.get().createObjectable(clazz);
                    existingObject.setOid(oid);
                    existingObject.setName(PolyStringType.fromOrig("Unreadable object"));
                    // in case of administrator's request we continue - in order to allow deleting malformed (unreadable) objects
                    // creating "shadow" existing object for auditing needs.
                }
            }
            ModelController.checkIndestructible(existingObject);
            if (!preAuthorized) {
                securityEnforcer.authorize(
                        ModelAuthorizationAction.RAW_OPERATION.getUrl(), null,
                        AuthorizationParameters.Builder.buildObjectDelete(asPrismObject(existingObject)), task, result);
                securityEnforcer.authorize(
                        ModelAuthorizationAction.DELETE.getUrl(), null,
                        AuthorizationParameters.Builder.buildObjectDelete(asPrismObject(existingObject)), task, result);
            }
            try {
                if (ObjectTypes.isClassManagedByProvisioning(clazz)) {
                    ModelImplUtils.clearRequestee(task);

                    ProvisioningOperationContext ctx = new ProvisioningOperationContext()
                            .requestIdentifier(requestIdentifier)
                            .expressionEnvironmentSupplier((sTask, sResult) ->
                                    new ModelExpressionEnvironment<>(null, null, sTask, sResult));

                    provisioningService.deleteObject(
                            clazz, oid, ProvisioningOperationOptions.createRaw(), null, ctx, task, result);
                } else if (TaskType.class.isAssignableFrom(clazz)) {
                    // Maybe we should check if the task is not running. However, this is raw processing.
                    // (But, actually, this is better than simply deleting the task from repository.)
                    taskManager.deleteTask(oid, result);
                } else {
                    cacheRepositoryService.deleteObject(clazz, oid, result);
                }
                task.recordObjectActionExecuted(
                        asPrismObject(objectToDetermineDetailsForAudit), clazz, oid, ChangeType.DELETE, task.getChannel(), null);
            } catch (Throwable t) {
                task.recordObjectActionExecuted(
                        asPrismObject(objectToDetermineDetailsForAudit), clazz, oid, ChangeType.DELETE, task.getChannel(), t);
                throw t;
            }
        } finally {
            QNameUtil.setTemporarilyTolerateUndeclaredPrefixes(false);
        }
        return objectToDetermineDetailsForAudit;
    }

    private <T extends ObjectType> T executeModifyDeltaRaw(ObjectDelta<T> delta,
            boolean preAuthorized, ModelExecuteOptions options, Task task, OperationResult result)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ConfigurationException,
            CommunicationException, SecurityViolationException, ExpressionEvaluationException {
        QNameUtil.setTemporarilyTolerateUndeclaredPrefixes(true); // MID-2218
        Class<T> clazz = delta.getObjectTypeClass();
        String oid = delta.getOid();
        T objectToDetermineDetailsForAudit;
        try {
            T existingObject = cacheRepositoryService
                    .getObject(clazz, oid, readOnly(), result)
                    .asObjectable();
            objectToDetermineDetailsForAudit = existingObject;
            if (!preAuthorized) {
                //noinspection unchecked
                AuthorizationParameters<T, ObjectType> autzParams =
                        AuthorizationParameters.Builder.buildObjectDelta((PrismObject<T>) existingObject.asPrismObject(), delta, true);
                securityEnforcer.authorize(
                        ModelAuthorizationAction.RAW_OPERATION.getUrl(), null, autzParams, task, result);
                securityEnforcer.authorize(
                        ModelAuthorizationAction.MODIFY.getUrl(), null, autzParams, task, result);
            }
            try {
                if (TaskType.class.isAssignableFrom(clazz)) {
                    taskManager.modifyTask(oid, delta.getModifications(), result);
                } else {
                    cacheRepositoryService.modifyObject(clazz, oid, delta.getModifications(), result);
                }
                task.recordObjectActionExecuted(asPrismObject(existingObject), ChangeType.MODIFY, null);
            } catch (Throwable t) {
                task.recordObjectActionExecuted(asPrismObject(existingObject), ChangeType.MODIFY, t);
                throw t;
            }
        } finally {
            QNameUtil.setTemporarilyTolerateUndeclaredPrefixes(false);
        }
        if (ModelExecuteOptions.isReevaluateSearchFilters(options)) { // treat filters that already exist in the object (case #2 above)
            reevaluateSearchFilters(clazz, oid, task, result);
        }
        return objectToDetermineDetailsForAudit;
    }

    private <T extends ObjectType> void reevaluateSearchFilters(
            Class<T> objectTypeClass, String oid, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        OperationResult result = parentResult.createSubresult(OP_REEVALUATE_SEARCH_FILTERS);
        try {
            PrismObject<T> storedObject =
                    cacheRepositoryService.getObject(objectTypeClass, oid, readOnly(), result);
            PrismObject<T> updatedObject = storedObject.clone();
            ModelImplUtils.resolveReferences(
                    updatedObject,
                    cacheRepositoryService,
                    false,
                    true,
                    EvaluationTimeType.IMPORT,
                    true,
                    result);
            ObjectDelta<T> delta = storedObject.diff(updatedObject);
            LOGGER.trace("reevaluateSearchFilters found delta: {}", delta.debugDumpLazily());
            if (!delta.isEmpty()) {
                try {
                    cacheRepositoryService.modifyObject(objectTypeClass, oid, delta.getModifications(), result);
                    task.recordObjectActionExecuted(updatedObject, ChangeType.MODIFY, null);
                } catch (Throwable t) {
                    task.recordObjectActionExecuted(updatedObject, ChangeType.MODIFY, t);
                    throw t;
                }
            }
            result.recordSuccess();
        } catch (SchemaException | ObjectNotFoundException | ObjectAlreadyExistsException | RuntimeException e) {
            result.recordFatalError("Couldn't reevaluate search filters: " + e.getMessage(), e);
            throw e;
        } finally {
            result.close();
        }
    }

    private void applyDefinitionsIfNeeded(ObjectDelta<? extends ObjectType> delta, OperationResult result) {
        // MID-2486
        Class<? extends ObjectType> type = delta.getObjectTypeClass();
        if (type == ShadowType.class || type == ResourceType.class) {
            try {
                provisioningService.applyDefinition(delta, task, result);
            } catch (Exception e) {
                // we can tolerate this - if there's a real problem with definition, repo call below will fail
                LoggingUtils.logExceptionAsWarning(
                        LOGGER, "Couldn't apply definition on shadow/resource raw-mode delta {} -- continuing "
                                + "the operation.", e, delta);
                result.muteLastSubresultError();
            }
        }
    }
}
