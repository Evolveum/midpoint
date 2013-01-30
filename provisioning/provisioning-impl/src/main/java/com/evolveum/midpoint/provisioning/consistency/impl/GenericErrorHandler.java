package com.evolveum.midpoint.provisioning.consistency.impl;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.consistency.api.ErrorHandler;
import com.evolveum.midpoint.provisioning.impl.ShadowCacheReconciler;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.ObjectOperationOption;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AvailabilityStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.FailedOperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceObjectShadowType;
import com.evolveum.prism.xml.ns._public.types_2.ObjectDeltaType;

@Component
public class GenericErrorHandler extends ErrorHandler{

	
	@Autowired(required = true)
	private ProvisioningService provisioningService;

//	@Autowired(required = true)
//	private ShadowCacheReconciler shadowCacheFinisher;
	
	@Autowired
	@Qualifier("cacheRepositoryService")
	private RepositoryService cacheRepositoryService;
	
//	@Autowired
//	private OperationFinisher operationFinisher;
	
	
	@Override
	public <T extends ResourceObjectShadowType> T handleError(T shadow, FailedOperation op, Exception ex, boolean compensate, 
			Task task, OperationResult parentResult) throws SchemaException, GenericFrameworkException, CommunicationException,
			ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException, SecurityViolationException {
		
//		OperationResult result = OperationResult.createOperationResult(shadow.getResult());
		String operation = (shadow.getFailedOperationType() == null ? "null" : shadow.getFailedOperationType().name());
		
		OperationResult result = parentResult.createSubresult("Compensating operation: " + operation + " while executing operation: "+ op.name());
		result.addParam("shadow", shadow);
		result.addParam("currentOperation", op);
		result.addParam("reconciled", true);
		
//		Task task = taskManager.createTaskInstance();
		
		switch (op) {
		case GET:
			if ((shadow.isDead() != null && shadow.isDead()) || shadow.getResource().getOperationalState() != null && AvailabilityStatusType.DOWN == shadow.getResource().getOperationalState().getLastAvailabilityStatus()){
//				parentResult.computeStatus("Unable to get account from the resource. Probably it has not been created yet because of previous unavailability of the resource.");
//				if (parentResult.isError()) {
//					parentResult.setStatus(OperationResultStatus.PARTIAL_ERROR);
//				}
				result.recordStatus(OperationResultStatus.PARTIAL_ERROR, "Unable to get account from the resource. Probably it has not been created yet because of previous unavailability of the resource.");
				parentResult.computeStatus();
				shadow.setFetchResult(parentResult.createOperationResultType());
				return shadow;
			}
			
			if (shadow.getFailedOperationType() == null){
				throw new GenericFrameworkException("Generic error in the connector. Can't process shadow "
						+ ObjectTypeUtil.toShortString(shadow) + ". ", ex);
			}
//				if (FailedOperationTypeType.ADD == shadow.getFailedOperationType()){
					provisioningService.finishOperation(shadow.asPrismObject(), task, result);
//					String oid = provisioningService.addObject(shadow.asPrismObject(), null, result);
					result.computeStatus();
					if (result.isSuccess()){
						 PrismObject prismShadow = provisioningService.getObject(shadow.getClass(), shadow.getOid(), null, result);
						 shadow = (T) prismShadow.asObjectable();
					}
//				} else if (FailedOperationTypeType.MODIFY == shadow.getFailedOperationType()){
//					if (shadow.getObjectChange() != null) {
//						ObjectDeltaType deltaType = shadow.getObjectChange();
//						Collection<? extends ItemDelta> modifications = DeltaConvertor.toModifications(
//								deltaType.getModification(), shadow.asPrismObject().getDefinition());
//						provisioningService.modifyObject(AccountShadowType.class, shadow.getOid(), modifications, null, result);
//					} 
//					shadow = (T) shadowCache.getShadow(shadow.getClass(), shadow.getOid(), null, result);
//				} else if (FailedOperationTypeType.DELETE == shadow.getFailedOperationType()){
//					
//					provisioningService.deleteObject(AccountShadowType.class, shadow.getOid(), ObjectOperationOption.FORCE, null, result);
//				}
				return shadow;
			
//			throw new GenericFrameworkException("Generic error in the connector. Can't process shadow "
//					+ ObjectTypeUtil.toShortString(shadow) + ". ", ex);
		case MODIFY:
			if (shadow.getFailedOperationType() == null) {
				throw new GenericFrameworkException("Generic error in the connector. Can't process shadow "
						+ ObjectTypeUtil.toShortString(shadow) + ". ", ex);
			}
				// get the modifications from the shadow before the account
				// is created, because after successful creation of account,
				// the modification will be lost
				Collection<? extends ItemDelta> modifications = null;
				if (shadow.getObjectChange() != null) {
					ObjectDeltaType deltaType = shadow.getObjectChange();

					modifications = DeltaConvertor.toModifications(deltaType.getModification(), shadow
							.asPrismObject().getDefinition());
				}
					PropertyDelta.applyTo(modifications, shadow.asPrismObject());
//				if (FailedOperationTypeType.ADD == shadow.getFailedOperationType()) {
//				
					provisioningService.finishOperation(shadow.asPrismObject(), task, result);						
//					provisioningService.addObject(shadow.asPrismObject(), null, result);
					// if (oid != null){
					// shadow = shadowCache.getShadow(AccountShadowType.class,
					// oid, null, parentResult);
					// }
					result.computeStatus();

					if (!result.isSuccess()) {
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
						result.recordHandledError("Modifications not applied to the account, because resource is unreachable. They are stored to the shadow and will be applied when the resource goes online.");
					}

				
				return shadow;
			
		case DELETE:
			cacheRepositoryService.deleteObject(shadow.getClass(), shadow.getOid(), result);
			result.recordStatus(OperationResultStatus.HANDLED_ERROR, "Account has been not created on the resource yet. Shadow deleted from the repository");
			return null;
		default:
			result.recordFatalError("Can't process "
					+ ObjectTypeUtil.toShortString(shadow) + ": "+ex.getMessage(), ex);
			if (shadow.getOid() == null){
				throw new GenericFrameworkException("Can't process "
						+ ObjectTypeUtil.toShortString(shadow) + ": "+ex.getMessage(), ex);
			}
			
			Collection<ItemDelta> modification = createAttemptModification(shadow, null);
			cacheRepositoryService.modifyObject(shadow.asPrismObject().getCompileTimeClass(), shadow.getOid(), modification, parentResult);
			
			throw new GenericFrameworkException("Can't process "
					+ ObjectTypeUtil.toShortString(shadow) + ". ", ex);
		}		
	}

}
