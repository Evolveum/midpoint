/*
 * Copyright (c) 2010-2018 Evolveum
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

package com.evolveum.midpoint.provisioning.impl.errorhandling;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.provisioning.impl.ProvisioningOperationState;
import com.evolveum.midpoint.schema.result.AsynchronousOperationResult;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

@Component
public class ConfigurationExceptionHandler extends HardErrorHandler {
	
	private static final Trace LOGGER = TraceManager.getTrace(ConfigurationExceptionHandler.class);
	
	@Override
	protected void throwException(Exception cause, ProvisioningOperationState<? extends AsynchronousOperationResult> opState, OperationResult result)
			throws ConfigurationException {
		recordCompletionError(cause, opState, result);
		if (cause instanceof ConfigurationException) {
			throw (ConfigurationException)cause;
		} else {
			throw new ConfigurationException(cause.getMessage(), cause);
		}
	}

}
