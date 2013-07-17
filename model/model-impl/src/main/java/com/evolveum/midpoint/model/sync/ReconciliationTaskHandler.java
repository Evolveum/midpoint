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
package com.evolveum.midpoint.model.sync;

import java.util.Collection;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.logging.LoggingUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.model.ModelConstants;
import com.evolveum.midpoint.model.util.Utils;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualsFilter;
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
import com.evolveum.midpoint.schema.constants.SchemaConstants;
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
import com.evolveum.midpoint.xml.ns._public.common.common_2a.FailedOperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowKindType;

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

	public static final String HANDLER_URI = ModelConstants.NS_SYNCHRONIZATION_TASK_PREFIX + "/reconciliation/handler-2";
	public static final long DEFAULT_SHADOW_RECONCILIATION_FRESHNESS_INTERNAL = 5 * 60 * 1000;

	@Autowired(required = true)
	private TaskManager taskManager;

	@Autowired(required = true)
	private ProvisioningService provisioningService;

	@Autowired(required = true)
	private RepositoryService repositoryService;

	@Autowired(required = true)
	private PrismContext prismContext;

	@Autowired(required = true)
	private ChangeNotificationDispatcher changeNotificationDispatcher;
	
	@Autowired(required = true)
	private AuditService auditService;

	private static final transient Trace LOGGER = TraceManager.getTrace(ReconciliationTaskHandler.class);

	private static final int SEARCH_MAX_SIZE = 100;

	private static final int MAX_ITERATIONS = 10;

	private static final int BLOCK_SIZE = 20;

	@PostConstruct
	private void initialize() {
		taskManager.registerHandler(HANDLER_URI, this);
	}

	@Override
	public TaskRunResult run(Task task) {
		LOGGER.trace("ReconciliationTaskHandler.run starting");

		long progress = task.getProgress();
		OperationResult opResult = new OperationResult(OperationConstants.RECONCILIATION);
		opResult.setStatus(OperationResultStatus.IN_PROGRESS);
		TaskRunResult runResult = new TaskRunResult();
		runResult.setOperationResult(opResult);
		String resourceOid = task.getObjectOid();
		opResult.addContext("resourceOid", resourceOid);

		if (resourceOid == null) {
			throw new IllegalArgumentException("Resource OID is missing in task extension");
		}
		
		PrismObject<ResourceType> resource;
		try {
			resource = provisioningService.getObject(ResourceType.class, resourceOid, null, opResult);
		} catch (ObjectNotFoundException ex) {
			// This is bad. The resource does not exist. Permanent problem.
			processErrorPartial(runResult, "Resource does not exist, OID: " + resourceOid, ex, TaskRunResultStatus.PERMANENT_ERROR, progress, null, task, opResult);
			return runResult;
		} catch (CommunicationException ex) {
			// Error, but not critical. Just try later.
			processErrorPartial(runResult, "Communication error", ex, TaskRunResultStatus.TEMPORARY_ERROR, progress, null, task, opResult);
			return runResult;
		} catch (SchemaException ex) {
			// Not sure about this. But most likely it is a misconfigured resource or connector
			// It may be worth to retry. Error is fatal, but may not be permanent.
			processErrorPartial(runResult, "Error dealing with schema", ex, TaskRunResultStatus.TEMPORARY_ERROR, progress, null, task, opResult);
			return runResult;
		} catch (RuntimeException ex) {
			// Can be anything ... but we can't recover from that.
			// It is most likely a programming error. Does not make much sense
			// to retry.
			processErrorPartial(runResult, "Internal Error", ex, TaskRunResultStatus.PERMANENT_ERROR, progress, null, task, opResult);
			return runResult;
		} catch (ConfigurationException ex) {
			// Not sure about this. But most likely it is a misconfigured resource or connector
			// It may be worth to retry. Error is fatal, but may not be permanent.
			processErrorPartial(runResult, "Configuration error", ex, TaskRunResultStatus.TEMPORARY_ERROR, progress, null, task, opResult);
			return runResult;
		} catch (SecurityViolationException ex) {
			processErrorPartial(runResult, "Security violation", ex, TaskRunResultStatus.PERMANENT_ERROR, progress, null, task, opResult);
			return runResult;
		}
		
		Long freshnessInterval = DEFAULT_SHADOW_RECONCILIATION_FRESHNESS_INTERNAL;
		PrismProperty<Long> freshnessIntervalProperty = task.getExtensionProperty(SchemaConstants.MODEL_EXTENSION_FRESHENESS_INTERVAL_PROPERTY_NAME);
		if (freshnessIntervalProperty != null) {
			PrismPropertyValue<Long> freshnessIntervalPropertyValue = freshnessIntervalProperty.getValue();
			if (freshnessIntervalPropertyValue == null || freshnessIntervalPropertyValue.getValue() == null || 
					freshnessIntervalPropertyValue.getValue() < 0) {
				freshnessInterval = null;
			}
			freshnessInterval = freshnessIntervalPropertyValue.getValue();
		}
		
		AuditEventRecord requestRecord = new AuditEventRecord(AuditEventType.RECONCILIATION, AuditEventStage.REQUEST);
		requestRecord.setTarget(resource);
		auditService.audit(requestRecord, task);
		
		try {
			scanForUnfinishedOperations(task, resourceOid, opResult);
		} catch (ObjectNotFoundException ex) {
			// This is bad. The resource does not exist. Permanent problem.
			processErrorPartial(runResult, "Resource does not exist, OID: " + resourceOid, ex, TaskRunResultStatus.PERMANENT_ERROR, progress, resource, task, opResult);
		} catch (ObjectAlreadyExistsException ex) {
			processErrorPartial(runResult, "Object already exist", ex, TaskRunResultStatus.PERMANENT_ERROR, progress, resource, task, opResult);
		} catch (CommunicationException ex) {
			// Error, but not critical. Just try later.
			processErrorFinal(runResult, "Communication error", ex, TaskRunResultStatus.TEMPORARY_ERROR, progress, resource, task, opResult);
			return runResult;
		} catch (SchemaException ex) {
			// Not sure about this. But most likely it is a misconfigured resource or connector
			// It may be worth to retry. Error is fatal, but may not be permanent.
			processErrorPartial(runResult, "Error dealing with schema", ex, TaskRunResultStatus.TEMPORARY_ERROR, progress, resource, task, opResult);
		} catch (RuntimeException ex) {
			// Can be anything ... but we can't recover from that.
			// It is most likely a programming error. Does not make much sense
			// to retry.
			processErrorFinal(runResult, "Internal Error", ex, TaskRunResultStatus.PERMANENT_ERROR, progress, resource, task, opResult);
			return runResult;
		} catch (ConfigurationException ex) {
			// Not sure about this. But most likely it is a misconfigured resource or connector
			// It may be worth to retry. Error is fatal, but may not be permanent.
			processErrorFinal(runResult, "Configuration error", ex, TaskRunResultStatus.TEMPORARY_ERROR, progress, resource, task, opResult);
			return runResult;
		} catch (SecurityViolationException ex) {
			processErrorPartial(runResult, "Security violation", ex, TaskRunResultStatus.PERMANENT_ERROR, progress, resource, task, opResult);
		}


		try {			
			performResourceReconciliation(resource, task, opResult);
			performShadowReconciliation(resource, freshnessInterval, task, opResult);
		} catch (ObjectNotFoundException ex) {
			// This is bad. The resource does not exist. Permanent problem.
			processErrorFinal(runResult, "Resource does not exist, OID: " + resourceOid, ex, TaskRunResultStatus.PERMANENT_ERROR, progress, resource, task, opResult);
			return runResult;
		} catch (CommunicationException ex) {
			// Error, but not critical. Just try later.
			processErrorFinal(runResult, "Communication error", ex, TaskRunResultStatus.TEMPORARY_ERROR, progress, resource, task, opResult);
			return runResult;
		} catch (SchemaException ex) {
			// Not sure about this. But most likely it is a misconfigured resource or connector
			// It may be worth to retry. Error is fatal, but may not be permanent.
			processErrorFinal(runResult, "Error dealing with schema", ex, TaskRunResultStatus.TEMPORARY_ERROR, progress, resource, task, opResult);
			return runResult;
		} catch (RuntimeException ex) {
			// Can be anything ... but we can't recover from that.
			// It is most likely a programming error. Does not make much sense
			// to retry.
			processErrorFinal(runResult, "Internal Error", ex, TaskRunResultStatus.PERMANENT_ERROR, progress, resource, task, opResult);
			return runResult;
		} catch (ConfigurationException ex) {
			// Not sure about this. But most likely it is a misconfigured resource or connector
			// It may be worth to retry. Error is fatal, but may not be permanent.
			processErrorFinal(runResult, "Configuration error", ex, TaskRunResultStatus.TEMPORARY_ERROR, progress, resource, task, opResult);
			return runResult;
		} catch (SecurityViolationException ex) {
			processErrorFinal(runResult, "Security violation", ex, TaskRunResultStatus.PERMANENT_ERROR, progress, resource, task, opResult);
			return runResult;
		}
		
		opResult.computeStatus("Reconciliation run has failed");
		// This "run" is finished. But the task goes on ...
		runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);
		runResult.setProgress(progress);
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Reconciliation.run stopping, result: {}", opResult.getStatus());
			LOGGER.trace("Reconciliation.run stopping, result: {}", opResult.dump());
		}
		
		AuditEventRecord executionRecord = new AuditEventRecord(AuditEventType.RECONCILIATION, AuditEventStage.EXECUTION);
		executionRecord.setTarget(resource);
		executionRecord.setOutcome(OperationResultStatus.SUCCESS);
		auditService.audit(executionRecord , task);
		
		return runResult;
	}

	private void processErrorFinal(TaskRunResult runResult, String errorDesc, Exception ex,
			TaskRunResultStatus runResultStatus, long progress, PrismObject<ResourceType> resource, Task task, OperationResult opResult) {
		String message = errorDesc+": "+ex.getMessage();
		LOGGER.error("Reconciliation: {}", new Object[]{message, ex});
		opResult.recordFatalError(message, ex);
		runResult.setRunResultStatus(runResultStatus);
		runResult.setProgress(progress);
		
		AuditEventRecord executionRecord = new AuditEventRecord(AuditEventType.RECONCILIATION, AuditEventStage.EXECUTION);
		executionRecord.setTarget(resource);
		executionRecord.setOutcome(OperationResultStatus.FATAL_ERROR);
		auditService.audit(executionRecord , task);
	}
	
	private void processErrorPartial(TaskRunResult runResult, String errorDesc, Exception ex,
			TaskRunResultStatus runResultStatus, long progress, PrismObject<ResourceType> resource, Task task, OperationResult opResult) {
		String message = errorDesc+": "+ex.getMessage();
		LOGGER.error("Reconciliation: {}", new Object[]{message, ex});
		opResult.recordFatalError(message, ex);
		runResult.setRunResultStatus(runResultStatus);
		runResult.setProgress(progress);
	}

	private void performResourceReconciliation(PrismObject<ResourceType> resource, Task task, OperationResult result)
			throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
			SecurityViolationException {

		OperationResult opResult = result.createSubresult(OperationConstants.RECONCILIATION+".ResourceReconciliation");
		

		RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resource, LayerType.MODEL, prismContext);
		RefinedObjectClassDefinition refinedAccountDefinition = refinedSchema.getDefaultRefinedDefinition(ShadowKindType.ACCOUNT);

		LOGGER.info("Start executing reconciliation of resource {}, reconciling object class {}",
				resource, refinedAccountDefinition);

		// Instantiate result handler. This will be called with every search
		// result in the following iterative search
		SynchronizeAccountResultHandler handler = new SynchronizeAccountResultHandler(resource.asObjectable(),
				refinedAccountDefinition, "reconciliation", task, changeNotificationDispatcher);
		handler.setSourceChannel(SchemaConstants.CHANGE_CHANNEL_RECON);
		handler.setStopOnError(false);

		ObjectQuery query = createAccountSearchQuery(resource, refinedAccountDefinition);

		OperationResult searchResult = new OperationResult(OperationConstants.RECONCILIATION+".searchIterative"); 
		provisioningService.searchObjectsIterative(ShadowType.class, query, handler, searchResult);

		opResult.computeStatus();

        result.createSubresult(OperationConstants.RECONCILIATION+".ResourceReconciliation.statistics").recordStatus(OperationResultStatus.SUCCESS, "Processed " + handler.getProgress() + " account(s), got " + handler.getErrors() + " error(s)");
	}
	
	private void performShadowReconciliation(final PrismObject<ResourceType> resource, Long freshnessInterval, final Task task, OperationResult result) throws SchemaException {
		// find accounts
		
		LOGGER.debug("Shadow reconciliation starting for {}, freshness interval {}", resource, freshnessInterval);
		OperationResult opResult = result.createSubresult(OperationConstants.RECONCILIATION+".shadowReconciliation");
		
		ObjectFilter filter;
		
		if (freshnessInterval == null) {
			filter = RefFilter.createReferenceEqual(ShadowType.class, ShadowType.F_RESOURCE_REF, prismContext, resource.getOid());
		} else {
			long timestamp = System.currentTimeMillis() - freshnessInterval;
			XMLGregorianCalendar timestampCal = XmlTypeConverter.createXMLGregorianCalendar(timestamp);
			LessFilter timestampFilter = LessFilter.createLessFilter(ShadowType.class, prismContext, 
					ShadowType.F_SYNCHRONIZATION_TIMESTAMP, timestampCal , true);
			filter = AndFilter.createAnd(timestampFilter, RefFilter.createReferenceEqual(ShadowType.class,
					ShadowType.F_RESOURCE_REF, prismContext, resource.getOid()));
		}
		
		ObjectQuery query = ObjectQuery.createObjectQuery(filter);
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Shadow recon query:\n{}", query.dump());
		}
		
		final Holder<Long> countHolder = new Holder<Long>(0L);

		Handler<PrismObject<ShadowType>> handler = new Handler<PrismObject<ShadowType>>() {
			@Override
			public void handle(PrismObject<ShadowType> shadow) {
				reconcileShadow(shadow, resource, task);
				countHolder.setValue(countHolder.getValue() + 1);
			}
		};
		Utils.searchIterative(repositoryService, ShadowType.class, query, handler , BLOCK_SIZE, opResult);
		
		// for each try the operation again
		
		opResult.computeStatus();
		
		LOGGER.debug("Shadow reconciliation finished, processed {} shadows for {}, result: {}", 
				new Object[]{countHolder.getValue(), resource, opResult.getStatus()});

        result.createSubresult(OperationConstants.RECONCILIATION+".shadowReconciliation.statistics").recordStatus(OperationResultStatus.SUCCESS, "Processed " + countHolder.getValue() + " shadow(s)");
	}
	
	private void reconcileShadow(PrismObject<ShadowType> shadow, PrismObject<ResourceType> resource, Task task) {
		OperationResult opResult = new OperationResult(OperationConstants.RECONCILIATION+".shadowReconciliation.object");
		try {
			provisioningService.getObject(ShadowType.class, shadow.getOid(), null, opResult);
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
	 */
	private void scanForUnfinishedOperations(Task task, String resourceOid, OperationResult result) throws SchemaException,
			ObjectAlreadyExistsException, CommunicationException, ObjectNotFoundException,
			ConfigurationException, SecurityViolationException {
		// find accounts
		
//		ResourceType resource = repositoryService.getObject(ResourceType.class, resourceOid, opResult)
//				.asObjectable();
		LOGGER.debug("Scan for unfinished operations starting");
		OperationResult opResult = result.createSubresult(OperationConstants.RECONCILIATION+".RepoReconciliation");
		opResult.addParam("reconciled", true);


		NotFilter notNull = NotFilter.createNot(createFailedOpFilter(null));
		AndFilter andFilter = AndFilter.createAnd(notNull, RefFilter.createReferenceEqual(ShadowType.class,
				ShadowType.F_RESOURCE_REF, prismContext, resourceOid));
		ObjectQuery query = ObjectQuery.createObjectQuery(andFilter);

		List<PrismObject<ShadowType>> shadows = repositoryService.searchObjects(
				ShadowType.class, query, opResult);

		LOGGER.trace("Found {} accounts that were not successfully processed.", shadows.size());
		
		for (PrismObject<ShadowType> shadow : shadows) {
			OperationResult provisioningResult = new OperationResult(OperationConstants.RECONCILIATION+".finishOperation");
			try {
				
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
			}
		}

		// for each try the operation again
		
		opResult.computeStatus();
		
		LOGGER.debug("Scan for unfinished operations finished, processed {} accounts, result: {}", shadows.size(), opResult.getStatus());
	}

	// private void normalizeShadow(PrismObject<AccountShadowType> shadow,
	// OperationResult parentResult){
	//
	// }

	private ObjectFilter createFailedOpFilter(FailedOperationTypeType failedOp) throws SchemaException{
		return EqualsFilter.createEqual(ShadowType.class, prismContext, ShadowType.F_FAILED_OPERATION_TYPE, failedOp);
	}
	
//	private void retryFailedOperation(AccountShadowType shadow, OperationResult parentResult)
//			throws ObjectAlreadyExistsException, SchemaException, CommunicationException,
//			ObjectNotFoundException, ConfigurationException, SecurityViolationException {
//		if (shadow.getFailedOperationType() == null){
//			return;
//		}
//		if (shadow.getFailedOperationType().equals(FailedOperationTypeType.ADD)) {
//			LOGGER.debug("Re-adding {}", shadow);
//			addObject(shadow.asPrismObject(), parentResult);
//			LOGGER.trace("Re-adding account was successful.");
//			return;
//
//		}
//		if (shadow.getFailedOperationType().equals(FailedOperationTypeType.MODIFY)) {
//			ObjectDeltaType shadowDelta = shadow.getObjectChange();
//			Collection<? extends ItemDelta> modifications = DeltaConvertor.toModifications(
//					shadowDelta.getModification(), shadow.asPrismObject().getDefinition());
//
//			LOGGER.debug("Re-modifying {}", shadow);
//			modifyObject(shadow.getOid(), modifications, parentResult);
//			LOGGER.trace("Re-modifying account was successful.");
//			return;
//		}
//
//		if (shadow.getFailedOperationType().equals(FailedOperationTypeType.DELETE)) {
//			LOGGER.debug("Re-deleting {}", shadow);
//			deleteObject(shadow.getOid(), parentResult);
//			LOGGER.trace("Re-deleting account was successful.");
//			return;
//		}
//	}
//
//	private void addObject(PrismObject<AccountShadowType> shadow, OperationResult parentResult)
//			throws ObjectAlreadyExistsException, SchemaException, CommunicationException,
//			ObjectNotFoundException, ConfigurationException, SecurityViolationException {
//		try {
//			provisioningService.addObject(shadow, null, parentResult);
//		} catch (ObjectAlreadyExistsException e) {
//			parentResult.recordFatalError("Cannot add object: object already exist: " + e.getMessage(), e);
//			throw e;
//		} catch (SchemaException e) {
//			parentResult.recordFatalError("Cannot add object: schema violation: " + e.getMessage(), e);
//			throw e;
//		} catch (CommunicationException e) {
//			parentResult.recordFatalError("Cannot add object: communication problem: " + e.getMessage(), e);
//			throw e;
//		} catch (ObjectNotFoundException e) {
//			parentResult.recordFatalError("Cannot add object: object not found: " + e.getMessage(), e);
//			throw e;
//		} catch (ConfigurationException e) {
//			parentResult.recordFatalError("Cannot add object: configuration problem: " + e.getMessage(), e);
//			throw e;
//		} catch (SecurityViolationException e) {
//			parentResult.recordFatalError("Cannot add object: security violation: " + e.getMessage(), e);
//			throw e;
//		}
//	}
//
//	private void modifyObject(String oid, Collection<? extends ItemDelta> modifications,
//			OperationResult parentResult) throws ObjectNotFoundException, SchemaException,
//			CommunicationException, ConfigurationException, SecurityViolationException, ObjectAlreadyExistsException {
//		try {
//			provisioningService.modifyObject(AccountShadowType.class, oid, modifications, null, parentResult);
//		} catch (ObjectNotFoundException e) {
//			parentResult.recordFatalError("Cannot modify object: object not found: " + e.getMessage(), e);
//			throw e;
//		} catch (SchemaException e) {
//			parentResult.recordFatalError("Cannot modify object: schema violation: " + e.getMessage(), e);
//			throw e;
//		} catch (CommunicationException e) {
//			parentResult
//					.recordFatalError("Cannot modify object: communication problem: " + e.getMessage(), e);
//			throw e;
//		} catch (ConfigurationException e) {
//			parentResult
//					.recordFatalError("Cannot modify object: configuration problem: " + e.getMessage(), e);
//			throw e;
//		} catch (SecurityViolationException e) {
//			parentResult.recordFatalError("Cannot modify object: security violation: " + e.getMessage(), e);
//			throw e;
//		} catch (ObjectAlreadyExistsException e) {
//			parentResult.recordFatalError("Cannot modify object: conflict with another object: " + e.getMessage(), e);
//			throw e;
//		}
//	}

//	private void deleteObject(String oid, OperationResult parentResult) throws ObjectNotFoundException,
//			CommunicationException, SchemaException, ConfigurationException, SecurityViolationException {
//		try {
//			provisioningService.deleteObject(AccountShadowType.class, oid, null, null, parentResult);
//		} catch (ObjectNotFoundException e) {
//			parentResult.recordFatalError("Cannot delete object: object not found: " + e.getMessage(), e);
//			throw e;
//		} catch (CommunicationException e) {
//			parentResult
//					.recordFatalError("Cannot delete object: communication problem: " + e.getMessage(), e);
//			throw e;
//		} catch (SchemaException e) {
//			parentResult.recordFatalError("Cannot delete object: schema violation: " + e.getMessage(), e);
//			throw e;
//		} catch (ConfigurationException e) {
//			parentResult
//					.recordFatalError("Cannot delete object: configuration problem: " + e.getMessage(), e);
//			throw e;
//		} catch (SecurityViolationException e) {
//			parentResult.recordFatalError("Cannot delete object: security violation: " + e.getMessage(), e);
//			throw e;
//		}
//	}

	private ObjectQuery createAccountSearchQuery(PrismObject<ResourceType> resource,
			RefinedObjectClassDefinition refinedAccountDefinition) throws SchemaException {
		QName objectClass = refinedAccountDefinition.getObjectClassDefinition().getTypeName();
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
