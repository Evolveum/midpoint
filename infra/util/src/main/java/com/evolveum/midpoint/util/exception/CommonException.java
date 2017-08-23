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
 * Superclass for all common midPoint exceptions.
 * 
 * 
 * @author Radovan Semancik
 */
public abstract class CommonException extends Exception {
	
	LocalizableMessage userFriendlyMessage;

	public CommonException() {
	}

	public CommonException(String message) {
		super(message);
	}
	
	public CommonException(LocalizableMessage userFriendlyMessage) {
		super(userFriendlyMessage.getFallbackMessage());
		this.userFriendlyMessage = userFriendlyMessage;
	}

	public CommonException(Throwable cause) {
		super(cause);
	}

	public CommonException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public CommonException(LocalizableMessage userFriendlyMessage, Throwable cause) {
		super(userFriendlyMessage.getFallbackMessage(), cause);
		this.userFriendlyMessage = userFriendlyMessage;
	}
	
	public CommonException(String message, Throwable cause, LocalizableMessage userFriendlyMessage) {
		super(message, cause);
		this.userFriendlyMessage = userFriendlyMessage;
	}
	
	/**
	 * Returns a human-readable message that describes the type or class of errors
	 * that the exception represents. E.g. "Communication error", "Policy violation", etc.
	 * 
	 * TOTO: switch return value to a localized message
	 * 
	 * @return
	 */
	public abstract String getErrorTypeMessage();

	/**
	 * User-friendly (localizable) message that describes this error.
	 * The message is intended to be understood by user or system administrators.
	 * It should NOT contain any developer language (even if this is internal error).
	 */
	public LocalizableMessage getUserFriendlyMessage() {
		return userFriendlyMessage;
	}

	public void setUserFriendlyMessage(LocalizableMessage userFriendlyMessage) {
		this.userFriendlyMessage = userFriendlyMessage;
	}

	@Override
	public String toString() {
		if (userFriendlyMessage == null) {
			return super.toString();
		} else {
			return super.toString() + " [" + userFriendlyMessage.shortDump() + "]";
		}
	}
	
	
	
}
