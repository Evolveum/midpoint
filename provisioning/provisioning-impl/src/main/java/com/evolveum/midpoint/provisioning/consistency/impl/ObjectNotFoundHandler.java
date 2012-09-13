package com.evolveum.midpoint.provisioning.consistency.impl;

import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.api.ChangeNotificationDispatcher;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.provisioning.consistency.api.ErrorHandler;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.util.ShadowCacheUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceObjectShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.FailedOperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.SynchronizationSituationType;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;
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
	@Autowired(required = true)
	private TaskManager taskManager;

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
	public <T extends ResourceObjectShadowType> T handleError(T shadow, FailedOperation op, Exception ex) throws SchemaException,
			GenericFrameworkException, CommunicationException, ObjectNotFoundException,
			ObjectAlreadyExistsException, ConfigurationException, SecurityViolationException {

		OperationResult parentResult = OperationResult.createOperationResult(shadow.getResult());
		switch (op) {
		case DELETE:
			cacheRepositoryService.deleteObject(AccountShadowType.class, shadow.getOid(), parentResult);
			return null;
		case MODIFY:
			OperationResult handleErrorResult = parentResult.createSubresult(ObjectNotFoundHandler.class.getName()
					+ ".handleError[MODIFY]");
			ResourceObjectShadowChangeDescription change = createResourceObjectShadowChangeDescription(shadow,
					parentResult);

			// notify model, that the expected account doesn't exist on the
			// resource..(the change form resource is therefore deleted) and let
			// the model to decide, if the account will be revived or unlinked
			// form the user
			// TODO: task initialication
			Task task = taskManager.createTaskInstance();
			changeNotificationDispatcher.notifyChange(change, task, handleErrorResult);
			String oidVal = null;
			foundReturnedValue(handleErrorResult, oidVal);
//			try {
//				PrismObject<AccountShadowType> repoShadow = cacheRepositoryService.getObject(AccountShadowType.class,
//						shadow.getOid(), parentResult);
//				SynchronizationSituationType syncSituation = repoShadow.asObjectable().getSynchronizationSituation();
//				if (syncSituation != null && syncSituation == SynchronizationSituationType.LINKED) {
					 if (oid != null) {
					ObjectDeltaType shadowModifications = shadow.getObjectChange();
					Collection<? extends ItemDelta> modifications = DeltaConvertor.toModifications(
							shadowModifications.getModification(), shadow.asPrismObject().getDefinition());
					try {
						provisioningService.modifyObject(AccountShadowType.class, shadow.getOid(), modifications, null,
								parentResult);
					} catch (ObjectNotFoundException e) {
						parentResult
								.recordWarning("Modifications were not applied, because shadow was previously deleted. Repository state were refreshed.");
					}
				} else {
					try {
						cacheRepositoryService.deleteObject(AccountShadowType.class, shadow.getOid(), parentResult);

					} catch (ObjectNotFoundException e) {
						// delete the old shadow that was probably deleted from
						// the
						// user, or the new one was assigned
						parentResult
								.recordWarning("Modifications were not applied, because shadow was previously deleted. Repository state were refreshed.");

					}
				}

//			} catch (ObjectNotFoundException e) {
				// delete the old shadow that was probably deleted from the
				// user, or the new one was assigned
//				parentResult.recordWarning("Cannot get shadow with oid " + shadow.getOid()
//						+ ". It was probably previously deleted by synchronization process.");
//
//			}
			
	
			return shadow;
			default: 
				throw new ObjectNotFoundException(ex.getMessage(), ex);
		}
		
	}



//		if (shadow.getFailedOperationType() == FailedOperationTypeType.DELETE) {
//
//			OperationResult parentResult = OperationResult.createOperationResult(shadow.getResult());
////			if (shadow instanceof AccountShadowType) {
//				cacheRepositoryService.deleteObject(AccountShadowType.class, shadow.getOid(), parentResult);
//
////			}
//			return null;
//		}
//
//		if (shadow.getFailedOperationType() == FailedOperationTypeType.MODIFY) {
//			OperationResult parentResult = OperationResult.createOperationResult(shadow.getResult());
//			OperationResult handleErrorResult = parentResult.createSubresult(ObjectNotFoundHandler.class
//					.getName() + ".handleError[MODIFY]");
//			ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
//
//			if (shadow instanceof AccountShadowType) {
//
//				// cacheRepositoryService.deleteObject(AccountShadowType.class,
//				// shadow.getOid(), parentResult);
//
//				ObjectDelta<AccountShadowType> objectDelta = new ObjectDelta<AccountShadowType>(
//						AccountShadowType.class, ChangeType.DELETE, shadow.asPrismObject().getPrismContext());
//				objectDelta.setOid(shadow.getOid());
//				change.setObjectDelta(objectDelta);
//				change.setResource(shadow.getResource().asPrismObject());
//				AccountShadowType account = (AccountShadowType) shadow;
//				account.setActivation(ShadowCacheUtil.completeActivation(account, account.getResource(),
//						parentResult));
//				 change.setOldShadow(account.asPrismObject());
//				// change.setCurrentShadow(account.asPrismObject());
//
//			}
//
//			change.setSourceChannel(QNameUtil.qNameToUri(SchemaConstants.CHANGE_CHANNEL_DISCOVERY));
//
//			// notify model, that the expected account doesn't exist on the
//			// resource..(the change form resource is therefore deleted) and let
//			// the model to decide, if the account will be revived or unlinked
//			// form the user
//			//TODO: task initialication
//			Task task = taskManager.createTaskInstance();
//			changeNotificationDispatcher.notifyChange(change, task, handleErrorResult);
//			// String oid = (String)
//			// handleErrorResult.getReturn("createdAccountOid");
//			String oidVal = null;
//			foundReturnedValue(handleErrorResult, oidVal);
//			if (oid != null) {
//
//				ObjectDeltaType shadowModifications = shadow.getObjectChange();
//				Collection<? extends ItemDelta> modifications = DeltaConvertor.toModifications(
//						shadowModifications.getModification(), shadow.asPrismObject().getDefinition());
//
//				// QueryType query = createQueryByIcfName(shadow);
//				// final List<AccountShadowType> foundAccount = new
//				// ArrayList<AccountShadowType>();
//				// provisioningService.searchObjectsIterative(AccountShadowType.class,
//				// query, null,
//				// new ResultHandler<AccountShadowType>() {
//				//
//				// @Override
//				// public boolean handle(PrismObject<AccountShadowType> object,
//				// OperationResult parentResult) {
//				// return foundAccount.add(object.asObjectable());
//				// }
//				// }, parentResult);
//				//
//				// if (foundAccount.size() > 1) {
//				// throw new IllegalArgumentException(
//				// "More than one account with the same identifier found on the resource.");
//				// }
//				//
//				// if (!foundAccount.isEmpty()) {
//
//				try {
//					provisioningService.modifyObject(AccountShadowType.class, oid, modifications, null,
//							parentResult);
//				} catch (ObjectNotFoundException e) {
//					parentResult
//							.recordWarning("Modifications were not applied, because shadow was previously deleted. Repository state were refreshed.");
//
//				}
//			}
//
//			try {
//					cacheRepositoryService.deleteObject(AccountShadowType.class, shadow.getOid(),
//							parentResult);
//				
//			} catch (ObjectNotFoundException e) {
//				//delete the old shadow that was probably deleted from the user, or the new one was assigned
//				parentResult
//				.recordWarning("Modifications were not applied, because shadow was previously deleted. Repository state were refreshed.");
//				
//
//			}
//			// ObjectTypeUtil.toShortString(shadow));
//			return shadow;
//		}
//		
//		throw new ObjectNotFoundException(ex);
//
//	}

	private ResourceObjectShadowChangeDescription createResourceObjectShadowChangeDescription(ResourceObjectShadowType shadow, OperationResult result) {
		ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();

		if (shadow instanceof AccountShadowType) {
			ObjectDelta<AccountShadowType> objectDelta = new ObjectDelta<AccountShadowType>(
					AccountShadowType.class, ChangeType.DELETE, shadow.asPrismObject().getPrismContext());
			objectDelta.setOid(shadow.getOid());
			change.setObjectDelta(objectDelta);
			change.setResource(shadow.getResource().asPrismObject());
			AccountShadowType account = (AccountShadowType) shadow;
			account.setActivation(ShadowCacheUtil.completeActivation(account, account.getResource(),
					result));
			 change.setOldShadow(account.asPrismObject());
		}

		change.setSourceChannel(QNameUtil.qNameToUri(SchemaConstants.CHANGE_CHANNEL_DISCOVERY));
		return change;
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
