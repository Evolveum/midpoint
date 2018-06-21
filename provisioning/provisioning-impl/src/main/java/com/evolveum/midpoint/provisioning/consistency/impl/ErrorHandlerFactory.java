/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.provisioning.consistency.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.consistency.api.ErrorHandler;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;

@Component
public class ErrorHandlerFactory {
	
	@Autowired(required=true)
	CommunicationExceptionHandler communicationExceptionHandler;
	
	@Autowired(required=true)
	SchemaExceptionHandler schemaExceptionHandler;
	
	@Autowired(required=true)
	ObjectNotFoundHandler objectNotFoundHandler;
	
	@Autowired(required=true)
	ObjectAlreadyExistHandler objectAlreadyExistsHandler;
	
	@Autowired(required=true)
	GenericErrorHandler genericErrorHandler;
	
	@Autowired(required=true)
	ConfigurationExceptionHandler configurationExceptionHandler;
	
	@Autowired(required=true)
	SecurityViolationHandler securityViolationHandler;
	
	
//	public CommunicationExceptionHandler getCommunicationExceptionHandler() {
//		return communicationExceptionHandler;
//	}
//	public void setCommunicationExceptionHandler(CommunicationExceptionHandler communicationExceptionHandler) {
//		this.communicationExceptionHandler = communicationExceptionHandler;
//	}
	
	
	
	public ErrorHandler createErrorHandler(Throwable ex) {
		if (ex instanceof CommunicationException){
			return communicationExceptionHandler;
		}
		if (ex instanceof GenericConnectorException){
			return genericErrorHandler;
		}
		if (ex instanceof ObjectAlreadyExistsException){
			return objectAlreadyExistsHandler;
		}
		if (ex instanceof ObjectNotFoundException){
			return objectNotFoundHandler;
		}
		if (ex instanceof SchemaException){
			return schemaExceptionHandler;
		}
		if (ex instanceof ConfigurationException){
			return configurationExceptionHandler;
		}
		if (ex instanceof SecurityViolationException){
			return securityViolationHandler;
		}
		if (ex instanceof RuntimeException) {
			throw (RuntimeException)ex;
		}
		if (ex instanceof Error) {
			throw (Error)ex;
		}
		
		throw new SystemException(ex != null ? ex.getClass().getName() +": "+ ex.getMessage() : "Unexpected error:", ex);
	}
	
	

}
