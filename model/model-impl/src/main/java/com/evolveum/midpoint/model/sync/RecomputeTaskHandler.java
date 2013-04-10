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

import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.lens.ChangeExecutor;
import com.evolveum.midpoint.model.lens.Clockwork;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.model.lens.LensFocusContext;
import com.evolveum.midpoint.model.lens.LensUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.task.api.TaskHandler;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

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
	private PrismContext prismContext;
	
    @Autowired(required = true)
    private Clockwork clockwork;
    
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
			LOGGER.error("Recompute: Object already exist: {}",ex.getMessage(),ex);
			// This is bad. The resource does not exist. Permanent problem.
			opResult.recordFatalError("Object already exist: "+ex.getMessage(),ex);
			runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
			runResult.setProgress(progress);
			return runResult;
		} catch (CommunicationException ex) {
			LOGGER.error("Recompute: Communication error: {}",ex.getMessage(),ex);
			// Error, but not critical. Just try later.
			opResult.recordPartialError("Communication error: "+ex.getMessage(),ex);
			runResult.setRunResultStatus(TaskRunResultStatus.TEMPORARY_ERROR);
			runResult.setProgress(progress);
			return runResult;
		} catch (SchemaException ex) {
			LOGGER.error("Recompute: Error dealing with schema: {}",ex.getMessage(),ex);
			// Not sure about this. But most likely it is a misconfigured resource or connector
			// It may be worth to retry. Error is fatal, but may not be permanent.
			opResult.recordFatalError("Error dealing with schema: "+ex.getMessage(),ex);
			runResult.setRunResultStatus(TaskRunResultStatus.TEMPORARY_ERROR);
			runResult.setProgress(progress);
			return runResult;
		} catch (PolicyViolationException ex) {
			LOGGER.error("Recompute: Policy violation: {}",ex.getMessage(),ex);
			opResult.recordFatalError("Policy violation: "+ex.getMessage(),ex);
			runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
			runResult.setProgress(progress);
			return runResult;
		} catch (ExpressionEvaluationException ex) {
			LOGGER.error("Recompute: Error evaluating expression: {}",ex.getMessage(),ex);
			opResult.recordFatalError("Error evaluating expression: "+ex.getMessage(),ex);
			runResult.setRunResultStatus(TaskRunResultStatus.TEMPORARY_ERROR);
			runResult.setProgress(progress);
			return runResult;
		} catch (RuntimeException ex) {
			LOGGER.error("Recompute: Internal Error: {}",ex.getMessage(),ex);
			// Can be anything ... but we can't recover from that.
			// It is most likely a programming error. Does not make much sense to retry.
			opResult.recordFatalError("Internal Error: "+ex.getMessage(),ex);
			runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
			runResult.setProgress(progress);
			return runResult;
		} catch (ConfigurationException ex) {
			LOGGER.error("Recompute: Configuration error: {}",ex.getMessage(),ex);
			// Not sure about this. But most likely it is a misconfigured resource or connector
			// It may be worth to retry. Error is fatal, but may not be permanent.
			opResult.recordFatalError("Configuration error: "+ex.getMessage(),ex);
			runResult.setRunResultStatus(TaskRunResultStatus.TEMPORARY_ERROR);
			runResult.setProgress(progress);
			return runResult;
		} catch (SecurityViolationException ex) {
			LOGGER.error("Recompute: Security violation: {}",ex.getMessage(),ex);
			opResult.recordFatalError("Security violation: "+ex.getMessage(),ex);
			runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
			runResult.setProgress(progress);
			return runResult;
		}
		
		opResult.computeStatus();
		// This "run" is finished. But the task goes on ...
		runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);
		runResult.setProgress(progress);
		LOGGER.trace("Recompute.run stopping");
		return runResult;
	}


	/**
	 * Iterate over all the users, trigger a recompute of each of them
	 */
	private void performUserRecompute(Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, 
			ExpressionEvaluationException, CommunicationException, ObjectAlreadyExistsException, ConfigurationException, 
			PolicyViolationException, SecurityViolationException {
		
//		PagingType paging = new PagingType();
		
		int offset = 0;
		while (true) {
			ObjectPaging paging = ObjectPaging.createPaging(offset, SEARCH_MAX_SIZE);
			ObjectQuery q = ObjectQuery.createObjectQuery(paging);
//			paging.setOffset(offset);
//			paging.setMaxSize(SEARCH_MAX_SIZE);
			List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, q, result);
			if (users == null || users.isEmpty()) {
				break;
			}
			for (PrismObject<UserType> user : users) {
				OperationResult subResult = result.createSubresult(OperationConstants.RECOMPUTE_USER);
				subResult.addContext(OperationResult.CONTEXT_OBJECT, user);
				recomputeUser(user, task, subResult);
				subResult.computeStatus();
			}
			offset += SEARCH_MAX_SIZE;
		}
		
		// TODO: result
		
	}

	private void recomputeUser(PrismObject<UserType> user, Task task, OperationResult result) throws SchemaException, 
			ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ObjectAlreadyExistsException, 
			ConfigurationException, PolicyViolationException, SecurityViolationException {
		LOGGER.trace("Recomputing user {}", user);
		
		
//		int rewindAttempts = 0;
//		while (true) {
//			RewindException rewindException = null;
//			
			LensContext<UserType, ShadowType> syncContext = new LensContext<UserType, ShadowType>(UserType.class, ShadowType.class, prismContext);
			LensFocusContext<UserType> focusContext = syncContext.createFocusContext();
			focusContext.setObjectOld(user);
			focusContext.setOid(user.getOid());
			syncContext.setChannel(QNameUtil.qNameToUri(SchemaConstants.CHANGE_CHANNEL_RECON));
			syncContext.setDoReconciliationForAllProjections(true);
//			
//			try {
//				
				LOGGER.trace("Recomputing of user {}: context:\n{}", user,syncContext.dump());
				
				clockwork.run(syncContext, task, result);
				
				LOGGER.trace("Recomputing of user {}: {}", user,result.getStatus());
				
				// No rewind exception, the execution was acceptable
//				break;
//				
//			} catch (RewindException e) {
//				rewindException = e;
//				LOGGER.debug("Rewind caused by {} (attempt {})", new Object[]{ e.getCause(), rewindAttempts, e.getCause()});
//				rewindAttempts++;
//			}
//			if (rewindAttempts >= Clockwork.MAX_REWIND_ATTEMPTS) {
//				Clockwork.throwException(rewindException.getCause());
//			}
//		}
		
		// TODO: process result
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
        return TaskCategory.USER_RECOMPUTATION;
    }

    @Override
    public List<String> getCategoryNames() {
        return null;
    }
}
