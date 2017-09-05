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
 * This may seems too simple and maybe pointless now. But we expect
 * that it may later evolve to something like future/promise.
 *
 * @author semancik
 *
 */
public class AsynchronousOperationReturnValue<T> extends AsynchronousOperationResult {

	private T returnValue;

	public T getReturnValue() {
		return returnValue;
	}

	public void setReturnValue(T returnValue) {
		this.returnValue = returnValue;
	}

	public static <T> AsynchronousOperationReturnValue<T> wrap(T returnValue, OperationResult result) {
		AsynchronousOperationReturnValue<T> ret = new AsynchronousOperationReturnValue<>();
		ret.setOperationResult(result);
		ret.setReturnValue(returnValue);
		return ret;
	}

}
