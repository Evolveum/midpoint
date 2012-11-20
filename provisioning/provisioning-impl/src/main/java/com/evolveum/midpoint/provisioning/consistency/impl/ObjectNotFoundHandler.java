package com.evolveum.midpoint.provisioning.consistency.impl;

import java.util.ArrayList;
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
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.provisioning.api.ChangeNotificationDispatcher;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.provisioning.consistency.api.ErrorHandler;
import com.evolveum.midpoint.provisioning.impl.ShadowCache;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.util.ShadowCacheUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
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
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.FailedOperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SynchronizationSituationType;
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
	@Autowired(required = true)
	private ShadowCache shadowCache;

	private String oid = null;
	
	private static final Trace LOGGER = TraceManager.getTrace(ObjectNotFoundHandler.class);

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
			ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException, SecurityViolationException {

		OperationResult result = parentResult
				.createSubresult("Compensating object not found situation while execution operation: " + op.name());
		result.addParam("shadow", shadow);
		result.addParam("currentOperation", op);
		if (ex.getMessage() != null) {
			result.addParam("exception", ex.getMessage());
		}

		LOGGER.trace("Start compensationg object not found situation while execution operation: {}", op.name());
		
		switch (op) {
		case DELETE:
			LOGGER.trace("Deleting sahdow from the repostiory.");
			cacheRepositoryService.deleteObject(AccountShadowType.class, shadow.getOid(), result);
			result.recordStatus(
					OperationResultStatus.HANDLED_ERROR,
					"Account was not found on the "
							+ ObjectTypeUtil.toShortString(shadow.getResource())
							+ ". Shadow deleted from the repository to equalize the state on the resource and in the repository.");
			LOGGER.trace("Shadow deleted from the repository. Inconsistencies are now removed.");
			return null;
		case MODIFY:
			LOGGER.trace("Starting discovery to find out if the account should exist or not.");
			OperationResult handleErrorResult = result.createSubresult("Discovery for situation: Object not found on the " + ObjectTypeUtil.toShortString(shadow.getResource()));
			
			ObjectDeltaType shadowModifications = shadow.getObjectChange();
			Collection<? extends ItemDelta> modifications = DeltaConvertor.toModifications(
					shadowModifications.getModification(), shadow.asPrismObject().getDefinition());
			
			shadow.setDead(true);
			Collection<PropertyDelta> deadDeltas = new ArrayList<PropertyDelta>();
			deadDeltas.add(PropertyDelta.createModificationReplaceProperty(AccountShadowType.F_DEAD, shadow.asPrismObject().getDefinition(), true));
			cacheRepositoryService.modifyObject(AccountShadowType.class, shadow.getOid(), deadDeltas, result);
			
			ResourceObjectShadowChangeDescription change = createResourceObjectShadowChangeDescription(shadow,
					result);

			// notify model, that the expected account doesn't exist on the
			// resource..(the change form resource is therefore deleted) and let
			// the model to decide, if the account will be revived or unlinked
			// form the user
			// TODO: task initialication
			Task task = taskManager.createTaskInstance();
			changeNotificationDispatcher.notifyChange(change, task, handleErrorResult);
			handleErrorResult.computeStatus();
			String oidVal = null;
			foundReturnedValue(handleErrorResult, oidVal);
			if (oid != null){
				LOGGER.trace("Found new oid {} as a return param from model. Probably the new shadow was created.", oid);
			}
			
			if (oid != null ) {
				LOGGER.trace("Modifying re-created account according to given changes.");
				try {
					provisioningService.modifyObject(AccountShadowType.class, oid, modifications, null,
							result);
					parentResult.recordHandledError(
							"Account was recreated and modifications were applied to newly cleated account.");
				} catch (ObjectNotFoundException e) {
					parentResult.recordHandledError(
							"Modifications were not applied, because shadow was deleted by discovery. Repository state were refreshed and unused shadow was deleted.");
				}
//				return shadow;
				
			} else{
				parentResult.recordHandledError(
						"Account was deleted by discovery. Modification were not applied.");
			}
		
//				LOGGER.trace("Shadow was probably unlinked from the user, so the discovery decided that the account should not exist. Deleting also unused shadow from the repo.");
				try {
					cacheRepositoryService.deleteObject(AccountShadowType.class, shadow.getOid(), parentResult);
				} catch (ObjectNotFoundException e) {
					// delete the old shadow that was probably deleted from
					// the
					// user, or the new one was assigned
					//TODO: log this

				}
				result.computeStatus();
				if (oid != null){
					shadowModifications.setOid(oid);
					shadow.setOid(oid);
				}
			return shadow;
		case GET:
			OperationResult handleGetErrorResult = result.createSubresult("Discovery for situation: Object not found on the " + ObjectTypeUtil.toShortString(shadow.getResource()));
			
			shadow.setDead(true);
			ResourceObjectShadowChangeDescription getChange = createResourceObjectShadowChangeDescription(shadow,
					result);

			// notify model, that the expected account doesn't exist on the
			// resource..(the change form resource is therefore deleted) and let
			// the model to decide, if the account will be revived or unlinked
			// form the user
			// TODO: task initialication
			Task getTask = taskManager.createTaskInstance();
			changeNotificationDispatcher.notifyChange(getChange, getTask, handleGetErrorResult);
			// String oidVal = null;
			handleGetErrorResult.computeStatus();
			foundReturnedValue(handleGetErrorResult, null);
			
//			if (oid != null && !shadow.getOid().equals(oid)){
				try {
					cacheRepositoryService.deleteObject(AccountShadowType.class, shadow.getOid(), result);

				} catch (ObjectNotFoundException e) {
					// delete the old shadow that was probably deleted from
					// the
					// user, or the new one was assigned
					//TODO: log this

				}
//			}
			
			if (oid != null) {
				shadow = (T) shadowCache.getShadow(shadow.getClass(), oid, null, result);
				parentResult.recordHandledError("Account was re-created by the discovery.");
				result.computeStatus();
				return shadow;
			} else {
				parentResult.recordHandledError("Account was deleted by the discovery and the invalid link was removed from the user.");
				result.computeStatus();
				throw new ObjectNotFoundException(ex.getMessage(), ex);
			}
			
		default:
			throw new ObjectNotFoundException(ex.getMessage(), ex);
		}

	}


	private ResourceObjectShadowChangeDescription createResourceObjectShadowChangeDescription(
			ResourceObjectShadowType shadow, OperationResult result) {
		ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();

		if (shadow instanceof AccountShadowType) {
			ObjectDelta<AccountShadowType> objectDelta = new ObjectDelta<AccountShadowType>(AccountShadowType.class,
					ChangeType.DELETE, shadow.asPrismObject().getPrismContext());
			objectDelta.setOid(shadow.getOid());
			change.setObjectDelta(objectDelta);
			change.setResource(shadow.getResource().asPrismObject());
			AccountShadowType account = (AccountShadowType) shadow;
			account.setActivation(ShadowCacheUtil.completeActivation(account, account.getResource(), result));
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
		Element nameFilter = QueryUtil.createEqualFilter(doc, holder, nameProperty.getName(), (String) nameProperty
				.getValue().getValue());
		Element resourceFilter = QueryUtil.createEqualRefFilter(doc, null, ResourceObjectShadowType.F_RESOURCE_REF,
				shadow.getResourceRef().getOid());
		Element objectClassFilter = QueryUtil.createEqualFilter(doc, null, ResourceObjectShadowType.F_OBJECT_CLASS,
				shadow.getObjectClass());
		Element filter = QueryUtil
				.createAndFilter(doc, new Element[] { nameFilter, resourceFilter, objectClassFilter });
		return QueryUtil.createQuery(filter);
	}
}
