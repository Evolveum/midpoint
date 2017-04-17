/*
 * Copyright (c) 2010-2017 Evolveum
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

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.common.refinery.RefinedAssociationDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.ShadowDiscriminatorObjectDelta;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.api.*;
import com.evolveum.midpoint.provisioning.consistency.api.ErrorHandler;
import com.evolveum.midpoint.provisioning.consistency.api.ErrorHandler.FailedOperation;
import com.evolveum.midpoint.provisioning.consistency.impl.ErrorHandlerFactory;
import com.evolveum.midpoint.provisioning.ucf.api.Change;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.ucf.api.ResultHandler;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.AsynchronousOperationResult;
import com.evolveum.midpoint.schema.result.AsynchronousOperationReturnValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CountObjectsCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CountObjectsSimulateType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ReadCapabilityType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Shadow cache is a facade that covers all the operations with shadows. It
 * takes care of splitting the operations between repository and resource,
 * merging the data back, handling the errors and generally controlling the
 * process.
 * 
 * The two principal classes that do the operations are:
 * ResourceObjectConvertor: executes operations on resource ShadowManager:
 * executes operations in the repository
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
	private Clock clock;

	@Autowired(required = true)
	private PrismContext prismContext;

	@Autowired(required = true)
	private ResourceObjectConverter resouceObjectConverter;

	@Autowired(required = true)
	private MatchingRuleRegistry matchingRuleRegistry;

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
	 * DO NOT USE. Only ShadowManager shoudl access repository
	 * 
	 * @return the value of repositoryService
	 */
	@Deprecated
	public RepositoryService getRepositoryService() {
		return repositoryService;
	}

	public PrismContext getPrismContext() {
		return prismContext;
	}

	public PrismObject<ShadowType> getShadow(String oid, PrismObject<ShadowType> repositoryShadow,
			Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult parentResult)
					throws ObjectNotFoundException, CommunicationException, SchemaException,
					ConfigurationException, SecurityViolationException {

		Validate.notNull(oid, "Object id must not be null.");

		LOGGER.trace("Start getting object with oid {}", oid);

		GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);

		// We are using parent result directly, not creating subresult.
		// We want to hide the existence of shadow cache from the user.

		// Get the shadow from repository. There are identifiers that we need
		// for accessing the object by UCF.Later, the repository object may 
		// have a fully cached object from the resource.
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
		ctx.setGetOperationOptions(options);
		try {
			ctx.assertDefinition();
			applyAttributesDefinition(ctx, repositoryShadow);
		} catch (ObjectNotFoundException | SchemaException | CommunicationException
				| ConfigurationException e) {
			throw e;
			// String msg = e.getMessage()+" (returning repository shadow)";
			// LOGGER.error("{}", msg, e);
			// parentResult.recordPartialError(msg, e);
			// return repositoryShadow;
		}
		ResourceType resource = ctx.getResource();

		if (!ResourceTypeUtil.isReadCapabilityEnabled(resource)) {
			throw new UnsupportedOperationException("Resource does not support 'read' operation");
		}
		
		repositoryShadow = refreshShadow(repositoryShadow, task, parentResult);
		
		if (canReturnCached(options, repositoryShadow, resource)) {
			applyDefinition(repositoryShadow, parentResult);
			futurizeShadow(repositoryShadow, options);
			return repositoryShadow;
		}
		
		PrismObject<ShadowType> resourceShadow = null;
		try {

			Collection<? extends ResourceAttribute<?>> primaryIdentifiers = ShadowUtil
					.getPrimaryIdentifiers(repositoryShadow);

			if (primaryIdentifiers == null || primaryIdentifiers.isEmpty()) {
				// check if the account is not only partially created (exist
				// only in repo so far)
				if (repositoryShadow.asObjectable().getFailedOperationType() != null) {
					throw new GenericConnectorException(
							"Unable to get object from the resource. Probably it has not been created yet because of previous unavailability of the resource.");
				}
				
				// No identifiers found
				SchemaException ex = new SchemaException("No primary identifiers found in the repository shadow "
						+ repositoryShadow + " with respect to " + resource);
				parentResult.recordFatalError(
						"No prmary identifiers found in the repository shadow " + repositoryShadow, ex);
				throw ex;
			}
			
			Collection<? extends ResourceAttribute<?>> identifiers = ShadowUtil
					.getAllIdentifiers(repositoryShadow);
			
			resourceShadow = resouceObjectConverter.getResourceObject(ctx, identifiers, true, parentResult);

			// Resource shadow may have different auxiliary object classes than
			// the original repo shadow. Make sure we have the definition that 
			// applies to resource shadow. We will fix repo shadow later.
			// BUT we need also information about kind/intent and these information is only
			// in repo shadow, therefore the following 2 lines..
			resourceShadow.asObjectable().setKind(repositoryShadow.asObjectable().getKind());
			resourceShadow.asObjectable().setIntent(repositoryShadow.asObjectable().getIntent());
			ProvisioningContext shadowCtx = ctx.spawn(resourceShadow);

			resourceManager.modifyResourceAvailabilityStatus(resource.asPrismObject(),
					AvailabilityStatusType.UP, parentResult);
			// try to apply changes to the account only if the resource if UP
			if (isCompensate(rootOptions) && repositoryShadow.asObjectable().getObjectChange() != null
					&& repositoryShadow.asObjectable().getFailedOperationType() != null
					&& resource.getOperationalState() != null && resource.getOperationalState()
							.getLastAvailabilityStatus() == AvailabilityStatusType.UP) {
				throw new GenericConnectorException(
						"Found changes that have been not applied to the resource object yet. Trying to apply them now.");
			}

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Shadow from repository:\n{}", repositoryShadow.debugDump());
				LOGGER.trace("Resource object fetched from resource:\n{}", resourceShadow.debugDump());
			}

			repositoryShadow = shadowManager.updateShadow(shadowCtx, resourceShadow, repositoryShadow,
					parentResult);
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Repository shadow after update:\n{}", repositoryShadow.debugDump());
			}
			// Complete the shadow by adding attributes from the resource object
			PrismObject<ShadowType> resultShadow = completeShadow(shadowCtx, resourceShadow, repositoryShadow,
					parentResult);

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Shadow when assembled:\n{}", resultShadow.debugDump());
			}

			futurizeShadow(repositoryShadow, options);
			parentResult.recordSuccess();
			return resultShadow;

		} catch (Exception ex) {
			try {

				resourceShadow = handleError(ctx, ex, repositoryShadow, FailedOperation.GET, null,
						isDoDiscovery(resource, rootOptions), isCompensate(rootOptions), parentResult);
				if (parentResult.getStatus() == OperationResultStatus.FATAL_ERROR) {
					// We are going to return an object. Therefore this cannot
					// be fatal error, as at least some information
					// is returned
					parentResult.setStatus(OperationResultStatus.PARTIAL_ERROR);
				}
				return resourceShadow;

			} catch (GenericFrameworkException e) {
				throw new SystemException(e.getMessage(), e);
			} catch (ObjectAlreadyExistsException e) {
				throw new SystemException(e.getMessage(), e);
			}
		} finally {
			// We need to record the fetch down here. Now it is certain that we
			// are going to fetch from resource
			// (we do not have raw/noFetch option)
			// TODO: is this correct behaviour? don't we really want to record
			// fetch for protected objects?
			if (!ShadowUtil.isProtected(resourceShadow)) {
				InternalMonitor.recordShadowFetchOperation();

			}
		}
	}

	private void futurizeShadow(PrismObject<ShadowType> shadow,
			Collection<SelectorOptions<GetOperationOptions>> options) throws SchemaException {
		PointInTimeType pit = GetOperationOptions.getPointInTimeType(SelectorOptions.findRootOptions(options));
		if (pit != PointInTimeType.FUTURE) {
			return;
		}
		List<PendingOperationType> sortedOperations = sortOperations(shadow.asObjectable().getPendingOperation());
		for (PendingOperationType pendingOperation: sortedOperations) {
			OperationResultStatusType resultStatus = pendingOperation.getResultStatus();
			if (resultStatus != null && resultStatus != OperationResultStatusType.IN_PROGRESS && resultStatus != OperationResultStatusType.UNKNOWN) {
				continue;
			}
			ObjectDeltaType pendingDeltaType = pendingOperation.getDelta();
			ObjectDelta<ShadowType> pendingDelta = DeltaConvertor.createObjectDelta(pendingDeltaType, prismContext);
			if (pendingDelta.isModify()) {
				pendingDelta.applyTo(shadow);
			}
			if (pendingDelta.isDelete()) {
				shadow.asObjectable().setDead(true);
			}
		}
	}

	private boolean canReturnCached(Collection<SelectorOptions<GetOperationOptions>> options, PrismObject<ShadowType> repositoryShadow, ResourceType resource) throws ConfigurationException {
		ReadCapabilityType readCapabilityType = ResourceTypeUtil.getEffectiveCapability(resource, ReadCapabilityType.class);
		Boolean cachingOnly = readCapabilityType.isCachingOnly();
		if (cachingOnly == Boolean.TRUE) {
			return true;
		}
		PointInTimeType pit = GetOperationOptions.getPointInTimeType(SelectorOptions.findRootOptions(options));
		if (pit != null) {
			if (pit != PointInTimeType.CACHED) {
				return false;
			}
		}
		long stalenessOption = GetOperationOptions.getStaleness(SelectorOptions.findRootOptions(options));
		if (stalenessOption == 0L) {
			return false;
		}
		CachingMetadataType cachingMetadata = repositoryShadow.asObjectable().getCachingMetadata();
		if (cachingMetadata == null) {
			if (stalenessOption == Long.MAX_VALUE) {
				// We must return cached version but there is no cached version.
				throw new ConfigurationException("Cached version of "+repositoryShadow+" requested, but there is no cached value");
			}
			return false;
		}
		if (stalenessOption == Long.MAX_VALUE) {
			return true;
		}

		XMLGregorianCalendar retrievalTimestamp = cachingMetadata.getRetrievalTimestamp();
		if (retrievalTimestamp == null) {
			return false;
		}
		long retrievalTimestampMillis = XmlTypeConverter.toMillis(retrievalTimestamp);
		return (clock.currentTimeMillis() - retrievalTimestampMillis < stalenessOption);
	}

	private boolean isCompensate(GetOperationOptions rootOptions) {
		return !GetOperationOptions.isDoNotDiscovery(rootOptions);
	}

	private boolean isCompensate(ProvisioningOperationOptions options) {
		return ProvisioningOperationOptions.isCompletePostponed(options);
	}
	
	private boolean isDoDiscovery(ResourceType resource, GetOperationOptions rootOptions) {
		return !GetOperationOptions.isDoNotDiscovery(rootOptions) && isDoDiscovery(resource);
	}

	private boolean isDoDiscovery(ResourceType resource, ProvisioningOperationOptions options) {
		return !ProvisioningOperationOptions.isDoNotDiscovery(options) && isDoDiscovery(resource);
	}
	
	private boolean isDoDiscovery (ResourceType resource) {
		if (resource == null) {
			return true;
		}
		if (resource.getConsistency() == null) {
			return true;
		}
		
		if (resource.getConsistency().isDiscovery() == null) {
			return true;
		}
		
		return resource.getConsistency().isDiscovery();
	}
	
	public abstract String afterAddOnResource(ProvisioningContext ctx, AsynchronousOperationReturnValue<PrismObject<ShadowType>> addResult,
			OperationResult parentResult) throws SchemaException, ObjectAlreadyExistsException,
					ObjectNotFoundException, ConfigurationException, CommunicationException;

	public String addShadow(PrismObject<ShadowType> shadow, OperationProvisioningScriptsType scripts,
			ResourceType resource, ProvisioningOperationOptions options, Task task,
			OperationResult parentResult) throws CommunicationException, GenericFrameworkException,
					ObjectAlreadyExistsException, SchemaException, ObjectNotFoundException,
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
			handleError(ctx, e, shadow, FailedOperation.ADD, null, 
					isDoDiscovery(resource, options), true, parentResult);
			return null;
		}

//		if (LOGGER.isTraceEnabled()) {
//			LOGGER.trace("Definition:\n{}", ctx.getObjectClassDefinition().debugDump());
//		}

		PrismContainer<?> attributesContainer = shadow.findContainer(ShadowType.F_ATTRIBUTES);
		if (attributesContainer == null || attributesContainer.isEmpty()) {
			SchemaException e = new SchemaException(
					"Attempt to add shadow without any attributes: " + shadow);
			parentResult.recordFatalError(e);
			handleError(ctx, e, shadow, FailedOperation.ADD, null, 
					isDoDiscovery(resource, options), true, parentResult);
			return null;
		}

		AsynchronousOperationReturnValue<PrismObject<ShadowType>> asyncReturnValue;
		PrismObject<ShadowType> addedShadow;
		try {
			preprocessEntitlements(ctx, shadow, parentResult);

			applyAttributesDefinition(ctx, shadow);
			shadowManager.setKindIfNecessary(shadow.asObjectable(), ctx.getObjectClassDefinition());
			accessChecker.checkAdd(ctx, shadow, parentResult);

			// RESOURCE OPERATION: add
			asyncReturnValue = 
					resouceObjectConverter.addResourceObject(ctx, shadow, scripts, parentResult);
			addedShadow = asyncReturnValue.getReturnValue();

		} catch (Exception ex) {
			addedShadow = handleError(ctx, ex, shadow, FailedOperation.ADD, null,
					isDoDiscovery(resource, options), isCompensate(options), parentResult);
			return addedShadow.getOid();
		}

		// REPO OPERATION: add
		// This is where the repo shadow is created (if needed)
		String oid = afterAddOnResource(ctx, asyncReturnValue, parentResult);
		addedShadow.setOid(oid);

		ObjectDelta<ShadowType> delta = ObjectDelta.createAddDelta(addedShadow);
		ResourceOperationDescription operationDescription = createSuccessOperationDescription(ctx, addedShadow,
				delta, parentResult);
		if (asyncReturnValue.isInProgress()) {
			operationListener.notifyInProgress(operationDescription, task, parentResult);
		} else {
			operationListener.notifySuccess(operationDescription, task, parentResult);
		}
		return oid;
	}

	private ResourceOperationDescription createSuccessOperationDescription(ProvisioningContext ctx,
			PrismObject<ShadowType> shadowType, ObjectDelta delta, OperationResult parentResult)
					throws ObjectNotFoundException, SchemaException, CommunicationException,
					ConfigurationException {
		ResourceOperationDescription operationDescription = new ResourceOperationDescription();
		operationDescription.setCurrentShadow(shadowType);
		operationDescription.setResource(ctx.getResource().asPrismObject());
		if (ctx.getTask() != null) {
			operationDescription.setSourceChannel(ctx.getTask().getChannel());
		}
		operationDescription.setObjectDelta(delta);
		operationDescription.setResult(parentResult);
		return operationDescription;
	}

	public abstract void afterModifyOnResource(ProvisioningContext ctx, PrismObject<ShadowType> shadow,
			Collection<? extends ItemDelta> modifications, OperationResult resourceOperationResult, OperationResult parentResult)
					throws SchemaException, ObjectNotFoundException, ConfigurationException,
					CommunicationException;

	public abstract Collection<? extends ItemDelta> beforeModifyOnResource(PrismObject<ShadowType> shadow,
			ProvisioningOperationOptions options, Collection<? extends ItemDelta> modifications)
					throws SchemaException;

	public String modifyShadow(PrismObject<ShadowType> repoShadow, String oid,
			Collection<? extends ItemDelta> modifications, OperationProvisioningScriptsType scripts,
			ProvisioningOperationOptions options, Task task, OperationResult parentResult)
					throws CommunicationException, GenericFrameworkException, ObjectNotFoundException,
					SchemaException, ConfigurationException, SecurityViolationException {

		Validate.notNull(repoShadow, "Object to modify must not be null.");
		Validate.notNull(oid, "OID must not be null.");
		Validate.notNull(modifications, "Object modification must not be null.");

		InternalMonitor.recordShadowChangeOperation();

		Collection<QName> additionalAuxiliaryObjectClassQNames = new ArrayList<>();
		ItemPath auxPath = new ItemPath(ShadowType.F_AUXILIARY_OBJECT_CLASS);
		for (ItemDelta modification : modifications) {
			if (auxPath.equals(modification.getPath())) {
				PropertyDelta<QName> auxDelta = (PropertyDelta<QName>) modification;
				for (PrismPropertyValue<QName> pval : auxDelta.getValues(QName.class)) {
					additionalAuxiliaryObjectClassQNames.add(pval.getValue());
				}
			}
		}

		ProvisioningContext ctx = ctxFactory.create(repoShadow, additionalAuxiliaryObjectClassQNames, task,
				parentResult);

		AsynchronousOperationReturnValue<Collection<PropertyDelta<PrismPropertyValue>>> asyncReturnValue;
		try {
			ctx.assertDefinition();
			RefinedObjectClassDefinition rOCDef = ctx.getObjectClassDefinition();

			applyAttributesDefinition(ctx, repoShadow);

			accessChecker.checkModify(ctx.getResource(), repoShadow, modifications,
					ctx.getObjectClassDefinition(), parentResult);

			modifications = beforeModifyOnResource(repoShadow, options, modifications);
			
			preprocessEntitlements(ctx, modifications, "delta for shadow " + oid, parentResult);

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Applying change: {}", DebugUtil.debugDump(modifications));
			}

			asyncReturnValue = resouceObjectConverter.modifyResourceObject(ctx, repoShadow, scripts,
					modifications, parentResult);
			
		} catch (Exception ex) {
			LOGGER.debug("Provisioning exception: {}:{}, attempting to handle it",
					new Object[] { ex.getClass(), ex.getMessage(), ex });
			try {
				repoShadow = handleError(ctx, ex, repoShadow, FailedOperation.MODIFY, modifications,
						isDoDiscovery(ctx.getResource(), options), isCompensate(options), parentResult);
				parentResult.computeStatus();
			} catch (ObjectAlreadyExistsException e) {
				parentResult.recordFatalError(
						"While compensating communication problem for modify operation got: "
								+ ex.getMessage(),
						ex);
				throw new SystemException(e);
			}

			return repoShadow.getOid();
		}

		Collection<PropertyDelta<PrismPropertyValue>> sideEffectChanges = asyncReturnValue.getReturnValue();
		if (sideEffectChanges != null) {
			ItemDelta.addAll(modifications, sideEffectChanges);
		}

		afterModifyOnResource(ctx, repoShadow, modifications, asyncReturnValue.getOperationResult(), parentResult);

		ObjectDelta<ShadowType> delta = ObjectDelta.createModifyDelta(repoShadow.getOid(), modifications,
				repoShadow.getCompileTimeClass(), prismContext);
		ResourceOperationDescription operationDescription = createSuccessOperationDescription(ctx, repoShadow,
				delta, parentResult);
		if (asyncReturnValue.isInProgress()) {
			operationListener.notifyInProgress(operationDescription, task, parentResult);
			parentResult.recordInProgress();
			parentResult.setAsynchronousOperationReference(asyncReturnValue.getOperationResult().getAsynchronousOperationReference());
		} else {
			operationListener.notifySuccess(operationDescription, task, parentResult);
			parentResult.recordSuccess();
		}
		return oid;
	}

	public void deleteShadow(PrismObject<ShadowType> shadow, ProvisioningOperationOptions options,
			OperationProvisioningScriptsType scripts, Task task, OperationResult parentResult)
					throws CommunicationException, GenericFrameworkException, ObjectNotFoundException,
					SchemaException, ConfigurationException, SecurityViolationException {

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
				shadowManager.deleteShadow(ctx, shadow, null, parentResult);		
				parentResult.recordHandledError(
						"Resource defined in shadow does not exists. Shadow was deleted from the repository.");
				return;
			} else {
				throw ex;
			}
		}

		applyAttributesDefinition(ctx, shadow);

		LOGGER.trace("Deleting object {} from the resource {}.", shadow, ctx.getResource());

		AsynchronousOperationResult asyncReturnValue = null;
		if (shadow.asObjectable().getFailedOperationType() == null
				|| (shadow.asObjectable().getFailedOperationType() != null
						&& FailedOperationTypeType.ADD != shadow.asObjectable().getFailedOperationType())) {
			try {
				
				asyncReturnValue = resouceObjectConverter.deleteResourceObject(ctx, shadow, scripts, parentResult);
				
			} catch (Exception ex) {
				try {
					handleError(ctx, ex, shadow, FailedOperation.DELETE, null,
							isDoDiscovery(ctx.getResource(), options), isCompensate(options), parentResult);
				} catch (ObjectAlreadyExistsException e) {
					parentResult.recordFatalError(e);
					throw new SystemException(e.getMessage(), e);
				}
				return;
			}
		}

		LOGGER.trace("Detele object with oid {} form repository.", shadow.getOid());
		try {
			shadowManager.deleteShadow(ctx, shadow, asyncReturnValue==null?null:asyncReturnValue.getOperationResult(), parentResult);			
		} catch (ObjectNotFoundException ex) {
			parentResult.recordFatalError("Can't delete object " + shadow + ". Reason: " + ex.getMessage(),
					ex);
			throw new ObjectNotFoundException("An error occured while deleting resource object " + shadow
					+ "whith identifiers " + shadow + ": " + ex.getMessage(), ex);
		}
		
		ObjectDelta<ShadowType> delta = ObjectDelta.createDeleteDelta(shadow.getCompileTimeClass(),
				shadow.getOid(), prismContext);
		ResourceOperationDescription operationDescription = createSuccessOperationDescription(ctx, shadow,
				delta, parentResult);
		if (asyncReturnValue != null && asyncReturnValue.isInProgress()) {
			operationListener.notifyInProgress(operationDescription, task, parentResult);
		} else {
			operationListener.notifySuccess(operationDescription, task, parentResult);
		}
		
		LOGGER.trace("Object deleted from repository successfully.");
		parentResult.computeStatus();
		resourceManager.modifyResourceAvailabilityStatus(ctx.getResource().asPrismObject(),
				AvailabilityStatusType.UP, parentResult);
	}

	public PrismObject<ShadowType> refreshShadow(PrismObject<ShadowType> repoShadow, Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {
		ShadowType shadowType = repoShadow.asObjectable();
		List<PendingOperationType> pendingOperations = shadowType.getPendingOperation();
		if (pendingOperations.isEmpty()) {
			return repoShadow;
		}
		
		ProvisioningContext ctx = ctxFactory.create(repoShadow, task, parentResult);
		ctx.assertDefinition();
		
		Duration gracePeriod = null;
		ResourceConsistencyType consistency = ctx.getResource().getConsistency();
		if (consistency != null) {
			gracePeriod = consistency.getPendingOperationGracePeriod();
		}
		
		List<ObjectDelta<ShadowType>> notificationDeltas = new ArrayList<>();
		List<PendingOperationType> sortedOperations = sortOperations(pendingOperations);
		
		ObjectDelta<ShadowType> shadowDelta = repoShadow.createModifyDelta();
		for (PendingOperationType pendingOperation: sortedOperations) {

			ItemPath containerPath = pendingOperation.asPrismContainerValue().getPath();
			OperationResultStatusType statusType = pendingOperation.getResultStatus();
			XMLGregorianCalendar completionTimestamp = pendingOperation.getCompletionTimestamp();
			XMLGregorianCalendar now = null;
			
			String asyncRef = pendingOperation.getAsynchronousOperationReference();
			if (asyncRef != null) {
				
				OperationResultStatus newStaus = resouceObjectConverter.refreshOperationStatus(ctx, repoShadow, asyncRef, parentResult);
				
				now = clock.currentTimeXMLGregorianCalendar();
						
				if (newStaus != null) {
					OperationResultStatusType newStatusType = newStaus.createStatusType();
					if (!newStatusType.equals(pendingOperation.getResultStatus())) {
						
						
						boolean operationCompleted = isCompleted(newStatusType) && pendingOperation.getCompletionTimestamp() == null;
										
						if (operationCompleted && gracePeriod == null) {
							LOGGER.trace("Deleting pending operation because it is completed (no grace): {}", pendingOperation);
							shadowDelta.addModificationDeleteContainer(new ItemPath(ShadowType.F_PENDING_OPERATION), pendingOperation.clone());
							continue;
						} else {
						
							PropertyDelta<OperationResultStatusType> statusDelta = shadowDelta.createPropertyModification(containerPath.subPath(PendingOperationType.F_RESULT_STATUS));
							statusDelta.setValuesToReplace(new PrismPropertyValue<>(newStatusType));
							shadowDelta.addModification(statusDelta);
						}

						statusType = newStatusType;
						
						if (operationCompleted) {
							// Operation completed
							PropertyDelta<XMLGregorianCalendar> timestampDelta = shadowDelta.createPropertyModification(containerPath.subPath(PendingOperationType.F_COMPLETION_TIMESTAMP));
							timestampDelta.setValuesToReplace(new PrismPropertyValue<>(now));
							shadowDelta.addModification(timestampDelta);
							completionTimestamp = now;
							
							ObjectDeltaType pendingDeltaType = pendingOperation.getDelta();
							ObjectDelta<ShadowType> pendingDelta = DeltaConvertor.createObjectDelta(pendingDeltaType, prismContext);
							
							// We do not need to care about add deltas here. The add operation is already applied to
							// attributes. We need this to "allocate" the identifiers, so iteration mechanism in the
							// model can find unique values while taking pending create operations into consideration.
							
							if (pendingDelta.isModify()) {
								for (ItemDelta<?, ?> pendingModification: pendingDelta.getModifications()) {
									shadowDelta.addModification(pendingModification.clone());
								}
							}
							
							if (pendingDelta.isDelete()) {
								if (gracePeriod == null) {
									shadowDelta = repoShadow.createDeleteDelta();
									break;
								} else {
									PropertyDelta<Boolean> deadDelta = shadowDelta.createPropertyModification(new ItemPath(ShadowType.F_DEAD));
									deadDelta.setValuesToReplace(new PrismPropertyValue<>(true));
									shadowDelta.addModification(deadDelta);
								}
							}
							
							notificationDeltas.add(pendingDelta);
						}
						
					}
				}
			}
			
			if (now == null) {
				now = clock.currentTimeXMLGregorianCalendar();
			}
			
			if (isCompleted(statusType)) {
				if (isOverGrace(now, gracePeriod, completionTimestamp)) {
					LOGGER.trace("Deleting pending operation because it is completed '{}' (and over grace): {}", statusType.value(), pendingOperation);
					shadowDelta.addModificationDeleteContainer(new ItemPath(ShadowType.F_PENDING_OPERATION), pendingOperation.clone());
					continue;
				}
			}
		}
		
		if (!shadowDelta.isEmpty()) {
			shadowManager.modifyShadowAttributes(ctx, repoShadow, shadowDelta.getModifications(), parentResult);
		}
		
		for (ObjectDelta<ShadowType> notificationDelta: notificationDeltas) {
			ResourceOperationDescription operationDescription = createSuccessOperationDescription(ctx, repoShadow,
					notificationDelta, parentResult);
			operationListener.notifySuccess(operationDescription, task, parentResult);
		}
		
		if (shadowDelta.isEmpty()) {
			return repoShadow;
		}
		shadowDelta.applyTo(repoShadow);
		return repoShadow;
	}
	
	private boolean isOverGrace(XMLGregorianCalendar now, Duration gracePeriod, XMLGregorianCalendar completionTimestamp) {
		if (gracePeriod == null) {
			return true;
		}
		XMLGregorianCalendar graceExpiration = XmlTypeConverter.addDuration(completionTimestamp, gracePeriod);
		return XmlTypeConverter.compare(now, graceExpiration) == DatatypeConstants.GREATER;
	}

	private boolean isCompleted(OperationResultStatusType statusType) {
		 return statusType != OperationResultStatusType.IN_PROGRESS && statusType != OperationResultStatusType.UNKNOWN;
	}

	private List<PendingOperationType> sortOperations(List<PendingOperationType> pendingOperations) {
		// Copy to mutable list that is not bound to the prism
		List<PendingOperationType> sortedList = new ArrayList<>(pendingOperations.size());
		sortedList.addAll(pendingOperations);
		Collections.sort(sortedList, (o1,o2) -> XmlTypeConverter.compare(o1.getRequestTimestamp(), o2.getRequestTimestamp()) );
		return sortedList;
	}

	public void applyDefinition(ObjectDelta<ShadowType> delta, ShadowType shadowTypeWhenNoOid,
			OperationResult parentResult) throws SchemaException, ObjectNotFoundException,
					CommunicationException, ConfigurationException {
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
						throw new IllegalArgumentException("No OID in object delta " + delta
								+ " and no externally-supplied shadow is present as well.");
					}
					shadow = shadowTypeWhenNoOid.asPrismObject();
				} else {
					shadow = repositoryService.getObject(delta.getObjectTypeClass(), shadowOid, null,
							parentResult); // TODO consider fetching only when
											// really necessary
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
		ctx = applyAttributesDefinition(ctx, shadow);

	}

	public void setProtectedShadow(PrismObject<ShadowType> shadow, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {
		ProvisioningContext ctx = ctxFactory.create(shadow, null, parentResult);
		ctx.assertDefinition();
		ProvisioningUtil.setProtectedFlag(ctx, shadow, matchingRuleRegistry);
	}

	public void applyDefinition(final ObjectQuery query, OperationResult result)
			throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException {
		ResourceShadowDiscriminator coordinates = ObjectQueryUtil.getCoordinates(query.getFilter());
		ProvisioningContext ctx = ctxFactory.create(coordinates, null, result);
		ctx.assertDefinition();
		applyDefinition(ctx, query);
	}

	private void applyDefinition(final ProvisioningContext ctx, final ObjectQuery query)
			throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException {
		if (query == null) {
			return;
		}
		ObjectFilter filter = query.getFilter();
		if (filter == null) {
			return;
		}
		final RefinedObjectClassDefinition objectClassDefinition = ctx.getObjectClassDefinition();
		final ItemPath attributesPath = new ItemPath(ShadowType.F_ATTRIBUTES);
		com.evolveum.midpoint.prism.query.Visitor visitor = subfilter -> {
			if (subfilter instanceof PropertyValueFilter) {
				PropertyValueFilter<?> valueFilter = (PropertyValueFilter<?>) subfilter;
				ItemDefinition definition = valueFilter.getDefinition();
				if (definition instanceof ResourceAttributeDefinition) {
					return;		// already has a resource-related definition
				}
				if (!attributesPath.equivalent(valueFilter.getParentPath())) {
					return;
				}
				QName attributeName = valueFilter.getElementName();
				ResourceAttributeDefinition attributeDefinition = objectClassDefinition.findAttributeDefinition(attributeName);
				if (attributeDefinition == null) {
					throw new TunnelException(new SchemaException("No definition for attribute "
							+ attributeName + " in query " + query));
				}
				valueFilter.setDefinition(attributeDefinition);
			}
		};
		try {
			filter.accept(visitor);
		} catch (TunnelException te) {
			SchemaException e = (SchemaException) te.getCause();
			throw e;
		}
	}

	protected ResourceType getResource(ResourceShadowDiscriminator coords, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {
		String resourceOid = coords.getResourceOid();
		if (resourceOid == null) {
			throw new IllegalArgumentException("No resource OID in " + coords);
		}
		return resourceManager.getResource(resourceOid, null, parentResult).asObjectable();
	}

	@SuppressWarnings("rawtypes")
	protected PrismObject<ShadowType> handleError(ProvisioningContext ctx, Exception ex,
			PrismObject<ShadowType> shadow, FailedOperation op, Collection<? extends ItemDelta> modifications,
			boolean doDiscovery, boolean compensate, OperationResult parentResult) throws SchemaException,
					GenericFrameworkException, CommunicationException, ObjectNotFoundException,
					ObjectAlreadyExistsException, ConfigurationException, SecurityViolationException {

		if (parentResult.isUnknown()) {
			parentResult.computeStatus();
		}
		// do not set result in the shadow in case of get operation, it will
		// resulted to misleading information
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

		LOGGER.debug("Handling provisioning exception {}: {}",
				new Object[] { ex.getClass(), ex.getMessage() });
		LOGGER.trace("Handling provisioning exception {}: {}\ndoDiscovery={}, compensate={}",
				new Object[] { ex.getClass(), ex.getMessage(), 
						doDiscovery, compensate, ex });

		return handler.handleError(shadow.asObjectable(), op, ex, doDiscovery, compensate, ctx.getTask(), parentResult)
				.asPrismObject();

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
			
			ContainerDelta<ShadowAssociationType> associationDelta = objectDelta.findContainerDelta(ShadowType.F_ASSOCIATION);
			if (associationDelta != null) {
				normalizeAssociationDeltasBeforeSave(associationDelta.getValuesToAdd());
				normalizeAssociationDeltasBeforeSave(associationDelta.getValuesToReplace());
				normalizeAssociationDeltasBeforeSave(associationDelta.getValuesToDelete());
				
			}
			
			
			ObjectDeltaType objectDeltaType = DeltaConvertor.toObjectDeltaType(objectDelta);

			shadowType.setObjectChange(objectDeltaType);
		}
		return shadow;
	}
	
	//we need to remove resolved identifiers form the ShadowAssociationType before we save it to the shadow as an unfinished operation. 
	void normalizeAssociationDeltasBeforeSave(Collection<PrismContainerValue<ShadowAssociationType>> associationContainers) {
		if (associationContainers == null) {
			return;
		}
		for (PrismContainerValue<ShadowAssociationType> associationContainer : associationContainers) {
			if (associationContainer.contains(ShadowAssociationType.F_IDENTIFIERS) && associationContainer.contains(ShadowAssociationType.F_SHADOW_REF)) {
				associationContainer.removeContainer(ShadowAssociationType.F_IDENTIFIERS);
			}
		}
	}

	////////////////////////////////////////////////////////////////////////////
	// SEARCH
	////////////////////////////////////////////////////////////////////////////

	public SearchResultMetadata searchObjectsIterative(ObjectQuery query,
			Collection<SelectorOptions<GetOperationOptions>> options, final ShadowHandler<ShadowType> handler,
			final boolean readFromRepository, Task task, final OperationResult parentResult)
					throws SchemaException, ObjectNotFoundException, CommunicationException,
					ConfigurationException, SecurityViolationException {

		ResourceShadowDiscriminator coordinates = ObjectQueryUtil.getCoordinates(query.getFilter());
		final ProvisioningContext ctx = ctxFactory.create(coordinates, task, parentResult);
		ctx.setGetOperationOptions(options);
		ctx.assertDefinition();

		return searchObjectsIterative(ctx, query, options, handler, readFromRepository, parentResult);
	}

	public SearchResultMetadata searchObjectsIterative(final ProvisioningContext ctx, ObjectQuery query,
			Collection<SelectorOptions<GetOperationOptions>> options, final ShadowHandler<ShadowType> handler,
			final boolean readFromRepository, final OperationResult parentResult)
					throws SchemaException, ObjectNotFoundException, CommunicationException,
					ConfigurationException, SecurityViolationException {
		applyDefinition(ctx, query);

		GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);
		if (ProvisioningUtil.shouldDoRepoSearch(rootOptions)) {
			return searchObjectsIterativeRepository(ctx, query, options, handler, parentResult);
		}

		// We need to record the fetch down here. Now it is certain that we are
		// going to fetch from resource
		// (we do not have raw/noFetch option)
		InternalMonitor.recordShadowFetchOperation();

		ObjectQuery attributeQuery = createAttributeQuery(query);

		ResultHandler<ShadowType> resultHandler = new ResultHandler<ShadowType>() {

			@Override
			public boolean handle(PrismObject<ShadowType> resourceShadow) {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Found resource object\n{}", resourceShadow.debugDump(1));
				}
				PrismObject<ShadowType> resultShadow;
				try {
					// The shadow does not have any kind or intent at this
					// point.
					// But at least locate the definition using object classes.
					ProvisioningContext estimatedShadowCtx = reapplyDefinitions(ctx, resourceShadow);
					// Try to find shadow that corresponds to the resource
					// object.
					if (readFromRepository) {
						PrismObject<ShadowType> repoShadow = lookupOrCreateShadowInRepository(
								estimatedShadowCtx, resourceShadow, true, parentResult);

						// This determines the definitions exactly. How the repo
						// shadow should have proper kind/intent
						ProvisioningContext shadowCtx = applyAttributesDefinition(ctx, repoShadow);

						repoShadow = shadowManager.updateShadow(shadowCtx, resourceShadow, repoShadow,
								parentResult);

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

		return resouceObjectConverter.searchResourceObjects(ctx, resultHandler, attributeQuery,
				fetchAssociations, parentResult);

	}

	ObjectQuery createAttributeQuery(ObjectQuery query) throws SchemaException {
		ObjectFilter filter = null;
		if (query != null) {
			filter = query.getFilter();
		}

		ObjectQuery attributeQuery = null;

		if (filter instanceof AndFilter) {
			List<? extends ObjectFilter> conditions = ((AndFilter) filter).getConditions();
			List<ObjectFilter> attributeFilter = createAttributeQueryInternal(conditions);
			if (attributeFilter.size() > 1) {
				attributeQuery = ObjectQuery.createObjectQuery(AndFilter.createAnd(attributeFilter));
			} else if (attributeFilter.size() < 1) {
				LOGGER.trace("No attribute filter defined in the query.");
			} else if (attributeFilter.size() == 1) {
				attributeQuery = ObjectQuery.createObjectQuery(attributeFilter.iterator().next());
			}
		}

		if (query != null && query.getPaging() != null) {
			if (attributeQuery == null) {
				attributeQuery = new ObjectQuery();
			}
			attributeQuery.setPaging(query.getPaging());
		}
		if (query != null && query.isAllowPartialResults()) {
			if (attributeQuery == null) {
				attributeQuery = new ObjectQuery();
			}
			attributeQuery.setAllowPartialResults(true);
		}

		if (InternalsConfig.consistencyChecks && attributeQuery != null
				&& attributeQuery.getFilter() != null) {
			attributeQuery.getFilter().checkConsistence(true);
		}
		return attributeQuery;
	}

	private List<ObjectFilter> createAttributeQueryInternal(List<? extends ObjectFilter> conditions)
			throws SchemaException {
		List<ObjectFilter> attributeFilter = new ArrayList<>();
		for (ObjectFilter f : conditions) {
			if (f instanceof EqualFilter) {
				ItemPath parentPath = ((EqualFilter) f).getParentPath();
				if (parentPath.isEmpty()) {
					QName elementName = ((EqualFilter) f).getElementName();
					if (QNameUtil.match(ShadowType.F_OBJECT_CLASS, elementName) ||
							QNameUtil.match(ShadowType.F_AUXILIARY_OBJECT_CLASS, elementName) ||
							QNameUtil.match(ShadowType.F_KIND, elementName) ||
							QNameUtil.match(ShadowType.F_INTENT, elementName)) {
						continue;
					}
					throw new SchemaException("Cannot combine on-resource and off-resource properties in a shadow search query. Encountered property " +
							((EqualFilter) f).getFullPath());
				}
				attributeFilter.add(f);
			} else if (f instanceof NaryLogicalFilter) {
				List<ObjectFilter> subFilters = createAttributeQueryInternal(
						((NaryLogicalFilter) f).getConditions());
				if (subFilters.size() > 1) {
					if (f instanceof OrFilter) {
						attributeFilter.add(OrFilter.createOr(subFilters));
					} else if (f instanceof AndFilter) {
						attributeFilter.add(AndFilter.createAnd(subFilters));
					} else {
						throw new IllegalArgumentException(
								"Could not translate query filter. Unknown type: " + f);
					}
				} else if (subFilters.size() < 1) {
					continue;
				} else if (subFilters.size() == 1) {
					attributeFilter.add(subFilters.iterator().next());
				}

			} else if (f instanceof SubstringFilter) {
				attributeFilter.add(f);
			} else if (f instanceof RefFilter) {
				ItemPath parentPath = ((RefFilter)f).getParentPath();
				if (parentPath.isEmpty()) {
					QName elementName = ((RefFilter) f).getElementName();
					if (QNameUtil.match(ShadowType.F_RESOURCE_REF, elementName)) {
						continue;
					}
				}
				throw new SchemaException("Cannot combine on-resource and off-resource properties in a shadow search query. Encountered filter " + f);
			} else {
				throw new SchemaException("Cannot combine on-resource and off-resource properties in a shadow search query. Encountered filter " + f);
			}

		}

		return attributeFilter;
	}

	private SearchResultMetadata searchObjectsIterativeRepository(final ProvisioningContext ctx,
			ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options,
			final ShadowHandler<ShadowType> shadowHandler, OperationResult parentResult)
					throws SchemaException, ConfigurationException, ObjectNotFoundException,
					CommunicationException {

		com.evolveum.midpoint.schema.ResultHandler<ShadowType> repoHandler = new com.evolveum.midpoint.schema.ResultHandler<ShadowType>() {
			@Override
			public boolean handle(PrismObject<ShadowType> object, OperationResult parentResult) {
				try {
					applyAttributesDefinition(ctx, object);
					// fixing MID-1640; hoping that the protected object filter uses only identifiers
					// (that are stored in repo)
					ProvisioningUtil.setProtectedFlag(ctx, object, matchingRuleRegistry); 
					boolean cont = shadowHandler.handle(object.asObjectable());
					parentResult.recordSuccess();
					return cont;
				} catch (RuntimeException e) {
					parentResult.recordFatalError(e);
					throw e;
				} catch (SchemaException | ConfigurationException | ObjectNotFoundException
						| CommunicationException e) {
					parentResult.recordFatalError(e);
					throw new SystemException(e);
				}
			}
		};

		return shadowManager.searchObjectsIterativeRepository(ctx, query, options, repoHandler, parentResult);
	}

	private PrismObject<ShadowType> lookupOrCreateShadowInRepository(ProvisioningContext ctx,
			PrismObject<ShadowType> resourceShadow, boolean unknownIntent, OperationResult parentResult)
					throws SchemaException, ConfigurationException, ObjectNotFoundException,
					CommunicationException, SecurityViolationException, GenericConnectorException {
		PrismObject<ShadowType> repoShadow = shadowManager.lookupShadowInRepository(ctx, resourceShadow,
				parentResult);

		if (repoShadow == null) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace(
						"Shadow object (in repo) corresponding to the resource object (on the resource) was not found. The repo shadow will be created. The resource object:\n{}",
						SchemaDebugUtil.prettyPrint(resourceShadow));
			}

			repoShadow = createShadowInRepository(ctx, resourceShadow, unknownIntent, parentResult);
		} else {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Found shadow object in the repository {}",
						SchemaDebugUtil.prettyPrint(repoShadow));
			}
		}

		return repoShadow;
	}

	private PrismObject<ShadowType> createShadowInRepository(ProvisioningContext ctx,
			PrismObject<ShadowType> resourceShadow, boolean unknownIntent, OperationResult parentResult)
					throws SchemaException, ConfigurationException, ObjectNotFoundException,
					CommunicationException, SecurityViolationException, GenericConnectorException {

		PrismObject<ShadowType> repoShadow;
		PrismObject<ShadowType> conflictingShadow = shadowManager.lookupConflictingShadowBySecondaryIdentifiers(ctx,
				resourceShadow, parentResult);
		if (conflictingShadow != null) {
			applyAttributesDefinition(ctx, conflictingShadow);
			conflictingShadow = completeShadow(ctx, resourceShadow, conflictingShadow, parentResult);
			Task task = taskManager.createTaskInstance();
			ResourceOperationDescription failureDescription = shadowManager
					.createResourceFailureDescription(conflictingShadow, ctx.getResource(), parentResult);
			changeNotificationDispatcher.notifyFailure(failureDescription, task, parentResult);
			shadowManager.deleteConflictedShadowFromRepo(conflictingShadow, parentResult);
		}
		// The resource object obviously exists on the resource, but appropriate
		// shadow does not exist in the
		// repository we need to create the shadow to align repo state to the
		// reality (resource)

		try {

			repoShadow = shadowManager.addDiscoveredRepositoryShadow(ctx, resourceShadow, parentResult);

		} catch (ObjectAlreadyExistsException e) {
			// This should not happen. We haven't supplied an OID so is should
			// not conflict
			LOGGER.error("Unexpected repository behavior: Object already exists: {}", e.getMessage(), e);
			throw new SystemException(
					"Unexpected repository behavior: Object already exists: " + e.getMessage(), e);
		}

		resourceShadow.setOid(repoShadow.getOid());
		resourceShadow.asObjectable().setResource(ctx.getResource());

		// We have object for which there was no shadow. Which means that
		// midPoint haven't known about this shadow
		// before. Invoke notifyChange() so the new shadow is properly
		// initialized.

		ResourceObjectShadowChangeDescription shadowChangeDescription = new ResourceObjectShadowChangeDescription();
		shadowChangeDescription.setResource(ctx.getResource().asPrismObject());
		shadowChangeDescription.setOldShadow(null);
		shadowChangeDescription.setCurrentShadow(resourceShadow);
		shadowChangeDescription.setSourceChannel(SchemaConstants.CHANGE_CHANNEL_DISCOVERY_URI);
		shadowChangeDescription.setUnrelatedChange(true);

		Task task = taskManager.createTaskInstance();
		notifyResourceObjectChangeListeners(shadowChangeDescription, task, task.getResult());

		if (unknownIntent) {
			// Intent may have been changed during the notifyChange processing.
			// Re-read the shadow if necessary.
			repoShadow = shadowManager.fixShadow(ctx, repoShadow, parentResult);
		}

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Final repo shadow (created):\n{}", repoShadow.debugDump());
		}

		return repoShadow;
	}

	public Integer countObjects(ObjectQuery query, Task task, final OperationResult result)
			throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
			SecurityViolationException {

		ResourceShadowDiscriminator coordinates = ObjectQueryUtil.getCoordinates(query.getFilter());
		final ProvisioningContext ctx = ctxFactory.create(coordinates, null, result);
		ctx.assertDefinition();
		applyDefinition(ctx, query);

		RefinedObjectClassDefinition objectClassDef = ctx.getObjectClassDefinition();
		ResourceType resourceType = ctx.getResource();
		CountObjectsCapabilityType countObjectsCapabilityType = objectClassDef
				.getEffectiveCapability(CountObjectsCapabilityType.class);
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
						count = connector.count(objectClassDef.getObjectClassDefinition(), attributeQuery,
								objectClassDef.getPagedSearches(), ctx, result);
					} catch (CommunicationException | GenericFrameworkException | SchemaException
							| UnsupportedOperationException e) {
						result.recordFatalError(e);
						throw e;
					}
					result.computeStatus();
					result.cleanupResult();
					return count;
				} catch (GenericFrameworkException | UnsupportedOperationException e) {
					SystemException ex = new SystemException(
							"Couldn't count objects on resource " + resourceType + ": " + e.getMessage(), e);
					result.recordFatalError(ex);
					throw ex;
				}

			} else if (simulate == CountObjectsSimulateType.PAGED_SEARCH_ESTIMATE) {

				if (!objectClassDef.isPagedSearchEnabled()) {
					throw new ConfigurationException(
							"Configured count object capability to be simulated using a paged search but paged search capability is not present");
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
				Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(
						new ItemPath(ShadowType.F_ASSOCIATION),
						GetOperationOptions.createRetrieve(RetrieveOption.EXCLUDE));
				SearchResultMetadata resultMetadata;
				try {
					resultMetadata = searchObjectsIterative(query, options, handler, false, task, result);
				} catch (SchemaException | ObjectNotFoundException | ConfigurationException
						| SecurityViolationException e) {
					result.recordFatalError(e);
					throw e;
				}
				result.computeStatus();
				result.cleanupResult();

				return resultMetadata.getApproxNumberOfAllResults();

			} else if (simulate == CountObjectsSimulateType.SEQUENTIAL_SEARCH) {

				// traditional way of counting objects (i.e. counting them one
				// by one)
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

				Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(
						new ItemPath(ShadowType.F_ASSOCIATION),
						GetOperationOptions.createRetrieve(RetrieveOption.EXCLUDE));

				searchObjectsIterative(query, options, handler, false, task, result);
				// TODO: better error handling
				result.computeStatus();
				result.cleanupResult();
				return countHolder.getValue();

			} else {
				throw new IllegalArgumentException("Unknown count capability simulate type " + simulate);

			}
		}

	}

	///////////////////////////////////////////////////////////////////////////
	// TODO: maybe split this to a separate class
	///////////////////////////////////////////////////////////////////////////

	public int synchronize(ResourceShadowDiscriminator shadowCoordinates, PrismProperty<?> lastToken,
			Task task, OperationResult parentResult) throws ObjectNotFoundException, CommunicationException,
					GenericFrameworkException, SchemaException, ConfigurationException,
					SecurityViolationException, ObjectAlreadyExistsException {

		InternalMonitor.recordShadowOtherOperation();

		final ProvisioningContext ctx = ctxFactory.create(shadowCoordinates, task, parentResult);

		List<Change> changes = null;
		try {

			changes = resouceObjectConverter.fetchChanges(ctx, lastToken, parentResult);

			LOGGER.trace("Found {} change(s). Start processing it (them).", changes.size());

			int processedChanges = 0;

			for (Change change : changes) {

				if (change.isTokenOnly()) {
					LOGGER.trace("Found token-only change: {}", change);
					task.setExtensionProperty(change.getToken());
					continue;
				}

				ObjectClassComplexTypeDefinition changeObjectClassDefinition = change
						.getObjectClassDefinition();

				ProvisioningContext shadowCtx;
				PrismObject<ShadowType> oldShadow = null;
				if (changeObjectClassDefinition == null) {
					if (change.getObjectDelta() != null && change.getObjectDelta().isDelete()) {
						oldShadow = change.getOldShadow();
						if (oldShadow == null) {
							oldShadow = shadowManager.findOrAddShadowFromChangeGlobalContext(ctx, change,
									parentResult);
						}
						if (oldShadow == null) {
							LOGGER.debug(
									"No old shadow for delete synchronization event {}, we probably did not know about that object anyway, so well be ignoring this event",
									change);
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
					task.setProgress(task.getProgress() + 1); // because
																// processedChanges
																// are reflected
																// into task
																// only at task
																// run finish
					LOGGER.debug(
							"Skipping processing change. Can't find appropriate shadow (e.g. the object was deleted on the resource meantime).");
					continue;
				}
				boolean isSuccess = processSynchronization(shadowCtx, change, parentResult);
                                
                                boolean retryUnhandledError = true;
                                if (task.getExtension() != null) {
                                      PrismProperty tokenRetryUnhandledErrProperty = task.getExtensionProperty(SchemaConstants.SYNC_TOKEN_RETRY_UNHANDLED);
                                      
                                      if (tokenRetryUnhandledErrProperty != null) {
                                          retryUnhandledError = (boolean) tokenRetryUnhandledErrProperty.getRealValue(); 
                                      }                                                                     
                                }     
                                
				if (!retryUnhandledError || isSuccess) {                                    
					// // get updated token from change,
					// // create property modification from new token
					// // and replace old token with the new one
					PrismProperty<?> newToken = change.getToken();
					task.setExtensionProperty(newToken);
					processedChanges++;
					task.setProgress(task.getProgress() + 1); // because
																// processedChanges
																// are reflected
																// into task
																// only at task
																// run finish
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
		} catch (ObjectNotFoundException ex) {
			parentResult.recordFatalError("Object not found error: " + ex.getMessage(), ex);
			throw ex;
		} catch (ObjectAlreadyExistsException ex) {
			parentResult.recordFatalError("Already exists error: " + ex.getMessage(), ex);
			throw ex;
		}
	}

	@SuppressWarnings("rawtypes")
	boolean processSynchronization(ProvisioningContext ctx, Change change, OperationResult result)
			throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException,
			CommunicationException, ConfigurationException {

		ResourceObjectShadowChangeDescription shadowChangeDescription = createResourceShadowChangeDescription(
				change, ctx.getResource(), ctx.getChannel());

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("**PROVISIONING: Created resource object shadow change description {}",
					SchemaDebugUtil.prettyPrint(shadowChangeDescription));
		}
		OperationResult notifyChangeResult = new OperationResult(
				ShadowCache.class.getName() + "notifyChange");
		notifyChangeResult.addParam("resourceObjectShadowChangeDescription", shadowChangeDescription);

		try {
			notifyResourceObjectChangeListeners(shadowChangeDescription, ctx.getTask(), notifyChangeResult);
			notifyChangeResult.recordSuccess();
		} catch (RuntimeException ex) {
			// recordFatalError(LOGGER, notifyChangeResult, "Synchronization
			// error: " + ex.getMessage(), ex);
			saveAccountResult(shadowChangeDescription, change, notifyChangeResult, result);
			throw new SystemException("Synchronization error: " + ex.getMessage(), ex);
		}

		notifyChangeResult.computeStatus("Error in notify change operation.");

		boolean successfull = false;
		if (notifyChangeResult.isSuccess() || notifyChangeResult.isHandledError()) {
			deleteShadowFromRepo(change, result);
			successfull = true;
			// // get updated token from change,
			// // create property modification from new token
			// // and replace old token with the new one
			// PrismProperty<?> newToken = change.getToken();
			// task.setExtensionProperty(newToken);
			// processedChanges++;

		} else {
			successfull = false;
			saveAccountResult(shadowChangeDescription, change, notifyChangeResult, result);
		}

		return successfull;
		// }
		// // also if no changes was detected, update token
		// if (changes.isEmpty() && tokenProperty != null) {
		// LOGGER.trace("No changes to synchronize on " +
		// ObjectTypeUtil.toShortString(resourceType));
		// task.setExtensionProperty(tokenProperty);
		// }
		// task.savePendingModifications(result);
		// return processedChanges;
	}

	private void notifyResourceObjectChangeListeners(ResourceObjectShadowChangeDescription change, Task task,
			OperationResult parentResult) {
		changeNotificationDispatcher.notifyChange(change, task, parentResult);
	}

	@SuppressWarnings("unchecked")
	private ResourceObjectShadowChangeDescription createResourceShadowChangeDescription(
			Change change, ResourceType resourceType, String channel) {
		ResourceObjectShadowChangeDescription shadowChangeDescription = new ResourceObjectShadowChangeDescription();
		shadowChangeDescription.setObjectDelta(change.getObjectDelta());
		shadowChangeDescription.setResource(resourceType.asPrismObject());
		shadowChangeDescription.setOldShadow(change.getOldShadow());
		shadowChangeDescription.setCurrentShadow(change.getCurrentShadow());
		if (null == channel) {
			shadowChangeDescription
					.setSourceChannel(QNameUtil.qNameToUri(SchemaConstants.CHANGE_CHANNEL_LIVE_SYNC));
		} else {
			shadowChangeDescription.setSourceChannel(channel);
		}
		return shadowChangeDescription;
	}

	@SuppressWarnings("rawtypes")
	private void saveAccountResult(ResourceObjectShadowChangeDescription shadowChangeDescription,
			Change change, OperationResult notifyChangeResult, OperationResult parentResult)
					throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {

		Collection<? extends ItemDelta> shadowModification = createShadowResultModification(change,
				notifyChangeResult);
		String oid = getOidFromChange(change);
		// maybe better error handling is needed
		try {
			ConstraintsChecker.onShadowModifyOperation(shadowModification);
			repositoryService.modifyObject(ShadowType.class, oid, shadowModification, parentResult);
		} catch (SchemaException ex) {
			parentResult.recordPartialError("Couldn't modify object: schema violation: " + ex.getMessage(),
					ex);
			// throw ex;
		} catch (ObjectNotFoundException ex) {
			parentResult.recordWarning("Couldn't modify object: object not found: " + ex.getMessage(), ex);
			// throw ex;
		} catch (ObjectAlreadyExistsException ex) {
			parentResult.recordPartialError(
					"Couldn't modify object: object already exists: " + ex.getMessage(), ex);
			// throw ex;
		}

	}

	private PrismObjectDefinition<ShadowType> getResourceObjectShadowDefinition() {
		// if (resourceObjectShadowDefinition == null) {
		return prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ShadowType.class);
		// }
		// return resourceObjectShadowDefinition;
	}

	@SuppressWarnings("rawtypes")
	private Collection<? extends ItemDelta> createShadowResultModification(Change change,
			OperationResult shadowResult) {
		PrismObjectDefinition<ShadowType> shadowDefinition = getResourceObjectShadowDefinition();

		Collection<ItemDelta> modifications = new ArrayList<ItemDelta>();
		PropertyDelta resultDelta = PropertyDelta.createModificationReplaceProperty(ShadowType.F_RESULT,
				shadowDefinition, shadowResult.createOperationResultType());
		modifications.add(resultDelta);
		if (change.getObjectDelta() != null && change.getObjectDelta().getChangeType() == ChangeType.DELETE) {
			PropertyDelta failedOperationTypeDelta = PropertyDelta.createModificationReplaceProperty(
					ShadowType.F_FAILED_OPERATION_TYPE, shadowDefinition, FailedOperationTypeType.DELETE);
			modifications.add(failedOperationTypeDelta);
		}
		return modifications;
	}

	private String getOidFromChange(Change change) {
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

	private void deleteShadowFromRepo(Change change, OperationResult parentResult)
			throws ObjectNotFoundException {
		if (change.getObjectDelta() != null && change.getObjectDelta().getChangeType() == ChangeType.DELETE
				&& change.getOldShadow() != null) {
			LOGGER.trace("Deleting detected shadow object form repository.");
			try {
				repositoryService.deleteObject(ShadowType.class, change.getOldShadow().getOid(),
						parentResult);
				LOGGER.debug("Shadow object successfully deleted form repository.");
			} catch (ObjectNotFoundException ex) {
				// What we want to delete is already deleted. Not a big problem.
				LOGGER.debug("Shadow object {} already deleted from repository ({})", change.getOldShadow(),
						ex);
				parentResult.recordHandledError(
						"Shadow object " + change.getOldShadow() + " already deleted from repository", ex);
			}

		}
	}

	void processChange(ProvisioningContext ctx, Change change, PrismObject<ShadowType> oldShadow,
			OperationResult parentResult) throws SchemaException, CommunicationException,
					ConfigurationException, SecurityViolationException, ObjectNotFoundException,
					GenericConnectorException, ObjectAlreadyExistsException {

		if (oldShadow == null) {
			oldShadow = shadowManager.findOrAddShadowFromChange(ctx, change, parentResult);
		}

		if (oldShadow != null) {
			applyAttributesDefinition(ctx, oldShadow);

			LOGGER.trace("Old shadow: {}", oldShadow);

			// skip setting other attribute when shadow is null
			if (oldShadow == null) {
				change.setOldShadow(null);
				return;
			}

			ProvisioningUtil.setProtectedFlag(ctx, oldShadow, matchingRuleRegistry);
			change.setOldShadow(oldShadow);

			if (change.getCurrentShadow() != null) {
				PrismObject<ShadowType> currentShadow = completeShadow(ctx, change.getCurrentShadow(),
						oldShadow, parentResult);
				change.setCurrentShadow(currentShadow);
				shadowManager.updateShadow(ctx, currentShadow, oldShadow, parentResult);
			}

			// FIXME: hack. the object delta must have oid specified.
			if (change.getObjectDelta() != null && change.getObjectDelta().getOid() == null) {
				// if (LOGGER.isTraceEnabled()) {
				// LOGGER.trace("No OID present, assuming delta of type DELETE;
				// change = {}\nobjectDelta: {}", change,
				// change.getObjectDelta().debugDump());
				// }
				// ObjectDelta<ShadowType> objDelta = new
				// ObjectDelta<ShadowType>(ShadowType.class, ChangeType.DELETE,
				// prismContext);
				// change.setObjectDelta(objDelta);
				change.getObjectDelta().setOid(oldShadow.getOid());
			}
		} else {
			LOGGER.debug(
					"No old shadow for synchronization event {}, the shadow must be gone in the meantime (this is probably harmless)",
					change);
		}

	}

	public PrismProperty<?> fetchCurrentToken(ResourceShadowDiscriminator shadowCoordinates,
			OperationResult parentResult) throws ObjectNotFoundException, CommunicationException,
					SchemaException, ConfigurationException {
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

	private void applyAttributesDefinition(ProvisioningContext ctx, ObjectDelta<ShadowType> delta)
			throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException {
		if (delta.isAdd()) {
			applyAttributesDefinition(ctx, delta.getObjectToAdd());
		} else if (delta.isModify()) {
			for (ItemDelta<?, ?> itemDelta : delta.getModifications()) {
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
			return; // should never occur
		}
		PrismContainerValue<ShadowAttributesType> pcv = (PrismContainerValue<ShadowAttributesType>) value;
		for (Item item : pcv.getItems()) {
			ItemDefinition itemDef = item.getDefinition();
			if (itemDef == null || !(itemDef instanceof ResourceAttributeDefinition)) {
				QName attributeName = item.getElementName();
				ResourceAttributeDefinition attributeDefinition = ctx.getObjectClassDefinition()
						.findAttributeDefinition(attributeName);
				if (attributeDefinition == null) {
					throw new SchemaException("No definition for attribute " + attributeName);
				}
				if (itemDef != null) {
					// We are going to rewrite the definition anyway. Let's just
					// do some basic checks first
					if (!QNameUtil.match(itemDef.getTypeName(), attributeDefinition.getTypeName())) {
						throw new SchemaException("The value of type " + itemDef.getTypeName()
								+ " cannot be applied to attribute " + attributeName + " which is of type "
								+ attributeDefinition.getTypeName());
					}
				}
				item.applyDefinition(attributeDefinition);
			}
		}
	}

	private <V extends PrismValue, D extends ItemDefinition> void applyAttributeDefinition(
			ProvisioningContext ctx, ObjectDelta<ShadowType> delta, ItemDelta<V, D> itemDelta)
					throws SchemaException, ConfigurationException, ObjectNotFoundException,
					CommunicationException {
		if (!SchemaConstants.PATH_ATTRIBUTES.equivalent(itemDelta.getParentPath())) { // just
																						// to
																						// be
																						// sure
			return;
		}
		D itemDef = itemDelta.getDefinition();
		if (itemDef == null || !(itemDef instanceof ResourceAttributeDefinition)) {
			QName attributeName = itemDelta.getElementName();
			ResourceAttributeDefinition attributeDefinition = ctx.getObjectClassDefinition()
					.findAttributeDefinition(attributeName);
			if (attributeDefinition == null) {
				throw new SchemaException(
						"No definition for attribute " + attributeName + " in object delta " + delta);
			}
			if (itemDef != null) {
				// We are going to rewrite the definition anyway. Let's just do
				// some basic checks first
				if (!QNameUtil.match(itemDef.getTypeName(), attributeDefinition.getTypeName())) {
					throw new SchemaException("The value of type " + itemDef.getTypeName()
							+ " cannot be applied to attribute " + attributeName + " which is of type "
							+ attributeDefinition.getTypeName());
				}
			}
			itemDelta.applyDefinition((D) attributeDefinition);
		}
	}

	private ProvisioningContext applyAttributesDefinition(ProvisioningContext ctx,
			PrismObject<ShadowType> shadow) throws SchemaException, ConfigurationException,
					ObjectNotFoundException, CommunicationException {
		ProvisioningContext subctx = ctx.spawn(shadow);
		RefinedObjectClassDefinition objectClassDefinition = subctx.getObjectClassDefinition();

		PrismContainer<ShadowAttributesType> attributesContainer = shadow
				.findContainer(ShadowType.F_ATTRIBUTES);
		if (attributesContainer != null) {
			if (attributesContainer instanceof ResourceAttributeContainer) {
				if (attributesContainer.getDefinition() == null) {
					attributesContainer
							.applyDefinition(objectClassDefinition.toResourceAttributeContainerDefinition());
				}
			} else {
				try {
					// We need to convert <attributes> to
					// ResourceAttributeContainer
					ResourceAttributeContainer convertedContainer = ResourceAttributeContainer
							.convertFromContainer(attributesContainer, objectClassDefinition);
					shadow.getValue().replace(attributesContainer, convertedContainer);
				} catch (SchemaException e) {
					throw new SchemaException(e.getMessage() + " in " + shadow, e);
				}
			}
		}

		// We also need to replace the entire object definition to inject
		// correct object class definition here
		// If we don't do this then the patch (delta.applyTo) will not work
		// correctly because it will not be able to
		// create the attribute container if needed.

		PrismObjectDefinition<ShadowType> objectDefinition = shadow.getDefinition();
		PrismContainerDefinition<ShadowAttributesType> origAttrContainerDef = objectDefinition
				.findContainerDefinition(ShadowType.F_ATTRIBUTES);
		if (origAttrContainerDef == null
				|| !(origAttrContainerDef instanceof ResourceAttributeContainerDefinition)) {
			PrismObjectDefinition<ShadowType> clonedDefinition = objectDefinition.cloneWithReplacedDefinition(
					ShadowType.F_ATTRIBUTES, objectClassDefinition.toResourceAttributeContainerDefinition());
			shadow.setDefinition(clonedDefinition);
		}

		return subctx;
	}

	/**
	 * Reapplies definition to the shadow if needed. The definition needs to be
	 * reapplied e.g. if the shadow has auxiliary object classes, it if subclass
	 * of the object class that was originally requested, etc.
	 */
	private ProvisioningContext reapplyDefinitions(ProvisioningContext ctx,
			PrismObject<ShadowType> rawResourceShadow) throws SchemaException, ConfigurationException,
					ObjectNotFoundException, CommunicationException {
		ShadowType rawResourceShadowType = rawResourceShadow.asObjectable();
		QName objectClassQName = rawResourceShadowType.getObjectClass();
		List<QName> auxiliaryObjectClassQNames = rawResourceShadowType.getAuxiliaryObjectClass();
		if (auxiliaryObjectClassQNames.isEmpty()
				&& objectClassQName.equals(ctx.getObjectClassDefinition().getTypeName())) {
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
	private PrismObject<ShadowType> completeShadow(ProvisioningContext ctx,
			PrismObject<ShadowType> resourceShadow, PrismObject<ShadowType> repoShadow,
			OperationResult parentResult)
					throws SchemaException, ConfigurationException, ObjectNotFoundException,
					CommunicationException, SecurityViolationException, GenericConnectorException {

		PrismObject<ShadowType> resultShadow = repoShadow.clone();

		// The real definition may be different than that of repo shadow (e.g.
		// different auxiliary object classes).
		resultShadow.applyDefinition(ctx.getObjectClassDefinition().getObjectDefinition(), true);

		assert resultShadow.getPrismContext() != null : "No prism context in resultShadow";

		ResourceAttributeContainer resourceAttributesContainer = ShadowUtil
				.getAttributesContainer(resourceShadow);

		ShadowType resultShadowType = resultShadow.asObjectable();
		ShadowType repoShadowType = repoShadow.asObjectable();
		ShadowType resourceShadowType = resourceShadow.asObjectable();

		Collection<QName> auxObjectClassQNames = new ArrayList<>();

		// Always take auxiliary object classes from the resource. Unlike
		// structural object classes
		// the auxiliary object classes may change.
		resultShadow.removeProperty(ShadowType.F_AUXILIARY_OBJECT_CLASS);
		PrismProperty<QName> resourceAuxOcProp = resourceShadow
				.findProperty(ShadowType.F_AUXILIARY_OBJECT_CLASS);
		if (resourceAuxOcProp != null) {
			PrismProperty<QName> resultAuxOcProp = resultShadow
					.findOrCreateProperty(ShadowType.F_AUXILIARY_OBJECT_CLASS);
			resultAuxOcProp.addAll(PrismPropertyValue.cloneCollection(resourceAuxOcProp.getValues()));
			auxObjectClassQNames.addAll(resultAuxOcProp.getRealValues());
		}

		resultShadowType.setName(new PolyStringType(ShadowUtil.determineShadowName(resourceShadow)));
		if (resultShadowType.getObjectClass() == null) {
			resultShadowType.setObjectClass(resourceAttributesContainer.getDefinition().getTypeName());
		}
		if (resultShadowType.getResource() == null) {
			resultShadowType.setResourceRef(ObjectTypeUtil.createObjectRef(ctx.getResource()));
		}

		// Attributes
		resultShadow.removeContainer(ShadowType.F_ATTRIBUTES);
		ResourceAttributeContainer resultAttibutes = resourceAttributesContainer.clone();
		accessChecker.filterGetAttributes(resultAttibutes, ctx.computeCompositeObjectClassDefinition(auxObjectClassQNames), parentResult);
		resultShadow.add(resultAttibutes);

		resultShadowType.setIgnored(resourceShadowType.isIgnored());

		resultShadowType.setActivation(resourceShadowType.getActivation());

		ShadowType resultAccountShadow = resultShadow.asObjectable();
		ShadowType resourceAccountShadow = resourceShadow.asObjectable();

		// Credentials
		resultAccountShadow.setCredentials(resourceAccountShadow.getCredentials());
		transplantPasswordMetadata(repoShadowType, resultAccountShadow);

		// protected
		ProvisioningUtil.setProtectedFlag(ctx, resultShadow, matchingRuleRegistry);

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
		PrismContainer<ShadowAssociationType> resourceAssociationContainer = resourceShadow
				.findContainer(ShadowType.F_ASSOCIATION);
		if (resourceAssociationContainer != null) {
			PrismContainer<ShadowAssociationType> associationContainer = resourceAssociationContainer.clone();
			resultShadow.addReplaceExisting(associationContainer);
			if (associationContainer != null) {
				for (PrismContainerValue<ShadowAssociationType> associationCVal : associationContainer
						.getValues()) {
					ResourceAttributeContainer identifierContainer = ShadowUtil
							.getAttributesContainer(associationCVal, ShadowAssociationType.F_IDENTIFIERS);
					Collection<ResourceAttribute<?>> entitlementIdentifiers = identifierContainer
							.getAttributes();
					if (entitlementIdentifiers == null || entitlementIdentifiers.isEmpty()) {
						throw new IllegalStateException(
								"No entitlement identifiers present for association " + associationCVal + " " + ctx.getDesc());
					}
					ShadowAssociationType shadowAssociationType = associationCVal.asContainerable();
					QName associationName = shadowAssociationType.getName();
					RefinedAssociationDefinition rEntitlementAssociation = ctx.getObjectClassDefinition()
							.findEntitlementAssociationDefinition(associationName);
					if (rEntitlementAssociation == null) {
						if (LOGGER.isTraceEnabled()) {
							LOGGER.trace("Entitlement association with name {} couldn't be found in {} {}\nresource shadow:\n{}\nrepo shadow:\n{}",
									new Object[]{ associationName, ctx.getObjectClassDefinition(), ctx.getDesc(), 
											resourceShadow.debugDump(1), repoShadow==null?null:repoShadow.debugDump(1)});
							LOGGER.trace("Full refined definition: {}", ctx.getObjectClassDefinition().debugDump());
						}
						throw new SchemaException("Entitlement association with name " + associationName
								+ " couldn't be found in " + ctx.getObjectClassDefinition() + " " + ctx.getDesc() + ", with using shadow coordinates " + ctx.isUseRefinedDefinition());
					}
					for (String intent : rEntitlementAssociation.getIntents()) {
						ProvisioningContext ctxEntitlement = ctx.spawn(ShadowKindType.ENTITLEMENT, intent);

						PrismObject<ShadowType> entitlementRepoShadow;
						PrismObject<ShadowType> entitlementShadow = (PrismObject<ShadowType>) identifierContainer
								.getUserData(ResourceObjectConverter.FULL_SHADOW_KEY);
						if (entitlementShadow == null) {
							try {
								entitlementRepoShadow = shadowManager.lookupShadowInRepository(ctxEntitlement,
										identifierContainer, parentResult);
								if (entitlementRepoShadow == null) {

									entitlementShadow = resouceObjectConverter.locateResourceObject(
											ctxEntitlement, entitlementIdentifiers, parentResult);

									// Try to look up repo shadow again, this
									// time with full resource shadow. When we
									// have searched before we might
									// have only some identifiers. The shadow
									// might still be there, but it may be
									// renamed
									entitlementRepoShadow = shadowManager.lookupShadowInRepository(
											ctxEntitlement, entitlementShadow, parentResult);

									if (entitlementRepoShadow == null) {
										entitlementRepoShadow = createShadowInRepository(ctxEntitlement,
												entitlementShadow, false, parentResult);
									}
								}
							} catch (ObjectNotFoundException e) {
								// The entitlement to which we point is not
								// there.
								// Simply ignore this association value.
								parentResult.muteLastSubresultError();
								LOGGER.warn(
										"The entitlement identified by {} referenced from {} does not exist. Skipping.",
										new Object[] { associationCVal, resourceShadow });
								continue;
							} catch (SchemaException e) {
								// The entitlement to which we point is not bad.
								// Simply ignore this association value.
								parentResult.muteLastSubresultError();
								LOGGER.warn(
										"The entitlement identified by {} referenced from {} violates the schema. Skipping. Original error: {}",
										new Object[] { associationCVal, resourceShadow, e.getMessage(), e });
								continue;
							}
						} else {
							entitlementRepoShadow = lookupOrCreateShadowInRepository(ctxEntitlement,
									entitlementShadow, false, parentResult);
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
		assert resultName != null : "No name generated in " + resultShadow;
		assert !StringUtils.isEmpty(resultName.getOrig()) : "No name (orig) in " + resultShadow;
		assert !StringUtils.isEmpty(resultName.getNorm()) : "No name (norm) in " + resultShadow;

		return resultShadow;
	}
	
	private void transplantPasswordMetadata(ShadowType repoShadowType, ShadowType resultAccountShadow) {
		CredentialsType repoCreds = repoShadowType.getCredentials();
		if (repoCreds == null) {
			return;
		}
		PasswordType repoPassword = repoCreds.getPassword();
		if (repoPassword == null) {
			return;
		}
		MetadataType repoMetadata = repoPassword.getMetadata();
		if (repoMetadata == null) {
			return;
		}
		CredentialsType resultCreds = resultAccountShadow.getCredentials();
		if (resultCreds == null) {
			resultCreds = new CredentialsType();
			resultAccountShadow.setCredentials(resultCreds);
		}
		PasswordType resultPassword = resultCreds.getPassword();
		if (resultPassword == null) {
			resultPassword = new PasswordType();
			resultCreds.setPassword(resultPassword);
		}
		MetadataType resultMetadata = resultPassword.getMetadata();
		if (resultMetadata == null) {
			resultMetadata = repoMetadata.clone();
			resultPassword.setMetadata(resultMetadata);
		}
	}

	// ENTITLEMENTS

	/**
	 * Makes sure that all the entitlements have identifiers in them so this is
	 * usable by the ResourceObjectConverter.
	 */
	private void preprocessEntitlements(final ProvisioningContext ctx, final PrismObject<ShadowType> shadow,
			final OperationResult result) throws SchemaException, ObjectNotFoundException,
					ConfigurationException, CommunicationException {
		Visitor visitor = new Visitor() {
			@Override
			public void visit(Visitable visitable) {
				try {
					preprocessEntitlement(ctx, (PrismContainerValue<ShadowAssociationType>) visitable,
							shadow.toString(), result);
				} catch (SchemaException | ObjectNotFoundException | ConfigurationException
						| CommunicationException e) {
					throw new TunnelException(e);
				}
			}
		};
		try {
			shadow.accept(visitor, new ItemPath(new NameItemPathSegment(ShadowType.F_ASSOCIATION),
					IdItemPathSegment.WILDCARD), false);
		} catch (TunnelException e) {
			Throwable cause = e.getCause();
			if (cause instanceof SchemaException) {
				throw (SchemaException) cause;
			} else if (cause instanceof ObjectNotFoundException) {
				throw (ObjectNotFoundException) cause;
			} else if (cause instanceof ConfigurationException) {
				throw (ConfigurationException) cause;
			} else if (cause instanceof CommunicationException) {
				throw (CommunicationException) cause;
			} else {
				throw new SystemException("Unexpected exception " + cause, cause);
			}
		}
	}

	/**
	 * Makes sure that all the entitlements have identifiers in them so this is
	 * usable by the ResourceObjectConverter.
	 */
	private void preprocessEntitlements(final ProvisioningContext ctx,
			Collection<? extends ItemDelta> modifications, final String desc, final OperationResult result)
					throws SchemaException, ObjectNotFoundException, ConfigurationException,
					CommunicationException {
		Visitor visitor = new Visitor() {
			@Override
			public void visit(Visitable visitable) {
				try {
					preprocessEntitlement(ctx, (PrismContainerValue<ShadowAssociationType>) visitable, desc,
							result);
				} catch (SchemaException | ObjectNotFoundException | ConfigurationException
						| CommunicationException e) {
					throw new TunnelException(e);
				}
			}
		};
		try {
			ItemDelta.accept(modifications, visitor, new ItemPath(
					new NameItemPathSegment(ShadowType.F_ASSOCIATION), IdItemPathSegment.WILDCARD), false);
		} catch (TunnelException e) {
			Throwable cause = e.getCause();
			if (cause instanceof SchemaException) {
				throw (SchemaException) cause;
			} else if (cause instanceof ObjectNotFoundException) {
				throw (ObjectNotFoundException) cause;
			} else if (cause instanceof ConfigurationException) {
				throw (ConfigurationException) cause;
			} else if (cause instanceof CommunicationException) {
				throw (CommunicationException) cause;
			} else {
				throw new SystemException("Unexpected exception " + cause, cause);
			}
		}
	}

	private void preprocessEntitlement(ProvisioningContext ctx,
			PrismContainerValue<ShadowAssociationType> association, String desc, OperationResult result)
					throws SchemaException, ObjectNotFoundException, ConfigurationException,
					CommunicationException {
		PrismContainer<Containerable> identifiersContainer = association
				.findContainer(ShadowAssociationType.F_IDENTIFIERS);
		if (identifiersContainer != null && !identifiersContainer.isEmpty()) {
			// We already have identifiers here
			return;
		}
		ShadowAssociationType associationType = association.asContainerable();
		if (associationType.getShadowRef() == null
				|| StringUtils.isEmpty(associationType.getShadowRef().getOid())) {
			throw new SchemaException(
					"No identifiers and no OID specified in entitlements association " + association);
		}
		PrismObject<ShadowType> repoShadow;
		try {
			repoShadow = repositoryService.getObject(ShadowType.class,
					associationType.getShadowRef().getOid(), null, result);
		} catch (ObjectNotFoundException e) {
			throw new ObjectNotFoundException(e.getMessage()
					+ " while resolving entitlement association OID in " + association + " in " + desc, e);
		}
		applyAttributesDefinition(ctx, repoShadow);
		transplantIdentifiers(association, repoShadow);
	}

	private void transplantIdentifiers(PrismContainerValue<ShadowAssociationType> association,
			PrismObject<ShadowType> repoShadow) throws SchemaException {
		PrismContainer<Containerable> identifiersContainer = association
				.findContainer(ShadowAssociationType.F_IDENTIFIERS);
		if (identifiersContainer == null) {
			ResourceAttributeContainer origContainer = ShadowUtil.getAttributesContainer(repoShadow);
			identifiersContainer = new ResourceAttributeContainer(ShadowAssociationType.F_IDENTIFIERS,
					origContainer.getDefinition(), prismContext);
			association.add(identifiersContainer);
		}
		Collection<ResourceAttribute<?>> identifiers = ShadowUtil.getPrimaryIdentifiers(repoShadow);
		for (ResourceAttribute<?> identifier : identifiers) {
			identifiersContainer.add(identifier.clone());
		}
		Collection<ResourceAttribute<?>> secondaryIdentifiers = ShadowUtil
				.getSecondaryIdentifiers(repoShadow);
		for (ResourceAttribute<?> identifier : secondaryIdentifiers) {
			identifiersContainer.add(identifier.clone());
		}
	}

}
