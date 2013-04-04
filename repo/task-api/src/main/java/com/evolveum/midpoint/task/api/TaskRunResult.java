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
         *
		 * This does not necessarily mean that the task itself is finished. For single tasks this means that
         * the task is finished, but it is different for recurrent tasks. Such a task will run again after
         * it sleeps for a while (or after the scheduler will start it again).
		 */
		FINISHED,

        /**
         * The task run has finished, and this was the last run of the current handler.
         *
         * For single-run tasks, the effect is the same as of FINISHED value.
         * However, for recurring tasks, this return value causes current handler to be removed from the handler stack.
         */

        FINISHED_HANDLER,

        /**
         * The run has failed.
         *
         * The error is permanent. Unless the administrator does something to recover from the situation, there is no point in
         * re-trying the run. Usual case of this error is task misconfiguration.
         */
		PERMANENT_ERROR,
		
		/**
		 * Temporary failure during the run.
		 * 
		 * The error is temporary. The situation may change later when the conditions will be more "favorable".
         * It makes sense to retry the run. Usual cases of this error are network timeouts.
         *
         * For single-run tasks we SUSPEND them on such occasion. So the administrator can release them after
         * correcting the problem.
		 */
		TEMPORARY_ERROR,

        /**
         * Task run hasn't finished but nevertheless it must end (for now). An example of such a situation is
         * when the long-living task run execution is requested to stop (e.g. when suspending the task or
         * shutting down the node).
         *
         * For single-run tasks this state means that the task SHOULD NOT be closed, nor the handler should
         * be removed from the handler stack.
         */
        INTERRUPTED,

        /**
         * Task has to be restarted, typically because a new handler was put onto the handler stack during
         * the task run.
         */
        RESTART_REQUESTED

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
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((operationResult == null) ? 0 : operationResult.hashCode());
		result = prime * result + (int) (progress ^ (progress >>> 32));
		result = prime * result
				+ ((runResultStatus == null) ? 0 : runResultStatus.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TaskRunResult other = (TaskRunResult) obj;
		if (operationResult == null) {
			if (other.operationResult != null)
				return false;
		} else if (!operationResult.equals(other.operationResult))
			return false;
		if (progress != other.progress)
			return false;
		if (runResultStatus != other.runResultStatus)
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return "TaskRunResult(progress=" + progress + ", status="
				+ runResultStatus + ", result=" + operationResult
				+ ")";
	}
	
}
