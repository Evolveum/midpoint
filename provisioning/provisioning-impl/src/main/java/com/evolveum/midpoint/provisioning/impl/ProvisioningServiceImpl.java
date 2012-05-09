/*
 * Copyright (c) 2011 Evolveum
 * 
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 * 
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.provisioning.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.provisioning.api.ChangeNotificationDispatcher;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.provisioning.api.ResultHandler;
import com.evolveum.midpoint.provisioning.ucf.api.Change;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.util.ShadowCacheUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorHostType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.FailedOperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ScriptsType;

/**
 * Implementation of provisioning service.
 * 
 * It is just a "dispatcher" that routes interface calls to appropriate places.
 * E.g. the operations regarding resource definitions are routed directly to the
 * repository, operations of shadow objects are routed to the shadow cache and
 * so on.
 * 
 * WORK IN PROGRESS
 * 
 * There be dragons. Beware the dog. Do not trespass.
 * 
 * @author Radovan Semancik
 */
@Service(value = "provisioningService")
public class ProvisioningServiceImpl implements ProvisioningService {

	@Autowired(required = true)
	private ShadowCache shadowCache;
	@Autowired(required = true)
	private ResourceTypeManager resourceTypeManager;
	@Autowired(required = true)
	@Qualifier("cacheRepositoryService")
	private RepositoryService cacheRepositoryService;
	@Autowired(required = true)
	private ChangeNotificationDispatcher changeNotificationDispatcher;
	@Autowired(required = true)
	private ConnectorTypeManager connectorTypeManager;
	@Autowired(required = true)
	private PrismContext prismContext;

	private PrismObjectDefinition<ResourceObjectShadowType> resourceObjectShadowDefinition = null;

	private static final Trace LOGGER = TraceManager.getTrace(ProvisioningServiceImpl.class);

	// private static final QName TOKEN_ELEMENT_QNAME = new QName(
	// SchemaConstants.NS_PROVISIONING_LIVE_SYNC, "token");

	public ShadowCache getShadowCache() {
		return shadowCache;
	}

	public void setShadowCache(ShadowCache shadowCache) {
		this.shadowCache = shadowCache;
	}

	public ResourceTypeManager getResourceTypeManager() {
		return resourceTypeManager;
	}

	public void setResourceTypeManager(ResourceTypeManager resourceTypeManager) {
		this.resourceTypeManager = resourceTypeManager;
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

	@SuppressWarnings("unchecked")
	@Override
	public <T extends ObjectType> PrismObject<T> getObject(Class<T> type, String oid, OperationResult parentResult) 
			throws ObjectNotFoundException,
			CommunicationException, SchemaException, ConfigurationException, SecurityViolationException {

		Validate.notNull(oid, "Oid of object to get must not be null.");
		Validate.notNull(parentResult, "Operation result must not be null.");

		// LOGGER.trace("**PROVISIONING: Getting object with oid {}", oid);

		// Result type for this operation
		OperationResult result = parentResult.createSubresult(ProvisioningService.class.getName()
				+ ".getObject");
		result.addParam(OperationResult.PARAM_OID, oid);
		result.addParam(OperationResult.PARAM_TYPE, type);
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);

		PrismObject<T> repositoryObject = null;

		try {
			repositoryObject = getCacheRepositoryService().getObject(type, oid, result);
		} catch (ObjectNotFoundException e) {
			logFatalError(LOGGER, result, "Can't get obejct with oid " + oid + ". Reason " + e.getMessage(),
					e);
			// LOGGER.error("Can't get obejct with oid {}. Reason {}", oid, e);
			// result.recordFatalError("Can't get object with oid " + oid +
			// ". Reason: " + e.getMessage(), e);
			throw e;
		} catch (SchemaException ex) {
			logFatalError(LOGGER, result, "Can't get obejct with oid " + oid + ". Reason " + ex.getMessage(),
					ex);
			// LOGGER.error("Can't get obejct with oid {}. Reason {}", oid, ex);
			// result.recordFatalError("Can't get object with oid " + oid +
			// ". Reason: " + ex.getMessage(), ex);
			throw ex;
		}

		if (repositoryObject.canRepresent(ResourceObjectShadowType.class)) {
			// TODO: optimization needed: avoid multiple "gets" of the same
			// object

			ResourceObjectShadowType shadow = null;
			try {

				
				shadow = getShadowCache().getShadow((Class<ResourceObjectShadowType>) type, oid,
						(ResourceObjectShadowType) (repositoryObject.asObjectable()), result);

			} catch (ObjectNotFoundException e) {
				logFatalError(LOGGER, result,
						"Can't get obejct with oid " + oid + ". Reason " + e.getMessage(), e);
				throw e;
			} catch (CommunicationException e) {
				logFatalError(LOGGER, result,
						"Can't get obejct with oid " + oid + ". Reason " + e.getMessage(), e);
				// LOGGER.error("Can't get obejct with oid {}. Reason {}", oid,
				// e);
				// result.recordFatalError(e);
				
				throw e;
			} catch (SchemaException e) {
				logFatalError(LOGGER, result,
						"Can't get obejct with oid " + oid + ". Reason " + e.getMessage(), e);
				// LOGGER.error("Can't get obejct with oid {}. Reason {}", oid,
				// e);
				// result.recordFatalError(e);
				throw e;
			} catch (ConfigurationException e) {
				logFatalError(LOGGER, result,
						"Can't get obejct with oid " + oid + ". Reason " + e.getMessage(), e);
				// LOGGER.error("Can't get obejct with oid {}. Reason {}", oid,
				// e);
				// result.recordFatalError(e);
				throw e;
			}

			// TODO: object resolving

			result.recordSuccess();
			// LOGGER.trace("Get object finished.");
			return shadow.asPrismObject();

		} else if (repositoryObject.canRepresent(ResourceType.class)) {
			// Make sure that the object is complete, e.g. there is a (fresh)
			// schema
			try {
				ResourceType completeResource = getResourceTypeManager().completeResource(
						(ResourceType) repositoryObject.asObjectable(), null, result);
				result.computeStatus("Resource retrieval failed");
				return completeResource.asPrismObject();
			} catch (ObjectNotFoundException ex) {
				logFatalError(LOGGER, result, "Resource object not found", ex);
				// result.recordFatalError("Resource object not found", ex);
				throw ex;
			} catch (SchemaException ex) {
				logFatalError(LOGGER, result, "Schema violation", ex);
				// result.recordFatalError("Schema violation", ex);
				throw ex;
			} catch (CommunicationException ex) {
				logFatalError(LOGGER, result, "Error communicating with resource", ex);
				// result.recordFatalError("Error communicating with resource",
				// ex);
				throw ex;
			}
		} else {
			result.recordSuccess();
			return repositoryObject;
		}

	}

	private void logFatalError(Trace logger, OperationResult opResult, String message, Exception ex) {
		logger.error(message, ex);
		opResult.recordFatalError(message, ex);
	}

	@Override
	public <T extends ObjectType> String addObject(PrismObject<T> object, ScriptsType scripts,
			OperationResult parentResult) throws ObjectAlreadyExistsException, SchemaException,
			CommunicationException, ObjectNotFoundException, ConfigurationException, SecurityViolationException {
		// TODO

		Validate.notNull(object, "Object to add must not be null.");
		Validate.notNull(parentResult, "Operation result must not be null.");

		LOGGER.trace("**PROVISIONING: Start to add object {}", object);
		boolean reconciled;
		Object isReconciled = parentResult.getParams().get("reconciled");
		if (isReconciled == null){
			reconciled = false;
		} else{
			reconciled = true;
		}

		OperationResult result = parentResult.createSubresult(ProvisioningService.class.getName()
				+ ".addObject");
		result.addParam("object", object);
		result.addParam("scripts", scripts);
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);

		String oid = null;
		if (object.canRepresent(ResourceObjectShadowType.class)) {
			try {
				// calling shadow cache to add object
				oid = getShadowCache().addShadow((ResourceObjectShadowType) object.asObjectable(), reconciled, scripts,
						null, result);
				LOGGER.trace("**PROVISIONING: Added shadow object {}", oid);
				result.recordSuccess();
			} catch (GenericFrameworkException ex) {
				logFatalError(LOGGER, result,
						"Couldn't add object " + object + ". Reason: " + ex.getMessage(), ex);
				// LOGGER.error("**PROVISIONING: Can't add object {}. Reason {}",
				// object, ex);
				// result.recordFatalError("Failed to add shadow object: " +
				// ex.getMessage(), ex);
				throw new CommunicationException(ex.getMessage(), ex);
			} catch (SchemaException ex) {
				logFatalError(LOGGER, result, "Couldn't add object. Reason: " + ex.getMessage(), ex);
				// LOGGER.error("**PROVISIONING: Couldn't add object. Reason: {}",
				// ex.getMessage(), ex);
				// result.recordFatalError("Couldn't add object. Reason: " +
				// ex.getMessage(), ex);
				throw new SchemaException("Couldn't add object. Reason: " + ex.getMessage(), ex);
			} catch (ObjectAlreadyExistsException ex) {
				logFatalError(LOGGER, result,
						"Couldn't add object. Object already exist, " + ex.getMessage(), ex);
				// result.recordFatalError("Could't add object. Object already exist, "
				// + ex.getMessage(), ex);
				throw new ObjectAlreadyExistsException("Could't add object. Object already exist, "
						+ ex.getMessage(), ex);
			} catch (ConfigurationException ex) {
				logFatalError(LOGGER, result, "Couldn't add object. Configuration error, " + ex.getMessage(),
						ex);
				// result.recordFatalError("Could't add object. Configuration error, "
				// + ex.getMessage(), ex);
				throw ex;
			} catch (SecurityViolationException ex) {
				logFatalError(LOGGER, result, "Couldn't add object. Security violation: " + ex.getMessage(),
						ex);
				throw ex;
			}
		} else {
			oid = cacheRepositoryService.addObject(object, result);
		}

		LOGGER.trace("**PROVISIONING: Adding object finished.");
		return oid;
	}

	@Override
	public int synchronize(String resourceOid, Task task, OperationResult parentResult)
			throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, SecurityViolationException {

		Validate.notNull(resourceOid, "Resource oid must not be null.");
		Validate.notNull(task, "Task must not be null.");
		Validate.notNull(parentResult, "Operation result must not be null.");

		OperationResult result = parentResult.createSubresult(ProvisioningService.class.getName()
				+ ".synchronize");
		result.addParam(OperationResult.PARAM_OID, resourceOid);
		result.addParam(OperationResult.PARAM_TASK, task);

		int processedChanges = 0;

		try {
			// Resolve resource
			PrismObject<ResourceType> resourceObject = getObject(ResourceType.class, resourceOid, result);

			ResourceType resourceType = resourceObject.asObjectable();

			LOGGER.trace("**PROVISIONING: Start synchronization of resource {} ",
					SchemaDebugUtil.prettyPrint(resourceType));

			// getting token form task
			PrismProperty tokenProperty = null;

			if (task.getExtension() != null) {
				tokenProperty = task.getExtension(SchemaConstants.SYNC_TOKEN);
			}

			if (tokenProperty != null
					&& (tokenProperty.getValue() == null || tokenProperty.getValue().getValue() == null)) {
				LOGGER.warn("Sync token exists, but it is empty (null value). Ignoring it.");
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Empty sync token property:\n{}", tokenProperty.dump());
				}
				tokenProperty = null;
			}

			// if the token is not specified in the task, get the latest token
			if (tokenProperty == null) {
				tokenProperty = getShadowCache().fetchCurrentToken(resourceType, parentResult);
				if (tokenProperty == null || tokenProperty.getValue() == null
						|| tokenProperty.getValue().getValue() == null) {
					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("Empty current sync token property:\n{}", tokenProperty.dump());
					}
					throw new IllegalStateException("Current sync token null or empty: " + tokenProperty);
				}

			}

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("**PROVISIONING: Got token property: {} from the task extension.",
						SchemaDebugUtil.prettyPrint(tokenProperty));
			}

			// Collection<PropertyDelta> taskModifications = new
			// ArrayList<PropertyDelta>();
			List<Change> changes = null;

			LOGGER.trace("Calling shadow cache to fetch changes.");
			changes = getShadowCache().fetchChanges(resourceType, tokenProperty, result);
			LOGGER.trace("Changes returned to ProvisioningServiceImpl:\n{}", changes);

			// for each change from the connector create change description
			for (Change change : changes) {

				// this is the case,when we want to skip processing of change,
				// because the shadow was not created or found to the resource
				// object
				// it may be caused with the fact, that the object which was
				// created in the resource was deleted before the sync run
				// such a change should be skipped to process consistent changes
				if (change.getOldShadow() == null) {
					PrismProperty<?> newToken = change.getToken();
					task.setExtensionProperty(newToken);
					// PropertyDelta modificatedToken =
					// getTokenModification(newToken);
					// taskModifications.add(modificatedToken);
					processedChanges++;
					LOGGER.debug("Skipping processing change. Can't find appropriate shadow (e.g. the object was deleted on the resource meantime).");
					continue;
				}

				ResourceObjectShadowChangeDescription shadowChangeDescription = createResourceShadowChangeDescription(
						change, resourceType);

				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("**PROVISIONING: Created resource object shadow change description {}",
							SchemaDebugUtil.prettyPrint(shadowChangeDescription));
				}
				OperationResult notifyChangeResult = new OperationResult(ProvisioningService.class.getName()
						+ "notifyChange");
				notifyChangeResult.addParam("resourceObjectShadowChangeDescription", shadowChangeDescription);

				try {
					notifyResourceObjectChangeListeners(shadowChangeDescription, task, notifyChangeResult);
					notifyChangeResult.recordSuccess();
				} catch (RuntimeException ex) {
					logFatalError(LOGGER, notifyChangeResult, "Synchronization error: " + ex.getMessage(), ex);
					// notifyChangeResult.recordFatalError("Runtime exception occur: "
					// + ex.getMessage(), ex);
					saveAccountResult(shadowChangeDescription, change, notifyChangeResult, parentResult);
					// LOGGER.error("Synchronization error: "+ ex.getMessage(),
					// ex);
					throw new SystemException("Synchronization error: " + ex.getMessage(), ex);
				}

				notifyChangeResult.computeStatus("Error by notify change operation.");

				if (notifyChangeResult.isSuccess()) {
					deleteShadowFromRepo(change, parentResult);

					// get updated token from change,
					// create property modification from new token
					// and replace old token with the new one
					PrismProperty<?> newToken = change.getToken();
					task.setExtensionProperty(newToken);
					// PropertyDelta modificatedToken =
					// getTokenModification(newToken);
					// taskModifications.add(modificatedToken);
					processedChanges++;

				} else {
					saveAccountResult(shadowChangeDescription, change, notifyChangeResult, parentResult);
				}

			}
			// also if no changes was detected, update token
			if (changes.isEmpty()) {
				LOGGER.trace("No changes to synchronize on " + ObjectTypeUtil.toShortString(resourceType));
				task.setExtensionProperty(tokenProperty);
				// PropertyDelta modificatedToken =
				// getTokenModification(tokenProperty);
				// taskModifications.add(modificatedToken);
			}
			// task.modify(taskModifications, result);
			task.savePendingModifications(result);

			// This happens in the (scheduled async) task. Recording of results
			// in the task is still not
			// ideal, therefore also log the errors with a full stack trace.
		} catch (ObjectNotFoundException e) {
			logFatalError(LOGGER, result, "Synchronization error: object not found: " + e.getMessage(), e);
			// LOGGER.error("Synchronization error: object not found: {}",
			// e.getMessage(), e);
			// result.recordFatalError(e.getMessage(), e);
			throw e;
		} catch (CommunicationException e) {
			logFatalError(LOGGER, result, "Synchronization error: communication problem: " + e.getMessage(),
					e);
			// LOGGER.error("Synchronization error: communication problem: {}",
			// e.getMessage(), e);
			// result.recordFatalError("Error communicating with connector: " +
			// e.getMessage(), e);
			throw e;
		} catch (GenericFrameworkException e) {
			logFatalError(LOGGER, result,
					"Synchronization error: generic connector framework error: " + e.getMessage(), e);
			// LOGGER.error("Synchronization error: generic connector framework error: {}",
			// e.getMessage(), e);
			// result.recordFatalError(e.getMessage(), e);
			throw new GenericConnectorException(e.getMessage(), e);
		} catch (SchemaException e) {
			logFatalError(LOGGER, result, "Synchronization error: schema problem: " + e.getMessage(), e);
			// LOGGER.error("Synchronization error: schema problem: {}",
			// e.getMessage(), e);
			// result.recordFatalError(e.getMessage(), e);
			throw e;
		} catch (SecurityViolationException e) {
			logFatalError(LOGGER, result, "Synchronization error: security violation: " + e.getMessage(), e);
			throw e;
		} catch (ConfigurationException e) {
			logFatalError(LOGGER, result, "Synchronization error: configuration problem: " + e.getMessage(),
					e);
			// LOGGER.error("Synchronization error: configuration problem: {}",
			// e.getMessage(), e);
			// result.recordFatalError(e.getMessage(), e);
			throw e;
		} catch (RuntimeException e) {
			logFatalError(LOGGER, result, "Synchronization error: unexpected problem: " + e.getMessage(), e);
			// LOGGER.error("Synchronization error: unexpected problem: {}",
			// e.getMessage(), e);
			// result.recordFatalError(e.getMessage(), e);
			throw e;
		}

		result.recordSuccess();
		return processedChanges;

	}

	@Override
	public <T extends ObjectType> List<PrismObject<T>> listObjects(Class<T> objectType, PagingType paging,
			OperationResult parentResult) {

		try {
			return searchObjects(objectType, null, paging, parentResult);
		} catch (Exception e) {
			throw new SystemException(e.getMessage(),e);
		}

	}

	@Override
	public <T extends ObjectType> List<PrismObject<T>> searchObjects(Class<T> type, QueryType query,
			PagingType paging, OperationResult parentResult) throws SchemaException, ObjectNotFoundException,
			CommunicationException, ConfigurationException, SecurityViolationException {
		
		OperationResult result = parentResult.createSubresult(ProvisioningService.class.getName()
				+ ".searchObjects");
		result.addParam("objectType", type);
		result.addParam("paging", paging);
		result.addParam("query", query);
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);
		
		if (!ResourceObjectShadowType.class.isAssignableFrom(type)) {
			List<PrismObject<T>> objects = searchRepoObjects(type, query, paging, result);
			result.computeStatus();
			result.recordSuccessIfUnknown();
			return objects;
		}

		final List<PrismObject<T>> objListType = new ArrayList<PrismObject<T>>();

		final ResultHandler<T> handler = new ResultHandler<T>() {

			@SuppressWarnings("unchecked")
			@Override
			public boolean handle(PrismObject<T> object, OperationResult parentResult) {
				return objListType.add(object);
			}
		};

		searchObjectsIterative(type, query, paging, handler, result);
		// TODO: better error handling
		result.computeStatus();
		return objListType;
	}
	
	
	private <T extends ObjectType> List<PrismObject<T>> searchRepoObjects(Class<T> type, QueryType query, PagingType paging,
			OperationResult result) throws SchemaException {

		List<PrismObject<T>> objListType = null;

		// TODO: should searching connectors trigger rediscovery?

		objListType = getCacheRepositoryService().searchObjects(type, query, paging, result);

		if (ResourceType.class.equals(type)) {
			List<PrismObject<T>> newObjListType = new ArrayList<PrismObject<T>>();
			for (PrismObject<T> obj : objListType) {
				OperationResult objResult = new OperationResult(ProvisioningService.class.getName()
						+ ".searchObjects.object");
				PrismObject<ResourceType> resource = (PrismObject<ResourceType>) obj;
				ResourceType completeResource;

				try {

					completeResource = getResourceTypeManager().completeResource(resource.asObjectable(),
							null, objResult);
					newObjListType.add(completeResource.asPrismObject());
					// TODO: what do to with objResult??

				} catch (ObjectNotFoundException e) {
					LOGGER.error("Error while completing {}: {}. Using non-complete resource.", new Object[] {
							resource, e.getMessage(), e });
					objResult.recordFatalError(e);
					obj.asObjectable().setFetchResult(objResult.createOperationResultType());
					newObjListType.add(obj);
					result.addSubresult(objResult);
					result.recordPartialError(e);

				} catch (SchemaException e) {
					LOGGER.error("Error while completing {}: {}. Using non-complete resource.", new Object[] {
							resource, e.getMessage(), e });
					objResult.recordFatalError(e);
					obj.asObjectable().setFetchResult(objResult.createOperationResultType());
					newObjListType.add(obj);
					result.addSubresult(objResult);
					result.recordPartialError(e);

				} catch (CommunicationException e) {
					LOGGER.error("Error while completing {}: {}. Using non-complete resource.", new Object[] {
							resource, e.getMessage(), e });
					objResult.recordFatalError(e);
					obj.asObjectable().setFetchResult(objResult.createOperationResultType());
					newObjListType.add(obj);
					result.addSubresult(objResult);
					result.recordPartialError(e);

				} catch (ConfigurationException e) {
					LOGGER.error("Error while completing {}: {}. Using non-complete resource.", new Object[] {
							resource, e.getMessage(), e });
					objResult.recordFatalError(e);
					obj.asObjectable().setFetchResult(objResult.createOperationResultType());
					newObjListType.add(obj);
					result.addSubresult(objResult);
					result.recordPartialError(e);

				} catch (RuntimeException e) {
					// FIXME: Strictly speaking, the runtime exception should
					// not be handled here.
					// The runtime exceptions should be considered fatal anyway
					// ... but some of the
					// ICF exceptions are still translated to system exceptions.
					// So this provides
					// a better robustness now.
					LOGGER.error("System error while completing {}: {}. Using non-complete resource.",
							new Object[] { resource, e.getMessage(), e });
					objResult.recordFatalError(e);
					obj.asObjectable().setFetchResult(objResult.createOperationResultType());
					newObjListType.add(obj);
					result.addSubresult(objResult);
					result.recordPartialError(e);
				}
			}
			return newObjListType;
		}

		return objListType;

	}

	
	public <T extends ObjectType> int countObjects(Class<T> type, QueryType query, OperationResult parentResult) 
			throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
		
		OperationResult result = parentResult.createSubresult(ProvisioningService.class.getName()
				+ ".countObjects");
		result.addParam("objectType", type);
		result.addParam("query", query);
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);
		
		if (!ResourceObjectShadowType.class.isAssignableFrom(type)) {
			int count = getCacheRepositoryService().countObjects(type, query, parentResult);
			result.computeStatus();
			result.recordSuccessIfUnknown();
			return count;
		}

		final Holder<Integer> countHolder = new Holder<Integer>(0);

		final ResultHandler<T> handler = new ResultHandler<T>() {
			
			@SuppressWarnings("unchecked")
			@Override
			public boolean handle(PrismObject<T> object, OperationResult parentResult) {
				int count = countHolder.getValue();
				count++;
				countHolder.setValue(count);
				return true;
			}
		};

		searchObjectsIterative(type, query, null, handler, result);
		// TODO: better error handling
		result.computeStatus();
		return countHolder.getValue();		
	}

	@Override
	public <T extends ObjectType> void modifyObject(Class<T> type, String oid,
			Collection<? extends ItemDelta> modifications, ScriptsType scripts, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {

		Validate.notNull(oid, "OID must not be null.");
		Validate.notNull(modifications, "Modifications must not be null.");
		Validate.notNull(parentResult, "Operation result must not be null.");

		boolean reconciled;
		Object isReconciled = parentResult.getParams().get("reconciled");
		if (isReconciled == null){
			reconciled = false;
		} else{
			reconciled = true;
		}
		
		OperationResult result = parentResult.createSubresult(ProvisioningService.class.getName()
				+ ".modifyObject");
		result.addParam("modifications", modifications);
		result.addParam(OperationResult.PARAM_OID, oid);
		result.addParam("scripts", scripts);
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("*PROVISIONING: modifyObject: object modifications:\n{}",
					DebugUtil.debugDump(modifications));
		}

		// getting object to modify
		PrismObject<T> object = getCacheRepositoryService().getObject(type, oid, parentResult);

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("**PROVISIONING: modifyObject: object to modify:\n{}.", object.dump());
		}

		try {

			// calling shadow cache to modify object
			getShadowCache().modifyShadow(object.asObjectable(), null, oid, modifications, reconciled, scripts,
					parentResult);
			result.recordSuccess();

		} catch (CommunicationException e) {
			logFatalError(LOGGER, result, "Couldn't modify object: communication problem: " + e.getMessage(),
					e);
			// result.recordFatalError(e);
			throw e;
		} catch (GenericFrameworkException e) {
			logFatalError(LOGGER, result,
					"Couldn't modify object: generic error in the connector: " + e.getMessage(), e);
			// result.recordFatalError(e);
			throw new CommunicationException(e.getMessage(), e);
		} catch (SchemaException e) {
			logFatalError(LOGGER, result, "Couldn't modify object: schema problem: " + e.getMessage(), e);
			// result.recordFatalError(e);
			throw e;
		} catch (ObjectNotFoundException e) {
			logFatalError(LOGGER, result, "Couldn't modify object: object doesn't exist: " + e.getMessage(),
					e);
			// result.recordFatalError(e);
			throw e;
		} catch (RuntimeException e) {
			logFatalError(LOGGER, result, "Couldn't modify object: unexpected problem: " + e.getMessage(), e);
			// result.recordFatalError(e);
			throw new SystemException("Internal error: " + e.getMessage(), e);
		} catch (ConfigurationException e) {
			logFatalError(LOGGER, result, "Couldn't modify object: configuration problem: " + e.getMessage(),
					e);
			// result.recordFatalError(e);
			throw e;
		} catch (SecurityViolationException e) {
			logFatalError(LOGGER, result, "Couldn't modify object: security violation: " + e.getMessage(),
					e);
			throw e;
		}

		LOGGER.trace("Finished modifying of object with oid {}", oid);
	}

	@Override
	public <T extends ObjectType> void deleteObject(Class<T> type, String oid, ScriptsType scripts,
			OperationResult parentResult) throws ObjectNotFoundException, CommunicationException,
			SchemaException, ConfigurationException, SecurityViolationException {
		// TODO Auto-generated method stub

		Validate.notNull(oid, "Oid of object to delete must not be null.");
		Validate.notNull(parentResult, "Operation result must not be null.");

		LOGGER.trace("**PROVISIONING: Start to delete object with oid {}", oid);

		
		
		OperationResult result = parentResult.createSubresult(ProvisioningService.class.getName()
				+ ".deleteObject");
		result.addParam("oid", oid);
		result.addParam("scripts", scripts);
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);

		PrismObject<T> object = null;
		try {
			object = getCacheRepositoryService().getObject(type, oid, parentResult);
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("**PROVISIONING: Object from repository to delete:\n{}", object.dump());
			}
		} catch (SchemaException e) {
			result.recordFatalError("Can't get object with oid " + oid + " from repository. Reason:  "
					+ e.getMessage() + " " + e);
			throw new ObjectNotFoundException(e.getMessage());
		}

		// TODO:check also others shadow objects
		if (object.canRepresent(ResourceObjectShadowType.class)) {

			try {
				getShadowCache().deleteShadow(object.asObjectable(), scripts, null, parentResult);
				result.recordSuccess();
			} catch (CommunicationException e) {
				logFatalError(LOGGER, result,
						"Couldn't delete object: communication problem: " + e.getMessage(), e);
				// result.recordFatalError(e);
				throw new CommunicationException(e.getMessage(), e);
			} catch (GenericFrameworkException e) {
				logFatalError(LOGGER, result,
						"Couldn't delete object: generic error in the connector: " + e.getMessage(), e);
				// result.recordFatalError(e);
				throw new CommunicationException(e.getMessage(), e);
			} catch (SchemaException e) {
				logFatalError(LOGGER, result, "Couldn't delete object: schema problem: " + e.getMessage(), e);
				// result.recordFatalError(e);
				throw new SchemaException(e.getMessage(), e);
			} catch (ConfigurationException e) {
				logFatalError(LOGGER, result,
						"Couldn't delete object: configuration problem: " + e.getMessage(), e);
				// result.recordFatalError(e);
				throw e;
			}

		} else {

			try {

				getCacheRepositoryService().deleteObject(type, oid, result);

			} catch (ObjectNotFoundException ex) {
				result.recordFatalError(ex);
				throw ex;
			}

		}
		LOGGER.trace("**PROVISIONING: Finished deleting object.");

		result.recordSuccess();
	}

	@Override
	public OperationResult testResource(String resourceOid) throws ObjectNotFoundException {
		// We are not going to create parent result here. We don't want to
		// pollute the result with
		// implementation details, as this will be usually displayed in the
		// table of "test resource" results.

		Validate.notNull(resourceOid, "Resource OID to test is null.");

		LOGGER.trace("Start testing resource with oid {} ", resourceOid);

		OperationResult parentResult = new OperationResult(TEST_CONNECTION_OPERATION);
		parentResult.addParam("resourceOid", resourceOid);
		parentResult.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);

		ResourceType resourceType = null;
		try {
			PrismObject<ResourceType> resource = getCacheRepositoryService().getObject(ResourceType.class,
					resourceOid, parentResult);

			resourceType = resource.asObjectable();
			resourceTypeManager.testConnection(resourceType, parentResult);

		} catch (ObjectNotFoundException ex) {
			throw new ObjectNotFoundException("Object with OID " + resourceOid + " not found");
		} catch (SchemaException ex) {
			throw new IllegalArgumentException(ex.getMessage(), ex);
		}
		parentResult.computeStatus("Test resource has failed");

		LOGGER.trace("Finished testing {}, result: {} ", ObjectTypeUtil.toShortString(resourceType),
				parentResult.getStatus());
		return parentResult;
	}

	@Override
	public List<PrismObject<? extends ResourceObjectShadowType>> listResourceObjects(String resourceOid,
			QName objectClass, PagingType paging, OperationResult parentResult) throws SchemaException,
			ObjectNotFoundException, CommunicationException {

		final OperationResult result = parentResult.createSubresult(ProvisioningService.class.getName()
				+ ".listResourceObjects");
		result.addParam("resourceOid", resourceOid);
		result.addParam("objectClass", objectClass);
		result.addParam("paging", paging);
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);

		if (resourceOid == null) {
			throw new IllegalArgumentException("Resource not defined in a search query");
		}
		if (objectClass == null) {
			throw new IllegalArgumentException("Objectclass not defined in a search query");
		}

		PrismObject<ResourceType> resource = null;
		try {
			resource = getCacheRepositoryService().getObject(ResourceType.class, resourceOid, result);

		} catch (ObjectNotFoundException e) {
			result.recordFatalError("Resource with oid " + resourceOid + "not found. Reason: " + e);
			throw new ObjectNotFoundException(e.getMessage(), e);
		}

		final List<PrismObject<? extends ResourceObjectShadowType>> objectList = new ArrayList<PrismObject<? extends ResourceObjectShadowType>>();

		final ShadowHandler shadowHandler = new ShadowHandler() {
			@Override
			public boolean handle(ResourceObjectShadowType shadow) {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("listResourceObjects: processing shadow: {}",
							SchemaDebugUtil.prettyPrint(shadow));
				}

				objectList.add(shadow.asPrismObject());
				return true;
			}
		};

		try {
			resourceTypeManager.listShadows(resource.asObjectable(), objectClass, shadowHandler, false,
					result);
		} catch (ConfigurationException ex) {
			parentResult.recordFatalError(ex.getMessage(), ex);
			throw new CommunicationException("Error in the configuration: " + ex.getMessage(), ex);
		}
		return objectList;
	}

	@Override
	public <T extends ObjectType> void searchObjectsIterative(Class<T> type, QueryType query,
			PagingType paging, final ResultHandler<T> handler, final OperationResult parentResult)
			throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {

		Validate.notNull(parentResult, "Operation result must not be null.");

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Start to search object. Query {}", QueryUtil.dump(query));
		}

		final OperationResult result = parentResult.createSubresult(ProvisioningService.class.getName()
				+ ".searchObjectsIterative");
		result.addParam("query", query);
		result.addParam("paging", paging);
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);
		
		Element filter = null;
		if (query != null) {
			filter = query.getFilter();
		}
		NodeList list = null;
		if (filter != null) {
			list = filter.getChildNodes();
		}
		String resourceOid = null;
		QName objectClass = null;
		List<NodeList> attributeFilter = new ArrayList<NodeList>();

		if (list != null) {
			if (QNameUtil.compareQName(SchemaConstantsGenerated.Q_AND, filter)) {
				for (int i = 0; i < list.getLength(); i++) {
					if (QNameUtil.compareQName(SchemaConstantsGenerated.Q_TYPE, list.item(i))) {
						String filterType = list.item(i).getAttributes().getNamedItem("uri").getNodeValue();
						if (filterType == null || "".equals(filterType)) {
							result.recordFatalError("Object type is not defined.");
							throw new IllegalArgumentException("Object type is not defined.");
						}
	
					} else if (QNameUtil.compareQName(SchemaConstantsGenerated.Q_EQUAL, list.item(i))) {
						NodeList equealList = list.item(i).getChildNodes();
	
						for (int j = 0; j < equealList.getLength(); j++) {
							if (QNameUtil.compareQName(SchemaConstantsGenerated.Q_VALUE, equealList.item(j))) {
								Node value = equealList.item(j).getFirstChild();
								if (QNameUtil.compareQName(SchemaConstants.I_RESOURCE_REF, value)) {
									resourceOid = value.getAttributes().getNamedItem("oid").getNodeValue();
									LOGGER.trace("**PROVISIONING: Search objects on resource with oid {}",
											resourceOid);
	
								} else if (QNameUtil.compareQName(SchemaConstants.I_OBJECT_CLASS, value)) {
									objectClass = DOMUtil.getQNameValue((Element) value);
									LOGGER.trace("**PROVISIONING: Object class to search: {}", objectClass);
									if (objectClass == null) {
										result.recordFatalError("Object class was not defined.");
										throw new IllegalArgumentException("Object class was not defined.");
									}
								}
							} else if (QNameUtil.compareQName(SchemaConstantsGenerated.Q_PATH, equealList.item(j))){
	
								attributeFilter.add(equealList);
								continue;
	
							}
						}
					}
				}
			}
		}

		if (resourceOid == null) {
			throw new IllegalArgumentException("Resource not defined in a search query");
		}
		if (objectClass == null) {
			throw new IllegalArgumentException("Objectclass not defined in a search query");
		}

		PrismObject<ResourceType> resource = null;
		try {
			// Don't use repository. Repository resource will not have properly
			// set capabilities
			resource = getObject(ResourceType.class, resourceOid, result);

		} catch (ObjectNotFoundException e) {
			result.recordFatalError("Resource with oid " + resourceOid + "not found. Reason: " + e);
			throw new ObjectNotFoundException(e.getMessage(), e);
		} catch (ConfigurationException e) {
			result.recordFatalError("Configuration error regarding resource with oid " + resourceOid
					+ ". Reason: " + e);
			throw e;
		} catch (SecurityViolationException e) {
			result.recordFatalError("Security violation: " + e);
			throw e;
		}

		List<ResourceAttribute> resourceAttributesFilter = new ArrayList<ResourceAttribute>();

		if (!attributeFilter.isEmpty()) {
			ResourceSchema schema = RefinedResourceSchema.getResourceSchema(resource, prismContext);
			ObjectClassComplexTypeDefinition objectClassDef = schema.findObjectClassDefinition(objectClass);
			for (NodeList attribute : attributeFilter) {
				for (int j = 0; j < attribute.getLength(); j++) {
					Node attrFilter = attribute.item(j);

					if (!QNameUtil.compareQName(SchemaConstantsGenerated.Q_PATH, attrFilter)) {
						QName attrName = QNameUtil.getNodeQName(attrFilter.getFirstChild());
						ResourceAttributeDefinition resourceAttrDef = objectClassDef
								.findAttributeDefinition(attrName);
						ResourceAttribute resourceAttr = resourceAttrDef.instantiate();
						resourceAttr.setRealValue(attrFilter.getFirstChild().getTextContent());
						resourceAttributesFilter.add(resourceAttr);
					}

				}
			}

		}

		final ShadowHandler shadowHandler = new ShadowHandler() {

			@Override
			public boolean handle(ResourceObjectShadowType shadowType) {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("searchObjectsIterative: processing shadow: {}",
							SchemaDebugUtil.prettyPrint(shadowType));
				}

				OperationResult accountResult = result.createSubresult(ProvisioningService.class.getName()
						+ ".searchObjectsIterative.handle");

				boolean doContinue = handler.handle(shadowType.asPrismObject(), accountResult);
				accountResult.computeStatus();

				if (!accountResult.isSuccess()) {
					Collection<? extends ItemDelta> shadowModificationType = PropertyDelta
							.createModificationReplacePropertyCollection(ResourceObjectShadowType.F_RESULT,
									getResourceObjectShadowDefinition(),
									accountResult.createOperationResultType());
					try {
						cacheRepositoryService.modifyObject(AccountShadowType.class, shadowType.getOid(),
								shadowModificationType, result);
					} catch (ObjectNotFoundException ex) {
						result.recordFatalError(
								"Saving of result to " + ObjectTypeUtil.toShortString(shadowType)
										+ " shadow failed: Not found: " + ex.getMessage(), ex);
					} catch (SchemaException ex) {
						result.recordFatalError(
								"Saving of result to " + ObjectTypeUtil.toShortString(shadowType)
										+ " shadow failed: Schema error: " + ex.getMessage(), ex);
					}
				}

				return doContinue;
			}
		};

		getResourceTypeManager().searchObjectsIterative((Class<? extends ResourceObjectShadowType>) type,
				objectClass, resource.asObjectable(), resourceAttributesFilter, shadowHandler, null, result);
		result.recordSuccess();
	}

	private synchronized void notifyResourceObjectChangeListeners(
			ResourceObjectShadowChangeDescription change, Task task, OperationResult parentResult) {
		changeNotificationDispatcher.notifyChange(change, task, parentResult);
	}

	private ResourceObjectShadowChangeDescription createResourceShadowChangeDescription(Change change,
			ResourceType resourceType) {
		ResourceObjectShadowChangeDescription shadowChangeDescription = new ResourceObjectShadowChangeDescription();
		shadowChangeDescription.setObjectDelta(change.getObjectDelta());
		shadowChangeDescription.setResource(resourceType.asPrismObject());
		shadowChangeDescription.setOldShadow(change.getOldShadow());
		ResourceObjectShadowType currentShadowType = change.getCurrentShadow().asObjectable();
		currentShadowType.setActivation(ShadowCacheUtil.completeActivation(currentShadowType, resourceType,
				null));

		shadowChangeDescription.setCurrentShadow(change.getCurrentShadow());
		shadowChangeDescription.setSourceChannel(QNameUtil.qNameToUri(SchemaConstants.CHANGE_CHANNEL_SYNC));
		return shadowChangeDescription;

	}

	private PropertyDelta getTokenModification(PrismProperty token) {
		if (token == null) {
			throw new IllegalArgumentException("Cannot create modification from a null live sync token");
		}
		if (token.getDefinition() == null) {
			throw new IllegalArgumentException("Live sync token " + token
					+ " has no definition. Cannot create modification.");
		}
		PropertyDelta tokenDelta = new PropertyDelta(new PropertyPath(ResourceObjectShadowType.F_EXTENSION,
				token.getName()), token.getDefinition());
		tokenDelta.setValuesToReplace((Collection) token.getValues());
		return tokenDelta;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.evolveum.midpoint.provisioning.api.ProvisioningService#discoverConnectors
	 * (com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorHostType,
	 * com.evolveum.midpoint.common.result.OperationResult)
	 */
	@Override
	public Set<ConnectorType> discoverConnectors(ConnectorHostType hostType, OperationResult parentResult)
			throws CommunicationException {
		OperationResult result = parentResult.createSubresult(ProvisioningService.class.getName()
				+ ".discoverConnectors");
		result.addParam("host", hostType);
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);

		Set<ConnectorType> discoverConnectors;
		try {
			discoverConnectors = connectorTypeManager.discoverConnectors(hostType, result);
		} catch (CommunicationException ex) {
			result.recordFatalError("Discovery failed", ex);
			throw ex;
		}

		result.computeStatus("Connector discovery failed");
		return discoverConnectors;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.evolveum.midpoint.provisioning.api.ProvisioningService#initialize()
	 */
	@Override
	public void postInit(OperationResult parentResult) {

		OperationResult result = parentResult.createSubresult(ProvisioningService.class.getName()
				+ ".initialize");
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);

		// Discover local connectors
		Set<ConnectorType> discoverLocalConnectors = connectorTypeManager.discoverLocalConnectors(result);
		for (ConnectorType connector : discoverLocalConnectors) {
			LOGGER.info("Discovered local connector {}" + ObjectTypeUtil.toShortString(connector));
		}

		result.computeStatus("Provisioning post-initialization failed");
	}

	private ObjectDelta<? extends ResourceObjectShadowType> createShadowResultModification(
			ResourceObjectShadowChangeDescription shadowChangeDescription, Change change,
			OperationResult shadowResult) {

		String shadowOid = null;
		if (change.getObjectDelta() != null && change.getObjectDelta().getOid() != null) {
			shadowOid = change.getObjectDelta().getOid();
		} else {
			if (change.getCurrentShadow().getOid() != null) {
				shadowOid = change.getCurrentShadow().getOid();
			} else {
				if (change.getOldShadow().getOid() != null) {
					shadowOid = change.getOldShadow().getOid();
				} else {
					throw new IllegalArgumentException("No uid value defined for the object to synchronize.");
				}
			}
		}

		PrismObjectDefinition<ResourceObjectShadowType> shadowDefinition = ShadowCacheUtil
				.getResourceObjectShadowDefinition(prismContext);

		ObjectDelta<? extends ResourceObjectShadowType> shadowModification = ObjectDelta
				.createModificationReplaceProperty(ResourceObjectShadowType.class, shadowOid,
						SchemaConstants.C_RESULT, prismContext, shadowResult.createOperationResultType());

		// ObjectDelta<? extends ResourceObjectShadowType> shadowModification =
		// ObjectDelta
		// .createModificationReplaceProperty(shadowOid,
		// SchemaConstants.C_RESULT,
		// shadowResult.createOperationResultType());

		if (change.getObjectDelta() != null && change.getObjectDelta().getChangeType() == ChangeType.DELETE) {
			PrismPropertyDefinition failedOperationTypePropDef = shadowDefinition
					.findPropertyDefinition(ResourceObjectShadowType.F_FAILED_OPERATION_TYPE);
			PropertyDelta failedOperationTypeDelta = new PropertyDelta(
					ResourceObjectShadowType.F_FAILED_OPERATION_TYPE, failedOperationTypePropDef);
			failedOperationTypeDelta
					.setValueToReplace(new PrismPropertyValue(FailedOperationTypeType.DELETE));
			shadowModification.addModification(failedOperationTypeDelta);
		}
		return shadowModification;

	}

	private void saveAccountResult(ResourceObjectShadowChangeDescription shadowChangeDescription,
			Change change, OperationResult notifyChangeResult, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {

		ObjectDelta<ResourceObjectShadowType> shadowModification = (ObjectDelta<ResourceObjectShadowType>) createShadowResultModification(
				shadowChangeDescription, change, notifyChangeResult);
		// maybe better error handling is needed
		cacheRepositoryService.modifyObject(ResourceObjectShadowType.class, shadowModification.getOid(),
				shadowModification.getModifications(), parentResult);

	}

	private void deleteShadowFromRepo(Change change, OperationResult parentResult)
			throws ObjectNotFoundException {
		if (change.getObjectDelta() != null && change.getObjectDelta().getChangeType() == ChangeType.DELETE
				&& change.getOldShadow() != null) {
			LOGGER.debug("Deleting detected shadow object form repository.");
			try {
				cacheRepositoryService.deleteObject(AccountShadowType.class, change.getOldShadow().getOid(),
						parentResult);
			} catch (ObjectNotFoundException ex) {
				parentResult.recordFatalError("Can't find object " + change.getOldShadow()
						+ " in repository.");
				throw new ObjectNotFoundException("Can't find object " + change.getOldShadow()
						+ " in repository.");
			}
			LOGGER.debug("Shadow object deleted successfully form repository.");
		}
	}

	private PrismObjectDefinition<ResourceObjectShadowType> getResourceObjectShadowDefinition() {
		if (resourceObjectShadowDefinition == null) {
			resourceObjectShadowDefinition = prismContext.getSchemaRegistry()
					.findObjectDefinitionByCompileTimeClass(ResourceObjectShadowType.class);
		}
		return resourceObjectShadowDefinition;
	}

}
