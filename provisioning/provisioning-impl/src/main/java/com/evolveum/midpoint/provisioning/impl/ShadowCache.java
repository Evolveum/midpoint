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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.xml.namespace.QName;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import com.evolveum.midpoint.common.mapping.Mapping;
import com.evolveum.midpoint.common.refinery.ResourceShadowDiscriminator;
import com.evolveum.midpoint.common.refinery.ShadowDiscriminatorObjectDelta;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.api.ChangeNotificationDispatcher;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.api.ResourceOperationDescription;
import com.evolveum.midpoint.provisioning.api.ResourceOperationListener;
import com.evolveum.midpoint.provisioning.consistency.api.ErrorHandler;
import com.evolveum.midpoint.provisioning.consistency.api.ErrorHandler.FailedOperation;
import com.evolveum.midpoint.provisioning.consistency.impl.ErrorHandlerFactory;
import com.evolveum.midpoint.provisioning.ucf.api.ExecuteProvisioningScriptOperation;
import com.evolveum.midpoint.provisioning.ucf.api.ExecuteScriptArgument;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.ucf.api.Operation;
import com.evolveum.midpoint.provisioning.ucf.api.PropertyModificationOperation;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.ObjectOperationOption;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceObjectShadowUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
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
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AvailabilityStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.FailedOperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ProvisioningOperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ProvisioningScriptArgumentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ProvisioningScriptHostType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ProvisioningScriptType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ProvisioningScriptsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.prism.xml.ns._public.types_2.ObjectDeltaType;

public abstract class ShadowCache {
	
	@Autowired(required = true)
	@Qualifier("cacheRepositoryService")
	private RepositoryService repositoryService;
	@Autowired(required = true)
	private ErrorHandlerFactory errorHandlerFactory;
	@Autowired(required = true)
	private ResourceTypeManager resourceTypeManager;
	@Autowired(required = true)
	private PrismContext prismContext;
	@Autowired(required = true)
	private ShadowConverter shadowConverter;
	@Autowired(required = true)
	private ChangeNotificationDispatcher operationListener;

	private static final QName FAKE_SCRIPT_ARGUMENT_NAME = new QName(SchemaConstants.NS_C, "arg");
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
	
	public PrismContext getPrismContext() {
		return prismContext;
	}
	
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

		try {
			resultShadow = shadowConverter.getShadow(type, resource, repositoryShadow, parentResult);
			resourceTypeManager.modifyResourceAvailabilityStatus(resource, AvailabilityStatusType.UP, parentResult);
		} catch (Exception ex) {
			try {
				resultShadow = handleError(ex, repositoryShadow, FailedOperation.GET, resource, null, true,
						parentResult);
			} catch (GenericFrameworkException e) {
				throw new SystemException(e);
			} catch (ObjectAlreadyExistsException e) {
				throw new SystemException(e);
			}
		}

		parentResult.recordSuccess();
		return resultShadow;

	}

	public abstract String afterAddOnResource(ResourceObjectShadowType shadowType, ResourceType resource, OperationResult parentResult) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException;
	
	public String addShadow(ResourceObjectShadowType shadowType, ProvisioningScriptsType scripts,
			ResourceType resource, Task task, OperationResult parentResult) throws CommunicationException,
			GenericFrameworkException, ObjectAlreadyExistsException, SchemaException, ObjectNotFoundException,
			ConfigurationException, SecurityViolationException {
	Validate.notNull(shadowType, "Object to add must not be null.");

	if (LOGGER.isTraceEnabled()) {
		LOGGER.trace("Start adding shadow object:\n{}", shadowType.asPrismObject().dump());
		LOGGER.trace("Scripts: {}",
				SchemaDebugUtil.dumpJaxbObject(scripts, "scripts", shadowType.asPrismObject().getPrismContext()));
	}

	if (resource == null) {
		resource = getResource(shadowType, parentResult);
	}
	PrismContainer<?> attributesContainer = shadowType.asPrismObject().findContainer(
			ResourceObjectShadowType.F_ATTRIBUTES);
	if (attributesContainer == null || attributesContainer.isEmpty()) {
//		throw new SchemaException("Attempt to add shadow without any attributes: " + shadowType);
		handleError(new SchemaException("Attempt to add shadow without any attributes: " + shadowType), shadowType, FailedOperation.ADD, resource, null, true, parentResult);
	}

	Set<Operation> additionalOperations = new HashSet<Operation>();

	addExecuteScriptOperation(additionalOperations, ProvisioningOperationTypeType.ADD, scripts, resource,
			parentResult);

	
	try {
		shadowType = shadowConverter.addShadow(resource, shadowType, additionalOperations, parentResult);
		
	} catch (Exception ex) {
		shadowType = handleError(ex, shadowType, FailedOperation.ADD, resource, null, true, parentResult);
		return shadowType.getOid();
	}
	
		String oid = afterAddOnResource(shadowType, resource, parentResult);
		shadowType.setOid(oid);
		ObjectDelta delta = ObjectDelta.createAddDelta(shadowType.asPrismObject());
		ResourceOperationDescription operationDescription = createSuccessOperationDescription(shadowType, resource, delta, task, parentResult);
		operationListener.notifySuccess(operationDescription, task, parentResult);
		return oid;
	}

	private ResourceOperationDescription createSuccessOperationDescription(ResourceObjectShadowType shadowType, ResourceType resource, ObjectDelta delta, Task task, OperationResult parentResult) {
		ResourceOperationDescription operationDescription = new ResourceOperationDescription();
		operationDescription.setCurrentShadow(shadowType.asPrismObject());
		operationDescription.setResource(resource.asPrismObject());
		if (task != null){
		operationDescription.setSourceChannel(task.getChannel());
		}
		operationDescription.setObjectDelta(delta);
		operationDescription.setResult(parentResult);
		return operationDescription;
	}

	public abstract void afterModifyOnResource(ResourceObjectShadowType shadowType, Collection<? extends ItemDelta> modifications, OperationResult parentResult) throws SchemaException, ObjectNotFoundException;
	
	public abstract Collection<? extends ItemDelta> beforeModifyOnResource(ResourceObjectShadowType shadowType, ProvisioningOperationOptions options, Collection<? extends ItemDelta> modifications) throws SchemaException;
	
	public String modifyShadow(ResourceObjectShadowType objectType, ResourceType resource, String oid,
				Collection<? extends ItemDelta> modifications, ProvisioningScriptsType scripts, ProvisioningOperationOptions options, Task task, OperationResult parentResult)
				throws CommunicationException, GenericFrameworkException, ObjectNotFoundException, SchemaException,
				ConfigurationException, SecurityViolationException {

			Validate.notNull(objectType, "Object to modify must not be null.");
			Validate.notNull(oid, "OID must not be null.");
			Validate.notNull(modifications, "Object modification must not be null.");

			if (!(objectType instanceof ResourceObjectShadowType)) {
				throw new IllegalArgumentException("The object to modify is not a shadow, it is " + objectType);
			}

			ResourceObjectShadowType shadow = (ResourceObjectShadowType) objectType;

			if (resource == null) {
				resource = getResource(shadow, parentResult);

			}
			
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Modifying resource with oid {}, object:\n{}", resource.getOid(), shadow.asPrismObject()
						.dump());
			}

			Set<Operation> changes = new HashSet<Operation>();
			addExecuteScriptOperation(changes, ProvisioningOperationTypeType.MODIFY, scripts, resource, parentResult);
			LOGGER.trace("modifications before merging deltas, {}", DebugUtil.debugDump(modifications));
			
			modifications = beforeModifyOnResource(shadow, options, modifications);
			
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Applying change: {}", DebugUtil.debugDump(modifications));
			}
			
			Set<PropertyModificationOperation> sideEffectChanges = null;

			try {
				sideEffectChanges = shadowConverter.modifyShadow(resource, shadow, changes,
						modifications, parentResult);
			} catch (Exception ex) {
				LOGGER.debug("Provisioning exception: {}:{}, attempting to handle it",
						new Object[] { ex.getClass(), ex.getMessage(), ex });
				try {
					shadow = handleError(ex, shadow, FailedOperation.MODIFY, resource, modifications, true, parentResult);
					parentResult.computeStatus();
				} catch (ObjectAlreadyExistsException e) {
					parentResult.recordFatalError("While compensating communication problem for modify operation got: "
							+ ex.getMessage(), ex);
					throw new SystemException(e);
				}

				return shadow.getOid();
			}
			
			afterModifyOnResource(shadow, modifications, parentResult);

			
			if (!sideEffectChanges.isEmpty()) {
				// TODO: implement
				throw new UnsupportedOperationException("Handling of side-effect changes is not yet supported");
			}
			
			ObjectDelta delta = ObjectDelta.createModifyDelta(shadow.getOid(), modifications, shadow.asPrismObject().getCompileTimeClass(), prismContext);
			ResourceOperationDescription operationDescription = createSuccessOperationDescription(shadow, resource, delta, task, parentResult);
			operationListener.notifySuccess(operationDescription, task, parentResult);
			parentResult.recordSuccess();
			return oid;
		}


	public void deleteShadow(ObjectType objectType, ProvisioningOperationOptions options, ProvisioningScriptsType scripts,
			ResourceType resource, Task task, OperationResult parentResult) throws CommunicationException,
			GenericFrameworkException, ObjectNotFoundException, SchemaException, ConfigurationException,
			SecurityViolationException {

		Validate.notNull(objectType, "Object to delete must not be null.");
		Validate.notNull(parentResult, "Operation result must not be null.");

		if (objectType instanceof AccountShadowType) {

			AccountShadowType accountShadow = (AccountShadowType) objectType;

			if (resource == null) {
				try {
					resource = getResource(accountShadow, parentResult);
				} catch (ObjectNotFoundException ex) {
					// if the force option is set, delete shadow from the repo
					// although the resource does not exists..
					if (ProvisioningOperationOptions.isForce(options)) {
						parentResult.muteLastSubresultError();
						getRepositoryService().deleteObject(AccountShadowType.class, accountShadow.getOid(),
								parentResult);
						parentResult.recordHandledError("Resource defined in shadow does not exists. Shadow was deleted from the repository.");
						return;
					}
				}
			}

			LOGGER.trace("Deleting obeject {} from the resource {}.", ObjectTypeUtil.toShortString(objectType),
					ObjectTypeUtil.toShortString(resource));

			Set<Operation> additionalOperations = new HashSet<Operation>();

			addExecuteScriptOperation(additionalOperations, ProvisioningOperationTypeType.DELETE, scripts, resource,
					parentResult);

			if (accountShadow.getFailedOperationType() == null
					|| (accountShadow.getFailedOperationType() != null && FailedOperationTypeType.ADD != accountShadow
							.getFailedOperationType())) {
				try {
					shadowConverter.deleteShadow(resource, accountShadow, additionalOperations, parentResult);
				} catch (Exception ex) {
					try {
						handleError(ex, accountShadow, FailedOperation.DELETE, resource, null, true, parentResult);
					} catch (ObjectAlreadyExistsException e) {
						e.printStackTrace();
					}
					return;
				}
			}

			LOGGER.trace("Detele object with oid {} form repository.", accountShadow.getOid());
			try {
				getRepositoryService().deleteObject(AccountShadowType.class, accountShadow.getOid(), parentResult);
				ObjectDelta delta = ObjectDelta.createDeleteDelta(accountShadow.asPrismObject().getCompileTimeClass(), accountShadow.getOid(), prismContext);
				ResourceOperationDescription operationDescription = createSuccessOperationDescription(accountShadow, resource, delta, task, parentResult);
				operationListener.notifySuccess(operationDescription, task, parentResult);
			} catch (ObjectNotFoundException ex) {
				parentResult.recordFatalError("Can't delete object " + ObjectTypeUtil.toShortString(accountShadow)
						+ ". Reason: " + ex.getMessage(), ex);
				throw new ObjectNotFoundException("An error occured while deleting resource object " + accountShadow
						+ "whith identifiers " + ObjectTypeUtil.toShortString(accountShadow) + ": " + ex.getMessage(),
						ex);
			}
			LOGGER.trace("Object deleted from repository successfully.");
			parentResult.recordSuccess();
			resourceTypeManager.modifyResourceAvailabilityStatus(resource, AvailabilityStatusType.UP, parentResult);
		}
	}


	public <T extends ResourceObjectShadowType> void applyDefinition(ObjectDelta delta, OperationResult parentResult)
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

	
	protected ResourceType getResource(ResourceObjectShadowType shadowType, OperationResult parentResult)
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

		return resourceTypeManager.completeResource(resource.asObjectable(), null, parentResult);
	}
	
	@SuppressWarnings("rawtypes")
	protected <T extends ResourceObjectShadowType> T handleError(Exception ex, T shadow, FailedOperation op,
			ResourceType resource, Collection<? extends ItemDelta> modifications, boolean compensate,
			OperationResult parentResult) throws SchemaException, GenericFrameworkException, CommunicationException,
			ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException, SecurityViolationException {

		// do not set result in the shadow in case of get operation, it will
		// resilted to misleading information
		// by get operation we do not modify the result in the shadow, so only
		// fetch result in this case needs to be set
		if (FailedOperation.GET != op) {
			shadow = extendShadow(shadow, parentResult, resource, modifications);
		} else {
			shadow.setResource(resource);
		}
		ErrorHandler handler = errorHandlerFactory.createErrorHandler(ex);

		if (handler == null) {
			parentResult.recordFatalError("Error without a handler. Reason: " + ex.getMessage(), ex);
			throw new SystemException(ex.getMessage(), ex);
		}

		LOGGER.debug("Handling provisioning exception {}:{}", new Object[] { ex.getClass(), ex.getMessage() });
		LOGGER.trace("Handling provisioning exception {}:{}", new Object[] { ex.getClass(), ex.getMessage(), ex });

		return handler.handleError(shadow, op, ex, compensate, parentResult);

	}

	@SuppressWarnings("rawtypes")
	private <T extends ResourceObjectShadowType> T extendShadow(T shadow, OperationResult shadowResult,
			ResourceType resource, Collection<? extends ItemDelta> modifications) throws SchemaException {

		shadow.setResult(shadowResult.createOperationResultType());
		shadow.setResource(resource);

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
	private void addExecuteScriptOperation(Set<Operation> operations, ProvisioningOperationTypeType type,
			ProvisioningScriptsType scripts, ResourceType resource, OperationResult result) throws SchemaException {
		if (scripts == null) {
			// No warning needed, this is quite normal
			LOGGER.trace("Skipping creating script operation to execute. Scripts was not defined.");
			return;
		}

		PrismPropertyDefinition scriptArgumentDefinition = new PrismPropertyDefinition(FAKE_SCRIPT_ARGUMENT_NAME,
				FAKE_SCRIPT_ARGUMENT_NAME, DOMUtil.XSD_STRING, prismContext);
		for (ProvisioningScriptType script : scripts.getScript()) {
			for (ProvisioningOperationTypeType operationType : script.getOperation()) {
				if (type.equals(operationType)) {
					ExecuteProvisioningScriptOperation scriptOperation = new ExecuteProvisioningScriptOperation();

					for (ProvisioningScriptArgumentType argument : script.getArgument()) {
						ExecuteScriptArgument arg = new ExecuteScriptArgument(argument.getName(),
								Mapping.getPropertyStaticRealValues(argument, scriptArgumentDefinition,
										"script value for " + operationType + " in " + resource, prismContext));
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

}
