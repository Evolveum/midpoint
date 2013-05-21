/*
 * Copyright (c) 2010-2013 Evolveum
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.identityconnectors.framework.impl.api.ConnectorFacadeFactoryImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import com.evolveum.midpoint.common.mapping.Mapping;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.ResourceShadowDiscriminator;
import com.evolveum.midpoint.common.refinery.ShadowDiscriminatorObjectDelta;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.Visitable;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.NaryLogicalFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.SubstringFilter;
import com.evolveum.midpoint.provisioning.api.ChangeNotificationDispatcher;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.api.ResourceOperationDescription;
import com.evolveum.midpoint.provisioning.api.ResourceOperationListener;
import com.evolveum.midpoint.provisioning.consistency.api.ErrorHandler;
import com.evolveum.midpoint.provisioning.consistency.api.ErrorHandler.FailedOperation;
import com.evolveum.midpoint.provisioning.consistency.impl.ErrorHandlerFactory;
import com.evolveum.midpoint.provisioning.ucf.api.Change;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.api.ExecuteProvisioningScriptOperation;
import com.evolveum.midpoint.provisioning.ucf.api.ExecuteScriptArgument;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.ucf.api.Operation;
import com.evolveum.midpoint.provisioning.ucf.api.PropertyModificationOperation;
import com.evolveum.midpoint.provisioning.ucf.api.ResultHandler;
import com.evolveum.midpoint.provisioning.ucf.impl.ConnectorFactoryIcfImpl;
import com.evolveum.midpoint.provisioning.ucf.impl.ConnectorInstanceIcfImpl;
import com.evolveum.midpoint.provisioning.util.ShadowCacheUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ObjectOperationOption;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainerDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.exception.TunnelException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AvailabilityStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ExpressionReturnMultiplicityType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.FailedOperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ProvisioningOperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ProvisioningScriptArgumentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ProvisioningScriptHostType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ProvisioningScriptType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ProvisioningScriptsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceObjectAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowAttributesType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowKindType;
import com.evolveum.prism.xml.ns._public.types_2.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;

/**
 * Shadow cache is a facade that covers all the operations with shadows.
 * It takes care of splitting the operations between repository and resource, merging the data back,
 * handling the errors and generally controlling the process.
 * 
 * The two principal classes that do the operations are:
 *   ResourceObjectConvertor: executes operations on resource
 *   ShadowManager: executes operations in the repository
 *   
 * Note: These three classes were refactored recently. There may still be some some
 * leftovers that needs to be cleaned up.
 * 
 * @author Radovan Semancik
 * @author Katarina Valalikova
 *
 */
public abstract class ShadowCache {
	
	@Autowired(required = true)
	@Qualifier("cacheRepositoryService")
	private RepositoryService repositoryService;
	@Autowired(required = true)
	private ErrorHandlerFactory errorHandlerFactory;
	@Autowired(required = true)
	private ResourceManager resourceTypeManager;
	@Autowired(required = true)
	private PrismContext prismContext;
	@Autowired(required = true)
	private ResourceObjectConverter resouceObjectConverter;
	@Autowired(required = true)
	protected ShadowManager shadowManager;
	@Autowired(required = true)
	private ConnectorManager connectorManager;
	@Autowired(required = true)
	private ChangeNotificationDispatcher operationListener;
	@Autowired(required = true)
	private TaskManager taskManager;
	@Autowired(required = true)
	private ChangeNotificationDispatcher changeNotificationDispatcher;

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
	
	public PrismContext getPrismContext() {
		return prismContext;
	}
	
	public PrismObject<ShadowType> getShadow(String oid, PrismObject<ShadowType> repositoryShadow, GetOperationOptions options,
			OperationResult parentResult) throws ObjectNotFoundException, CommunicationException, SchemaException,
			ConfigurationException, SecurityViolationException {

		Validate.notNull(oid, "Object id must not be null.");

		LOGGER.trace("Start getting object with oid {}", oid);

		// We are using parent result directly, not creating subresult.
		// We want to hide the existence of shadow cache from the user.

		// Get the shadow from repository. There are identifiers that we need
		// for accessing the object by UCF.
		// Later, the repository object may have a fully cached object from the resource.
		if (repositoryShadow == null) {
			repositoryShadow = repositoryService.getObject(ShadowType.class, oid, parentResult);
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Got repository shadow object:\n{}", repositoryShadow.dump());
			}
		}

		// Sanity check
		if (!oid.equals(repositoryShadow.getOid())) {
			parentResult.recordFatalError("Provided OID is not equal to OID of repository shadow");
			throw new IllegalArgumentException("Provided OID is not equal to OID of repository shadow");
		}

		ResourceType resource = null;
		try{
			resource = getResource(repositoryShadow, parentResult);
		} catch(ObjectNotFoundException ex){
			parentResult.recordFatalError("Resource defined in shadow was not found: " + ex.getMessage(), ex);
			return repositoryShadow;
		}
		LOGGER.trace("Getting fresh object from ucf.");

		PrismObject<ShadowType> resourceShadow = null;
		ConnectorInstance connector = getConnectorInstance(resource, parentResult);
		RefinedObjectClassDefinition objectClassDefinition = applyAttributesDefinition(repositoryShadow, resource);

		try {			
			
			// Let's get all the identifiers from the Shadow <attributes> part
			Collection<? extends ResourceAttribute<?>> identifiers = ShadowUtil.getIdentifiers(repositoryShadow);
			
			if (identifiers == null || identifiers.isEmpty()) {
				//check if the account is not only partially created (exist only in repo so far)
				if (repositoryShadow.asObjectable().getFailedOperationType() != null) {
					throw new GenericConnectorException(
							"Unable to get account from the resource. Probably it has not been created yet because of previous unavailability of the resource.");
				}
				// No identifiers found
				SchemaException ex = new SchemaException("No identifiers found in the respository shadow "
						+ repositoryShadow + " with respect to " + resource);
				parentResult.recordFatalError("No identifiers found in the respository shadow "+ repositoryShadow, ex);
				throw ex;
			}
	
			//try to apply changes to the account only if the resource if UP
			if (repositoryShadow.asObjectable().getObjectChange() != null && repositoryShadow.asObjectable().getFailedOperationType() != null
					&& resource.getOperationalState() != null
					&& resource.getOperationalState().getLastAvailabilityStatus() == AvailabilityStatusType.UP) {
				throw new GenericConnectorException(
						"Found changes that have been not applied to the account yet. Trying to apply them now.");
			}
			
			resourceShadow = resouceObjectConverter.getResourceObject(connector, resource, identifiers, objectClassDefinition, parentResult);
			resourceTypeManager.modifyResourceAvailabilityStatus(resource.asPrismObject(), AvailabilityStatusType.UP, parentResult);
		} catch (Exception ex) {
			try {
				boolean compensate = GetOperationOptions.isDoNotDiscovery(options)? false : true;
				resourceShadow = handleError(ex, repositoryShadow, FailedOperation.GET, resource, null, compensate,
						null, parentResult);
			} catch (GenericFrameworkException e) {
				throw new SystemException(e);
			} catch (ObjectAlreadyExistsException e) {
				throw new SystemException(e);
			}
		}
		
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Shadow from repository:\n{}", repositoryShadow.dump());
			LOGGER.trace("Resource object fetched from resource:\n{}", resourceShadow.dump());
		}
		
		// Complete the shadow by adding attributes from the resource object
		PrismObject<ShadowType> resultShadow = completeShadow(connector, resourceShadow, repositoryShadow, resource, objectClassDefinition, parentResult);

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Shadow when assembled:\n{}", resultShadow.dump());
		}
		
		parentResult.recordSuccess();
		return resultShadow;

	}

	public abstract String afterAddOnResource(PrismObject<ShadowType> shadow, ResourceType resource, 
			RefinedObjectClassDefinition objectClassDefinition, OperationResult parentResult)
					throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException;
	
	public String addShadow(PrismObject<ShadowType> shadow, ProvisioningScriptsType scripts,
			ResourceType resource, ProvisioningOperationOptions options, Task task, OperationResult parentResult) throws CommunicationException,
			GenericFrameworkException, ObjectAlreadyExistsException, SchemaException, ObjectNotFoundException,
			ConfigurationException, SecurityViolationException {
		Validate.notNull(shadow, "Object to add must not be null.");

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Start adding shadow object:\n{}", shadow.dump());
			LOGGER.trace("Scripts: {}",
					SchemaDebugUtil.dumpJaxbObject(scripts, "scripts", shadow.getPrismContext()));
		}
	
		if (resource == null) {
			resource = getResource(shadow, parentResult);
		}
		
		PrismContainer<?> attributesContainer = shadow.findContainer(
				ShadowType.F_ATTRIBUTES);
		if (attributesContainer == null || attributesContainer.isEmpty()) {
	//		throw new SchemaException("Attempt to add shadow without any attributes: " + shadowType);
				handleError(new SchemaException("Attempt to add shadow without any attributes: " + shadow), shadow,
						FailedOperation.ADD, resource, null, true, task, parentResult);
		}
		
		preprocessEntitlements(shadow, resource, parentResult);
		
		RefinedObjectClassDefinition objectClassDefinition;
		try {
			objectClassDefinition = determineObjectClassDefinition(shadow, resource);
			applyAttributesDefinition(shadow, resource);
			ConnectorInstance connector = getConnectorInstance(resource, parentResult);
			shadow = resouceObjectConverter.addResourceObject(connector, resource, shadow, objectClassDefinition, scripts, parentResult);
			
		} catch (Exception ex) {
			shadow = handleError(ex, shadow, FailedOperation.ADD, resource, null, ProvisioningOperationOptions.isCompletePostponed(options), task, parentResult);
			return shadow.getOid();
		}
	
		// This is where the repo shadow is created (if needed) 
		String oid = afterAddOnResource(shadow, resource, objectClassDefinition, parentResult);
		shadow.setOid(oid);
		
		ObjectDelta<ShadowType> delta = ObjectDelta.createAddDelta(shadow);
		ResourceOperationDescription operationDescription = createSuccessOperationDescription(shadow, resource, delta, task, parentResult);
		operationListener.notifySuccess(operationDescription, task, parentResult);
		return oid;
	}

	private ResourceOperationDescription createSuccessOperationDescription(PrismObject<ShadowType> shadowType, ResourceType resource, ObjectDelta delta, Task task, OperationResult parentResult) {
		ResourceOperationDescription operationDescription = new ResourceOperationDescription();
		operationDescription.setCurrentShadow(shadowType);
		operationDescription.setResource(resource.asPrismObject());
		if (task != null){
		operationDescription.setSourceChannel(task.getChannel());
		}
		operationDescription.setObjectDelta(delta);
		operationDescription.setResult(parentResult);
		return operationDescription;
	}

	public abstract void afterModifyOnResource(PrismObject<ShadowType> shadowType, Collection<? extends ItemDelta> modifications, OperationResult parentResult) throws SchemaException, ObjectNotFoundException;
	
	public abstract Collection<? extends ItemDelta> beforeModifyOnResource(PrismObject<ShadowType> shadow, ProvisioningOperationOptions options, Collection<? extends ItemDelta> modifications) throws SchemaException;
	
	public String modifyShadow(PrismObject<ShadowType> shadow, ResourceType resource, String oid,
				Collection<? extends ItemDelta> modifications, ProvisioningScriptsType scripts, ProvisioningOperationOptions options, Task task, OperationResult parentResult)
				throws CommunicationException, GenericFrameworkException, ObjectNotFoundException, SchemaException,
				ConfigurationException, SecurityViolationException {

		Validate.notNull(shadow, "Object to modify must not be null.");
		Validate.notNull(oid, "OID must not be null.");
		Validate.notNull(modifications, "Object modification must not be null.");

		if (resource == null) {
			resource = getResource(shadow, parentResult);
		}

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Modifying resource with oid {}, object:\n{}", resource.getOid(), shadow.dump());
		}
		
		RefinedObjectClassDefinition objectClassDefinition =  applyAttributesDefinition(shadow, resource);

		preprocessEntitlements(modifications, resource, parentResult);

		modifications = beforeModifyOnResource(shadow, options, modifications);

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Applying change: {}", DebugUtil.debugDump(modifications));
		}

		Collection<PropertyModificationOperation> sideEffectChanges = null;

		ConnectorInstance connector = getConnectorInstance(resource, parentResult);
		
		try {
			sideEffectChanges = resouceObjectConverter.modifyResourceObject(connector, resource, objectClassDefinition, shadow, scripts, modifications,
					parentResult);
		} catch (Exception ex) {
			LOGGER.debug("Provisioning exception: {}:{}, attempting to handle it",
					new Object[] { ex.getClass(), ex.getMessage(), ex });
			try {
				shadow = handleError(ex, shadow, FailedOperation.MODIFY, resource, modifications,
						ProvisioningOperationOptions.isCompletePostponed(options), task, parentResult);
				parentResult.computeStatus();
			} catch (ObjectAlreadyExistsException e) {
				parentResult.recordFatalError(
						"While compensating communication problem for modify operation got: "
								+ ex.getMessage(), ex);
				throw new SystemException(e);
			}

			return shadow.getOid();
		}

		afterModifyOnResource(shadow, modifications, parentResult);

		PropertyDelta<?> renameDelta = checkShadowName(modifications, shadow);

		// if (!sideEffectChanges.isEmpty()) {
		Collection<? extends ItemDelta> sideEffectDelta = convertToPropertyDelta(sideEffectChanges);
		if (renameDelta != null) {
			((Collection) sideEffectDelta).add(renameDelta);
		}
		if (!sideEffectDelta.isEmpty()) {
			try {

				repositoryService.modifyObject(shadow.getCompileTimeClass(), oid, sideEffectDelta, parentResult);
			} catch (ObjectAlreadyExistsException ex) {
				parentResult.recordFatalError("Side effect changes could not be applied", ex);
				LOGGER.error("Side effect changes could not be applied. " + ex.getMessage(), ex);
				throw new SystemException("Side effect changes could not be applied. " + ex.getMessage(), ex);
			}
		}

		ObjectDelta<ShadowType> delta = ObjectDelta.createModifyDelta(shadow.getOid(), modifications, shadow.getCompileTimeClass(), prismContext);
		ResourceOperationDescription operationDescription = createSuccessOperationDescription(shadow,
				resource, delta, task, parentResult);
		operationListener.notifySuccess(operationDescription, task, parentResult);
		parentResult.recordSuccess();
		return oid;
	}

	private PropertyDelta<?> checkShadowName(Collection<? extends ItemDelta> modifications, 
			PrismObject<ShadowType> shadow) throws SchemaException {
		ItemDelta<?> nameDelta = ItemDelta.findItemDelta(modifications, new ItemPath(ShadowType.F_ATTRIBUTES, ConnectorFactoryIcfImpl.ICFS_NAME), ItemDelta.class); 
		String newName = null;//ShadowCacheUtil.determineShadowName(shadow);
		
		if (nameDelta == null){
			return null;
		}
		
		if (nameDelta.isReplace()){
			Item name = nameDelta.getItemNew();
			newName = (String) ((PrismPropertyValue) name.getValue(0)).getValue();
		}
		
		if (newName.equals(shadow.asObjectable().getName().getOrig())){
			return null;
		}
		 
		PropertyDelta<?> renameDelta = PropertyDelta.createModificationReplaceProperty(ShadowType.F_NAME, shadow.getDefinition(), new PolyStringType(newName));
		return renameDelta;
	}

	private Collection<? extends ItemDelta> convertToPropertyDelta(
			Collection<PropertyModificationOperation> sideEffectChanges) {
		Collection<PropertyDelta> sideEffectDelta = new ArrayList<PropertyDelta>();
		if (sideEffectChanges != null) {
			for (PropertyModificationOperation mod : sideEffectChanges){
				sideEffectDelta.add(mod.getPropertyDelta());
			}
		}
		
		return sideEffectDelta;
	}

	public void deleteShadow(PrismObject<ShadowType> shadow, ProvisioningOperationOptions options, ProvisioningScriptsType scripts,
			ResourceType resource, Task task, OperationResult parentResult) throws CommunicationException,
			GenericFrameworkException, ObjectNotFoundException, SchemaException, ConfigurationException,
			SecurityViolationException {

		Validate.notNull(shadow, "Object to delete must not be null.");
		Validate.notNull(parentResult, "Operation result must not be null.");

		if (resource == null) {
			try {
				resource = getResource(shadow, parentResult);
			} catch (ObjectNotFoundException ex) {
				// if the force option is set, delete shadow from the repo
				// although the resource does not exists..
				if (ProvisioningOperationOptions.isForce(options)) {
					parentResult.muteLastSubresultError();
					getRepositoryService().deleteObject(ShadowType.class, shadow.getOid(),
							parentResult);
					parentResult.recordHandledError("Resource defined in shadow does not exists. Shadow was deleted from the repository.");
					return;
				}
			}

			RefinedObjectClassDefinition objectClassDefinition =  applyAttributesDefinition(shadow, resource);
			
			ConnectorInstance connector = getConnectorInstance(resource, parentResult);
			
			LOGGER.trace("Deleting obeject {} from the resource {}.", shadow, resource);

			if (shadow.asObjectable().getFailedOperationType() == null
					|| (shadow.asObjectable().getFailedOperationType() != null 
						&& FailedOperationTypeType.ADD != shadow.asObjectable().getFailedOperationType())) {
				try {
					resouceObjectConverter.deleteResourceObject(connector, resource, shadow, objectClassDefinition, scripts, parentResult);
				} catch (Exception ex) {
					try {
						handleError(ex, shadow, FailedOperation.DELETE, resource, null, ProvisioningOperationOptions.isCompletePostponed(options), task, parentResult);
					} catch (ObjectAlreadyExistsException e) {
						e.printStackTrace();
					}
					return;
				}
			}

			LOGGER.trace("Detele object with oid {} form repository.", shadow.getOid());
			try {
				getRepositoryService().deleteObject(ShadowType.class, shadow.getOid(), parentResult);
				ObjectDelta<ShadowType> delta = ObjectDelta.createDeleteDelta(shadow.getCompileTimeClass(), shadow.getOid(), prismContext);
				ResourceOperationDescription operationDescription = createSuccessOperationDescription(shadow, resource, delta, task, parentResult);
				operationListener.notifySuccess(operationDescription, task, parentResult);
			} catch (ObjectNotFoundException ex) {
				parentResult.recordFatalError("Can't delete object " + shadow + ". Reason: " + ex.getMessage(), ex);
				throw new ObjectNotFoundException("An error occured while deleting resource object " + shadow
						+ "whith identifiers " + shadow + ": " + ex.getMessage(),
						ex);
			}
			LOGGER.trace("Object deleted from repository successfully.");
			parentResult.recordSuccess();
			resourceTypeManager.modifyResourceAvailabilityStatus(resource.asPrismObject(), AvailabilityStatusType.UP, parentResult);
		}
	}


	public void applyDefinition(ObjectDelta<ShadowType> delta, ShadowType shadowTypeWhenNoOid, OperationResult parentResult)
			throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException {
		PrismObject<ShadowType> shadow = null;
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
                    if (shadowTypeWhenNoOid == null) {
					    throw new IllegalArgumentException("No OID in object delta " + delta + " and no externally-supplied shadow is present as well.");
				    }
                    shadow = shadowTypeWhenNoOid.asPrismObject();
                } else {
				    shadow = repositoryService.getObject(delta.getObjectTypeClass(), shadowOid, parentResult);
                }
			}
		} else {
			// Delete delta, nothing to do at all
			return;
		}
		if (shadow == null) {
			ResourceType resource = resourceTypeManager.getResource(discriminator.getResourceOid(), parentResult).asObjectable();
			applyAttributesDefinition(delta, discriminator, resource);
		} else {
			ResourceType resource = getResource(shadow, parentResult);
			applyAttributesDefinition(delta, shadow, resource);
		}
	}

	public void applyDefinition(PrismObject<ShadowType> shadow, OperationResult parentResult)
			throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException {
		ResourceType resource = getResource(shadow, parentResult);
		applyAttributesDefinition(shadow, resource);
	}

	
	protected ResourceType getResource(PrismObject<ShadowType> shadow, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {
		String resourceOid = ShadowUtil.getResourceOid(shadow.asObjectable());
		if (resourceOid == null) {
			throw new SchemaException("Shadow " + shadow + " does not have an resource OID");
		}
		return resourceTypeManager.getResource(resourceOid, parentResult).asObjectable();
	}

	@SuppressWarnings("rawtypes")
	protected PrismObject<ShadowType> handleError(Exception ex, PrismObject<ShadowType> shadow, FailedOperation op,
			ResourceType resource, Collection<? extends ItemDelta> modifications, boolean compensate, Task task, 
			OperationResult parentResult) throws SchemaException, GenericFrameworkException, CommunicationException,
			ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException, SecurityViolationException {

		// do not set result in the shadow in case of get operation, it will
		// resilted to misleading information
		// by get operation we do not modify the result in the shadow, so only
		// fetch result in this case needs to be set
		if (FailedOperation.GET != op) {
			shadow = extendShadow(shadow, parentResult, resource, modifications);
		} else {
			shadow.asObjectable().setResource(resource);
		}
		ErrorHandler handler = errorHandlerFactory.createErrorHandler(ex);

		if (handler == null) {
			parentResult.recordFatalError("Error without a handler. Reason: " + ex.getMessage(), ex);
			throw new SystemException(ex.getMessage(), ex);
		}

		LOGGER.debug("Handling provisioning exception {}:{}", new Object[] { ex.getClass(), ex.getMessage() });
		LOGGER.trace("Handling provisioning exception {}:{}", new Object[] { ex.getClass(), ex.getMessage(), ex });

		return handler.handleError(shadow.asObjectable(), op, ex, compensate, task, parentResult).asPrismObject();

	}

	private PrismObject<ShadowType> extendShadow(PrismObject<ShadowType> shadow, OperationResult shadowResult,
			ResourceType resource, Collection<? extends ItemDelta> modifications) throws SchemaException {

		ShadowType shadowType = shadow.asObjectable();
		shadowType.setResult(shadowResult.createOperationResultType());
		shadowType.setResource(resource);

		if (modifications != null) {
			ObjectDelta<? extends ObjectType> objectDelta = ObjectDelta.createModifyDelta(shadow.getOid(),
					modifications, shadowType.getClass(), prismContext);
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Storing delta to shadow:\n{}", objectDelta.dump());
			}
			ObjectDeltaType objectDeltaType = DeltaConvertor.toObjectDeltaType(objectDelta);

			shadowType.setObjectChange(objectDeltaType);
		}
		return shadow;
	}
	
	////////////////////////////////////////////////////////////////////////////
	// SEARCH
	////////////////////////////////////////////////////////////////////////////
	
	public void listShadows(final ResourceType resource, final QName objectClass,
			final ShadowHandler<ShadowType> handler, final boolean readFromRepository, final OperationResult parentResult)
			throws CommunicationException, ObjectNotFoundException, SchemaException, ConfigurationException {

		Validate.notNull(objectClass);
		if (resource == null) {
			parentResult.recordFatalError("Resource must not be null");
			throw new IllegalArgumentException("Resource must not be null.");
		}

		searchObjectsIterativeInternal(objectClass, resource, null, handler,
				readFromRepository, parentResult);

	}

	public void searchObjectsIterative(final QName objectClassName, final ResourceType resourceType,
			ObjectQuery query, final ShadowHandler<ShadowType> handler, final OperationResult parentResult)
			throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException {

		Validate.notNull(resourceType, "Resource must not be null.");
		Validate.notNull(objectClassName, "Object class must not be null.");
		Validate.notNull(parentResult, "Operation result must not be null.");

		LOGGER.trace("Searching objects iterative with obejct class {}, resource: {}.", objectClassName,
				ObjectTypeUtil.toShortString(resourceType));

		searchObjectsIterativeInternal(objectClassName, resourceType, query, handler,
				true, parentResult);

	}

	private void searchObjectsIterativeInternal(QName objectClassName,
			final ResourceType resourceType, ObjectQuery query,
			final ShadowHandler<ShadowType> handler,
			final boolean readFromRepository, final OperationResult parentResult) throws SchemaException,
			ObjectNotFoundException, CommunicationException, ConfigurationException {

		final ResourceSchema resourceSchema = resourceTypeManager.getResourceSchema(resourceType, parentResult);

		if (resourceSchema == null) {
			parentResult.recordFatalError("No schema for "+resourceType);
			throw new ConfigurationException("No schema for "+resourceType);
		}
		
		final RefinedObjectClassDefinition objectClassDef = determineObjectClassDefinition(objectClassName, resourceType, query);

		if (objectClassDef == null) {
			String message = "Object class " + objectClassName + " is not defined in schema of "
					+ ObjectTypeUtil.toShortString(resourceType);
			LOGGER.error(message);
			parentResult.recordFatalError(message);
			throw new SchemaException(message);
		}
		
		ObjectFilter filter = null;
		if (query != null) {
			filter = query.getFilter();
		}
		ObjectQuery attributeQuery = null;
		List<ObjectFilter> attributeFilter = new ArrayList<ObjectFilter>();
		
		if (filter instanceof AndFilter){
			List<? extends ObjectFilter> conditions = ((AndFilter) filter).getCondition();
			attributeFilter = getAttributeQuery(conditions, attributeFilter);
			if (attributeFilter.size() > 1){
				attributeQuery = ObjectQuery.createObjectQuery(AndFilter.createAnd(attributeFilter));
			}
			
			if (attributeFilter.size() < 1){
				LOGGER.trace("No attribute filter defined in the query.");
			}
			
			if (attributeFilter.size() == 1){
				attributeQuery = ObjectQuery.createObjectQuery(attributeFilter.get(0));
			}
			
		}
		
		if (query != null && query.getPaging() != null){
			if (attributeQuery == null){
				attributeQuery = new ObjectQuery();
			}
			attributeQuery.setPaging(query.getPaging());
		}

		final ConnectorInstance connector = getConnectorInstance(resourceType, parentResult);

		ResultHandler<ShadowType> resultHandler = new ResultHandler<ShadowType>() {

			@Override
			public boolean handle(PrismObject<ShadowType> resourceShadow) {
				LOGGER.trace("Found resource object {}", SchemaDebugUtil.prettyPrint(resourceShadow));
				PrismObject<ShadowType> resultShadow;
				try {
					// Try to find shadow that corresponds to the resource object
					if (readFromRepository) {
						PrismObject<ShadowType> repoShadow = lookupOrCreateShadowInRepository(connector, resourceShadow, objectClassDef, resourceType, parentResult); 
						
						applyAttributesDefinition(repoShadow, resourceType);
						resultShadow = completeShadow(connector, resourceShadow, repoShadow,
								resourceType, objectClassDef, parentResult);

					} else {
						resultShadow = resourceShadow;
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
				} catch (ObjectNotFoundException e) {
					// TODO: better error handling
					parentResult.recordFatalError(e.getMessage(), e);
					LOGGER.error("{}", e.getMessage(), e);
					return false;
				} catch (CommunicationException e) {
					// TODO: better error handling
					parentResult.recordFatalError(e.getMessage(), e);
					LOGGER.error("{}", e.getMessage(), e);
					return false;
				} catch (SecurityViolationException e) {
					// TODO: better error handling
					parentResult.recordFatalError(e.getMessage(), e);
					LOGGER.error("{}", e.getMessage(), e);
					return false;
				} catch (GenericConnectorException e) {
					// TODO: better error handling
					parentResult.recordFatalError(e.getMessage(), e);
					LOGGER.error("{}", e.getMessage(), e);
					return false;
				}

				return handler.handle(resultShadow.asObjectable());
			}

		};
		
		resouceObjectConverter.searchResourceObjects(connector, resourceType, objectClassDef, resultHandler, attributeQuery, parentResult);
		
	}
	
	private PrismObject<ShadowType> lookupOrCreateShadowInRepository(ConnectorInstance connector, PrismObject<ShadowType> resourceShadow,
			RefinedObjectClassDefinition objectClassDef, ResourceType resourceType, OperationResult parentResult) 
					throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, SecurityViolationException, GenericConnectorException {
		PrismObject<ShadowType> repoShadow = shadowManager.lookupShadowInRepository(resourceShadow, objectClassDef, resourceType,
				parentResult);

		if (repoShadow == null) {
			LOGGER.trace(
					"Shadow object (in repo) corresponding to the resource object (on the resource) was not found. The repo shadow will be created. The resource object:\n{}",
					SchemaDebugUtil.prettyPrint(resourceShadow));

			
			PrismObject<ShadowType> conflictingShadow = shadowManager.lookupShadowByName(resourceShadow, objectClassDef, resourceType, parentResult);
			if (conflictingShadow != null){
				applyAttributesDefinition(conflictingShadow, resourceType);
				conflictingShadow = completeShadow(connector, resourceShadow, conflictingShadow, resourceType, objectClassDef, parentResult);
				Task task = taskManager.createTaskInstance();
				ResourceOperationDescription failureDescription = shadowManager.createResourceFailureDescription(conflictingShadow, resourceType, parentResult);
				changeNotificationDispatcher.notifyFailure(failureDescription, task, parentResult);
				shadowManager.deleteConflictedShadowFromRepo(conflictingShadow, parentResult);
			}
			// TODO: make sure that the resource object has appropriate definition (use objectClass and schema)
			// The resource object obviously exists on the resource, but appropriate shadow does not exist in the
			// repository we need to create the shadow to align repo state to the reality (resource)

			try {

				repoShadow = shadowManager.createRepositoryShadow(
						resourceShadow, resourceType, objectClassDef);
				String oid = repositoryService.addObject(repoShadow, null,
						parentResult);
				repoShadow.setOid(oid);
				
			} catch (ObjectAlreadyExistsException e) {
				// This should not happen. We haven't supplied an OID so is should not conflict
				LOGGER.error("Unexpected repository behavior: Object already exists: {}", e.getMessage(), e);
				throw new SystemException("Unexpected repository behavior: Object already exists: "+e.getMessage(),e);
			}

		} else {
			LOGGER.trace("Found shadow object in the repository {}",
					SchemaDebugUtil.prettyPrint(repoShadow));
		}
		
		return repoShadow;
	}
	
	private List<ObjectFilter> getAttributeQuery(List<? extends ObjectFilter> conditions, List<ObjectFilter> attributeFilter) throws SchemaException{
		
		for (ObjectFilter f : conditions){
			if (f instanceof EqualsFilter){
				if (ShadowType.F_OBJECT_CLASS.equals(((EqualsFilter) f).getDefinition().getName())){
					continue;
				}
				if (ShadowType.F_RESOURCE_REF.equals(((EqualsFilter) f).getDefinition().getName())){
					continue;
				}
				
				attributeFilter.add(f);
			} else if (f instanceof NaryLogicalFilter){
				attributeFilter = getAttributeQuery(((NaryLogicalFilter) f).getCondition(), attributeFilter);
			} else if (f instanceof SubstringFilter){
				attributeFilter.add(f);
			}
			
		}
		
		return attributeFilter;
		
}
	
	////////////////
	
	private ConnectorInstance getConnectorInstance(ResourceType resource, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {
		return connectorManager.getConfiguredConnectorInstance(resource.asPrismObject(), false, parentResult);
	}
	
	///////////////////////////////////////////////////////////////////////////
	// TODO: maybe split this to a separate class
	///////////////////////////////////////////////////////////////////////////
	
	public List<Change<ShadowType>> fetchChanges(ResourceType resourceType, 
			QName objectClass, PrismProperty<?> lastToken,  OperationResult parentResult)
			throws ObjectNotFoundException, CommunicationException, GenericFrameworkException, SchemaException,
			ConfigurationException, SecurityViolationException {

		RefinedObjectClassDefinition objecClassDefinition = determineObjectClassDefinition(objectClass, resourceType);
		ConnectorInstance connector = getConnectorInstance(resourceType, parentResult);
		
		List<Change<ShadowType>> changes = null;
		try {

			changes = resouceObjectConverter.fetchChanges(connector, resourceType, objecClassDefinition, lastToken, parentResult);

			LOGGER.trace("Found {} change(s). Start processing it (them).", changes.size());

			for (Iterator<Change<ShadowType>> i = changes.iterator(); i.hasNext();) {
				// search objects in repository
				Change<ShadowType> change = i.next();

				PrismObject<ShadowType> oldShadow = shadowManager.findOrCreateShadowFromChange(resourceType, change, objecClassDefinition, parentResult);
				if (oldShadow != null) {
					applyAttributesDefinition(oldShadow, resourceType);
					ShadowType oldShadowType = oldShadow.asObjectable();
	
					LOGGER.trace("Old shadow: {}", oldShadow);
	
					// skip setting other attribute when shadow is null
					if (oldShadow == null) {
						change.setOldShadow(null);
						continue;
					}
	
					change.setOldShadow(oldShadow);

					// FIXME: hack. make sure that the current shadow has OID
					// and resource ref, also the account type should be set
					if (change.getCurrentShadow() != null) {
						ShadowType currentShadowType = change.getCurrentShadow().asObjectable();
						if (currentShadowType != null) {
							currentShadowType.setOid(oldShadow.getOid());
							currentShadowType.setResourceRef(oldShadowType.getResourceRef());
							currentShadowType.setKind(objecClassDefinition.getKind());
							currentShadowType.setIntent(oldShadowType.getIntent());
						}
					}

					// FIXME: hack. the object delta must have oid specified.
					if (change.getObjectDelta() != null && change.getObjectDelta().getOid() == null) {
						ObjectDelta<ShadowType> objDelta = new ObjectDelta<ShadowType>(ShadowType.class, ChangeType.DELETE, prismContext);
						change.setObjectDelta(objDelta);
						change.getObjectDelta().setOid(oldShadow.getOid());
					}
					
				} else {
					LOGGER.debug("No old shadow for synchronization event {}, the shadow must be gone in the meantime (this is probably harmless)", change);
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

	public PrismProperty<?> fetchCurrentToken(ResourceType resourceType, OperationResult parentResult)
			throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException {

		Validate.notNull(resourceType, "Resource must not be null.");
		Validate.notNull(parentResult, "Operation result must not be null.");

		ObjectClassComplexTypeDefinition objecClassDefinition = determineDefaultAccountObjectClassDefinition(resourceType);
		ConnectorInstance connector = getConnectorInstance(resourceType, parentResult);
		
		LOGGER.trace("Getting last token");
		PrismProperty<?> lastToken = null;
		try {
			ResourceSchema resourceSchema = resourceTypeManager.getResourceSchema(resourceType, parentResult);
			if (resourceSchema == null) {
				throw new ConfigurationException("No schema for "+resourceType);
			}
			lastToken = resouceObjectConverter.fetchCurrentToken(connector, resourceType, objecClassDefinition, parentResult);
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
	
	
	//////////////////////////////////////////////////////////////////////////////////////
	

	public ObjectClassComplexTypeDefinition applyAttributesDefinition(ObjectDelta<ShadowType> delta, 
			ResourceShadowDiscriminator discriminator, ResourceType resource) throws SchemaException, ConfigurationException {
		ObjectClassComplexTypeDefinition objectClassDefinition = determineObjectClassDefinition(discriminator, resource);
		return applyAttributesDefinition(delta, objectClassDefinition, resource);
	}
	
	public ObjectClassComplexTypeDefinition applyAttributesDefinition(ObjectDelta<ShadowType> delta, 
			PrismObject<ShadowType> shadow, ResourceType resource) throws SchemaException, ConfigurationException {
		ObjectClassComplexTypeDefinition objectClassDefinition = determineObjectClassDefinition(shadow, resource);
		return applyAttributesDefinition(delta, objectClassDefinition, resource);
	}

	private ObjectClassComplexTypeDefinition applyAttributesDefinition(ObjectDelta<ShadowType> delta, 
			ObjectClassComplexTypeDefinition objectClassDefinition, ResourceType resource) throws SchemaException, ConfigurationException {
		if (delta.isAdd()) {
			applyAttributesDefinition(delta.getObjectToAdd(), resource);
		} else if (delta.isModify()) {
			ItemPath attributesPath = new ItemPath(ShadowType.F_ATTRIBUTES);
			for(ItemDelta<?> modification: delta.getModifications()) {
				if (modification.getDefinition() == null && attributesPath.equals(modification.getParentPath())) {
					QName attributeName = modification.getName();
					ResourceAttributeDefinition attributeDefinition = objectClassDefinition.findAttributeDefinition(attributeName);
					if (attributeDefinition == null) {
						throw new SchemaException("No definition for attribute "+attributeName+" in object delta "+delta);
					}
					modification.applyDefinition(attributeDefinition);
				}
			}
		}

		return objectClassDefinition;
	}

	public RefinedObjectClassDefinition applyAttributesDefinition(
			PrismObject<ShadowType> shadow, ResourceType resource) throws SchemaException, ConfigurationException {
		RefinedObjectClassDefinition objectClassDefinition = determineObjectClassDefinition(shadow, resource);

		PrismContainer<?> attributesContainer = shadow.findContainer(ShadowType.F_ATTRIBUTES);
		if (attributesContainer != null) {
			if (attributesContainer instanceof ResourceAttributeContainer) {
				if (attributesContainer.getDefinition() == null) {
					attributesContainer.applyDefinition(objectClassDefinition.toResourceAttributeContainerDefinition());
				}
			} else {
				// We need to convert <attributes> to ResourceAttributeContainer
				ResourceAttributeContainer convertedContainer = ResourceAttributeContainer.convertFromContainer(
						attributesContainer, objectClassDefinition);
				shadow.getValue().replace(attributesContainer, convertedContainer);
			}
		}
		
		// We also need to replace the entire object definition to inject correct object class definition here
		// If we don't do this then the patch (delta.applyTo) will not work correctly because it will not be able to
		// create the attribute container if needed.

		PrismObjectDefinition<ShadowType> objectDefinition = shadow.getDefinition();
		PrismContainerDefinition<ShadowAttributesType> origAttrContainerDef = objectDefinition.findContainerDefinition(ShadowType.F_ATTRIBUTES);
		if (origAttrContainerDef == null || !(origAttrContainerDef instanceof ResourceAttributeContainerDefinition)) {
			PrismObjectDefinition<ShadowType> clonedDefinition = objectDefinition.cloneWithReplacedDefinition(ShadowType.F_ATTRIBUTES,
					objectClassDefinition.toResourceAttributeContainerDefinition());
			shadow.setDefinition(clonedDefinition);
		}
		
		return objectClassDefinition;
	}

	private RefinedObjectClassDefinition determineObjectClassDefinition(PrismObject<ShadowType> shadow, ResourceType resource) throws SchemaException, ConfigurationException {
		ShadowType shadowType = shadow.asObjectable();
		RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resource, prismContext);
		if (refinedSchema == null) {
			throw new ConfigurationException("No schema definied for "+resource);
		}
		
		
		RefinedObjectClassDefinition objectClassDefinition = null;
		ShadowKindType kind = shadowType.getKind();
		String intent = shadowType.getIntent();
		QName objectClass = shadow.asObjectable().getObjectClass();
		if (kind != null) {
			objectClassDefinition = refinedSchema.getRefinedDefinition(kind, intent);
		} else {
			// Fallback to objectclass only
			if (objectClass == null) {
				throw new SchemaException("No kind nor objectclass definied in "+shadow);
			}
			objectClassDefinition = refinedSchema.findRefinedDefinitionByObjectClassQName(null, objectClass);
		}
		
		if (objectClassDefinition == null) {
			throw new SchemaException("Definition for "+shadow+" not found (objectClass=" + PrettyPrinter.prettyPrint(objectClass) +
					", kind="+kind+", intent='"+intent+"') in schema of " + resource);
		}		
		
		return objectClassDefinition;
	}
	
	private ObjectClassComplexTypeDefinition determineObjectClassDefinition(
			ResourceShadowDiscriminator discriminator, ResourceType resource) throws SchemaException {
		ResourceSchema schema = RefinedResourceSchema.getResourceSchema(resource, prismContext);
		// HACK FIXME
		ObjectClassComplexTypeDefinition objectClassDefinition = schema.findObjectClassDefinition(ShadowKindType.ACCOUNT, discriminator.getIntent());

		if (objectClassDefinition == null) {
			// Unknown objectclass
			throw new SchemaException("Account type " + discriminator.getIntent()
					+ " is not known in schema of " + resource);
		}
		
		return objectClassDefinition;
	}
	
	private RefinedObjectClassDefinition determineObjectClassDefinition( 
			QName objectClassName, ResourceType resourceType, ObjectQuery query) throws SchemaException, ConfigurationException {
		ShadowKindType kind = null;
		String intent = null;
		if (query != null && query.getFilter() != null) {
			List<? extends ObjectFilter> conditions = ((AndFilter) query.getFilter()).getCondition();
			kind = ShadowCacheUtil.getValueFromFilter(conditions, ShadowType.F_KIND);
			intent = ShadowCacheUtil.getValueFromFilter(conditions, ShadowType.F_INTENT);
		}
		RefinedObjectClassDefinition objectClassDefinition;
		if (kind == null) {
			objectClassDefinition = getRefinedScema(resourceType).getRefinedDefinition(objectClassName);
		} else {
			objectClassDefinition = getRefinedScema(resourceType).getRefinedDefinition(kind, intent);
		}
		
		return objectClassDefinition;
	}
	
	private RefinedObjectClassDefinition determineObjectClassDefinition(QName objectClassName, ResourceType resourceType)
			throws SchemaException, ConfigurationException {
		return getRefinedScema(resourceType).getRefinedDefinition(objectClassName);
	}
	
	
	private ObjectClassComplexTypeDefinition determineDefaultAccountObjectClassDefinition(ResourceType resourceType) throws SchemaException, ConfigurationException {
		// HACK, FIXME
		return getRefinedScema(resourceType).getDefaultRefinedDefinition(ShadowKindType.ACCOUNT);
	}
	
	private RefinedResourceSchema getRefinedScema(ResourceType resourceType) throws SchemaException, ConfigurationException {
		RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resourceType);
		if (refinedSchema == null) {
			throw new ConfigurationException("No schema for "+resourceType);
		}
		return refinedSchema;
	}
	
	/**
	 * Make sure that the shadow is complete, e.g. that all the mandatory fields
	 * are filled (e.g name, resourceRef, ...) Also transforms the shadow with
	 * respect to simulated capabilities.
	 */
	private PrismObject<ShadowType> completeShadow(ConnectorInstance connector, PrismObject<ShadowType> resourceShadow,
			PrismObject<ShadowType> repoShadow, ResourceType resource, RefinedObjectClassDefinition objectClassDefinition, 
			OperationResult parentResult) throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, SecurityViolationException, GenericConnectorException {

		PrismObject<ShadowType> resultShadow = repoShadow;
		boolean cloned = false;
		if (resultShadow == null) {
			resultShadow = resourceShadow.clone();
			cloned = true;
		}
		
		ResourceAttributeContainer resourceAttributesContainer = ShadowUtil
				.getAttributesContainer(resourceShadow);
		ResourceAttributeContainer repoAttributesContainer = ShadowUtil
				.getAttributesContainer(resultShadow);

		ShadowType resultShadowType = resultShadow.asObjectable();
		ShadowType resourceShadowType = resourceShadow.asObjectable();
		
		if (resultShadowType.getObjectClass() == null) {
			resultShadowType.setObjectClass(resourceAttributesContainer.getDefinition().getTypeName());
		}
		if (resultShadowType.getName() == null) {
			resultShadowType.setName(ShadowCacheUtil.determineShadowName(resourceShadow));
		}
		if (resultShadowType.getResource() == null) {
			resultShadowType.setResourceRef(ObjectTypeUtil.createObjectRef(resource));
		}

		// Attributes
		// If the shadows are the same then no copy is needed.
		if (resultShadow != resourceShadow && !cloned) {
			repoAttributesContainer.getValue().clear();
			for (ResourceAttribute<?> resourceAttribute : resourceAttributesContainer.getAttributes()) {
				repoAttributesContainer.add(resourceAttribute.clone());
			}
			
			resultShadowType.setProtectedObject(resourceShadowType.isProtectedObject());
			resultShadowType.setIgnored(resourceShadowType.isIgnored());
			resultShadowType.setActivation(resourceShadowType.getActivation());
			
			// Credentials
			ShadowType resultAccountShadow = resultShadow.asObjectable();
			ShadowType resourceAccountShadow = resourceShadow.asObjectable();
			resultAccountShadow.setCredentials(resourceAccountShadow.getCredentials());
			
		}
		
		// Associations
		PrismContainer<ShadowAssociationType> resourceAssociationContainer = resourceShadow.findContainer(ShadowType.F_ASSOCIATION);
		if (resourceAssociationContainer != null) {
			RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resource);
			PrismContainer<ShadowAssociationType> associationContainer = resourceAssociationContainer.clone();
			resultShadow.add(associationContainer);
			if (associationContainer != null) {
				for (PrismContainerValue<ShadowAssociationType> associationCVal: associationContainer.getValues()) {
					ResourceAttributeContainer identifierContainer = ShadowUtil.getAttributesContainer(associationCVal, ShadowAssociationType.F_IDENTIFIERS);
					Collection<ResourceAttribute<?>> entitlementIdentifiers = identifierContainer.getAttributes();
					if (entitlementIdentifiers == null || entitlementIdentifiers.isEmpty()) {
						throw new IllegalStateException("No entitlement identifiers present for association "+associationCVal);
					}
					ShadowAssociationType shadowAssociationType = associationCVal.asContainerable();
					QName associationName = shadowAssociationType.getName();
					ResourceObjectAssociationType entitlementAssociationType = objectClassDefinition.findEntitlementAssociation(associationName);
					String entitlementIntent = entitlementAssociationType.getIntent();
					RefinedObjectClassDefinition entitlementObjectClassDef = refinedSchema.getRefinedDefinition(ShadowKindType.ENTITLEMENT, entitlementIntent);
					
					PrismObject<ShadowType> entitlementShadow = (PrismObject<ShadowType>) identifierContainer.getUserData(ResourceObjectConverter.FULL_SHADOW_KEY);
					if (entitlementShadow == null) {
						entitlementShadow = resouceObjectConverter.locateResourceObject(connector, resource, entitlementIdentifiers, entitlementObjectClassDef, parentResult); 
					}
					PrismObject<ShadowType> entitlementRepoShadow = lookupOrCreateShadowInRepository(connector, entitlementShadow, entitlementObjectClassDef, resource, parentResult);
					ObjectReferenceType shadowRefType = new ObjectReferenceType();
					shadowRefType.setOid(entitlementRepoShadow.getOid());
					shadowAssociationType.setShadowRef(shadowRefType);
				}
			}
		}
		

		return resultShadow;
	}
	
	
	
	// ENTITLEMENTS
	
	/**
	 * Makes sure that all the entitlements have identifiers in them so this is usable by the
	 * ResourceObjectConverter.
	 */
	private void preprocessEntitlements(PrismObject<ShadowType> shadow,  final ResourceType resource,
			final OperationResult result) throws SchemaException, ObjectNotFoundException, ConfigurationException {
		Visitor visitor = new Visitor() {
			@Override
			public void visit(Visitable visitable) {
				try {
					preprocessEntitlement((PrismContainerValue<ShadowAssociationType>)visitable, resource, result);
				} catch (SchemaException e) {
					throw new TunnelException(e);
				} catch (ObjectNotFoundException e) {
					throw new TunnelException(e);
				} catch (ConfigurationException e) {
					throw new TunnelException(e);
				}
			}
		};
		try {
			shadow.accept(visitor , new ItemPath(
				new NameItemPathSegment(ShadowType.F_ASSOCIATION),
				IdItemPathSegment.WILDCARD), false);
		} catch (TunnelException e) {
			Throwable cause = e.getCause();
			if (cause instanceof SchemaException) {
				throw (SchemaException)cause;
			} else if (cause instanceof ObjectNotFoundException) {
				throw (ObjectNotFoundException)cause;
			} else if (cause instanceof ConfigurationException) {
				throw (ConfigurationException)cause;
			} else {
				throw new SystemException("Unexpected exception "+cause, cause);
			}
		}
	}
	
	/**
	 * Makes sure that all the entitlements have identifiers in them so this is usable by the
	 * ResourceObjectConverter.
	 */	
	private void preprocessEntitlements(Collection<? extends ItemDelta> modifications, final ResourceType resource, 
			final OperationResult result) throws SchemaException, ObjectNotFoundException, ConfigurationException {
		Visitor visitor = new Visitor() {
			@Override
			public void visit(Visitable visitable) {
				try {
					preprocessEntitlement((PrismContainerValue<ShadowAssociationType>)visitable, resource, result);
				} catch (SchemaException e) {
					throw new TunnelException(e);
				} catch (ObjectNotFoundException e) {
					throw new TunnelException(e);
				} catch (ConfigurationException e) {
					throw new TunnelException(e);
				}
			}
		};
		try {
			ItemDelta.accept(modifications, visitor , new ItemPath(
				new NameItemPathSegment(ShadowType.F_ASSOCIATION),
				IdItemPathSegment.WILDCARD), false);
		} catch (TunnelException e) {
			Throwable cause = e.getCause();
			if (cause instanceof SchemaException) {
				throw (SchemaException)cause;
			} else if (cause instanceof ObjectNotFoundException) {
				throw (ObjectNotFoundException)cause;
			} else if (cause instanceof ConfigurationException) {
				throw (ConfigurationException)cause;
			} else {
				throw new SystemException("Unexpected exception "+cause, cause);
			}
		}
	}
	

	private void preprocessEntitlement(PrismContainerValue<ShadowAssociationType> association, ResourceType resource, OperationResult result) 
			throws SchemaException, ObjectNotFoundException, ConfigurationException {
		PrismContainer<Containerable> identifiersContainer = association.findContainer(ShadowAssociationType.F_IDENTIFIERS);
		if (identifiersContainer != null && !identifiersContainer.isEmpty()) {
			// We already have identifiers here
			return;
		}
		ShadowAssociationType associationType = association.asContainerable();
		if (associationType.getShadowRef() == null || StringUtils.isEmpty(associationType.getShadowRef().getOid())) {
			throw new SchemaException("No identifiers and no OID specified in entitlements association "+association);
		}
		PrismObject<ShadowType> repoShadow;
		try {
			repoShadow = repositoryService.getObject(ShadowType.class, associationType.getShadowRef().getOid(), result);
		} catch (ObjectNotFoundException e) {
			throw new ObjectNotFoundException(e.getMessage()+" while resolving entitlement association OID in "+association, e);
		}
		applyAttributesDefinition(repoShadow, resource);
		transplantIdentifiers(association, repoShadow);
	}

	private void transplantIdentifiers(PrismContainerValue<ShadowAssociationType> association, PrismObject<ShadowType> repoShadow) throws SchemaException {
		PrismContainer<Containerable> identifiersContainer = association.findContainer(ShadowAssociationType.F_IDENTIFIERS);
		if (identifiersContainer == null) {
			ResourceAttributeContainer origContainer = ShadowUtil.getAttributesContainer(repoShadow);
			identifiersContainer = new ResourceAttributeContainer(ShadowAssociationType.F_IDENTIFIERS, origContainer.getDefinition(), prismContext);
			association.add(identifiersContainer);
		}
		Collection<ResourceAttribute<?>> identifiers = ShadowUtil.getIdentifiers(repoShadow);
		for (ResourceAttribute<?> identifier: identifiers) {
			identifiersContainer.add(identifier.clone());
		}
		Collection<ResourceAttribute<?>> secondaryIdentifiers = ShadowUtil.getSecondaryIdentifiers(repoShadow);
		for (ResourceAttribute<?> identifier: secondaryIdentifiers) {
			identifiersContainer.add(identifier.clone());
		}
	}

}
