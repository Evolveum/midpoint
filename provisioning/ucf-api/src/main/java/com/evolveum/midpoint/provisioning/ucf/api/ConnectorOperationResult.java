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
package com.evolveum.midpoint.provisioning.ucf.api;

import com.evolveum.midpoint.schema.result.OperationResult;

/**
 * Primary goal of this class is to support asynchronous operations.
 * The call to UCF operation may return even if the resource operation
 * is still in progress. The IN_PROGRESS status will be indicated in
 * this class in the operation result. The connector may also include 
 * the asynchronous operation reference in the operational status.
 * This reference may be later used to check the status of the
 * operation. 
 * 
 * @author semancik
 *
 */
public class ConnectorOperationResult {

	private OperationResult operationResult;

	public OperationResult getOperationResult() {
		return operationResult;
	}

	public void setOperationResult(OperationResult operationResult) {
		this.operationResult = operationResult;
	}
	
	public static ConnectorOperationResult wrap(OperationResult result) {
		ConnectorOperationResult ret = new ConnectorOperationResult();
		ret.setOperationResult(result);
		return ret;
	}
	
}
