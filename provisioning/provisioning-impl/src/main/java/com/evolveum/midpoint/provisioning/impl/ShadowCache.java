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
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.evolveum.midpoint.common.monitor.InternalMonitor;
import com.evolveum.midpoint.common.refinery.RefinedAssociationDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.ResourceShadowDiscriminator;
import com.evolveum.midpoint.common.refinery.ShadowDiscriminatorObjectDelta;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.Visitable;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.NaryLogicalFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.SubstringFilter;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.provisioning.api.ChangeNotificationDispatcher;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.provisioning.api.ResourceOperationDescription;
import com.evolveum.midpoint.provisioning.consistency.api.ErrorHandler;
import com.evolveum.midpoint.provisioning.consistency.api.ErrorHandler.FailedOperation;
import com.evolveum.midpoint.provisioning.consistency.impl.ErrorHandlerFactory;
import com.evolveum.midpoint.provisioning.ucf.api.Change;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.ucf.api.PropertyModificationOperation;
import com.evolveum.midpoint.provisioning.ucf.api.ResultHandler;
import com.evolveum.midpoint.provisioning.ucf.impl.ConnectorFactoryIcfImpl;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainerDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.QNameUtil;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AvailabilityStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FailedOperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationProvisioningScriptsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

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
	private AccessChecker accessChecker;
	
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
	
	public PrismObject<ShadowType> getShadow(String oid, PrismObject<ShadowType> repositoryShadow, 
			Collection<SelectorOptions<GetOperationOptions>> options, Task task, 
			OperationResult parentResult) throws ObjectNotFoundException, CommunicationException, SchemaException,
			ConfigurationException, SecurityViolationException {

		Validate.notNull(oid, "Object id must not be null.");
		
		LOGGER.trace("Start getting object with oid {}", oid);
		
		GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);

		// We are using parent result directly, not creating subresult.
		// We want to hide the existence of shadow cache from the user.

		// Get the shadow from repository. There are identifiers that we need
		// for accessing the object by UCF.
		// Later, the repository object may have a fully cached object from the resource.
		if (repositoryShadow == null) {
			repositoryShadow = repositoryService.getObject(ShadowType.class, oid, null, parentResult);
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Got repository shadow object:\n{}", repositoryShadow.debugDump());
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
		
		RefinedObjectClassDefinition objectClassDefinition = applyAttributesDefinition(repositoryShadow, resource);
		
		ConnectorInstance connector = null;
		OperationResult connectorResult = parentResult.createMinorSubresult(ShadowCache.class.getName() + ".getConnectorInstance");
		try {
			connector = getConnectorInstance(resource, parentResult);
			connectorResult.recordSuccess();
		} catch (ObjectNotFoundException ex){
			connectorResult.recordPartialError("Could not get connector instance. " + ex.getMessage(),  ex);
			return repositoryShadow;
		} catch (SchemaException ex){
			connectorResult.recordPartialError("Could not get connector instance. " + ex.getMessage(),  ex);
			return repositoryShadow;
		} catch (CommunicationException ex){
			connectorResult.recordPartialError("Could not get connector instance. " + ex.getMessage(),  ex);
			return repositoryShadow;
		} catch (ConfigurationException ex){
			connectorResult.recordPartialError("Could not get connector instance. " + ex.getMessage(),  ex);
			return repositoryShadow;
		}

		PrismObject<ShadowType> resourceShadow = null;
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
				SchemaException ex = new SchemaException("No identifiers found in the repository shadow "
						+ repositoryShadow + " with respect to " + resource);
				parentResult.recordFatalError("No identifiers found in the repository shadow "+ repositoryShadow, ex);
				throw ex;
			}
	
			// We need to record the fetch down here. Now it is certain that we are going to fetch from resource
			// (we do not have raw/noFetch option)
			InternalMonitor.recordShadowFetchOperation();
			
			resourceShadow = resouceObjectConverter.getResourceObject(connector, resource, identifiers, objectClassDefinition, parentResult);
			resourceTypeManager.modifyResourceAvailabilityStatus(resource.asPrismObject(), AvailabilityStatusType.UP, parentResult);
			
			//try to apply changes to the account only if the resource if UP
			if (repositoryShadow.asObjectable().getObjectChange() != null && repositoryShadow.asObjectable().getFailedOperationType() != null
					&& resource.getOperationalState() != null
					&& resource.getOperationalState().getLastAvailabilityStatus() == AvailabilityStatusType.UP) {
				throw new GenericConnectorException(
						"Found changes that have been not applied to the account yet. Trying to apply them now.");
			}
			
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Shadow from repository:\n{}", repositoryShadow.debugDump());
				LOGGER.trace("Resource object fetched from resource:\n{}", resourceShadow.debugDump());
			}
			
			// Complete the shadow by adding attributes from the resource object
			PrismObject<ShadowType> resultShadow = completeShadow(connector, resourceShadow, repositoryShadow, resource, objectClassDefinition, parentResult);

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Shadow when assembled:\n{}", resultShadow.debugDump());
			}
			
			parentResult.recordSuccess();
			return resultShadow;

			
		} catch (Exception ex) {
			try {
				boolean compensate = GetOperationOptions.isDoNotDiscovery(rootOptions)? false : true;
				resourceShadow = handleError(ex, repositoryShadow, FailedOperation.GET, resource, null, compensate,
						task, parentResult);
				
				return resourceShadow;

			} catch (GenericFrameworkException e) {
				throw new SystemException(e);
			} catch (ObjectAlreadyExistsException e) {
				throw new SystemException(e);
			}
		}
		
		
	}

	public abstract String afterAddOnResource(PrismObject<ShadowType> shadow, ResourceType resource, 
			RefinedObjectClassDefinition objectClassDefinition, OperationResult parentResult)
					throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException;
	
	public String addShadow(PrismObject<ShadowType> shadow, OperationProvisioningScriptsType scripts,
			ResourceType resource, ProvisioningOperationOptions options, Task task, OperationResult parentResult) throws CommunicationException,
			GenericFrameworkException, ObjectAlreadyExistsException, SchemaException, ObjectNotFoundException,
			ConfigurationException, SecurityViolationException {
		Validate.notNull(shadow, "Object to add must not be null.");

		InternalMonitor.recordShadowChangeOperation();
		
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Start adding shadow object:\n{}", shadow.debugDump());
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
            shadowManager.setKindIfNecessary(shadow.asObjectable(), objectClassDefinition);
			accessChecker.checkAdd(resource, shadow, objectClassDefinition, parentResult);
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

	public abstract void afterModifyOnResource(PrismObject<ShadowType> shadow, Collection<? extends ItemDelta> modifications, OperationResult parentResult) throws SchemaException, ObjectNotFoundException;
	
	public abstract Collection<? extends ItemDelta> beforeModifyOnResource(PrismObject<ShadowType> shadow, ProvisioningOperationOptions options, Collection<? extends ItemDelta> modifications) throws SchemaException;
	
	public String modifyShadow(PrismObject<ShadowType> shadow, ResourceType resource, String oid,
				Collection<? extends ItemDelta> modifications, OperationProvisioningScriptsType scripts, ProvisioningOperationOptions options, Task task, OperationResult parentResult)
				throws CommunicationException, GenericFrameworkException, ObjectNotFoundException, SchemaException,
				ConfigurationException, SecurityViolationException {

		Validate.notNull(shadow, "Object to modify must not be null.");
		Validate.notNull(oid, "OID must not be null.");
		Validate.notNull(modifications, "Object modification must not be null.");

		InternalMonitor.recordShadowChangeOperation();
		
		if (resource == null) {
			resource = getResource(shadow, parentResult);
		}
		
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Modifying resource with oid {}, object:\n{}", resource.getOid(), shadow.debugDump());
		}
		
		RefinedObjectClassDefinition objectClassDefinition =  applyAttributesDefinition(shadow, resource);
		
		accessChecker.checkModify(resource, shadow, modifications, objectClassDefinition, parentResult);

		preprocessEntitlements(modifications, resource, "delta for shadow "+oid, parentResult);

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

		Collection<PropertyDelta<?>> renameDeltas = distillRenameDeltas(modifications, shadow, objectClassDefinition);

		Collection<? extends ItemDelta> sideEffectDelta = convertToPropertyDelta(sideEffectChanges);
		if (renameDeltas != null) {
			((Collection) sideEffectDelta).addAll(renameDeltas);
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

	private Collection<PropertyDelta<?>> distillRenameDeltas(Collection<? extends ItemDelta> modifications, 
			PrismObject<ShadowType> shadow, RefinedObjectClassDefinition objectClassDefinition) throws SchemaException {
		PropertyDelta<String> nameDelta = (PropertyDelta<String>) ItemDelta.findItemDelta(modifications, new ItemPath(ShadowType.F_ATTRIBUTES, ConnectorFactoryIcfImpl.ICFS_NAME), ItemDelta.class); 
		if (nameDelta == null){
			return null;
		}

		PrismProperty<String> name = nameDelta.getPropertyNew();
		String newName = name.getRealValue();
		
		Collection<PropertyDelta<?>> deltas = new ArrayList<PropertyDelta<?>>();
		
		// $shadow/attributes/icfs:name
		String normalizedNewName = shadowManager.getNormalizedAttributeValue(name.getValue(), objectClassDefinition.findAttributeDefinition(name.getElementName()));
		PropertyDelta<String> cloneNameDelta = nameDelta.clone();
		cloneNameDelta.clearValuesToReplace();
		cloneNameDelta.setValueToReplace(new PrismPropertyValue<String>(normalizedNewName));
		deltas.add(cloneNameDelta);
		
		// $shadow/name
		if (!newName.equals(shadow.asObjectable().getName().getOrig())){
			
			PropertyDelta<?> shadowNameDelta = PropertyDelta.createModificationReplaceProperty(ShadowType.F_NAME, shadow.getDefinition(), 
					new PolyString(newName));
			deltas.add(shadowNameDelta);
		}
		
		return deltas;
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

	public void deleteShadow(PrismObject<ShadowType> shadow, ProvisioningOperationOptions options, OperationProvisioningScriptsType scripts,
			ResourceType resource, Task task, OperationResult parentResult) throws CommunicationException,
			GenericFrameworkException, ObjectNotFoundException, SchemaException, ConfigurationException,
			SecurityViolationException {

		Validate.notNull(shadow, "Object to delete must not be null.");
		Validate.notNull(parentResult, "Operation result must not be null.");

		InternalMonitor.recordShadowChangeOperation();
		
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
				    shadow = repositoryService.getObject(delta.getObjectTypeClass(), shadowOid, null, parentResult);
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

	public void applyDefinition(final ObjectQuery query, OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException {
		ObjectFilter filter = query.getFilter();
		String resourceOid = null;
		QName objectClassName = null;
		if (filter instanceof AndFilter){
			List<? extends ObjectFilter> conditions = ((AndFilter) filter).getConditions();
			resourceOid = ProvisioningUtil.getResourceOidFromFilter(conditions);
			objectClassName = ProvisioningUtil.getValueFromFilter(conditions, ShadowType.F_OBJECT_CLASS);
		}
		PrismObject<ResourceType> resource = resourceTypeManager.getResource(resourceOid, result);
		final RefinedObjectClassDefinition objectClassDef = determineObjectClassDefinition(objectClassName, resource.asObjectable(), query);
		applyDefinition(query, objectClassDef);
	}

	public void applyDefinition(final ObjectQuery query, final RefinedObjectClassDefinition objectClassDef) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException {
		if (query == null) {
			return;
		}
		ObjectFilter filter = query.getFilter();
		if (filter == null) {
			return;
		}
		final ItemPath attributesPath = new ItemPath(ShadowType.F_ATTRIBUTES);
		com.evolveum.midpoint.prism.query.Visitor visitor = new com.evolveum.midpoint.prism.query.Visitor() {
			@Override
			public void visit(ObjectFilter filter) {
				if (filter instanceof ValueFilter) {
					ValueFilter<?> valueFilter = (ValueFilter<?>)filter;
					ItemDefinition definition = valueFilter.getDefinition();
					if (definition == null) {
						ItemPath itemPath = valueFilter.getFullPath();
						if (attributesPath.equivalent(valueFilter.getParentPath())) {
							QName attributeName = valueFilter.getElementName();
							ResourceAttributeDefinition attributeDefinition = objectClassDef.findAttributeDefinition(attributeName);
							if (attributeDefinition == null) {
								throw new TunnelException(
										new SchemaException("No definition for attribute "+attributeName+" in query "+query));
							}
							valueFilter.setDefinition(attributeDefinition);
						}
					}
				}
			}
		};
		try {
			filter.accept(visitor);
		} catch (TunnelException te) {
			SchemaException e = (SchemaException)te.getCause();
			throw e;
		}
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
				LOGGER.trace("Storing delta to shadow:\n{}", objectDelta.debugDump());
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

		InternalMonitor.recordShadowFetchOperation();
		
		Validate.notNull(objectClass);
		if (resource == null) {
			parentResult.recordFatalError("Resource must not be null");
			throw new IllegalArgumentException("Resource must not be null.");
		}

		searchObjectsIterativeInternal(objectClass, resource, null, null, handler,
				readFromRepository, parentResult);

	}

	public void searchObjectsIterative(final QName objectClassName, final ResourceType resourceType,
			ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options, final ShadowHandler<ShadowType> handler, final OperationResult parentResult)
			throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException {

		Validate.notNull(resourceType, "Resource must not be null.");
		Validate.notNull(objectClassName, "Object class must not be null.");
		Validate.notNull(parentResult, "Operation result must not be null.");

		LOGGER.trace("Searching objects iterative with obejct class {}, resource: {}.", objectClassName,
				resourceType);

		searchObjectsIterativeInternal(objectClassName, resourceType, query, options, handler,
				true, parentResult);

	}

	private void searchObjectsIterativeInternal(QName objectClassName,
			final ResourceType resourceType, ObjectQuery query,
			Collection<SelectorOptions<GetOperationOptions>> options, final ShadowHandler<ShadowType> handler,
			final boolean readFromRepository, final OperationResult parentResult) throws SchemaException,
			ObjectNotFoundException, CommunicationException, ConfigurationException {

		final ResourceSchema resourceSchema = resourceTypeManager.getResourceSchema(resourceType, parentResult);

		if (resourceSchema == null) {
			parentResult.recordFatalError("No schema for "+resourceType);
			throw new ConfigurationException("No schema for "+resourceType);
		}
		
		final RefinedObjectClassDefinition objectClassDef = determineObjectClassDefinition(objectClassName, resourceType, query);

		applyDefinition(query, objectClassDef);
		
		if (objectClassDef == null) {
			String message = "Object class " + objectClassName + " is not defined in schema of "
					+ ObjectTypeUtil.toShortString(resourceType);
			LOGGER.error(message);
			parentResult.recordFatalError(message);
			throw new SchemaException(message);
		}
		
		GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);
		if (GetOperationOptions.isNoFetch(rootOptions)) {
			searchObjectsIterativeRepository(objectClassDef, resourceType, query, options, handler, parentResult);
			return;
		}
		
		// We need to record the fetch down here. Now it is certain that we are going to fetch from resource
		// (we do not have raw/noFetch option)
		InternalMonitor.recordShadowFetchOperation();
		
		ObjectFilter filter = null;
		if (query != null) {
			filter = query.getFilter();
		}
		ObjectQuery attributeQuery = null;
		List<ObjectFilter> attributeFilter = new ArrayList<ObjectFilter>();
		
		if (filter instanceof AndFilter){
			List<? extends ObjectFilter> conditions = ((AndFilter) filter).getConditions();
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
						
						forceRenameIfNeeded(resourceShadow.asObjectable(), repoShadow.asObjectable(), objectClassDef, parentResult);
						
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
				} catch (ObjectAlreadyExistsException e) {
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
	
	private void searchObjectsIterativeRepository(
			RefinedObjectClassDefinition objectClassDef,
			final ResourceType resourceType, ObjectQuery query,
			Collection<SelectorOptions<GetOperationOptions>> options,
			final ShadowHandler<ShadowType> shadowHandler, OperationResult parentResult) throws SchemaException {
		
		com.evolveum.midpoint.schema.ResultHandler<ShadowType> repoHandler = new com.evolveum.midpoint.schema.ResultHandler<ShadowType>() {
			@Override
			public boolean handle(PrismObject<ShadowType> object,
					OperationResult parentResult) {
				try {
					applyAttributesDefinition(object, resourceType);
					boolean cont = shadowHandler.handle(object.asObjectable());
					parentResult.recordSuccess();
					return cont;
				} catch (RuntimeException e) {
					parentResult.recordFatalError(e);
					throw e;
				} catch (SchemaException e) {
					parentResult.recordFatalError(e);
					throw new SystemException(e);
				} catch (ConfigurationException e) {
					parentResult.recordFatalError(e);
					throw new SystemException(e);
				}
			}
		};
		
		shadowManager.searchObjectsIterativeRepository(objectClassDef, resourceType, query, options, repoHandler, parentResult);
		
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
		
		ItemPath objectClassPath = new ItemPath(ShadowType.F_OBJECT_CLASS);
		ItemPath resourceRefPath = new ItemPath(ShadowType.F_RESOURCE_REF);
		for (ObjectFilter f : conditions){
			if (f instanceof EqualFilter){
				if (objectClassPath.equivalent(((EqualFilter) f).getFullPath())){
					continue;
				}
				if (resourceRefPath.equivalent(((EqualFilter) f).getFullPath())){
					continue;
				}
				
				attributeFilter.add(f);
			} else if (f instanceof NaryLogicalFilter){
				attributeFilter = getAttributeQuery(((NaryLogicalFilter) f).getConditions(), attributeFilter);
			} else if (f instanceof SubstringFilter){
				attributeFilter.add(f);
			}
			
		}
		
		return attributeFilter;
		
}
	
	////////////////
	
	ConnectorInstance getConnectorInstance(ResourceType resource, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {
		return connectorManager.getConfiguredConnectorInstance(resource.asPrismObject(), false, parentResult);
	}
	
	///////////////////////////////////////////////////////////////////////////
	// TODO: maybe split this to a separate class
	///////////////////////////////////////////////////////////////////////////
	
	public List<Change<ShadowType>> fetchChanges(ResourceType resourceType, 
			QName objectClass, PrismProperty<?> lastToken,  OperationResult parentResult)
			throws ObjectNotFoundException, CommunicationException, GenericFrameworkException, SchemaException,
			ConfigurationException, SecurityViolationException, ObjectAlreadyExistsException {

		InternalMonitor.recordShadowOtherOperation();
		
		RefinedObjectClassDefinition refinedObjectClassDefinition = determineObjectClassDefinition(objectClass, resourceType);
		ConnectorInstance connector = getConnectorInstance(resourceType, parentResult);
		
		List<Change<ShadowType>> changes = null;
		try {

			changes = resouceObjectConverter.fetchChanges(connector, resourceType, refinedObjectClassDefinition, lastToken, parentResult);

			LOGGER.trace("Found {} change(s). Start processing it (them).", changes.size());

			for (Iterator<Change<ShadowType>> i = changes.iterator(); i.hasNext();) {
				// search objects in repository
				Change<ShadowType> change = i.next();
				
				processChange(resourceType, refinedObjectClassDefinition, objectClass, parentResult, change, connector);

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
		} catch (ObjectNotFoundException ex){
			parentResult.recordFatalError("Object not found error: " + ex.getMessage(), ex);
			throw ex;
		} catch (ObjectAlreadyExistsException ex){
			parentResult.recordFatalError("Already exists error: " + ex.getMessage(), ex);
			throw ex;
		}
		parentResult.recordSuccess();
		return changes;
	}
	
	@SuppressWarnings("rawtypes")
	boolean processSynchronization(Change<ShadowType> change, Task task, ResourceType resourceType, String channel, OperationResult result) throws SchemaException, ObjectNotFoundException,
			ObjectAlreadyExistsException {
//		int processedChanges = 0;
//		// for each change from the connector create change description
//		for (Change change : changes) {
//
//			// this is the case,when we want to skip processing of change,
//			// because the shadow was not created or found to the resource
//			// object
//			// it may be caused with the fact, that the object which was
//			// created in the resource was deleted before the sync run
//			// such a change should be skipped to process consistent changes
//			if (change.getOldShadow() == null) {
//				PrismProperty<?> newToken = change.getToken();
//				task.setExtensionProperty(newToken);
//				processedChanges++;
//				LOGGER.debug("Skipping processing change. Can't find appropriate shadow (e.g. the object was deleted on the resource meantime).");
//				continue;
//			}

			ResourceObjectShadowChangeDescription shadowChangeDescription = createResourceShadowChangeDescription(
					change, resourceType, channel);

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("**PROVISIONING: Created resource object shadow change description {}",
						SchemaDebugUtil.prettyPrint(shadowChangeDescription));
			}
			OperationResult notifyChangeResult = new OperationResult(ShadowCache.class.getName()
					+ "notifyChange");
			notifyChangeResult.addParam("resourceObjectShadowChangeDescription", shadowChangeDescription);

			try {
				notifyResourceObjectChangeListeners(shadowChangeDescription, task, notifyChangeResult);
				notifyChangeResult.recordSuccess();
			} catch (RuntimeException ex) {
//				recordFatalError(LOGGER, notifyChangeResult, "Synchronization error: " + ex.getMessage(), ex);
				saveAccountResult(shadowChangeDescription, change, notifyChangeResult, result);
				throw new SystemException("Synchronization error: " + ex.getMessage(), ex);
			}

			notifyChangeResult.computeStatus("Error by notify change operation.");

			boolean successfull = false;
			if (notifyChangeResult.isSuccess()) {
				deleteShadowFromRepo(change, result);
				successfull  = true;
//				// get updated token from change,
//				// create property modification from new token
//				// and replace old token with the new one
//				PrismProperty<?> newToken = change.getToken();
//				task.setExtensionProperty(newToken);
//				processedChanges++;

			} else {
				successfull =false;
				saveAccountResult(shadowChangeDescription, change, notifyChangeResult, result);
			}

			return successfull;
//		}
//		// also if no changes was detected, update token
//		if (changes.isEmpty() && tokenProperty != null) {
//			LOGGER.trace("No changes to synchronize on " + ObjectTypeUtil.toShortString(resourceType));
//			task.setExtensionProperty(tokenProperty);
//		}
//		task.savePendingModifications(result);
//		return processedChanges;
	}
	
	private void notifyResourceObjectChangeListeners(ResourceObjectShadowChangeDescription change,
			Task task, OperationResult parentResult) {
		changeNotificationDispatcher.notifyChange(change, task, parentResult);
	}

	@SuppressWarnings("unchecked")
	private ResourceObjectShadowChangeDescription createResourceShadowChangeDescription(Change<ShadowType> change,
			ResourceType resourceType, String channel) {
		ResourceObjectShadowChangeDescription shadowChangeDescription = new ResourceObjectShadowChangeDescription();
		shadowChangeDescription.setObjectDelta(change.getObjectDelta());
		shadowChangeDescription.setResource(resourceType.asPrismObject());
		shadowChangeDescription.setOldShadow(change.getOldShadow());
		shadowChangeDescription.setCurrentShadow(change.getCurrentShadow());
		if (null == channel){
		shadowChangeDescription.setSourceChannel(QNameUtil.qNameToUri(SchemaConstants.CHANGE_CHANNEL_LIVE_SYNC));
		} else{
			shadowChangeDescription.setSourceChannel(channel);
		}
		return shadowChangeDescription;
	}
	
	@SuppressWarnings("rawtypes")
	private void saveAccountResult(ResourceObjectShadowChangeDescription shadowChangeDescription, Change change,
			OperationResult notifyChangeResult, OperationResult parentResult) throws ObjectNotFoundException,
			SchemaException, ObjectAlreadyExistsException {

		Collection<? extends ItemDelta> shadowModification = createShadowResultModification(change, notifyChangeResult);
		String oid = getOidFromChange(change);
		// maybe better error handling is needed
		try{
		repositoryService.modifyObject(ShadowType.class, oid,
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
	
	private PrismObjectDefinition<ShadowType> getResourceObjectShadowDefinition() {
//		if (resourceObjectShadowDefinition == null) {
			return prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(
					ShadowType.class);
//		}
//		return resourceObjectShadowDefinition;
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

	private void deleteShadowFromRepo(Change change, OperationResult parentResult) throws ObjectNotFoundException {
		if (change.getObjectDelta() != null && change.getObjectDelta().getChangeType() == ChangeType.DELETE
				&& change.getOldShadow() != null) {
			LOGGER.debug("Deleting detected shadow object form repository.");
			try {
				repositoryService.deleteObject(ShadowType.class, change.getOldShadow().getOid(),
						parentResult);
			} catch (ObjectNotFoundException ex) {
				parentResult.recordFatalError("Can't find object " + change.getOldShadow() + " in repository.");
				throw new ObjectNotFoundException("Can't find object " + change.getOldShadow() + " in repository.");
			}
			LOGGER.debug("Shadow object deleted successfully form repository.");
		}
	}


	
	void processChange(ResourceType resourceType, RefinedObjectClassDefinition refinedObjectClassDefinition, QName objectClass, OperationResult parentResult, Change<ShadowType> change, ConnectorInstance connector) throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ObjectNotFoundException, GenericConnectorException, ObjectAlreadyExistsException{
		
		if (refinedObjectClassDefinition == null){
			refinedObjectClassDefinition = determineObjectClassDefinition(objectClass, resourceType);
		}
		
		PrismObject<ShadowType> oldShadow = change.getOldShadow();
		if (oldShadow == null){
			oldShadow = shadowManager.findOrCreateShadowFromChange(resourceType, change, refinedObjectClassDefinition, parentResult);
		}
		if (oldShadow != null) {
			applyAttributesDefinition(oldShadow, resourceType);
			ShadowType oldShadowType = oldShadow.asObjectable();

			LOGGER.trace("Old shadow: {}", oldShadow);

			// skip setting other attribute when shadow is null
			if (oldShadow == null) {
				change.setOldShadow(null);
				return;
			}

			resouceObjectConverter.setProtectedFlag(resourceType, refinedObjectClassDefinition, oldShadow);
			change.setOldShadow(oldShadow);

			if (change.getCurrentShadow() != null) {
				PrismObject<ShadowType> currentShadow = completeShadow(connector, change.getCurrentShadow(), 
						oldShadow, resourceType, refinedObjectClassDefinition, parentResult);
				change.setCurrentShadow(currentShadow);
				ShadowType currentShadowType = currentShadow.asObjectable();
				forceRenameIfNeeded(currentShadowType, oldShadowType, refinedObjectClassDefinition, parentResult);
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


	private void forceRenameIfNeeded(ShadowType currentShadowType, ShadowType oldShadowType, RefinedObjectClassDefinition refinedObjectClassDefinition, OperationResult parentResult) throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
		Collection<ResourceAttribute<?>> oldSecondaryIdentifiers = ShadowUtil.getSecondaryIdentifiers(oldShadowType);
		if (oldSecondaryIdentifiers.isEmpty()){
			return;
		}
		
		if (oldSecondaryIdentifiers.size() > 1){
			return;
		}
		
		ResourceAttribute<?> oldSecondaryIdentifier = oldSecondaryIdentifiers.iterator().next();
		Object oldValue = oldSecondaryIdentifier.getRealValue();

		Collection<ResourceAttribute<?>> newSecondaryIdentifiers = ShadowUtil.getSecondaryIdentifiers(currentShadowType);
		if (newSecondaryIdentifiers.isEmpty()){
			return;
		}
		
		if (newSecondaryIdentifiers.size() > 1){
			return;
		}
		
		ResourceAttribute newSecondaryIdentifier = newSecondaryIdentifiers.iterator().next();
		Object newValue = newSecondaryIdentifier.getRealValue();
		
		if (!shadowManager.compareAttribute(refinedObjectClassDefinition, newSecondaryIdentifier, oldValue)){
			Collection<PropertyDelta> renameDeltas = new ArrayList<PropertyDelta>();
			
			
			PropertyDelta<?> shadowNameDelta = PropertyDelta.createModificationReplaceProperty(ShadowType.F_NAME, 
					oldShadowType.asPrismObject().getDefinition(), 
					ProvisioningUtil.determineShadowName(currentShadowType.asPrismObject()));
			renameDeltas.add(shadowNameDelta);
			
			shadowNameDelta = PropertyDelta.createModificationReplaceProperty(new ItemPath(ShadowType.F_ATTRIBUTES, ConnectorFactoryIcfImpl.ICFS_NAME), oldShadowType.asPrismObject().getDefinition(), newValue);
			renameDeltas.add(shadowNameDelta);
			
			repositoryService.modifyObject(ShadowType.class, oldShadowType.getOid(), renameDeltas, parentResult);
		}
		
	}

	public PrismProperty<?> fetchCurrentToken(ResourceType resourceType, OperationResult parentResult)
			throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException {

		InternalMonitor.recordShadowOtherOperation();
		
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
				if (modification.getDefinition() == null && attributesPath.equivalent(modification.getParentPath())) {
					QName attributeName = modification.getElementName();
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
		} 
		if (objectClassDefinition == null) {
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
			List<? extends ObjectFilter> conditions = ((AndFilter) query.getFilter()).getConditions();
			kind = ProvisioningUtil.getValueFromFilter(conditions, ShadowType.F_KIND);
			intent = ProvisioningUtil.getValueFromFilter(conditions, ShadowType.F_INTENT);
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

		PrismObject<ShadowType> resultShadow = repoShadow.clone();
		boolean resultIsResourceShadowClone = false;
		if (resultShadow == null) {
			resultShadow = resourceShadow.clone();
			resultIsResourceShadowClone = true;
		}
		
		assert resultShadow.getPrismContext() != null : "No prism context in resultShadow";
		
		ResourceAttributeContainer resourceAttributesContainer = ShadowUtil
				.getAttributesContainer(resourceShadow);

		ShadowType resultShadowType = resultShadow.asObjectable();
		ShadowType repoShadowType = repoShadow.asObjectable();
		ShadowType resourceShadowType = resourceShadow.asObjectable();
		
		if (resultShadowType.getObjectClass() == null) {
			resultShadowType.setObjectClass(resourceAttributesContainer.getDefinition().getTypeName());
		}
		if (resultShadowType.getName() == null) {
			resultShadowType.setName(new PolyStringType(ProvisioningUtil.determineShadowName(resourceShadow)));
		}
		if (resultShadowType.getResource() == null) {
			resultShadowType.setResourceRef(ObjectTypeUtil.createObjectRef(resource));
		}

		
		// If the shadows are the same then no copy is needed. This was already copied by clone.
		if (!resultIsResourceShadowClone) {
			// Attributes
			resultShadow.removeContainer(ShadowType.F_ATTRIBUTES);
			ResourceAttributeContainer resultAttibutes = resourceAttributesContainer.clone();
			accessChecker.filterGetAttributes(resultAttibutes, objectClassDefinition, parentResult);
			resultShadow.add(resultAttibutes);
			
//			resultShadowType.setProtectedObject(resourceShadowType.isProtectedObject());
			resultShadowType.setIgnored(resourceShadowType.isIgnored());

			resultShadowType.setActivation(resourceShadowType.getActivation());
			
			// Credentials
			ShadowType resultAccountShadow = resultShadow.asObjectable();
			ShadowType resourceAccountShadow = resourceShadow.asObjectable();
			resultAccountShadow.setCredentials(resourceAccountShadow.getCredentials());
		}
		
		//protected
		resouceObjectConverter.setProtectedFlag(resource, objectClassDefinition, resultShadow);
//		resultShadowType.setProtectedObject();

		// Activation
		ActivationType resultActivationType = resultShadowType.getActivation();
		ActivationType repoActivation = repoShadowType.getActivation();
		if (repoActivation != null) {
            if (resultActivationType == null) {
                resultActivationType = new ActivationType();
                resultShadowType.setActivation(resultActivationType);
            }
			resultActivationType.setId(repoActivation.getId());
			// .. but we want metadata from repo
			resultActivationType.setDisableReason(repoActivation.getDisableReason());
			resultActivationType.setEnableTimestamp(repoActivation.getEnableTimestamp());
			resultActivationType.setDisableTimestamp(repoActivation.getDisableTimestamp());
			resultActivationType.setArchiveTimestamp(repoActivation.getArchiveTimestamp());
			resultActivationType.setValidityChangeTimestamp(repoActivation.getValidityChangeTimestamp());
		}
		
		// Associations
		PrismContainer<ShadowAssociationType> resourceAssociationContainer = resourceShadow.findContainer(ShadowType.F_ASSOCIATION);
		if (resourceAssociationContainer != null) {
			RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resource);
			PrismContainer<ShadowAssociationType> associationContainer = resourceAssociationContainer.clone();
			resultShadow.addReplaceExisting(associationContainer);
			if (associationContainer != null) {
				for (PrismContainerValue<ShadowAssociationType> associationCVal: associationContainer.getValues()) {
					ResourceAttributeContainer identifierContainer = ShadowUtil.getAttributesContainer(associationCVal, ShadowAssociationType.F_IDENTIFIERS);
					Collection<ResourceAttribute<?>> entitlementIdentifiers = identifierContainer.getAttributes();
					if (entitlementIdentifiers == null || entitlementIdentifiers.isEmpty()) {
						throw new IllegalStateException("No entitlement identifiers present for association "+associationCVal);
					}
					ShadowAssociationType shadowAssociationType = associationCVal.asContainerable();
					QName associationName = shadowAssociationType.getName();
					RefinedAssociationDefinition rEntitlementAssociation = objectClassDefinition.findEntitlementAssociation(associationName);
					RefinedObjectClassDefinition entitlementObjectClassDef = refinedSchema.getRefinedDefinition(ShadowKindType.ENTITLEMENT, rEntitlementAssociation.getIntents());
					
					PrismObject<ShadowType> entitlementShadow = (PrismObject<ShadowType>) identifierContainer.getUserData(ResourceObjectConverter.FULL_SHADOW_KEY);
					if (entitlementShadow == null) {
						entitlementShadow = resouceObjectConverter.locateResourceObject(connector, resource, entitlementIdentifiers, entitlementObjectClassDef, parentResult); 
					}
					PrismObject<ShadowType> entitlementRepoShadow = lookupOrCreateShadowInRepository(connector, entitlementShadow, entitlementObjectClassDef, resource, parentResult);
					ObjectReferenceType shadowRefType = new ObjectReferenceType();
					shadowRefType.setOid(entitlementRepoShadow.getOid());
					shadowRefType.setType(ShadowType.COMPLEX_TYPE);
					shadowAssociationType.setShadowRef(shadowRefType);
				}
			}
		}
		
		// Sanity asserts to catch some exotic bugs
		PolyStringType resultName = resultShadow.asObjectable().getName();
		assert resultName != null : "No name generated in "+resultShadow;
		assert !StringUtils.isEmpty(resultName.getOrig()) : "No name (orig) in "+resultShadow;
		assert !StringUtils.isEmpty(resultName.getNorm()) : "No name (norm) in "+resultShadow;

		return resultShadow;
	}
	
	
	
	// ENTITLEMENTS
	
	/**
	 * Makes sure that all the entitlements have identifiers in them so this is usable by the
	 * ResourceObjectConverter.
	 */
	private void preprocessEntitlements(final PrismObject<ShadowType> shadow,  final ResourceType resource,
			final OperationResult result) throws SchemaException, ObjectNotFoundException, ConfigurationException {
		Visitor visitor = new Visitor() {
			@Override
			public void visit(Visitable visitable) {
				try {
					preprocessEntitlement((PrismContainerValue<ShadowAssociationType>)visitable, resource, shadow.toString(), result);
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
			final String desc, final OperationResult result) throws SchemaException, ObjectNotFoundException, ConfigurationException {
		Visitor visitor = new Visitor() {
			@Override
			public void visit(Visitable visitable) {
				try {
					preprocessEntitlement((PrismContainerValue<ShadowAssociationType>)visitable, resource, desc, result);
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
	

	private void preprocessEntitlement(PrismContainerValue<ShadowAssociationType> association, ResourceType resource, String desc, OperationResult result) 
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
			repoShadow = repositoryService.getObject(ShadowType.class, associationType.getShadowRef().getOid(), null, result);
		} catch (ObjectNotFoundException e) {
			throw new ObjectNotFoundException(e.getMessage()+" while resolving entitlement association OID in "+association+" in "+desc, e);
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
