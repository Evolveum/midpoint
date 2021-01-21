/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.sync.tasks.recon;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.impl.sync.tasks.SyncTaskHelper;
import com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.impl.ModelConstants;
import com.evolveum.midpoint.model.impl.tasks.AbstractSearchIterativeModelTaskHandler;
import com.evolveum.midpoint.model.impl.util.AuditHelper;
import com.evolveum.midpoint.provisioning.api.ChangeNotificationDispatcher;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.task.TaskExecutionClass;
import com.evolveum.midpoint.schema.SchemaHelper;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.schema.constants.Channel;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.StatisticsCollectionStrategy;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * The task handler for reconciliation.
 *
 * This handler takes care of executing reconciliation "runs". It means that the
 * handler "run" method will be as scheduled (every few days). The
 * responsibility is to iterate over accounts and compare the real state with
 * the assumed IDM state.
 *
 * @author Radovan Semancik
 *
 */
@Component
@TaskExecutionClass(ReconciliationTaskExecution.class)
public class ReconciliationTaskHandler
        extends AbstractSearchIterativeModelTaskHandler
        <ReconciliationTaskHandler, ReconciliationTaskExecution> {

    /**
     * Just for testability. Used in tests. Injected by explicit call to a
     * setter.
     */
    private ReconciliationTaskResultListener reconciliationTaskResultListener;

    @Autowired ChangeNotificationDispatcher changeNotificationDispatcher;
    @Autowired AuditHelper auditHelper;
    @Autowired private Clock clock;
    @Autowired protected ExpressionFactory expressionFactory;
    @Autowired SchemaHelper schemaHelper;
    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;
    @Autowired SyncTaskHelper syncTaskHelper;

    @Autowired CacheConfigurationManager cacheConfigurationManager;

    private static final Trace LOGGER = TraceManager.getTrace(ReconciliationTaskHandler.class);

    protected ReconciliationTaskHandler() {
        super("Reconciliation", OperationConstants.RECONCILIATION);
        reportingOptions.setEnableSynchronizationStatistics(true);
    }

    public ReconciliationTaskResultListener getReconciliationTaskResultListener() {
        return reconciliationTaskResultListener;
    }

    public void setReconciliationTaskResultListener(ReconciliationTaskResultListener reconciliationTaskResultListener) {
        this.reconciliationTaskResultListener = reconciliationTaskResultListener;
    }

    @PostConstruct
    private void initialize() {
        taskManager.registerHandler(ModelPublicConstants.RECONCILIATION_TASK_HANDLER_URI, this);
        taskManager.registerAdditionalHandlerUri(ModelPublicConstants.PARTITIONED_RECONCILIATION_TASK_HANDLER_URI_1, this);
        taskManager.registerAdditionalHandlerUri(ModelPublicConstants.PARTITIONED_RECONCILIATION_TASK_HANDLER_URI_2, this);
        taskManager.registerAdditionalHandlerUri(ModelPublicConstants.PARTITIONED_RECONCILIATION_TASK_HANDLER_URI_3, this);
    }

    @NotNull
    @Override
    public StatisticsCollectionStrategy getStatisticsCollectionStrategy() {
        return new StatisticsCollectionStrategy()
                .fromZero()
                .maintainIterationStatistics()
                .maintainSynchronizationStatistics()
                .maintainActionsExecutedStatistics();
    }

//    public TaskWorkBucketProcessingResult run000(RunningTask localCoordinatorTask, WorkBucketType workBucket,
//            TaskPartitionDefinitionType partitionDefinition, TaskWorkBucketProcessingResult previousRunResult) {
//
//
//        if (previousRunResult != null) {
//            runResult.setProgress(previousRunResult.getProgress());
//            AbstractSearchIterativeTaskPartExecution.logPreviousResultIfNeeded(localCoordinatorTask, previousRunResult, LOGGER);  // temporary
//        }
//        runResult.setShouldContinue(false); // overridden later
//        runResult.setBucketComplete(false); // overridden later
//
//        SynchronizationObjectsFilter objectsFilter = ModelImplUtils.determineSynchronizationObjectsFilter(objectclassDef,
//                localCoordinatorTask);
//
//        reconResult.setResource(resource);
//        reconResult.setObjectclassDefinition(objectclassDef);
//
//        LOGGER.info("Start executing reconciliation of resource {}, reconciling object class {}, stage: {}, work bucket: {}",
//                resource, objectclassDef, stage, workBucket);
//        long reconStartTimestamp = clock.currentTimeMillis();
//
//
//        long beforeResourceReconTimestamp = clock.currentTimeMillis();
//        long afterResourceReconTimestamp;
//        long afterShadowReconTimestamp;
//
//
//        opResult.computeStatus();
//        // This "run" is finished. But the task goes on ...
//        runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);
//        runResult.setShouldContinue(true);
//        runResult.setBucketComplete(true);
//        LOGGER.trace("Reconciliation.run stopping, result: {}", opResult.getStatus());
//
//        long reconEndTimestamp = clock.currentTimeMillis();
//
//        long etime = reconEndTimestamp - reconStartTimestamp;
//        long unOpsTime = beforeResourceReconTimestamp - reconStartTimestamp;
//        long resourceReconTime = afterResourceReconTimestamp - beforeResourceReconTimestamp;
//        long shadowReconTime = afterShadowReconTimestamp - afterResourceReconTimestamp;
//        LOGGER.info("Done executing reconciliation of resource {}, object class {}, Etime: {} ms (un-ops: {}, resource: {}, shadow: {})",
//                resource, objectclassDef, etime, unOpsTime, resourceReconTime, shadowReconTime);
//
//        reconResult.setRunResult(runResult);
//        if (reconciliationTaskResultListener != null) {
//            reconciliationTaskResultListener.process(reconResult);
//        }
//
//        TaskHandlerUtil.appendLastFailuresInformation(OperationConstants.RECONCILIATION, localCoordinatorTask, opResult);
//        return runResult;
//    }

//    private void setExpectedTotalToNull(Task coordinatorTask, OperationResult opResult) {
//        coordinatorTask.setExpectedTotal(null);
//        try {
//            coordinatorTask.flushPendingModifications(opResult);
//        } catch (Throwable t) {
//            throw new SystemException("Couldn't update the task: " + t.getMessage(), t);
//        }
//    }

    /**
     * Launch an import. Calling this method will start import in a new
     * thread, possibly on a different node.
     */
    public void launch(ResourceType resource, QName objectclass, Task task, OperationResult parentResult) {

        LOGGER.info("Launching reconciliation for resource {} as asynchronous task", ObjectTypeUtil.toShortString(resource));

        OperationResult result = parentResult.createSubresult(ReconciliationTaskHandler.class.getName() + ".launch");
        result.addParam("resource", resource);
        result.addParam("objectclass", objectclass);
        // TODO

        // Set handler URI so we will be called back
        task.setHandlerUri(ModelPublicConstants.RECONCILIATION_TASK_HANDLER_URI);

        // Readable task name
        PolyStringType polyString = new PolyStringType("Reconciling " + resource.getName());
        task.setName(polyString);

        // Set reference to the resource
        task.setObjectRef(ObjectTypeUtil.createObjectRef(resource, prismContext));

        try {
            task.setExtensionPropertyValue(ModelConstants.OBJECTCLASS_PROPERTY_NAME, objectclass);
            task.flushPendingModifications(result);        // just to be sure (if the task was already persistent)
        } catch (ObjectNotFoundException e) {
            LOGGER.error("Task object not found, expecting it to exist (task {})", task, e);
            result.recordFatalError("Task object not found", e);
            throw new IllegalStateException("Task object not found, expecting it to exist", e);
        } catch (ObjectAlreadyExistsException e) {
            LOGGER.error("Task object wasn't updated (task {})", task, e);
            result.recordFatalError("Task object wasn't updated", e);
            throw new IllegalStateException("Task object wasn't updated", e);
        } catch (SchemaException e) {
            LOGGER.error("Error dealing with schema (task {})", task, e);
            result.recordFatalError("Error dealing with schema", e);
            throw new IllegalStateException("Error dealing with schema", e);
        }

        task.addArchetypeInformationIfMissing(SystemObjectsType.ARCHETYPE_RECONCILIATION_TASK.value());

        // Switch task to background. This will start new thread and call
        // the run(task) method.
        // Note: the thread may be actually started on a different node
        taskManager.switchToBackground(task, result);
        result.setBackgroundTaskOid(task.getOid());
        result.computeStatus("Reconciliation launch failed");

        LOGGER.trace("Reconciliation for resource {} switched to background, control thread returning with task {}", ObjectTypeUtil.toShortString(resource), task);
    }

//    private void processInterruption(TaskRunResult runResult, PrismObject<ResourceType> resource, RunningTask task, OperationResult opResult) {
//        opResult.recordWarning("Interrupted");
//        if (LOGGER.isWarnEnabled()) {
//            LOGGER.warn("Reconciliation on {} interrupted", resource);
//        }
//        runResult.setRunResultStatus(TaskRunResultStatus.INTERRUPTED);          // not strictly necessary, because using task.canRun() == false the task manager knows we were interrupted
//        TaskHandlerUtil.appendLastFailuresInformation(OperationConstants.RECONCILIATION, task, opResult);    // TODO implement more seriously
//    }
//
//    private void processErrorFinal(TaskRunResult runResult, String errorDesc, Exception ex,
//            TaskRunResultStatus runResultStatus, PrismObject<ResourceType> resource, RunningTask task, OperationResult opResult) {
//        AuditEventRecord executionRecord = new AuditEventRecord(AuditEventType.RECONCILIATION, AuditEventStage.EXECUTION);
//        executionRecord.setTarget(resource, prismContext);
//        executionRecord.setOutcome(OperationResultStatus.FATAL_ERROR);
//        executionRecord.setMessage(ex.getMessage());
//        auditHelper.audit(executionRecord, null, task, opResult);
//
//        String message = errorDesc+": "+ex.getMessage();
//        LOGGER.error("Reconciliation: {}-{}", message, ex);
//        opResult.recordFatalError(message, ex);
//        TaskHandlerUtil.appendLastFailuresInformation(OperationConstants.RECONCILIATION, task, opResult); // TODO implement more seriously
//        runResult.setRunResultStatus(runResultStatus);
//    }
//
//    private void processErrorPartial(TaskRunResult runResult, String errorDesc, Exception ex,
//            TaskRunResultStatus runResultStatus, OperationResult opResult) {
//        String message;
//        if (ex == null) {
//            message = errorDesc;
//        } else {
//            message = errorDesc+": "+ex.getMessage();
//        }
//        LOGGER.error("Reconciliation: {}-{}", message, ex);
//        opResult.recordFatalError(message, ex);
//        runResult.setRunResultStatus(runResultStatus);
//    }
//
//    // returns false in case of execution interruption
//    private boolean performResourceReconciliation(PrismObject<ResourceType> resource,
//            ObjectClassComplexTypeDefinition objectclassDef,
//            SynchronizationObjectsFilter objectsFilter, ReconciliationTaskResult reconResult, RunningTask localCoordinatorTask,
//            TaskPartitionDefinitionType partitionDefinition, WorkBucketType workBucket, OperationResult parentResult)
//            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
//            SecurityViolationException, ExpressionEvaluationException, PreconditionViolationException, PolicyViolationException, ObjectAlreadyExistsException {
//
//        boolean interrupted;
//
//        OperationResult result = parentResult.createSubresult(OperationConstants.RECONCILIATION+".resourceReconciliation");
//
//        // Instantiate result handler. This will be called with every search
//        // result in the following iterative search
//        SynchronizeAccountResultHandler handler = new SynchronizeAccountResultHandler(resource.asObjectable(), objectclassDef,
//                objectsFilter, "reconciliation", localCoordinatorTask, changeNotificationDispatcher,
//                partitionDefinition, taskManager);
//        handler.setSourceChannel(SchemaConstants.CHANNEL_RECON);
//        handler.setStopOnError(false);
//        handler.setEnableSynchronizationStatistics(true);
//        handler.setEnableActionsExecutedStatistics(true);
//
//        localCoordinatorTask.setExpectedTotal(null);
//
//        try {
//
//
//            OperationResult searchResult = new OperationResult(OperationConstants.RECONCILIATION+".searchIterative");
//
//            handler.createWorkerThreads(localCoordinatorTask);
//            // note that progress is incremented within the handler, as it extends AbstractSearchIterativeResultHandler
//            try {
//                provisioningService.searchObjectsIterative(ShadowType.class, bucketNarrowedQuery, options, handler,
//                        localCoordinatorTask, searchResult);
//            } finally {
//                handler.completeProcessing(localCoordinatorTask, searchResult);
//            }
//
//            interrupted = !localCoordinatorTask.canRun();
//
//            result.computeStatus();
//
//            String message = "Processed " + handler.getProgress() + " account(s), got " + handler.getErrors() + " error(s)";
//            if (interrupted) {
//                message += "; was interrupted during processing";
//            }
//            if (handler.getProgress() > 0) {
//                message += ". Average time for one object: " + handler.getAverageTime() + " ms (wall clock time average: " + handler.getWallAverageTime() + " ms).";
//            }
//
//            OperationResultStatus resultStatus;
//            if (handler.getErrors() > 0) {
//                resultStatus = OperationResultStatus.PARTIAL_ERROR;
//            } else {
//                resultStatus = OperationResultStatus.SUCCESS;
//            }
//            result.recordStatus(resultStatus, message);
//            LOGGER.info("Finished resource part of {} reconciliation: {}", resource, message);
//
//            reconResult.setResourceReconCount(handler.getProgress());
//            reconResult.setResourceReconErrors(handler.getErrors());
//
//        } catch (ConfigurationException | SecurityViolationException | SchemaException | CommunicationException | ObjectNotFoundException | ExpressionEvaluationException | RuntimeException | Error e) {
//            result.recordFatalError(e);
//            throw e;
//        }
//
//        if (handler.getExceptionEncountered() != null) {
//            RepoCommonUtils.throwException(handler.getExceptionEncountered(), result);
//        }
//
//        return !interrupted;
//    }
//
//    // returns false in case of execution interruption
//    private boolean performShadowReconciliation(final PrismObject<ResourceType> resource,
//            @NotNull ObjectClassComplexTypeDefinition objectclassDef, @NotNull SynchronizationObjectsFilter objectsFilter,
//            long startTimestamp, long endTimestamp, ReconciliationTaskResult reconResult, RunningTask localCoordinatorTask,
//            WorkBucketType workBucket, OperationResult parentResult) throws SchemaException,
//            ObjectNotFoundException, ExpressionEvaluationException, ConfigurationException, CommunicationException {
//        boolean interrupted;
//
//        // find accounts
//
//        LOGGER.trace("Shadow reconciliation starting for {}, {} -> {}", resource, startTimestamp, endTimestamp);
//        OperationResult result = parentResult.createSubresult(OperationConstants.RECONCILIATION+".shadowReconciliation");
//        try {
//
//            long started = System.currentTimeMillis();
//
//            AtomicLong count = new AtomicLong(0);
//            AtomicLong errors = new AtomicLong(0);
//
//            ResultHandler<ShadowType> handler = (shadow, shadowResult) -> {
//            };
//
//            repositoryService.searchObjectsIterative(ShadowType.class, bucketNarrowedQuery, handler, null, true, result);
//            interrupted = !localCoordinatorTask.canRun();
//
//            // for each try the operation again
//
//            result.computeStatus();
//            if (errors.longValue() > 0) {
//                result.setStatus(OperationResultStatus.PARTIAL_ERROR);
//            }
//
//            LOGGER.trace("Shadow reconciliation finished, processed {} shadows for {}, result: {}",
//                    count.longValue(), resource, result.getStatus());
//
//            reconResult.setShadowReconCount(count.longValue());
//
//            parentResult.createSubresult(OperationConstants.RECONCILIATION + ".shadowReconciliation.statistics")
//                    .recordStatus(OperationResultStatus.SUCCESS, "Processed " + count.longValue() + " shadow(s) in "
//                            + (System.currentTimeMillis() - started) + " ms."
//                            + " Errors: " + errors.longValue() + "."
//                            + (interrupted ? " Was interrupted during processing." : ""));
//
//            return !interrupted;
//        } catch (Throwable t) {
//            result.recordFatalError(t);
//            throw t;
//        } finally {
//            result.computeStatusIfUnknown();
//        }
//    }
//
//    private void processShadowReconError(Throwable t, PrismObject<ShadowType> shadow, OperationResult result) {
//        LOGGER.error("Error reconciling shadow {}: {}", shadow, t.getMessage(), t);
//        result.recordFatalError(t);
//        // TODO: store error in the shadow?
//    }

    @Override
    public Long heartbeat(Task task) {
        return null;
    }

    @Override
    public void refreshStatus(Task task) {
        // Do nothing. Everything is fresh already.
    }

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.RECONCILIATION;
    }

    @Override
    public String getArchetypeOid() {
        return SystemObjectsType.ARCHETYPE_RECONCILIATION_TASK.value();
    }

    @Override
    public String getDefaultChannel() {
        return Channel.RECONCILIATION.getUri();
    }

    public ResourceObjectChangeListener getObjectChangeListener() {
        return changeNotificationDispatcher;
    }
}
