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
import java.util.List;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.refinery.RefinedAccountDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.ChangeNotificationDispatcher;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
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
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.FailedOperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
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

		try {

			performResourceReconciliation(resourceOid, task, opResult);
			performRepoReconciliation(task, opResult);

		} catch (ObjectNotFoundException ex) {
			LOGGER.error("Reconciliation: Resource does not exist, OID: {}", resourceOid, ex);
			// This is bad. The resource does not exist. Permanent problem.
			opResult.recordFatalError("Resource does not exist, OID: " + resourceOid, ex);
			runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
			runResult.setProgress(progress);
			return runResult;
		} catch (ObjectAlreadyExistsException ex) {
			LOGGER.error("Reconciliation: Object already exist: {}", ex.getMessage(), ex);
			// This is bad. The resource does not exist. Permanent problem.
			opResult.recordFatalError("Object already exist: " + ex.getMessage(), ex);
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
			// } catch (ExpressionEvaluationException ex) {
			// LOGGER.error("Reconciliation: Error evaluating expression: {}",ex.getMessage(),ex);
			// opResult.recordFatalError("Error evaluating expression: "+ex.getMessage(),ex);
			// runResult.setRunResultStatus(TaskRunResultStatus.TEMPORARY_ERROR);
			// runResult.setProgress(progress);
			// return runResult;
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
		return runResult;
	}

	private void performResourceReconciliation(String resourceOid, Task task, OperationResult result)
			throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
			SecurityViolationException {

		OperationResult opResult = result.createSubresult(OperationConstants.RECONCILIATION+".ResourceReconciliation");
		ResourceType resource = repositoryService.getObject(ResourceType.class, resourceOid, opResult)
				.asObjectable();

		RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resource, prismContext);
		RefinedAccountDefinition refinedAccountDefinition = refinedSchema.getDefaultAccountDefinition();

		LOGGER.info("Start executing reconciliation of resource {}, reconciling object class {}",
				ObjectTypeUtil.toShortString(resource), refinedAccountDefinition);

		// Instantiate result handler. This will be called with every search
		// result in the following iterative search
		SynchronizeAccountResultHandler handler = new SynchronizeAccountResultHandler(resource,
				refinedAccountDefinition, task, changeNotificationDispatcher);
		handler.setSourceChannel(SchemaConstants.CHANGE_CHANNEL_RECON);
		handler.setProcessShortName("reconciliation");
		handler.setStopOnError(false);

		ObjectQuery query = createAccountSearchQuery(resource, refinedAccountDefinition);

		provisioningService.searchObjectsIterative(AccountShadowType.class, query, handler, opResult);

		opResult.computeStatus();
	}

	private void performRepoReconciliation(Task task, OperationResult result) throws SchemaException,
			ObjectAlreadyExistsException, CommunicationException, ObjectNotFoundException,
			ConfigurationException, SecurityViolationException {
		// find accounts
		LOGGER.debug("Start repository reconciliation");
//		QueryType query = QueryUtil.createQuery(QueryUtil.createEqualFilter(DOMUtil.getDocument(), null,
//				AccountShadowType.F_FAILED_OPERATION_TYPE, "!=null"));
		OperationResult opResult = result.createSubresult(OperationConstants.RECONCILIATION+".RepoReconciliation");
		opResult.addParam("reconciled", true);
		LOGGER.debug("Start repository reconciliation");
		List<PrismObject<AccountShadowType>> shadows = repositoryService.searchObjects(
				AccountShadowType.class, new ObjectQuery(), opResult);

		LOGGER.debug("Found {} accounts that were not successfully proccessed.", shadows.size());
		for (PrismObject<AccountShadowType> shadow : shadows) {

			// if (shadow.asObjectable().getAttemptNumber().intValue() >
			// MAX_ITERATIONS){
			// normalizeShadow(shadow, result);
			// }
			if (shadow.asObjectable().getFailedOperationType() == null){
				continue;
			}

			try {
				retryFailedOperation(shadow.asObjectable(), opResult);
			} catch (Exception ex) {
				Collection<? extends ItemDelta> modifications = PropertyDelta
						.createModificationReplacePropertyCollection(AccountShadowType.F_ATTEMPT_NUMBER,
								shadow.getDefinition(), shadow.asObjectable().getAttemptNumber() + 1);
				repositoryService.modifyObject(AccountShadowType.class, shadow.getOid(), modifications,
						opResult);
			}
		}

		// for each try the operation again
		
		opResult.computeStatus();
	}

	// private void normalizeShadow(PrismObject<AccountShadowType> shadow,
	// OperationResult parentResult){
	//
	// }

	private void retryFailedOperation(AccountShadowType shadow, OperationResult parentResult)
			throws ObjectAlreadyExistsException, SchemaException, CommunicationException,
			ObjectNotFoundException, ConfigurationException, SecurityViolationException {
		if (shadow.getFailedOperationType().equals(FailedOperationTypeType.ADD)) {
			LOGGER.debug("Trying to re-add account {}", ObjectTypeUtil.toShortString(shadow));
			addObject(shadow.asPrismObject(), parentResult);
			LOGGER.debug("Re-adding account was successful.");
			return;

		}
		if (shadow.getFailedOperationType().equals(FailedOperationTypeType.MODIFY)) {
			ObjectDeltaType shadowDelta = shadow.getObjectChange();
			Collection<? extends ItemDelta> modifications = DeltaConvertor.toModifications(
					shadowDelta.getModification(), shadow.asPrismObject().getDefinition());

			LOGGER.debug("Trying to re-modify account {}", ObjectTypeUtil.toShortString(shadow));
			modifyObject(shadow.getOid(), modifications, parentResult);
			LOGGER.debug("Re-modifying account was successful.");
			return;
		}

		if (shadow.getFailedOperationType().equals(FailedOperationTypeType.DELETE)) {
			LOGGER.debug("Trying to re-delete account {}", ObjectTypeUtil.toShortString(shadow));
			deleteObject(shadow.getOid(), parentResult);
			LOGGER.debug("Re-deleting account was successful.");
			return;
		}
	}

	private void addObject(PrismObject<AccountShadowType> shadow, OperationResult parentResult)
			throws ObjectAlreadyExistsException, SchemaException, CommunicationException,
			ObjectNotFoundException, ConfigurationException, SecurityViolationException {
		try {
			provisioningService.addObject(shadow, null, parentResult);
		} catch (ObjectAlreadyExistsException e) {
			parentResult.recordFatalError("Cannot add object: object already exist: " + e.getMessage(), e);
			throw e;
		} catch (SchemaException e) {
			parentResult.recordFatalError("Cannot add object: schema violation: " + e.getMessage(), e);
			throw e;
		} catch (CommunicationException e) {
			parentResult.recordFatalError("Cannot add object: communication problem: " + e.getMessage(), e);
			throw e;
		} catch (ObjectNotFoundException e) {
			parentResult.recordFatalError("Cannot add object: object not found: " + e.getMessage(), e);
			throw e;
		} catch (ConfigurationException e) {
			parentResult.recordFatalError("Cannot add object: configuration problem: " + e.getMessage(), e);
			throw e;
		} catch (SecurityViolationException e) {
			parentResult.recordFatalError("Cannot add object: security violation: " + e.getMessage(), e);
			throw e;
		}
	}

	private void modifyObject(String oid, Collection<? extends ItemDelta> modifications,
			OperationResult parentResult) throws ObjectNotFoundException, SchemaException,
			CommunicationException, ConfigurationException, SecurityViolationException {
		try {
			provisioningService.modifyObject(AccountShadowType.class, oid, modifications, null, parentResult);
		} catch (ObjectNotFoundException e) {
			parentResult.recordFatalError("Cannot modify object: object not found: " + e.getMessage(), e);
			throw e;
		} catch (SchemaException e) {
			parentResult.recordFatalError("Cannot modify object: schema violation: " + e.getMessage(), e);
			throw e;
		} catch (CommunicationException e) {
			parentResult
					.recordFatalError("Cannot modify object: communication problem: " + e.getMessage(), e);
			throw e;
		} catch (ConfigurationException e) {
			parentResult
					.recordFatalError("Cannot modify object: configuration problem: " + e.getMessage(), e);
			throw e;
		} catch (SecurityViolationException e) {
			parentResult.recordFatalError("Cannot modify object: security violation: " + e.getMessage(), e);
			throw e;
		}
	}

	private void deleteObject(String oid, OperationResult parentResult) throws ObjectNotFoundException,
			CommunicationException, SchemaException, ConfigurationException, SecurityViolationException {
		try {
			provisioningService.deleteObject(AccountShadowType.class, oid, null, parentResult);
		} catch (ObjectNotFoundException e) {
			parentResult.recordFatalError("Cannot delete object: object not found: " + e.getMessage(), e);
			throw e;
		} catch (CommunicationException e) {
			parentResult
					.recordFatalError("Cannot delete object: communication problem: " + e.getMessage(), e);
			throw e;
		} catch (SchemaException e) {
			parentResult.recordFatalError("Cannot delete object: schema violation: " + e.getMessage(), e);
			throw e;
		} catch (ConfigurationException e) {
			parentResult
					.recordFatalError("Cannot delete object: configuration problem: " + e.getMessage(), e);
			throw e;
		} catch (SecurityViolationException e) {
			parentResult.recordFatalError("Cannot delete object: security violation: " + e.getMessage(), e);
			throw e;
		}
	}

	private ObjectQuery createAccountSearchQuery(ResourceType resource,
			RefinedAccountDefinition refinedAccountDefinition) throws SchemaException {
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
