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
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.provisioning.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.mapping.Mapping;
import com.evolveum.midpoint.common.refinery.ResourceShadowDiscriminator;
import com.evolveum.midpoint.common.refinery.ShadowDiscriminatorObjectDelta;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.consistency.api.ErrorHandler;
import com.evolveum.midpoint.provisioning.consistency.api.ErrorHandler.FailedOperation;
import com.evolveum.midpoint.provisioning.consistency.impl.ErrorHandlerFactory;
import com.evolveum.midpoint.provisioning.ucf.api.Change;
import com.evolveum.midpoint.provisioning.ucf.api.ExecuteScriptArgument;
import com.evolveum.midpoint.provisioning.ucf.api.ExecuteProvisioningScriptOperation;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.ucf.api.Operation;
import com.evolveum.midpoint.provisioning.ucf.api.PropertyModificationOperation;
import com.evolveum.midpoint.provisioning.util.ShadowCacheUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainerDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceObjectShadowUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.util.DebugUtil;
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
import com.evolveum.midpoint.xml.ns._public.common.common_2.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AvailabilityStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.FailedOperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ProvisioningOperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ProtectedStringType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ProvisioningScriptArgumentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ProvisioningScriptHostType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ProvisioningScriptType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ProvisioningScriptsType;
import com.evolveum.prism.xml.ns._public.types_2.ObjectDeltaType;

/**
 * This class manages the "cache" of ResourceObjectShadows in the repository.
 * <p/>
 * In short, this class takes care of aligning the shadow objects in repository
 * with the real state of the resource.
 * <p/>
 * The repository content is considered a "cache" when it comes to Shadow
 * objects. That's why they are called "shadow" objects after all. When a new
 * state (values) of the resource object is detected, the shadow in the
 * repository should be updated. No matter if that was detected by
 * synchronization, reconciliation or an ordinary get from resource. This class
 * is supposed to do that.
 * <p/>
 * Therefore all operations that deal with "shadows" should pass through this
 * class. It forms yet another layer of the provisioning subsystem.
 * <p/>
 * Current implementation assumes we are only storing primary identifier in the
 * repository. That should be made configurable later. It also only support
 * Account objects now.
 * 
 * @author Radovan Semancik
 */
@Component
public class ShadowCache {

	@Autowired(required = true)
	@Qualifier("cacheRepositoryService")
	private RepositoryService repositoryService;
	@Autowired(required = true)
	private ShadowConverter shadowConverter;
	@Autowired(required = true)
	private PrismContext prismContext;
	@Autowired(required = true)
	private ErrorHandlerFactory errorHandlerFactory;
	// @Autowired(required = true)
	// private ResourceTypeManager resourceTypeManager;

	private static final Trace LOGGER = TraceManager.getTrace(ShadowCache.class);

	public ShadowCache() {
		repositoryService = null;
	}

	/**
	 * Get the value of repositoryService.
	 * 
	 * @return the value of repositoryService
	 */
	public RepositoryService getRepositoryService() {
		return repositoryService;
	}

	/**
	 * Set the value of repositoryService
	 * <p/>
	 * Expected to be injected.
	 * 
	 * @param repositoryService
	 *            new value of repositoryService
	 */
	public void setRepositoryService(RepositoryService repositoryService) {
		this.repositoryService = repositoryService;
	}

	/**
	 * Gets the shadow with specified OID
	 * <p/>
	 * The shadow will be read from the repository and missing information will
	 * be fetched from the resource.
	 * <p/>
	 * If no repositoryShadow is specified, the shadow will be retrieved from
	 * the repository. This is just an optimization if the object was already
	 * fetched (which is a usual case).
	 * <p/>
	 * This method is using identification by OID. This is intended for normal
	 * usage. Method that uses native identification will be provided later.
	 * 
	 * @param oid
	 *            OID of shadow to get.
	 * @param repositoryShadow
	 *            shadow that was read from the repository
	 * @return retrieved shadow (merged attributes from repository and resource)
	 * @throws ObjectNotFoundException
	 *             shadow was not found or object was not found on the resource
	 * @throws CommunicationException
	 *             problem communicating with the resource
	 * @throws SchemaException
	 *             problem processing schema or schema violation
	 * @throws ConfigurationException
	 */
	public <T extends ResourceObjectShadowType> T getShadow(Class<T> type, String oid, T repositoryShadow,
			OperationResult parentResult) throws ObjectNotFoundException, CommunicationException, SchemaException,
			ConfigurationException, SecurityViolationException {

		Validate.notNull(oid, "Object id must not be null.");

		LOGGER.trace("Start getting object with oid {}", oid);

		// We are using parent result directly, not creating subresult.
		// We want to hide the existence of shadow cache from the user.

		// Get the shadow from repository. There are identifiers that we need
		// for accessing the object by UCF.
		// Later, the repository object may have a fully cached object from.
		if (repositoryShadow == null) {
			PrismObject<T> repositoryPrism = getRepositoryService().getObject(type, oid, parentResult);
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Found shadow object:\n{}", repositoryPrism.dump());
			}
			repositoryShadow = repositoryPrism.asObjectable();
		}

		// Sanity check
		if (!oid.equals(repositoryShadow.getOid())) {
			parentResult.recordFatalError("Provided OID is not equal to OID of repository shadow");
			throw new IllegalArgumentException("Provided OID is not equal to OID of repository shadow");
		}

		ResourceType resource = getResource(repositoryShadow, parentResult);

		LOGGER.trace("Getting fresh object from ucf.");

		T resultShadow = null;
		// OperationResult fetchResult =
		// parentResult.createSubresult("Fetch object result.");

		try {
			resultShadow = shadowConverter.getShadow(type, resource, repositoryShadow, parentResult);
		} catch (Exception ex) {
			try {
				repositoryShadow = extendShadow(repositoryShadow, FailedOperationTypeType.GET, parentResult, resource, null);
				resultShadow = handleError(ex, repositoryShadow, FailedOperation.GET, parentResult);
			} catch (GenericFrameworkException e) {
				throw new SystemException(e);
			} catch (ObjectAlreadyExistsException e) {
				throw new SystemException(e);
			}
		}
			// TODO: Discovery
//		} catch (ObjectNotFoundException ex) {
//			parentResult.recordFatalError("Object " + ObjectTypeUtil.toShortString(repositoryShadow)
//					+ "not found on the " + ObjectTypeUtil.toShortString(resource), ex);
//
//			throw ex;
//		} catch (CommunicationException ex) {
//			parentResult.recordWarning("Cannot get " + ObjectTypeUtil.toShortString(repositoryShadow)
//					+ " from resource " + resource.getName()
//					+ ", because the resource is unreachable. The returned object is one from the repository.");
//			repositoryShadow.setFetchResult(parentResult.createOperationResultType());
//			return repositoryShadow;
//		} catch (ConfigurationException ex) {
//			parentResult.recordFatalError("Configuration error. Reason: " + ex.getMessage(), ex);
//			throw ex;
//		}
		parentResult.recordSuccess();
		return resultShadow;

	}

	public String addShadow(ResourceObjectShadowType shadow, boolean isReconciled, ProvisioningScriptsType scripts,
			ResourceType resource, OperationResult parentResult) throws CommunicationException,
			GenericFrameworkException, ObjectAlreadyExistsException, SchemaException, ObjectNotFoundException,
			ConfigurationException, SecurityViolationException {

		Validate.notNull(shadow, "Object to add must not be null.");

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Start adding shadow object:\n{}", shadow.asPrismObject().dump());
			LOGGER.trace("Scripts: {}",
					SchemaDebugUtil.dumpJaxbObject(scripts, "scripts", shadow.asPrismObject().getPrismContext()));
		}

		if (resource == null) {
			resource = getResource(shadow, parentResult);
		}

		Set<Operation> additionalOperations = new HashSet<Operation>();

		addExecuteScriptOperation(additionalOperations, ProvisioningOperationTypeType.ADD, scripts, parentResult);

//		OperationResult shadowConverterResult = parentResult.createSubresult(ShadowConverter.class.getName()
//				+ ".addShadow");

		try {
			shadow = shadowConverter.addShadow(resource, shadow, additionalOperations, isReconciled,
					parentResult);
			modifyResourceAvailabilityStatus(resource, AvailabilityStatusType.UP, parentResult);
		} catch (Exception ex) {
			shadow = extendShadow(shadow, FailedOperationTypeType.ADD, parentResult, resource, null);
			shadow = handleError(ex, shadow, FailedOperation.ADD, parentResult);
			return shadow.getOid();
		}

		if (shadow == null) {
			parentResult
					.recordFatalError("Error while creating account shadow object to save in the reposiotory. AccountShadow is null.");
			throw new IllegalStateException(
					"Error while creating account shadow object to save in the reposiotory. AccountShadow is null.");
		}

		LOGGER.trace("Adding object with identifiers to the repository.");

		LOGGER.trace("Reconciled shadow: {}", isReconciled);
		addOrReplaceShadowToRepository(shadow, isReconciled, parentResult.isError(), parentResult);

		LOGGER.trace("Object added to the repository successfully.");

		parentResult.recordSuccess();
		
		return shadow.getOid();

	}

	private void modifyResourceAvailabilityStatus(ResourceType resource, AvailabilityStatusType status, OperationResult result) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
		if (resource.getLastAvailabilityStatus() != null && status == resource.getLastAvailabilityStatus()){
			return;
		}
		List<PropertyDelta> modifications = new ArrayList<PropertyDelta>();
		PropertyDelta statusDelta = PropertyDelta.createModificationReplaceProperty(
				ResourceType.F_LAST_AVAILABILITY_STATUS, resource.asPrismObject().getDefinition(), status);
		modifications.add(statusDelta);
		resource.setLastAvailabilityStatus(status);
		repositoryService.modifyObject(ResourceType.class, resource.getOid(), modifications, result);
	}

	public void deleteShadow(ObjectType objectType, ProvisioningScriptsType scripts, ResourceType resource,
			OperationResult parentResult) throws CommunicationException, GenericFrameworkException,
			ObjectNotFoundException, SchemaException, ConfigurationException, SecurityViolationException {

		Validate.notNull(objectType, "Object to delete must not be null.");
		Validate.notNull(parentResult, "Operation result must not be null.");

		if (objectType instanceof AccountShadowType) {

			AccountShadowType accountShadow = (AccountShadowType) objectType;

			if (resource == null) {
				resource = getResource(accountShadow, parentResult);
			}

			LOGGER.trace("Deleting obeject {} from the resource {}.", ObjectTypeUtil.toShortString(objectType),
					ObjectTypeUtil.toShortString(resource));

			Set<Operation> additionalOperations = new HashSet<Operation>();

			addExecuteScriptOperation(additionalOperations, ProvisioningOperationTypeType.DELETE, scripts, parentResult);

			try {
				shadowConverter.deleteShadow(resource, accountShadow, additionalOperations, parentResult);
			} catch (Exception ex) {
				accountShadow = extendShadow(accountShadow, FailedOperationTypeType.DELETE, parentResult, resource,
						null);
				try {
					handleError(ex, accountShadow, FailedOperation.DELETE, parentResult);
				} catch (ObjectAlreadyExistsException e) {
					e.printStackTrace();
				}
				return;
			}

			LOGGER.trace("Detele object with oid {} form repository.", accountShadow.getOid());
			try {
				getRepositoryService().deleteObject(AccountShadowType.class, accountShadow.getOid(), parentResult);
			} catch (ObjectNotFoundException ex) {
				parentResult.recordFatalError("Can't delete object " + ObjectTypeUtil.toShortString(accountShadow)
						+ ". Reason: " + ex.getMessage(), ex);
				throw new ObjectNotFoundException("An error occured while deleting resource object " + accountShadow
						+ "whith identifiers " + ObjectTypeUtil.toShortString(accountShadow) + ": " + ex.getMessage(),
						ex);
			}
			LOGGER.trace("Object deleted from repository successfully.");
			parentResult.recordSuccess();
			try {
				modifyResourceAvailabilityStatus(resource, AvailabilityStatusType.UP, parentResult);
			} catch (ObjectAlreadyExistsException e) {
				throw new SystemException(e);
			}
		}
	}

	public void modifyShadow(ObjectType objectType, ResourceType resource, String oid,
			Collection<? extends ItemDelta> modifications, boolean isReconciled, ProvisioningScriptsType scripts,
			OperationResult parentResult) throws CommunicationException, GenericFrameworkException,
			ObjectNotFoundException, SchemaException, ConfigurationException, SecurityViolationException {

		Validate.notNull(objectType, "Object to modify must not be null.");
		Validate.notNull(oid, "OID must not be null.");
		Validate.notNull(modifications, "Object modification must not be null.");

		if (objectType instanceof ResourceObjectShadowType) {
			ResourceObjectShadowType shadow = (ResourceObjectShadowType) objectType;
			if (resource == null) {
				resource = getResource(shadow, parentResult);

			}

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Modifying resource with oid {}, object:\n{}", resource.getOid(), shadow.asPrismObject()
						.dump());
			}

			Set<Operation> changes = new HashSet<Operation>();
			addExecuteScriptOperation(changes, ProvisioningOperationTypeType.MODIFY, scripts, parentResult);

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Applying change: {}", DebugUtil.debugDump(modifications));
			}

			Set<PropertyModificationOperation> sideEffectChanges = null;

			try {
				sideEffectChanges = shadowConverter.modifyShadow(resource, shadow, changes, oid, modifications,
						parentResult);
				modifyResourceAvailabilityStatus(resource, AvailabilityStatusType.UP, parentResult);
			} catch (Exception ex) {

				shadow = extendShadow(shadow, FailedOperationTypeType.MODIFY, parentResult, resource, modifications);
				try {
					handleError(ex, shadow, FailedOperation.MODIFY, parentResult);
					
//					modifyResourceAvailabilityStatus(resource, AvailabilityStatusType.DOWN, parentResult);
				} catch (ObjectAlreadyExistsException e) {
					parentResult.recordFatalError("While compensating communication problem for modify operation got: "+ ex.getMessage(), ex);
					throw new SystemException(e);
				}
				parentResult.computeStatus();
				return;

			}

			Collection<? extends ItemDelta> shadowChanges = getShadowChanges(modifications);
			LOGGER.trace(
					"Detected shadow changes. Start to modify shadow in the repository, applying modifications {}",
					DebugUtil.debugDump(shadowChanges));
			if (shadowChanges != null && !shadowChanges.isEmpty()) {
				try {
					repositoryService.modifyObject(AccountShadowType.class, oid, shadowChanges, parentResult);
					LOGGER.trace("Shadow changes processed successfully.");
				} catch (ObjectAlreadyExistsException ex) {
					throw new SystemException(ex);
				}
			}

			if (isReconciled) {
				LOGGER.trace("Modified shadow is reconciled. Start to clean up account after successfull reconciliation.");
				try {
					addOrReplaceShadowToRepository(shadow, isReconciled, false, parentResult);
					LOGGER.trace("Shadow cleaned up successfully.");
				} catch (ObjectAlreadyExistsException ex) {
					// should be never thrown
				}
			}

			if (!sideEffectChanges.isEmpty()) {
				// TODO: implement
				throw new UnsupportedOperationException("Handling of side-effect changes is not yet supported");
			}
//			try {
//				
//			} catch (ObjectAlreadyExistsException e) {
//				throw new SystemException(e);
//			}
			parentResult.recordSuccess();
		}
	}

	private Collection<? extends ItemDelta> getShadowChanges(Collection<? extends ItemDelta> objectChange)
			throws SchemaException {

		Collection<ItemDelta> shadowChanges = new ArrayList<ItemDelta>();
		for (ItemDelta itemDelta : objectChange) {
			if (new PropertyPath(ResourceObjectShadowType.F_ATTRIBUTES).equals(itemDelta.getParentPath())
					|| SchemaConstants.PATH_PASSWORD.equals(itemDelta.getParentPath())
					|| SchemaConstants.PATH_ACTIVATION.equals(itemDelta.getParentPath())) {
				continue;
			} else {
				shadowChanges.add(itemDelta);
			}
		}
		return shadowChanges;
		// return repository changes;

	}

	public PrismProperty fetchCurrentToken(ResourceType resourceType, OperationResult parentResult)
			throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException {

		Validate.notNull(resourceType, "Resource must not be null.");
		Validate.notNull(parentResult, "Operation result must not be null.");

		LOGGER.trace("Getting last token");
		PrismProperty lastToken = null;
		try {
			lastToken = shadowConverter.fetchCurrentToken(resourceType, parentResult);
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

	public List<Change> fetchChanges(ResourceType resourceType, PrismProperty lastToken, OperationResult parentResult)
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

				ResourceObjectShadowType newShadow = findOrCreateShadowFromChange(resourceType, change, parentResult);

				LOGGER.trace("Old shadow: {}", ObjectTypeUtil.toShortString(newShadow));

				// skip setting other attribute when shadow is null
				if (newShadow == null) {
					change.setOldShadow(null);
					continue;
				}

				change.setOldShadow(newShadow.asPrismObject());

				// FIXME: hack. make sure that the current shadow has OID
				// and resource ref, also the account type should be set
				if (change.getCurrentShadow() != null) {
					ResourceObjectShadowType currentShadowType = change.getCurrentShadow().asObjectable();
					if (currentShadowType != null) {
						currentShadowType.setOid(newShadow.getOid());
						currentShadowType.setResourceRef(newShadow.getResourceRef());
						currentShadowType.setIntent(newShadow.getIntent());
						if (currentShadowType instanceof AccountShadowType && newShadow instanceof AccountShadowType) {
							((AccountShadowType) currentShadowType).setAccountType(((AccountShadowType) newShadow)
									.getAccountType());
						}
					}
				}

				// FIXME: hack. the object delta must have oid specified.
				if (change.getObjectDelta() != null && change.getObjectDelta().getOid() == null) {
					if (newShadow instanceof AccountShadowType) {
						ObjectDelta<AccountShadowType> objDelta = new ObjectDelta<AccountShadowType>(
								AccountShadowType.class, ChangeType.DELETE, prismContext);
						change.setObjectDelta(objDelta);
					}
					change.getObjectDelta().setOid(newShadow.getOid());
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

	private <T extends ResourceObjectShadowType> T extendShadow(T shadow, FailedOperationTypeType failedOperation,
			OperationResult shadowResult, ResourceType resource, Collection<? extends ItemDelta> modifications)
			throws SchemaException {
		
		shadow.setResult(shadowResult.createOperationResultType());
		shadow.setResource(resource);

		// if (shadow.getFailedOperationType() == null) {
		// shadow.setFailedOperationType(failedOperation);
		//
		// } else {
		// if (FailedOperationTypeType.ADD == shadow.getFailedOperationType()) {
		// // nothing to do
		// }
		// }

		if (modifications != null) {
			ObjectDelta<? extends ObjectType> objectDelta = ObjectDelta.createModifyDelta(shadow.getOid(),
					modifications, shadow.getClass(), prismContext);
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Storing delta to shadow:\n{}", objectDelta.dump());
			}
			ObjectDeltaType objectDeltaType = DeltaConvertor.toObjectDeltaType(objectDelta);

			shadow.setObjectChange(objectDeltaType);
		}
		return shadow;
	}

	private <T extends ResourceObjectShadowType> T handleError(Exception ex, T shadow, FailedOperation op,
			OperationResult parentResult) throws SchemaException, GenericFrameworkException, CommunicationException,
			ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException, SecurityViolationException {

		ErrorHandler handler = errorHandlerFactory.createErrorHandler(ex);

		if (handler == null) {
			parentResult.recordFatalError("Error without a handler. Reason: " + ex.getMessage(), ex);
			throw new SystemException(ex.getMessage(), ex);
		}

		return handler.handleError(shadow, op, ex, parentResult);

	}

	private void addExecuteScriptOperation(Set<Operation> operations, ProvisioningOperationTypeType type, ProvisioningScriptsType scripts,
			OperationResult result) throws SchemaException {
		if (scripts == null) {
			// No warning needed, this is quite normal
			// result.recordWarning("Skiping creating script operation to execute. Scripts was not defined.");
			LOGGER.trace("Skiping creating script operation to execute. Scripts was not defined.");
			return;
		}

		for (ProvisioningScriptType script : scripts.getScript()) {
			for (ProvisioningOperationTypeType operationType : script.getOperation()) {
				if (type.equals(operationType)) {
					ExecuteProvisioningScriptOperation scriptOperation = new ExecuteProvisioningScriptOperation();

					for (ProvisioningScriptArgumentType argument : script.getArgument()) {
						ExecuteScriptArgument arg = new ExecuteScriptArgument(argument.getName(),
								Mapping.getStaticValueList(argument));
						scriptOperation.getArgument().add(arg);
					}

					scriptOperation.setLanguage(script.getLanguage());
					scriptOperation.setTextCode(script.getCode());

					scriptOperation.setScriptOrder(script.getOrder());

					if (script.getHost().equals(ProvisioningScriptHostType.CONNECTOR)) {
						scriptOperation.setConnectorHost(true);
						scriptOperation.setResourceHost(false);
					}
					if (script.getHost().equals(ProvisioningScriptHostType.RESOURCE)) {
						scriptOperation.setConnectorHost(false);
						scriptOperation.setResourceHost(true);
					}
					LOGGER.trace("Created script operation: {}", SchemaDebugUtil.prettyPrint(scriptOperation));
					operations.add(scriptOperation);
				}
			}
		}
	}

	private ResourceObjectShadowType findOrCreateShadowFromChange(ResourceType resource, Change change,
			OperationResult parentResult) throws SchemaException, ObjectNotFoundException, CommunicationException,
			GenericFrameworkException, ConfigurationException, SecurityViolationException {

		// Try to locate existing shadow in the repository
		List<PrismObject<AccountShadowType>> accountList = searchAccountByIdenifiers(change, parentResult);

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
					newShadow = shadowConverter.createNewAccountFromChange(change, resource, parentResult);
				} catch (ObjectNotFoundException ex) {
					throw ex;
				}

				try {
					addOrReplaceShadowToRepository(newShadow, false, false, parentResult);
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
			// if the fetched change was one of the deletion type, delete
			// corresponding account from repo now
			// if (change.getObjectDelta() != null
			// && change.getObjectDelta().getChangeType() == ChangeType.DELETE)
			// {
			// try {
			// getRepositoryService().deleteObject(AccountShadowType.class,
			// newShadow.getOid(),
			// parentResult);
			// } catch (ObjectNotFoundException ex) {
			// parentResult.recordFatalError("Object with oid " +
			// newShadow.getOid()
			// + " not found in repo. Reason: " + ex.getMessage(), ex);
			// throw new ObjectNotFoundException("Object with oid " +
			// newShadow.getOid()
			// + " not found in repo. Reason: " + ex.getMessage(), ex);
			// }
			// }
		}

		return newShadow;
	}

	private List<PrismObject<AccountShadowType>> searchAccountByIdenifiers(Change change, OperationResult parentResult)
			throws SchemaException {

		ObjectQuery query = ShadowCacheUtil
				.createSearchShadowQuery(change.getIdentifiers(), prismContext, parentResult);

		List<PrismObject<AccountShadowType>> accountList = null;
		try {
			accountList = getRepositoryService().searchObjects(AccountShadowType.class, query, new PagingType(),
					parentResult);
		} catch (SchemaException ex) {
			parentResult.recordFatalError(
					"Failed to search account according to the identifiers: " + change.getIdentifiers() + ". Reason: "
							+ ex.getMessage(), ex);
			throw new SchemaException("Failed to search account according to the identifiers: "
					+ change.getIdentifiers() + ". Reason: " + ex.getMessage(), ex);
		}
		return accountList;
	}

	private ResourceType getResource(ResourceObjectShadowType shadowType, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {
		String resourceOid = ResourceObjectShadowUtil.getResourceOid(shadowType);
		if (resourceOid == null) {
			throw new SchemaException("Shadow " + shadowType + " does not have an resource OID");
		}
		return getResource(resourceOid, parentResult);
	}

	private ResourceType getResource(String resourceOid, OperationResult parentResult) throws ObjectNotFoundException,
			SchemaException, CommunicationException, ConfigurationException {

		// TODO: add some caching ?
		PrismObject<ResourceType> resource = getRepositoryService().getObject(ResourceType.class, resourceOid,
				parentResult);

		return shadowConverter.completeResource(resource.asObjectable(), parentResult);
	}

	private void addOrReplaceShadowToRepository(ResourceObjectShadowType shadow, boolean isReconciled, boolean error,
			OperationResult parentResult) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {

		// Store shadow in the repository
		if (isReconciled && !error) {
			PrismObject<AccountShadowType> oldShadow = shadow.asPrismObject().clone();
			ResourceObjectShadowUtil.getAttributesContainer(oldShadow).clear();
			ShadowCacheUtil.normalizeShadow(shadow, parentResult);

			ObjectDelta delta = oldShadow.diff(shadow.asPrismObject());
			LOGGER.trace("normalizing shadow: change description: {}", delta.dump());
			prismContext.adopt(shadow);
			repositoryService.modifyObject(AccountShadowType.class, shadow.getOid(), delta.getModifications(),
					parentResult);
		} else if (!isReconciled) {
			String oid = null;
			try {

				oid = getRepositoryService().addObject(shadow.asPrismObject(), parentResult);

			} catch (ObjectAlreadyExistsException ex) {
				// This should not happen. The OID is not supplied and it is
				// generated by the repo
				// If it happens, it must be a repo bug. Therefore it is safe to
				// convert to runtime exception
				parentResult.recordFatalError(
						"Can't add shadow object to the repository. Shadow object already exist. Reason: "
								+ ex.getMessage(), ex);
				throw new ObjectAlreadyExistsException(
						"Can't add shadow object to the repository. Shadow object already exist. Reason: "
								+ ex.getMessage(), ex);
			}
			shadow.setOid(oid);
		}
	}

	public <T extends ResourceObjectShadowType> void applyDefinition(ObjectDelta<T> delta, OperationResult parentResult)
			throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException {
		PrismObject<T> shadow = null;
		ResourceShadowDiscriminator discriminator = null;
		if (delta.isAdd()) {
			shadow = delta.getObjectToAdd();
		} else if (delta.isModify()) {
			if (delta instanceof ShadowDiscriminatorObjectDelta) {
				// This one does not have OID, it has to be specially processed
				discriminator = ((ShadowDiscriminatorObjectDelta) delta).getDiscriminator();
			} else {
				String shadowOid = delta.getOid();
				if (shadowOid == null) {
					throw new IllegalArgumentException("No OID in object delta " + delta);
				}
				shadow = repositoryService.getObject(delta.getObjectTypeClass(), shadowOid, parentResult);
			}
		} else {
			// Delete delta, nothing to do at all
			return;
		}
		if (shadow == null) {
			ResourceType resource = getResource(discriminator.getResourceOid(), parentResult);
			shadowConverter.applyAttributesDefinition(delta, discriminator, resource);
		} else {
			ResourceType resource = getResource(shadow.asObjectable(), parentResult);
			shadowConverter.applyAttributesDefinition(delta, shadow, resource);
		}
	}

	public <T extends ResourceObjectShadowType> void applyDefinition(PrismObject<T> shadow, OperationResult parentResult)
			throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException {
		ResourceType resource = getResource(shadow.asObjectable(), parentResult);
		shadowConverter.applyAttributesDefinition(shadow, resource);
	}

}
