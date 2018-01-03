/**
 * Copyright (c) 2018 Evolveum
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
package com.evolveum.midpoint.provisioning.impl;

import com.evolveum.midpoint.schema.result.AsynchronousOperationResult;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType;

/**
 * @author semancik
 *
 */
public class ProvisioningOperationState<A extends AsynchronousOperationResult> implements ShortDumpable {
	
	private A asyncResult;
	private PendingOperationExecutionStatusType executionStatus;
	private String existingShadowOid;
	
	public A getAsyncResult() {
		return asyncResult;
	}
	
	public void setAsyncResult(A asyncResult) {
		this.asyncResult = asyncResult;
	}
	
	public PendingOperationExecutionStatusType getExecutionStatus() {
		return executionStatus;
	}
	
	public void setExecutionStatus(PendingOperationExecutionStatusType executionStatus) {
		this.executionStatus = executionStatus;
	}
	
	public String getExistingShadowOid() {
		return existingShadowOid;
	}
	
	public void setExistingShadowOid(String existingShadowOid) {
		this.existingShadowOid = existingShadowOid;
	}

	/**
	 * Returns true if the operation was started. It returns true
	 * if the operation is executing (in progress) or finished.
	 */
	public boolean wasStarted() {
		return executionStatus == PendingOperationExecutionStatusType.EXECUTING || executionStatus == PendingOperationExecutionStatusType.COMPLETED;
	}
	
	public boolean isCompleted() {
		return executionStatus == PendingOperationExecutionStatusType.COMPLETED;
	}
	
	public boolean isExecuting() {
		return executionStatus == PendingOperationExecutionStatusType.EXECUTING;
	}


	public OperationResultStatusType getResultStatusType() {
		OperationResultStatus resultStatus = getResultStatus();
		if (resultStatus == null) {
			return null;
		}
		return resultStatus.createStatusType();
	}
	
	public OperationResultStatus getResultStatus() {
		if (asyncResult == null || asyncResult.getOperationResult() == null) {
			return null;
		} else {
			return asyncResult.getOperationResult().getStatus();
		}
	}

	public String getAsynchronousOperationReference() {
		if (asyncResult == null || asyncResult.getOperationResult() == null) {
			return null;
		} else {
			return asyncResult.getOperationResult().getAsynchronousOperationReference();
		}
	}

	public void processAsyncResult(A asyncReturnValue) {
		setAsyncResult(asyncReturnValue);
		if (asyncReturnValue == null) {
			return;
		}
		OperationResult operationResult = asyncReturnValue.getOperationResult();
		if (operationResult == null) {
			return;
		}
		if (operationResult.isInProgress()) {
			executionStatus = PendingOperationExecutionStatusType.EXECUTING;
		} else {
			executionStatus = PendingOperationExecutionStatusType.COMPLETED;
		}
	}

	@Override
	public String toString() {
		return "ProvisioningOperationState(asyncResult=" + asyncResult + ", executionStatus="
				+ executionStatus + ", existingShadowOid=" + existingShadowOid + ")";
	}

	@Override
	public void shortDump(StringBuilder sb) {
		sb.append(executionStatus);
		if (asyncResult != null) {
			sb.append(":");
			asyncResult.shortDump(sb);
		}
	}
	
}
