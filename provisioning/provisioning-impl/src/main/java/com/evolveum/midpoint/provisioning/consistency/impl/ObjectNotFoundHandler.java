package com.evolveum.midpoint.provisioning.consistency.impl;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.Utils;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.provisioning.api.ChangeNotificationDispatcher;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.provisioning.consistency.api.ErrorHandler;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.util.ShadowCacheUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.constants.SchemaConstants;

import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.FailedOperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.prism.xml.ns._public.types_2.ObjectDeltaType;

@Component
public class ObjectNotFoundHandler extends ErrorHandler {

	@Autowired
	@Qualifier("cacheRepositoryService")
	private RepositoryService cacheRepositoryService;
	@Autowired
	private ChangeNotificationDispatcher changeNotificationDispatcher;
	@Autowired(required = true)
	private ProvisioningService provisioningService;

	/**
	 * Get the value of repositoryService.
	 * 
	 * @return the value of repositoryService
	 */
	public RepositoryService getCacheRepositoryService() {
		return cacheRepositoryService;
	}

	/**
	 * Set the value of repositoryService
	 * 
	 * Expected to be injected.
	 * 
	 * @param repositoryService
	 *            new value of repositoryService
	 */
	public void setCacheRepositoryService(RepositoryService repositoryService) {
		this.cacheRepositoryService = repositoryService;
	}

	@Override
	public void handleError(ResourceObjectShadowType shadow, Exception ex) throws SchemaException,
			GenericFrameworkException, CommunicationException, ObjectNotFoundException,
			ObjectAlreadyExistsException, ConfigurationException, SecurityViolationException {

		if (shadow.getFailedOperationType() == FailedOperationTypeType.DELETE) {
//
			OperationResult parentResult = OperationResult.createOperationResult(shadow.getResult());
//			OperationResult handleErrorResult = parentResult.createSubresult(ObjectNotFoundHandler.class
//					.getName() + ".handleError[DELETE]");
//			ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
//
			if (shadow instanceof AccountShadowType) {
//				ObjectDelta<AccountShadowType> delta = new ObjectDelta<AccountShadowType>(
//						AccountShadowType.class, ChangeType.DELETE);
//
//				change.setObjectDelta(delta);
//				change.getObjectDelta().setOid(shadow.getOid());
//				change.setResource(shadow.getResource());
//
//				cacheRepositoryService.getObject(ResourceObjectShadowType.class, shadow.getOid(),
//						Utils.getResolveResourceList(), parentResult);
//
//				AccountShadowType account = (AccountShadowType) shadow;
//				change.setOldShadow(account);
//				account.setActivation(ShadowCacheUtil.determineActivation(shadow.getResource(), account, null));
//				change.setCurrentShadow(account);
//			}
//
//			change.setSourceChannel(QNameUtil.qNameToUri(SchemaConstants.CHANGE_CHANNEL_SYNC));
//
//			changeNotificationDispatcher.notifyChange(change, null, handleErrorResult);

//			if (handleErrorResult.isSuccess()) {
//				ResourceObjectShadowType foundShadow = null;
//				try {
//					foundShadow = provisioningService.getObject(ResourceObjectShadowType.class,
//							shadow.getOid(), null, parentResult);
//				} catch (ObjectNotFoundException e) {
					cacheRepositoryService.deleteObject(AccountShadowType.class, shadow.getOid(),
							parentResult);
//				}
//				if (foundShadow != null) {
//					provisioningService.deleteObject(ResourceObjectShadowType.class, shadow.getOid(), null,
//							parentResult);
//				}

//			} else {
//				ObjectModificationType objectChange = ObjectTypeUtil.createModificationReplaceProperty(
//						shadow.getOid(), SchemaConstants.C_RESULT,
//						handleErrorResult.createOperationResultType());
//				PropertyModificationType operationType = ObjectTypeUtil.createPropertyModificationType(
//						PropertyModificationTypeType.replace, null, SchemaConstants.C_FAILED_OPERATION_TYPE,
//						FailedOperationTypeType.DELETE);
//				objectChange.getPropertyModification().add(operationType);
//				cacheRepositoryService.modifyObject(ResourceObjectShadowType.class, objectChange,
//						parentResult);
			}
		}

		 if (shadow.getFailedOperationType() == FailedOperationTypeType.MODIFY) {
			 OperationResult parentResult = OperationResult.createOperationResult(shadow.getResult());
				OperationResult handleErrorResult = parentResult.createSubresult(ObjectNotFoundHandler.class
						.getName() + ".handleError[MODIFY]");
				ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();

				if (shadow instanceof AccountShadowType) {
					
					change.setObjectDelta(null);
//					change.getObjectDelta().setOid(shadow.getOid());
					change.setResource(shadow.getResource().asPrismObject());

//					AccountShadowType oldShadow = cacheRepositoryService.getObject(AccountShadowType.class, shadow.getOid(),
//							Utils.getResolveResourceList(), parentResult);

					AccountShadowType account = (AccountShadowType) shadow;
//					change.setOldShadow(oldShadow);
					account.setActivation(ShadowCacheUtil.completeActivation(account, account.getResource(), parentResult));
					change.setCurrentShadow(account.asPrismObject());
					
				}

				change.setSourceChannel(QNameUtil.qNameToUri(SchemaConstants.CHANGE_CHANNEL_SYNC));

				changeNotificationDispatcher.notifyChange(change, null, handleErrorResult);
				
				ObjectDeltaType shadowModifications  = shadow.getObjectChange();
				Collection<? extends ItemDelta> modifications = DeltaConvertor.toModifications(shadowModifications.getModification(),
						shadow.asPrismObject().getDefinition());
				
				try{
					provisioningService.modifyObject(AccountShadowType.class, shadow.getOid(), modifications, null, parentResult);
				} catch (ObjectNotFoundException e){
					cacheRepositoryService.deleteObject(AccountShadowType.class, shadow.getOid(), parentResult);
				}
				
				
				

		 }
		throw new ObjectNotFoundException("Can't find object " + ObjectTypeUtil.toShortString(shadow));

	}
}
