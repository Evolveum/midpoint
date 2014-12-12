/*
 * Copyright (c) 2010-2013 Evolveum
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

import java.util.Collection;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.util.logging.LoggingUtils;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.model.impl.ModelConstants;
import com.evolveum.midpoint.model.impl.util.Utils;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.LessFilter;
import com.evolveum.midpoint.prism.query.NotFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.api.ChangeNotificationDispatcher;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.task.api.TaskHandler;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.Handler;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FailedOperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

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

		ReconciliationTaskResult reconResult = new ReconciliationTaskResult();
		
		OperationResult opResult = new OperationResult(OperationConstants.RECONCILIATION);
		opResult.setStatus(OperationResultStatus.IN_PROGRESS);
		TaskRunResult runResult = new TaskRunResult();
		runResult.setOperationResult(opResult);
		String resourceOid = coordinatorTask.getObjectOid();
		opResult.addContext("resourceOid", resourceOid);

		if (resourceOid == null) {
			throw new IllegalArgumentException("Resource OID is missing in task extension");
		}

        recordProgress(coordinatorTask, 0, opResult);
        // todo consider setting expectedTotal to null here
		
		PrismObject<ResourceType> resource;
		ObjectClassComplexTypeDefinition objectclassDef;
		try {
			resource = provisioningService.getObject(ResourceType.class, resourceOid, null, coordinatorTask, opResult);
			
			RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resource, LayerType.MODEL, prismContext);
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
		}

		reconResult.setResource(resource);
		reconResult.setObjectclassDefinition(objectclassDef);
		
		LOGGER.info("Start executing reconciliation of resource {}, reconciling object class {}",
				resource, objectclassDef);
		long reconStartTimestamp = clock.currentTimeMillis();
		
		AuditEventRecord requestRecord = new AuditEventRecord(AuditEventType.RECONCILIATION, AuditEventStage.REQUEST);
		requestRecord.setTarget(resource);
		auditService.audit(requestRecord, coordinatorTask);
		
		try {
			if (!scanForUnfinishedOperations(coordinatorTask, resourceOid, reconResult, opResult)) {
                processInterruption(runResult, resource, coordinatorTask, opResult);
                return runResult;
            }
		} catch (ObjectNotFoundException ex) {
			// This is bad. The resource does not exist. Permanent problem.
			processErrorPartial(runResult, "Resource does not exist, OID: " + resourceOid, ex, TaskRunResultStatus.PERMANENT_ERROR, resource, coordinatorTask, opResult);
		} catch (ObjectAlreadyExistsException ex) {
			processErrorPartial(runResult, "Object already exist", ex, TaskRunResultStatus.PERMANENT_ERROR, resource, coordinatorTask, opResult);
		} catch (CommunicationException ex) {
			// Error, but not critical. Just try later.
			processErrorFinal(runResult, "Communication error", ex, TaskRunResultStatus.TEMPORARY_ERROR, resource, coordinatorTask, opResult);
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
			if (!performResourceReconciliation(resource, objectclassDef, reconResult, coordinatorTask, opResult)) {
                processInterruption(runResult, resource, coordinatorTask, opResult);
                return runResult;
            }
			afterResourceReconTimestamp = clock.currentTimeMillis();
			if (!performShadowReconciliation(resource, objectclassDef, reconStartTimestamp, afterResourceReconTimestamp, reconResult, coordinatorTask, opResult)) {
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
        opResult.recordPartialError("Interrupted");
        if (LOGGER.isWarnEnabled()) {
            LOGGER.warn("Reconciliation on {} interrupted", resource);
        }
        runResult.setProgress(task.getProgress());
        runResult.setRunResultStatus(TaskRunResultStatus.INTERRUPTED);          // not strictly necessary, because using task.canRun() == false the task manager knows we were interrupted
    }

    private void processErrorFinal(TaskRunResult runResult, String errorDesc, Exception ex,
			TaskRunResultStatus runResultStatus, PrismObject<ResourceType> resource, Task task, OperationResult opResult) {
		String message = errorDesc+": "+ex.getMessage();
		LOGGER.error("Reconciliation: {}", new Object[]{message, ex});
		opResult.recordFatalError(message, ex);
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
		String message = errorDesc+": "+ex.getMessage();
		LOGGER.error("Reconciliation: {}", new Object[]{message, ex});
		opResult.recordFatalError(message, ex);
		runResult.setRunResultStatus(runResultStatus);
		runResult.setProgress(task.getProgress());
	}

    // returns false in case of execution interruption
	private boolean performResourceReconciliation(PrismObject<ResourceType> resource, ObjectClassComplexTypeDefinition objectclassDef, ReconciliationTaskResult reconResult, Task coordinatorTask, OperationResult result)
			throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
			SecurityViolationException {

        boolean interrupted;

		OperationResult opResult = result.createSubresult(OperationConstants.RECONCILIATION+".ResourceReconciliation");

		// Instantiate result handler. This will be called with every search
		// result in the following iterative search
		SynchronizeAccountResultHandler handler = new SynchronizeAccountResultHandler(resource.asObjectable(),
				objectclassDef, "reconciliation", coordinatorTask, changeNotificationDispatcher, taskManager);
		handler.setSourceChannel(SchemaConstants.CHANGE_CHANNEL_RECON);
		handler.setStopOnError(false);

		try {
			
			ObjectQuery query = createObjectclassSearchQuery(resource, objectclassDef);

			OperationResult searchResult = new OperationResult(OperationConstants.RECONCILIATION+".searchIterative");

			handler.createWorkerThreads(coordinatorTask, searchResult);
			provisioningService.searchObjectsIterative(ShadowType.class, query, null, handler, searchResult);               // note that progress is incremented within the handler, as it extends AbstractSearchIterativeResultHandler
			handler.completeProcessing(searchResult);

			interrupted = !coordinatorTask.canRun();

			opResult.computeStatus();

			String message = "Processed " + handler.getProgress() + " account(s), got " + handler.getErrors() + " error(s)";
            if (interrupted) {
                message += "; was interrupted during processing.";
            }
			if (handler.getProgress() > 0) {
				message += " Average time for one object: " + handler.getAverageTime() + " ms (wall clock time average: " + handler.getWallAverageTime() + " ms).";
			}

			OperationResultStatus resultStatus = OperationResultStatus.SUCCESS;
			if (handler.getErrors() > 0) {
				resultStatus = OperationResultStatus.PARTIAL_ERROR;
			}
			opResult.recordStatus(resultStatus, message);
			LOGGER.info("Finished resource part of {} reconciliation: {}", resource, message);
			
			reconResult.setResourceReconCount(handler.getProgress());
			reconResult.setResourceReconErrors(handler.getErrors());
			
		} catch (ConfigurationException e) {
			opResult.recordFatalError(e);
			throw e;
		} catch (SecurityViolationException e) {
			opResult.recordFatalError(e);
			throw e;
		} catch (SchemaException e) {
			opResult.recordFatalError(e);
			throw e;
		} catch (CommunicationException e) {
			opResult.recordFatalError(e);
			throw e;
		} catch (ObjectNotFoundException e) {
			opResult.recordFatalError(e);
			throw e;
		} catch (RuntimeException e) {
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
		
		LessFilter timestampFilter = LessFilter.createLess(ShadowType.F_FULL_SYNCHRONIZATION_TIMESTAMP, ShadowType.class, prismContext, 
				XmlTypeConverter.createXMLGregorianCalendar(startTimestamp) , true);
		ObjectFilter filter = AndFilter.createAnd(timestampFilter, RefFilter.createReferenceEqual(ShadowType.F_RESOURCE_REF, ShadowType.class,
				prismContext, resource.getOid()));
		
		ObjectQuery query = ObjectQuery.createObjectQuery(filter);
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Shadow recon query:\n{}", query.debugDump());
		}
		
		final Holder<Long> countHolder = new Holder<Long>(0L);

		Handler<PrismObject<ShadowType>> handler = new Handler<PrismObject<ShadowType>>() {
			@Override
			public boolean handle(PrismObject<ShadowType> shadow) {
				if ((objectclassDef instanceof RefinedObjectClassDefinition) && !((RefinedObjectClassDefinition)objectclassDef).matches(shadow.asObjectable())) {
					return true;
				}
				reconcileShadow(shadow, resource, task);
				countHolder.setValue(countHolder.getValue() + 1);
                incrementAndRecordProgress(task, new OperationResult("dummy"));     // reconcileShadow writes to its own dummy OperationResult, so we do the same here
                return task.canRun();
			}
		};
		Utils.searchIterative(repositoryService, ShadowType.class, query, handler , BLOCK_SIZE, opResult);
        interrupted = !task.canRun();
		
		// for each try the operation again
		
		opResult.computeStatus();
		
		LOGGER.trace("Shadow reconciliation finished, processed {} shadows for {}, result: {}", 
				new Object[]{countHolder.getValue(), resource, opResult.getStatus()});
		
		reconResult.setShadowReconCount(countHolder.getValue());

        result.createSubresult(OperationConstants.RECONCILIATION+".shadowReconciliation.statistics")
                .recordStatus(OperationResultStatus.SUCCESS, "Processed " + countHolder.getValue() + " shadow(s)"
                    + (interrupted ? "; was interrupted during processing" : ""));

        return !interrupted;
	}
	
	private void reconcileShadow(PrismObject<ShadowType> shadow, PrismObject<ResourceType> resource, Task task) {
		OperationResult opResult = new OperationResult(OperationConstants.RECONCILIATION+".shadowReconciliation.object");
		try {
			Collection<SelectorOptions<GetOperationOptions>> options = null;
			if (Utils.isDryRun(task)){
				 options = SelectorOptions.createCollection(GetOperationOptions.createDoNotDiscovery());
			}
			provisioningService.getObject(ShadowType.class, shadow.getOid(), options, task, opResult);
		} catch (ObjectNotFoundException e) {
			// Account is gone
			reactShadowGone(shadow, resource, task, opResult);
		} catch (CommunicationException e) {
			processShadowReconErrror(e, shadow, opResult);
		} catch (SchemaException e) {
			processShadowReconErrror(e, shadow, opResult);
		} catch (ConfigurationException e) {
			processShadowReconErrror(e, shadow, opResult);
		} catch (SecurityViolationException e) {
			processShadowReconErrror(e, shadow, opResult);
		}
	}


	private void reactShadowGone(PrismObject<ShadowType> shadow, PrismObject<ResourceType> resource, 
			Task task, OperationResult result) {
		try {
			provisioningService.applyDefinition(shadow, result);
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
		} catch (SchemaException e) {
			processShadowReconErrror(e, shadow, result);
		} catch (ObjectNotFoundException e) {
			processShadowReconErrror(e, shadow, result);
		} catch (CommunicationException e) {
			processShadowReconErrror(e, shadow, result);
		} catch (ConfigurationException e) {
			processShadowReconErrror(e, shadow, result);
		}
	}

	private void processShadowReconErrror(Exception e, PrismObject<ShadowType> shadow, OperationResult opResult) {
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
		OperationResult opResult = result.createSubresult(OperationConstants.RECONCILIATION+".RepoReconciliation");
		opResult.addParam("reconciled", true);


		NotFilter notNull = NotFilter.createNot(createFailedOpFilter(null));
		AndFilter andFilter = AndFilter.createAnd(notNull, RefFilter.createReferenceEqual(ShadowType.F_RESOURCE_REF, ShadowType.class,
				prismContext, resourceOid));
		ObjectQuery query = ObjectQuery.createObjectQuery(andFilter);

		List<PrismObject<ShadowType>> shadows = repositoryService.searchObjects(
				ShadowType.class, query, null, opResult);

		LOGGER.trace("Found {} accounts that were not successfully processed.", shadows.size());
		reconResult.setUnOpsCount(shadows.size());
		
		for (PrismObject<ShadowType> shadow : shadows) {
			OperationResult provisioningResult = new OperationResult(OperationConstants.RECONCILIATION+".finishOperation");
			try {
				RepositoryCache.enter();

				ProvisioningOperationOptions options = ProvisioningOperationOptions.createCompletePostponed(false);
                Utils.clearRequestee(task);
				provisioningService.finishOperation(shadow, options, task, provisioningResult);
//				retryFailedOperation(shadow.asObjectable(), opResult);
			} catch (Exception ex) {
				opResult.recordFatalError("Failed to finish operation with shadow: " + ObjectTypeUtil.toShortString(shadow.asObjectable()) +". Reason: " + ex.getMessage(), ex);
				Collection<? extends ItemDelta> modifications = PropertyDelta
						.createModificationReplacePropertyCollection(ShadowType.F_ATTEMPT_NUMBER,
								shadow.getDefinition(), shadow.asObjectable().getAttemptNumber() + 1);
				try {
                    repositoryService.modifyObject(ShadowType.class, shadow.getOid(), modifications,
                            provisioningResult);
				} catch(Exception e) {
                    LoggingUtils.logException(LOGGER, "Failed to record finish operation failure with shadow: " + ObjectTypeUtil.toShortString(shadow.asObjectable()), e);
				}
			} finally {
				RepositoryCache.exit();
			}

            incrementAndRecordProgress(task, opResult);

            if (!task.canRun()) {
                return false;
            }
		}

		// for each try the operation again
		
		opResult.computeStatus();
		
		LOGGER.trace("Scan for unfinished operations finished, processed {} accounts, result: {}", shadows.size(), opResult.getStatus());
        return true;
	}

	private ObjectFilter createFailedOpFilter(FailedOperationTypeType failedOp) throws SchemaException{
		return EqualFilter.createEqual(ShadowType.F_FAILED_OPERATION_TYPE, ShadowType.class, prismContext, null, failedOp);
	}
	
	private ObjectQuery createObjectclassSearchQuery(PrismObject<ResourceType> resource,
			ObjectClassComplexTypeDefinition objectClassDefinition) throws SchemaException {
		QName objectClass = objectClassDefinition.getTypeName();
		return ObjectQueryUtil.createResourceAndAccountQuery(resource.getOid(), objectClass, prismContext);
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
