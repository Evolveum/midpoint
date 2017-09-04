/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.model.impl.sync;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.model.impl.ModelConstants;
import com.evolveum.midpoint.model.impl.util.Utils;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.api.ChangeNotificationDispatcher;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.cache.RepositoryCache;
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
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.apache.commons.lang.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.List;

/**
 * The task hander for reconciliation.
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
public class ReconciliationTaskHandler implements TaskHandler {

	public static final String HANDLER_URI = ModelConstants.NS_SYNCHRONIZATION_TASK_PREFIX + "/reconciliation/handler-3";
	public static final long DEFAULT_SHADOW_RECONCILIATION_FRESHNESS_INTERNAL = 5 * 60 * 1000;

	/**
	 * Just for testability. Used in tests. Injected by explicit call to a
	 * setter.
	 */
	private ReconciliationTaskResultListener reconciliationTaskResultListener;

	@Autowired(required = true)
	private TaskManager taskManager;

	@Autowired(required = true)
	private ProvisioningService provisioningService;

	@Autowired(required = true)
	@Qualifier("cacheRepositoryService")
	private RepositoryService repositoryService;

	@Autowired(required = true)
	private PrismContext prismContext;

	@Autowired(required = true)
	private ChangeNotificationDispatcher changeNotificationDispatcher;

	@Autowired(required = true)
	private AuditService auditService;

	@Autowired(required = true)
	private Clock clock;

	private static final transient Trace LOGGER = TraceManager.getTrace(ReconciliationTaskHandler.class);

	private static final int SEARCH_MAX_SIZE = 100;

	private static final int MAX_ITERATIONS = 10;

	private static final int BLOCK_SIZE = 20;

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
	}

	@Override
	public TaskRunResult run(Task coordinatorTask) {
		LOGGER.trace("ReconciliationTaskHandler.run starting");
		coordinatorTask.startCollectingOperationStatsFromZero(true, true, true);
		try {
			return runInternal(coordinatorTask);
		} finally {
			coordinatorTask.storeOperationStats();
		}
	}

	public TaskRunResult runInternal(Task coordinatorTask) {
		ReconciliationTaskResult reconResult = new ReconciliationTaskResult();

		boolean finishOperationsOnly = BooleanUtils.isTrue(
				coordinatorTask.getExtensionPropertyRealValue(SchemaConstants.MODEL_EXTENSION_FINISH_OPERATIONS_ONLY));

		OperationResult opResult = new OperationResult(OperationConstants.RECONCILIATION);
		opResult.setStatus(OperationResultStatus.IN_PROGRESS);
		TaskRunResult runResult = new TaskRunResult();
		runResult.setOperationResult(opResult);
		String resourceOid = coordinatorTask.getObjectOid();
		opResult.addContext("resourceOid", resourceOid);

		if (coordinatorTask.getChannel() == null) {
			coordinatorTask.setChannel(SchemaConstants.CHANGE_CHANNEL_RECON_URI);
		}

		if (resourceOid == null) {
			throw new IllegalArgumentException("Resource OID is missing in task extension");
		}

        recordProgress(coordinatorTask, 0, opResult);
        // todo consider setting expectedTotal to null here

		PrismObject<ResourceType> resource;
		ObjectClassComplexTypeDefinition objectclassDef;
		try {
			resource = provisioningService.getObject(ResourceType.class, resourceOid, null, coordinatorTask, opResult);

			RefinedResourceSchema refinedSchema = RefinedResourceSchemaImpl.getRefinedSchema(resource, LayerType.MODEL, prismContext);
			objectclassDef = Utils.determineObjectClass(refinedSchema, coordinatorTask);

		} catch (ObjectNotFoundException ex) {
			// This is bad. The resource does not exist. Permanent problem.
			processErrorPartial(runResult, "Resource does not exist, OID: " + resourceOid, ex, TaskRunResultStatus.PERMANENT_ERROR, null, coordinatorTask, opResult);
			return runResult;
		} catch (CommunicationException ex) {
			// Error, but not critical. Just try later.
			processErrorPartial(runResult, "Communication error", ex, TaskRunResultStatus.TEMPORARY_ERROR, null, coordinatorTask, opResult);
			return runResult;
		} catch (SchemaException ex) {
			// Not sure about this. But most likely it is a misconfigured resource or connector
			// It may be worth to retry. Error is fatal, but may not be permanent.
			processErrorPartial(runResult, "Error dealing with schema", ex, TaskRunResultStatus.TEMPORARY_ERROR, null, coordinatorTask, opResult);
			return runResult;
		} catch (RuntimeException ex) {
			// Can be anything ... but we can't recover from that.
			// It is most likely a programming error. Does not make much sense
			// to retry.
			processErrorPartial(runResult, "Internal Error", ex, TaskRunResultStatus.PERMANENT_ERROR, null, coordinatorTask, opResult);
			return runResult;
		} catch (ConfigurationException ex) {
			// Not sure about this. But most likely it is a misconfigured resource or connector
			// It may be worth to retry. Error is fatal, but may not be permanent.
			processErrorPartial(runResult, "Configuration error", ex, TaskRunResultStatus.TEMPORARY_ERROR, null, coordinatorTask, opResult);
			return runResult;
		} catch (SecurityViolationException ex) {
			processErrorPartial(runResult, "Security violation", ex, TaskRunResultStatus.PERMANENT_ERROR, null, coordinatorTask, opResult);
			return runResult;

		} catch (ExpressionEvaluationException ex) {
			processErrorPartial(runResult, "Expression error", ex, TaskRunResultStatus.PERMANENT_ERROR, null, coordinatorTask, opResult);
			return runResult;
		}

		if (objectclassDef == null) {
			processErrorPartial(runResult, "Reconciliation without an object class specification is not supported", null, TaskRunResultStatus.PERMANENT_ERROR, null, coordinatorTask, opResult);
			return runResult;
		}

		reconResult.setResource(resource);
		reconResult.setObjectclassDefinition(objectclassDef);

		LOGGER.info("Start executing reconciliation of resource {}, reconciling object class {}, finish operations only: {}",
				resource, objectclassDef, finishOperationsOnly);
		long reconStartTimestamp = clock.currentTimeMillis();

		AuditEventRecord requestRecord = new AuditEventRecord(AuditEventType.RECONCILIATION, AuditEventStage.REQUEST);
		requestRecord.setTarget(resource);
		auditService.audit(requestRecord, coordinatorTask);

		try {
			if (!scanForUnfinishedOperations(coordinatorTask, resourceOid, reconResult, opResult)) {
                processInterruption(runResult, resource, coordinatorTask, opResult);			// appends also "last N failures" (TODO refactor)
                return runResult;
            }
		} catch (ObjectNotFoundException ex) {
			// This is bad. The resource does not exist. Permanent problem.
			processErrorPartial(runResult, "Resource does not exist, OID: " + resourceOid, ex, TaskRunResultStatus.PERMANENT_ERROR, resource, coordinatorTask, opResult);
		} catch (ObjectAlreadyExistsException ex) {
			processErrorPartial(runResult, "Object already exist", ex, TaskRunResultStatus.PERMANENT_ERROR, resource, coordinatorTask, opResult);
		} catch (CommunicationException ex) {
			// Error, but not critical. Just try later.
			processErrorFinal(runResult, "Communication error", ex, TaskRunResultStatus.TEMPORARY_ERROR, resource, coordinatorTask, opResult);	// appends also "last N failures" (TODO refactor)
			return runResult;
		} catch (SchemaException ex) {
			// Not sure about this. But most likely it is a misconfigured resource or connector
			// It may be worth to retry. Error is fatal, but may not be permanent.
			processErrorPartial(runResult, "Error dealing with schema", ex, TaskRunResultStatus.TEMPORARY_ERROR, resource, coordinatorTask, opResult);
		} catch (RuntimeException ex) {
			// Can be anything ... but we can't recover from that.
			// It is most likely a programming error. Does not make much sense
			// to retry.
			processErrorFinal(runResult, "Internal Error", ex, TaskRunResultStatus.PERMANENT_ERROR, resource, coordinatorTask, opResult);
			return runResult;
		} catch (ConfigurationException ex) {
			// Not sure about this. But most likely it is a misconfigured resource or connector
			// It may be worth to retry. Error is fatal, but may not be permanent.
			processErrorFinal(runResult, "Configuration error", ex, TaskRunResultStatus.TEMPORARY_ERROR, resource, coordinatorTask, opResult);
			return runResult;
		} catch (SecurityViolationException ex) {
			processErrorPartial(runResult, "Security violation", ex, TaskRunResultStatus.PERMANENT_ERROR, resource, coordinatorTask, opResult);
		}

		long beforeResourceReconTimestamp = clock.currentTimeMillis();
		long afterResourceReconTimestamp;
		long afterShadowReconTimestamp;
		try {
			if (!finishOperationsOnly && !performResourceReconciliation(resource, objectclassDef, reconResult, coordinatorTask, opResult)) {
                processInterruption(runResult, resource, coordinatorTask, opResult);
                return runResult;
            }
			afterResourceReconTimestamp = clock.currentTimeMillis();
			if (!finishOperationsOnly && !performShadowReconciliation(resource, objectclassDef, reconStartTimestamp, afterResourceReconTimestamp, reconResult, coordinatorTask, opResult)) {
                processInterruption(runResult, resource, coordinatorTask, opResult);
                return runResult;
            }
			afterShadowReconTimestamp = clock.currentTimeMillis();
		} catch (ObjectNotFoundException ex) {
			// This is bad. The resource does not exist. Permanent problem.
			processErrorFinal(runResult, "Resource does not exist, OID: " + resourceOid, ex, TaskRunResultStatus.PERMANENT_ERROR, resource, coordinatorTask, opResult);
			return runResult;
		} catch (CommunicationException ex) {
			// Error, but not critical. Just try later.
			processErrorFinal(runResult, "Communication error", ex, TaskRunResultStatus.TEMPORARY_ERROR, resource, coordinatorTask, opResult);
			return runResult;
		} catch (SchemaException ex) {
			// Not sure about this. But most likely it is a misconfigured resource or connector
			// It may be worth to retry. Error is fatal, but may not be permanent.
			processErrorFinal(runResult, "Error dealing with schema", ex, TaskRunResultStatus.TEMPORARY_ERROR, resource, coordinatorTask, opResult);
			return runResult;
		} catch (RuntimeException ex) {
			// Can be anything ... but we can't recover from that.
			// It is most likely a programming error. Does not make much sense
			// to retry.
			processErrorFinal(runResult, "Internal Error", ex, TaskRunResultStatus.PERMANENT_ERROR, resource, coordinatorTask, opResult);
			return runResult;
		} catch (ConfigurationException ex) {
			// Not sure about this. But most likely it is a misconfigured resource or connector
			// It may be worth to retry. Error is fatal, but may not be permanent.
			processErrorFinal(runResult, "Configuration error", ex, TaskRunResultStatus.TEMPORARY_ERROR, resource, coordinatorTask, opResult);
			return runResult;
		} catch (SecurityViolationException ex) {
			processErrorFinal(runResult, "Security violation", ex, TaskRunResultStatus.PERMANENT_ERROR, resource, coordinatorTask, opResult);
			return runResult;
		} catch (ExpressionEvaluationException ex) {
			processErrorFinal(runResult, "Expression error", ex, TaskRunResultStatus.PERMANENT_ERROR, resource, coordinatorTask, opResult);
			return runResult;
        }

		opResult.computeStatus();
		// This "run" is finished. But the task goes on ...
		runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);
		runResult.setProgress(coordinatorTask.getProgress());
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Reconciliation.run stopping, result: {}", opResult.getStatus());
//			LOGGER.trace("Reconciliation.run stopping, result: {}", opResult.dump());
		}

		AuditEventRecord executionRecord = new AuditEventRecord(AuditEventType.RECONCILIATION, AuditEventStage.EXECUTION);
		executionRecord.setTarget(resource);
		executionRecord.setOutcome(OperationResultStatus.SUCCESS);
		auditService.audit(executionRecord , coordinatorTask);

		long reconEndTimestamp = clock.currentTimeMillis();

		long etime = reconEndTimestamp - reconStartTimestamp;
		long unOpsTime = beforeResourceReconTimestamp - reconStartTimestamp;
		long resourceReconTime = afterResourceReconTimestamp - beforeResourceReconTimestamp;
		long shadowReconTime = afterShadowReconTimestamp - afterResourceReconTimestamp;
		LOGGER.info("Done executing reconciliation of resource {}, object class {}, Etime: {} ms (un-ops: {}, resource: {}, shadow: {})",
				new Object[]{resource, objectclassDef,
					etime,
					unOpsTime,
					resourceReconTime,
					shadowReconTime});

		reconResult.setRunResult(runResult);
		if (reconciliationTaskResultListener != null) {
			reconciliationTaskResultListener.process(reconResult);
		}

		TaskHandlerUtil.appendLastFailuresInformation(OperationConstants.RECONCILIATION, coordinatorTask, opResult);
		return runResult;
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
		task.setObjectRef(ObjectTypeUtil.createObjectRef(resource));

		try {
			task.setExtensionPropertyValue(ModelConstants.OBJECTCLASS_PROPERTY_NAME, objectclass);
			task.savePendingModifications(result);		// just to be sure (if the task was already persistent)
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

		// Switch task to background. This will start new thread and call
		// the run(task) method.
		// Note: the thread may be actually started on a different node
		taskManager.switchToBackground(task, result);
		result.setBackgroundTaskOid(task.getOid());
		result.computeStatus("Reconciliation launch failed");

		LOGGER.trace("Reconciliation for resource {} switched to background, control thread returning with task {}", ObjectTypeUtil.toShortString(resource), task);
	}

	private void recordProgress(Task task, long progress, OperationResult opResult) {
        try {
            task.setProgressImmediate(progress, opResult);
        } catch (ObjectNotFoundException e) {             // these exceptions are of so little probability and harmless, so we just log them and do not report higher
            LoggingUtils.logException(LOGGER, "Couldn't record progress to task {}, probably because the task does not exist anymore", e, task);
        } catch (SchemaException e) {
            LoggingUtils.logException(LOGGER, "Couldn't record progress to task {}, due to unexpected schema exception", e, task);
        }
    }

	// TODO do this only each N seconds (as in AbstractSearchIterativeResultHandler)
    private void incrementAndRecordProgress(Task task, OperationResult opResult) {
        recordProgress(task, task.getProgress() + 1, opResult);
    }

    private void processInterruption(TaskRunResult runResult, PrismObject<ResourceType> resource, Task task, OperationResult opResult) {
        opResult.recordWarning("Interrupted");
        if (LOGGER.isWarnEnabled()) {
            LOGGER.warn("Reconciliation on {} interrupted", resource);
        }
        runResult.setProgress(task.getProgress());
        runResult.setRunResultStatus(TaskRunResultStatus.INTERRUPTED);          // not strictly necessary, because using task.canRun() == false the task manager knows we were interrupted
		TaskHandlerUtil.appendLastFailuresInformation(OperationConstants.RECONCILIATION, task, opResult);	// TODO implement more seriously
    }

    private void processErrorFinal(TaskRunResult runResult, String errorDesc, Exception ex,
			TaskRunResultStatus runResultStatus, PrismObject<ResourceType> resource, Task task, OperationResult opResult) {
		String message = errorDesc+": "+ex.getMessage();
		LOGGER.error("Reconciliation: {}", new Object[]{message, ex});
		opResult.recordFatalError(message, ex);
		TaskHandlerUtil.appendLastFailuresInformation(OperationConstants.RECONCILIATION, task, opResult); // TODO implement more seriously
		runResult.setRunResultStatus(runResultStatus);
		runResult.setProgress(task.getProgress());

		AuditEventRecord executionRecord = new AuditEventRecord(AuditEventType.RECONCILIATION, AuditEventStage.EXECUTION);
		executionRecord.setTarget(resource);
		executionRecord.setOutcome(OperationResultStatus.FATAL_ERROR);
		executionRecord.setMessage(ex.getMessage());
		auditService.audit(executionRecord , task);
	}

	private void processErrorPartial(TaskRunResult runResult, String errorDesc, Exception ex,
			TaskRunResultStatus runResultStatus, PrismObject<ResourceType> resource, Task task, OperationResult opResult) {
		String message;
		if (ex == null) {
			message = errorDesc;
		} else {
			message = errorDesc+": "+ex.getMessage();
		}
		LOGGER.error("Reconciliation: {}", new Object[]{message, ex});
		opResult.recordFatalError(message, ex);
		runResult.setRunResultStatus(runResultStatus);
		runResult.setProgress(task.getProgress());
	}

    // returns false in case of execution interruption
	private boolean performResourceReconciliation(PrismObject<ResourceType> resource, ObjectClassComplexTypeDefinition objectclassDef, ReconciliationTaskResult reconResult, Task coordinatorTask, OperationResult result)
			throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
			SecurityViolationException, ExpressionEvaluationException {

        boolean interrupted;

		OperationResult opResult = result.createSubresult(OperationConstants.RECONCILIATION+".resourceReconciliation");

		// Instantiate result handler. This will be called with every search
		// result in the following iterative search
		SynchronizeAccountResultHandler handler = new SynchronizeAccountResultHandler(resource.asObjectable(),
				objectclassDef, "reconciliation", coordinatorTask, changeNotificationDispatcher, taskManager);
		handler.setSourceChannel(SchemaConstants.CHANGE_CHANNEL_RECON);
		handler.setStopOnError(false);

		coordinatorTask.setExpectedTotal(null);

		try {

			ObjectQuery query = objectclassDef.createShadowSearchQuery(resource.getOid());

			OperationResult searchResult = new OperationResult(OperationConstants.RECONCILIATION+".searchIterative");

			handler.createWorkerThreads(coordinatorTask, searchResult);
			provisioningService.searchObjectsIterative(ShadowType.class, query, null, handler, coordinatorTask, searchResult);               // note that progress is incremented within the handler, as it extends AbstractSearchIterativeResultHandler
			handler.completeProcessing(coordinatorTask, searchResult);

			interrupted = !coordinatorTask.canRun();

			opResult.computeStatus();

			String message = "Processed " + handler.getProgress() + " account(s), got " + handler.getErrors() + " error(s)";
            if (interrupted) {
                message += "; was interrupted during processing";
            }
			if (handler.getProgress() > 0) {
				message += ". Average time for one object: " + handler.getAverageTime() + " ms (wall clock time average: " + handler.getWallAverageTime() + " ms).";
			}

			OperationResultStatus resultStatus = OperationResultStatus.SUCCESS;
			if (handler.getErrors() > 0) {
				resultStatus = OperationResultStatus.PARTIAL_ERROR;
			}
			opResult.recordStatus(resultStatus, message);
			LOGGER.info("Finished resource part of {} reconciliation: {}", resource, message);

			reconResult.setResourceReconCount(handler.getProgress());
			reconResult.setResourceReconErrors(handler.getErrors());

		} catch (ConfigurationException | SecurityViolationException | SchemaException | CommunicationException | ObjectNotFoundException | ExpressionEvaluationException | RuntimeException | Error e) {
			opResult.recordFatalError(e);
			throw e;
		}
        return !interrupted;
	}

    // returns false in case of execution interruption
	private boolean performShadowReconciliation(final PrismObject<ResourceType> resource, final ObjectClassComplexTypeDefinition objectclassDef,
			long startTimestamp, long endTimestamp, ReconciliationTaskResult reconResult, final Task task, OperationResult result) throws SchemaException {
        boolean interrupted;

		// find accounts

		LOGGER.trace("Shadow reconciliation starting for {}, {} -> {}", new Object[]{resource, startTimestamp, endTimestamp});
		OperationResult opResult = result.createSubresult(OperationConstants.RECONCILIATION+".shadowReconciliation");

		ObjectQuery query = QueryBuilder.queryFor(ShadowType.class, prismContext)
				.block()
					.item(ShadowType.F_FULL_SYNCHRONIZATION_TIMESTAMP).le(XmlTypeConverter.createXMLGregorianCalendar(startTimestamp))
					.or().item(ShadowType.F_FULL_SYNCHRONIZATION_TIMESTAMP).isNull()
				.endBlock()
				.and().item(ShadowType.F_RESOURCE_REF).ref(ObjectTypeUtil.createObjectRef(resource).asReferenceValue())
				.and().item(ShadowType.F_OBJECT_CLASS).eq(objectclassDef.getTypeName())
				.build();
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Shadow recon query:\n{}", query.debugDump());
		}

		long started = System.currentTimeMillis();

		final Holder<Long> countHolder = new Holder<Long>(0L);

		ResultHandler<ShadowType> handler = new ResultHandler<ShadowType>() {
			@Override
			public boolean handle(PrismObject<ShadowType> shadow, OperationResult parentResult) {
				if ((objectclassDef instanceof RefinedObjectClassDefinition) && !((RefinedObjectClassDefinition)objectclassDef).matches(shadow.asObjectable())) {
					return true;
				}

				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Shadow reconciliation of {}, fullSynchronizationTimestamp={}", shadow, shadow.asObjectable().getFullSynchronizationTimestamp());
				}
				long started = System.currentTimeMillis();
				PrismObject<ShadowType> resourceShadow = null;
				try {
					task.recordIterativeOperationStart(shadow.asObjectable());
					resourceShadow = reconcileShadow(shadow, resource, task);
					task.recordIterativeOperationEnd(shadow.asObjectable(), started, null);
				} catch (Throwable t) {
					task.recordIterativeOperationEnd(shadow.asObjectable(), started, t);
					throw t;
				}

				if (ShadowUtil.isProtected(resourceShadow)) {
					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("Skipping recording counter for {} because it is protected", shadow);
					}
					return task.canRun();
				}

				countHolder.setValue(countHolder.getValue() + 1);
                incrementAndRecordProgress(task, new OperationResult("dummy"));     // reconcileShadow writes to its own dummy OperationResult, so we do the same here
                return task.canRun();
			}
		};

		repositoryService.searchObjectsIterative(ShadowType.class, query, handler, null, true, opResult);
        interrupted = !task.canRun();

		// for each try the operation again

		opResult.computeStatus();

		LOGGER.trace("Shadow reconciliation finished, processed {} shadows for {}, result: {}",
				new Object[]{countHolder.getValue(), resource, opResult.getStatus()});

		reconResult.setShadowReconCount(countHolder.getValue());

        result.createSubresult(OperationConstants.RECONCILIATION+".shadowReconciliation.statistics")
                .recordStatus(OperationResultStatus.SUCCESS, "Processed " + countHolder.getValue() + " shadow(s) in "
						+ (System.currentTimeMillis() - started) + " ms."
                    + (interrupted ? " Was interrupted during processing." : ""));

        return !interrupted;
	}

	private PrismObject<ShadowType> reconcileShadow(PrismObject<ShadowType> shadow, PrismObject<ResourceType> resource, Task task) {
		OperationResult opResult = new OperationResult(OperationConstants.RECONCILIATION+".shadowReconciliation.object");
		try {
			Collection<SelectorOptions<GetOperationOptions>> options = null;
			if (Utils.isDryRun(task)){
				 options = SelectorOptions.createCollection(GetOperationOptions.createDoNotDiscovery());
			}
			return provisioningService.getObject(ShadowType.class, shadow.getOid(), options, task, opResult);
		} catch (ObjectNotFoundException e) {
			// Account is gone
			reactShadowGone(shadow, resource, task, opResult);		// actually, for deleted objects here is the recon code called second time
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
			change.setSourceChannel(QNameUtil.qNameToUri(SchemaConstants.CHANGE_CHANNEL_RECON));
			change.setResource(resource);
			ObjectDelta<ShadowType> shadowDelta = ObjectDelta.createDeleteDelta(ShadowType.class, shadow.getOid(),
					shadow.getPrismContext());
			change.setObjectDelta(shadowDelta);
			// Need to also set current shadow. This will get reflected in "old" object in lens context
			change.setCurrentShadow(shadow);
            Utils.clearRequestee(task);
			changeNotificationDispatcher.notifyChange(change, task, result);
		} catch (SchemaException | ObjectNotFoundException | CommunicationException | ConfigurationException | ExpressionEvaluationException e) {
			processShadowReconError(e, shadow, result);
		}
	}

	private void processShadowReconError(Exception e, PrismObject<ShadowType> shadow, OperationResult opResult) {
		LOGGER.error("Error reconciling shadow {}: {}", new Object[]{shadow, e.getMessage(), e});
		opResult.recordFatalError(e);
		// TODO: store error in the shadow?
	}

	/**
	 * Scans shadows for unfinished operations and tries to finish them.
     * Returns false if the reconciliation was interrupted.
	 */
	private boolean scanForUnfinishedOperations(Task task, String resourceOid, ReconciliationTaskResult reconResult, OperationResult result) throws SchemaException,
			ObjectAlreadyExistsException, CommunicationException, ObjectNotFoundException,
			ConfigurationException, SecurityViolationException {
		LOGGER.trace("Scan for unfinished operations starting");
		OperationResult opResult = result.createSubresult(OperationConstants.RECONCILIATION+".repoReconciliation");
		opResult.addParam("reconciled", true);

		ObjectQuery query = QueryBuilder.queryFor(ShadowType.class, prismContext)
				.block().not().item(ShadowType.F_FAILED_OPERATION_TYPE).isNull().endBlock()
				.and().item(ShadowType.F_RESOURCE_REF).ref(resourceOid)
				.build();
		List<PrismObject<ShadowType>> shadows = repositoryService.searchObjects(ShadowType.class, query, null, opResult);

		task.setExpectedTotal((long) shadows.size());		// for this phase, obviously

		LOGGER.trace("Found {} accounts that were not successfully processed.", shadows.size());
		reconResult.setUnOpsCount(shadows.size());

		long startedAll = System.currentTimeMillis();
		int processedSuccess = 0, processedFailure = 0;

		for (PrismObject<ShadowType> shadow : shadows) {

			long started = System.currentTimeMillis();
			task.recordIterativeOperationStart(shadow.asObjectable());

			OperationResult provisioningResult = new OperationResult(OperationConstants.RECONCILIATION+".finishOperation");
			try {
				RepositoryCache.enter();

				ProvisioningOperationOptions options = ProvisioningOperationOptions.createCompletePostponed(false);
                Utils.clearRequestee(task);
				provisioningService.refreshShadow(shadow, options, task, provisioningResult);
//				retryFailedOperation(shadow.asObjectable(), opResult);

				task.recordIterativeOperationEnd(shadow.asObjectable(), started, null);
				processedSuccess++;
			} catch (Throwable ex) {
				task.recordIterativeOperationEnd(shadow.asObjectable(), started, ex);
				processedFailure++;
				opResult.recordFatalError("Failed to finish operation with shadow: " + ObjectTypeUtil.toShortString(shadow.asObjectable()) +". Reason: " + ex.getMessage(), ex);
				Collection<? extends ItemDelta> modifications = PropertyDelta
						.createModificationReplacePropertyCollection(ShadowType.F_ATTEMPT_NUMBER,
								shadow.getDefinition(), shadow.asObjectable().getAttemptNumber() + 1);
				try {
                    repositoryService.modifyObject(ShadowType.class, shadow.getOid(), modifications,
							provisioningResult);
					task.recordObjectActionExecuted(shadow, null, null, ChangeType.MODIFY, SchemaConstants.CHANGE_CHANNEL_RECON_URI, null);
				} catch(Exception e) {
					task.recordObjectActionExecuted(shadow, null, null, ChangeType.MODIFY, SchemaConstants.CHANGE_CHANNEL_RECON_URI, e);
                    LoggingUtils.logException(LOGGER, "Failed to record finish operation failure with shadow: " + ObjectTypeUtil.toShortString(shadow.asObjectable()), e);
				}
			} finally {
				task.markObjectActionExecutedBoundary();
				RepositoryCache.exit();
			}

			// TODO record statistics as well
            incrementAndRecordProgress(task, opResult);

            if (!task.canRun()) {
                break;
            }
		}

		task.setExpectedTotal(null);		// for next phases, it looks strangely to see progress e.g. 2/1

		// for each try the operation again

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
		// TODO Auto-generated method stub
		return 0L;
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
    public List<String> getCategoryNames() {
        return null;
    }
}
