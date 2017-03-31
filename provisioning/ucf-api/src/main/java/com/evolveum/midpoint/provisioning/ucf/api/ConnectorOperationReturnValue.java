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
 * @author semancik
 *
 */
public class ConnectorOperationReturnValue<T> extends ConnectorOperationResult {

	private T returnValue;

	public T getReturnValue() {
		return returnValue;
	}

	public void setReturnValue(T returnValue) {
		this.returnValue = returnValue;
	}
	
	public static <T> ConnectorOperationReturnValue<T> wrap(T returnValue, OperationResult result) {
		ConnectorOperationReturnValue<T> ret = new ConnectorOperationReturnValue<>();
		ret.setOperationResult(result);
		ret.setReturnValue(returnValue);
		return ret;
	}
	
}
