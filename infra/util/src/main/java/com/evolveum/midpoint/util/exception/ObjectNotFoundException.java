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
 * Object with specified criteria (OID) has not been found in the repository.
 * 
 * @author Radovan Semancik
 * 
 */
public class ObjectNotFoundException extends CommonException {
	private static final long serialVersionUID = -9003686713018111855L;

	private String oid = null;;
	
	public ObjectNotFoundException() {
		super();
	}

	public ObjectNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public ObjectNotFoundException(LocalizableMessage userFriendlyMessage, Throwable cause) {
		super(userFriendlyMessage, cause);
	}

	public ObjectNotFoundException(String message, Throwable cause, String oid) {
		super(message, cause);
		this.oid = oid;
	}
	
	public ObjectNotFoundException(String message) {
		super(message);
	}
	
	public ObjectNotFoundException(LocalizableMessage userFriendlyMessage) {
		super(userFriendlyMessage);
	}

    public ObjectNotFoundException(String message, String oid) {
        super(message);
        this.oid = oid;
    }

    public ObjectNotFoundException(Throwable cause) {
		super(cause);
	}

	public String getOid() {
		return oid;
	}

	@Override
	public String getErrorTypeMessage() {
		return "Object not found";
	}
}
