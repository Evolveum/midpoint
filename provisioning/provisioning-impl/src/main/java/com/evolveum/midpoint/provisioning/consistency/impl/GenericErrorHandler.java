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
	public <T extends ResourceObjectShadowType> T handleError(T shadow, FailedOperation op, Exception ex, OperationResult parentResult) throws SchemaException, GenericFrameworkException, CommunicationException, ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException, SecurityViolationException{
		
//		OperationResult result = OperationResult.createOperationResult(shadow.getResult());
		String operation = (shadow.getFailedOperationType() == null ? "null" : shadow.getFailedOperationType().name());
		
		OperationResult result = parentResult.createSubresult("Compensating operation: " + operation + " while executing operation: "+ op.name());
		result.addParam("shadow", shadow);
		result.addParam("currentOperation", op);
		result.addParam("reconciled", true);
		
		switch (op) {
		case GET:
			if (AvailabilityStatusType.DOWN == shadow.getResource().getLastAvailabilityStatus()){
				parentResult.computeStatus("Unable to get account from the resource. Probably it has not been created yet because of previous unavailability of the resource.");
				parentResult.muteError();
				shadow.setFetchResult(parentResult.createOperationResultType());
				return shadow;
			}
			
			if (shadow.getFailedOperationType() != null){
				
				if (FailedOperationTypeType.ADD == shadow.getFailedOperationType()){
					
					String oid = provisioningService.addObject(shadow.asPrismObject(), null, result);
					result.computeStatus();
					if (result.isSuccess()){
						 shadow = (T) shadowCache.getShadow(shadow.getClass(), oid, null, result);
					}
				} else if (FailedOperationTypeType.MODIFY == shadow.getFailedOperationType()){
					if (shadow.getObjectChange() != null) {
						ObjectDeltaType deltaType = shadow.getObjectChange();
						Collection<? extends ItemDelta> modifications = DeltaConvertor.toModifications(
								deltaType.getModification(), shadow.asPrismObject().getDefinition());
						provisioningService.modifyObject(AccountShadowType.class, shadow.getOid(), modifications, null, result);
					} 
					shadow = (T) shadowCache.getShadow(shadow.getClass(), shadow.getOid(), null, result);
				} else if (FailedOperationTypeType.DELETE == shadow.getFailedOperationType()){
					
					provisioningService.deleteObject(AccountShadowType.class, shadow.getOid(), null, result);
				}
			}
			return shadow;
		case MODIFY:
			if (shadow.getFailedOperationType() != null) {
				if (FailedOperationTypeType.ADD == shadow.getFailedOperationType()) {
					// get the modifications from the shadow before the account
					// is created, because after successful creation of account,
					// the modification will be lost
					Collection<? extends ItemDelta> modifications = null;
					if (shadow.getObjectChange() != null) {
						ObjectDeltaType deltaType = shadow.getObjectChange();

						modifications = DeltaConvertor.toModifications(deltaType.getModification(), shadow
								.asPrismObject().getDefinition());
					}

					provisioningService.addObject(shadow.asPrismObject(), null, result);
					// if (oid != null){
					// shadow = shadowCache.getShadow(AccountShadowType.class,
					// oid, null, parentResult);
					// }
					result.computeStatus();

					if (result.isSuccess()) {
						// we assume, that the account was successfully
						// created,
						// so apply also modifications to the resource
						// account
						provisioningService.modifyObject(AccountShadowType.class, shadow.getOid(), modifications, null,
								result);
						result.computeStatus();
					} else {
						// account wasn't created, probably resource is
						// still
						// down, or there is other reason.just save the
						// pending
						// modifications to the shadow in the
						// repository..next
						// time by processing this shadow, we can try again
						// TODO: probably there is a need to union current
						// changes with previous
						cacheRepositoryService.modifyObject(AccountShadowType.class, shadow.getOid(), modifications,
								result);
						result.recordStatus(OperationResultStatus.HANDLED_ERROR, "Modifications not applied to the account, because resource is unreachable. They are stored to the shadow and will be applied when the resource goes online.");
					}

				}
			}
			return shadow;
		case DELETE:
			cacheRepositoryService.deleteObject(shadow.getClass(), shadow.getOid(), result);
			result.recordStatus(OperationResultStatus.HANDLED_ERROR, "Account has been not created on the resource yet. Shadow deleted from the repository");
		default:
			throw new GenericFrameworkException("Generic error in the connector. Can't process shadow "
					+ ObjectTypeUtil.toShortString(shadow) + ". ", ex);

		}
		
	}

}
