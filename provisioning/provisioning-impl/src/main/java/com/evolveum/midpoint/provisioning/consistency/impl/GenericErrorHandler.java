package com.evolveum.midpoint.provisioning.consistency.impl;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.consistency.api.ErrorHandler;
import com.evolveum.midpoint.provisioning.impl.ShadowCache;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AvailabilityStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.FailedOperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceObjectShadowType;
import com.evolveum.prism.xml.ns._public.types_2.ObjectDeltaType;

@Component
public class GenericErrorHandler extends ErrorHandler{

	
	@Autowired(required = true)
	private ProvisioningService provisioningService;

	@Autowired(required = true)
	private ShadowCache shadowCache;
	
	@Autowired
	@Qualifier("cacheRepositoryService")
	private RepositoryService cacheRepositoryService;
	
	
	@Override
	public <T extends ResourceObjectShadowType> T handleError(T shadow, FailedOperation op, Exception ex) throws SchemaException, GenericFrameworkException, CommunicationException, ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException, SecurityViolationException{
		
		OperationResult result = OperationResult.createOperationResult(shadow.getResult());
		
		if (AvailabilityStatusType.DOWN == shadow.getResource().getLastAvailabilityStatus()){
			result.muteError();
			shadow.setFetchResult(result.createOperationResultType());
			return shadow;
		}
		
		switch (op) {
		case GET:
			
			if (shadow.getFailedOperationType() != null){
				if (FailedOperationTypeType.ADD == shadow.getFailedOperationType()){
					OperationResult subResult = new OperationResult(GenericErrorHandler.class.getName() +".re-add");
					result.addSubresult(subResult);
					subResult.addParam("shadow", shadow);
					subResult.addParam("currentOperation", op);
					subResult.addParam("reconciled", true);
					String oid = provisioningService.addObject(shadow.asPrismObject(), null, subResult);
					if (subResult.isSuccess()){
						 shadow = (T) shadowCache.getShadow(shadow.getClass(), oid, null, subResult);
					}
					return shadow;
				} else if (FailedOperationTypeType.MODIFY == shadow.getFailedOperationType()){
					OperationResult subResult = new OperationResult(GenericErrorHandler.class.getName() +".re-modify");
					result.addSubresult(subResult);
					subResult.addParam("shadow", shadow);
					subResult.addParam("currentOperation", op);
					subResult.addParam("reconciled", true);
					ObjectDeltaType deltaType = shadow.getObjectChange();
					Collection<? extends ItemDelta> modifications = DeltaConvertor.toModifications(deltaType.getModification(), shadow.asPrismObject().getDefinition());
					provisioningService.modifyObject(AccountShadowType.class, shadow.getOid(), modifications, null, subResult);
				} else if (FailedOperationTypeType.DELETE == shadow.getFailedOperationType()){
					OperationResult subResult = new OperationResult(GenericErrorHandler.class.getName() +".re-delete");
					result.addSubresult(subResult);
					subResult.addParam("shadow", shadow);
					subResult.addParam("currentOperation", op);
					subResult.addParam("reconciled", true);
					provisioningService.deleteObject(AccountShadowType.class, shadow.getOid(), null, subResult);
				}
			}
			return shadow;
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
					// }
					parentResult.computeStatus();
					ObjectDeltaType deltaType = shadow.getObjectChange();
					Collection<? extends ItemDelta> modifications = DeltaConvertor.toModifications(
							deltaType.getModification(), shadow.asPrismObject().getDefinition());
					if (parentResult.isSuccess()) {
						// we assume, that tha account was successfully created,
						// so apply also modifications to the resource account
						provisioningService.modifyObject(AccountShadowType.class, shadow.getOid(), modifications, null,
								parentResult);
					} else{
						// account wasn't created, probably resource is still
						// down, or there is other reason.just save the pending
						// modifications to the shadow in the repository..next
						// time by processing this shadow, we can try again
						cacheRepositoryService.modifyObject(AccountShadowType.class, shadow.getOid(), modifications, parentResult);
					}
				}
			}
			return shadow;
		default:
			throw new GenericFrameworkException("Generic error in the connector. Can't process shadow "
					+ ObjectTypeUtil.toShortString(shadow) + ". ", ex);

		}
		
	}

}
