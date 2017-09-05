/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
