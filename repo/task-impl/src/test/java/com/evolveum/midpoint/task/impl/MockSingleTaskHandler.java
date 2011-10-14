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
package com.evolveum.midpoint.task.impl;

import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskHandler;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author Radovan Semancik
 *
 */
public class MockSingleTaskHandler implements TaskHandler {
	
	private static final transient Trace LOGGER = TraceManager.getTrace(MockSingleTaskHandler.class);
	
	@Override
	public TaskRunResult run(Task task) {
		LOGGER.info("MockSingle.run starting");
		
		long progress = task.getProgress();
		OperationResult opResult = new OperationResult(MockSingleTaskHandler.class.getName()+".run");
		TaskRunResult runResult = new TaskRunResult();

		runResult.setOperationResult(opResult);
		
		// TODO
		progress++;
		
		opResult.recordSuccess();
		
		// This "run" is finished. But the task goes on ...
		runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);
		runResult.setProgress(progress);
		
		LOGGER.info("MockSingle.run stopping");
		return runResult;
	}
	
	@Override
	public Long heartbeat(Task task) {
		// TODO Auto-generated method stub
		return 0L;
	}
	@Override
	public void refreshStatus(Task task) {
		// TODO Auto-generated method stub
		
	}

}
