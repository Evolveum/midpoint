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

import java.util.Objects;

/**
 * Superclass for all common midPoint exceptions.
 *
 *
 * @author Radovan Semancik
 */
public abstract class CommonException extends Exception {

	/**
	 * User-friendly localizable detail message.
	 */
	protected LocalizableMessage userFriendlyMessage;

	/**
	 * User-friendly message in system locale. This value should correspond to userFriendlyMessage translated into system locale.
	 */
	protected String localizedUserFriendlyMessage;

	/**
	 * Technical version of the message. Normally it is the value of userFriendlyMessage translated to English.
	 * However in some cases technicalMessage might be a bit more technical than userFriendlyMessage/localizedUserFriendlyMessage.
	 *
	 * Note that we don't use super.detailMessage for this purpose mainly because it's not settable. We need to set this value
	 * when the message is created from the user-friendly LocalizableMessage already present in the exception.
	 * (see LocalizationService.translate(CommonException e)).
	 *
	 * So, super.detailMessage should not be relied on in any way. It is basically the same as technicalMessage, differing only
	 * in rare cases like if a CommonException is initialized from LocalizableMessage (userFriendlyMessage) but is not
	 * translated by the LocalizationService afterwards. In this situation super.detailMessage is initialized
	 * from the (potentially non-null) fallback message, while the technicalMessage is null.
	 */
	private String technicalMessage;

	public CommonException() {
	}

	public CommonException(String technicalMessage) {
		super(technicalMessage);
		this.technicalMessage = technicalMessage;
	}

	public CommonException(LocalizableMessage userFriendlyMessage) {
		super(userFriendlyMessage.getFallbackMessage());
		this.userFriendlyMessage = userFriendlyMessage;
	}

	public CommonException(Throwable cause) {
		super(cause);
	}

	public CommonException(String technicalMessage, Throwable cause) {
		super(technicalMessage, cause);
		this.technicalMessage = technicalMessage;
	}

	public CommonException(LocalizableMessage userFriendlyMessage, Throwable cause) {
		super(userFriendlyMessage.getFallbackMessage(), cause);
		this.userFriendlyMessage = userFriendlyMessage;
	}

	public CommonException(String technicalMessage, Throwable cause, LocalizableMessage userFriendlyMessage) {
		super(technicalMessage, cause);
		this.userFriendlyMessage = userFriendlyMessage;
		this.technicalMessage = technicalMessage;
	}

	/**
	 * Returns a human-readable message that describes the type or class of errors
	 * that the exception represents. E.g. "Communication error", "Policy violation", etc.
	 *
	 * TODO: switch return value to a localized message
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

	@Override
	public String getMessage() {
		return technicalMessage != null ? technicalMessage : super.getMessage();
	}

	@Override
	public String getLocalizedMessage() {
		return localizedUserFriendlyMessage != null ? localizedUserFriendlyMessage : getMessage();
	}

	// should return null if "real" technical message is not set -- this makes it different from getMessage()
	public String getTechnicalMessage() {
		return technicalMessage;
	}

	public void setTechnicalMessage(String technicalMessage) {
		this.technicalMessage = technicalMessage;
	}

	// should return null if "real" localized user friendly message is not set -- this makes it different from getLocalizedMessage()
	public String getLocalizedUserFriendlyMessage() {
		return getLocalizedMessage();
	}

	public void setLocalizedUserFriendlyMessage(String localizedUserFriendlyMessage) {
		this.localizedUserFriendlyMessage = localizedUserFriendlyMessage;
	}

	@Override
	public String toString() {
		if (userFriendlyMessage == null) {
			return super.toString();
		} else {
			String technicalMessage = getMessage();
			String localizedUserFriendlyMessage = getLocalizedMessage();        // this one is used by super.toString
			String technicalMessagePart;
			if (technicalMessage != null && !Objects.equals(technicalMessage, localizedUserFriendlyMessage)) {
				technicalMessagePart = " [" + technicalMessage + "]";
			} else {
				technicalMessagePart = "";
			}
			// TODO consider if we really want to display short dump of userFriendlyMessage even if localized and/or english message is present
			return super.toString() + technicalMessagePart + " [" + userFriendlyMessage.shortDump() + "]";
		}
	}
}
