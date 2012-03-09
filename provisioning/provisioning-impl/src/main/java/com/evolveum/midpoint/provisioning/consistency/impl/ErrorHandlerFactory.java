package com.evolveum.midpoint.provisioning.consistency.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.consistency.api.ErrorHandler;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;

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
	
	
//	public CommunicationExceptionHandler getCommunicationExceptionHandler() {
//		return communicationExceptionHandler;
//	}
//	public void setCommunicationExceptionHandler(CommunicationExceptionHandler communicationExceptionHandler) {
//		this.communicationExceptionHandler = communicationExceptionHandler;
//	}
	
	
	
	public ErrorHandler createErrorHandler(Exception ex){
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
		
		return null;
	}
	
	

}
