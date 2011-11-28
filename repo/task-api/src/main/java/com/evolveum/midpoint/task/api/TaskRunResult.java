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

import com.evolveum.midpoint.schema.result.OperationResult;

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
		/**
		 * The task run has finished.
		 * This does not necessarily mean that the task itself is finished. For single tasks this means that the task is finished, but it is different for reccurent tasks. 
		 * The task may be a cycle and it will run again after it sleeps for a while.
		 * Or it may be other type of recurrent task.
		 */
		FINISHED, 
		
		/**
		 * Task run haven't finished. It is executed in a different thread or on a different system.
		 * TODO: do we need it? Is it correct?
		 */
		IN_PROGRESS, 
		
		/**
		 * The run has failed.
		 * 
		 * The error is permanent. Unless the administrator does something to recover from the situation, there is no point in
		 * re-trying the run. Usual case of this error is task miscofiguration.
		 */
		PERMANENT_ERROR,
		
		/**
		 * Temporary failure during the run.
		 * 
		 * The error is temporary. The situation may change later when the conditions will be more "favorable". It makes sense to
		 * retry the run. Usual cases of this error are network timeouts.
		 */
		TEMPORARY_ERROR
	}
	
	private long progress;
	private TaskRunResultStatus runResultStatus;
	private OperationResult operationResult;
	
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
	
	public OperationResult getOperationResult() {
		return operationResult;
	}
	
	public void setOperationResult(OperationResult operationResult) {
		this.operationResult = operationResult;
	}
	
}
