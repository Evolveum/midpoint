/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens;

import java.util.*;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.schema.constants.ObjectTypes;

import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Responsible for maintaining "operation execution" records.
 */
@Component
public class OperationExecutionRecorder {

    private static final Trace LOGGER = TraceManager.getTrace(OperationExecutionRecorder.class);

    @Autowired private Clock clock;
    @Autowired private SystemObjectCache systemObjectCache;
    @Autowired private PrismContext prismContext;
    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;

    private static final int DEFAULT_NUMBER_OF_RESULTS_TO_KEEP = 5;

    <F extends ObjectType> void recordOperationExecution(LensContext<F> context, Throwable clockworkException,
            Task task, OperationResult result) {
        boolean skip = context.getInternalsConfiguration() != null &&
                context.getInternalsConfiguration().getOperationExecutionRecording() != null &&
                Boolean.TRUE.equals(context.getInternalsConfiguration().getOperationExecutionRecording().isSkip());
        if (!skip) {
            XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
            try {
                LOGGER.trace("recordOperationExecution starting; task = {}, clockworkException = {}", task, clockworkException);
                boolean opRecordedIntoFocus = recordFocusOperationExecution(context, now, clockworkException, task, result);
                for (LensProjectionContext projectionContext : context.getProjectionContexts()) {
                    Throwable exceptionToProjection;
                    if (clockworkException != null && !opRecordedIntoFocus && projectionContext.isSynchronizationSource()) {
                        // We need to record the exception somewhere. Because we were not able to put it into focus,
                        // we have to do it into sync-source projection.
                        exceptionToProjection = clockworkException;
                    } else {
                        exceptionToProjection = null;
                    }
                    recordProjectionOperationExecution(context, projectionContext, now, exceptionToProjection, task, result);
                }
            } catch (Throwable t) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't record operation execution. Model context:\n{}", t,
                        context.debugDump());
                // Let us ignore this for the moment. It should not have happened, sure. But it's not that crucial.
                // Administrator will be able to learn about the problem from the log.
            }
        } else {
            LOGGER.trace("Skipping operation execution recording (as set in system configuration)");
        }
    }

    /**
     * @return true if the operation execution was recorded (or would be recorded, but skipped because of the configuration)
     */
    private <F extends ObjectType> boolean recordFocusOperationExecution(LensContext<F> context, XMLGregorianCalendar now,
            Throwable clockworkException, Task task, OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
        LensFocusContext<F> focusContext = context.getFocusContext();
        if (focusContext == null || focusContext.isDelete()) {
            LOGGER.trace("focusContext is null or 'delete', not recording focus operation execution");
            return false;
        }
        PrismObject<F> objectNew = focusContext.getObjectNew();
        Validate.notNull(objectNew, "No focus object even if the context is not of 'delete' type");

        //noinspection unchecked
        List<LensObjectDeltaOperation<F>> executedDeltas = getExecutedDeltas(focusContext,
                (Class<F>) objectNew.asObjectable().getClass(), clockworkException, result);
        LOGGER.trace("recordFocusOperationExecution: executedDeltas: {}", executedDeltas.size());
        return recordOperationExecution(context, objectNew, false, executedDeltas, now, task, result);
    }

    @NotNull
    private <O extends ObjectType> List<LensObjectDeltaOperation<O>> getExecutedDeltas(LensElementContext<O> elementContext,
            Class<O> objectClass, Throwable clockworkException, OperationResult result) {
        List<LensObjectDeltaOperation<O>> executedDeltas;
        if (clockworkException != null) {
            executedDeltas = new ArrayList<>(elementContext.getExecutedDeltas());
            LensObjectDeltaOperation<O> odo = new LensObjectDeltaOperation<>();
            ObjectDelta<O> primaryDelta = elementContext.getPrimaryDelta();
            if (primaryDelta != null) {
                odo.setObjectDelta(primaryDelta);
            } else {
                ObjectDelta<O> fakeDelta = prismContext.deltaFactory().object().create(objectClass, ChangeType.MODIFY);
                odo.setObjectDelta(fakeDelta);
            }
            odo.setExecutionResult(result);        // we rely on the fact that 'result' already contains record of the exception
            executedDeltas.add(odo);
        } else {
            executedDeltas = elementContext.getExecutedDeltas();
        }
        return executedDeltas;
    }

    private <F extends ObjectType> boolean getSkipWhenSuccess(LensContext<F> context) {
        return context.getInternalsConfiguration() != null &&
                context.getInternalsConfiguration().getOperationExecutionRecording() != null &&
                Boolean.TRUE.equals(context.getInternalsConfiguration().getOperationExecutionRecording().isSkipWhenSuccess());
    }

    private <F extends ObjectType> void recordProjectionOperationExecution(LensContext<F> context,
            LensProjectionContext projectionContext, XMLGregorianCalendar now, Throwable clockworkException,
            Task task, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
        PrismObject<ShadowType> object = projectionContext.getObjectAny();
        if (object == null) {
            return;            // this can happen
        }
        List<LensObjectDeltaOperation<ShadowType>> executedDeltas = getExecutedDeltas(projectionContext, ShadowType.class,
                clockworkException, result);
        recordOperationExecution(context, object, true, executedDeltas, now, task, result);
    }

    /**
     * @return true if the operation execution was recorded (or would be recorded, but skipped because of the configuration)
     */
    private <O extends ObjectType, F extends ObjectType> boolean recordOperationExecution(LensContext<O> context, PrismObject<F> object, boolean deletedOk,
            List<LensObjectDeltaOperation<F>> executedDeltas, XMLGregorianCalendar now, Task task, OperationResult result)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationExecutionType operation = new OperationExecutionType(prismContext);
        OperationResult summaryResult = new OperationResult("recordOperationExecution");
        String oid = object.getOid();
        for (LensObjectDeltaOperation<F> deltaOperation : executedDeltas) {
            operation.getOperation().add(createObjectDeltaOperation(deltaOperation));
            if (deltaOperation.getExecutionResult() != null) {
                summaryResult.addSubresult(deltaOperation.getExecutionResult().clone());        // todo eliminate this clone (but beware of modifying the subresult)
            }
            if (oid == null && deltaOperation.getObjectDelta() != null) {
                oid = deltaOperation.getObjectDelta().getOid();
            }
        }
        if (oid == null) {        // e.g. if there is an exception in provisioning.addObject method
            LOGGER.trace("recordOperationExecution: skipping because oid is null for object = {}", object);
            return false;
        }
        createTaskRef(context, operation, task, result);
        summaryResult.computeStatus();
        OperationResultStatusType overallStatus = summaryResult.getStatus().createStatusType();
        setOperationContext(operation, overallStatus, now, context.getChannel(), task);
        storeOperationExecution(object, oid, operation, deletedOk, getSkipWhenSuccess(context), result);
        return true;
    }

    private <F extends ObjectType> void storeOperationExecution(@NotNull PrismObject<F> object, @NotNull String oid,
            @NotNull OperationExecutionType executionToAdd, boolean deletedOk, boolean skipWhenSuccess, OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
        Integer recordsToKeep;
        Long deleteBefore;
        boolean keepNoExecutions = false;
        PrismObject<SystemConfigurationType> systemConfiguration = systemObjectCache.getSystemConfiguration(result);
        if (systemConfiguration != null && systemConfiguration.asObjectable().getCleanupPolicy() != null
                && systemConfiguration.asObjectable().getCleanupPolicy().getObjectResults() != null) {
            CleanupPolicyType policy = systemConfiguration.asObjectable().getCleanupPolicy().getObjectResults();
            recordsToKeep = policy.getMaxRecords();
            if (recordsToKeep != null && recordsToKeep == 0) {
                LOGGER.trace("objectResults.recordsToKeep is 0, will skip storing operationExecutions");
                keepNoExecutions = true;
            }
            if (policy.getMaxAge() != null) {
                XMLGregorianCalendar limit = XmlTypeConverter.addDuration(
                        XmlTypeConverter.createXMLGregorianCalendar(new Date()), policy.getMaxAge().negate());
                deleteBefore = XmlTypeConverter.toMillis(limit);
            } else {
                deleteBefore = null;
            }
        } else {
            recordsToKeep = DEFAULT_NUMBER_OF_RESULTS_TO_KEEP;
            deleteBefore = null;
        }

        String taskOid = executionToAdd.getTaskRef() != null ? executionToAdd.getTaskRef().getOid() : null;
        if (executionToAdd.getStatus() == OperationResultStatusType.SUCCESS && skipWhenSuccess) {
            // We want to skip writing operationExecution. But let's check if there are some older non-success results
            // related to the current task
            if (taskOid != null) {
                boolean hasNonSuccessFromCurrentTask = object.asObjectable().getOperationExecution().stream()
                        .anyMatch(oe -> oe.getTaskRef() != null && taskOid.equals(oe.getTaskRef().getOid()) &&
                                oe.getStatus() != OperationResultStatusType.SUCCESS);
                if (hasNonSuccessFromCurrentTask) {
                    LOGGER.trace("Cannot skip OperationExecution recording because there's an older non-success record from the current task");
                } else {
                    LOGGER.trace("Skipping OperationExecution recording because status is SUCCESS and skipWhenSuccess is true "
                            + "(and no older non-success records for current task {} exist)", taskOid);
                    return;
                }
            } else {
                LOGGER.trace("Skipping OperationExecution recording because status is SUCCESS and skipWhenSuccess is true");
                return;
            }
        }
        List<OperationExecutionType> executionsToDelete = new ArrayList<>();
        List<OperationExecutionType> executions = new ArrayList<>(object.asObjectable().getOperationExecution());
        // delete all executions related to current task and all old ones
        for (Iterator<OperationExecutionType> iterator = executions.iterator(); iterator.hasNext(); ) {
            OperationExecutionType execution = iterator.next();
            boolean isPreviousTaskResult = taskOid != null && execution.getTaskRef() != null && taskOid.equals(execution.getTaskRef().getOid());
            boolean isOld = deleteBefore != null && XmlTypeConverter.toMillis(execution.getTimestamp()) < deleteBefore;
            if (isPreviousTaskResult || isOld) {
                executionsToDelete.add(execution);
                iterator.remove();
            }
        }

        // delete all surplus executions
        if (recordsToKeep != null && executions.size() > recordsToKeep - 1) {
            if (keepNoExecutions) {
                executionsToDelete.addAll(executions);
            } else {
                executions.sort(Comparator.nullsFirst(Comparator.comparing(e -> XmlTypeConverter.toDate(e.getTimestamp()))));
                executionsToDelete.addAll(executions.subList(0, executions.size() - (recordsToKeep - 1)));
            }
        }
        // construct and execute the delta
        Class<? extends ObjectType> objectClass = object.asObjectable().getClass();
        List<ItemDelta<?, ?>> deltas = new ArrayList<>();
        if (!keepNoExecutions) {
            deltas.add(prismContext.deltaFor(objectClass)
                    .item(ObjectType.F_OPERATION_EXECUTION)
                    .add(executionToAdd)
                    .asItemDelta());
        }
        if (!executionsToDelete.isEmpty()) {
            deltas.add(prismContext.deltaFor(objectClass)
                    .item(ObjectType.F_OPERATION_EXECUTION)
                    .delete(PrismContainerValue.toPcvList(CloneUtil.cloneCollectionMembers(executionsToDelete)))
                    .asItemDelta());
        }
        LOGGER.trace("Operation execution delta:\n{}", DebugUtil.debugDumpLazily(deltas));
        try {
            if (!deltas.isEmpty()) {
                repositoryService.modifyObject(objectClass, oid, deltas, result);
            }
        } catch (ObjectNotFoundException e) {
            if (!deletedOk) {
                throw e;
            } else {
                LOGGER.trace("Object {} deleted but this was expected.", oid);
                result.deleteLastSubresultIfError();
            }
        }
    }

    private void setOperationContext(OperationExecutionType operation,
            OperationResultStatusType overallStatus, XMLGregorianCalendar now, String channel, Task task) {

        operation.setStatus(overallStatus);
        operation.setInitiatorRef(ObjectTypeUtil.createObjectRef(task.getOwner(), prismContext));        // TODO what if the real initiator is different? (e.g. when executing approved changes)
        operation.setChannel(channel);
        operation.setTimestamp(now);
    }

    private <O extends ObjectType> void createTaskRef(LensContext<O> context, OperationExecutionType operation, Task task, OperationResult result) {
        if (task instanceof RunningTask && ((RunningTask) task).getParentForLightweightAsynchronousTask() != null) {
            task = ((RunningTask) task).getParentForLightweightAsynchronousTask();
        }
        if (task.isPersistent()) {
            String taskOid;
            try {
                taskOid = context.getTaskTreeOid(task, result);
            } catch (SchemaException e) {
                //if something unexpeced happened, let's try current task oid
                LOGGER.warn("Cannot get task tree oid, current task oid will be used, task: {}, \nreason: {}", task, e.getMessage(), e);
                result.recordWarning("Cannot get task tree oid, current task oid will be used, task: " + task + "\nreason: " + e.getMessage(), e);
                taskOid = task.getOid();
            }

            operation.setTaskRef(ObjectTypeUtil.createObjectRef(taskOid, ObjectTypes.TASK));
        }
    }

    private <F extends ObjectType> ObjectDeltaOperationType createObjectDeltaOperation(LensObjectDeltaOperation<F> deltaOperation) {
        ObjectDeltaOperationType odo;
        try {
            odo = simplifyOperation(deltaOperation).toLensObjectDeltaOperationType().getObjectDeltaOperation();
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't create operation information", e);
            odo = new ObjectDeltaOperationType();
            OperationResult r = new OperationResult(Clockwork.class.getName() + ".createObjectDeltaOperation");
            r.recordFatalError("Couldn't create operation information: " + e.getMessage(), e);
            odo.setExecutionResult(r.createOperationResultType());
        }
        return odo;
    }

    private <F extends ObjectType> LensObjectDeltaOperation<F> simplifyOperation(ObjectDeltaOperation<F> operation) {
        LensObjectDeltaOperation<F> rv = new LensObjectDeltaOperation<>();
        rv.setObjectDelta(simplifyDelta(operation.getObjectDelta()));
        rv.setExecutionResult(OperationResult.keepRootOnly(operation.getExecutionResult()));
        rv.setObjectName(operation.getObjectName());
        rv.setResourceName(operation.getResourceName());
        rv.setResourceOid(operation.getResourceOid());
        return rv;
    }

    private <F extends ObjectType> ObjectDelta<F> simplifyDelta(ObjectDelta<F> delta) {
        return prismContext.deltaFactory().object().create(delta.getObjectTypeClass(), delta.getChangeType());
    }
}
