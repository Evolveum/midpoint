/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted 2011 [name of copyright owner]"
 * 
 */
package com.evolveum.midpoint.model.sync;

import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.model.util.Utils;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.LessFilter;
import com.evolveum.midpoint.prism.query.NotFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrFilter;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.api.ChangeNotificationDispatcher;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.DeltaConvertor;
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
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowKindType;
import com.evolveum.prism.xml.ns._public.types_2.ObjectDeltaType;

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

	public static final String HANDLER_URI = "http://midpoint.evolveum.com/model/sync/reconciliation-handler-1";
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
		
		Long freshnessInterval = DEFAULT_SHADOW_RECONCILIATION_FRESHNESS_INTERNAL;
		PrismProperty<Long> freshnessIntervalProperty = task.getExtension(SynchronizationConstants.FRESHENESS_INTERVAL_PROPERTY_NAME);
		if (freshnessIntervalProperty != null) {
			PrismPropertyValue<Long> freshnessIntervalPropertyValue = freshnessIntervalProperty.getValue();
			if (freshnessIntervalPropertyValue == null || freshnessIntervalPropertyValue.getValue() == null || 
					freshnessIntervalPropertyValue.getValue() < 0) {
				freshnessInterval = null;
			}
			freshnessInterval = freshnessIntervalPropertyValue.getValue();
		}
		
		try {
			scanForUnfinishedOperations(task, resourceOid, opResult);
		} catch (ObjectNotFoundException ex) {
			LOGGER.error("Reconciliation: Object does not exist, OID: {}", ex.getMessage(), ex);
			// This is bad. The resource does not exist. Permanent problem.
			opResult.recordPartialError("Resource does not exist, OID: " + resourceOid, ex);
			runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
			runResult.setProgress(progress);
//			return runResult;
		} catch (ObjectAlreadyExistsException ex) {
			LOGGER.error("Reconciliation: Object already exist: {}", ex.getMessage(), ex);
			// This is bad. The resource does not exist. Permanent problem.
			opResult.recordPartialError("Object already exist: " + ex.getMessage(), ex);
			runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
			runResult.setProgress(progress);
//			return runResult;
		} catch (CommunicationException ex) {
			LOGGER.error("Reconciliation: Communication error: {}", ex.getMessage(), ex);
			// Error, but not critical. Just try later.
			opResult.recordPartialError("Communication error: " + ex.getMessage(), ex);
			runResult.setRunResultStatus(TaskRunResultStatus.TEMPORARY_ERROR);
			runResult.setProgress(progress);
			return runResult;
		} catch (SchemaException ex) {
			LOGGER.error("Reconciliation: Error dealing with schema: {}", ex.getMessage(), ex);
			// Not sure about this. But most likely it is a misconfigured
			// resource or connector
			// It may be worth to retry. Error is fatal, but may not be
			// permanent.
			opResult.recordFatalError("Error dealing with schema: " + ex.getMessage(), ex);
			runResult.setRunResultStatus(TaskRunResultStatus.TEMPORARY_ERROR);
			runResult.setProgress(progress);
//			return runResult;
		} catch (RuntimeException ex) {
			LOGGER.error("Reconciliation: Internal Error: {}", ex.getMessage(), ex);
			// Can be anything ... but we can't recover from that.
			// It is most likely a programming error. Does not make much sense
			// to retry.
			opResult.recordFatalError("Internal Error: " + ex.getMessage(), ex);
			runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
			runResult.setProgress(progress);
			return runResult;
		} catch (ConfigurationException ex) {
			LOGGER.error("Reconciliation: Configuration error: {}", ex.getMessage(), ex);
			// Not sure about this. But most likely it is a misconfigured
			// resource or connector
			// It may be worth to retry. Error is fatal, but may not be
			// permanent.
			opResult.recordFatalError("Error dealing with schema: " + ex.getMessage(), ex);
			runResult.setRunResultStatus(TaskRunResultStatus.TEMPORARY_ERROR);
			runResult.setProgress(progress);
			return runResult;
		} catch (SecurityViolationException ex) {
			LOGGER.error("Recompute: Security violation: {}", ex.getMessage(), ex);
			opResult.recordFatalError("Security violation: " + ex.getMessage(), ex);
			runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
			runResult.setProgress(progress);
//			return runResult;
		}


		try {
			PrismObject<ResourceType> resource = repositoryService.getObject(ResourceType.class, resourceOid, opResult);
			
			performResourceReconciliation(resource, task, opResult);
			performShadowReconciliation(resource, freshnessInterval, task, opResult);

		} catch (ObjectNotFoundException ex) {
			LOGGER.error("Reconciliation: Resource does not exist, OID: {}", resourceOid, ex);
			// This is bad. The resource does not exist. Permanent problem.
			opResult.recordFatalError("Resource does not exist, OID: " + resourceOid, ex);
			runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
			runResult.setProgress(progress);
			return runResult;
		} catch (CommunicationException ex) {
			LOGGER.error("Reconciliation: Communication error: {}", ex.getMessage(), ex);
			// Error, but not critical. Just try later.
			opResult.recordPartialError("Communication error: " + ex.getMessage(), ex);
			runResult.setRunResultStatus(TaskRunResultStatus.TEMPORARY_ERROR);
			runResult.setProgress(progress);
			return runResult;
		} catch (SchemaException ex) {
			LOGGER.error("Reconciliation: Error dealing with schema: {}", ex.getMessage(), ex);
			// Not sure about this. But most likely it is a misconfigured
			// resource or connector
			// It may be worth to retry. Error is fatal, but may not be
			// permanent.
			opResult.recordFatalError("Error dealing with schema: " + ex.getMessage(), ex);
			runResult.setRunResultStatus(TaskRunResultStatus.TEMPORARY_ERROR);
			runResult.setProgress(progress);
			return runResult;
		} catch (RuntimeException ex) {
			LOGGER.error("Reconciliation: Internal Error: {}", ex.getMessage(), ex);
			// Can be anything ... but we can't recover from that.
			// It is most likely a programming error. Does not make much sense
			// to retry.
			opResult.recordFatalError("Internal Error: " + ex.getMessage(), ex);
			runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
			runResult.setProgress(progress);
			return runResult;
		} catch (ConfigurationException ex) {
			LOGGER.error("Reconciliation: Configuration error: {}", ex.getMessage(), ex);
			// Not sure about this. But most likely it is a misconfigured
			// resource or connector
			// It may be worth to retry. Error is fatal, but may not be
			// permanent.
			opResult.recordFatalError("Error dealing with schema: " + ex.getMessage(), ex);
			runResult.setRunResultStatus(TaskRunResultStatus.TEMPORARY_ERROR);
			runResult.setProgress(progress);
			return runResult;
		} catch (SecurityViolationException ex) {
			LOGGER.error("Recompute: Security violation: {}", ex.getMessage(), ex);
			opResult.recordFatalError("Security violation: " + ex.getMessage(), ex);
			runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
			runResult.setProgress(progress);
			return runResult;
		}
		
		opResult.computeStatus("Reconciliation run has failed");
		// This "run" is finished. But the task goes on ...
		runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);
		runResult.setProgress(progress);
		LOGGER.trace("Reconciliation.run stopping, result: {}", opResult.getStatus());
		LOGGER.trace("Reconciliation.run stopping, result: {}", opResult.dump());
		return runResult;
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
				refinedAccountDefinition, task, changeNotificationDispatcher);
		handler.setSourceChannel(SchemaConstants.CHANGE_CHANNEL_RECON);
		handler.setProcessShortName("reconciliation");
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
		LOGGER.debug("Scan for unifinished operations starting");
		OperationResult opResult = result.createSubresult(OperationConstants.RECONCILIATION+".RepoReconciliation");
		opResult.addParam("reconciled", true);


		NotFilter notNull = NotFilter.createNot(createFailedOpFilter(null));
		AndFilter andFilter = AndFilter.createAnd(notNull, RefFilter.createReferenceEqual(ShadowType.class,
				ShadowType.F_RESOURCE_REF, prismContext, resourceOid));
		ObjectQuery query = ObjectQuery.createObjectQuery(andFilter);

		List<PrismObject<ShadowType>> shadows = repositoryService.searchObjects(
				ShadowType.class, query, opResult);

		LOGGER.trace("Found {} accounts that were not successfully proccessed.", shadows.size());
		
		for (PrismObject<ShadowType> shadow : shadows) {
			OperationResult provisioningResult = new OperationResult(OperationConstants.RECONCILIATION+".finishOperation");
			try {
				
				ProvisioningOperationOptions options = ProvisioningOperationOptions.createCompletePostponed(false);
				provisioningService.finishOperation(shadow, options, task, provisioningResult);
//				retryFailedOperation(shadow.asObjectable(), opResult);
			} catch (Exception ex) {
				opResult.recordFatalError("Failed to finish operation with shadow: " + ObjectTypeUtil.toShortString(shadow.asObjectable()) +". Reason: " + ex.getMessage(), ex);
				Collection<? extends ItemDelta> modifications = PropertyDelta
						.createModificationReplacePropertyCollection(ShadowType.F_ATTEMPT_NUMBER,
								shadow.getDefinition(), shadow.asObjectable().getAttemptNumber() + 1);
				try{
				repositoryService.modifyObject(ShadowType.class, shadow.getOid(), modifications,
						provisioningResult);
				}catch(Exception e){
					
				}
			}
		}

		// for each try the operation again
		
		opResult.computeStatus();
		
		LOGGER.debug("Scan for unifinished operations finished, processed {} accounts, result: {}", shadows.size(), opResult.getStatus());
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
