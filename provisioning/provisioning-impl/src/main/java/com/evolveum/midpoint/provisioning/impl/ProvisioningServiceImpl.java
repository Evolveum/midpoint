/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.provisioning.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.annotation.PreDestroy;
import javax.xml.namespace.QName;

import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.evolveum.midpoint.common.crypto.CryptoUtil;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.NoneFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.ConstraintViolationConfirmer;
import com.evolveum.midpoint.provisioning.api.ConstraintsCheckingResult;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.api.ItemComparisonResult;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.impl.ShadowCacheFactory.Mode;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.LabeledString;
import com.evolveum.midpoint.schema.ProvisioningDiag;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SearchResultMetadata;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ConnectorTestOperation;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.ConnectorOperationalStatus;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorHostType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FailedOperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationProvisioningScriptsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvisioningScriptType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

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
@Primary
public class ProvisioningServiceImpl implements ProvisioningService {
	
	private static final String OPERATION_REFRESH_SHADOW = ProvisioningServiceImpl.class.getName() +".refreshShadow";

	@Autowired ShadowCacheFactory shadowCacheFactory;
	@Autowired ResourceManager resourceManager;
	@Autowired ConnectorManager connectorManager;
	@Autowired ProvisioningContextFactory ctxFactory;
	@Autowired PrismContext prismContext;

	@Autowired(required = true)
	@Qualifier("cacheRepositoryService")
	private RepositoryService cacheRepositoryService;

	private PrismObjectDefinition<ShadowType> resourceObjectShadowDefinition;

	private static final Trace LOGGER = TraceManager.getTrace(ProvisioningServiceImpl.class);

    private static final String DETAILS_CONNECTOR_FRAMEWORK_VERSION = "ConnId framework version";       // TODO generalize

	public ShadowCache getShadowCache(ShadowCacheFactory.Mode mode){
		return shadowCacheFactory.getShadowCache(mode);
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
	public <T extends ObjectType> PrismObject<T> getObject(Class<T> type, String oid,
			Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult parentResult) throws ObjectNotFoundException,
			CommunicationException, SchemaException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {

		Validate.notNull(oid, "Oid of object to get must not be null.");
		Validate.notNull(parentResult, "Operation result must not be null.");

		// Result type for this operation
		OperationResult result = parentResult.createMinorSubresult(ProvisioningService.class.getName() + ".getObject");
		result.addParam(OperationResult.PARAM_OID, oid);
		result.addParam(OperationResult.PARAM_TYPE, type);
		result.addArbitraryObjectCollectionAsParam("options", options);
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);

		GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);
		PrismObject<T> resultingObject = null;

		if (ResourceType.class.isAssignableFrom(type)) {

			if (GetOperationOptions.isRaw(rootOptions)) {
                try {
				    resultingObject = (PrismObject<T>) cacheRepositoryService.getObject(ResourceType.class, oid,
						null, result);
                } catch (ObjectNotFoundException|SchemaException ex) {
                    // catching an exception is important because otherwise the result is UNKNOWN
                    result.recordFatalError(ex);
                    throw ex;
                }
				try {
					applyDefinition(resultingObject, task, result);
				} catch (ObjectNotFoundException ex) {
					// this is almost OK, we use raw for debug pages, so we want
					// to return resource and it can be fixed
					result.muteLastSubresultError();
					ProvisioningUtil.logWarning(LOGGER, result,
							"Bad connector reference defined for resource:  " + ex.getMessage(), ex);
				} catch (SchemaException ex){
					result.muteLastSubresultError();
					ProvisioningUtil.logWarning(LOGGER, result,
							"Schema violation:  " + ex.getMessage(), ex);
				} catch (ConfigurationException ex){
					result.muteLastSubresultError();
					ProvisioningUtil.logWarning(LOGGER, result,
							"Configuration problem:  " + ex.getMessage(), ex);
				}
			} else {
				// We need to handle resource specially. This is usually cached
				// and we do not want to get the repository
				// object if it is in the cache.

				// Make sure that the object is complete, e.g. there is a
				// (fresh)
				// schema
				try {
					resultingObject = (PrismObject<T>) resourceManager.getResource(oid, SelectorOptions.findRootOptions(options), task, result);
				} catch (ObjectNotFoundException ex) {
					ProvisioningUtil.recordFatalError(LOGGER, result, "Resource object not found", ex);
					throw ex;
				} catch (SchemaException ex) {
					ProvisioningUtil.recordFatalError(LOGGER, result, "Schema violation", ex);
					throw ex;
				} catch (CommunicationException ex) {
					ProvisioningUtil.recordFatalError(LOGGER, result, "Error communicating with resource", ex);
					throw ex;
				} catch (ConfigurationException ex) {
					ProvisioningUtil.recordFatalError(LOGGER, result, "Bad resource configuration", ex);
					throw ex;
				} catch (ExpressionEvaluationException ex) {
					ProvisioningUtil.recordFatalError(LOGGER, result, "Expression error", ex);
					throw ex;
				}
			}

		} else {
			// Not resource

			PrismObject<T> repositoryObject = getRepoObject(type, oid, rootOptions, result);
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Retrieved repository object:\n{}", repositoryObject.debugDump());
			}

			if (repositoryObject.canRepresent(ShadowType.class)) {

				try {

					resultingObject = (PrismObject<T>) getShadowCache(Mode.STANDARD).getShadow(oid,
							(PrismObject<ShadowType>) (repositoryObject), options, task, result);

				} catch (ObjectNotFoundException e) {
					if (!GetOperationOptions.isAllowNotFound(rootOptions)){
						ProvisioningUtil.recordFatalError(LOGGER, result, "Error getting object OID=" + oid + ": " + e.getMessage(), e);
					} else{
						result.muteLastSubresultError();
						result.computeStatus();
					}
					throw e;
				} catch (CommunicationException | SchemaException | ConfigurationException | SecurityViolationException | RuntimeException | Error e) {
					ProvisioningUtil.recordFatalError(LOGGER, result, "Error getting object OID=" + oid + ": " + e.getMessage(), e);
					throw e;
				} catch (EncryptionException e){
					ProvisioningUtil.recordFatalError(LOGGER, result, "Error getting object OID=" + oid + ": " + e.getMessage(), e);
					throw new SystemException(e);
				}


			} else {
				resultingObject = repositoryObject;
			}
		}

		result.computeStatus();
		if (!GetOperationOptions.isRaw(rootOptions)) {
			resultingObject = resultingObject.cloneIfImmutable();
			resultingObject.asObjectable().setFetchResult(result.createOperationResultType());
		}
		result.cleanupResult();

		return resultingObject;

	}

	@Override
	public <T extends ObjectType> String addObject(PrismObject<T> object, OperationProvisioningScriptsType scripts, ProvisioningOperationOptions options,
			Task task, OperationResult parentResult) throws ObjectAlreadyExistsException, SchemaException, CommunicationException,
			ObjectNotFoundException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {

		Validate.notNull(object, "Object to add must not be null.");
		Validate.notNull(parentResult, "Operation result must not be null.");

		if (InternalsConfig.encryptionChecks) {
			CryptoUtil.checkEncrypted(object);
		}

		OperationResult result = parentResult.createSubresult(ProvisioningService.class.getName() + ".addObject");
		result.addParam("object", object);
		result.addArbitraryObjectAsParam("scripts", scripts);
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
				ProvisioningUtil.recordFatalError(LOGGER, result, "Couldn't add object " + object + ". Reason: " + ex.getMessage(), ex);
				throw new CommunicationException(ex.getMessage(), ex);
			} catch (SchemaException ex) {
				ProvisioningUtil.recordFatalError(LOGGER, result, "Couldn't add object. Schema violation: " + ex.getMessage(), ex);
				throw new SchemaException("Couldn't add object. Schema violation: " + ex.getMessage(), ex);
			} catch (ObjectAlreadyExistsException ex) {
				result.computeStatus();
				if (!result.isSuccess() && !result.isHandledError()) {
					ProvisioningUtil.recordFatalError(LOGGER, result, "Couldn't add object. Object already exist: " + ex.getMessage(), ex);
				} else {
					result.recordSuccess();
				}
				result.cleanupResult(ex);
				throw new ObjectAlreadyExistsException("Couldn't add object. Object already exists: " + ex.getMessage(),
						ex);
			} catch (ConfigurationException ex) {
				ProvisioningUtil.recordFatalError(LOGGER, result, "Couldn't add object. Configuration error: " + ex.getMessage(), ex);
				throw ex;
			} catch (SecurityViolationException ex) {
				ProvisioningUtil.recordFatalError(LOGGER, result, "Couldn't add object. Security violation: " + ex.getMessage(), ex);
				throw ex;
			} catch (ExpressionEvaluationException ex) {
				ProvisioningUtil.recordFatalError(LOGGER, result, "Couldn't add object. Expression error: " + ex.getMessage(), ex);
				throw ex;
			} catch (EncryptionException e) {
				ProvisioningUtil.recordFatalError(LOGGER, result, null, e);
				throw new SystemException(e.getMessage(), e);
			} catch (RuntimeException | Error ex){
				ProvisioningUtil.recordFatalError(LOGGER, result, "Couldn't add object. Runtime error: " + ex.getMessage(), ex);
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
	public int synchronize(ResourceShadowDiscriminator shadowCoordinates, Task task, OperationResult parentResult) throws ObjectNotFoundException,
			CommunicationException, SchemaException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {

		Validate.notNull(shadowCoordinates, "Coordinates oid must not be null.");
		String resourceOid = shadowCoordinates.getResourceOid();
		Validate.notNull(resourceOid, "Resource oid must not be null.");
		Validate.notNull(task, "Task must not be null.");
		Validate.notNull(parentResult, "Operation result must not be null.");

		OperationResult result = parentResult.createSubresult(ProvisioningService.class.getName() + ".synchronize");
		result.addParam(OperationResult.PARAM_OID, resourceOid);
		result.addParam(OperationResult.PARAM_TASK, task.toString());

		int processedChanges = 0;

		try {
			// Resolve resource
			PrismObject<ResourceType> resource = getObject(ResourceType.class, resourceOid, null, task, result);
			ResourceType resourceType = resource.asObjectable();

			LOGGER.trace("**PROVISIONING: Start synchronization of resource {} ", resourceType);

			// getting token form task
			PrismProperty tokenProperty = getTokenProperty(shadowCoordinates, task, result);
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("**PROVISIONING: Got token property: {} from the task extension.",
						SchemaDebugUtil.prettyPrint(tokenProperty));
			}

			processedChanges = getShadowCache(Mode.STANDARD).synchronize(shadowCoordinates, tokenProperty, task, result);
			LOGGER.debug("Synchronization of {} done, token {}, {} changes", resource, tokenProperty, processedChanges);

		} catch (ObjectNotFoundException | CommunicationException | SchemaException | SecurityViolationException | ConfigurationException | ExpressionEvaluationException | RuntimeException | Error e) {
			ProvisioningUtil.recordFatalError(LOGGER, result, null, e);
			throw e;
		} catch (ObjectAlreadyExistsException | EncryptionException e) {
			ProvisioningUtil.recordFatalError(LOGGER, result, null, e);
			throw new SystemException(e);
		} catch (GenericFrameworkException e) {
			ProvisioningUtil.recordFatalError(LOGGER, result,
					"Synchronization error: generic connector framework error: " + e.getMessage(), e);
			throw new GenericConnectorException(e.getMessage(), e);
		}

		result.recordSuccess();
		result.cleanupResult();
		return processedChanges;

	}

	@SuppressWarnings("rawtypes")
	private PrismProperty getTokenProperty(ResourceShadowDiscriminator shadowCoordinates, Task task, OperationResult result)
			throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, ExpressionEvaluationException {
		PrismProperty tokenProperty = null;
		if (task.getExtension() != null) {
			tokenProperty = task.getExtensionProperty(SchemaConstants.SYNC_TOKEN);
		}

		if (tokenProperty != null && (tokenProperty.getAnyRealValue() == null)) {
			LOGGER.warn("Sync token exists, but it is empty (null value). Ignoring it.");
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Empty sync token property:\n{}", tokenProperty.debugDump());
			}
			tokenProperty = null;
		}

		// if the token is not specified in the task, get the latest token
		if (tokenProperty == null) {
			tokenProperty = getShadowCache(Mode.STANDARD).fetchCurrentToken(shadowCoordinates, result);
			if (tokenProperty == null || tokenProperty.getValue() == null
					|| tokenProperty.getValue().getValue() == null) {
				LOGGER.warn("Empty current sync token provided by {}", shadowCoordinates);
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Empty current sync token property.");
				}
				return null;
			}

		}
		return tokenProperty;
	}

	@Override
	public <T extends ObjectType> SearchResultList<PrismObject<T>> searchObjects(Class<T> type, ObjectQuery query,
			Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult parentResult)
					throws SchemaException, ObjectNotFoundException, CommunicationException,
						ConfigurationException, SecurityViolationException, ExpressionEvaluationException {

		OperationResult result = parentResult.createSubresult(ProvisioningService.class.getName() + ".searchObjects");
		result.addParam("objectType", type);
		result.addParam("query", query);
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);

		final SearchResultList<PrismObject<T>> objListType = new SearchResultList<>(new ArrayList<PrismObject<T>>());

		SearchResultMetadata metadata;
		try {
			if (!ShadowType.class.isAssignableFrom(type)) {
				SearchResultList<PrismObject<T>> objects = searchRepoObjects(type, query, options, task, result);
				result.computeStatus();
				result.recordSuccessIfUnknown();
				result.cleanupResult();
//				validateObjects(objects);
				return objects;
			}

			final ResultHandler<T> handler = (object, parentResult1) -> objListType.add(object);

			metadata = searchObjectsIterative(type, query, options, handler, task, result);

		} catch (ConfigurationException | SecurityViolationException | CommunicationException | ObjectNotFoundException |  SchemaException | ExpressionEvaluationException | RuntimeException | Error e) {
			ProvisioningUtil.recordFatalError(LOGGER, result, "Could not search objects: " + e.getMessage(), e);
			throw e;
		}

		result.computeStatus();
		result.cleanupResult();
//		validateObjects(objListType);
		objListType.setMetadata(metadata);
		return objListType;
	}


	@SuppressWarnings("unchecked")
	private <T extends ObjectType> SearchResultList<PrismObject<T>> searchRepoObjects(Class<T> type, ObjectQuery query,
			Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult result) throws SchemaException {

		List<PrismObject<T>> repoObjects = null;

		// TODO: should searching connectors trigger rediscovery?

		Collection<SelectorOptions<GetOperationOptions>> repoOptions = null;
		if (GetOperationOptions.isReadOnly(SelectorOptions.findRootOptions(options))) {
			repoOptions = SelectorOptions.createCollection(GetOperationOptions.createReadOnly());
		}
		repoObjects = getCacheRepositoryService().searchObjects(type, query, repoOptions, result);

		SearchResultList<PrismObject<T>> newObjListType = new SearchResultList(new ArrayList<PrismObject<T>>());
		for (PrismObject<T> repoObject : repoObjects) {
			OperationResult objResult = new OperationResult(ProvisioningService.class.getName()
					+ ".searchObjects.object");

			try {

				PrismObject<T> completeResource = completeObject(type, repoObject, options, task, objResult);
                objResult.computeStatusIfUnknown();
                if (!objResult.isSuccess()) {
                    completeResource.asObjectable().setFetchResult(objResult.createOperationResultType());      // necessary e.g. to skip validation for resources that had issues when checked
                    result.addSubresult(objResult);
                }
                newObjListType.add((PrismObject<T>) completeResource);

				// TODO: what else do to with objResult??

			} catch (ObjectNotFoundException | SchemaException | CommunicationException | ConfigurationException | ExpressionEvaluationException e) {
				LOGGER.error("Error while completing {}: {}-{}. Using non-complete object.", new Object[] {
						repoObject, e.getMessage(), e });
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
				LOGGER.error("System error while completing {}: {}-{}. Using non-complete object.", new Object[] {
						repoObject, e.getMessage(), e });
				objResult.recordFatalError(e);
				repoObject.asObjectable().setFetchResult(objResult.createOperationResultType());
				newObjListType.add(repoObject);
				result.addSubresult(objResult);
				result.recordPartialError(e);
			}
		}
		return newObjListType;

	}

	// TODO: move to ResourceManager and ConnectorManager
	// the shadow-related code is already in the ShadowCache
	private <T extends ObjectType> PrismObject<T> completeObject(Class<T> type, PrismObject<T> inObject,
			Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {

		if (ResourceType.class.equals(type)) {

				PrismObject<ResourceType> completeResource = resourceManager.getResource((PrismObject<ResourceType>) inObject,
						SelectorOptions.findRootOptions(options), task, result);
				return (PrismObject<T>) completeResource;
		} else if (ShadowType.class.equals(type)) {
			// should not happen, the shadow-related code is already in the ShadowCache
			throw new IllegalStateException("BOOOM!");
		} else {
			//TODO: connectors etc..

		}
		return inObject;

	}

	public <T extends ObjectType> Integer countObjects(Class<T> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult parentResult)
			throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
			SecurityViolationException, ExpressionEvaluationException {

		OperationResult result = parentResult.createMinorSubresult(ProvisioningService.class.getName() + ".countObjects");
		result.addParam("objectType", type);
		result.addParam("query", query);
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);

		ObjectFilter filter = null;
		if (query != null) {
			filter = ObjectQueryUtil.simplify(query.getFilter());
			query = query.cloneEmpty();
			query.setFilter(filter);
		}

		if (filter != null && filter instanceof NoneFilter) {
			result.recordSuccessIfUnknown();
			result.cleanupResult();
			LOGGER.trace("Finished counting. Nothing to do. Filter is NONE");
			return 0;
		}

		GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);

		if (!ShadowType.class.isAssignableFrom(type) || GetOperationOptions.isNoFetch(rootOptions) || GetOperationOptions.isRaw(rootOptions)) {
			int count = getCacheRepositoryService().countObjects(type, query, options, parentResult);
			result.computeStatus();
			result.recordSuccessIfUnknown();
			result.cleanupResult();
			return count;
		}

		Integer count;
		try {

			count = getShadowCache(Mode.STANDARD).countObjects(query, task, result);

			result.computeStatus();

		} catch (ConfigurationException | CommunicationException | ObjectNotFoundException | SchemaException | ExpressionEvaluationException | RuntimeException | Error e) {
			ProvisioningUtil.recordFatalError(LOGGER, result, null, e);
			throw e;
		} finally {
			result.cleanupResult();
		}

		return count;
    }

	@SuppressWarnings("rawtypes")
	@Override
	public <T extends ObjectType> String modifyObject(Class<T> type, String oid,
			Collection<? extends ItemDelta> modifications, OperationProvisioningScriptsType scripts, ProvisioningOperationOptions options, Task task, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
			SecurityViolationException, ObjectAlreadyExistsException, ExpressionEvaluationException {

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
		result.addArbitraryObjectCollectionAsParam("modifications", modifications);
		result.addParam(OperationResult.PARAM_OID, oid);
		result.addArbitraryObjectAsParam("scripts", scripts);
		result.addArbitraryObjectAsParam("options", options);
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("*PROVISIONING: modifyObject: object modifications:\n{}", DebugUtil.debugDump(modifications));
		}

		// getting object to modify
		PrismObject<T> repoShadow = getRepoObject(type, oid, null, result);

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("**PROVISIONING: modifyObject: object to modify (repository):\n{}.", repoShadow.debugDump());
		}

		try {

			if (ShadowType.class.isAssignableFrom(type)) {
				// calling shadow cache to modify object
				oid = getShadowCache(Mode.STANDARD).modifyShadow((PrismObject<ShadowType>)repoShadow, modifications, scripts, options, task,
					result);
			} else {
				cacheRepositoryService.modifyObject(type, oid, modifications, result);
			}
			if (!result.isInProgress()) {
				// This is the case when there is already a conflicting pending operation.
				result.computeStatus();
			}

		} catch (CommunicationException | SchemaException | ObjectNotFoundException | ConfigurationException | SecurityViolationException 
				| ExpressionEvaluationException | RuntimeException | Error e) {
			ProvisioningUtil.recordFatalError(LOGGER, result, null, e);
			throw e;
		} catch (GenericFrameworkException e) {
			ProvisioningUtil.recordFatalError(LOGGER, result, null, e);
			throw new CommunicationException(e.getMessage(), e);
		} catch (EncryptionException e) {
			ProvisioningUtil.recordFatalError(LOGGER, result, null, e);
			throw new SystemException(e.getMessage(), e);
		} catch (ObjectAlreadyExistsException e) {
			ProvisioningUtil.recordFatalError(LOGGER, result, "Couldn't modify object: object after modification would conflict with another existing object: " + e.getMessage(), e);
			throw e;
		}

		result.cleanupResult();
		return oid;
	}

	@Override
	public <T extends ObjectType> void deleteObject(Class<T> type, String oid, ProvisioningOperationOptions options, OperationProvisioningScriptsType scripts,
			Task task, OperationResult parentResult) throws ObjectNotFoundException, CommunicationException, SchemaException,
			ConfigurationException, SecurityViolationException, ExpressionEvaluationException {

		Validate.notNull(oid, "Oid of object to delete must not be null.");
		Validate.notNull(parentResult, "Operation result must not be null.");

		LOGGER.trace("**PROVISIONING: Start to delete object with oid {}", oid);

		OperationResult result = parentResult.createSubresult(ProvisioningService.class.getName() + ".deleteObject");
		result.addParam("oid", oid);
		result.addArbitraryObjectAsParam("scripts", scripts);
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);

		//TODO: is critical when shadow does not exits anymore?? do we need to log it?? if not, change null to allowNotFound options
		PrismObject<T> object = getRepoObject(type, oid, null, result);
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("**PROVISIONING: Object from repository to delete:\n{}", object.debugDump());
		}

		if (object.canRepresent(ShadowType.class) && !ProvisioningOperationOptions.isRaw(options)) {

			try {

				getShadowCache(Mode.STANDARD).deleteShadow((PrismObject<ShadowType>)object, options, scripts, task, result);

			} catch (CommunicationException e) {
				ProvisioningUtil.recordFatalError(LOGGER, result, "Couldn't delete object: communication problem: " + e.getMessage(), e);
				throw new CommunicationException(e.getMessage(), e);
			} catch (GenericFrameworkException e) {
				ProvisioningUtil.recordFatalError(LOGGER, result,
						"Couldn't delete object: generic error in the connector: " + e.getMessage(), e);
				throw new CommunicationException(e.getMessage(), e);
			} catch (SchemaException e) {
				ProvisioningUtil.recordFatalError(LOGGER, result, "Couldn't delete object: schema problem: " + e.getMessage(), e);
				throw new SchemaException(e.getMessage(), e);
			} catch (ConfigurationException e) {
				ProvisioningUtil.recordFatalError(LOGGER, result, "Couldn't delete object: configuration problem: " + e.getMessage(), e);
				throw e;
			} catch (SecurityViolationException e) {
				ProvisioningUtil.recordFatalError(LOGGER, result, "Couldn't delete object: security violation: " + e.getMessage(), e);
				throw e;
			} catch (ExpressionEvaluationException e) {
				ProvisioningUtil.recordFatalError(LOGGER, result, "Couldn't delete object: expression errror: " + e.getMessage(), e);
				throw e;
			} catch (RuntimeException | Error e) {
				ProvisioningUtil.recordFatalError(LOGGER, result, "Couldn't delete object: " + e.getMessage(), e);
				throw e;
			}

		} else if (object.canRepresent(ResourceType.class)) {

			resourceManager.deleteResource(oid, options, task, result);

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

		if (!result.isInProgress()) {
			// This is the case when there is already a conflicting pending operation.
			result.computeStatus();
		}
		result.cleanupResult();
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.provisioning.api.ProvisioningService#executeScript(java.lang.Class, java.lang.String, com.evolveum.midpoint.xml.ns._public.common.common_3.ProvisioningScriptType, com.evolveum.midpoint.task.api.Task, com.evolveum.midpoint.schema.result.OperationResult)
	 */
	@Override
	public <T extends ObjectType> Object executeScript(String resourceOid, ProvisioningScriptType script,
			Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException,
			CommunicationException, ConfigurationException, SecurityViolationException, ObjectAlreadyExistsException, ExpressionEvaluationException {
		Validate.notNull(resourceOid, "Oid of object for script execution must not be null.");
		Validate.notNull(parentResult, "Operation result must not be null.");

		OperationResult result = parentResult.createSubresult(ProvisioningService.class.getName() + ".executeScript");
		result.addParam("oid", resourceOid);
		result.addArbitraryObjectAsParam("script", script);
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);

		Object scriptResult;
		try {

			scriptResult = resourceManager.executeScript(resourceOid, script, task, result);

		} catch (CommunicationException | SchemaException | ConfigurationException | SecurityViolationException | ExpressionEvaluationException | RuntimeException | Error  e) {
			ProvisioningUtil.recordFatalError(LOGGER, result, null, e);
			throw e;
		}

		result.computeStatus();
		result.cleanupResult();

		return scriptResult;
	}


	@Override
	public OperationResult testResource(String resourceOid, Task task) throws ObjectNotFoundException {
		// We are not going to create parent result here. We don't want to
		// pollute the result with
		// implementation details, as this will be usually displayed in the
		// table of "test resource" results.

		Validate.notNull(resourceOid, "Resource OID to test is null.");

		LOGGER.trace("Start testing resource with oid {} ", resourceOid);

		OperationResult testResult = new OperationResult(ConnectorTestOperation.TEST_CONNECTION.getOperation());
		testResult.addParam("resourceOid", resourceOid);
		testResult.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);

		PrismObject<ResourceType> resource = null;

		try {
			resource = getRepoObject(ResourceType.class, resourceOid, null, testResult);
			resourceManager.testConnection(resource, task, testResult);

		} catch (SchemaException ex) {
			throw new IllegalArgumentException(ex.getMessage(), ex);
		}
		testResult.computeStatus("Test resource has failed");

		LOGGER.trace("Finished testing {}, result: {} ", resource,
				testResult.getStatus());
		return testResult;
	}

	@Deprecated
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public List<PrismObject<? extends ShadowType>> listResourceObjects(String resourceOid,
																	   QName objectClass, ObjectPaging paging, Task task, OperationResult parentResult) throws SchemaException,
			ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {

		final OperationResult result = parentResult.createSubresult(ProvisioningService.class.getName()
				+ ".listResourceObjects");
		result.addParam("resourceOid", resourceOid);
		result.addParam("objectClass", objectClass);
		result.addArbitraryObjectAsParam("paging", paging);
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);

		if (resourceOid == null) {
			throw new IllegalArgumentException("Resource not defined in a search query");
		}
		if (objectClass == null) {
			throw new IllegalArgumentException("Objectclass not defined in a search query");
		}

		ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(resourceOid, objectClass, prismContext);

		final List<PrismObject<? extends ShadowType>> objectList = new ArrayList<>();
		final ResultHandler<ShadowType> shadowHandler = new ResultHandler<ShadowType>() {
			@Override
			public boolean handle(PrismObject<ShadowType> shadow, OperationResult objResult) {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("listResourceObjects: processing shadow: {}", SchemaDebugUtil.prettyPrint(shadow));
				}

				objectList.add(shadow);
				return true;
			}
		};

		try {

			getShadowCache(Mode.STANDARD).searchObjectsIterative(query, null, shadowHandler, false, task, result);

		} catch (SchemaException | ObjectNotFoundException | CommunicationException |
				ConfigurationException | SecurityViolationException | ExpressionEvaluationException | RuntimeException | Error  ex) {
			result.recordFatalError(ex.getMessage(), ex);
			result.cleanupResult(ex);
			throw ex;
		}
		result.cleanupResult();
		return objectList;
	}

	@Override
	public void refreshShadow(PrismObject<ShadowType> shadow, ProvisioningOperationOptions options, Task task, OperationResult parentResult)
			throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
			ObjectAlreadyExistsException, SecurityViolationException, ExpressionEvaluationException {
		Validate.notNull(shadow, "Shadow for refresh must not be null.");
		OperationResult result = parentResult.createSubresult(OPERATION_REFRESH_SHADOW);

		LOGGER.debug("Refreshing shadow {}", shadow);

		try {

			getShadowCache(Mode.RECON).refreshShadow(shadow, task, result);

			refreshShadowLegacy(shadow, options, task, result);

		} catch (GenericFrameworkException e) {
			ProvisioningUtil.recordFatalError(LOGGER, result, "Couldn't refresh shadow: " + e.getClass().getSimpleName() + ": "+ e.getMessage(), e);
			throw new CommunicationException(e.getMessage(), e);

		} catch (CommunicationException | SchemaException | ObjectNotFoundException | ConfigurationException
				| SecurityViolationException | ObjectAlreadyExistsException | ExpressionEvaluationException | RuntimeException | Error e) {
			ProvisioningUtil.recordFatalError(LOGGER, result, "Couldn't refresh shadow: " + e.getClass().getSimpleName() + ": "+ e.getMessage(), e);
			throw e;
			
		} catch (EncryptionException e) {
			ProvisioningUtil.recordFatalError(LOGGER, result, null, e);
			throw new SystemException(e.getMessage(), e);
		}

		result.computeStatus();
		result.cleanupResult();

		LOGGER.debug("Finished refreshing shadow {}: {}", shadow, result);
	}

	private  void refreshShadowLegacy(PrismObject<ShadowType> shadow, ProvisioningOperationOptions options, Task task, OperationResult result)
			throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
			ObjectAlreadyExistsException, SecurityViolationException, GenericFrameworkException, ExpressionEvaluationException, EncryptionException {

		ShadowType shadowType = shadow.asObjectable();

		if (shadowType.getFailedOperationType() == null) {
			return;
		} else if (FailedOperationTypeType.ADD == shadowType.getFailedOperationType()) {
			getShadowCache(Mode.RECON).addShadow(shadow, null, null, options, task, result);
		} else if (FailedOperationTypeType.MODIFY == shadowType.getFailedOperationType()) {
			getShadowCache(Mode.RECON).modifyShadow(shadow, new ArrayList<>(), null, options, task, result);
		} else if (FailedOperationTypeType.DELETE == shadowType.getFailedOperationType()) {
			getShadowCache(Mode.RECON).deleteShadow(shadow, options, null, task, result);
		} else {
			result.recordWarning("Missing or unknown type of operation to finish: " + shadowType.getFailedOperationType());
		}

	}

	// TODO: move to ResourceManager and ConnectorManager
	// the shadow-related code is already in the ShadowCache
	private <T extends ObjectType> boolean handleRepoObject(final Class<T> type, PrismObject<T> object,
			  final Collection<SelectorOptions<GetOperationOptions>> options,
			  final ResultHandler<T> handler, Task task, final OperationResult objResult) {

		PrismObject<T> completeObject;
		try {

			completeObject = completeObject(type, object, options, task, objResult);

		} catch (SchemaException | ObjectNotFoundException | CommunicationException | ConfigurationException | ExpressionEvaluationException e) {
			LOGGER.error("Error while completing {}: {}-{}. Using non-complete object.", object, e.getMessage(), e);
			objResult.recordFatalError(e);
			object.asObjectable().setFetchResult(objResult.createOperationResultType());
			completeObject = object;
		}

		objResult.computeStatus();
		objResult.recordSuccessIfUnknown();

		if (!objResult.isSuccess()) {
			OperationResultType resultType = objResult.createOperationResultType();
			completeObject.asObjectable().setFetchResult(resultType);
		}

		return handler.handle(completeObject, objResult);

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public <T extends ObjectType> SearchResultMetadata searchObjectsIterative(final Class<T> type, ObjectQuery query,
																			  final Collection<SelectorOptions<GetOperationOptions>> options,
																			  final ResultHandler<T> handler, Task task, final OperationResult parentResult) throws SchemaException,
				ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {

		Validate.notNull(parentResult, "Operation result must not be null.");
		Validate.notNull(handler, "Handler must not be null.");

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Start to search object. Query {}", query != null ? query.debugDump() : "(null)");
		}

		final OperationResult result = parentResult.createSubresult(ProvisioningService.class.getName()
				+ ".searchObjectsIterative");
        result.setSummarizeSuccesses(true);
        result.setSummarizeErrors(true);
        result.setSummarizePartialErrors(true);

		result.addParam("query", query);
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);

		ObjectFilter filter = null;
		if (query != null) {
			filter = ObjectQueryUtil.simplify(query.getFilter());
			query = query.cloneEmpty();
			query.setFilter(filter);
		}

		if (InternalsConfig.consistencyChecks && filter != null) {
			// We may not have all the definitions here. We will apply the definitions later
			filter.checkConsistence(false);
		}

		if (filter != null && filter instanceof NoneFilter) {
			result.recordSuccessIfUnknown();
			result.cleanupResult();
			LOGGER.trace("Finished searching. Nothing to do. Filter is NONE");
			SearchResultMetadata metadata = new SearchResultMetadata();
			metadata.setApproxNumberOfAllResults(0);
			return metadata;
		}

		final GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);

		if (ShadowType.class.isAssignableFrom(type)) {

			SearchResultMetadata metadata;
			try {

				metadata = getShadowCache(Mode.STANDARD).searchObjectsIterative(query, options, (ResultHandler<ShadowType>)handler, true, task, result);

				result.computeStatus();

			} catch (ConfigurationException | CommunicationException | ObjectNotFoundException | SchemaException | ExpressionEvaluationException | RuntimeException | Error e) {
				ProvisioningUtil.recordFatalError(LOGGER, result, null, e);
				throw e;
			} finally {
				result.cleanupResult();
			}

			return metadata;

		} else {

			ResultHandler<T> internalHandler = (object, objResult) -> handleRepoObject(type, object, options, handler, task, objResult);

			Collection<SelectorOptions<GetOperationOptions>> repoOptions = null;
			if (GetOperationOptions.isReadOnly(rootOptions)) {
				repoOptions = SelectorOptions.createCollection(GetOperationOptions.createReadOnly());
			}

			SearchResultMetadata metadata = null;
			try {

				metadata = getCacheRepositoryService().searchObjectsIterative(type, query, internalHandler, repoOptions, false, result);	// TODO think about strictSequential flag

				result.computeStatus();
				result.recordSuccessIfUnknown();

			} catch (SchemaException | RuntimeException | Error e) {
				ProvisioningUtil.recordFatalError(LOGGER, result, null, e);
			}

			result.cleanupResult();

			return metadata;
		}

	}

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
			ProvisioningUtil.recordFatalError(LOGGER, result, "Discovery failed: "+ex.getMessage(), ex);
			throw ex;
		}

		result.computeStatus("Connector discovery failed");
		result.cleanupResult();
		return discoverConnectors;
	}

	@Override
	public List<ConnectorOperationalStatus> getConnectorOperationalStatus(String resourceOid, Task task, OperationResult parentResult)
			throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException  {
		OperationResult result = parentResult.createMinorSubresult(ProvisioningService.class.getName()
				+ ".getConnectorOperationalStatus");
		result.addParam("resourceOid", resourceOid);
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);

		PrismObject<ResourceType> resource;
		try {

			resource = resourceManager.getResource(resourceOid, null, task, result);

		} catch (SchemaException | ObjectNotFoundException | ExpressionEvaluationException ex) {
			ProvisioningUtil.recordFatalError(LOGGER, result, ex.getMessage(), ex);
			throw ex;
		}

		List<ConnectorOperationalStatus> stats;
		try {
			stats = resourceManager.getConnectorOperationalStatus(resource, result);
		} catch (ObjectNotFoundException | SchemaException | CommunicationException | ConfigurationException ex) {
			ProvisioningUtil.recordFatalError(LOGGER, result, "Getting operations status from connector for resource "+resourceOid+" failed: "+ex.getMessage(), ex);
			throw ex;
		}

		result.computeStatus();
		result.cleanupResult();
		return stats;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends ObjectType> void applyDefinition(ObjectDelta<T> delta, Task task, OperationResult parentResult)
			throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {

			applyDefinition(delta, null, task, parentResult);
	}

    @SuppressWarnings("unchecked")
    @Override
    public <T extends ObjectType> void applyDefinition(ObjectDelta<T> delta, Objectable object, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {

    	OperationResult result = parentResult.createMinorSubresult(ProvisioningService.class.getName() + ".applyDefinition");
		result.addParam("delta", delta);
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);

		try {

	        if (ShadowType.class.isAssignableFrom(delta.getObjectTypeClass())){
	            getShadowCache(Mode.STANDARD).applyDefinition((ObjectDelta<ShadowType>) delta, (ShadowType) object, result);
	        } else if (ResourceType.class.isAssignableFrom(delta.getObjectTypeClass())){
	            resourceManager.applyDefinition((ObjectDelta<ResourceType>) delta, (ResourceType) object, null, task, result);
	        } else {
	            throw new IllegalArgumentException("Could not apply definition to deltas for object type: " + delta.getObjectTypeClass());
	        }

	        result.recordSuccessIfUnknown();
			result.cleanupResult();

		} catch (ObjectNotFoundException | CommunicationException | ConfigurationException | SchemaException | ExpressionEvaluationException | RuntimeException | Error e) {
			ProvisioningUtil.recordFatalError(LOGGER, result, null, e);
			throw e;
		}
    }

    @Override
    @SuppressWarnings("unchecked")
	public <T extends ObjectType> void applyDefinition(PrismObject<T> object, Task task, OperationResult parentResult)
			throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {

    	OperationResult result = parentResult.createMinorSubresult(ProvisioningService.class.getName() + ".applyDefinition");
		result.addParam(OperationResult.PARAM_OBJECT, object);
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);

		try {

			if (ShadowType.class.isAssignableFrom(object.getCompileTimeClass())){
				getShadowCache(Mode.STANDARD).applyDefinition((PrismObject<ShadowType>) object, result);
			} else if (ResourceType.class.isAssignableFrom(object.getCompileTimeClass())){
				resourceManager.applyDefinition((PrismObject<ResourceType>) object, task, result);
			} else {
				throw new IllegalArgumentException("Could not apply definition to object type: " + object.getCompileTimeClass());
			}

			result.computeStatus();
			result.recordSuccessIfUnknown();

		} catch (ObjectNotFoundException | CommunicationException | ConfigurationException | SchemaException | ExpressionEvaluationException | RuntimeException | Error e) {
			ProvisioningUtil.recordFatalError(LOGGER, result, null, e);
			throw e;
		} finally {
			result.cleanupResult();
		}
	}

    private void setProtectedShadow(PrismObject<ShadowType> shdaow, OperationResult result)
    		throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
    	getShadowCache(Mode.STANDARD).setProtectedShadow(shdaow, result);
    }

    @Override
    public <T extends ObjectType> void applyDefinition(Class<T> type, ObjectQuery query, Task task, OperationResult parentResult)
			throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {

    	OperationResult result = parentResult.createMinorSubresult(ProvisioningService.class.getName() + ".applyDefinition");
    	result.addParam(OperationResult.PARAM_TYPE, type);
		result.addParam(OperationResult.PARAM_QUERY, query);
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);

		try {

			if (ObjectQueryUtil.hasAllDefinitions(query)) {
				return;
			}

			if (ShadowType.class.isAssignableFrom(type)){
				getShadowCache(Mode.STANDARD).applyDefinition(query, result);
			} else if (ResourceType.class.isAssignableFrom(type)){
				resourceManager.applyDefinition(query, result);
			} else {
				throw new IllegalArgumentException("Could not apply definition to query for object type: " + type);
			}

			result.computeStatus();
			result.recordSuccessIfUnknown();

		} catch (ObjectNotFoundException | CommunicationException | ConfigurationException | SchemaException | ExpressionEvaluationException | RuntimeException | Error e) {
			ProvisioningUtil.recordFatalError(LOGGER, result, null, e);
			throw e;
		} finally {
			result.cleanupResult();
		}
	}

	@Override
	public void provisioningSelfTest(OperationResult parentTestResult, Task task) {
		CryptoUtil.securitySelfTest(parentTestResult);
		connectorManager.connectorFrameworkSelfTest(parentTestResult, task);
	}

    @Override
    public ProvisioningDiag getProvisioningDiag() {
        ProvisioningDiag provisioningDiag = new ProvisioningDiag();

        String frameworkVersion = connectorManager.getFrameworkVersion();
        if (frameworkVersion == null) {
            frameworkVersion = "unknown";
        }
        provisioningDiag.getAdditionalDetails().add(new LabeledString(DETAILS_CONNECTOR_FRAMEWORK_VERSION, frameworkVersion));
        return provisioningDiag;
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

	@PreDestroy
    public void shutdown() {
		connectorManager.shutdown();
	}

	@Override
	public ConstraintsCheckingResult checkConstraints(RefinedObjectClassDefinition shadowDefinition,
													  PrismObject<ShadowType> shadowObject,
													  ResourceType resourceType,
													  String shadowOid,
													  ResourceShadowDiscriminator resourceShadowDiscriminator,
													  ConstraintViolationConfirmer constraintViolationConfirmer,
													  Task task, OperationResult parentResult)
					  throws CommunicationException, ObjectAlreadyExistsException, SchemaException, SecurityViolationException, ConfigurationException, ObjectNotFoundException, ExpressionEvaluationException {
		OperationResult result = parentResult.createSubresult(ProvisioningService.class.getName() + ".checkConstraints");
		ConstraintsChecker checker = new ConstraintsChecker();
		checker.setRepositoryService(cacheRepositoryService);
		checker.setShadowCache(getShadowCache(Mode.STANDARD));
		checker.setPrismContext(prismContext);
		ProvisioningContext ctx = ctxFactory.create(shadowObject, task, parentResult);
		ctx.setObjectClassDefinition(shadowDefinition);
		ctx.setResource(resourceType);
		ctx.setShadowCoordinates(resourceShadowDiscriminator);
		checker.setProvisioningContext(ctx);
		checker.setShadowObject(shadowObject);
		checker.setShadowOid(shadowOid);
		checker.setConstraintViolationConfirmer(constraintViolationConfirmer);
		try {
			ConstraintsCheckingResult retval = checker.check(task, result);
			result.computeStatus();
			return retval;
		} catch (CommunicationException|ObjectAlreadyExistsException|SchemaException|SecurityViolationException|ConfigurationException|ObjectNotFoundException|RuntimeException e) {
			result.recordFatalError(e.getMessage(), e);
			throw e;
		}
	}

	@Override
	public void enterConstraintsCheckerCache() {
		ConstraintsChecker.enterCache();
	}

	@Override
	public void exitConstraintsCheckerCache() {
		ConstraintsChecker.exitCache();
	}

	private PrismObjectDefinition<ShadowType> getResourceObjectShadowDefinition() {
		if (resourceObjectShadowDefinition == null) {
			resourceObjectShadowDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(
					ShadowType.class);
		}
		return resourceObjectShadowDefinition;
	}

	private <T extends ObjectType> PrismObject<T> getRepoObject(Class<T> type, String oid, GetOperationOptions options, OperationResult result) throws ObjectNotFoundException, SchemaException{

		try {
			return getCacheRepositoryService().getObject(type, oid, null, result);
		} catch (ObjectNotFoundException e) {
			if (!GetOperationOptions.isAllowNotFound(options)){
				ProvisioningUtil.recordFatalError(LOGGER, result, "Can't get object with oid " + oid + ". Reason " + e.getMessage(), e);
			} else {
				result.muteLastSubresultError();
				result.computeStatus();
			}
			throw e;
		} catch (SchemaException ex) {
			ProvisioningUtil.recordFatalError(LOGGER, result, "Can't get object with oid " + oid + ". Reason " + ex.getMessage(), ex);
			throw ex;
		}
	}

	@Override
	public <O extends ObjectType, T> ItemComparisonResult compare(Class<O> type, String oid, ItemPath path,
			T expectedValue, Task task, OperationResult parentResult) 
				throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException, EncryptionException {
		Validate.notNull(oid, "Oid of object to get must not be null.");
		Validate.notNull(parentResult, "Operation result must not be null.");

		if (!ShadowType.class.isAssignableFrom(type)) {
			throw new UnsupportedOperationException("Only shadow compare is supported");
		}

		// Result type for this operation
		OperationResult result = parentResult.createMinorSubresult(ProvisioningService.class.getName() + ".compare");
		result.addParam(OperationResult.PARAM_OID, oid);
		result.addParam(OperationResult.PARAM_TYPE, type);

		ItemComparisonResult comparisonResult;
		try {

			PrismObject<O> repositoryObject = getRepoObject(type, oid, null, result);
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Retrieved repository object:\n{}", repositoryObject.debugDump());
			}

			comparisonResult = getShadowCache(Mode.STANDARD).compare((PrismObject<ShadowType>) (repositoryObject), path, expectedValue, task, result);

		} catch (ObjectNotFoundException | CommunicationException | SchemaException | ConfigurationException | SecurityViolationException | ExpressionEvaluationException | EncryptionException | RuntimeException | Error e) {
			ProvisioningUtil.recordFatalError(LOGGER, result, null, e);
			throw e;
		}


		result.computeStatus();
		result.cleanupResult();

		return comparisonResult;
	}

}
