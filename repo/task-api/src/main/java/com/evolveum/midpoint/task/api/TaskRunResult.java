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
package com.evolveum.midpoint.task.api;

/**
 * Single-purpose class to return task run results.
 * 
 * More than one value is returned, therefore it is
 * bundled into a class.
 * 
 * @author Radovan Semancik
 *
 */
public final class TaskRunResult {

	public enum TaskRunResultStatus {
		FINISHED, IN_PROGRESS
	}
	
	private long progress;
	private TaskRunResultStatus runResultStatus;
	
	/**
	 * @return the progress
	 */
	public long getProgress() {
		return progress;
	}
	/**
	 * @param progress the progress to set
	 */
	public void setProgress(long progress) {
		this.progress = progress;
	}
	/**
	 * @return the status
	 */
	public TaskRunResultStatus getRunResultStatus() {
		return runResultStatus;
	}
	/**
	 * @param status the status to set
	 */
	public void setRunResultStatus(TaskRunResultStatus status) {
		this.runResultStatus = status;
	}
	
}
