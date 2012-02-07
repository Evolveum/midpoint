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

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.common.refinery.RefinedAccountDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.model.ChangeExecutor;
import com.evolveum.midpoint.model.SyncContext;
import com.evolveum.midpoint.model.importer.ImportAccountsFromResourceTaskHandler;
import com.evolveum.midpoint.model.synchronizer.UserSynchronizer;
import com.evolveum.midpoint.prism.SchemaRegistry;
import com.evolveum.midpoint.provisioning.api.ChangeNotificationDispatcher;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResultHandler;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ResultList;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.exception.CommunicationException;
import com.evolveum.midpoint.schema.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.schema.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.processor.MidPointObject;
import com.evolveum.midpoint.schema.processor.Schema;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskHandler;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * The task hander for user recompute.
 * 
 *  This handler takes care of executing recompute "runs". The task will iterate over all users
 *  and recompute their assignments and expressions. This is needed after the expressions are changed,
 *  e.g in resource outbound expressions or in a role definition.
 * 
 * @author Radovan Semancik
 *
 */
@Component
public class RecomputeTaskHandler implements TaskHandler {
	
	public static final String HANDLER_URI = "http://midpoint.evolveum.com/model/sync/recompute-handler-1";
	
	@Autowired(required=true)
	private TaskManager taskManager;
	
	@Autowired(required=true)
	private RepositoryService repositoryService;
	
	@Autowired(required=true)
	private SchemaRegistry schemaRegistry;
	
    @Autowired(required = true)
    private UserSynchronizer userSynchronizer;
    
    @Autowired
    private ChangeExecutor changeExecutor;
    	
	private static final transient Trace LOGGER = TraceManager.getTrace(RecomputeTaskHandler.class);

	private static final int SEARCH_MAX_SIZE = 100;

	@PostConstruct
	private void initialize() {
		taskManager.registerHandler(HANDLER_URI, this);
	}
	
	@Override
	public TaskRunResult run(Task task) {
		LOGGER.trace("RecomputeTaskHandler.run starting");
		
		long progress = task.getProgress();
		OperationResult opResult = new OperationResult(OperationConstants.RECOMPUTE);
		TaskRunResult runResult = new TaskRunResult();
		runResult.setOperationResult(opResult);
//		String roleOid = task.getObjectOid();
//		opResult.addContext("roleOid", roleOid);

		try {

			performUserRecompute(task, opResult);
					
		} catch (ObjectNotFoundException ex) {
			LOGGER.error("Recompute: Object does not exist: {}",ex.getMessage(),ex);
			// This is bad. The resource does not exist. Permanent problem.
			opResult.recordFatalError("Object does not exist: "+ex.getMessage(),ex);
			runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
			runResult.setProgress(progress);
			return runResult;
		} catch (ObjectAlreadyExistsException ex) {
			LOGGER.error("Reconciliation: Object already exist: {}",ex.getMessage(),ex);
			// This is bad. The resource does not exist. Permanent problem.
			opResult.recordFatalError("Object already exist: "+ex.getMessage(),ex);
			runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
			runResult.setProgress(progress);
			return runResult;
		} catch (CommunicationException ex) {
			LOGGER.error("Reconciliation: Communication error: {}",ex.getMessage(),ex);
			// Error, but not critical. Just try later.
			opResult.recordPartialError("Communication error: "+ex.getMessage(),ex);
			runResult.setRunResultStatus(TaskRunResultStatus.TEMPORARY_ERROR);
			runResult.setProgress(progress);
			return runResult;
		} catch (SchemaException ex) {
			LOGGER.error("Reconciliation: Error dealing with schema: {}",ex.getMessage(),ex);
			// Not sure about this. But most likely it is a misconfigured resource or connector
			// It may be worth to retry. Error is fatal, but may not be permanent.
			opResult.recordFatalError("Error dealing with schema: "+ex.getMessage(),ex);
			runResult.setRunResultStatus(TaskRunResultStatus.TEMPORARY_ERROR);
			runResult.setProgress(progress);
			return runResult;
		} catch (ExpressionEvaluationException ex) {
			LOGGER.error("Reconciliation: Error evaluating expression: {}",ex.getMessage(),ex);
			opResult.recordFatalError("Error evaluating expression: "+ex.getMessage(),ex);
			runResult.setRunResultStatus(TaskRunResultStatus.TEMPORARY_ERROR);
			runResult.setProgress(progress);
			return runResult;
		} catch (RuntimeException ex) {
			LOGGER.error("Reconciliation: Internal Error: {}",ex.getMessage(),ex);
			// Can be anything ... but we can't recover from that.
			// It is most likely a programming error. Does not make much sense to retry.
			opResult.recordFatalError("Internal Error: "+ex.getMessage(),ex);
			runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
			runResult.setProgress(progress);
			return runResult;
		}
		
		opResult.computeStatus("Reconciliation run has failed");
		// This "run" is finished. But the task goes on ...
		runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);
		runResult.setProgress(progress);
		LOGGER.trace("Reconciliation.run stopping");
		return runResult;
	}


	/**
	 * Iterate over all the users, trigger a recompute of each of them
	 */
	private void performUserRecompute(Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ObjectAlreadyExistsException {
		
		PagingType paging = new PagingType();
		
		int offset = 0;
		while (true) {
			paging.setOffset(offset);
			paging.setMaxSize(SEARCH_MAX_SIZE);
			ResultList<UserType> users = repositoryService.listObjects(UserType.class, paging, result);
			if (users == null || users.isEmpty()) {
				break;
			}
			for (UserType user : users) {
				OperationResult subResult = result.createSubresult(OperationConstants.RECOMPUTE_USER);
				subResult.addContext(OperationResult.CONTEXT_OBJECT, user);
				recomputeUser(user, subResult);
			}
			offset += SEARCH_MAX_SIZE;
		}
		
		// TODO: result
		
	}

	private void recomputeUser(UserType user, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ObjectAlreadyExistsException {
		LOGGER.trace("Reconciling user {}",ObjectTypeUtil.toShortString(user));
		
		SyncContext syncContext = new SyncContext();
		syncContext.setUserTypeOld(user);
		Schema objectSchema = schemaRegistry.getObjectSchema();
		MidPointObject<UserType> mpUser = objectSchema.parseObjectType(user);
		syncContext.setUserOld(mpUser);
		syncContext.setUserOid(user.getOid());

		syncContext.setChannel(QNameUtil.qNameToUri(SchemaConstants.CHANGE_CHANNEL_RECON));
		syncContext.setDoReconciliationForAllAccounts(true);
		
		userSynchronizer.synchronizeUser(syncContext, result);
		
		LOGGER.trace("Reconciling of user {}: context:\n{}",ObjectTypeUtil.toShortString(user),syncContext.dump());
		
		changeExecutor.executeChanges(syncContext, result);
		
		// TODO: process result
		
		LOGGER.trace("Reconciling of user {}: {}",ObjectTypeUtil.toShortString(user),result.getStatus());
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

}
