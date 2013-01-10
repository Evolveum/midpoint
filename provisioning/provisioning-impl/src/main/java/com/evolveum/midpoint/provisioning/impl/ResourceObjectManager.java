package com.evolveum.midpoint.provisioning.impl;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.provisioning.api.ChangeNotificationDispatcher;
import com.evolveum.midpoint.provisioning.api.ResourceOperationDescription;
import com.evolveum.midpoint.provisioning.ucf.api.Change;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.ucf.api.ResultHandler;
import com.evolveum.midpoint.provisioning.util.ShadowCacheUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceObjectShadowUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.FailedOperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;

@Component
public class ResourceObjectManager {
	
	@Autowired(required = true)
	@Qualifier("cacheRepositoryService")
	private RepositoryService repositoryService;
	@Autowired(required = true)
	private ShadowConverter shadowConverter;
	@Autowired(required = true)
	private PrismContext prismContext;
	@Autowired(required = true)
	private ChangeNotificationDispatcher changeNotificationDispatcher;
	@Autowired(required = true)
	private TaskManager taskManager;
	@Autowired(required = true)
	ResourceTypeManager resourceTypeManager;
	
	private static final Trace LOGGER = TraceManager.getTrace(ResourceObjectManager.class);
	
	public void setRepositoryService(RepositoryService repositoryService) {
		this.repositoryService = repositoryService;
	}
	
	public RepositoryService getRepositoryService() {
		return repositoryService;
	}

	public PrismProperty<?> fetchCurrentToken(ResourceType resourceType, OperationResult parentResult)
			throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException {

		Validate.notNull(resourceType, "Resource must not be null.");
		Validate.notNull(parentResult, "Operation result must not be null.");

		LOGGER.trace("Getting last token");
		PrismProperty<?> lastToken = null;
		try {
			ResourceSchema resourceSchema = resourceTypeManager.getResourceSchema(resourceType, parentResult);
			if (resourceSchema == null) {
				throw new ConfigurationException("No schema for "+resourceType);
			}
			lastToken = shadowConverter.fetchCurrentToken(resourceType, resourceSchema, parentResult);
		} catch (CommunicationException e) {
			parentResult.recordFatalError(e.getMessage(), e);
			throw e;
		} catch (ConfigurationException e) {
			parentResult.recordFatalError(e.getMessage(), e);
			throw e;
		}

		LOGGER.trace("Got last token: {}", SchemaDebugUtil.prettyPrint(lastToken));
		parentResult.recordSuccess();
		return lastToken;
	}

	@SuppressWarnings("unchecked")
	public List<Change> fetchChanges(ResourceType resourceType, PrismProperty<?> lastToken, OperationResult parentResult)
			throws ObjectNotFoundException, CommunicationException, GenericFrameworkException, SchemaException,
			ConfigurationException, SecurityViolationException {

		List<Change> changes = null;
		try {
			// changes = connector.fetchChanges(objectClass, lastToken,
			// parentResult);

			changes = shadowConverter.fetchChanges(resourceType, lastToken, parentResult);

			LOGGER.trace("Found {} change(s). Start processing it (them).", changes.size());

			for (Iterator<Change> i = changes.iterator(); i.hasNext();) {
				// search objects in repository
				Change change = i.next();

				ResourceObjectShadowType oldShadow = findOrCreateShadowFromChange(resourceType, change, parentResult);

				LOGGER.trace("Old shadow: {}", ObjectTypeUtil.toShortString(oldShadow));

				// skip setting other attribute when shadow is null
				if (oldShadow == null) {
					change.setOldShadow(null);
					continue;
				}

				change.setOldShadow(oldShadow.asPrismObject());

				// FIXME: hack. make sure that the current shadow has OID
				// and resource ref, also the account type should be set
				if (change.getCurrentShadow() != null) {
					ResourceObjectShadowType currentShadowType = change.getCurrentShadow().asObjectable();
					if (currentShadowType != null) {
						currentShadowType.setOid(oldShadow.getOid());
						currentShadowType.setResourceRef(oldShadow.getResourceRef());
						currentShadowType.setIntent(oldShadow.getIntent());
						if (currentShadowType instanceof AccountShadowType && oldShadow instanceof AccountShadowType) {
							((AccountShadowType) currentShadowType).setAccountType(((AccountShadowType) oldShadow)
									.getAccountType());
						}
					}
				}

				// FIXME: hack. the object delta must have oid specified.
				if (change.getObjectDelta() != null && change.getObjectDelta().getOid() == null) {
					if (oldShadow instanceof AccountShadowType) {
						ObjectDelta<AccountShadowType> objDelta = new ObjectDelta<AccountShadowType>(
								AccountShadowType.class, ChangeType.DELETE, prismContext);
						change.setObjectDelta(objDelta);
					}
					change.getObjectDelta().setOid(oldShadow.getOid());
				}

			}

		} catch (SchemaException ex) {
			parentResult.recordFatalError("Schema error: " + ex.getMessage(), ex);
			throw ex;
		} catch (CommunicationException ex) {
			parentResult.recordFatalError("Communication error: " + ex.getMessage(), ex);
			throw ex;
		} catch (GenericFrameworkException ex) {
			parentResult.recordFatalError("Generic error: " + ex.getMessage(), ex);
			throw ex;
		} catch (ConfigurationException ex) {
			parentResult.recordFatalError("Configuration error: " + ex.getMessage(), ex);
			throw ex;
		}
		parentResult.recordSuccess();
		return changes;
	}

	
	@SuppressWarnings("unchecked")
	public <T extends ResourceObjectShadowType> void listShadows(final ResourceType resource, final QName objectClass,
			final ShadowHandler<T> handler, final boolean readFromRepository, final OperationResult parentResult)
			throws CommunicationException, ObjectNotFoundException, SchemaException, ConfigurationException {

		Validate.notNull(objectClass);
		if (resource == null) {
			parentResult.recordFatalError("Resource must not be null");
			throw new IllegalArgumentException("Resource must not be null.");
		}

		searchObjects((Class<T>) ResourceObjectShadowType.class , objectClass, resource, null, handler, null,
				readFromRepository, parentResult);

	}


	public <T extends ResourceObjectShadowType> void searchObjectsIterative(final Class<T> type,
			final QName objectClass, final ResourceType resourceType,
			ObjectQuery query, final ShadowHandler<T> handler,
			final DiscoveryHandler discoveryHandler, final OperationResult parentResult)
			throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException {

		Validate.notNull(resourceType, "Resource must not be null.");
		Validate.notNull(objectClass, "Object class must not be null.");
		Validate.notNull(parentResult, "Operation result must not be null.");

		LOGGER.trace("Searching objects iterative with obejct class {}, resource: {}.", objectClass,
				ObjectTypeUtil.toShortString(resourceType));

		searchObjects(type, objectClass, resourceType, query, handler, discoveryHandler,
				true, parentResult);

	}

	private <T extends ResourceObjectShadowType> void searchObjects(final Class<T> type, QName objectClass,
			final ResourceType resourceType, ObjectQuery query,
			final ShadowHandler<T> handler, final DiscoveryHandler discoveryHandler,
			final boolean readFromRepository, final OperationResult parentResult) throws SchemaException,
			ObjectNotFoundException, CommunicationException, ConfigurationException {

		final ResourceSchema resourceSchema = resourceTypeManager.getResourceSchema(resourceType, parentResult);

		if (resourceSchema == null) {
			parentResult.recordFatalError("No schema for "+resourceType);
			throw new ConfigurationException("No schema for "+resourceType);
		}

		ResultHandler<T> resultHandler = new ResultHandler<T>() {

			@SuppressWarnings("unchecked")
			@Override
			public boolean handle(PrismObject<T> resourceShadow) {
				LOGGER.trace("Found resource object {}", SchemaDebugUtil.prettyPrint(resourceShadow));
				T resultShadowType;
				try {
					if (shadowConverter.isProtectedShadow(resourceType, resourceShadow)) {
						// Protected shadow. We will pretend that it does not exist.
						LOGGER.trace("Skipping protected shadow " + resourceShadow + " in search");
						return true;
					}
					T resourceShadowType = resourceShadow.asObjectable();
					// Try to find shadow that corresponds to the resource object
					if (readFromRepository) {
						resultShadowType = lookupShadowInRepository(type, resourceShadowType, resourceType,
								parentResult);

						ResourceObjectShadowType conflictedShadow = lookupShadowAccordingToName(type, resourceShadowType, resourceType, parentResult);
						if (resultShadowType == null) {
							LOGGER.trace(
									"Shadow object (in repo) corresponding to the resource object (on the resource) was not found. The repo shadow will be created. The resource object:\n{}",
									SchemaDebugUtil.prettyPrint(resourceShadow));

							
							// TODO: make sure that the resource object has appropriate definition (use objectClass and schema)
							// The resource object obviously exists on the resource, but appropriate shadow does not exist in the
							// repository we need to create the shadow to align repo state to the reality (resource)

							try {

								ResourceObjectShadowType repoShadow = ShadowCacheUtil.createRepositoryShadow(
										resourceShadowType, resourceType);
								String oid = getRepositoryService().addObject(repoShadow.asPrismObject(),
										parentResult);

								resultShadowType = ShadowCacheUtil.completeShadow(resourceShadowType, null,
										resourceType, parentResult);

								resultShadowType.setOid(oid);
								
							} catch (ObjectAlreadyExistsException e) {
								// This should not happen. We haven't supplied an OID so is should not conflict
								LOGGER.error("Unexpected repository behavior: Object already exists: {}", e.getMessage(), e);
								throw new SystemException("Unexpected repository behavior: Object already exists: "+e.getMessage(),e);
							}

							// And notify about the change we have discovered (if requested to do so)
							if (discoveryHandler != null) {
								discoveryHandler.discovered(resultShadowType, parentResult);
							}
						} else {
							LOGGER.trace("Found shadow object in the repository {}",
									SchemaDebugUtil.prettyPrint(resultShadowType));
						}
						if (conflictedShadow != null){
							Task task = taskManager.createTaskInstance();
							ResourceOperationDescription failureDescription = createResourceFailureDescription(conflictedShadow, resourceType, parentResult);
							changeNotificationDispatcher.notifyFailure(failureDescription, task, parentResult);
							deleteConflictedShadowFromRepo(conflictedShadow, parentResult);
							}
					} else {
						resultShadowType = ShadowCacheUtil.completeShadow(resourceShadowType, null,
								resourceType, parentResult);

					}

				} catch (SchemaException e) {
					// TODO: better error handling
					parentResult.recordFatalError("Schema error: " + e.getMessage(), e);
					LOGGER.error("Schema error: {}", e.getMessage(), e);
					return false;
				} catch (ConfigurationException e) {
					// TODO: better error handling
					parentResult.recordFatalError("Configuration error: " + e.getMessage(), e);
					LOGGER.error("Configuration error: {}", e.getMessage(), e);
					return false;
				}

				return handler.handle(resultShadowType);
			}

		};
		
		shadowConverter.searchObjects(resourceType, resourceSchema, objectClass, resultHandler, query, parentResult);
		
	}
	
	private void deleteConflictedShadowFromRepo(ResourceObjectShadowType shadow, OperationResult parentResult){
		
		try{
		repositoryService.deleteObject(AccountShadowType.class, shadow.getOid(), parentResult);
		} catch (Exception ex){
			throw new SystemException(ex.getMessage(), ex);
		}
		
	}
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private ResourceOperationDescription createResourceFailureDescription(ResourceObjectShadowType conflictedShadow, ResourceType resource, OperationResult parentResult){
		ResourceOperationDescription failureDesc = new ResourceOperationDescription();
		PrismObject<AccountShadowType> account = conflictedShadow.asPrismObject();
		failureDesc.setCurrentShadow(account);
		ObjectDelta objectDelta = null;
		if (FailedOperationTypeType.ADD == conflictedShadow.getFailedOperationType()) {
			objectDelta = ObjectDelta.createAddDelta(account);
		} 
//		else if (FailedOperationTypeType.MODIFY == conflictedShadow.getFailedOperationType()) {
//			ObjectDeltaType objDeltaType = conflictedShadow.getObjectChange();
//			if (objDeltaType != null) {
//				Collection<? extends ItemDelta> modifications = DeltaConvertor.toModifications(
//						objDeltaType.getModification(), account.getDefinition());
//				objectDelta = ObjectDelta.createModifyDelta(conflictedShadow.getOid(), modifications,
//						AccountShadowType.class, prismContext);
//
//			}
//		}
//		else if (FailedOperationTypeType.DELETE == conflictedShadow.getFailedOperationType()){
//			objectDelta = ObjectDelta.createDeleteDelta(AccountShadowType.class, conflictedShadow.getOid(), prismContext);
//		}
		failureDesc.setObjectDelta(objectDelta);
		failureDesc.setResource(resource.asPrismObject());
		failureDesc.setResult(parentResult);
		failureDesc.setSourceChannel(SchemaConstants.CHANGE_CHANNEL_DISCOVERY.getLocalPart());
		
		return failureDesc;
	}

	/**
	 * Locates the appropriate Shadow in repository that corresponds to the
	 * provided resource object.
	 * 
	 * @param parentResult
	 * 
	 * @return current unchanged shadow object that corresponds to provided
	 *         resource object or null if the object does not exist
	 * @throws SchemaException
	 * @throws ConfigurationException 
	 */
	@SuppressWarnings("unchecked")
	private <T extends ResourceObjectShadowType> T lookupShadowInRepository(Class<T> type, T resourceShadow,
			ResourceType resource, OperationResult parentResult) throws SchemaException, ConfigurationException {

		ObjectQuery query = ShadowCacheUtil.createSearchShadowQuery(resourceShadow, resource, prismContext,
				parentResult);
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Searching for shadow using filter:\n{}",
					query.dump());
		}
//		PagingType paging = new PagingType();

		// TODO: check for errors
		List<PrismObject<T>> results;

		results = getRepositoryService().searchObjects(type, query, parentResult);

		LOGGER.trace("lookupShadow found {} objects", results.size());

		if (results.size() == 0) {
			return null;
		}
		if (results.size() > 1) {
			for (PrismObject<T> result : results) {
				LOGGER.trace("Search result:\n{}", result.dump());
			}
			LOGGER.error("More than one shadows found for " + resourceShadow);
			// TODO: Better error handling later
			throw new IllegalStateException("More than one shadows found for " + resourceShadow);
		}

		T repoShadow = results.get(0).asObjectable();
		if (repoShadow != null) {
			shadowConverter.applyAttributesDefinition(repoShadow.asPrismObject(), resource);
		}
		return ShadowCacheUtil.completeShadow(resourceShadow, repoShadow, resource, parentResult);
	}

	@SuppressWarnings("unchecked")
	private <T extends ResourceObjectShadowType> T lookupShadowAccordingToName(Class<T> type, T resourceShadow,
			ResourceType resource, OperationResult parentResult) throws SchemaException, ConfigurationException {

		Collection<ResourceAttribute<?>> secondaryIdentifiers = ResourceObjectShadowUtil.getSecondaryIdentifiers(resourceShadow);
		ResourceAttribute<?> secondaryIdentifier = null;
		if (secondaryIdentifiers.size() < 1){
			LOGGER.trace("Shadow does not contain secondary idetifier. Skipping lookup shadows according to name.");
		}
		
		secondaryIdentifier = secondaryIdentifiers.iterator().next();
		LOGGER.trace("Shadow secondary identifier {}", secondaryIdentifier);
		
		AndFilter filter = AndFilter.createAnd(RefFilter.createReferenceEqual(AccountShadowType.class,
				AccountShadowType.F_RESOURCE_REF, prismContext, resource.getOid()), EqualsFilter.createEqual(
				new ItemPath(AccountShadowType.F_ATTRIBUTES), secondaryIdentifier.getDefinition(),
				secondaryIdentifier.getValue()));
//		ObjectQuery query = ShadowCacheUtil.createSearchShadowQuery(resourceShadow, resource, prismContext,
//				parentResult);
		ObjectQuery query = ObjectQuery.createObjectQuery(filter);
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Searching for shadow using filter on secondary identifier:\n{}",
					query.dump());
		}
//		PagingType paging = new PagingType();

		// TODO: check for errors
		List<PrismObject<T>> results;

		results = getRepositoryService().searchObjects(type, query, parentResult);

		LOGGER.trace("lookupShadow found {} objects", results.size());

		if (results.size() == 0) {
			return null;
		}
		if (results.size() > 1) {
			for (PrismObject<T> result : results) {
				LOGGER.trace("Search result:\n{}", result.dump());
			}
			LOGGER.error("More than one shadows found for " + resourceShadow);
			// TODO: Better error handling later
			throw new IllegalStateException("More than one shadows found for " + resourceShadow);
		}

		T repoShadow = results.get(0).asObjectable();
		if (repoShadow != null) {
			if (repoShadow.getFailedOperationType() == null){
				LOGGER.trace("Found shadow is ok, returning null");
				return null;
			} 
			if (repoShadow.getFailedOperationType() != null && FailedOperationTypeType.ADD != repoShadow.getFailedOperationType()){
				return null;
			}
			shadowConverter.applyAttributesDefinition(repoShadow.asPrismObject(), resource);
		}
		return ShadowCacheUtil.completeShadow(resourceShadow, repoShadow, resource, parentResult);
	}

	@SuppressWarnings("unchecked")
	private ResourceObjectShadowType findOrCreateShadowFromChange(ResourceType resource, Change change,
			OperationResult parentResult) throws SchemaException, ObjectNotFoundException, CommunicationException,
			GenericFrameworkException, ConfigurationException, SecurityViolationException {

		// Try to locate existing shadow in the repository
		List<PrismObject<AccountShadowType>> accountList = searchAccountByIdenifiers(change, resource, parentResult);

		if (accountList.size() > 1) {
			String message = "Found more than one account with the identifier " + change.getIdentifiers() + ".";
			LOGGER.error(message);
			parentResult.recordFatalError(message);
			throw new IllegalArgumentException(message);
		}

		ResourceObjectShadowType newShadow = null;

		if (accountList.isEmpty()) {
			// account was not found in the repository, create it now

			if (change.getObjectDelta() == null || !(change.getObjectDelta().getChangeType() == ChangeType.DELETE)) {
				try {
					ResourceSchema resourceSchema = resourceTypeManager.getResourceSchema(resource, parentResult);
					if (resourceSchema == null){
						
							parentResult.recordFatalError("No schema for "+resource);
							throw new ConfigurationException("No schema for "+resource);
						
					}
					newShadow = shadowConverter.createNewAccountFromChange(change, resource, resourceSchema, parentResult);
				} catch (ObjectNotFoundException ex) {
					throw ex;
				}

				try {
					String oid = getRepositoryService().addObject(newShadow.asPrismObject(), parentResult);
					newShadow.setOid(oid);
				} catch (ObjectAlreadyExistsException e) {
					parentResult.recordFatalError("Can't add account " + SchemaDebugUtil.prettyPrint(newShadow)
							+ " to the repository. Reason: " + e.getMessage(), e);
					throw new IllegalStateException(e.getMessage(), e);
				}
				LOGGER.trace("Created account shadow object: {}", ObjectTypeUtil.toShortString(newShadow));
			}

		} else {
			// Account was found in repository

			newShadow = accountList.get(0).asObjectable();
		}
		
		if (newShadow != null) {
			shadowConverter.applyAttributesDefinition(newShadow.asPrismObject(), resource);
		}

		return newShadow;
	}
	
	private List<PrismObject<AccountShadowType>> searchAccountByIdenifiers(Change change, ResourceType resource, OperationResult parentResult)
			throws SchemaException {

		ObjectQuery query = ShadowCacheUtil
				.createSearchShadowQuery(change.getIdentifiers(), resource, prismContext, parentResult);

		List<PrismObject<AccountShadowType>> accountList = null;
		try {
			accountList = getRepositoryService().searchObjects(AccountShadowType.class, query, parentResult);
		} catch (SchemaException ex) {
			parentResult.recordFatalError(
					"Failed to search account according to the identifiers: " + change.getIdentifiers() + ". Reason: "
							+ ex.getMessage(), ex);
			throw new SchemaException("Failed to search account according to the identifiers: "
					+ change.getIdentifiers() + ". Reason: " + ex.getMessage(), ex);
		}
		return accountList;
	}


}
