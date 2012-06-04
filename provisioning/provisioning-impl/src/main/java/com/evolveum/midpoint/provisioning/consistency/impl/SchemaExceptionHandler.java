package com.evolveum.midpoint.provisioning.consistency.impl;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.provisioning.consistency.api.ErrorHandler;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceObjectShadowType;

@Component
public class SchemaExceptionHandler extends ErrorHandler{

	@Override
	public void handleError(ResourceObjectShadowType shadow, Exception ex) throws SchemaException, GenericFrameworkException, CommunicationException, ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException{
		
		throw new SchemaException("Schema violation during processing shadow: "+ ObjectTypeUtil.toShortString(shadow)+".", ex);
		
	}

}
