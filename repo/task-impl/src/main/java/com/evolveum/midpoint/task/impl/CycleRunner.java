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

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskHandler;
import com.evolveum.midpoint.task.api.TaskRunResult;

/**
 * Runner class for Cycles.
 * 
 * This will be executed in its own thread. It will take care of executing the handler's run method and sleeping the thread for
 * appropriate interval.
 * 
 * @author Radovan Semancik
 *
 */
public class CycleRunner implements Runnable {
	
	private TaskHandler handler;
	private Task task;
	private boolean enabled = true;
	private long lastLoopRun = 0;
	
	private static final transient Trace logger = TraceManager.getTrace(CycleRunner.class);

	public CycleRunner(TaskHandler handler, Task task) {
		this.handler = handler;
		this.task = task;
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		logger.info("CycleRunner.run starting");
		
		while (enabled) {
			logger.trace("Task scanner loop: start");
			
			task.recordRunStart();
	
			TaskRunResult runResult = handler.run(task);

			// record this run
			
			task.recordRunFinish(runResult);
			
			// Determine how long we need to sleep and hit the bed
			
			long sleepFor = ScheduleEvaluator.determineSleepTime(task);
			try {
				Thread.sleep(sleepFor);
			} catch (InterruptedException e) {
				// Safe to ignore. Next loop iteration will check enabled status.
			}
		}
		
		// TODO: loop
		
		logger.info("CycleRunner.run stopping");

	}
	
	public void disable() {
		enabled = false;
	}

	public void enable() {
		enabled = true;
	}

}
