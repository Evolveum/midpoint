/*
 * Copyright (c) 2010-2015 Evolveum
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

import com.evolveum.midpoint.prism.Item;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.identityconnectors.framework.spi.operations.CreateOp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.evolveum.midpoint.common.InternalsConfig;
import com.evolveum.midpoint.common.monitor.InternalMonitor;
import com.evolveum.midpoint.common.refinery.RefinedAssociationDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.CompositeRefinedObjectClassDefinition;
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
import com.evolveum.midpoint.prism.PrismValue;
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
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrFilter;
import com.evolveum.midpoint.prism.query.SubstringFilter;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.provisioning.api.ChangeNotificationDispatcher;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.provisioning.api.ResourceOperationDescription;
import com.evolveum.midpoint.provisioning.consistency.api.ErrorHandler;
import com.evolveum.midpoint.provisioning.consistency.api.ErrorHandler.FailedOperation;
import com.evolveum.midpoint.provisioning.consistency.impl.ErrorHandlerFactory;
import com.evolveum.midpoint.provisioning.impl.ShadowCacheFactory.Mode;
import com.evolveum.midpoint.provisioning.ucf.api.Change;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.ucf.api.PropertyModificationOperation;
import com.evolveum.midpoint.provisioning.ucf.api.ResultHandler;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SearchResultMetadata;
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
import com.evolveum.midpoint.util.Holder;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CountObjectsCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CountObjectsSimulateType;
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
 * Note: These three classes were refactored recently. But it will need more refactoring.
 * It is not a very pretty OO code. But it is better than before.
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
	private ResourceManager resourceManager;
	
	@Autowired(required = true)
	private PrismContext prismContext;
	
	@Autowired(required = true)
	private ResourceObjectConverter resouceObjectConverter;
	
	@Autowired(required = true)
	protected ShadowManager shadowManager;
	
	@Autowired(required = true)
	private ChangeNotificationDispatcher operationListener;
	
	@Autowired(required = true)
	private AccessChecker accessChecker;
	
	@Autowired(required = true)
	private TaskManager taskManager;
	
	@Autowired(required = true)
	private ChangeNotificationDispatcher changeNotificationDispatcher;
	
	@Autowired(required = true)
	private ProvisioningContextFactory ctxFactory;

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

		ProvisioningContext ctx = ctxFactory.create(repositoryShadow, task, parentResult);
		try {
			ctx.assertDefinition();
			applyAttributesDefinition(ctx, repositoryShadow);
		} catch (ObjectNotFoundException | SchemaException |  CommunicationException | ConfigurationException e){
			throw e;
//			String msg = e.getMessage()+" (returning repository shadow)";
//			LOGGER.error("{}", msg, e);
//			parentResult.recordPartialError(msg,  e);
//			return repositoryShadow;
		}
		ResourceType resource = ctx.getResource();
			
		PrismObject<ShadowType> resourceShadow = null;
		try {			
			
			// Let's get all the identifiers from the Shadow <attributes> part
			Collection<? extends ResourceAttribute<?>> identifiers = ShadowUtil.getIdentifiers(repositoryShadow);
			
			if (identifiers == null || identifiers.isEmpty()) {
				//check if the account is not only partially created (exist only in repo so far)
				if (repositoryShadow.asObjectable().getFailedOperationType() != null) {
					throw new GenericConnectorException(
							"Unable to get object from the resource. Probably it has not been created yet because of previous unavailability of the resource.");
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
			
			resourceShadow = resouceObjectConverter.getResourceObject(ctx, identifiers, parentResult);
			
			
			resourceManager.modifyResourceAvailabilityStatus(resource.asPrismObject(), AvailabilityStatusType.UP, parentResult);
			//try to apply changes to the account only if the resource if UP
			if (isCompensate(rootOptions) && repositoryShadow.asObjectable().getObjectChange() != null && repositoryShadow.asObjectable().getFailedOperationType() != null
					&& resource.getOperationalState() != null
					&& resource.getOperationalState().getLastAvailabilityStatus() == AvailabilityStatusType.UP) {
				throw new GenericConnectorException(
						"Found changes that have been not applied to the resource object yet. Trying to apply them now.");
			}
			
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Shadow from repository:\n{}", repositoryShadow.debugDump());
				LOGGER.trace("Resource object fetched from resource:\n{}", resourceShadow.debugDump());
			}
			
			forceRenameIfNeeded(ctx, resourceShadow.asObjectable(), repositoryShadow.asObjectable(), parentResult);
			// Complete the shadow by adding attributes from the resource object
			PrismObject<ShadowType> resultShadow = completeShadow(ctx, resourceShadow, repositoryShadow, parentResult);
			
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Shadow when assembled:\n{}", resultShadow.debugDump());
			}
			
			parentResult.recordSuccess();
			return resultShadow;

			
		} catch (Exception ex) {
			try {
				
				resourceShadow = handleError(ctx, ex, repositoryShadow, FailedOperation.GET, null, isCompensate(rootOptions),
						parentResult);
				
				return resourceShadow;

			} catch (GenericFrameworkException e) {
				throw new SystemException(e);
			} catch (ObjectAlreadyExistsException e) {
				throw new SystemException(e);
			}
		}
		
		
	}
	
	private boolean isCompensate(GetOperationOptions rootOptions){
		return GetOperationOptions.isDoNotDiscovery(rootOptions)? false : true;
	}

	public abstract String afterAddOnResource(ProvisioningContext ctx, PrismObject<ShadowType> shadow, OperationResult parentResult)
					throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ConfigurationException, CommunicationException;
	
	public String addShadow(PrismObject<ShadowType> shadow, OperationProvisioningScriptsType scripts,
			ResourceType resource, ProvisioningOperationOptions options, Task task, OperationResult parentResult) throws CommunicationException,
			GenericFrameworkException, ObjectAlreadyExistsException, SchemaException, ObjectNotFoundException,
			ConfigurationException, SecurityViolationException {
		Validate.notNull(shadow, "Object to add must not be null.");

		InternalMonitor.recordShadowChangeOperation();
		
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Start adding shadow object:\n{}", shadow.debugDump());
		}
	
		ProvisioningContext ctx = ctxFactory.create(shadow, task, parentResult);
		try {
			ctx.assertDefinition();
		} catch (SchemaException e) {
			handleError(ctx, e, shadow, FailedOperation.ADD, null, true, parentResult);
			return null;
		}
		
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Definition:\n{}", ctx.getObjectClassDefinition().debugDump());
		}
		
		PrismContainer<?> attributesContainer = shadow.findContainer(
				ShadowType.F_ATTRIBUTES);
		if (attributesContainer == null || attributesContainer.isEmpty()) {
			SchemaException e= new SchemaException("Attempt to add shadow without any attributes: " + shadow);
			parentResult.recordFatalError(e);
			handleError(ctx, e, shadow, FailedOperation.ADD, null, true, parentResult);
			return null;
		}
		
		try {
			preprocessEntitlements(ctx, shadow, parentResult);
			
			applyAttributesDefinition(ctx, shadow);
            shadowManager.setKindIfNecessary(shadow.asObjectable(), ctx.getObjectClassDefinition());
			accessChecker.checkAdd(ctx, shadow, parentResult);
			shadow = resouceObjectConverter.addResourceObject(ctx, shadow, scripts, parentResult);
			
		} catch (Exception ex) {
			shadow = handleError(ctx, ex, shadow, FailedOperation.ADD, null, ProvisioningOperationOptions.isCompletePostponed(options), parentResult);
			return shadow.getOid();
		}
	
		// This is where the repo shadow is created (if needed) 
		String oid = afterAddOnResource(ctx, shadow, parentResult);
		shadow.setOid(oid);
		
		ObjectDelta<ShadowType> delta = ObjectDelta.createAddDelta(shadow);
		ResourceOperationDescription operationDescription = createSuccessOperationDescription(ctx, shadow, delta, parentResult);
		operationListener.notifySuccess(operationDescription, task, parentResult);
		return oid;
	}

	private ResourceOperationDescription createSuccessOperationDescription(ProvisioningContext ctx, PrismObject<ShadowType> shadowType, ObjectDelta delta, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {
		ResourceOperationDescription operationDescription = new ResourceOperationDescription();
		operationDescription.setCurrentShadow(shadowType);
		operationDescription.setResource(ctx.getResource().asPrismObject());
		if (ctx.getTask() != null){
			operationDescription.setSourceChannel(ctx.getTask().getChannel());
		}
		operationDescription.setObjectDelta(delta);
		operationDescription.setResult(parentResult);
		return operationDescription;
	}

	public abstract void afterModifyOnResource(ProvisioningContext ctx, PrismObject<ShadowType> shadow, 
			Collection<? extends ItemDelta> modifications, OperationResult parentResult) throws SchemaException, ObjectNotFoundException, ConfigurationException, CommunicationException;
	
	public abstract Collection<? extends ItemDelta> beforeModifyOnResource(PrismObject<ShadowType> shadow, ProvisioningOperationOptions options, Collection<? extends ItemDelta> modifications) throws SchemaException;
	
	public String modifyShadow(PrismObject<ShadowType> shadow, String oid,
				Collection<? extends ItemDelta> modifications, OperationProvisioningScriptsType scripts, ProvisioningOperationOptions options, Task task, OperationResult parentResult)
				throws CommunicationException, GenericFrameworkException, ObjectNotFoundException, SchemaException,
				ConfigurationException, SecurityViolationException {

		Validate.notNull(shadow, "Object to modify must not be null.");
		Validate.notNull(oid, "OID must not be null.");
		Validate.notNull(modifications, "Object modification must not be null.");

		InternalMonitor.recordShadowChangeOperation();
		
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Modifying resource shadow:\n{}", shadow.debugDump());
		}
		
		Collection<QName> additionalAuxiliaryObjectClassQNames = new ArrayList<>();
		ItemPath auxPath = new ItemPath(ShadowType.F_AUXILIARY_OBJECT_CLASS);
		for (ItemDelta modification: modifications) {
			if (auxPath.equals(modification.getPath())) {
				PropertyDelta<QName> auxDelta = (PropertyDelta<QName>)modification;
				for (PrismPropertyValue<QName> pval: auxDelta.getValues(QName.class)) {
					additionalAuxiliaryObjectClassQNames.add(pval.getValue());
				}
			}
		}
		
		ProvisioningContext ctx = ctxFactory.create(shadow, additionalAuxiliaryObjectClassQNames, task, parentResult);
		Collection<PropertyModificationOperation> sideEffectChanges;
		try {
			ctx.assertDefinition();
						
			applyAttributesDefinition(ctx, shadow);
			
			accessChecker.checkModify(ctx.getResource(), shadow, modifications, ctx.getObjectClassDefinition(), parentResult);
	
			preprocessEntitlements(ctx, modifications, "delta for shadow "+oid, parentResult);
	
			modifications = beforeModifyOnResource(shadow, options, modifications);
	
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Applying change: {}", DebugUtil.debugDump(modifications));
			}
	
			sideEffectChanges = resouceObjectConverter.modifyResourceObject(ctx, shadow, scripts, modifications,
					parentResult);
		} catch (Exception ex) {
			LOGGER.debug("Provisioning exception: {}:{}, attempting to handle it",
					new Object[] { ex.getClass(), ex.getMessage(), ex });
			try {
				shadow = handleError(ctx, ex, shadow, FailedOperation.MODIFY, modifications,
						ProvisioningOperationOptions.isCompletePostponed(options), parentResult);
				parentResult.computeStatus();
			} catch (ObjectAlreadyExistsException e) {
				parentResult.recordFatalError(
						"While compensating communication problem for modify operation got: "
								+ ex.getMessage(), ex);
				throw new SystemException(e);
			}

			return shadow.getOid();
		}

		Collection<PropertyDelta<PrismPropertyValue>> sideEffectDeltas = convertToPropertyDelta(sideEffectChanges);
		if (!sideEffectDeltas.isEmpty()) {
			PrismUtil.setDeltaOldValue(shadow, sideEffectDeltas);
			ItemDelta.addAll(modifications, (Collection) sideEffectDeltas);
		}
		
		afterModifyOnResource(ctx, shadow, modifications, parentResult);

		ObjectDelta<ShadowType> delta = ObjectDelta.createModifyDelta(shadow.getOid(), modifications, shadow.getCompileTimeClass(), prismContext);
		ResourceOperationDescription operationDescription = createSuccessOperationDescription(ctx, shadow,
				delta, parentResult);
		operationListener.notifySuccess(operationDescription, task, parentResult);
		parentResult.recordSuccess();
		return oid;
	}


	private Collection<PropertyDelta<PrismPropertyValue>> convertToPropertyDelta(
			Collection<PropertyModificationOperation> sideEffectChanges) {
		Collection<PropertyDelta<PrismPropertyValue>> sideEffectDelta = new ArrayList<PropertyDelta<PrismPropertyValue>>();
		if (sideEffectChanges != null) {
			for (PropertyModificationOperation mod : sideEffectChanges){
				sideEffectDelta.add(mod.getPropertyDelta());
			}
		}
		
		return sideEffectDelta;
	}

	public void deleteShadow(PrismObject<ShadowType> shadow, ProvisioningOperationOptions options, OperationProvisioningScriptsType scripts,
			Task task, OperationResult parentResult) throws CommunicationException,
			GenericFrameworkException, ObjectNotFoundException, SchemaException, ConfigurationException,
			SecurityViolationException {

		Validate.notNull(shadow, "Object to delete must not be null.");
		Validate.notNull(parentResult, "Operation result must not be null.");

		InternalMonitor.recordShadowChangeOperation();

		ProvisioningContext ctx = ctxFactory.create(shadow, task, parentResult);
		try {
			ctx.assertDefinition();
		} catch (ObjectNotFoundException ex) {
			// if the force option is set, delete shadow from the repo
			// although the resource does not exists..
			if (ProvisioningOperationOptions.isForce(options)) {
				parentResult.muteLastSubresultError();
				getRepositoryService().deleteObject(ShadowType.class, shadow.getOid(),
						parentResult);
				parentResult.recordHandledError("Resource defined in shadow does not exists. Shadow was deleted from the repository.");
				return;
			} else {
				throw ex;
			}
		}
		
		applyAttributesDefinition(ctx, shadow);
		
		LOGGER.trace("Deleting object {} from the resource {}.", shadow, ctx.getResource());

		if (shadow.asObjectable().getFailedOperationType() == null
				|| (shadow.asObjectable().getFailedOperationType() != null 
					&& FailedOperationTypeType.ADD != shadow.asObjectable().getFailedOperationType())) {
			try {
				resouceObjectConverter.deleteResourceObject(ctx, shadow, scripts, parentResult);
			} catch (Exception ex) {
				try {
					handleError(ctx, ex, shadow, FailedOperation.DELETE, null, ProvisioningOperationOptions.isCompletePostponed(options), parentResult);
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
			ResourceOperationDescription operationDescription = createSuccessOperationDescription(ctx, shadow, delta, parentResult);
			operationListener.notifySuccess(operationDescription, task, parentResult);
		} catch (ObjectNotFoundException ex) {
			parentResult.recordFatalError("Can't delete object " + shadow + ". Reason: " + ex.getMessage(), ex);
			throw new ObjectNotFoundException("An error occured while deleting resource object " + shadow
					+ "whith identifiers " + shadow + ": " + ex.getMessage(),
					ex);
		}
		LOGGER.trace("Object deleted from repository successfully.");
		parentResult.recordSuccess();
		resourceManager.modifyResourceAvailabilityStatus(ctx.getResource().asPrismObject(), AvailabilityStatusType.UP, parentResult);
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
				    shadow = repositoryService.getObject(delta.getObjectTypeClass(), shadowOid, null, parentResult);	// TODO consider fetching only when really necessary
                }
			}
		} else {
			// Delete delta, nothing to do at all
			return;
		}
		ProvisioningContext ctx;
		if (shadow == null) {
			ctx = ctxFactory.create(discriminator, null, parentResult);
			ctx.assertDefinition();
		} else {
			ctx = ctxFactory.create(shadow, null, parentResult);
			ctx.assertDefinition();
		}
		applyAttributesDefinition(ctx, delta);
	}

	public void applyDefinition(PrismObject<ShadowType> shadow, OperationResult parentResult)
			throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException {
		ProvisioningContext ctx = ctxFactory.create(shadow, null, parentResult);
		ctx.assertDefinition();
		applyAttributesDefinition(ctx, shadow);
	}

	public void applyDefinition(final ObjectQuery query, OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException {
		ResourceShadowDiscriminator coordinates = ProvisioningUtil.getCoordinates(query.getFilter());
		ProvisioningContext ctx = ctxFactory.create(coordinates, null, result);
		ctx.assertDefinition();
		applyDefinition(ctx, query);
	}
	
	private void applyDefinition(final ProvisioningContext ctx, final ObjectQuery query) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException {
		if (query == null) {
			return;
		}
		ObjectFilter filter = query.getFilter();
		if (filter == null) {
			return;
		}
		final RefinedObjectClassDefinition objectClassDefinition = ctx.getObjectClassDefinition();
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
							ResourceAttributeDefinition attributeDefinition = objectClassDefinition.findAttributeDefinition(attributeName);
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

	
	
	protected ResourceType getResource(ResourceShadowDiscriminator coords, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {
		String resourceOid = coords.getResourceOid();
		if (resourceOid == null) {
			throw new IllegalArgumentException("No resource OID in " + coords);
		}
		return resourceManager.getResource(resourceOid, parentResult).asObjectable();
	}

	@SuppressWarnings("rawtypes")
	protected PrismObject<ShadowType> handleError(ProvisioningContext ctx, Exception ex, PrismObject<ShadowType> shadow, FailedOperation op,
			Collection<? extends ItemDelta> modifications, boolean compensate, 
			OperationResult parentResult) throws SchemaException, GenericFrameworkException, CommunicationException,
			ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException, SecurityViolationException {

		// do not set result in the shadow in case of get operation, it will
		// resilted to misleading information
		// by get operation we do not modify the result in the shadow, so only
		// fetch result in this case needs to be set
		if (FailedOperation.GET != op) {
			shadow = extendShadow(shadow, parentResult, ctx.getResource(), modifications);
		} else {
			shadow.asObjectable().setResource(ctx.getResource());
		}
		ErrorHandler handler = errorHandlerFactory.createErrorHandler(ex);

		if (handler == null) {
			parentResult.recordFatalError("Error without a handler. Reason: " + ex.getMessage(), ex);
			throw new SystemException(ex.getMessage(), ex);
		}

		LOGGER.debug("Handling provisioning exception {}:{}", new Object[] { ex.getClass(), ex.getMessage() });
		LOGGER.trace("Handling provisioning exception {}:{}", new Object[] { ex.getClass(), ex.getMessage(), ex });

		return handler.handleError(shadow.asObjectable(), op, ex, compensate, ctx.getTask(), parentResult).asPrismObject();

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
	

	public SearchResultMetadata searchObjectsIterative(ObjectQuery query,
			Collection<SelectorOptions<GetOperationOptions>> options, final ShadowHandler<ShadowType> handler,
			final boolean readFromRepository, final OperationResult parentResult) throws SchemaException,
			ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {

		ResourceShadowDiscriminator coordinates = ProvisioningUtil.getCoordinates(query.getFilter());
		final ProvisioningContext ctx = ctxFactory.create(coordinates, null, parentResult);
		ctx.assertDefinition();
		applyDefinition(ctx, query);
		
		GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);
		if (GetOperationOptions.isNoFetch(rootOptions)) {
			return searchObjectsIterativeRepository(ctx, query, options, handler, parentResult);
		}
		
		// We need to record the fetch down here. Now it is certain that we are going to fetch from resource
		// (we do not have raw/noFetch option)
		InternalMonitor.recordShadowFetchOperation();
		
        ObjectQuery attributeQuery = createAttributeQuery(query);

		ResultHandler<ShadowType> resultHandler = new ResultHandler<ShadowType>() {

			@Override
			public boolean handle(PrismObject<ShadowType> resourceShadow) {
				LOGGER.trace("Found resource object {}", SchemaDebugUtil.prettyPrint(resourceShadow));
				PrismObject<ShadowType> resultShadow;
				try {
					ProvisioningContext shadowCtx = reapplyDefinitions(ctx, resourceShadow);
					// Try to find shadow that corresponds to the resource object
					if (readFromRepository) {
						PrismObject<ShadowType> repoShadow = lookupOrCreateShadowInRepository(shadowCtx, resourceShadow, 
								parentResult); 
						
						applyAttributesDefinition(shadowCtx, repoShadow);
						
						forceRenameIfNeeded(shadowCtx, resourceShadow.asObjectable(), repoShadow.asObjectable(), parentResult);

						resultShadow = completeShadow(shadowCtx, resourceShadow, repoShadow, parentResult);

					} else {
						resultShadow = resourceShadow;
					}

					// TODO: better error handling
				} catch (SchemaException e) {
					parentResult.recordFatalError("Schema error: " + e.getMessage(), e);
					LOGGER.error("Schema error: {}", e.getMessage(), e);
					return false;
				} catch (ConfigurationException e) {
					parentResult.recordFatalError("Configuration error: " + e.getMessage(), e);
					LOGGER.error("Configuration error: {}", e.getMessage(), e);
					return false;
				} catch (ObjectNotFoundException | ObjectAlreadyExistsException | CommunicationException 
						| SecurityViolationException | GenericConnectorException e) {
					parentResult.recordFatalError(e.getMessage(), e);
					LOGGER.error("{}", e.getMessage(), e);
					return false;
				}
				
				return handler.handle(resultShadow.asObjectable());
			}

		};

        boolean fetchAssociations = SelectorOptions.hasToLoadPath(ShadowType.F_ASSOCIATION, options);
		
		return resouceObjectConverter.searchResourceObjects(ctx, resultHandler,
                attributeQuery, fetchAssociations, parentResult);
		
	}

    ObjectQuery createAttributeQuery(ObjectQuery query) throws SchemaException {
        ObjectFilter filter = null;
        if (query != null) {
            filter = query.getFilter();
        }

        ObjectQuery attributeQuery = null;
        List<ObjectFilter> attributeFilter = new ArrayList<ObjectFilter>();

        if (filter instanceof AndFilter){
            List<? extends ObjectFilter> conditions = ((AndFilter) filter).getConditions();
            attributeFilter = createAttributeQueryInternal(conditions);
            if (attributeFilter.size() > 1){
                attributeQuery = ObjectQuery.createObjectQuery(AndFilter.createAnd(attributeFilter));
            } else if (attributeFilter.size() < 1){
                LOGGER.trace("No attribute filter defined in the query.");
            } else if (attributeFilter.size() == 1) {
                attributeQuery = ObjectQuery.createObjectQuery(attributeFilter.iterator().next());
            }

        }

        if (query != null && query.getPaging() != null){
            if (attributeQuery == null){
                attributeQuery = new ObjectQuery();
            }
            attributeQuery.setPaging(query.getPaging());
        }
        if (query != null && query.isAllowPartialResults()) {
        	if (attributeQuery == null){
                attributeQuery = new ObjectQuery();
            }
        	attributeQuery.setAllowPartialResults(true);
        }
        
        if (InternalsConfig.consistencyChecks && attributeQuery != null && attributeQuery.getFilter() != null) {
        	attributeQuery.getFilter().checkConsistence();
        }
        return attributeQuery;
    }
    
    private List<ObjectFilter> createAttributeQueryInternal(List<? extends ObjectFilter> conditions) throws SchemaException{
		List<ObjectFilter> attributeFilter = new ArrayList<>();
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
				List<ObjectFilter> subFilters = createAttributeQueryInternal(((NaryLogicalFilter) f).getConditions());
	            if (subFilters.size() > 1){
	            	if (f instanceof OrFilter){
						attributeFilter.add(OrFilter.createOr(subFilters));
					} else if (f instanceof AndFilter){
						attributeFilter.add(AndFilter.createAnd(subFilters));
					} else {
						throw new IllegalArgumentException("Could not translate query filter. Unknow type: " + f);
					}
	            } else if (subFilters.size() < 1){
	                continue;
	            } else if (subFilters.size() == 1) {
	            	attributeFilter.add(subFilters.iterator().next());
	            }
				
			} else if (f instanceof SubstringFilter){
				attributeFilter.add(f);
			}
			
		}
		
		return attributeFilter;	
	}

    private SearchResultMetadata searchObjectsIterativeRepository(
    		final ProvisioningContext ctx, ObjectQuery query,
			Collection<SelectorOptions<GetOperationOptions>> options,
			final ShadowHandler<ShadowType> shadowHandler, OperationResult parentResult) throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException {
		
		com.evolveum.midpoint.schema.ResultHandler<ShadowType> repoHandler = new com.evolveum.midpoint.schema.ResultHandler<ShadowType>() {
			@Override
			public boolean handle(PrismObject<ShadowType> object,
					OperationResult parentResult) {
				try {
					applyAttributesDefinition(ctx, object);
					resouceObjectConverter.setProtectedFlag(ctx, object);		// fixing MID-1640; hoping that the protected object filter uses only identifiers (that are stored in repo)
					boolean cont = shadowHandler.handle(object.asObjectable());
					parentResult.recordSuccess();
					return cont;
				} catch (RuntimeException e) {
					parentResult.recordFatalError(e);
					throw e;
				} catch (SchemaException | ConfigurationException | ObjectNotFoundException | CommunicationException e) {
					parentResult.recordFatalError(e);
					throw new SystemException(e);
				}
			}
		};
		
		return shadowManager.searchObjectsIterativeRepository(ctx, query, options, repoHandler, parentResult);
	}

	private PrismObject<ShadowType> lookupOrCreateShadowInRepository(ProvisioningContext ctx, PrismObject<ShadowType> resourceShadow,
			OperationResult parentResult) 
					throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, SecurityViolationException, GenericConnectorException {
		PrismObject<ShadowType> repoShadow = shadowManager.lookupShadowInRepository(ctx, resourceShadow, parentResult);

		if (repoShadow == null) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace(
						"Shadow object (in repo) corresponding to the resource object (on the resource) was not found. The repo shadow will be created. The resource object:\n{}",
						SchemaDebugUtil.prettyPrint(resourceShadow));
			}

			repoShadow = createShadowInRepository(ctx, resourceShadow, parentResult);
		} else {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Found shadow object in the repository {}", SchemaDebugUtil.prettyPrint(repoShadow));
			}
		}
		
		return repoShadow;
	}

	private PrismObject<ShadowType> createShadowInRepository(ProvisioningContext ctx, PrismObject<ShadowType> resourceShadow,
			OperationResult parentResult) 
					throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, SecurityViolationException, GenericConnectorException {
		
		PrismObject<ShadowType> repoShadow;
		PrismObject<ShadowType> conflictingShadow = shadowManager.lookupShadowBySecondaryIdentifiers(ctx, resourceShadow, parentResult);
		if (conflictingShadow != null){
			applyAttributesDefinition(ctx, conflictingShadow);
			conflictingShadow = completeShadow(ctx, resourceShadow, conflictingShadow, parentResult);
			Task task = taskManager.createTaskInstance();
			ResourceOperationDescription failureDescription = shadowManager.createResourceFailureDescription(conflictingShadow, ctx.getResource(), parentResult);
			changeNotificationDispatcher.notifyFailure(failureDescription, task, parentResult);
			shadowManager.deleteConflictedShadowFromRepo(conflictingShadow, parentResult);
		}
		// TODO: make sure that the resource object has appropriate definition (use objectClass and schema)
		// The resource object obviously exists on the resource, but appropriate shadow does not exist in the
		// repository we need to create the shadow to align repo state to the reality (resource)

		try {

			repoShadow =  shadowManager.addRepositoryShadow(ctx, resourceShadow, parentResult);
			
		} catch (ObjectAlreadyExistsException e) {
			// This should not happen. We haven't supplied an OID so is should not conflict
			LOGGER.error("Unexpected repository behavior: Object already exists: {}", e.getMessage(), e);
			throw new SystemException("Unexpected repository behavior: Object already exists: "+e.getMessage(),e);
		}

		return repoShadow;
	}
	
	public Integer countObjects(ObjectQuery query, final OperationResult result) throws SchemaException,
			ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {

		ResourceShadowDiscriminator coordinates = ProvisioningUtil.getCoordinates(query.getFilter());
		final ProvisioningContext ctx = ctxFactory.create(coordinates, null, result);
		ctx.assertDefinition();
		applyDefinition(ctx, query);
	
		RefinedObjectClassDefinition objectClassDef = ctx.getObjectClassDefinition();
		ResourceType resourceType = ctx.getResource();
        CountObjectsCapabilityType countObjectsCapabilityType = objectClassDef.getEffectiveCapability(CountObjectsCapabilityType.class);
        if (countObjectsCapabilityType == null) {
        	// Unable to count. Return null which means "I do not know"
        	result.recordNotApplicableIfUnknown();
    		return null;
        } else {
        	CountObjectsSimulateType simulate = countObjectsCapabilityType.getSimulate();
        	if (simulate == null) {
        		// We have native capability
        		
        		ConnectorInstance connector = ctx.getConnector(result);
                try {
                    ObjectQuery attributeQuery = createAttributeQuery(query);
                    int count;
                    try {
                    	count = connector.count(objectClassDef.getObjectClassDefinition(), attributeQuery, objectClassDef.getPagedSearches(), result);
                    } catch (CommunicationException | GenericFrameworkException| SchemaException | UnsupportedOperationException e) {
                    	result.recordFatalError(e);
                        throw e;
                    }
                    result.computeStatus();
    	            result.cleanupResult();
    	            return count;
                } catch (GenericFrameworkException|UnsupportedOperationException e) {
                    SystemException ex = new SystemException("Couldn't count objects on resource " + resourceType + ": " + e.getMessage(), e);
                    result.recordFatalError(ex);
                    throw ex;
                }
                
        	} else if (simulate == CountObjectsSimulateType.PAGED_SEARCH_ESTIMATE) {
        		
        		if (!objectClassDef.isPagedSearchEnabled()) {
        			throw new ConfigurationException("Configured count object capability to be simulated using a paged search but paged search capability is not present");
        		}
        		
        		final Holder<Integer> countHolder = new Holder<Integer>(0);
        		
	            final ShadowHandler<ShadowType> handler = new ShadowHandler<ShadowType>() {
	                @Override
	                public boolean handle(ShadowType object) {
	                    int count = countHolder.getValue();
	                    count++;
	                    countHolder.setValue(count);
	                    return true;
	                }
	            };
	
	            query = query.clone();
	            ObjectPaging paging = ObjectPaging.createEmptyPaging();
	            paging.setMaxSize(1);
				query.setPaging(paging);
				Collection<SelectorOptions<GetOperationOptions>> options =
						SelectorOptions.createCollection(new ItemPath(ShadowType.F_ASSOCIATION), GetOperationOptions.createRetrieve(RetrieveOption.EXCLUDE));
				SearchResultMetadata resultMetadata;
				try {
					resultMetadata = searchObjectsIterative(query, options, handler, false, result);
				} catch (SchemaException | ObjectNotFoundException | ConfigurationException | SecurityViolationException e) {
					result.recordFatalError(e);
                    throw e;
				}
	            result.computeStatus();
	            result.cleanupResult();
	            
        		return resultMetadata.getApproxNumberOfAllResults();
                
        	} else if (simulate == CountObjectsSimulateType.SEQUENTIAL_SEARCH) {
        		
        		// traditional way of counting objects (i.e. counting them one by one)
	            final Holder<Integer> countHolder = new Holder<Integer>(0);
	
	            final ShadowHandler<ShadowType> handler = new ShadowHandler<ShadowType>() {
	                @Override
	                public boolean handle(ShadowType object) {
	                    int count = countHolder.getValue();
	                    count++;
	                    countHolder.setValue(count);
	                    return true;
	                }
	            };
	
				Collection<SelectorOptions<GetOperationOptions>> options =
						SelectorOptions.createCollection(new ItemPath(ShadowType.F_ASSOCIATION), GetOperationOptions.createRetrieve(RetrieveOption.EXCLUDE));
	            searchObjectsIterative(query, options, handler, false, result);
	            // TODO: better error handling
	            result.computeStatus();
	            result.cleanupResult();
	            return countHolder.getValue();
        		
        	} else {
        		throw new IllegalArgumentException("Unknown count capability simulate type "+simulate);
        	}
        }

	}
	
	
	///////////////////////////////////////////////////////////////////////////
	// TODO: maybe split this to a separate class
	///////////////////////////////////////////////////////////////////////////
	
	public int synchronize(ResourceShadowDiscriminator shadowCoordinates, PrismProperty<?> lastToken,  
			Task task, OperationResult parentResult)
			throws ObjectNotFoundException, CommunicationException, GenericFrameworkException, SchemaException,
			ConfigurationException, SecurityViolationException, ObjectAlreadyExistsException {

		InternalMonitor.recordShadowOtherOperation();

		final ProvisioningContext ctx = ctxFactory.create(shadowCoordinates, task, parentResult);
		
		List<Change<ShadowType>> changes = null;
		try {

			changes = resouceObjectConverter.fetchChanges(ctx, lastToken, parentResult);

			LOGGER.trace("Found {} change(s). Start processing it (them).", changes.size());

			int processedChanges = 0;
			
			for (Change<ShadowType> change: changes) {	
				
				if (change.isTokenOnly()) {
					LOGGER.trace("Found token-only change: {}", change);
					task.setExtensionProperty(change.getToken());
					continue;
				}
				
				ObjectClassComplexTypeDefinition changeObjectClassDefinition = change.getObjectClassDefinition();
				
				ProvisioningContext shadowCtx;
				PrismObject<ShadowType> oldShadow = null;
				if (changeObjectClassDefinition == null) {
					if (change.getObjectDelta() != null && change.getObjectDelta().isDelete()) {				
						oldShadow = change.getOldShadow();
						if (oldShadow == null) {
							oldShadow = shadowManager.findOrAddShadowFromChangeGlobalContext(ctx, change, parentResult);
						}
						if (oldShadow == null) {
							LOGGER.debug("No old shadow for delete synchronization event {}, we probably did not know about that object anyway, so well be ignoring this event", change);
							continue;
						}
						shadowCtx = ctx.spawn(oldShadow);
					} else {
						throw new SchemaException("No object class definition in change " + change);
					}
				} else {
					shadowCtx = ctx.spawn(changeObjectClassDefinition.getTypeName());
				}
				
				processChange(shadowCtx, change, oldShadow, parentResult);
				
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
				boolean isSuccess = processSynchronization(shadowCtx, change, parentResult);

				if (isSuccess) {
//					// get updated token from change,
//					// create property modification from new token
//					// and replace old token with the new one
					PrismProperty<?> newToken = change.getToken();
					task.setExtensionProperty(newToken);
					processedChanges++;
				}
			}
			
			// also if no changes was detected, update token
			if (changes.isEmpty() && lastToken != null) {
				LOGGER.trace("No changes to synchronize on " + ctx.getResource());
				task.setExtensionProperty(lastToken);
			}
			task.savePendingModifications(parentResult);
			return processedChanges;

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
	}
	
	@SuppressWarnings("rawtypes") boolean processSynchronization(ProvisioningContext ctx, Change<ShadowType> change, OperationResult result) throws SchemaException, ObjectNotFoundException,
			ObjectAlreadyExistsException, CommunicationException, ConfigurationException {
			ResourceObjectShadowChangeDescription shadowChangeDescription = createResourceShadowChangeDescription(
					change, ctx.getResource(), ctx.getChannel());

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("**PROVISIONING: Created resource object shadow change description {}",
						SchemaDebugUtil.prettyPrint(shadowChangeDescription));
			}
			OperationResult notifyChangeResult = new OperationResult(ShadowCache.class.getName()
					+ "notifyChange");
			notifyChangeResult.addParam("resourceObjectShadowChangeDescription", shadowChangeDescription);

			try {
				notifyResourceObjectChangeListeners(shadowChangeDescription, ctx.getTask(), notifyChangeResult);
				notifyChangeResult.recordSuccess();
			} catch (RuntimeException ex) {
//				recordFatalError(LOGGER, notifyChangeResult, "Synchronization error: " + ex.getMessage(), ex);
				saveAccountResult(shadowChangeDescription, change, notifyChangeResult, result);
				throw new SystemException("Synchronization error: " + ex.getMessage(), ex);
			}

			notifyChangeResult.computeStatus("Error in notify change operation.");

			boolean successfull = false;
			if (notifyChangeResult.isSuccess() || notifyChangeResult.isHandledError()) {
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
		try {
			ConstraintsChecker.onShadowModifyOperation(shadowModification);
			repositoryService.modifyObject(ShadowType.class, oid, shadowModification, parentResult);
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


	
	void processChange(ProvisioningContext ctx, Change<ShadowType> change, PrismObject<ShadowType> oldShadow, OperationResult parentResult) 
					throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ObjectNotFoundException, GenericConnectorException, ObjectAlreadyExistsException{
				
		if (oldShadow == null) {
			oldShadow = shadowManager.findOrAddShadowFromChange(ctx, change, parentResult);
		}
		
		if (oldShadow != null) {
			applyAttributesDefinition(ctx, oldShadow);
			ShadowType oldShadowType = oldShadow.asObjectable();

			LOGGER.trace("Old shadow: {}", oldShadow);

			// skip setting other attribute when shadow is null
			if (oldShadow == null) {
				change.setOldShadow(null);
				return;
			}

			resouceObjectConverter.setProtectedFlag(ctx, oldShadow);
			change.setOldShadow(oldShadow);

			if (change.getCurrentShadow() != null) {
				PrismObject<ShadowType> currentShadow = completeShadow(ctx, change.getCurrentShadow(), 
						oldShadow, parentResult);
				change.setCurrentShadow(currentShadow);
				ShadowType currentShadowType = currentShadow.asObjectable();
				forceRenameIfNeeded(ctx, currentShadowType, oldShadowType, parentResult);
			}

			// FIXME: hack. the object delta must have oid specified.
			if (change.getObjectDelta() != null && change.getObjectDelta().getOid() == null) {
//				if (LOGGER.isTraceEnabled()) {
//					LOGGER.trace("No OID present, assuming delta of type DELETE; change = {}\nobjectDelta: {}", change, change.getObjectDelta().debugDump());
//				}
//				ObjectDelta<ShadowType> objDelta = new ObjectDelta<ShadowType>(ShadowType.class, ChangeType.DELETE, prismContext);
//				change.setObjectDelta(objDelta);
				change.getObjectDelta().setOid(oldShadow.getOid());
			}
		} else {
			LOGGER.debug("No old shadow for synchronization event {}, the shadow must be gone in the meantime (this is probably harmless)", change);
		}

	}


	private void forceRenameIfNeeded(ProvisioningContext ctx, ShadowType currentShadowType, ShadowType oldShadowType, OperationResult parentResult) throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException, CommunicationException {
		Collection<ResourceAttribute<?>> oldSecondaryIdentifiers = ShadowUtil.getSecondaryIdentifiers(oldShadowType);
		if (oldSecondaryIdentifiers.isEmpty()){
			return;
		}
		ResourceAttributeContainer newSecondaryIdentifiers = ShadowUtil.getAttributesContainer(currentShadowType);
		
		//remember name before normalizing attributes
		PolyString currentShadowName = ProvisioningUtil.determineShadowName(currentShadowType);
		currentShadowType.setName(new PolyStringType(currentShadowName));
		
		Iterator<ResourceAttribute<?>> oldSecondaryIterator = oldSecondaryIdentifiers.iterator();
		Collection<PropertyDelta> renameDeltas = new ArrayList<PropertyDelta>();
		while (oldSecondaryIterator.hasNext()){
			ResourceAttribute<?> oldSecondaryIdentifier = oldSecondaryIterator.next();
			ResourceAttribute newSecondaryIdentifier = newSecondaryIdentifiers.findAttribute(oldSecondaryIdentifier.getElementName());
			Collection newValue = newSecondaryIdentifier.getRealValues();
			
			if (!shadowManager.compareAttribute(ctx.getObjectClassDefinition(), newSecondaryIdentifier, oldSecondaryIdentifier)){
				PropertyDelta<?> shadowNameDelta = PropertyDelta.createDelta(new ItemPath(ShadowType.F_ATTRIBUTES, oldSecondaryIdentifier.getElementName()), oldShadowType.asPrismObject().getDefinition());
				shadowNameDelta.addValuesToDelete(PrismPropertyValue.cloneCollection((Collection)oldSecondaryIdentifier.getValues()));
				shadowManager.normalizeAttributes(currentShadowType.asPrismObject(), ctx.getObjectClassDefinition());
				shadowNameDelta.addValuesToAdd(PrismPropertyValue.cloneCollection((Collection)newSecondaryIdentifier.getValues()));
				renameDeltas.add(shadowNameDelta);
			}

		}
		
		if (!renameDeltas.isEmpty()){
		
			PropertyDelta<?> shadowNameDelta = PropertyDelta.createModificationReplaceProperty(ShadowType.F_NAME, 
					oldShadowType.asPrismObject().getDefinition(),currentShadowName);
			renameDeltas.add(shadowNameDelta);
		} else {
			
			if (!oldShadowType.getName().getOrig().equals(currentShadowType.getName().getOrig())){
				PropertyDelta<?> shadowNameDelta = PropertyDelta.createModificationReplaceProperty(ShadowType.F_NAME, 
						oldShadowType.asPrismObject().getDefinition(), currentShadowName);
				renameDeltas.add(shadowNameDelta);
				
			}
		}
		if (!renameDeltas.isEmpty()){
			ConstraintsChecker.onShadowModifyOperation(renameDeltas);
			repositoryService.modifyObject(ShadowType.class, oldShadowType.getOid(), renameDeltas, parentResult);
			oldShadowType.setName(new PolyStringType(currentShadowName));
		}

	}

	public PrismProperty<?> fetchCurrentToken(ResourceShadowDiscriminator shadowCoordinates, OperationResult parentResult)
			throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException {
		Validate.notNull(parentResult, "Operation result must not be null.");

		InternalMonitor.recordShadowOtherOperation();		

		ProvisioningContext ctx = ctxFactory.create(shadowCoordinates, null, parentResult);
				
		LOGGER.trace("Getting last token");
		PrismProperty<?> lastToken = null;
		try {
			lastToken = resouceObjectConverter.fetchCurrentToken(ctx, parentResult);
		} catch (CommunicationException | ConfigurationException e) {
			parentResult.recordFatalError(e.getMessage(), e);
			throw e;
		}

		LOGGER.trace("Got last token: {}", SchemaDebugUtil.prettyPrint(lastToken));
		parentResult.recordSuccess();
		return lastToken;
	}
	
	
	//////////////////////////////////////////////////////////////////////////////////////
	
	
//	public ObjectClassComplexTypeDefinition applyAttributesDefinition(ProvisioningContext ctx, ObjectDelta<ShadowType> delta, 
//			PrismObject<ShadowType> shadow) throws SchemaException, ConfigurationException {
//		RefinedResourceSchema refinedSchema = ProvisioningUtil.getRefinedSchema(resource);
//		ObjectClassComplexTypeDefinition objectClassDefinition = refinedSchema.determineCompositeObjectClassDefinition(shadow);
//		return applyAttributesDefinition(delta, objectClassDefinition, resource, refinedSchema);
//	}

	private void applyAttributesDefinition(ProvisioningContext ctx, ObjectDelta<ShadowType> delta) throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException {
		if (delta.isAdd()) {
			applyAttributesDefinition(ctx, delta.getObjectToAdd());
		} else if (delta.isModify()) {
			for(ItemDelta<?,?> itemDelta: delta.getModifications()) {
				if (SchemaConstants.PATH_ATTRIBUTES.equivalent(itemDelta.getParentPath())) {
					applyAttributeDefinition(ctx, delta, itemDelta);
				} else if (SchemaConstants.PATH_ATTRIBUTES.equivalent(itemDelta.getPath())) {
					if (itemDelta.isAdd()) {
						for (PrismValue value : itemDelta.getValuesToAdd()) {
							applyAttributeDefinition(ctx, value);
						}
					}
					if (itemDelta.isReplace()) {
						for (PrismValue value : itemDelta.getValuesToReplace()) {
							applyAttributeDefinition(ctx, value);
						}
					}
				}
			}
		}
	}

	// value should be a value of attributes container
	private void applyAttributeDefinition(ProvisioningContext ctx, PrismValue value)
			throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException {
		if (!(value instanceof PrismContainerValue)) {
			return;		// should never occur
		}
		PrismContainerValue<ShadowAttributesType> pcv = (PrismContainerValue<ShadowAttributesType>) value;
		for (Item item: pcv.getItems()) {
			ItemDefinition itemDef = item.getDefinition();
			if (itemDef == null || !(itemDef instanceof ResourceAttributeDefinition)) {
				QName attributeName = item.getElementName();
				ResourceAttributeDefinition attributeDefinition = ctx.getObjectClassDefinition().findAttributeDefinition(attributeName);
				if (attributeDefinition == null) {
					throw new SchemaException("No definition for attribute " + attributeName);
				}
				if (itemDef != null) {
					// We are going to rewrite the definition anyway. Let's just do some basic checks first
					if (!QNameUtil.match(itemDef.getTypeName(), attributeDefinition.getTypeName())) {
						throw new SchemaException("The value of type " + itemDef.getTypeName() + " cannot be applied to attribute " + attributeName + " which is of type " + attributeDefinition.getTypeName());
					}
				}
				item.applyDefinition(attributeDefinition);
			}
		}
	}

	private <V extends PrismValue, D extends ItemDefinition> void applyAttributeDefinition(ProvisioningContext ctx, 
			ObjectDelta<ShadowType> delta, ItemDelta<V, D> itemDelta) throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException {
		if (!SchemaConstants.PATH_ATTRIBUTES.equivalent(itemDelta.getParentPath())) {		// just to be sure
			return;
		}
		D itemDef = itemDelta.getDefinition();
		if (itemDef == null || !(itemDef instanceof ResourceAttributeDefinition)) {
			QName attributeName = itemDelta.getElementName();
			ResourceAttributeDefinition attributeDefinition = ctx.getObjectClassDefinition().findAttributeDefinition(attributeName);
			if (attributeDefinition == null) {
				throw new SchemaException("No definition for attribute "+attributeName+" in object delta "+delta);
			}
			if (itemDef != null) {
				// We are going to rewrite the definition anyway. Let's just do some basic checks first
				if (!QNameUtil.match(itemDef.getTypeName(),attributeDefinition.getTypeName())) {
					throw new SchemaException("The value of type "+itemDef.getTypeName()+" cannot be applied to attribute "+attributeName+" which is of type "+attributeDefinition.getTypeName());
				}
			}
			itemDelta.applyDefinition((D) attributeDefinition);
		}
	}

	private RefinedObjectClassDefinition applyAttributesDefinition(ProvisioningContext ctx, PrismObject<ShadowType> shadow)
			throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException {
		RefinedObjectClassDefinition objectClassDefinition =  ctx.getObjectClassDefinition();

		PrismContainer<ShadowAttributesType> attributesContainer = shadow.findContainer(ShadowType.F_ATTRIBUTES);
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
	
	/**
	 * Reapplies definition to the shadow if needed. The definition needs to be reapplied e.g.
	 * if the shadow has auxiliary object classes, it if subclass of the object class that was originally
	 * requested, etc. 
	 */
	private ProvisioningContext reapplyDefinitions(ProvisioningContext ctx, PrismObject<ShadowType> rawResourceShadow) throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException {
		ShadowType rawResourceShadowType = rawResourceShadow.asObjectable();
		QName objectClassQName = rawResourceShadowType.getObjectClass();
		List<QName> auxiliaryObjectClassQNames = rawResourceShadowType.getAuxiliaryObjectClass();
		if (auxiliaryObjectClassQNames.isEmpty() && objectClassQName.equals(ctx.getObjectClassDefinition().getTypeName())) {
			// shortcut, no need to reapply anything
			return ctx;
		}
		ProvisioningContext shadowCtx = ctx.spawn(rawResourceShadow);
		shadowCtx.assertDefinition();
		RefinedObjectClassDefinition shadowDef = shadowCtx.getObjectClassDefinition();
		ResourceAttributeContainer attributesContainer = ShadowUtil.getAttributesContainer(rawResourceShadow);
		attributesContainer.applyDefinition(shadowDef.toResourceAttributeContainerDefinition());
		return shadowCtx;
	}
	
	/**
	 * Make sure that the shadow is complete, e.g. that all the mandatory fields
	 * are filled (e.g name, resourceRef, ...) Also transforms the shadow with
	 * respect to simulated capabilities.
	 */
	private PrismObject<ShadowType> completeShadow(ProvisioningContext ctx, PrismObject<ShadowType> resourceShadow,
			PrismObject<ShadowType> repoShadow, OperationResult parentResult) 
					throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, SecurityViolationException, GenericConnectorException {
		
		PrismObject<ShadowType> resultShadow = repoShadow.clone();
		boolean resultIsResourceShadowClone = false;
		if (resultShadow == null) {         // todo how could this happen (see above)? [mederly]
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
			resultShadowType.setResourceRef(ObjectTypeUtil.createObjectRef(ctx.getResource()));
		}

		
		// If the shadows are the same then no copy is needed. This was already copied by clone.
		if (!resultIsResourceShadowClone) {
			// Attributes
			resultShadow.removeContainer(ShadowType.F_ATTRIBUTES);
			ResourceAttributeContainer resultAttibutes = resourceAttributesContainer.clone();
			accessChecker.filterGetAttributes(resultAttibutes, ctx.getObjectClassDefinition(), parentResult);
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
		resouceObjectConverter.setProtectedFlag(ctx, resultShadow);
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
					RefinedAssociationDefinition rEntitlementAssociation = ctx.getObjectClassDefinition().findEntitlementAssociation(associationName);
					for (String intent: rEntitlementAssociation.getIntents()) {
						ProvisioningContext ctxEntitlement = ctx.spawn(ShadowKindType.ENTITLEMENT, intent);
						
						PrismObject<ShadowType> entitlementRepoShadow;
						PrismObject<ShadowType> entitlementShadow = (PrismObject<ShadowType>) identifierContainer.getUserData(ResourceObjectConverter.FULL_SHADOW_KEY);
						if (entitlementShadow == null) {
							try {
								entitlementRepoShadow = shadowManager.lookupShadowInRepository(ctxEntitlement, identifierContainer, parentResult);
								if (entitlementRepoShadow == null) {								
									entitlementShadow = resouceObjectConverter.locateResourceObject(ctxEntitlement, entitlementIdentifiers, parentResult);
									entitlementRepoShadow = createShadowInRepository(ctxEntitlement, entitlementShadow, parentResult);
								}
							} catch (ObjectNotFoundException e) {
								// The entitlement to which we point is not there.
								// Simply ignore this association value.
								parentResult.muteLastSubresultError();
								LOGGER.warn("The entitlement identified by {} referenced from {} does not exist. Skipping.",
										new Object[]{associationCVal, resourceShadow});
								continue;
							} catch (SchemaException e) {
								// The entitlement to which we point is not bad.
								// Simply ignore this association value.
								parentResult.muteLastSubresultError();
								LOGGER.warn("The entitlement identified by {} referenced from {} violates the schema. Skipping. Original error: {}",
										new Object[]{associationCVal, resourceShadow, e.getMessage(), e});
								continue;
							}
						} else {
							entitlementRepoShadow = lookupOrCreateShadowInRepository(ctxEntitlement, entitlementShadow, parentResult);
						}
						ObjectReferenceType shadowRefType = new ObjectReferenceType();
						shadowRefType.setOid(entitlementRepoShadow.getOid());
						shadowRefType.setType(ShadowType.COMPLEX_TYPE);
						shadowAssociationType.setShadowRef(shadowRefType);
					}
				}
			}
		}
		
		resultShadowType.setCachingMetadata(resourceShadowType.getCachingMetadata());
		
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
	private void preprocessEntitlements(final ProvisioningContext ctx, final PrismObject<ShadowType> shadow,
			final OperationResult result) throws SchemaException, ObjectNotFoundException, ConfigurationException, CommunicationException {
		Visitor visitor = new Visitor() {
			@Override
			public void visit(Visitable visitable) {
				try {
					preprocessEntitlement(ctx, (PrismContainerValue<ShadowAssociationType>)visitable, shadow.toString(), result);
				} catch (SchemaException | ObjectNotFoundException | ConfigurationException | CommunicationException e) {
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
			} else if (cause instanceof CommunicationException) {
				throw (CommunicationException)cause;
			} else {
				throw new SystemException("Unexpected exception "+cause, cause);
			}
		}
	}
	
	/**
	 * Makes sure that all the entitlements have identifiers in them so this is usable by the
	 * ResourceObjectConverter. 
	 */	
	private void preprocessEntitlements(final ProvisioningContext ctx, Collection<? extends ItemDelta> modifications, 
			final String desc, final OperationResult result) 
					throws SchemaException, ObjectNotFoundException, ConfigurationException, CommunicationException {
		Visitor visitor = new Visitor() {
			@Override
			public void visit(Visitable visitable) {
				try {
					preprocessEntitlement(ctx, (PrismContainerValue<ShadowAssociationType>)visitable, desc, result);
				} catch (SchemaException | ObjectNotFoundException | ConfigurationException | CommunicationException e) {
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
			} else if (cause instanceof CommunicationException) {
				throw (CommunicationException)cause;
			} else {
				throw new SystemException("Unexpected exception "+cause, cause);
			}
		}
	}
	

	private void preprocessEntitlement(ProvisioningContext ctx, PrismContainerValue<ShadowAssociationType> association, 
			String desc, OperationResult result) 
			throws SchemaException, ObjectNotFoundException, ConfigurationException, CommunicationException {
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
		applyAttributesDefinition(ctx, repoShadow);
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
