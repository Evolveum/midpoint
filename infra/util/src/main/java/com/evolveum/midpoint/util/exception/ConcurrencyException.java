/*
 * Copyright (c) 2010-2017 Evolveum
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
 * Exceptional concurrency state or operation invocation.
 * 
 * This exception is thrown in case of race conditions and similar conflicting concurrency conditions.
 * It is also thrown in an attempt to acquire already acquired locks and similar cases.
 * 
 * This condition is implemented as exception in a hope that it will help avoid silently ignoring the
 * concurrency problems and that the developers will be forced to handle the condition.
 * It is much easier to ignore a return value than to ignore an exception.
 * 
 * @author Radovan Semancik
 *
 */
public class ConcurrencyException extends CommonException {
	private static final long serialVersionUID = 1L;

	public ConcurrencyException() {
	}

	public ConcurrencyException(String message) {
		super(message);
	}
	
	public ConcurrencyException(LocalizableMessage userFriendlyMessage) {
		super(userFriendlyMessage);
	}

	public ConcurrencyException(Throwable cause) {
		super(cause);
	}

	public ConcurrencyException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public ConcurrencyException(LocalizableMessage userFriendlyMessage, Throwable cause) {
		super(userFriendlyMessage, cause);
	}

	@Override
	public String getErrorTypeMessage() {
		return "Concurrency exception";
	}
	
	

}
