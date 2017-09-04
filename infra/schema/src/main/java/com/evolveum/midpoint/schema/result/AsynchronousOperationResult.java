/**
 * Copyright (c) 2017 Evolveum
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
package com.evolveum.midpoint.schema.result;

/**
 * Primary goal of this class is to support asynchronous operations.
 * The call to operation may return even if the resource operation
 * is still in progress. The IN_PROGRESS status will be indicated in
 * this class in the operation result. The result may also include
 * the asynchronous operation reference in the operational status.
 * This reference may be later used to check the status of the
 * operation.
 *
 * This may seems too simple and maybe pointless now. But we expect
 * that it may later evolve to something like future/promise.
 *
 * @author semancik
 *
 */
public class AsynchronousOperationResult {

	private OperationResult operationResult;

	public OperationResult getOperationResult() {
		return operationResult;
	}

	public void setOperationResult(OperationResult operationResult) {
		this.operationResult = operationResult;
	}

	public static AsynchronousOperationResult wrap(OperationResult result) {
		AsynchronousOperationResult ret = new AsynchronousOperationResult();
		ret.setOperationResult(result);
		return ret;
	}

	public boolean isInProgress() {
		return operationResult.isInProgress();
	}
}
