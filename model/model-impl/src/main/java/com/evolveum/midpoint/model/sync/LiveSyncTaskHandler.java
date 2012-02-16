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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskHandler;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * The task hander for a live synchronization.
 * 
 *  This handler takes care of executing live synchronization "runs". It means that the handler "run" method will
 *  be called every few seconds. The responsibility is to scan for changes that happened since the last run.
 * 
 * @author Radovan Semancik
 *
 */
@Component
public class LiveSyncTaskHandler implements TaskHandler {
	
	public static final String HANDLER_URI = "http://midpoint.evolveum.com/model/sync/handler-1";
	
	@Autowired(required=true)
	private TaskManager taskManager;
	
	@Autowired(required=true)
	private ProvisioningService provisioningService;
	
	private static final transient Trace LOGGER = TraceManager.getTrace(LiveSyncTaskHandler.class);

	@PostConstruct
	private void initialize() {
		taskManager.registerHandler(HANDLER_URI, this);
	}
	
	@Override
	public TaskRunResult run(Task task) {
		LOGGER.trace("SynchronizationCycle.run starting");
		
		long progress = task.getProgress();
		OperationResult opResult = new OperationResult(OperationConstants.LIVE_SYNC);
		TaskRunResult runResult = new TaskRunResult();
		runResult.setOperationResult(opResult);
		String resourceOid = task.getObjectOid();
		opResult.addContext("resourceOid", resourceOid);
		if (resourceOid==null) {
			// This "run" has failed. Utterly.
			opResult.recordFatalError("Resource OID is null");
			runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
			runResult.setProgress(progress);
			return runResult;
		}
		
		try {
			
			// MAIN PART
			// Calling synchronize(..) in provisioning.
			// This will detect the changes and notify model about them.
			// It will use extension of task to store synchronization state
			
			progress += provisioningService.synchronize(resourceOid, task, opResult);
			
		} catch (ObjectNotFoundException ex) {
			LOGGER.error("Live Sync: Resource does not exist, OID: {}",resourceOid);
            LOGGER.error("Exception stack trace", ex);
			// This is bad. The resource does not exist. Permanent problem.
			opResult.recordFatalError("Resource does not exist, OID: "+resourceOid,ex);
			runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
			runResult.setProgress(progress);
			return runResult;
		} catch (CommunicationException ex) {
			LOGGER.error("Live Sync: Communication error:",ex);
			// Error, but not critical. Just try later.
			opResult.recordPartialError("Communication error: "+ex.getMessage(),ex);
			runResult.setRunResultStatus(TaskRunResultStatus.TEMPORARY_ERROR);
			runResult.setProgress(progress);
			return runResult;
		} catch (SchemaException ex) {
			LOGGER.error("Live Sync: Error dealing with schema:",ex);
			// Not sure about this. But most likely it is a misconfigured resource or connector
			// It may be worth to retry. Error is fatal, but may not be permanent.
			opResult.recordFatalError("Error dealing with schema: "+ex.getMessage(),ex);
			runResult.setRunResultStatus(TaskRunResultStatus.TEMPORARY_ERROR);
			runResult.setProgress(progress);
			return runResult;
		} catch (RuntimeException ex) {
			LOGGER.error("Live Sync: Internal Error:", ex);
			// Can be anything ... but we can't recover from that.
			// It is most likely a programming error. Does not make much sense to retry.
			opResult.recordFatalError("Internal Error: "+ex.getMessage(),ex);
			runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
			runResult.setProgress(progress);
			return runResult;
		}
		
		opResult.computeStatus("Live sync run has failed");
		// This "run" is finished. But the task goes on ...
		runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);
		runResult.setProgress(progress);
		LOGGER.trace("SynchronizationCycle.run stopping");
		return runResult;
	}

	@Override
	public Long heartbeat(Task task) {
		return null;	// not to reset progress information
	}

	@Override
	public void refreshStatus(Task task) {
		// Do nothing. Everything is fresh already.		
	}

}
