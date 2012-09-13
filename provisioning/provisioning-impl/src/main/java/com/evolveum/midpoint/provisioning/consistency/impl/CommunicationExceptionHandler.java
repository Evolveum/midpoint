package com.evolveum.midpoint.provisioning.consistency.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
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
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceObjectShadowUtil;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AvailabilityStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.FailedOperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceType;
import com.evolveum.prism.xml.ns._public.types_2.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_2.ModificationTypeType;
import com.evolveum.prism.xml.ns._public.types_2.ObjectDeltaType;

@Component
public class CommunicationExceptionHandler extends ErrorHandler {

	@Autowired
	@Qualifier("cacheRepositoryService")
	private RepositoryService cacheRepositoryService;

	public CommunicationExceptionHandler() {
		cacheRepositoryService = null;
	}

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
	public <T extends ResourceObjectShadowType> T handleError(T shadow, FailedOperation op, Exception ex) throws SchemaException,
			GenericFrameworkException, CommunicationException, ObjectNotFoundException,
			ObjectAlreadyExistsException, ConfigurationException {

//		OperationResult operationResult = new OperationResult(CommunicationExceptionHandler.class.getName()
//				+ ".handleError");
		Validate.notNull(shadow, "Shadow must not be null.");
		OperationResult operationResult = OperationResult.createOperationResult(shadow.getResult());
		operationResult.muteError();
//		shadowResult.addSubresult(operationResult);
//		Validate.notNull(shadow.getFailedOperationType(), "Failed operation type must not be null.");
		// if the failed operation was adding, then what we need is to store the
		// whole object to the repository to try it add again later
		modifyResourceAvailabilityStatus(shadow.getResource(), AvailabilityStatusType.DOWN, operationResult);
		switch (op) {
		case ADD:
			if (shadow.getFailedOperationType() == null) {
				if (shadow.getName() == null) {
					shadow.setName(ShadowCacheUtil.determineShadowName(shadow));
				}
				shadow.setAttemptNumber(getAttemptNumber(shadow));
				shadow.setFailedOperationType(FailedOperationTypeType.ADD);
				String oid = cacheRepositoryService.addObject(shadow.asPrismObject(), operationResult);
				shadow.setOid(oid);
//				return shadow;
			} else {
				if (FailedOperationTypeType.ADD == shadow.getFailedOperationType()) {

					PropertyDelta attemptDelta = PropertyDelta.createReplaceDelta(shadow.asPrismObject()
							.getDefinition(), ResourceObjectShadowType.F_ATTEMPT_NUMBER, getAttemptNumber(shadow));
					Collection<PropertyDelta> delta = new ArrayList<PropertyDelta>();
					delta.add(attemptDelta);
					cacheRepositoryService.modifyObject(AccountShadowType.class, shadow.getOid(), delta,
							operationResult);
//					return shadow;
				}
				
			}
			operationResult.muteError();
			return shadow;
		case MODIFY:
			if (shadow.getFailedOperationType() == null) {

				shadow.setFailedOperationType(FailedOperationTypeType.MODIFY);
				List<PropertyDelta> modifications = createShadowModification(shadow);

				getCacheRepositoryService().modifyObject(AccountShadowType.class, shadow.getOid(), modifications,
						operationResult);
//				return shadow;
			} else {
				if (FailedOperationTypeType.ADD == shadow.getFailedOperationType()) {
					if (shadow.getObjectChange() != null && shadow.getOid() != null) {
						Collection<? extends ItemDelta> deltas = DeltaConvertor.toModifications(shadow
								.getObjectChange().getModification(), shadow.asPrismObject().getDefinition());

						cacheRepositoryService.modifyObject(AccountShadowType.class, shadow.getOid(), deltas,
								operationResult);
//						return shadow;
					}
				}
			}
			operationResult.muteError();
			return shadow;
		case DELETE:
			shadow.setFailedOperationType(FailedOperationTypeType.DELETE);
			List<PropertyDelta> modifications = createShadowModification(shadow);

			getCacheRepositoryService().modifyObject(AccountShadowType.class, shadow.getOid(), modifications,
					operationResult);
			return shadow;
		case GET:
			//nothing to do, just return the shadow from the repo and set fetch result..
			shadow.setFetchResult(shadow.getResult());
			return shadow;
		default:
			throw new CommunicationException(ex);
		}
		
//		if (FailedOperationTypeType.ADD == shadow.getFailedOperationType()) {
//
//			if (shadow.getName() == null) {
//				shadow.setName(ShadowCacheUtil.determineShadowName(shadow));
//			}
//			shadow.setAttemptNumber(0);
//			
//			// String oid =
//			// getCacheRepositoryService().addObject(shadow.asPrismObject(),
//			// operationResult);
//			// shadow.setOid(oid);
//			if (shadow.getObjectChange() != null && shadow.getOid()!= null) {
//				Collection<? extends ItemDelta> deltas = DeltaConvertor.toModifications(shadow
//						.getObjectChange().getModification(), shadow.asPrismObject().getDefinition());
//				
//				cacheRepositoryService.modifyObject(AccountShadowType.class, shadow.getOid(), deltas, operationResult);
//			}
//
//		} else {
//			// if the failed operation was modify, we to store the changes, that
//			// should be applied to the account (also operation result and
//			// operation type for later processing)
//
//			// storing operation result to the account which failed to be
//			// modified
//			if (FailedOperationTypeType.MODIFY == shadow.getFailedOperationType()) {
//
//				
//
//				List<PropertyDelta> modifications = new ArrayList<PropertyDelta>();
//
//				PropertyDelta propertyDelta = PropertyDelta.createReplaceDelta(shadow.asPrismObject()
//						.getDefinition(), ResourceObjectShadowType.F_RESULT, shadow.getResult());
//				modifications.add(propertyDelta);
//
//				propertyDelta = PropertyDelta.createReplaceDelta(shadow.asPrismObject().getDefinition(),
//						ResourceObjectShadowType.F_FAILED_OPERATION_TYPE, shadow.getFailedOperationType());
//				modifications.add(propertyDelta);
//
//				propertyDelta = PropertyDelta.createReplaceDelta(shadow.asPrismObject().getDefinition(),
//						ResourceObjectShadowType.F_OBJECT_CHANGE, shadow.getObjectChange());
//				modifications.add(propertyDelta);
//
//				propertyDelta = PropertyDelta.createReplaceDelta(shadow.asPrismObject().getDefinition(),
//						ResourceObjectShadowType.F_ATTEMPT_NUMBER, 0);
//				modifications.add(propertyDelta);
//
//				getCacheRepositoryService().modifyObject(AccountShadowType.class, shadow.getOid(),
//						modifications, operationResult);
//			} else if (FailedOperationTypeType.DELETE == shadow.getFailedOperationType()) {
//				// this is the case when the deletion of account failed..in this
//				// case, we need to sign the account with the tombstone and
//				// delete it later
//
//				List<PropertyDelta> modifications = new ArrayList<PropertyDelta>();
//
//				PropertyDelta propertyDelta = PropertyDelta.createReplaceDelta(shadow.asPrismObject()
//						.getDefinition(), ResourceObjectShadowType.F_RESULT, shadow.getResult());
//				modifications.add(propertyDelta);
//
//				propertyDelta = PropertyDelta.createReplaceDelta(shadow.asPrismObject().getDefinition(),
//						ResourceObjectShadowType.F_FAILED_OPERATION_TYPE, FailedOperationTypeType.DELETE);
//				modifications.add(propertyDelta);
//
//				propertyDelta = PropertyDelta.createReplaceDelta(shadow.asPrismObject().getDefinition(),
//						ResourceObjectShadowType.F_ATTEMPT_NUMBER, 0);
//				modifications.add(propertyDelta);
//
//				getCacheRepositoryService().modifyObject(AccountShadowType.class, shadow.getOid(),
//						modifications, operationResult);
//
//			}
//		}
//
	}

	private void modifyResourceAvailabilityStatus(ResourceType resource, AvailabilityStatusType status, OperationResult result) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
		
		if (resource.getLastAvailabilityStatus() == null || resource.getLastAvailabilityStatus() != status) {
			List<PropertyDelta> modifications = new ArrayList<PropertyDelta>();
			PropertyDelta statusDelta = PropertyDelta.createModificationReplaceProperty(
					ResourceType.F_LAST_AVAILABILITY_STATUS, resource.asPrismObject().getDefinition(), status);
			modifications.add(statusDelta);
			resource.setLastAvailabilityStatus(status);
			cacheRepositoryService.modifyObject(ResourceType.class, resource.getOid(), modifications, result);
		}
	}
	
	private List<PropertyDelta> createShadowModification(ResourceObjectShadowType shadow) {
		List<PropertyDelta> modifications = new ArrayList<PropertyDelta>();

		PropertyDelta propertyDelta = PropertyDelta.createReplaceDelta(shadow.asPrismObject()
				.getDefinition(), ResourceObjectShadowType.F_RESULT, shadow.getResult());
		modifications.add(propertyDelta);

		propertyDelta = PropertyDelta.createReplaceDelta(shadow.asPrismObject().getDefinition(),
				ResourceObjectShadowType.F_FAILED_OPERATION_TYPE, shadow.getFailedOperationType());
		modifications.add(propertyDelta);
		if (shadow.getObjectChange() != null) {
			propertyDelta = PropertyDelta.createReplaceDelta(shadow.asPrismObject().getDefinition(),
					ResourceObjectShadowType.F_OBJECT_CHANGE, shadow.getObjectChange());
			modifications.add(propertyDelta);
		}
	
		propertyDelta = PropertyDelta.createReplaceDelta(shadow.asPrismObject().getDefinition(),
				ResourceObjectShadowType.F_ATTEMPT_NUMBER, getAttemptNumber(shadow));
		modifications.add(propertyDelta);

		return modifications;
	}

	private Integer getAttemptNumber(ResourceObjectShadowType shadow) {
		Integer attemptNumber = (shadow.getAttemptNumber() == null ? 0 : shadow.getAttemptNumber()+1);
		return attemptNumber;
	}
}
