package com.evolveum.midpoint.provisioning.consistency.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.consistency.api.ErrorHandler;
import com.evolveum.midpoint.provisioning.impl.ShadowCache;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.FailedOperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceObjectShadowType;

@Component
public class GenericErrorHandler extends ErrorHandler{

	
	@Autowired(required = true)
	private ProvisioningService provisioningService;

	@Autowired(required = true)
	private ShadowCache shadowCache;
	
	@Override
	public void handleError(ResourceObjectShadowType shadow, FailedOperation op, Exception ex) throws SchemaException, GenericFrameworkException, CommunicationException, ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException, SecurityViolationException{
		
		switch (op) {
		case GET:
			if (shadow.getFailedOperationType() != null){
				if (FailedOperationTypeType.ADD == shadow.getFailedOperationType()){
					OperationResult parentResult = new OperationResult(GenericErrorHandler.class.getName() +".re-add");
					parentResult.addParam("shadow", shadow);
					parentResult.addParam("currentOperation", op);
					parentResult.addParam("reconciled", true);
					String oid = provisioningService.addObject(shadow.asPrismObject(), null, parentResult);
//					if (oid != null){
//						shadow = shadowCache.getShadow(AccountShadowType.class, oid, null, parentResult);
//					}
				}
			}
			break;
		case MODIFY:
			if (shadow.getFailedOperationType() != null){
				if (FailedOperationTypeType.ADD == shadow.getFailedOperationType()){
					OperationResult parentResult = new OperationResult(GenericErrorHandler.class.getName() +".re-add");
					parentResult.addParam("shadow", shadow);
					parentResult.addParam("currentOperation", op);
					parentResult.addParam("reconciled", true);
					provisioningService.addObject(shadow.asPrismObject(), null, parentResult);
//					if (oid != null){
//						shadow = shadowCache.getShadow(AccountShadowType.class, oid, null, parentResult);
//					}
//					provisioningService.modifyObject(type, oid, modifications, scripts, parentResult)
				}
			}
			break;
		default:
			throw new GenericFrameworkException("Generic error in the connector. Can't process shadow "
					+ ObjectTypeUtil.toShortString(shadow) + ". ", ex);

		}
		
	}

}
