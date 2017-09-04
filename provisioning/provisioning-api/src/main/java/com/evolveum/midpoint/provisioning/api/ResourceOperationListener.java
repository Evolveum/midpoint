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
package com.evolveum.midpoint.provisioning.api;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

/**
 *
 * This is an additional processing of the
 * result. Even if no listener instance ever handled the event then the system
 * will function normally. This is meant for
 *
 * (e.g. notify the administrator).
 *
 * 	 * The call should return without a major delay. It means that the
	 * implementation can do calls to repository, but it should not
	 * (synchronously) initiate a long-running process or provisioning request.

 *
 * @author Radovan Semancik
 *
 */
public interface ResourceOperationListener extends ProvisioningListener {

	/**
	 * Submits notification about a success of a provisioning operation.
	 *
	 * This describes the operation that was executed on the resource and it
	 * was successful. The upper layers are notified to post-process the result.
	 *
	 * It should be called for both synchronous and asynchronous operations.
	 * For synchronous operations it should provide the same result as is
	 * returned from the operation.
	 *
	 * @param parentResult the result that can be used to collect subresults of the listener execution.
	 * 						It is NOT the result of the operation that succeeded. That result is inside the
	 * 						operationDescription structure.
	 */
	public void notifySuccess(ResourceOperationDescription operationDescription, Task task, OperationResult parentResult);


	/**
	 * Submits notification about a failure of provisioning operation.
	 *
	 * This describes the operation that was executed on the resource and it
	 * failed. The upper layers are notified to handle that failure.
	 *
	 * This should be called for operations that were executed and failed
	 * to execute.
	 *
	 * This should be called only for the FINAL result. E.g. if the system will
	 * never again try the operation. If the operation has been just tried and
	 * there is an intermediary failure (e.g. a timeout) then this operation
	 * should NOT be called. notifyInProgress is used for that purpose.
	 * However, this operation should be called even for timeout-like errors
	 * if the system stops trying and will no longer attempt to retry the operation
	 * (e.g. if the attempt counter hits the maximum attempts limit).
	 *
	 * It should be called for both synchronous and asynchronous operations.
	 * For synchronous operations it should provide the same result as is
	 * returned from the operation.
	 *
	 * @param parentResult the result that can be used to collect subresults of the listener execution.
	 * 						It is NOT the result of the operation that failed. That result is inside the
	 * 						operationDescription structure.
	 */
	public void notifyFailure(ResourceOperationDescription operationDescription, Task task, OperationResult parentResult);

	/**
	 * Submits notification about provisioning operation that is in progress.
	 *
	 * This describes the operation that was tried to be executed on the resource
	 * but cannot be executed immediately. This may be caused by the resource being
	 * down (communication error), it may be caused by the mode of resource operation
	 * (e.g. asynchronous processing) or by any similar cause.
	 *
	 * This should be called after each attempt that the operation was tried. The number
	 * of attempts already tried in indicated inside operationDescription structure. This
	 * can be used e.g. to detect a first attempt to send mail to admin only once.
	 *
	 * This should NOT be called for the final result of the operation. Methods notifySuccess
	 * and notifyFailure should be used instead.
	 *
	 * @param parentResult the result that can be used to collect subresults of the listener execution.
	 * 						It is NOT the result of the operation that is in progress. That result is inside the
	 * 						operationDescription structure.
	 */
	public void notifyInProgress(ResourceOperationDescription operationDescription, Task task, OperationResult parentResult);

}
