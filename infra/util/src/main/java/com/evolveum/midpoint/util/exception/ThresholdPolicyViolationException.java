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
package com.evolveum.midpoint.util.exception;

import com.evolveum.midpoint.util.LocalizableMessage;

/**
 * @author katka
 *
 */
public class ThresholdPolicyViolationException extends PolicyViolationException {

	private static final long serialVersionUID = 1L;

	public ThresholdPolicyViolationException() {
	}

	public ThresholdPolicyViolationException(String message) {
		super(message);
	}

	public ThresholdPolicyViolationException(LocalizableMessage userFriendlyMessage) {
		super(userFriendlyMessage);
	}

	public ThresholdPolicyViolationException(Throwable cause) {
		super(cause);
	}

	public ThresholdPolicyViolationException(LocalizableMessage userFriendlyMessage, Throwable cause) {
		super(userFriendlyMessage, cause);
	}

	public ThresholdPolicyViolationException(String message, Throwable cause) {
		super(message, cause);
	}
	
	@Override
	public String getErrorTypeMessage() {
		return "Threshold policy violation";
	}
}
