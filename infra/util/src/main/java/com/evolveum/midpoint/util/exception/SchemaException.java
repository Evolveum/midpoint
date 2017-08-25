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

import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.LocalizableMessage;

/**
 * Error regarding schema.
 * 
 * E.g. Object class violation, missing object class, inconsistent schema, etc. 
 * 
 * @author Radovan Semancik
 *
 */
public class SchemaException extends CommonException {
	private static final long serialVersionUID = -6016220825724355014L;
	
	private QName propertyName;

	public SchemaException() {
		super();
	}

	public SchemaException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public SchemaException(LocalizableMessage userFriendlyMessage, Throwable cause) {
		super(userFriendlyMessage, cause);
	}

	public SchemaException(String message, Throwable cause, QName propertyName) {
		super(message, cause);
		this.propertyName = propertyName;
	}
	
	public SchemaException(String message) {
		super(message);
	}
	
	public SchemaException(LocalizableMessage userFriendlyMessage) {
		super(userFriendlyMessage);
	}

	public SchemaException(String message, QName propertyName) {
		super(message);
		this.propertyName = propertyName;
	}
		
	@Override
	public String getErrorTypeMessage() {
		return "Schema problem";
	}
	
	public QName getPropertyName() {
		return propertyName;
	}

}
