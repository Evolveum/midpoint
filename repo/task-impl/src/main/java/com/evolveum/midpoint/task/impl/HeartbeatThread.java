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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.task.impl;

import java.util.List;
import java.util.Set;

import org.slf4j.MDC;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.DebugUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;


/**
 * 
 * @author Radovan Semancik
 *
 */
public class HeartbeatThread extends Thread {
	
	private static final transient Trace LOGGER = TraceManager.getTrace(HeartbeatThread.class);

	private int sleepInterval = 5000;
	private boolean enabled = true;
	private long lastLoopRun = 0;

	private Set<TaskRunner> runners;
	
	HeartbeatThread(Set<TaskRunner> runners) {
		super();
		this.runners = runners;
	}

	@Override
	public void run() {
		try {
			MDC.put("subsystem", "TASKMANAGER");
			
			LOGGER.info("Heartbeat thread starting (enabled:{})", enabled);
			while (enabled) {
				LOGGER.trace("Heartbeat thread loop: start");
				lastLoopRun = System.currentTimeMillis();

				OperationResult loopResult = new OperationResult(HeartbeatThread.class.getName() + ".run");
				
				for (TaskRunner runner : runners) {
					runner.heartbeat(loopResult);
				}
				
				// TODO: do something with the result
				
				if (lastLoopRun + sleepInterval > System.currentTimeMillis()) {

					// Let's sleep a while to slow down the synch, to avoid
					// overloading the system with sync polling

					LOGGER.trace("Heartbeat thread loop: going to sleep");

					try {
						Thread.sleep(sleepInterval - (System.currentTimeMillis() - lastLoopRun));
					} catch (InterruptedException ex) {
						LOGGER.trace("Task scanner got InterruptedException: " + ex);
						// Safe to ignore
					}
				}
				LOGGER.trace("Heartbeat thread loop: end");
			}
			LOGGER.info("Heartbeat thread stopping");
		} catch (Throwable t) {
			LOGGER.error("Heartbeat thread: Critical error: {}: {}", new Object[] { t, t.getMessage(), t });
		}
	}
	
	public void disable() {
		enabled = false;
	}

	public void enable() {
		enabled = true;
	}
}
