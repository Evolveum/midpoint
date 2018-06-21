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
 * Object already exists.
 *
 * @author Radovan Semancik
 *
 */
public class ObjectAlreadyExistsException extends CommonException {
	private static final long serialVersionUID = -2851816602797097915L;

	public ObjectAlreadyExistsException() {
	}

	public ObjectAlreadyExistsException(String message) {
		super(message);
	}

	public ObjectAlreadyExistsException(LocalizableMessage userFriendlyMessage) {
		super(userFriendlyMessage);
	}

	public ObjectAlreadyExistsException(Throwable cause) {
		super(cause);
	}

	public ObjectAlreadyExistsException(String message, Throwable cause) {
		super(message, cause);
	}

	public ObjectAlreadyExistsException(LocalizableMessage userFriendlyMessage, Throwable cause) {
		super(userFriendlyMessage, cause);
	}

	@Override
	public String getErrorTypeMessage() {
		return "Object already exists";
	}

}
