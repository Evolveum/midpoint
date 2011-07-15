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

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.result.OperationConstants;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskHandler;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationalResultType;

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
	
	private static final transient Trace logger = TraceManager.getTrace(LiveSyncTaskHandler.class);

	@PostConstruct
	private void initialize() {
		taskManager.registerHandler(HANDLER_URI, this);
	}
	
	@Override
	public TaskRunResult run(Task task) {
		logger.info("SynchronizationCycle.run starting");
		
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
		
		// TODO call sync in provisioning with resourceOid

		// Temporary "fake"
		progress++;
		
		opResult.computeStatus("Live sync run has failed");
		// This "run" is finished. But the task goes on ...
		runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);
		runResult.setProgress(progress);
		logger.info("SynchronizationCycle.run stopping");
		return runResult;
	}

	@Override
	public long heartbeat(Task task) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void refreshStatus(Task task) {
		// Do nothing. Everything is fresh already.		
	}

}
