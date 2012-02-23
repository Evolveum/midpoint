package com.evolveum.midpoint.provisioning.consistency.impl;

import java.util.Collection;

import javax.xml.namespace.QName;

import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.consistency.api.ErrorHandler;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.FailedOperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;

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
	public void handleError(ResourceObjectShadowType shadow, Exception ex) throws SchemaException,
			GenericFrameworkException, CommunicationException, ObjectNotFoundException,
			ObjectAlreadyExistsException {

		OperationResult operationResult = new OperationResult(CommunicationExceptionHandler.class.getName()
				+ ".handleError");
		Validate.notNull(shadow, "Shadow must not be null.");
		Validate.notNull(shadow.getFailedOperationType(), "Failed operation type must not be null.");
		// if the failed operation was adding, then what we need is to store the
		// whole object to the repository to try it add again later
		if (FailedOperationTypeType.ADD == shadow.getFailedOperationType()) {

			if (shadow.getName() == null){
				shadow.setName("unknown"+ String.valueOf(System.currentTimeMillis()));
//				for (shadow.getAttributes().getAny())
			}
			getCacheRepositoryService().addObject(shadow.asPrismObject(), operationResult);

		} else {
			// if the failed operation was modify, we to store the changes, that
			// should be applied to the account (also operation result and
			// operation type for later processing)

			// storing operation result to the account which failed to be
			// modified
			if (FailedOperationTypeType.MODIFY == shadow.getFailedOperationType()) {
				ObjectModificationType shadowModification = ObjectTypeUtil.createModificationReplaceProperty(
						shadow.getOid(), SchemaConstants.C_RESULT, shadow.getResult());

				// storing failed operation type
				PropertyModificationType propertyModification = ObjectTypeUtil
						.createPropertyModificationType(PropertyModificationTypeType.replace, null,
								SchemaConstants.C_FAILED_OPERATION_TYPE, FailedOperationTypeType.MODIFY);
				shadowModification.getPropertyModification().add(propertyModification);

				propertyModification = ObjectTypeUtil.createPropertyModificationType(
						PropertyModificationTypeType.replace, null, new QName(SchemaConstants.NS_C,
								"objectChange"), shadow.getObjectChange());
				shadowModification.getPropertyModification().add(propertyModification);

				Collection<? extends ItemDelta> modifications = DeltaConvertor.toModifications(shadowModification);
				getCacheRepositoryService().modifyObject(AccountShadowType.class, shadow.getOid(), modifications,
						operationResult);
			} else {
				// this is the case when the deletion of account failed..in this
				// case, we need to sign the account with the tombstone and
				// delete it later

				ObjectModificationType shadowModification = ObjectTypeUtil.createModificationReplaceProperty(
						shadow.getOid(), SchemaConstants.C_RESULT, shadow.getResult());

				PropertyModificationType propertyModification = ObjectTypeUtil
						.createPropertyModificationType(PropertyModificationTypeType.replace, null,
								SchemaConstants.C_FAILED_OPERATION_TYPE, FailedOperationTypeType.DELETE);
				shadowModification.getPropertyModification().add(propertyModification);

				Collection<? extends ItemDelta> modifications = DeltaConvertor.toModifications(shadowModification);
				getCacheRepositoryService().modifyObject(AccountShadowType.class, shadow.getOid(), modifications,
						operationResult);

			}
		}

		throw new CommunicationException("Error communication with the connector while processing shadow "
				+ ObjectTypeUtil.toShortString(shadow), ex);

	}
}
