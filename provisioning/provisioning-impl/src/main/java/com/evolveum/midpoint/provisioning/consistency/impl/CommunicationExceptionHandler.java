package com.evolveum.midpoint.provisioning.consistency.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PropertyPath;
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
import com.evolveum.midpoint.xml.ns._public.common.common_2.OperationalStateType;
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
	public <T extends ResourceObjectShadowType> T handleError(T shadow, FailedOperation op, Exception ex,
			OperationResult parentResult) throws SchemaException, GenericFrameworkException, CommunicationException,
			ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException {

		Validate.notNull(shadow, "Shadow must not be null.");
		for (OperationResult subRes : parentResult.getSubresults()) {
			subRes.muteError();
		}
		OperationResult operationResult = parentResult.createSubresult("Compensation for communication problem. Operation: " + op.name());
		operationResult.addParam("shadow", shadow);
		operationResult.addParam("currentOperation", op);
		operationResult.addParam("exception", ex.getMessage());

		// first modify last availability status in the resource, so by others
		// operations, we can know that it is down
		modifyResourceAvailabilityStatus(shadow.getResource(), AvailabilityStatusType.DOWN, operationResult);

		switch (op) {
		case ADD:
			// if it is firt time, just store the whole account to the repo
			if (shadow.getFailedOperationType() == null) {
				if (shadow.getName() == null) {
					shadow.setName(ShadowCacheUtil.determineShadowName(shadow));
				}
				shadow.setAttemptNumber(getAttemptNumber(shadow));
				shadow.setFailedOperationType(FailedOperationTypeType.ADD);
				String oid = cacheRepositoryService.addObject(shadow.asPrismObject(), operationResult);
				shadow.setOid(oid);
			
				// if it is seccond time ,just increade the attempt number
			} else {
				if (FailedOperationTypeType.ADD == shadow.getFailedOperationType()) {

					Collection<? extends ItemDelta> delta = createAttemptModification(shadow, null);
					cacheRepositoryService.modifyObject(AccountShadowType.class, shadow.getOid(), delta,
							operationResult);
			
				}

			}
			// if the shadow was successfully stored in the repo, just mute the
			// error
			operationResult.computeStatus();
			parentResult
					.recordStatus(
							OperationResultStatus.HANDLED_ERROR,
							"Could not create account on the resource, because "
									+ ObjectTypeUtil.toShortString(shadow.getResource())
									+ " is unreachable at the moment. Shadow is stored in the repository and the account will be created when the resource goes online.");
			
			return shadow;
		case MODIFY:
			if (shadow.getFailedOperationType() == null) {

				shadow.setFailedOperationType(FailedOperationTypeType.MODIFY);
				Collection<ItemDelta> modifications = createShadowModification(shadow);

				getCacheRepositoryService().modifyObject(AccountShadowType.class, shadow.getOid(), modifications,
						operationResult);
//				operationResult.recordSuccess();
				// return shadow;
			} else {
				if (FailedOperationTypeType.ADD == shadow.getFailedOperationType()) {
					if (shadow.getObjectChange() != null && shadow.getOid() != null) {
						Collection<? extends ItemDelta> deltas = DeltaConvertor.toModifications(shadow
								.getObjectChange().getModification(), shadow.asPrismObject().getDefinition());

						cacheRepositoryService.modifyObject(AccountShadowType.class, shadow.getOid(), deltas,
								operationResult);
						// return shadow;
//						operationResult.recordSuccess();
					}
				}
			}
			operationResult.computeStatus();
			parentResult
					.recordStatus(
							OperationResultStatus.HANDLED_ERROR,
							"Could not apply modifications to account on the "
									+ ObjectTypeUtil.toShortString(shadow.getResource())
									+ ", becasue resource is unreachable. Modifications will be applied when the resource goes online.");
			return shadow;
		case DELETE:
			shadow.setFailedOperationType(FailedOperationTypeType.DELETE);
			Collection<ItemDelta> modifications = createShadowModification(shadow);

			getCacheRepositoryService().modifyObject(AccountShadowType.class, shadow.getOid(), modifications,
					operationResult);
			parentResult
					.recordStatus(
							OperationResultStatus.HANDLED_ERROR,
							"Could not delete account from the resource "
									+ ObjectTypeUtil.toShortString(shadow.getResource())
									+ ", becasue resource is unreachable. Account will be delete when the resource goes online.");
//			operationResult.recordSuccess();
			operationResult.computeStatus();
			return shadow;
		case GET:
			// nothing to do, just return the shadow from the repo and set fetch
			// result..
			parentResult.recordStatus(OperationResultStatus.HANDLED_ERROR, "Could not get account from the resource "
					+ ObjectTypeUtil.toShortString(shadow.getResource())
					+ ", becasue resource is unreachable. Returning shadow from the repository.");
			shadow.setFetchResult(parentResult.createOperationResultType());
//			operationResult.recordSuccess();
			operationResult.computeStatus();
			return shadow;
		default:
			throw new CommunicationException(ex);
		}

	}

	private void modifyResourceAvailabilityStatus(ResourceType resource, AvailabilityStatusType status, OperationResult result) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
		
		if (resource.getOperationalState() == null || resource.getOperationalState().getLastAvailabilityStatus() == null || resource.getOperationalState().getLastAvailabilityStatus() != status) {
			List<PropertyDelta> modifications = new ArrayList<PropertyDelta>();
			PropertyDelta statusDelta = PropertyDelta.createModificationReplaceProperty(OperationalStateType.F_LAST_AVAILABILITY_STATUS, resource.asPrismObject().getDefinition(), status);
			modifications.add(statusDelta);
			statusDelta.setParentPath(new PropertyPath(ResourceType.F_OPERATIONAL_STATE));
			resource.getOperationalState().setLastAvailabilityStatus(status);
			cacheRepositoryService.modifyObject(ResourceType.class, resource.getOid(), modifications, result);
		}
	}
	
	private <T extends ResourceObjectShadowType> Collection<ItemDelta> createShadowModification(T shadow) {
		Collection<ItemDelta> modifications = new ArrayList<ItemDelta>();

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
	
//		propertyDelta = PropertyDelta.createReplaceDelta(shadow.asPrismObject().getDefinition(),
//				ResourceObjectShadowType.F_ATTEMPT_NUMBER, getAttemptNumber(shadow));
//		modifications.add(propertyDelta);

		modifications = createAttemptModification(shadow, modifications);
		
		return modifications;
	}

//	private Integer getAttemptNumber(ResourceObjectShadowType shadow) {
//		Integer attemptNumber = (shadow.getAttemptNumber() == null ? 0 : shadow.getAttemptNumber()+1);
//		return attemptNumber;
//	}
}
