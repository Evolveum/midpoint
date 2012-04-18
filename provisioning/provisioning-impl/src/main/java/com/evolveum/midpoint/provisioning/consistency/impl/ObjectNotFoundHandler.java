package com.evolveum.midpoint.provisioning.consistency.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.common.Utils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.api.ChangeNotificationDispatcher;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.provisioning.api.ResultHandler;
import com.evolveum.midpoint.provisioning.consistency.api.ErrorHandler;
import com.evolveum.midpoint.provisioning.ucf.api.Change;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.util.ShadowCacheUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.holder.XPathHolder;

import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;

import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceObjectShadowUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.FailedOperationTypeType;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;
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

	private String oid = null;

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

			OperationResult parentResult = OperationResult.createOperationResult(shadow.getResult());
			if (shadow instanceof AccountShadowType) {
				cacheRepositoryService.deleteObject(AccountShadowType.class, shadow.getOid(), parentResult);

			}
		}

		if (shadow.getFailedOperationType() == FailedOperationTypeType.MODIFY) {
			OperationResult parentResult = OperationResult.createOperationResult(shadow.getResult());
			OperationResult handleErrorResult = parentResult.createSubresult(ObjectNotFoundHandler.class
					.getName() + ".handleError[MODIFY]");
			ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();

			if (shadow instanceof AccountShadowType) {

				// cacheRepositoryService.deleteObject(AccountShadowType.class,
				// shadow.getOid(), parentResult);

				ObjectDelta<AccountShadowType> objectDelta = new ObjectDelta<AccountShadowType>(
						AccountShadowType.class, ChangeType.DELETE);
				objectDelta.setOid(shadow.getOid());
				change.setObjectDelta(objectDelta);
				change.setResource(shadow.getResource().asPrismObject());
				AccountShadowType account = (AccountShadowType) shadow;
				account.setActivation(ShadowCacheUtil.completeActivation(account, account.getResource(),
						parentResult));
				// change.setOldShadow(account.asPrismObject());
				// change.setCurrentShadow(account.asPrismObject());

			}

			change.setSourceChannel(QNameUtil.qNameToUri(SchemaConstants.CHANGE_CHANNEL_SYNC));

			// notify model, that the expected account doesn't exist on the
			// resource..(the change form resource is therefore deleted) and let
			// the model to decide, if the account will be revived or unlinked
			// form the user
			changeNotificationDispatcher.notifyChange(change, null, handleErrorResult);
			// String oid = (String)
			// handleErrorResult.getReturn("createdAccountOid");
			String oidVal = null;
			foundReturnedValue(handleErrorResult, oidVal);
			if (oid != null) {

				ObjectDeltaType shadowModifications = shadow.getObjectChange();
				Collection<? extends ItemDelta> modifications = DeltaConvertor.toModifications(
						shadowModifications.getModification(), shadow.asPrismObject().getDefinition());

				// QueryType query = createQueryByIcfName(shadow);
				// final List<AccountShadowType> foundAccount = new
				// ArrayList<AccountShadowType>();
				// provisioningService.searchObjectsIterative(AccountShadowType.class,
				// query, null,
				// new ResultHandler<AccountShadowType>() {
				//
				// @Override
				// public boolean handle(PrismObject<AccountShadowType> object,
				// OperationResult parentResult) {
				// return foundAccount.add(object.asObjectable());
				// }
				// }, parentResult);
				//
				// if (foundAccount.size() > 1) {
				// throw new IllegalArgumentException(
				// "More than one account with the same identifier found on the resource.");
				// }
				//
				// if (!foundAccount.isEmpty()) {

				try {
					provisioningService.modifyObject(AccountShadowType.class, oid, modifications, null,
							parentResult);
				} catch (ObjectNotFoundException e) {
					parentResult
							.recordWarning("Modifications were not applied, because shadow was previously deleted. Repository state were refreshed.");

				}
			}

			cacheRepositoryService.deleteObject(AccountShadowType.class, shadow.getOid(), parentResult);
			// throw new ObjectNotFoundException("Can't find object " +
			// ObjectTypeUtil.toShortString(shadow));

		}

	}

	private void foundReturnedValue(OperationResult handleErrorResult, String oidVal) {
		if (oidVal != null) {
			oid = oidVal;
			return;
		}
		List<OperationResult> subresults = handleErrorResult.getSubresults();
		for (OperationResult subresult : subresults) {
			String oidValue = (String) subresult.getReturn("createdAccountOid");
			foundReturnedValue(subresult, oidValue);
		}
		return;
	}

	private QueryType createQueryByIcfName(ResourceObjectShadowType shadow) throws SchemaException {
		// TODO: error handling
		Document doc = DOMUtil.getDocument();
		XPathHolder holder = ObjectTypeUtil.createXPathHolder(SchemaConstants.I_ATTRIBUTES);
		PrismProperty nameProperty = shadow.getAttributes().asPrismContainerValue()
				.findProperty(new QName(SchemaConstants.NS_ICF_SCHEMA, "name"));
		Element nameFilter = QueryUtil.createEqualFilter(doc, holder, nameProperty.getName(),
				(String) nameProperty.getValue().getValue());
		Element resourceFilter = QueryUtil.createEqualRefFilter(doc, null,
				ResourceObjectShadowType.F_RESOURCE_REF, shadow.getResourceRef().getOid());
		Element objectClassFilter = QueryUtil.createEqualFilter(doc, null,
				ResourceObjectShadowType.F_OBJECT_CLASS,
				ResourceObjectShadowUtil.getObjectClassDefinition(shadow).getComplexTypeDefinition()
						.getTypeName());
		Element filter = QueryUtil.createAndFilter(doc, new Element[] { nameFilter, resourceFilter,
				objectClassFilter });
		return QueryUtil.createQuery(filter);
	}
}
