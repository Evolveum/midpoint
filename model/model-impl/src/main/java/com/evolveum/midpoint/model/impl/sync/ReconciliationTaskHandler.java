/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.sync;

import java.util.Collection;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.impl.util.AuditHelper;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.repo.common.util.RepoCommonUtils;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.schema.constants.Channel;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import org.apache.commons.lang.BooleanUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.impl.ModelConstants;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.api.ChangeNotificationDispatcher;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.repo.common.task.AbstractSearchIterativeTaskHandler;
import com.evolveum.midpoint.repo.common.task.TaskHandlerUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
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
public class ReconciliationTaskHandler implements WorkBucketAwareTaskHandler {

    private static final String HANDLER_URI = ModelPublicConstants.RECONCILIATION_TASK_HANDLER_URI;
    private static final String FIRST_STAGE_HANDLER_URI = ModelPublicConstants.PARTITIONED_RECONCILIATION_TASK_HANDLER_URI_1;
    private static final String SECOND_STAGE_HANDLER_URI = ModelPublicConstants.PARTITIONED_RECONCILIATION_TASK_HANDLER_URI_2;
    private static final String THIRD_STAGE_HANDLER_URI = ModelPublicConstants.PARTITIONED_RECONCILIATION_TASK_HANDLER_URI_3;

//    public static final String DRY_RUN_URI = ModelPublicConstants.RECONCILIATION_TASK_HANDLER_URI + "#dryRun";
//    public static final String SIMULATE_URI = ModelPublicConstants.RECONCILIATION_TASK_HANDLER_URI + "#simulate";
//    public static final String EXECUTE_URI = ModelPublicConstants.RECONCILIATION_TASK_HANDLER_URI + "#execute";

    /**
     * Just for testability. Used in tests. Injected by explicit call to a
     * setter.
     */
    private ReconciliationTaskResultListener reconciliationTaskResultListener;

    @Autowired private TaskManager taskManager;
    @Autowired private ProvisioningService provisioningService;
    @Autowired private PrismContext prismContext;
    @Autowired private ChangeNotificationDispatcher changeNotificationDispatcher;
    @Autowired private AuditHelper auditHelper;
    @Autowired private Clock clock;
    @Autowired private ExpressionFactory expressionFactory;
    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    @Autowired private CacheConfigurationManager cacheConfigurationManager;

    private static final Trace LOGGER = TraceManager.getTrace(ReconciliationTaskHandler.class);

    @SuppressWarnings("unused")
    public ReconciliationTaskResultListener getReconciliationTaskResultListener() {
        return reconciliationTaskResultListener;
    }

    public void setReconciliationTaskResultListener(
            ReconciliationTaskResultListener reconciliationTaskResultListener) {
        this.reconciliationTaskResultListener = reconciliationTaskResultListener;
    }

    @PostConstruct
    private void initialize() {
        taskManager.registerHandler(HANDLER_URI, this);
        taskManager.registerAdditionalHandlerUri(FIRST_STAGE_HANDLER_URI, this);
        taskManager.registerAdditionalHandlerUri(SECOND_STAGE_HANDLER_URI, this);
        taskManager.registerAdditionalHandlerUri(THIRD_STAGE_HANDLER_URI, this);
    }

    enum Stage {
        FIRST, SECOND, THIRD, ALL
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


    @Override
    public TaskWorkBucketProcessingResult run(RunningTask localCoordinatorTask, WorkBucketType workBucket,
            TaskPartitionDefinitionType partitionDefinition, TaskWorkBucketProcessingResult previousRunResult) {

        String handlerUri;
        if (partitionDefinition != null && partitionDefinition.getHandlerUri() != null) {
            handlerUri = partitionDefinition.getHandlerUri();
        } else {
            handlerUri = localCoordinatorTask.getHandlerUri();
        }
        Stage stageFromHandler = getStage(handlerUri);
        LOGGER.trace("ReconciliationTaskHandler.run starting (stage from handler: {})", stageFromHandler);
        ReconciliationTaskResult reconResult = new ReconciliationTaskResult();

        LOGGER.trace("Recon task: {}", localCoordinatorTask.debugDumpLazily());

        Stage stage;
        if (BooleanUtils.isTrue(localCoordinatorTask.getExtensionPropertyRealValue(SchemaConstants.MODEL_EXTENSION_FINISH_OPERATIONS_ONLY))) {
            if (stageFromHandler == Stage.ALL) {
                stage = Stage.FIRST;
                LOGGER.trace("'Finish operations only' mode selected, changing stage to {}", stage);
            } else {
                throw new IllegalStateException("Finish operations only selected for wrong stage: " + stageFromHandler);
            }
        } else {
            stage = stageFromHandler;
        }

        OperationResult opResult = new OperationResult(OperationConstants.RECONCILIATION);
        opResult.setStatus(OperationResultStatus.IN_PROGRESS);
        TaskWorkBucketProcessingResult runResult = new TaskWorkBucketProcessingResult();
        runResult.setOperationResult(opResult);
        if (previousRunResult != null) {
            runResult.setProgress(previousRunResult.getProgress());
            AbstractSearchIterativeTaskHandler.logPreviousResultIfNeeded(localCoordinatorTask, previousRunResult, LOGGER);  // temporary
        }
        runResult.setShouldContinue(false);     // overridden later
        runResult.setBucketComplete(false);     // overridden later

        String resourceOid = localCoordinatorTask.getObjectOid();
        opResult.addContext("resourceOid", resourceOid);

        if (resourceOid == null) {
            throw new IllegalArgumentException("Resource OID is missing in task extension");
        }

        PrismObject<ResourceType> resource;
        ObjectClassComplexTypeDefinition objectclassDef;
        try {
            resource = provisioningService.getObject(ResourceType.class, resourceOid, null, localCoordinatorTask, opResult);

            if (ResourceTypeUtil.isInMaintenance(resource.asObjectable()))
                throw new MaintenanceException("Resource " + resource + " is in the maintenance");

            RefinedResourceSchema refinedSchema = RefinedResourceSchemaImpl.getRefinedSchema(resource, LayerType.MODEL, prismContext);
            objectclassDef = ModelImplUtils.determineObjectClass(refinedSchema, localCoordinatorTask);
        } catch (ObjectNotFoundException ex) {
            // This is bad. The resource does not exist. Permanent problem.
            processErrorPartial(runResult, "Resource does not exist, OID: " + resourceOid, ex,
                    TaskRunResultStatus.PERMANENT_ERROR, opResult);
            return runResult;
        } catch (MaintenanceException ex) {
            LOGGER.warn("Reconciliation: {}-{}", ex.getMessage(), ex);
            opResult.recordHandledError(ex.getMessage(), ex);
            runResult.setRunResultStatus(TaskRunResultStatus.TEMPORARY_ERROR); // Resource is in the maintenance, do not suspend the task
            return runResult;
        } catch (CommunicationException ex) {
            // Error, but not critical. Just try later.
            processErrorPartial(runResult, "Communication error", ex, TaskRunResultStatus.TEMPORARY_ERROR, opResult);
            return runResult;
        } catch (SchemaException ex) {
            // Not sure about this. But most likely it is a misconfigured resource or connector
            // It may be worth to retry. Error is fatal, but may not be permanent.
            processErrorPartial(runResult, "Error dealing with schema", ex, TaskRunResultStatus.TEMPORARY_ERROR, opResult);
            return runResult;
        } catch (RuntimeException ex) {
            // Can be anything ... but we can't recover from that.
            // It is most likely a programming error. Does not make much sense
            // to retry.
            processErrorPartial(runResult, "Internal Error", ex, TaskRunResultStatus.PERMANENT_ERROR, opResult);
            return runResult;
        } catch (ConfigurationException ex) {
            // Not sure about this. But most likely it is a misconfigured resource or connector
            // It may be worth to retry. Error is fatal, but may not be permanent.
            processErrorPartial(runResult, "Configuration error", ex, TaskRunResultStatus.TEMPORARY_ERROR, opResult);
            return runResult;
        } catch (SecurityViolationException ex) {
            processErrorPartial(runResult, "Security violation", ex, TaskRunResultStatus.PERMANENT_ERROR, opResult);
            return runResult;
        } catch (ExpressionEvaluationException ex) {
            processErrorPartial(runResult, "Expression error", ex, TaskRunResultStatus.PERMANENT_ERROR, opResult);
            return runResult;
        }

        if (objectclassDef == null) {
            processErrorPartial(runResult, "Reconciliation without an object class specification is not supported",
                    null, TaskRunResultStatus.PERMANENT_ERROR, opResult);
            return runResult;
        }

        reconResult.setResource(resource);
        reconResult.setObjectclassDefinition(objectclassDef);

        LOGGER.info("Start executing reconciliation of resource {}, reconciling object class {}, stage: {}, work bucket: {}",
                resource, objectclassDef, stage, workBucket);
        long reconStartTimestamp = clock.currentTimeMillis();

        AuditEventRecord requestRecord = new AuditEventRecord(AuditEventType.RECONCILIATION, AuditEventStage.REQUEST);
        requestRecord.setTarget(resource, prismContext);
        requestRecord.setMessage("Stage: " + stage + ", Work bucket: " + workBucket);
        auditHelper.audit(requestRecord, null, localCoordinatorTask, opResult);

        try {
            if (isStage(stage, Stage.FIRST) && !scanForUnfinishedOperations(localCoordinatorTask, resourceOid, reconResult, opResult)) {
                processInterruption(runResult, resource, localCoordinatorTask, opResult);            // appends also "last N failures" (TODO refactor)
                return runResult;
            }
        } catch (SchemaException ex) {
            // Not sure about this. But most likely it is a misconfigured resource or connector
            // It may be worth to retry. Error is fatal, but may not be permanent.
            processErrorPartial(runResult, "Error dealing with schema", ex, TaskRunResultStatus.TEMPORARY_ERROR, opResult);
        } catch (RuntimeException ex) {
            // Can be anything ... but we can't recover from that.
            // It is most likely a programming error. Does not make much sense
            // to retry.
            processErrorFinal(runResult, "Internal Error", ex, TaskRunResultStatus.PERMANENT_ERROR, resource, localCoordinatorTask, opResult);
            return runResult;
        }
        if (stage == Stage.ALL) {
            setExpectedTotalToNull(localCoordinatorTask, opResult);              // expected total is unknown for the remaining phases
        }

        long beforeResourceReconTimestamp = clock.currentTimeMillis();
        long afterResourceReconTimestamp;
        long afterShadowReconTimestamp;

        boolean intentIsNull;
        PrismProperty<String> intentProperty = localCoordinatorTask.getExtensionPropertyOrClone(ModelConstants.INTENT_PROPERTY_NAME);
        String intent;
        if (intentProperty != null) {
            intent = intentProperty.getValue().getValue();
        } else {
            intent = null;
        }
        intentIsNull = intent == null;

        try {
            if (isStage(stage, Stage.SECOND) && !performResourceReconciliation(resource, objectclassDef, reconResult,
                    localCoordinatorTask, partitionDefinition, workBucket, opResult, intentIsNull)) {
                processInterruption(runResult, resource, localCoordinatorTask, opResult);
                return runResult;
            }
            afterResourceReconTimestamp = clock.currentTimeMillis();
            if (isStage(stage, Stage.THIRD) && !performShadowReconciliation(resource, objectclassDef, reconStartTimestamp,
                    afterResourceReconTimestamp, reconResult, localCoordinatorTask, workBucket, opResult, intentIsNull)) {
                processInterruption(runResult, resource, localCoordinatorTask, opResult);
                return runResult;
            }
            afterShadowReconTimestamp = clock.currentTimeMillis();
        } catch (ObjectNotFoundException ex) {
            // This is bad. The resource does not exist. Permanent problem.
            processErrorFinal(runResult, "Resource does not exist, OID: " + resourceOid, ex, TaskRunResultStatus.PERMANENT_ERROR, resource, localCoordinatorTask, opResult);
            return runResult;
        } catch (CommunicationException ex) {
            // Error, but not critical. Just try later.
            processErrorFinal(runResult, "Communication error", ex, TaskRunResultStatus.TEMPORARY_ERROR, resource, localCoordinatorTask, opResult);
            return runResult;
        } catch (SchemaException ex) {
            // Not sure about this. But most likely it is a misconfigured resource or connector
            // It may be worth to retry. Error is fatal, but may not be permanent.
            processErrorFinal(runResult, "Error dealing with schema", ex, TaskRunResultStatus.TEMPORARY_ERROR, resource, localCoordinatorTask, opResult);
            return runResult;
        } catch (RuntimeException ex) {
            // Can be anything ... but we can't recover from that.
            // It is most likely a programming error. Does not make much sense
            // to retry.
            processErrorFinal(runResult, "Internal Error", ex, TaskRunResultStatus.PERMANENT_ERROR, resource, localCoordinatorTask, opResult);
            return runResult;
        } catch (ConfigurationException ex) {
            // Not sure about this. But most likely it is a misconfigured resource or connector
            // It may be worth to retry. Error is fatal, but may not be permanent.
            processErrorFinal(runResult, "Configuration error", ex, TaskRunResultStatus.TEMPORARY_ERROR, resource, localCoordinatorTask, opResult);
            return runResult;
        } catch (SecurityViolationException ex) {
            processErrorFinal(runResult, "Security violation", ex, TaskRunResultStatus.PERMANENT_ERROR, resource, localCoordinatorTask, opResult);
            return runResult;
        } catch (ExpressionEvaluationException ex) {
            processErrorFinal(runResult, "Expression error", ex, TaskRunResultStatus.PERMANENT_ERROR, resource, localCoordinatorTask, opResult);
            return runResult;
        } catch (ObjectAlreadyExistsException ex) {
            processErrorFinal(runResult, "Object already existis error", ex, TaskRunResultStatus.PERMANENT_ERROR, resource, localCoordinatorTask, opResult);
            return runResult;
        } catch (PolicyViolationException ex) {
            processErrorFinal(runResult, "Policy violation error", ex, TaskRunResultStatus.PERMANENT_ERROR, resource, localCoordinatorTask, opResult);
            return runResult;
        } catch (PreconditionViolationException ex) {
            processErrorFinal(runResult, "Precondition violation error", ex, TaskRunResultStatus.PERMANENT_ERROR, resource, localCoordinatorTask, opResult);
            return runResult;
        }

        AuditEventRecord executionRecord = new AuditEventRecord(AuditEventType.RECONCILIATION, AuditEventStage.EXECUTION);
        executionRecord.setTarget(resource, prismContext);
        executionRecord.setOutcome(OperationResultStatus.SUCCESS);
        executionRecord.setMessage(requestRecord.getMessage());
        auditHelper.audit(executionRecord, null, localCoordinatorTask, opResult);

        opResult.computeStatus();
        // This "run" is finished. But the task goes on ...
        runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);
        runResult.setShouldContinue(true);
        runResult.setBucketComplete(true);
        LOGGER.trace("Reconciliation.run stopping, result: {}", opResult.getStatus());

        long reconEndTimestamp = clock.currentTimeMillis();

        long etime = reconEndTimestamp - reconStartTimestamp;
        long unOpsTime = beforeResourceReconTimestamp - reconStartTimestamp;
        long resourceReconTime = afterResourceReconTimestamp - beforeResourceReconTimestamp;
        long shadowReconTime = afterShadowReconTimestamp - afterResourceReconTimestamp;
        LOGGER.info("Done executing reconciliation of resource {}, object class {}, Etime: {} ms (un-ops: {}, resource: {}, shadow: {})",
                resource, objectclassDef, etime, unOpsTime, resourceReconTime, shadowReconTime);

        reconResult.setRunResult(runResult);
        if (reconciliationTaskResultListener != null) {
            reconciliationTaskResultListener.process(reconResult);
        }

        TaskHandlerUtil.appendLastFailuresInformation(OperationConstants.RECONCILIATION, localCoordinatorTask, opResult);
        return runResult;
    }

    private boolean isStage(Stage stage, Stage selector) {
        return stage == Stage.ALL || stage == selector;
    }

    private Stage getStage(String handlerUri) {
        if (HANDLER_URI.equals(handlerUri)) {
            return Stage.ALL;
        } else if (FIRST_STAGE_HANDLER_URI.equals(handlerUri)) {
            return Stage.FIRST;
        } else if (SECOND_STAGE_HANDLER_URI.equals(handlerUri)) {
            return Stage.SECOND;
        } else if (THIRD_STAGE_HANDLER_URI.equals(handlerUri)) {
            return Stage.THIRD;
        } else {
            throw new IllegalStateException("Unknown handler URI " + handlerUri);
        }
    }

    private void setExpectedTotalToNull(Task coordinatorTask, OperationResult opResult) {
        coordinatorTask.setExpectedTotal(null);
        try {
            coordinatorTask.flushPendingModifications(opResult);
        } catch (Throwable t) {
            throw new SystemException("Couldn't update the task: " + t.getMessage(), t);
        }
    }

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
        task.setHandlerUri(HANDLER_URI);

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

    private void processInterruption(TaskRunResult runResult, PrismObject<ResourceType> resource, RunningTask task, OperationResult opResult) {
        opResult.recordWarning("Interrupted");
        if (LOGGER.isWarnEnabled()) {
            LOGGER.warn("Reconciliation on {} interrupted", resource);
        }
        runResult.setRunResultStatus(TaskRunResultStatus.INTERRUPTED);          // not strictly necessary, because using task.canRun() == false the task manager knows we were interrupted
        TaskHandlerUtil.appendLastFailuresInformation(OperationConstants.RECONCILIATION, task, opResult);    // TODO implement more seriously
    }

    private void processErrorFinal(TaskRunResult runResult, String errorDesc, Exception ex,
            TaskRunResultStatus runResultStatus, PrismObject<ResourceType> resource, RunningTask task, OperationResult opResult) {
        AuditEventRecord executionRecord = new AuditEventRecord(AuditEventType.RECONCILIATION, AuditEventStage.EXECUTION);
        executionRecord.setTarget(resource, prismContext);
        executionRecord.setOutcome(OperationResultStatus.FATAL_ERROR);
        executionRecord.setMessage(ex.getMessage());
        auditHelper.audit(executionRecord, null, task, opResult);

        String message = errorDesc+": "+ex.getMessage();
        LOGGER.error("Reconciliation: {}-{}", new Object[]{message, ex});
        opResult.recordFatalError(message, ex);
        TaskHandlerUtil.appendLastFailuresInformation(OperationConstants.RECONCILIATION, task, opResult); // TODO implement more seriously
        runResult.setRunResultStatus(runResultStatus);
    }

    private void processErrorPartial(TaskRunResult runResult, String errorDesc, Exception ex,
            TaskRunResultStatus runResultStatus, OperationResult opResult) {
        String message;
        if (ex == null) {
            message = errorDesc;
        } else {
            message = errorDesc+": "+ex.getMessage();
        }
        LOGGER.error("Reconciliation: {}-{}", new Object[]{message, ex});
        opResult.recordFatalError(message, ex);
        runResult.setRunResultStatus(runResultStatus);
    }

    // returns false in case of execution interruption
    private boolean performResourceReconciliation(PrismObject<ResourceType> resource,
            ObjectClassComplexTypeDefinition objectclassDef,
            ReconciliationTaskResult reconResult, RunningTask localCoordinatorTask,
            TaskPartitionDefinitionType partitionDefinition, WorkBucketType workBucket, OperationResult result, boolean intentIsNull)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException, PreconditionViolationException, PolicyViolationException, ObjectAlreadyExistsException {

        boolean interrupted;

        OperationResult opResult = result.createSubresult(OperationConstants.RECONCILIATION+".resourceReconciliation");

        // Instantiate result handler. This will be called with every search
        // result in the following iterative search
        SynchronizeAccountResultHandler handler = new SynchronizeAccountResultHandler(resource.asObjectable(),
                objectclassDef, "reconciliation", localCoordinatorTask, changeNotificationDispatcher, partitionDefinition, taskManager);
        handler.setSourceChannel(SchemaConstants.CHANNEL_RECON);
        handler.setStopOnError(false);
        handler.setEnableSynchronizationStatistics(true);
        handler.setEnableActionsExecutedStatistics(true);
        handler.setIntentIsNull(intentIsNull);

        localCoordinatorTask.setExpectedTotal(null);

        try {
            ObjectQuery shadowSearchQuery = objectclassDef.createShadowSearchQuery(resource.getOid());
            ObjectQuery taskEnhancedQuery = addQueryFromTaskIfExists(shadowSearchQuery, localCoordinatorTask);
            ObjectQuery bucketNarrowedQuery = narrowQueryForBucket(taskEnhancedQuery, localCoordinatorTask, workBucket, objectclassDef, opResult);

            OperationResult searchResult = new OperationResult(OperationConstants.RECONCILIATION+".searchIterative");

            handler.createWorkerThreads(localCoordinatorTask);
            // note that progress is incremented within the handler, as it extends AbstractSearchIterativeResultHandler
            try {
                provisioningService.searchObjectsIterative(ShadowType.class, bucketNarrowedQuery, null, handler,
                        localCoordinatorTask, searchResult);
            } finally {
                handler.completeProcessing(localCoordinatorTask, searchResult);
            }

            interrupted = !localCoordinatorTask.canRun();

            opResult.computeStatus();

            String message = "Processed " + handler.getProgress() + " account(s), got " + handler.getErrors() + " error(s)";
            if (interrupted) {
                message += "; was interrupted during processing";
            }
            if (handler.getProgress() > 0) {
                message += ". Average time for one object: " + handler.getAverageTime() + " ms (wall clock time average: " + handler.getWallAverageTime() + " ms).";
            }

            OperationResultStatus resultStatus;
            if (handler.getErrors() > 0) {
                resultStatus = OperationResultStatus.PARTIAL_ERROR;
            } else {
                resultStatus = OperationResultStatus.SUCCESS;
            }
            opResult.recordStatus(resultStatus, message);
            LOGGER.info("Finished resource part of {} reconciliation: {}", resource, message);

            reconResult.setResourceReconCount(handler.getProgress());
            reconResult.setResourceReconErrors(handler.getErrors());

        } catch (ConfigurationException | SecurityViolationException | SchemaException | CommunicationException | ObjectNotFoundException | ExpressionEvaluationException | RuntimeException | Error e) {
            opResult.recordFatalError(e);
            throw e;
        }

        if (handler.getExceptionEncountered() != null) {
            RepoCommonUtils.throwException(handler.getExceptionEncountered(), opResult);
        }

        return !interrupted;
    }

    // returns false in case of execution interruption
    private boolean performShadowReconciliation(final PrismObject<ResourceType> resource,
            final ObjectClassComplexTypeDefinition objectclassDef,
            long startTimestamp, long endTimestamp, ReconciliationTaskResult reconResult, RunningTask localCoordinatorTask,
            WorkBucketType workBucket, OperationResult result, boolean intentIsNull) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, ConfigurationException, CommunicationException {
        boolean interrupted;

        // find accounts

        LOGGER.trace("Shadow reconciliation starting for {}, {} -> {}", resource, startTimestamp, endTimestamp);
        OperationResult opResult = result.createSubresult(OperationConstants.RECONCILIATION+".shadowReconciliation");

        ObjectQuery initialQuery = prismContext.queryFor(ShadowType.class)
                .block()
                    .item(ShadowType.F_FULL_SYNCHRONIZATION_TIMESTAMP).le(XmlTypeConverter.createXMLGregorianCalendar(startTimestamp))
                    .or().item(ShadowType.F_FULL_SYNCHRONIZATION_TIMESTAMP).isNull()
                .endBlock()
                .and().item(ShadowType.F_RESOURCE_REF).ref(ObjectTypeUtil.createObjectRef(resource, prismContext).asReferenceValue())
                .and().item(ShadowType.F_OBJECT_CLASS).eq(objectclassDef.getTypeName())
                .build();

        ObjectQuery taskEnhancedQuery = addQueryFromTaskIfExists(initialQuery, localCoordinatorTask);
        ObjectQuery bucketNarrowedQuery = narrowQueryForBucket(taskEnhancedQuery, localCoordinatorTask, workBucket, objectclassDef, opResult);

        LOGGER.trace("Shadow recon query:\n{}", bucketNarrowedQuery.debugDumpLazily());

        long started = System.currentTimeMillis();

        final Holder<Long> countHolder = new Holder<>(0L);

        ResultHandler<ShadowType> handler = (shadow, parentResult) -> {
            boolean isMatches;
            if (intentIsNull && objectclassDef instanceof RefinedObjectClassDefinition) {
                isMatches = ((RefinedObjectClassDefinition)objectclassDef).matchesWithoutIntent(shadow.asObjectable());
            } else {
                isMatches = objectclassDef.matches(shadow.asObjectable());
            }

            if ((objectclassDef instanceof RefinedObjectClassDefinition) && !isMatches) {
                return true;
            }

            LOGGER.trace("Shadow reconciliation of {}, fullSynchronizationTimestamp={}", shadow, shadow.asObjectable().getFullSynchronizationTimestamp());
            long started1 = System.currentTimeMillis();
            PrismObject<ShadowType> resourceShadow;
            try {
                localCoordinatorTask.recordIterativeOperationStart(shadow.asObjectable());
                resourceShadow = reconcileShadow(shadow, resource, localCoordinatorTask);
                localCoordinatorTask.recordIterativeOperationEnd(shadow.asObjectable(), started1, null);
            } catch (Throwable t) {
                localCoordinatorTask.recordIterativeOperationEnd(shadow.asObjectable(), started1, t);
                throw t;
            }

            if (ShadowUtil.isProtected(resourceShadow)) {
                LOGGER.trace("Skipping recording counter for {} because it is protected", shadow);
                return localCoordinatorTask.canRun();
            }

            countHolder.setValue(countHolder.getValue() + 1);
            localCoordinatorTask.incrementProgressAndStoreStatsIfNeeded();
            return localCoordinatorTask.canRun();
        };

        repositoryService.searchObjectsIterative(ShadowType.class, bucketNarrowedQuery, handler, null, true, opResult);
        interrupted = !localCoordinatorTask.canRun();

        // for each try the operation again

        opResult.computeStatus();

        LOGGER.trace("Shadow reconciliation finished, processed {} shadows for {}, result: {}",
                countHolder.getValue(), resource, opResult.getStatus());

        reconResult.setShadowReconCount(countHolder.getValue());

        result.createSubresult(OperationConstants.RECONCILIATION+".shadowReconciliation.statistics")
                .recordStatus(OperationResultStatus.SUCCESS, "Processed " + countHolder.getValue() + " shadow(s) in "
                        + (System.currentTimeMillis() - started) + " ms."
                    + (interrupted ? " Was interrupted during processing." : ""));

        return !interrupted;
    }

    private ObjectQuery addQueryFromTaskIfExists(ObjectQuery query, RunningTask localCoordinatorTask) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {

        QueryType queryType = getObjectQueryTypeFromTaskExtension(localCoordinatorTask);

        if (queryType == null || queryType.getFilter() == null) {
            return query;
        }

        ObjectFilter taskFilter = prismContext.getQueryConverter().createObjectFilter(ShadowType.class, queryType.getFilter());
        ObjectFilter evaluatedFilter = null;
        try {
            evaluatedFilter = ExpressionUtil.evaluateFilterExpressions(taskFilter, new ExpressionVariables(), MiscSchemaUtil.getExpressionProfile(),
                    expressionFactory, prismContext, "collection filter", localCoordinatorTask, localCoordinatorTask.getResult());
        } catch (SecurityViolationException e) {
            LOGGER.error("Couldn't evaluate query from task.");
            return query;
        }

        if (taskFilter == null) {
            return query;
        }

        if (query == null || query.getFilter() == null) {
            ObjectQuery taskQuery = prismContext.queryFactory().createQuery();
            taskQuery.setFilter(evaluatedFilter);
            return taskQuery;
        }

        AndFilter andFilter =  prismContext.queryFactory().createAnd(query.getFilter(), evaluatedFilter);
        ObjectQuery finalQuery = prismContext.queryFactory().createQuery(andFilter);
        provisioningService.applyDefinition(ShadowType.class, finalQuery, localCoordinatorTask, localCoordinatorTask.getResult());
        return finalQuery;

    }

    private ObjectQuery narrowQueryForBucket(ObjectQuery query, Task localCoordinatorTask,
            WorkBucketType workBucket, ObjectClassComplexTypeDefinition objectclassDef,
            OperationResult opResult) throws SchemaException, ObjectNotFoundException {
        return taskManager.narrowQueryForWorkBucket(query, ShadowType.class, itemPath -> {
            if (itemPath.startsWithName(ShadowType.F_ATTRIBUTES)) {
                return objectclassDef.findAttributeDefinition(itemPath.rest().asSingleName());
            } else {
                return null;
            }
        }, localCoordinatorTask,
                workBucket, opResult);
    }

    private PrismObject<ShadowType> reconcileShadow(PrismObject<ShadowType> shadow, PrismObject<ResourceType> resource, Task task) {
        OperationResult opResult = new OperationResult(OperationConstants.RECONCILIATION+".shadowReconciliation.object");
        try {
            Collection<SelectorOptions<GetOperationOptions>> options;
            if (TaskUtil.isDryRun(task)) {
                options = SelectorOptions.createCollection(GetOperationOptions.createDoNotDiscovery());
            } else {
                options = SelectorOptions.createCollection(GetOperationOptions.createForceRefresh());
            }
            return provisioningService.getObject(ShadowType.class, shadow.getOid(), options, task, opResult);
        } catch (ObjectNotFoundException e) {
            // Account is gone
            reactShadowGone(shadow, resource, task, opResult);        // actually, for deleted objects here is the recon code called second time
            if (opResult.isUnknown()) {
                opResult.setStatus(OperationResultStatus.HANDLED_ERROR);
            }
        } catch (CommunicationException | SchemaException | ConfigurationException | SecurityViolationException | ExpressionEvaluationException e) {
            processShadowReconError(e, shadow, opResult);
        }

        return null;
    }


    private void reactShadowGone(PrismObject<ShadowType> shadow, PrismObject<ResourceType> resource,
            Task task, OperationResult result) {
        try {
            provisioningService.applyDefinition(shadow, task, result);
            ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
            change.setSourceChannel(QNameUtil.qNameToUri(SchemaConstants.CHANNEL_RECON));
            change.setResource(resource);
            ObjectDelta<ShadowType> shadowDelta = shadow.getPrismContext().deltaFactory().object()
                    .createDeleteDelta(ShadowType.class, shadow.getOid());
            change.setObjectDelta(shadowDelta);
            // Need to also set current shadow. This will get reflected in "old" object in lens context
            change.setCurrentShadow(shadow);        // todo why current and not old [pmed]?
            ModelImplUtils.clearRequestee(task);
            changeNotificationDispatcher.notifyChange(change, task, result);
        } catch (SchemaException | ObjectNotFoundException | CommunicationException | ConfigurationException | ExpressionEvaluationException e) {
            processShadowReconError(e, shadow, result);
        }
    }

    private void processShadowReconError(Exception e, PrismObject<ShadowType> shadow, OperationResult opResult) {
        LOGGER.error("Error reconciling shadow {}: {}", shadow, e.getMessage(), e);
        opResult.recordFatalError(e);
        // TODO: store error in the shadow?
    }

    /**
     * Scans shadows for unfinished operations and tries to finish them.
     * Returns false if the reconciliation was interrupted.
     */
    private boolean scanForUnfinishedOperations(RunningTask task, String resourceOid, ReconciliationTaskResult reconResult,
            OperationResult result) throws SchemaException {
        LOGGER.trace("Scan for unfinished operations starting");
        OperationResult opResult = result.createSubresult(OperationConstants.RECONCILIATION+".repoReconciliation");
        opResult.addParam("reconciled", true);

        ObjectQuery query = prismContext.queryFor(ShadowType.class)
                .item(ShadowType.F_RESOURCE_REF).ref(resourceOid)
                .and()
                .exists(ShadowType.F_PENDING_OPERATION)
                .build();
        List<PrismObject<ShadowType>> shadows = repositoryService.searchObjects(ShadowType.class, query, null, opResult);

        task.setExpectedTotal((long) shadows.size());        // for this phase, obviously

        LOGGER.trace("Found {} accounts that were not successfully processed.", shadows.size());
        reconResult.setUnOpsCount(shadows.size());

        long startedAll = System.currentTimeMillis();
        int processedSuccess = 0, processedFailure = 0;

        for (PrismObject<ShadowType> shadow : shadows) {

            long started = System.currentTimeMillis();
            task.recordIterativeOperationStart(shadow.asObjectable());

            RepositoryCache.enterLocalCaches(cacheConfigurationManager);
            OperationResult provisioningResult = new OperationResult(OperationConstants.RECONCILIATION+".finishOperation");
            try {
                ProvisioningOperationOptions options = ProvisioningOperationOptions.createForceRetry(Boolean.TRUE);
                ModelImplUtils.clearRequestee(task);
                provisioningService.refreshShadow(shadow, options, task, provisioningResult);

                task.recordIterativeOperationEnd(shadow.asObjectable(), started, null);
                processedSuccess++;
            } catch (Throwable ex) {
                task.recordIterativeOperationEnd(shadow.asObjectable(), started, ex);
                processedFailure++;
                opResult.recordFatalError("Failed to finish operation with shadow: " + ObjectTypeUtil.toShortString(shadow.asObjectable()) +". Reason: " + ex.getMessage(), ex);
            } finally {
                task.markObjectActionExecutedBoundary();
                RepositoryCache.exitLocalCaches();
            }

            task.incrementProgressAndStoreStatsIfNeeded();

            if (!task.canRun()) {
                break;
            }
        }

        String message = "Processing unfinished operations done. Out of " + shadows.size() + " objects, "
                + processedSuccess + " were processed successfully and processing of " + processedFailure + " resulted in failure. " +
                "Total time spent: " + (System.currentTimeMillis() - startedAll) + " ms. " +
                (!task.canRun() ? "Was interrupted during processing." : "");

        opResult.computeStatus();
        result.createSubresult(opResult.getOperation()+".statistics").recordStatus(opResult.getStatus(), message);

        LOGGER.debug("{}. Result: {}", message, opResult.getStatus());
        return task.canRun();
    }

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
}
