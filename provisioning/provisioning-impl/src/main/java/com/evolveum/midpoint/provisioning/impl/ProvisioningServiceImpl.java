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

import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.evolveum.midpoint.common.InternalsConfig;
import com.evolveum.midpoint.common.crypto.CryptoUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.ChangeNotificationDispatcher;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.provisioning.impl.ShadowCacheFactory.Mode;
import com.evolveum.midpoint.provisioning.ucf.api.Change;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.util.ShadowCacheUtil;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.constants.ConnectorTestOperation;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.task.api.Task;
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
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ConnectorHostType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.FailedOperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ProvisioningScriptsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;

/**
 * Implementation of provisioning service.
 * 
 * It is just a "dispatcher" that routes interface calls to appropriate places.
 * E.g. the operations regarding resource definitions are routed directly to the
 * repository, operations of shadow objects are routed to the shadow cache and
 * so on.
 * 
 * @author Radovan Semancik
 */
@Service(value = "provisioningService")
public class ProvisioningServiceImpl implements ProvisioningService {
	
	@Autowired(required = true)
	private ShadowCacheFactory shadowCacheFactory;
	@Autowired(required = true)
	private ResourceManager resourceManager;
	@Autowired(required = true)
	@Qualifier("cacheRepositoryService")
	private RepositoryService cacheRepositoryService;
	@Autowired(required = true)
	private ChangeNotificationDispatcher changeNotificationDispatcher;
	@Autowired(required = true)
	private ConnectorManager connectorManager;
	@Autowired(required = true)
	private PrismContext prismContext;
	
	private PrismObjectDefinition<ShadowType> resourceObjectShadowDefinition;	
	
	private static final Trace LOGGER = TraceManager.getTrace(ProvisioningServiceImpl.class);


	public ShadowCache getShadowCache(ShadowCacheFactory.Mode mode){
		return shadowCacheFactory.getShadowCache(mode);
	}

	public ResourceManager getResourceTypeManager() {
		return resourceManager;
	}

	public void setResourceTypeManager(ResourceManager resourceTypeManager) {
		this.resourceManager = resourceTypeManager;
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

//	@SuppressWarnings("unchecked")
	@Override
	public <T extends ObjectType> PrismObject<T> getObject(Class<T> type, String oid,
			GetOperationOptions options, OperationResult parentResult) throws ObjectNotFoundException,
			CommunicationException, SchemaException, ConfigurationException, SecurityViolationException {

		Validate.notNull(oid, "Oid of object to get must not be null.");
		Validate.notNull(parentResult, "Operation result must not be null.");

		// Result type for this operation
		OperationResult result = parentResult.createMinorSubresult(ProvisioningService.class.getName() + ".getObject");
		result.addParam(OperationResult.PARAM_OID, oid);
		result.addParam(OperationResult.PARAM_TYPE, type);
		result.addParam(OperationResult.PARAM_OPTIONS, options);
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);

		PrismObject<T> repositoryObject = getRepoObject(type, oid, result);
		
		PrismObject<T> resultingObject = null;

		if (GetOperationOptions.isNoFetch(options) || GetOperationOptions.isRaw(options)) {
			
			// We have what we came for here. We have already got it from the repo.
			// Except if that is a shadow then we want to apply definition before returning it.
			
			if (repositoryObject.canRepresent(ShadowType.class)) {
				try {
					applyDefinition((PrismObject<ShadowType>)repositoryObject, result);
				} catch (SchemaException e) {
					if (GetOperationOptions.isRaw(options)) {
						// This is (almost) OK in raw. We want to get whatever is available, even if it violates
						// the schema
						logWarning(LOGGER, result, "Repository object "+repositoryObject+" violates the schema: " + e.getMessage() + ". Reason: ", e);
					} else {
						recordFatalError(LOGGER, result, "Repository object "+repositoryObject+" violates the schema: " + e.getMessage() + ". Reason: ", e);
						throw e;
					}
				} catch (ObjectNotFoundException e) {
					if (GetOperationOptions.isRaw(options)){
						logWarning(LOGGER, result, "Resource defined in shadow does not exist:  " + e.getMessage(), e);
					} else{
					recordFatalError(LOGGER, result, "Resource defined in shadow does not exist:  " + e.getMessage(), e);
					throw e;
					}
				} catch (CommunicationException e) {
					recordFatalError(LOGGER, result, "Resource defined is shadow is not available: "+ e.getMessage(), e);
					throw e;
				} catch (ConfigurationException e) {
					recordFatalError(LOGGER, result, "Could not apply definition to shadow, problem with configuration: "+ e.getMessage(), e);
					throw e;
				}
			}
			
			resultingObject = repositoryObject;
			
		} else if (repositoryObject.canRepresent(ShadowType.class)) {
			// TODO: optimization needed: avoid multiple "gets" of the same
			// object

			try {

				resultingObject = (PrismObject<T>) getShadowCache(Mode.STANDARD).getShadow(oid,
						(PrismObject<ShadowType>) (repositoryObject), options, result);

			} catch (ObjectNotFoundException e) {
				recordFatalError(LOGGER, result, "Error getting object OID=" + oid + ": " + e.getMessage(), e);
				throw e;
			} catch (CommunicationException e) {
				recordFatalError(LOGGER, result, "Error getting object OID=" + oid + ": " + e.getMessage(), e);
				throw e;
			} catch (SchemaException e) {
				recordFatalError(LOGGER, result, "Error getting object OID=" + oid + ": " + e.getMessage(), e);
				throw e;
			} catch (ConfigurationException e) {
				recordFatalError(LOGGER, result, "Error getting object OID=" + oid + ": " + e.getMessage(), e);
				throw e;
			} catch (SecurityViolationException e) {
				recordFatalError(LOGGER, result, "Error getting object OID=" + oid + ": " + e.getMessage(), e);
				throw e;
			}
			

		} else if (repositoryObject.canRepresent(ResourceType.class)) {
			// Make sure that the object is complete, e.g. there is a (fresh)
			// schema
			try {
				resultingObject = (PrismObject<T>) getResourceTypeManager().getResource(
						(PrismObject<ResourceType>) repositoryObject, result);
			} catch (ObjectNotFoundException ex) {
				recordFatalError(LOGGER, result, "Resource object not found", ex);
				throw ex;
			} catch (SchemaException ex) {
				recordFatalError(LOGGER, result, "Schema violation", ex);
				throw ex;
			} catch (CommunicationException ex) {
				recordFatalError(LOGGER, result, "Error communicating with resource", ex);
				throw ex;
			} catch (ConfigurationException ex){
				recordFatalError(LOGGER, result, "Bad resource configuration", ex);
				throw ex;
			}
		} else {
			resultingObject = repositoryObject;
		}
		
		result.computeStatus();
		repositoryObject.asObjectable().setFetchResult(result.createOperationResultType());
		result.cleanupResult();
		
		validateObject(repositoryObject);
		
		return resultingObject;

	}

	private void recordFatalError(Trace logger, OperationResult opResult, String message, Exception ex) {
		if (message == null) {
			message = ex.getMessage();
		}
		logger.error(message, ex);
		opResult.recordFatalError(message, ex);
		opResult.cleanupResult(ex);
	}

	private void logWarning(Trace logger, OperationResult opResult, String message, Exception ex) {
		logger.error(message, ex);
		opResult.recordWarning(message, ex);
	}

	@Override
	public <T extends ObjectType> String addObject(PrismObject<T> object, ProvisioningScriptsType scripts, ProvisioningOperationOptions options,
			Task task, OperationResult parentResult) throws ObjectAlreadyExistsException, SchemaException, CommunicationException,
			ObjectNotFoundException, ConfigurationException, SecurityViolationException {
	
		Validate.notNull(object, "Object to add must not be null.");
		Validate.notNull(parentResult, "Operation result must not be null.");
		
		if (InternalsConfig.encryptionChecks) {
			CryptoUtil.checkEncrypted(object);
		}

		OperationResult result = parentResult.createSubresult(ProvisioningService.class.getName() + ".addObject");
		result.addParam("object", object);
		result.addParam("scripts", scripts);
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);

		String oid = null;
		if (object.canRepresent(ShadowType.class)) {
			try {
				// calling shadow cache to add object
				oid = getShadowCache(Mode.STANDARD).addShadow((PrismObject<ShadowType>) object, scripts,
						null, options, task, result);
				LOGGER.trace("**PROVISIONING: Added shadow object {}", oid);
				result.computeStatus();
			} catch (GenericFrameworkException ex) {
				recordFatalError(LOGGER, result, "Couldn't add object " + object + ". Reason: " + ex.getMessage(), ex);
				throw new CommunicationException(ex.getMessage(), ex);
			} catch (SchemaException ex) {
				recordFatalError(LOGGER, result, "Couldn't add object. Schema violation: " + ex.getMessage(), ex);
				throw new SchemaException("Couldn't add object. Schema violation: " + ex.getMessage(), ex);
			} catch (ObjectAlreadyExistsException ex) {
				result.computeStatus();
				if (!result.isSuccess() && !result.isHandledError()) {
					recordFatalError(LOGGER, result, "Couldn't add object. Object already exist: " + ex.getMessage(), ex);
				} else {
					result.recordSuccess();
				}
				result.cleanupResult(ex);
				throw new ObjectAlreadyExistsException("Could't add object. Object already exists: " + ex.getMessage(),
						ex);
			} catch (ConfigurationException ex) {
				recordFatalError(LOGGER, result, "Couldn't add object. Configuration error: " + ex.getMessage(), ex);
				throw ex;
			} catch (SecurityViolationException ex) {
				recordFatalError(LOGGER, result, "Couldn't add object. Security violation: " + ex.getMessage(), ex);
				throw ex;
			}
		} else {
			RepoAddOptions addOptions = null;
			if (ProvisioningOperationOptions.isOverwrite(options)){
				addOptions = RepoAddOptions.createOverwrite();
			}
			oid = cacheRepositoryService.addObject(object, addOptions, result);
			result.computeStatus();
		}

		result.cleanupResult();
		return oid;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public int synchronize(String resourceOid, QName objectClass, Task task, OperationResult parentResult) throws ObjectNotFoundException,
			CommunicationException, SchemaException, ConfigurationException, SecurityViolationException {

		Validate.notNull(resourceOid, "Resource oid must not be null.");
		Validate.notNull(task, "Task must not be null.");
		Validate.notNull(parentResult, "Operation result must not be null.");

		OperationResult result = parentResult.createSubresult(ProvisioningService.class.getName() + ".synchronize");
		result.addParam(OperationResult.PARAM_OID, resourceOid);
		result.addParam(OperationResult.PARAM_TASK, task);

		int processedChanges = 0;

		try {
			// Resolve resource
			PrismObject<ResourceType> resource = getObject(ResourceType.class, resourceOid, null, result);

			ResourceType resourceType = resource.asObjectable();

			LOGGER.trace("**PROVISIONING: Start synchronization of resource {} ",
					SchemaDebugUtil.prettyPrint(resourceType));

			// getting token form task
			PrismProperty tokenProperty = getTokenProperty(task, resourceType, result);
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("**PROVISIONING: Got token property: {} from the task extension.",
						SchemaDebugUtil.prettyPrint(tokenProperty));
			}

			LOGGER.trace("Calling shadow cache to fetch changes.");
			List<Change<ShadowType>> changes = getShadowCache(Mode.STANDARD).fetchChanges(
					resourceType, objectClass, tokenProperty, result);
			LOGGER.trace("Changes returned to ProvisioningServiceImpl:\n{}", changes);

			// synchronize changes
			LOGGER.trace("Start synchronizing fetched changes.");
			processedChanges = processSynchronization(changes, task, resourceType, tokenProperty, result);
			LOGGER.trace("End synchronizing fetched changes.");
			// This happens in the (scheduled async) task. Recording of results
			// in the task is still not
			// ideal, therefore also log the errors with a full stack trace.
		} catch (ObjectNotFoundException e) {
			recordFatalError(LOGGER, result, "Synchronization error: object not found: " + e.getMessage(), e);
			throw e;
		} catch (CommunicationException e) {
			recordFatalError(LOGGER, result, "Synchronization error: communication problem: " + e.getMessage(), e);
			throw e;
		} catch (ObjectAlreadyExistsException e) {
			recordFatalError(LOGGER, result, "Synchronization error: object already exists problem: " + e.getMessage(), e);
			throw new SystemException(e);
		} catch (GenericFrameworkException e) {
			recordFatalError(LOGGER, result,
					"Synchronization error: generic connector framework error: " + e.getMessage(), e);
			throw new GenericConnectorException(e.getMessage(), e);
		} catch (SchemaException e) {
			recordFatalError(LOGGER, result, "Synchronization error: schema problem: " + e.getMessage(), e);
			throw e;
		} catch (SecurityViolationException e) {
			recordFatalError(LOGGER, result, "Synchronization error: security violation: " + e.getMessage(), e);
			throw e;
		} catch (ConfigurationException e) {
			recordFatalError(LOGGER, result, "Synchronization error: configuration problem: " + e.getMessage(), e);
			throw e;
		} catch (RuntimeException e) {
			recordFatalError(LOGGER, result, "Synchronization error: unexpected problem: " + e.getMessage(), e);
			throw e;
		}

		result.recordSuccess();
		result.cleanupResult();
		return processedChanges;

	}

	@SuppressWarnings("rawtypes")
	private PrismProperty getTokenProperty(Task task, ResourceType resourceType, OperationResult result)
			throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException {
		PrismProperty tokenProperty = null;
		if (task.getExtension() != null) {
			tokenProperty = task.getExtension(SchemaConstants.SYNC_TOKEN);
		}

		if (tokenProperty != null && (tokenProperty.getValue() == null || tokenProperty.getValue().getValue() == null)) {
			LOGGER.warn("Sync token exists, but it is empty (null value). Ignoring it.");
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Empty sync token property:\n{}", tokenProperty.dump());
			}
			tokenProperty = null;
		}

		// if the token is not specified in the task, get the latest token
		if (tokenProperty == null) {
			tokenProperty = getShadowCache(Mode.STANDARD).fetchCurrentToken(resourceType, result);
			if (tokenProperty == null || tokenProperty.getValue() == null
					|| tokenProperty.getValue().getValue() == null) {
				LOGGER.warn("Empty current sync token provided by {}", resourceType);
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Empty current sync token property.");
				}
				return null;
			}

		}
		return tokenProperty;
	}

	@SuppressWarnings("rawtypes")
	private int processSynchronization(List<Change<ShadowType>> changes, Task task, ResourceType resourceType,
			PrismProperty tokenProperty, OperationResult result) throws SchemaException, ObjectNotFoundException,
			ObjectAlreadyExistsException {
		int processedChanges = 0;
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
				recordFatalError(LOGGER, notifyChangeResult, "Synchronization error: " + ex.getMessage(), ex);
				saveAccountResult(shadowChangeDescription, change, notifyChangeResult, result);
				throw new SystemException("Synchronization error: " + ex.getMessage(), ex);
			}

			notifyChangeResult.computeStatus("Error by notify change operation.");

			if (notifyChangeResult.isSuccess()) {
				deleteShadowFromRepo(change, result);

				// get updated token from change,
				// create property modification from new token
				// and replace old token with the new one
				PrismProperty<?> newToken = change.getToken();
				task.setExtensionProperty(newToken);
				processedChanges++;

			} else {
				saveAccountResult(shadowChangeDescription, change, notifyChangeResult, result);
			}

		}
		// also if no changes was detected, update token
		if (changes.isEmpty() && tokenProperty != null) {
			LOGGER.trace("No changes to synchronize on " + ObjectTypeUtil.toShortString(resourceType));
			task.setExtensionProperty(tokenProperty);
		}
		task.savePendingModifications(result);
		return processedChanges;
	}
	
	@Override
	public <T extends ObjectType> List<PrismObject<T>> searchObjects(Class<T> type, ObjectQuery query, 
			OperationResult parentResult) throws SchemaException, ObjectNotFoundException, CommunicationException,
			ConfigurationException, SecurityViolationException {

		OperationResult result = parentResult.createSubresult(ProvisioningService.class.getName() + ".searchObjects");
		result.addParam("objectType", type);
		result.addParam("query", query);
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);

		final List<PrismObject<T>> objListType = new ArrayList<PrismObject<T>>();
		
		try {
			if (!ShadowType.class.isAssignableFrom(type)) {
				List<PrismObject<T>> objects = searchRepoObjects(type, query, result);
				result.computeStatus();
				result.recordSuccessIfUnknown();
				result.cleanupResult();
				validateObjects(objects);
				return objects;
			}
	
			final ResultHandler<T> handler = new ResultHandler<T>() {
				@Override
				public boolean handle(PrismObject<T> object, OperationResult parentResult) {
					return objListType.add(object);
				}
			};
		
			searchObjectsIterative(type, query, handler, result);
			
		} catch (ConfigurationException e) {
			recordFatalError(LOGGER, result, "Could not search objects: configuration problem: " + e.getMessage(), e);
			throw e;
		} catch (SecurityViolationException e) {
			recordFatalError(LOGGER, result, "Could not search objects: security violation: " + e.getMessage(), e);
			throw e;
		} catch (CommunicationException e) {
			recordFatalError(LOGGER, result, "Could not search objects: communication problem: " + e.getMessage(), e);
			throw e;
		} catch (ObjectNotFoundException e) {
			recordFatalError(LOGGER, result, "Could not search objects: resource not found: " + e.getMessage(), e);
			throw e;
		} catch (SchemaException e) {
			recordFatalError(LOGGER, result, "Could not search objects: schema violation: " + e.getMessage(), e);
			throw e;
		} catch (RuntimeException e) {
			recordFatalError(LOGGER, result, null, e);
			throw e;
		}
		
		result.computeStatus();
		result.cleanupResult();
		validateObjects(objListType);
		return objListType;
	}


	@SuppressWarnings("unchecked")
	private <T extends ObjectType> List<PrismObject<T>> searchRepoObjects(Class<T> type, ObjectQuery query, OperationResult result) throws SchemaException {

		List<PrismObject<T>> repoObjects = null;

		// TODO: should searching connectors trigger rediscovery?

		repoObjects = getCacheRepositoryService().searchObjects(type, query, result);

		if (ResourceType.class.equals(type)) {
			List<PrismObject<T>> newObjListType = new ArrayList<PrismObject<T>>();
			for (PrismObject<T> repoObject : repoObjects) {
				OperationResult objResult = new OperationResult(ProvisioningService.class.getName()
						+ ".searchObjects.object");
				PrismObject<ResourceType> repoResource = (PrismObject<ResourceType>) repoObject;

				try {

					PrismObject<ResourceType> completeResource = getResourceTypeManager().getResource(repoResource,
							objResult);
					newObjListType.add((PrismObject<T>) completeResource);
					// TODO: what do to with objResult??

				} catch (ObjectNotFoundException e) {
					LOGGER.error("Error while completing {}: {}. Using non-complete resource.", new Object[] {
							repoResource, e.getMessage(), e });
					objResult.recordFatalError(e);
					repoObject.asObjectable().setFetchResult(objResult.createOperationResultType());
					newObjListType.add(repoObject);
					result.addSubresult(objResult);
					result.recordPartialError(e);

				} catch (SchemaException e) {
					LOGGER.error("Error while completing {}: {}. Using non-complete resource.", new Object[] {
							repoResource, e.getMessage(), e });
					objResult.recordFatalError(e);
					repoObject.asObjectable().setFetchResult(objResult.createOperationResultType());
					newObjListType.add(repoObject);
					result.addSubresult(objResult);
					result.recordPartialError(e);

				} catch (CommunicationException e) {
					LOGGER.error("Error while completing {}: {}. Using non-complete resource.", new Object[] {
							repoResource, e.getMessage(), e });
					objResult.recordFatalError(e);
					repoObject.asObjectable().setFetchResult(objResult.createOperationResultType());
					newObjListType.add(repoObject);
					result.addSubresult(objResult);
					result.recordPartialError(e);

				} catch (ConfigurationException e) {
					LOGGER.error("Error while completing {}: {}. Using non-complete resource.", new Object[] {
							repoResource, e.getMessage(), e });
					objResult.recordFatalError(e);
					repoObject.asObjectable().setFetchResult(objResult.createOperationResultType());
					newObjListType.add(repoObject);
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
					LOGGER.error("System error while completing {}: {}. Using non-complete resource.", new Object[] {
							repoResource, e.getMessage(), e });
					objResult.recordFatalError(e);
					repoObject.asObjectable().setFetchResult(objResult.createOperationResultType());
					newObjListType.add(repoObject);
					result.addSubresult(objResult);
					result.recordPartialError(e);
				}
			}
			return newObjListType;
		}

		return repoObjects;

	}

	public <T extends ObjectType> int countObjects(Class<T> type, ObjectQuery query, OperationResult parentResult)
			throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
			SecurityViolationException {

		OperationResult result = parentResult.createMinorSubresult(ProvisioningService.class.getName() + ".countObjects");
		result.addParam("objectType", type);
		result.addParam("query", query);
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);

		if (!ShadowType.class.isAssignableFrom(type)) {
			int count = getCacheRepositoryService().countObjects(type, query, parentResult);
			result.computeStatus();
			result.recordSuccessIfUnknown();
			result.cleanupResult();
			return count;
		}

		final Holder<Integer> countHolder = new Holder<Integer>(0);

		final ResultHandler<T> handler = new ResultHandler<T>() {
			@Override
			public boolean handle(PrismObject<T> object, OperationResult parentResult) {
				int count = countHolder.getValue();
				count++;
				countHolder.setValue(count);
				return true;
			}
		};
		
		

		searchObjectsIterative(type, query, handler, result);
		// TODO: better error handling
		result.computeStatus();
		result.cleanupResult();
		return countHolder.getValue();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public <T extends ObjectType> String modifyObject(Class<T> type, String oid,
			Collection<? extends ItemDelta> modifications, ProvisioningScriptsType scripts, ProvisioningOperationOptions options, Task task, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
			SecurityViolationException, ObjectAlreadyExistsException {

		Validate.notNull(oid, "OID must not be null.");
		Validate.notNull(modifications, "Modifications must not be null.");
		Validate.notNull(parentResult, "Operation result must not be null.");
		
		if (InternalsConfig.encryptionChecks) {
			CryptoUtil.checkEncrypted(modifications);
		}
		
		if (InternalsConfig.consistencyChecks) {
			ItemDelta.checkConsistence(modifications);
		}

		OperationResult result = parentResult.createSubresult(ProvisioningService.class.getName() + ".modifyObject");
		result.addParam("modifications", modifications);
		result.addParam(OperationResult.PARAM_OID, oid);
		result.addParam("scripts", scripts);
		result.addParam("options", options);
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("*PROVISIONING: modifyObject: object modifications:\n{}", DebugUtil.debugDump(modifications));
		}

		// getting object to modify
		PrismObject<T> object = getRepoObject(type, oid, result);

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("**PROVISIONING: modifyObject: object to modify:\n{}.", object.dump());
		}

		try {
			
			if (ShadowType.class.isAssignableFrom(type)) {
				// calling shadow cache to modify object
				oid = getShadowCache(Mode.STANDARD).modifyShadow((PrismObject<ShadowType>)object, null, oid, modifications, scripts, options, task, 
					result);
			} else {
				cacheRepositoryService.modifyObject(type, oid, modifications, result);
			}
			result.computeStatus();

		} catch (CommunicationException e) {
			recordFatalError(LOGGER, result, "Couldn't modify object: communication problem: " + e.getMessage(), e);
			throw e;
		} catch (GenericFrameworkException e) {
			recordFatalError(LOGGER, result, "Couldn't modify object: generic error in the connector: " + e.getMessage(),
					e);
			throw new CommunicationException(e.getMessage(), e);
		} catch (SchemaException e) {
			recordFatalError(LOGGER, result, "Couldn't modify object: schema problem: " + e.getMessage(), e);
			throw e;
		} catch (ObjectNotFoundException e) {
			recordFatalError(LOGGER, result, "Couldn't modify object: object doesn't exist: " + e.getMessage(), e);
			throw e;
		} catch (RuntimeException e) {
			recordFatalError(LOGGER, result, "Couldn't modify object: unexpected problem: " + e.getMessage(), e);
			throw new SystemException("Internal error: " + e.getMessage(), e);
		} catch (ConfigurationException e) {
			recordFatalError(LOGGER, result, "Couldn't modify object: configuration problem: " + e.getMessage(), e);
			throw e;
		} catch (SecurityViolationException e) {
			recordFatalError(LOGGER, result, "Couldn't modify object: security violation: " + e.getMessage(), e);
			throw e;
		} catch (ObjectAlreadyExistsException e) {
			recordFatalError(LOGGER, result, "Couldn't modify object: object after modification would conflict with another existing object: " + e.getMessage(), e);
			throw e;
		}

		result.cleanupResult();
		return oid;
	}

	@Override
	public <T extends ObjectType> void deleteObject(Class<T> type, String oid, ProvisioningOperationOptions options, ProvisioningScriptsType scripts,
			Task task, OperationResult parentResult) throws ObjectNotFoundException, CommunicationException, SchemaException,
			ConfigurationException, SecurityViolationException {

		Validate.notNull(oid, "Oid of object to delete must not be null.");
		Validate.notNull(parentResult, "Operation result must not be null.");

		LOGGER.trace("**PROVISIONING: Start to delete object with oid {}", oid);

		OperationResult result = parentResult.createSubresult(ProvisioningService.class.getName() + ".deleteObject");
		result.addParam("oid", oid);
		result.addParam("scripts", scripts);
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);

		PrismObject<T> object = getRepoObject(type, oid, result);
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("**PROVISIONING: Object from repository to delete:\n{}", object.dump());
		}

		if (object.canRepresent(ShadowType.class)) {

			try {
				getShadowCache(Mode.STANDARD).deleteShadow((PrismObject<ShadowType>)object, options, scripts, null, task, result);
			} catch (CommunicationException e) {
				recordFatalError(LOGGER, result, "Couldn't delete object: communication problem: " + e.getMessage(), e);
				throw new CommunicationException(e.getMessage(), e);
			} catch (GenericFrameworkException e) {
				recordFatalError(LOGGER, result,
						"Couldn't delete object: generic error in the connector: " + e.getMessage(), e);
				throw new CommunicationException(e.getMessage(), e);
			} catch (SchemaException e) {
				recordFatalError(LOGGER, result, "Couldn't delete object: schema problem: " + e.getMessage(), e);
				throw new SchemaException(e.getMessage(), e);
			} catch (ConfigurationException e) {
				recordFatalError(LOGGER, result, "Couldn't delete object: configuration problem: " + e.getMessage(), e);
				throw e;
			} catch (SecurityViolationException e) {
				recordFatalError(LOGGER, result, "Couldn't delete object: security violation: " + e.getMessage(), e);
				throw e;
			} catch (RuntimeException e){
				recordFatalError(LOGGER, result, "Couldn't delete object: " + e.getMessage(), e);
				throw e;
			}

		} else {

			try {

				getCacheRepositoryService().deleteObject(type, oid, result);

			} catch (ObjectNotFoundException ex) {
				result.recordFatalError(ex);
				result.cleanupResult(ex);
				throw ex;
			}

		}
		LOGGER.trace("**PROVISIONING: Finished deleting object.");

		result.computeStatus();
		result.cleanupResult();
	}

	@Override
	public OperationResult testResource(String resourceOid) throws ObjectNotFoundException {
		// We are not going to create parent result here. We don't want to
		// pollute the result with
		// implementation details, as this will be usually displayed in the
		// table of "test resource" results.

		Validate.notNull(resourceOid, "Resource OID to test is null.");

		LOGGER.trace("Start testing resource with oid {} ", resourceOid);

		OperationResult testResult = new OperationResult(ConnectorTestOperation.TEST_CONNECTION.getOperation());
		testResult.addParam("resourceOid", resourceOid);
		testResult.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);

		ResourceType resourceType = null;
		
		try {
			PrismObject<ResourceType> resource = getRepoObject(ResourceType.class, resourceOid, testResult);

			resourceType = resource.asObjectable();
			resourceManager.testConnection(resourceType, testResult);

//		} catch (ObjectNotFoundException ex) {
//			throw new ObjectNotFoundException("Object with OID " + resourceOid + " not found");
		} catch (SchemaException ex) {
			throw new IllegalArgumentException(ex.getMessage(), ex);
		}
		testResult.computeStatus("Test resource has failed");

		LOGGER.trace("Finished testing {}, result: {} ", ObjectTypeUtil.toShortString(resourceType),
				testResult.getStatus());
		return testResult;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public List<PrismObject<? extends ShadowType>> listResourceObjects(String resourceOid,
			QName objectClass, ObjectPaging paging, OperationResult parentResult) throws SchemaException,
			ObjectNotFoundException, CommunicationException, ConfigurationException {

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
				
		try{
			resource = getObject(ResourceType.class, resourceOid, null, result);
		} catch (SecurityViolationException ex){
			recordFatalError(LOGGER, result, "Could not get resource: security violation: " + ex.getMessage(), ex);
			result.cleanupResult(ex);
			throw new SystemException("Could not get resource: security violation: " +ex.getMessage(), ex);
		}
		
		final List<PrismObject<? extends ShadowType>> objectList = new ArrayList<PrismObject<? extends ShadowType>>();

		final ShadowHandler shadowHandler = new ShadowHandler() {
			@Override
			public boolean handle(ShadowType shadow) {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("listResourceObjects: processing shadow: {}", SchemaDebugUtil.prettyPrint(shadow));
				}

				objectList.add(shadow.asPrismObject());
				return true;
			}
		};

		try {
			getShadowCache(Mode.STANDARD).listShadows(resource.asObjectable(), objectClass, shadowHandler, false, result);
		} catch (ConfigurationException ex) {
			parentResult.recordFatalError(ex.getMessage(), ex);
			result.cleanupResult(ex);
			throw ex;
		}
		result.cleanupResult();
		return objectList;
	}
	
	public <T extends ShadowType> void finishOperation(PrismObject<T> object, ProvisioningOperationOptions options, Task task, OperationResult parentResult)
			throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
			ObjectAlreadyExistsException, SecurityViolationException {
		Validate.notNull(object, "Object for finishing operation must not be null.");
		OperationResult result = parentResult.createSubresult(ProvisioningServiceImpl.class.getName() +".finishOperation");
		PrismObject<ShadowType> shadow = (PrismObject<ShadowType>)object;
		ShadowType shadowType = shadow.asObjectable();
		
		try{
			if (FailedOperationTypeType.ADD == shadowType.getFailedOperationType()){
				getShadowCache(Mode.RECON).addShadow(shadow, null, null, options, task, result);
			} else if (FailedOperationTypeType.MODIFY == shadowType.getFailedOperationType()){
				getShadowCache(Mode.RECON).modifyShadow(shadow, null, shadow.getOid(), new ArrayList<ItemDelta>(), null, options, task, result);
			} else if (FailedOperationTypeType.DELETE == shadowType.getFailedOperationType()){
				getShadowCache(Mode.RECON).deleteShadow(shadow, options, null, null, task, result);
			}
		} catch (CommunicationException e) {
			recordFatalError(LOGGER, result, "Couldn't finish operation: communication problem: " + e.getMessage(), e);
			throw e;
		} catch (GenericFrameworkException e) {
			recordFatalError(LOGGER, result, "Couldn't finish operation: generic error in the connector: " + e.getMessage(),
					e);
			throw new CommunicationException(e.getMessage(), e);
		} catch (SchemaException e) {
			recordFatalError(LOGGER, result, "Couldn't finish operation: schema problem: " + e.getMessage(), e);
			throw e;
		} catch (ObjectNotFoundException e) {
			recordFatalError(LOGGER, result, "Couldn't finish operation: object doesn't exist: " + e.getMessage(), e);
			throw e;
		} catch (RuntimeException e) {
			recordFatalError(LOGGER, result, "Couldn't finish operation: unexpected problem: " + e.getMessage(), e);
			throw new SystemException("Internal error: " + e.getMessage(), e);
		} catch (ConfigurationException e) {
			recordFatalError(LOGGER, result, "Couldn't finish operation: configuration problem: " + e.getMessage(), e);
			throw e;
		} catch (SecurityViolationException e) {
			recordFatalError(LOGGER, result, "Couldn't finish operation: security violation: " + e.getMessage(), e);
			throw e;
		} catch (ObjectAlreadyExistsException e) {
			recordFatalError(LOGGER, result, "Couldn't finish operation: object after modification would conflict with another existing object: " + e.getMessage(), e);
			throw e;
		}
		result.cleanupResult();
	}


	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public <T extends ObjectType> void searchObjectsIterative(Class<T> type, ObjectQuery query, final ResultHandler<T> handler, final OperationResult parentResult) throws SchemaException,
			ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {

		Validate.notNull(parentResult, "Operation result must not be null.");
		Validate.notNull(handler, "Handler must not be null.");

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Start to search object. Query {}", query.dump());
		}

		final OperationResult result = parentResult.createSubresult(ProvisioningService.class.getName()
				+ ".searchObjectsIterative");
		result.addParam("query", query);
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);

		ObjectFilter filter = null;
		if (query != null) {
			filter = query.getFilter();
		}
		
		String resourceOid = null;
		QName objectClass = null;

		if (filter instanceof AndFilter){
			List<? extends ObjectFilter> conditions = ((AndFilter) filter).getCondition();
			resourceOid = ShadowCacheUtil.getResourceOidFromFilter(conditions);
			objectClass = ShadowCacheUtil.getValueFromFilter(conditions, ShadowType.F_OBJECT_CLASS);
		}
		
		LOGGER.trace("**PROVISIONING: Search objects on resource with oid {}", resourceOid);
		
		
		if (resourceOid == null) {
			IllegalArgumentException e = new IllegalArgumentException("Resource not defined in a search query");
			recordFatalError(LOGGER, result, null, e);
			throw e;
		}
		if (objectClass == null) {
			IllegalArgumentException e = new IllegalArgumentException("Objectclass not defined in a search query");
			recordFatalError(LOGGER, result, null, e);
			throw e;
		}

		PrismObject<ResourceType> resource = null;
		try {
			// Don't use repository. Repository resource will not have properly
			// set capabilities
			resource = getObject(ResourceType.class, resourceOid, null, result);

		} catch (ObjectNotFoundException e) {
			recordFatalError(LOGGER, result, "Resource with oid " + resourceOid + "not found: " + e.getMessage(), e);
			throw new ObjectNotFoundException(e.getMessage(), e);
		} catch (ConfigurationException e) {
			recordFatalError(LOGGER, result, "Configuration error regarding resource with oid " + resourceOid + ": " + e.getMessage(), e);
			throw e;
		} catch (SecurityViolationException e) {
			recordFatalError(LOGGER, result, "Security violation: " + e.getMessage(), e);
			throw e;
		}

		final ShadowHandler shadowHandler = new ShadowHandler() {

			@Override
			public boolean handle(ShadowType shadowType) {
				if (shadowType == null) {
					throw new IllegalArgumentException("Null shadow in call to handler");
				}
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("searchObjectsIterative: processing shadow: {}",
							SchemaDebugUtil.prettyPrint(shadowType));
				}

				OperationResult accountResult = result.createSubresult(ProvisioningService.class.getName()
						+ ".searchObjectsIterative.handle");

				PrismObject shadow = shadowType.asPrismObject();
				validateObject(shadow);
				boolean doContinue = handler.handle(shadow, accountResult);
				accountResult.computeStatus();

				if (!accountResult.isSuccess()) {
					Collection<? extends ItemDelta> shadowModificationType = PropertyDelta
							.createModificationReplacePropertyCollection(ShadowType.F_RESULT,
									getResourceObjectShadowDefinition(), accountResult.createOperationResultType());
					try {
						cacheRepositoryService.modifyObject(ShadowType.class, shadowType.getOid(),
								shadowModificationType, result);
					} catch (ObjectNotFoundException ex) {
						result.recordFatalError("Saving of result to " + shadow
								+ " shadow failed: Not found: " + ex.getMessage(), ex);
					} catch (ObjectAlreadyExistsException ex) {
						result.recordFatalError("Saving of result to " + shadow
								+ " shadow failed: Already exists: " + ex.getMessage(), ex);
					} catch (SchemaException ex) {
						result.recordFatalError("Saving of result to " + shadow
								+ " shadow failed: Schema error: " + ex.getMessage(), ex);
					}
				}

				return doContinue;
			}
		};

		getShadowCache(Mode.STANDARD).searchObjectsIterative(objectClass,
				resource.asObjectable(), query, shadowHandler, result);
		result.recordSuccess();
		result.cleanupResult();
	}
	
	private synchronized void notifyResourceObjectChangeListeners(ResourceObjectShadowChangeDescription change,
			Task task, OperationResult parentResult) {
		changeNotificationDispatcher.notifyChange(change, task, parentResult);
	}

	@SuppressWarnings("unchecked")
	private ResourceObjectShadowChangeDescription createResourceShadowChangeDescription(Change<ShadowType> change,
			ResourceType resourceType) {
		ResourceObjectShadowChangeDescription shadowChangeDescription = new ResourceObjectShadowChangeDescription();
		shadowChangeDescription.setObjectDelta(change.getObjectDelta());
		shadowChangeDescription.setResource(resourceType.asPrismObject());
		shadowChangeDescription.setOldShadow(change.getOldShadow());
		shadowChangeDescription.setCurrentShadow(change.getCurrentShadow());
		shadowChangeDescription.setSourceChannel(QNameUtil.qNameToUri(SchemaConstants.CHANGE_CHANNEL_LIVE_SYNC));
		return shadowChangeDescription;

	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.evolveum.midpoint.provisioning.api.ProvisioningService#discoverConnectors
	 * (com.evolveum.midpoint.xml.ns._public.common.common_2.ConnectorHostType,
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
			discoverConnectors = connectorManager.discoverConnectors(hostType, result);
		} catch (CommunicationException ex) {
			recordFatalError(LOGGER, result, "Discovery failed: "+ex.getMessage(), ex);
			throw ex;
		}

		result.computeStatus("Connector discovery failed");
		result.cleanupResult();
		return discoverConnectors;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends ObjectType> void applyDefinition(ObjectDelta<T> delta, OperationResult parentResult)
			throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException {
		if (ShadowType.class.isAssignableFrom(delta.getObjectTypeClass())){	
			getShadowCache(Mode.STANDARD).applyDefinition((ObjectDelta<ShadowType>) delta, parentResult);
		} else if (ResourceType.class.isAssignableFrom(delta.getObjectTypeClass())){
			resourceManager.applyDefinition((ObjectDelta<ResourceType>) delta, parentResult);
		} else {
			throw new IllegalArgumentException("Could not apply definition to deltas for object type: " + delta.getObjectTypeClass());
		}
		
		
	}
	
	@SuppressWarnings("unchecked")
	public <T extends ObjectType> void applyDefinition(PrismObject<T> object, OperationResult parentResult)
			throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException {
		if (ShadowType.class.isAssignableFrom(object.getCompileTimeClass())){	
			getShadowCache(Mode.STANDARD).applyDefinition((PrismObject<ShadowType>) object, parentResult);
		} else if (ResourceType.class.isAssignableFrom(object.getCompileTimeClass())){
			resourceManager.applyDefinition((PrismObject<ResourceType>) object, parentResult);
		} else {
			throw new IllegalArgumentException("Could not apply definition to deltas for object type: " + object.getCompileTimeClass());
		}
	}
	

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.evolveum.midpoint.provisioning.api.ProvisioningService#initialize()
	 */
	@Override
	public void postInit(OperationResult parentResult) {

		OperationResult result = parentResult.createSubresult(ProvisioningService.class.getName() + ".initialize");
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);

		// Discover local connectors
		Set<ConnectorType> discoverLocalConnectors = connectorManager.discoverLocalConnectors(result);
		for (ConnectorType connector : discoverLocalConnectors) {
			LOGGER.info("Discovered local connector {}" + ObjectTypeUtil.toShortString(connector));
		}

		result.computeStatus("Provisioning post-initialization failed");
		result.cleanupResult();
	}

	@SuppressWarnings("rawtypes")
	private Collection<? extends ItemDelta> createShadowResultModification(Change change, OperationResult shadowResult) {
		PrismObjectDefinition<ShadowType> shadowDefinition = getResourceObjectShadowDefinition();
		
		Collection<ItemDelta> modifications = new ArrayList<ItemDelta>();
		PropertyDelta resultDelta = PropertyDelta.createModificationReplaceProperty(
				ShadowType.F_RESULT, shadowDefinition, shadowResult.createOperationResultType());
		modifications.add(resultDelta);
		if (change.getObjectDelta() != null && change.getObjectDelta().getChangeType() == ChangeType.DELETE) {
			PropertyDelta failedOperationTypeDelta = PropertyDelta.createModificationReplaceProperty(ShadowType.F_FAILED_OPERATION_TYPE, shadowDefinition, FailedOperationTypeType.DELETE);
			modifications.add(failedOperationTypeDelta);
		}		
		return modifications;
	}
	
	private String getOidFromChange(Change change){
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
					throw new IllegalArgumentException("No oid value defined for the object to synchronize.");
				}
			}
		}
		return shadowOid;
	}

	@SuppressWarnings("rawtypes")
	private void saveAccountResult(ResourceObjectShadowChangeDescription shadowChangeDescription, Change change,
			OperationResult notifyChangeResult, OperationResult parentResult) throws ObjectNotFoundException,
			SchemaException, ObjectAlreadyExistsException {

		Collection<? extends ItemDelta> shadowModification = createShadowResultModification(change, notifyChangeResult);
		String oid = getOidFromChange(change);
		// maybe better error handling is needed
		try{
		cacheRepositoryService.modifyObject(ShadowType.class, oid,
				shadowModification, parentResult);
		} catch (SchemaException ex){
			parentResult.recordPartialError("Couldn't modify object: schema violation: " + ex.getMessage(), ex);
//			throw ex;
		} catch (ObjectNotFoundException ex){
			parentResult.recordWarning("Couldn't modify object: object not found: " + ex.getMessage(), ex);
//			throw ex;
		} catch (ObjectAlreadyExistsException ex){
			parentResult.recordPartialError("Couldn't modify object: object already exists: " + ex.getMessage(), ex);
//			throw ex;
		}

	}

	private void deleteShadowFromRepo(Change change, OperationResult parentResult) throws ObjectNotFoundException {
		if (change.getObjectDelta() != null && change.getObjectDelta().getChangeType() == ChangeType.DELETE
				&& change.getOldShadow() != null) {
			LOGGER.debug("Deleting detected shadow object form repository.");
			try {
				cacheRepositoryService.deleteObject(ShadowType.class, change.getOldShadow().getOid(),
						parentResult);
			} catch (ObjectNotFoundException ex) {
				parentResult.recordFatalError("Can't find object " + change.getOldShadow() + " in repository.");
				throw new ObjectNotFoundException("Can't find object " + change.getOldShadow() + " in repository.");
			}
			LOGGER.debug("Shadow object deleted successfully form repository.");
		}
	}

	private PrismObjectDefinition<ShadowType> getResourceObjectShadowDefinition() {
		if (resourceObjectShadowDefinition == null) {
			resourceObjectShadowDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(
					ShadowType.class);
		}
		return resourceObjectShadowDefinition;
	}
	
	private <T extends ObjectType> PrismObject<T> getRepoObject(Class<T> type, String oid, OperationResult result) throws ObjectNotFoundException, SchemaException{
		
		try {
			return getCacheRepositoryService().getObject(type, oid, result);
		} catch (ObjectNotFoundException e) {
			recordFatalError(LOGGER, result, "Can't get object with oid " + oid + ". Reason " + e.getMessage(), e);
//			result.recordFatalError("Can't get object with oid " + oid + ". Reason " + e.getMessage(), e);
			throw e;
		} catch (SchemaException ex) {
			recordFatalError(LOGGER, result, "Can't get object with oid " + oid + ". Reason " + ex.getMessage(), ex);
			throw ex;
		} 
	}
	
	private <T extends ObjectType> void validateObjects(Collection<PrismObject<T>> objects) {
		for(PrismObject<T> object: objects) {
			validateObject(object);
		}
	}
	
	private <T extends ObjectType> void validateObject(PrismObject<T> object) {
		if (InternalsConfig.encryptionChecks) {
			CryptoUtil.checkEncrypted(object);
		}
	}

}
