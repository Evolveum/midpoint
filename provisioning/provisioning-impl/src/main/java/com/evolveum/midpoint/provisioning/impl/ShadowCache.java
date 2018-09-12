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

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.common.crypto.CryptoUtil;
import com.evolveum.midpoint.common.refinery.RefinedAssociationDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.ShadowDiscriminatorObjectDelta;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
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
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
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
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

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
	
	public static String OP_PROCESS_SYNCHRONIZATION = ShadowCache.class.getName() + ".processSynchronization";
	public static String OP_DELAYED_OPERATION = ShadowCache.class.getName() + ".delayedOperation";

	@Autowired(required = true)
	@Qualifier("cacheRepositoryService")
	private RepositoryService repositoryService;

	@Autowired private ErrorHandlerFactory errorHandlerFactory;
	@Autowired private ResourceManager resourceManager;
	@Autowired private Clock clock;
	@Autowired private PrismContext prismContext;
	@Autowired private ResourceObjectConverter resouceObjectConverter;
	@Autowired private ShadowCaretaker shadowCaretaker;
	@Autowired private MatchingRuleRegistry matchingRuleRegistry;
	@Autowired protected ShadowManager shadowManager;
	@Autowired private ChangeNotificationDispatcher operationListener;
	@Autowired private AccessChecker accessChecker;
	@Autowired private TaskManager taskManager;
	@Autowired private ChangeNotificationDispatcher changeNotificationDispatcher;
	@Autowired private ProvisioningContextFactory ctxFactory;
	@Autowired private Protector protector;

	private static final Trace LOGGER = TraceManager.getTrace(ShadowCache.class);

	public ShadowCache() {
		repositoryService = null;
	}

	/**
	 * Get the value of repositoryService.
	 * 
	 * DO NOT USE. Only ShadowManager should access repository
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
					ConfigurationException, SecurityViolationException, ExpressionEvaluationException, EncryptionException {

		Validate.notNull(oid, "Object id must not be null.");

		if (repositoryShadow == null) {
			LOGGER.trace("Start getting object with oid {}", oid);
		} else {
			LOGGER.trace("Start getting object {}", repositoryShadow);
		}

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
			shadowCaretaker.applyAttributesDefinition(ctx, repositoryShadow);
		} catch (ObjectNotFoundException | SchemaException | CommunicationException
				| ConfigurationException | ExpressionEvaluationException e) {
			throw e;
		}
		
		ResourceType resource = ctx.getResource();
		XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
		
		if (GetOperationOptions.isNoFetch(rootOptions) || GetOperationOptions.isRaw(rootOptions)) {
			return processNoFetchGet(ctx, repositoryShadow, options, now, parentResult);
		}

		if (!ResourceTypeUtil.isReadCapabilityEnabled(resource)) {
			UnsupportedOperationException e = new UnsupportedOperationException("Resource does not support 'read' operation");
			parentResult.recordFatalError(e);
			throw e;
		}
		
		repositoryShadow = refreshShadow(repositoryShadow, task, parentResult);
		if (repositoryShadow == null) {
			// Dead shadow was just removed
			// TODO: is this OK? What about re-appeared objects
			ObjectNotFoundException e = new ObjectNotFoundException("Resource object does not exist");
			parentResult.recordFatalError(e);
			throw e;
		}
		
		if (canReturnCached(options, repositoryShadow, resource)) {
			PrismObject<ShadowType> resultShadow = futurizeShadow(ctx, repositoryShadow, options, now);
			shadowCaretaker.applyAttributesDefinition(ctx, resultShadow);
			validateShadow(resultShadow, true);
			return resultShadow;
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
						"No primary identifiers found in the repository shadow " + repositoryShadow, ex);
				throw ex;
			}
			
			Collection<? extends ResourceAttribute<?>> identifiers = ShadowUtil
					.getAllIdentifiers(repositoryShadow);
			
			try {
				
				resourceShadow = resouceObjectConverter.getResourceObject(ctx, identifiers, true, parentResult);
				
			} catch (ObjectNotFoundException e) {
				// This may be OK, e.g. for connectors that have running async add operation.
				if (canReturnCachedAfterNotFoundOnResource(options, repositoryShadow, resource)) {
					LOGGER.trace("Object not found on reading of {}, but we can return cached shadow", repositoryShadow);
					parentResult.deleteLastSubresultIfError();		// we don't want to see 'warning-like' orange boxes in GUI (TODO reconsider this)
					parentResult.recordSuccess();
					repositoryShadow.asObjectable().setExists(false);
					PrismObject<ShadowType> resultShadow = futurizeShadow(ctx, repositoryShadow, options, now);
					shadowCaretaker.applyAttributesDefinition(ctx, resultShadow);
					LOGGER.trace("Returning futurized shadow:\n{}", DebugUtil.debugDumpLazily(resultShadow));
					validateShadow(resultShadow, true);
					return resultShadow;
				} else {
					throw e;
				}
			}
			
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Shadow returned by ResouceObjectConverter:\n{}", resourceShadow.debugDump(1));
			}

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

			resultShadow = futurizeShadow(ctx, resultShadow, options, now);
			parentResult.recordSuccess();
			validateShadow(resultShadow, true);
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
				validateShadow(resourceShadow, true);
				return resourceShadow;

			} catch (GenericFrameworkException | ObjectAlreadyExistsException e) {
				throw new SystemException(e.getMessage(), e);
			}
		} finally {
			// We need to record the fetch down here. Now it is certain that we
			// are going to fetch from resource
			// (we do not have raw/noFetch option)
			// TODO: is this correct behaviour? don't we really want to record
			// fetch for protected objects?
			if (!ShadowUtil.isProtected(resourceShadow)) {
				InternalMonitor.recordCount(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

			}
		}
	}

	private PrismObject<ShadowType> processNoFetchGet(ProvisioningContext ctx, PrismObject<ShadowType> repositoryShadow,
			Collection<SelectorOptions<GetOperationOptions>> options, XMLGregorianCalendar now, OperationResult parentResult) 
					throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException, EncryptionException {
		ResourceType resource = ctx.getResource();
		ShadowType repositoryShadowType = repositoryShadow.asObjectable();
		
		LOGGER.trace("Processing noFetch get for {}", repositoryShadow);
		
		// Even with noFetch we still want to delete expired pending operations. And even delete
		// the shadow if needed.
		ObjectDelta<ShadowType> shadowDelta = repositoryShadow.createModifyDelta();
		boolean atLeastOnePendingOperationRemains = expirePendingOperations(ctx, repositoryShadow, shadowDelta, now, parentResult);
		if (repositoryShadowType.getFailedOperationType() != null) {
			atLeastOnePendingOperationRemains = true;
		}
		if (ShadowUtil.isDead(repositoryShadowType) && !atLeastOnePendingOperationRemains) {
			LOGGER.trace("Removing dead shadow with no pending operation: {}", repositoryShadow);
			shadowManager.deleteShadow(ctx, repositoryShadow, parentResult);
			ObjectNotFoundException e = new ObjectNotFoundException("Resource object not found");
			parentResult.recordFatalError(e);
			throw e;
		}
		
		if (!shadowDelta.isEmpty()) {
			shadowManager.modifyShadowAttributes(ctx, repositoryShadow, shadowDelta.getModifications(), parentResult);
			shadowDelta.applyTo(repositoryShadow);
		}
		
		PrismObject<ShadowType> resultShadow = futurizeShadow(ctx, repositoryShadow, options, now);
		shadowCaretaker.applyAttributesDefinition(ctx, resultShadow);
		
		return resultShadow;
	}

	private PrismObject<ShadowType> futurizeShadow(ProvisioningContext ctx, PrismObject<ShadowType> shadow,
			Collection<SelectorOptions<GetOperationOptions>> options, XMLGregorianCalendar now) throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {
		PointInTimeType pit = GetOperationOptions.getPointInTimeType(SelectorOptions.findRootOptions(options));
		if (pit != PointInTimeType.FUTURE) {
			return shadow;
		}
		return shadowCaretaker.applyPendingOperations(ctx, shadow, now);
	}

	private boolean canReturnCachedAfterNotFoundOnResource(Collection<SelectorOptions<GetOperationOptions>> options, PrismObject<ShadowType> repositoryShadow, ResourceType resource) {
		if (repositoryShadow.asObjectable().getPendingOperation().isEmpty()) {
			return false;
		}
		// Explicitly check the capability of the resource (primary connector), not capabilities of additional connectors
		ReadCapabilityType readCapabilityType = CapabilityUtil.getEffectiveCapability(resource.getCapabilities(), ReadCapabilityType.class);
		if (readCapabilityType == null) {
			return false;
		}
		if (!CapabilityUtil.isCapabilityEnabled(readCapabilityType)) {
			return false;
		}
		return Boolean.TRUE.equals(readCapabilityType.isCachingOnly());
	}
	
	private boolean canReturnCached(Collection<SelectorOptions<GetOperationOptions>> options, PrismObject<ShadowType> repositoryShadow, ResourceType resource) throws ConfigurationException {
		if (ProvisioningUtil.resourceReadIsCachingOnly(resource)) {
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
	
	public abstract String afterAddOnResource(
			ProvisioningContext ctx, 
			PrismObject<ShadowType> shadowToAdd, 
			ProvisioningOperationState<AsynchronousOperationReturnValue<PrismObject<ShadowType>>> opState,
			OperationResult parentResult) 
					throws SchemaException, ObjectAlreadyExistsException,
					ObjectNotFoundException, ConfigurationException, CommunicationException, ExpressionEvaluationException, EncryptionException;

	public String addShadow(PrismObject<ShadowType> shadowToAdd, OperationProvisioningScriptsType scripts,
			ResourceType resource, ProvisioningOperationOptions options, Task task,
			OperationResult parentResult) throws CommunicationException, GenericFrameworkException,
					ObjectAlreadyExistsException, SchemaException, ObjectNotFoundException,
					ConfigurationException, SecurityViolationException, ExpressionEvaluationException, EncryptionException {
		Validate.notNull(shadowToAdd, "Object to add must not be null.");

		InternalMonitor.recordCount(InternalCounters.SHADOW_CHANGE_OPERATION_COUNT);

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Start adding shadow object:\n{}", shadowToAdd.debugDump(1));
		}

		ProvisioningContext ctx = ctxFactory.create(shadowToAdd, task, parentResult);
		try {
			ctx.assertDefinition();
		} catch (SchemaException e) {
			handleError(ctx, e, shadowToAdd, FailedOperation.ADD, null, 
					isDoDiscovery(resource, options), true, parentResult);
			return null;
		}

//		if (LOGGER.isTraceEnabled()) {
//			LOGGER.trace("Definition:\n{}", ctx.getObjectClassDefinition().debugDump());
//		}

		PrismContainer<?> attributesContainer = shadowToAdd.findContainer(ShadowType.F_ATTRIBUTES);
		if (attributesContainer == null || attributesContainer.isEmpty()) {
			SchemaException e = new SchemaException(
					"Attempt to add shadow without any attributes: " + shadowToAdd);
			parentResult.recordFatalError(e);
			handleError(ctx, e, shadowToAdd, FailedOperation.ADD, null, 
					isDoDiscovery(resource, options), true, parentResult);
			return null;
		}
		if (!(attributesContainer instanceof ResourceAttributeContainer)) {
			shadowCaretaker.applyAttributesDefinition(ctx, shadowToAdd);
			attributesContainer = shadowToAdd.findContainer(ShadowType.F_ATTRIBUTES);
		}
		
		preAddChecks(ctx, shadowToAdd, task, parentResult);
		
		String proposedShadowOid = shadowManager.addNewProposedShadow(ctx, shadowToAdd, task, parentResult);

		ProvisioningOperationState<AsynchronousOperationReturnValue<PrismObject<ShadowType>>> opState = new ProvisioningOperationState<>();
		if (proposedShadowOid == null && shadowToAdd.getOid() != null) {
			// legacy ... from old consistency code
			opState.setExistingShadowOid(shadowToAdd.getOid());
		}
		opState.setExistingShadowOid(proposedShadowOid);
		PrismObject<ShadowType> addedShadow = null;

		preprocessEntitlements(ctx, shadowToAdd, parentResult);

		shadowCaretaker.applyAttributesDefinition(ctx, shadowToAdd);
		shadowManager.setKindIfNecessary(shadowToAdd.asObjectable(), ctx.getObjectClassDefinition());
		accessChecker.checkAdd(ctx, shadowToAdd, parentResult);

		if (shouldExecuteResourceOperationDirectly(ctx)) {
			
			LOGGER.trace("ADD {}: resource operation, execution starting", shadowToAdd);
			
			try {
	
				// RESOURCE OPERATION: add
				AsynchronousOperationReturnValue<PrismObject<ShadowType>> asyncReturnValue = 
						resouceObjectConverter.addResourceObject(ctx, shadowToAdd, scripts, false, parentResult);
				opState.processAsyncResult(asyncReturnValue);
				addedShadow = asyncReturnValue.getReturnValue();

			} catch (ObjectAlreadyExistsException e) {
			
				// This exception may still be OK in some cases. Such as:
				// We are trying to add a shadow to a semi-manual connector.
				// But the resource object was recently deleted. The object is
				// still in the backing store (CSV file) because of a grace
				// period. Obviously, attempt to add such object would fail.
				// So, we need to handle this case specially. (MID-4414)
				
				OperationResult failedOperationResult = parentResult.getLastSubresult();
				
				if (hasDeadShadowWithDeleteOperation(ctx, shadowToAdd, parentResult)) {
					
					if (failedOperationResult.isError()) {
						failedOperationResult.setStatus(OperationResultStatus.HANDLED_ERROR);
					}
					
					// Try again, this time without explicit uniqueness check
					try {
						
						LOGGER.trace("ADD {}: retrying resource operation without uniquness check (previous dead shadow found), execution starting", shadowToAdd);
						AsynchronousOperationReturnValue<PrismObject<ShadowType>> asyncReturnValue = 
								resouceObjectConverter.addResourceObject(ctx, shadowToAdd, scripts, true, parentResult);
						opState.processAsyncResult(asyncReturnValue);
						addedShadow = asyncReturnValue.getReturnValue();
						
					} catch (Exception innerException) {
						return handleAddError(ctx, shadowToAdd, resource, options, proposedShadowOid, innerException, task, parentResult);
					}
						
				} else {
					return handleAddError(ctx, shadowToAdd, resource, options, proposedShadowOid, e, task, parentResult);
				}
				
			} catch (Exception e) {
				return handleAddError(ctx, shadowToAdd, resource, options, proposedShadowOid, e, task, parentResult);
			}
			
			LOGGER.debug("ADD {}: resource operation executed, operation state: {}", shadowToAdd, opState.shortDumpLazily());
			
		} else {
			opState.setExecutionStatus(PendingOperationExecutionStatusType.EXECUTION_PENDING);
			// Create dummy subresult with IN_PROGRESS state. 
			// This will force the entire result (parent) to be IN_PROGRESS rather than SUCCESS.
			OperationResult delayedSubresult = parentResult.createSubresult(OP_DELAYED_OPERATION);
			delayedSubresult.setStatus(OperationResultStatus.IN_PROGRESS);
			LOGGER.debug("ADD {}: resource operation NOT executed, execution pending", shadowToAdd);
		}

		// REPO OPERATION: add
		// This is where the repo shadow is created or updated (if needed)
		String oid = afterAddOnResource(ctx, shadowToAdd, opState, parentResult);
		
		if (addedShadow != null) {
			addedShadow.setOid(oid);
		} else {
			addedShadow = shadowToAdd;
		}

		notifyAfterAdd(ctx, addedShadow, opState, task, parentResult);
		
		return oid;
	}
	
	private boolean hasDeadShadowWithDeleteOperation(ProvisioningContext ctx,
			PrismObject<ShadowType> shadowToAdd,
			OperationResult parentResult)
					throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		
		
		Collection<PrismObject<ShadowType>> previousDeadShadows = shadowManager.lookForPreviousDeadShadows(ctx, shadowToAdd, parentResult);
		if (previousDeadShadows.isEmpty()) {
			return false;
		}
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Previous dead shadows:\n{}", DebugUtil.debugDump(previousDeadShadows, 1));
		}
		
		XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
		for (PrismObject<ShadowType> previousDeadShadow : previousDeadShadows) {
			if (hasPreviousDeletePendingOperationInGracePeriod(ctx, previousDeadShadow, now)) {
				return true;
			}
		}
		return false;
	}
	
	private boolean hasPreviousDeletePendingOperationInGracePeriod(ProvisioningContext ctx, PrismObject<ShadowType> shadow, XMLGregorianCalendar now) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		List<PendingOperationType> pendingOperations = shadow.asObjectable().getPendingOperation();
		if (pendingOperations == null || pendingOperations.isEmpty()) {
			return false;
		}
		Duration gracePeriod = ProvisioningUtil.getGracePeriod(ctx);
		for (PendingOperationType pendingOperation : pendingOperations) {
			ObjectDeltaType delta = pendingOperation.getDelta();
			if (delta == null) {
				continue;
			}
			ChangeTypeType changeType = delta.getChangeType();
			if (!ChangeTypeType.DELETE.equals(changeType)) {
				continue;
			}
			if (ProvisioningUtil.isOverGrace(now, gracePeriod, pendingOperation)) {
				continue;
			}
			return true;
		}
		return false;
	}

	private String handleAddError(ProvisioningContext ctx,
			PrismObject<ShadowType> shadowToAdd,
			ResourceType resource,
			ProvisioningOperationOptions options,
			String proposedShadowOid,
			Exception ex,
			Task task,
			OperationResult parentResult) 
					throws SchemaException, GenericFrameworkException, CommunicationException, ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
		if (proposedShadowOid != null) {
			// TODO: maybe integrate with consistency mechanism?
			shadowManager.handleProposedShadowError(ctx, shadowToAdd, proposedShadowOid, ex, task, parentResult);
		}
		
		PrismObject<ShadowType> addedShadow = handleError(ctx, ex, shadowToAdd, FailedOperation.ADD, null,
				isDoDiscovery(resource, options), isCompensate(options), parentResult);
		return addedShadow.getOid();
	}
	
	/**
	 * Used to execute delayed operations.
	 * Mostly copy&paste from addShadow(). But as consistency (handleError()) branch expects to return immediately
	 * I could not find a more elegant way to structure this without complicating the code too much.
	 */
	private ProvisioningOperationState<AsynchronousOperationReturnValue<PrismObject<ShadowType>>> executeDelayedResourceAdd(ProvisioningContext ctx, PrismObject<ShadowType> shadowToAdd, OperationProvisioningScriptsType scripts, ProvisioningOperationOptions options, boolean compensate, Task task, OperationResult parentResult) throws SchemaException, GenericFrameworkException, CommunicationException, ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
		ProvisioningOperationState<AsynchronousOperationReturnValue<PrismObject<ShadowType>>> opState = new ProvisioningOperationState<>();
		opState.setExistingShadowOid(shadowToAdd.getOid());
		try {
			
			// RESOURCE OPERATION: add
			AsynchronousOperationReturnValue<PrismObject<ShadowType>> asyncReturnValue = resouceObjectConverter.addResourceObject(ctx, shadowToAdd, scripts, false, parentResult);
			opState.processAsyncResult(asyncReturnValue);
			LOGGER.debug("ADD {}: resource operation executed, operation state: {}", shadowToAdd, opState.shortDumpLazily());
			return opState;
			
		} catch (ObjectAlreadyExistsException e) {
			
			// This exception may still be OK in some cases. Such as:
			// We are trying to add a shadow to a semi-manual connector.
			// But the resource object was recently deleted. The object is
			// still in the backing store (CSV file) because of a grace
			// period. Obviously, attempt to add such object would fail.
			// So, we need to handle this case specially. (MID-4414)
			
			OperationResult failedOperationResult = parentResult.getLastSubresult();
			
			if (hasDeadShadowWithDeleteOperation(ctx, shadowToAdd, parentResult)) {
				
				if (failedOperationResult.isError()) {
					failedOperationResult.setStatus(OperationResultStatus.HANDLED_ERROR);
				}
				
				// Try again, this time without explicit uniqueness check
				try {
					
					AsynchronousOperationReturnValue<PrismObject<ShadowType>> asyncReturnValue = resouceObjectConverter.addResourceObject(ctx, shadowToAdd, scripts, true, parentResult);
					opState.processAsyncResult(asyncReturnValue);
					LOGGER.debug("ADD {}: resource operation re-executed, operation state: {}", shadowToAdd, opState.shortDumpLazily());
					return opState;
					
				} catch (Exception innerException) {
					return handleDelayedResourceAddError(ctx, shadowToAdd, options, compensate, opState, innerException, parentResult.getLastSubresult(), task, parentResult);
				}
				
			} else {
				return handleDelayedResourceAddError(ctx, shadowToAdd, options, compensate, opState, e, failedOperationResult, task, parentResult);
			}
	
		} catch (Exception e) {
			return handleDelayedResourceAddError(ctx, shadowToAdd, options, compensate, opState, e, parentResult.getLastSubresult(), task, parentResult);
		}
		
	}
	
	private ProvisioningOperationState<AsynchronousOperationReturnValue<PrismObject<ShadowType>>> handleDelayedResourceAddError(
			ProvisioningContext ctx, 
			PrismObject<ShadowType> shadowToAdd, 
			ProvisioningOperationOptions options, 
			boolean compensate, 
			ProvisioningOperationState<AsynchronousOperationReturnValue<PrismObject<ShadowType>>> opState,
			Exception ex,
			OperationResult originalOperationResult,
			Task task, 
			OperationResult parentResult) 
					throws SchemaException, GenericFrameworkException, CommunicationException, ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
		
		shadowManager.handleProposedShadowError(ctx, shadowToAdd, shadowToAdd.getOid(), ex, task, parentResult);
		PrismObject<ShadowType> addedShadow = handleError(ctx, ex, shadowToAdd, FailedOperation.ADD, null,
				isDoDiscovery(ctx.getResource(), options), compensate, parentResult);
		originalOperationResult.muteError();
		AsynchronousOperationReturnValue<PrismObject<ShadowType>> asyncReturnValue = AsynchronousOperationReturnValue.wrap(addedShadow, originalOperationResult);
		opState.setAsyncResult(asyncReturnValue);
		opState.setExecutionStatus(PendingOperationExecutionStatusType.COMPLETED);
		return opState;
		
	}
	
	private void notifyAfterAdd(
			ProvisioningContext ctx,
			PrismObject<ShadowType> addedShadow,
			ProvisioningOperationState<AsynchronousOperationReturnValue<PrismObject<ShadowType>>> opState,
			Task task,
			OperationResult parentResult)
					throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		ObjectDelta<ShadowType> delta = ObjectDelta.createAddDelta(addedShadow);
		ResourceOperationDescription operationDescription = createSuccessOperationDescription(ctx, addedShadow,
				delta, parentResult);
		
		if (opState.isExecuting()) {
			operationListener.notifyInProgress(operationDescription, task, parentResult);
		} else if (opState.isCompleted()) {
			operationListener.notifySuccess(operationDescription, task, parentResult);
		}
	}

	private void preAddChecks(ProvisioningContext ctx, PrismObject<ShadowType> shadow, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException, ObjectAlreadyExistsException, SecurityViolationException {
		checkConstraints(ctx, shadow, task, result);
		validateSchema(ctx, shadow, task, result);
	}
		
	private void checkConstraints(ProvisioningContext ctx, PrismObject<ShadowType> shadow, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException, ObjectAlreadyExistsException, SecurityViolationException {
		ShadowCheckType shadowConstraintsCheck = ResourceTypeUtil.getShadowConstraintsCheck(ctx.getResource());
		if (shadowConstraintsCheck == ShadowCheckType.NONE) {
			return;
		}
		
		ConstraintsChecker checker = new ConstraintsChecker();
		checker.setRepositoryService(repositoryService);
		checker.setShadowCache(this);
		checker.setPrismContext(prismContext);
		checker.setProvisioningContext(ctx);
		checker.setShadowObject(shadow);
		checker.setShadowOid(shadow.getOid());
		checker.setConstraintViolationConfirmer(conflictingShadowCandidate -> !Boolean.TRUE.equals(conflictingShadowCandidate.asObjectable().isDead()) );
		checker.setUseCache(false);
		
		ConstraintsCheckingResult retval = checker.check(task, result);
		
		LOGGER.trace("Checked {} constraints, result={}", shadow, retval.isSatisfiesConstraints());
		if (!retval.isSatisfiesConstraints()) {
			throw new ObjectAlreadyExistsException("Conflicting shadow already exists on "+ctx.getResource());
		}
	}
	
	private void validateSchema(ProvisioningContext ctx, PrismObject<ShadowType> shadow, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		if (ResourceTypeUtil.isValidateSchema(ctx.getResource())) {
			ShadowUtil.validateAttributeSchema(shadow, ctx.getObjectClassDefinition());
		}
	}
	
	private boolean shouldExecuteResourceOperationDirectly(ProvisioningContext ctx) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		ResourceConsistencyType consistency = ctx.getResource().getConsistency();
		if (consistency == null) {
			return true;
		}
		Duration operationGroupingInterval = consistency.getOperationGroupingInterval();
		if (operationGroupingInterval == null) {
			return true;
		}
		return false;
	}

	private ResourceOperationDescription createSuccessOperationDescription(ProvisioningContext ctx,
			PrismObject<ShadowType> shadowType, ObjectDelta delta, OperationResult parentResult)
					throws ObjectNotFoundException, SchemaException, CommunicationException,
					ConfigurationException, ExpressionEvaluationException {
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

	public abstract void afterModifyOnResource(
			ProvisioningContext ctx,
			PrismObject<ShadowType> shadow,
			Collection<? extends ItemDelta> modifications,
			ProvisioningOperationState<AsynchronousOperationReturnValue<Collection<PropertyDelta<PrismPropertyValue>>>> opState,
			XMLGregorianCalendar now,
			OperationResult parentResult)
					throws SchemaException, ObjectNotFoundException, ConfigurationException,
					CommunicationException, ExpressionEvaluationException, EncryptionException;

	public abstract Collection<? extends ItemDelta> beforeModifyOnResource(PrismObject<ShadowType> shadow,
			ProvisioningOperationOptions options, Collection<? extends ItemDelta> modifications)
					throws SchemaException;

	public String modifyShadow(PrismObject<ShadowType> repoShadow,
			Collection<? extends ItemDelta> modifications, OperationProvisioningScriptsType scripts,
			ProvisioningOperationOptions options, Task task, OperationResult parentResult)
					throws CommunicationException, GenericFrameworkException, ObjectNotFoundException,
					SchemaException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException, EncryptionException {

		Validate.notNull(repoShadow, "Object to modify must not be null.");
		Validate.notNull(modifications, "Object modification must not be null.");

		InternalMonitor.recordCount(InternalCounters.SHADOW_CHANGE_OPERATION_COUNT);

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
		
		PendingOperationType duplicateOperation = shadowManager.checkAndRecordPendingModifyOperationBeforeExecution(ctx, repoShadow, modifications, task, parentResult);
		if (duplicateOperation != null) {
			parentResult.recordInProgress();
			return repoShadow.getOid();
		}

		ctx.assertDefinition();
		RefinedObjectClassDefinition rOCDef = ctx.getObjectClassDefinition();

		shadowCaretaker.applyAttributesDefinition(ctx, repoShadow);

		accessChecker.checkModify(ctx.getResource(), repoShadow, modifications,
				ctx.getObjectClassDefinition(), parentResult);
		
		XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();

		modifications = beforeModifyOnResource(repoShadow, options, modifications);
		
		preprocessEntitlements(ctx, modifications, "delta for shadow " + repoShadow.getOid(), parentResult);
		
		ProvisioningOperationState<AsynchronousOperationReturnValue<Collection<PropertyDelta<PrismPropertyValue>>>> opState = new ProvisioningOperationState<>();
		opState.setExistingShadowOid(repoShadow.getOid());
		
		if (shadowManager.isRepositoryOnlyModification(modifications)) {
			opState.setExecutionStatus(PendingOperationExecutionStatusType.COMPLETED);
			LOGGER.debug("MODIFY {}: repository-only modification", repoShadow);
			
		} else {
			if (shouldExecuteResourceOperationDirectly(ctx)) {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("MODIFY {}: resource modification, execution starting\n{}", repoShadow, DebugUtil.debugDump(modifications));
				}

				try {
				
					AsynchronousOperationReturnValue<Collection<PropertyDelta<PrismPropertyValue>>> asyncReturnValue =
							resouceObjectConverter.modifyResourceObject(ctx, repoShadow, scripts, modifications, now, parentResult);
					opState.processAsyncResult(asyncReturnValue);
					
					Collection<PropertyDelta<PrismPropertyValue>> sideEffectChanges = asyncReturnValue.getReturnValue();
					if (sideEffectChanges != null) {
						ItemDelta.addAll(modifications, sideEffectChanges);
					}
					
				} catch (Exception ex) {
					LOGGER.debug("Provisioning exception: {}:{}, attempting to handle it",
							ex.getClass(), ex.getMessage(), ex);
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
				
				LOGGER.debug("MODIFY {}: resource operation executed, operation state: {}", repoShadow, opState.shortDumpLazily());
				
			} else {
				opState.setExecutionStatus(PendingOperationExecutionStatusType.EXECUTION_PENDING);
				// Create dummy subresult with IN_PROGRESS state. 
				// This will force the entire result (parent) to be IN_PROGRESS rather than SUCCESS.
				OperationResult delayedSubresult = parentResult.createSubresult(OP_DELAYED_OPERATION);
				delayedSubresult.setStatus(OperationResultStatus.IN_PROGRESS);
				LOGGER.debug("MODIFY {}: Resource operation NOT executed, execution pending", repoShadow);
			}
		}

		afterModifyOnResource(ctx, repoShadow, modifications, opState, now, parentResult);

		notifyAfterModify(ctx, repoShadow, modifications, opState, task, parentResult);
		
		if (opState.isCompleted()) {
			parentResult.computeStatus();
		} else {
			parentResult.recordInProgress();
		}
		parentResult.setAsynchronousOperationReference(opState.getAsynchronousOperationReference());
		
		return repoShadow.getOid();
	}
	
	private void notifyAfterModify(
			ProvisioningContext ctx, 
			PrismObject<ShadowType> repoShadow,
			Collection<? extends ItemDelta> modifications, 
			ProvisioningOperationState<AsynchronousOperationReturnValue<Collection<PropertyDelta<PrismPropertyValue>>>> opState,
			Task task,
			OperationResult parentResult) 
					throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		
		ObjectDelta<ShadowType> delta = ObjectDelta.createModifyDelta(repoShadow.getOid(), modifications,
				repoShadow.getCompileTimeClass(), prismContext);
		ResourceOperationDescription operationDescription = createSuccessOperationDescription(ctx, repoShadow,
				delta, parentResult);
		
		if (opState.isExecuting()) {
			operationListener.notifyInProgress(operationDescription, task, parentResult);
		} else {
			operationListener.notifySuccess(operationDescription, task, parentResult);
		}
	}
	
	/**
	 * Used to execute delayed operations.
	 * Mostly copy&paste from modifyShadow(). But as consistency (handleError()) branch expects to return immediately
	 * I could not find a more elegant way to structure this without complicating the code too much.
	 */
	private ProvisioningOperationState<AsynchronousOperationReturnValue<Collection<PropertyDelta<PrismPropertyValue>>>> executeResourceModify(
			ProvisioningContext ctx,
			PrismObject<ShadowType> repoShadow,
			Collection<? extends ItemDelta> modifications,
			OperationProvisioningScriptsType scripts,
			ProvisioningOperationOptions options,
			XMLGregorianCalendar now,
			Task task,
			OperationResult parentResult)
			throws SchemaException, GenericFrameworkException, CommunicationException, ObjectNotFoundException,
			ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
		ProvisioningOperationState<AsynchronousOperationReturnValue<Collection<PropertyDelta<PrismPropertyValue>>>> opState = new ProvisioningOperationState<>();
		opState.setExistingShadowOid(repoShadow.getOid());
		
		try {
			
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Applying change: {}", DebugUtil.debugDump(modifications));
			}

			AsynchronousOperationReturnValue<Collection<PropertyDelta<PrismPropertyValue>>> asyncReturnValue =
					resouceObjectConverter.modifyResourceObject(ctx, repoShadow, scripts, modifications, now, parentResult);
			opState.processAsyncResult(asyncReturnValue);
			
			Collection<PropertyDelta<PrismPropertyValue>> sideEffectChanges = asyncReturnValue.getReturnValue();
			if (sideEffectChanges != null) {
				ItemDelta.addAll(modifications, sideEffectChanges);
			}
			
		} catch (Exception ex) {
			LOGGER.debug("Provisioning exception: {}:{}, attempting to handle it",
					ex.getClass(), ex.getMessage(), ex);
			try {
				handleError(ctx, ex, repoShadow, FailedOperation.MODIFY, modifications,
						isDoDiscovery(ctx.getResource(), options), isCompensate(options), parentResult);
				parentResult.computeStatus();
				AsynchronousOperationReturnValue<Collection<PropertyDelta<PrismPropertyValue>>> asyncReturnValue = AsynchronousOperationReturnValue.wrap(null, parentResult.getLastSubresult());
				opState.setAsyncResult(asyncReturnValue);
				opState.setExecutionStatus(PendingOperationExecutionStatusType.COMPLETED);
				return opState;

			} catch (ObjectAlreadyExistsException e) {
				parentResult.recordFatalError(
						"While compensating communication problem for modify operation got: "
								+ ex.getMessage(),
						ex);
				throw new SystemException(e);
			}

		}
		
		return opState;
	}

	public void deleteShadow(PrismObject<ShadowType> shadow, ProvisioningOperationOptions options,
			OperationProvisioningScriptsType scripts, Task task, OperationResult parentResult)
					throws CommunicationException, GenericFrameworkException, ObjectNotFoundException,
					SchemaException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {

		Validate.notNull(shadow, "Object to delete must not be null.");
		Validate.notNull(parentResult, "Operation result must not be null.");

		InternalMonitor.recordCount(InternalCounters.SHADOW_CHANGE_OPERATION_COUNT);

		ProvisioningContext ctx = ctxFactory.create(shadow, task, parentResult);
		try {
			ctx.assertDefinition();
		} catch (ObjectNotFoundException ex) {
			// if the force option is set, delete shadow from the repo
			// although the resource does not exists..
			if (ProvisioningOperationOptions.isForce(options)) {
				parentResult.muteLastSubresultError();
				shadowManager.deleteShadow(ctx, shadow, parentResult);		
				parentResult.recordHandledError(
						"Resource defined in shadow does not exists. Shadow was deleted from the repository.");
				return;
			} else {
				throw ex;
			}
		}

		shadowCaretaker.applyAttributesDefinition(ctx, shadow);

		PendingOperationType duplicateOperation = shadowManager.checkAndRecordPendingDeleteOperationBeforeExecution(ctx, shadow, task, parentResult);
		if (duplicateOperation != null) {
			parentResult.recordInProgress();
			return;
		}
		
		LOGGER.trace("Deleting object {} from the resource {}.", shadow, ctx.getResource());

		ProvisioningOperationState<AsynchronousOperationResult> opState = new ProvisioningOperationState<>();
		opState.setExistingShadowOid(shadow.getOid());
		
		if (shouldExecuteResourceOperationDirectly(ctx)) {
			if (shadow.asObjectable().getFailedOperationType() == null
					|| (shadow.asObjectable().getFailedOperationType() != null
							&& FailedOperationTypeType.ADD != shadow.asObjectable().getFailedOperationType())) {
				
				LOGGER.trace("DELETE {}: resource deletion, execution starting", shadow);
				
				try {
					
					AsynchronousOperationResult asyncReturnValue = resouceObjectConverter.deleteResourceObject(ctx, shadow, scripts, parentResult);
					opState.processAsyncResult(asyncReturnValue);
					
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
				
				LOGGER.debug("DELETE {}: resource operation executed, operation state: {}", shadow, opState.shortDumpLazily());
				
			} else {
				LOGGER.trace("DELETE {}: resource deletion skipped (failed ADD operation)", shadow);
			}
			
		} else {
			opState.setExecutionStatus(PendingOperationExecutionStatusType.EXECUTION_PENDING);
			// Create dummy subresult with IN_PROGRESS state. 
			// This will force the entire result (parent) to be IN_PROGRESS rather than SUCCESS.
			OperationResult delayedSubresult = parentResult.createSubresult(OP_DELAYED_OPERATION);
			delayedSubresult.setStatus(OperationResultStatus.IN_PROGRESS);
			LOGGER.debug("DELETE {}: resource operation NOT executed, execution pending", shadow);
		}

		LOGGER.trace("Deting object with oid {} form repository.", shadow.getOid());
		XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
		
		try {
			shadowManager.deleteShadow(ctx, shadow, opState, now, parentResult);			
		} catch (ObjectNotFoundException ex) {
			parentResult.recordFatalError("Can't delete object " + shadow + ". Reason: " + ex.getMessage(),
					ex);
			throw new ObjectNotFoundException("An error occured while deleting resource object " + shadow
					+ "whith identifiers " + shadow + ": " + ex.getMessage(), ex);
		}
		
		notifyAfterDelete(ctx, shadow, opState, task, parentResult);
		
		LOGGER.trace("Object deleted from repository successfully.");
		parentResult.computeStatus();
		resourceManager.modifyResourceAvailabilityStatus(ctx.getResource().asPrismObject(),
				AvailabilityStatusType.UP, parentResult);
	}
	
	private ProvisioningOperationState<AsynchronousOperationResult> executeResourceDelete(
			ProvisioningContext ctx,
			PrismObject<ShadowType> shadow,
			OperationProvisioningScriptsType scripts,
			ProvisioningOperationOptions options,
			Task task,
			OperationResult parentResult) throws SchemaException, GenericFrameworkException, CommunicationException, ObjectNotFoundException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
		ProvisioningOperationState<AsynchronousOperationResult> opState = new ProvisioningOperationState<>();
		opState.setExistingShadowOid(shadow.getOid());
		
		if (shadow.asObjectable().getFailedOperationType() == null
				|| (shadow.asObjectable().getFailedOperationType() != null
						&& FailedOperationTypeType.ADD != shadow.asObjectable().getFailedOperationType())) {
			try {
				
				AsynchronousOperationResult asyncReturnValue = resouceObjectConverter.deleteResourceObject(ctx, shadow, scripts, parentResult);
				opState.processAsyncResult(asyncReturnValue);
				
			} catch (Exception ex) {
				try {
					handleError(ctx, ex, shadow, FailedOperation.DELETE, null,
							isDoDiscovery(ctx.getResource(), options), isCompensate(options), parentResult);
					AsynchronousOperationResult asyncReturnValue = AsynchronousOperationResult.wrap(parentResult.getLastSubresult());
					opState.setAsyncResult(asyncReturnValue);
					opState.setExecutionStatus(PendingOperationExecutionStatusType.COMPLETED);
					return opState;
					
				} catch (ObjectAlreadyExistsException e) {
					parentResult.recordFatalError(e);
					throw new SystemException(e.getMessage(), e);
				}
			}
		}
		
		return opState;
	}

	
	private void notifyAfterDelete(
			ProvisioningContext ctx,
			PrismObject<ShadowType> shadow,
			ProvisioningOperationState<AsynchronousOperationResult> opState,
			Task task,
			OperationResult parentResult)
					throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		ObjectDelta<ShadowType> delta = ObjectDelta.createDeleteDelta(shadow.getCompileTimeClass(),
				shadow.getOid(), prismContext);
		ResourceOperationDescription operationDescription = createSuccessOperationDescription(ctx, shadow,
				delta, parentResult);
		
		if (opState.isExecuting()) {
			operationListener.notifyInProgress(operationDescription, task, parentResult);
		} else {
			operationListener.notifySuccess(operationDescription, task, parentResult);
		}
	}
		

	public PrismObject<ShadowType> refreshShadow(PrismObject<ShadowType> repoShadow, Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException, EncryptionException {
		ShadowType shadowType = repoShadow.asObjectable();
		List<PendingOperationType> pendingOperations = shadowType.getPendingOperation();
		if (pendingOperations.isEmpty()) {
			LOGGER.trace("Skipping refresh of {} because there are no pending operations", repoShadow);
			return repoShadow;
		}
		
		LOGGER.trace("Refreshing {}", repoShadow);
		
		ProvisioningContext ctx = ctxFactory.create(repoShadow, task, parentResult);
		ctx.assertDefinition();
		
		Duration gracePeriod = ProvisioningUtil.getGracePeriod(ctx);
		
		List<ObjectDelta<ShadowType>> notificationDeltas = new ArrayList<>();
		List<PendingOperationType> sortedOperations = shadowCaretaker.sortPendingOperations(pendingOperations);
		
		boolean isDead = ShadowUtil.isDead(shadowType);
		ObjectDelta<ShadowType> shadowDelta = repoShadow.createModifyDelta();
		for (PendingOperationType pendingOperation: sortedOperations) {

			if (!needsRefresh(pendingOperation)) {
				continue;
			}
			
			ItemPath containerPath = pendingOperation.asPrismContainerValue().getPath();
			
			String asyncRef = pendingOperation.getAsynchronousOperationReference();
			if (asyncRef != null) {
				
				OperationResultStatus newStatus = resouceObjectConverter.refreshOperationStatus(ctx, repoShadow, asyncRef, parentResult);
						
				if (newStatus != null) {
					OperationResultStatusType newStatusType = newStatus.createStatusType();
					if (!newStatusType.equals(pendingOperation.getResultStatus())) {
						
						
						boolean operationCompleted = ProvisioningUtil.isCompleted(newStatusType) && pendingOperation.getCompletionTimestamp() == null;
										
						if (operationCompleted && gracePeriod == null) {
							LOGGER.trace("Deleting pending operation because it is completed (no grace): {}", pendingOperation);
							shadowDelta.addModificationDeleteContainer(new ItemPath(ShadowType.F_PENDING_OPERATION), pendingOperation.clone());
							continue;
							
						} else {
							PropertyDelta<OperationResultStatusType> resultStatusDelta = shadowDelta.createPropertyModification(containerPath.subPath(PendingOperationType.F_RESULT_STATUS));
							resultStatusDelta.setValuesToReplace(new PrismPropertyValue<>(newStatusType));
							shadowDelta.addModification(resultStatusDelta);
						}

						if (operationCompleted) {
							// Operation completed
							
							PropertyDelta<PendingOperationExecutionStatusType> executionStatusDelta = shadowDelta.createPropertyModification(containerPath.subPath(PendingOperationType.F_EXECUTION_STATUS));
							executionStatusDelta.setValuesToReplace(new PrismPropertyValue<>(PendingOperationExecutionStatusType.COMPLETED));
							shadowDelta.addModification(executionStatusDelta);
							
							PropertyDelta<XMLGregorianCalendar> completionTimestampDelta = shadowDelta.createPropertyModification(containerPath.subPath(PendingOperationType.F_COMPLETION_TIMESTAMP));
							completionTimestampDelta.setValuesToReplace(new PrismPropertyValue<>(clock.currentTimeXMLGregorianCalendar()));
							shadowDelta.addModification(completionTimestampDelta);
							
							ObjectDeltaType pendingDeltaType = pendingOperation.getDelta();
							ObjectDelta<ShadowType> pendingDelta = DeltaConvertor.createObjectDelta(pendingDeltaType, prismContext);
							
							if (pendingDelta.isAdd()) {
								// We do not need to care about attributes in add deltas here. The add operation is already applied to
								// attributes. We need this to "allocate" the identifiers, so iteration mechanism in the
								// model can find unique values while taking pending create operations into consideration.
								
								PropertyDelta<Boolean> existsDelta = shadowDelta.createPropertyModification(new ItemPath(ShadowType.F_EXISTS));
								existsDelta.setValuesToReplace(new PrismPropertyValue<>(true));
								shadowDelta.addModification(existsDelta);
							}
							
							if (pendingDelta.isModify()) {
								for (ItemDelta<?, ?> pendingModification: pendingDelta.getModifications()) {
									shadowDelta.addModification(pendingModification.clone());
								}
							}
							
							if (pendingDelta.isDelete()) {
								isDead = true;
								if (gracePeriod == null) {      // TODO gracePeriod is not null here ... review this please
									shadowDelta = repoShadow.createDeleteDelta();
									notificationDeltas.add(pendingDelta);
									break;
								} else {
									PropertyDelta<Boolean> deadDelta = shadowDelta.createPropertyModification(new ItemPath(ShadowType.F_DEAD));
									deadDelta.setValuesToReplace(new PrismPropertyValue<>(true));
									shadowDelta.addModification(deadDelta);
									
									PropertyDelta<Boolean> existsDelta = shadowDelta.createPropertyModification(new ItemPath(ShadowType.F_EXISTS));
									existsDelta.setValuesToReplace(new PrismPropertyValue<>(false));
									shadowDelta.addModification(existsDelta);
								}
							}
							
							notificationDeltas.add(pendingDelta);
						}
						
					}
				}
			}
		}
		
		if (shadowDelta.isDelete()) {
			LOGGER.trace("Deleting dead shadow because pending delete delta was completed (no grace period): {}", repoShadow);
			shadowManager.deleteShadow(ctx, repoShadow, parentResult);
			return null;
		}
		
		XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
		boolean atLeastOnePendingOperationRemains = expirePendingOperations(ctx, repoShadow, shadowDelta, now, parentResult);
		if (shadowType.getFailedOperationType() != null) {
			atLeastOnePendingOperationRemains = true;
		}
		
		if (isDead && !atLeastOnePendingOperationRemains) {
			LOGGER.trace("Deleting dead shadow because all pending operations expired: {}", repoShadow);
			shadowManager.deleteShadow(ctx, repoShadow, parentResult);
			return null;
		}
		
		if (!shadowDelta.isEmpty()) {
			shadowCaretaker.applyAttributesDefinition(ctx, shadowDelta);
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
	
	private boolean needsRefresh(PendingOperationType pendingOperation) {
		PendingOperationExecutionStatusType executionStatus = pendingOperation.getExecutionStatus();
		if (executionStatus == null) {
			return OperationResultStatusType.IN_PROGRESS.equals(pendingOperation.getResultStatus());
		} else {
			return PendingOperationExecutionStatusType.EXECUTING.equals(executionStatus);
		}
	}

	private boolean expirePendingOperations(ProvisioningContext ctx, PrismObject<ShadowType> repoShadow, ObjectDelta<ShadowType> shadowDelta, XMLGregorianCalendar now, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		ShadowType shadowType = repoShadow.asObjectable();
		
		Duration gracePeriod = ProvisioningUtil.getGracePeriod(ctx);
		boolean atLeastOneOperationRemains = false;
		for (PendingOperationType pendingOperation: shadowType.getPendingOperation()) {
			if (ProvisioningUtil.isOverGrace(now, gracePeriod, pendingOperation)) {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Deleting pending operation because it is completed '{}' (and over grace): {}", pendingOperation.getResultStatus().value(), pendingOperation);
				}
				shadowDelta.addModificationDeleteContainer(new ItemPath(ShadowType.F_PENDING_OPERATION), pendingOperation.clone());
			} else {
				atLeastOneOperationRemains = true;
			}
		}

		return atLeastOneOperationRemains;
	}
	
	public void applyDefinition(ObjectDelta<ShadowType> delta, ShadowType repoShadow,
			OperationResult parentResult) throws SchemaException, ObjectNotFoundException,
					CommunicationException, ConfigurationException, ExpressionEvaluationException {
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
					if (repoShadow == null) {
						throw new IllegalArgumentException("No OID in object delta " + delta
								+ " and no externally-supplied shadow is present as well.");
					}
					shadow = repoShadow.asPrismObject();
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
		shadowCaretaker.applyAttributesDefinition(ctx, delta);
	}

	public void applyDefinition(PrismObject<ShadowType> shadow, OperationResult parentResult)
			throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		ProvisioningContext ctx = ctxFactory.create(shadow, null, parentResult);
		ctx.assertDefinition();
		shadowCaretaker.applyAttributesDefinition(ctx, shadow);
	}

	public void setProtectedShadow(PrismObject<ShadowType> shadow, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		ProvisioningContext ctx = ctxFactory.create(shadow, null, parentResult);
		ctx.assertDefinition();
		ProvisioningUtil.setProtectedFlag(ctx, shadow, matchingRuleRegistry);
	}

	public void applyDefinition(final ObjectQuery query, OperationResult result)
			throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		ResourceShadowDiscriminator coordinates = ObjectQueryUtil.getCoordinates(query.getFilter());
		ProvisioningContext ctx = ctxFactory.create(coordinates, null, result);
		ctx.assertDefinition();
		applyDefinition(ctx, query);
	}

	private void applyDefinition(final ProvisioningContext ctx, final ObjectQuery query)
			throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
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
			throw (SchemaException) te.getCause();
		}
	}

	protected ResourceType getResource(ResourceShadowDiscriminator coords, Task task, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		String resourceOid = coords.getResourceOid();
		if (resourceOid == null) {
			throw new IllegalArgumentException("No resource OID in " + coords);
		}
		return resourceManager.getResource(resourceOid, null, task, parentResult).asObjectable();
	}

	@SuppressWarnings("rawtypes")
	protected PrismObject<ShadowType> handleError(ProvisioningContext ctx, Exception ex,
			PrismObject<ShadowType> shadow, FailedOperation op, Collection<? extends ItemDelta> modifications,
			boolean doDiscovery, boolean compensate, OperationResult parentResult) throws SchemaException,
					GenericFrameworkException, CommunicationException, ObjectNotFoundException,
					ObjectAlreadyExistsException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {

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

		LOGGER.debug("Handling provisioning exception {}: {}", ex.getClass(), ex.getMessage());
		LOGGER.trace("Handling provisioning exception {}: {}\ndoDiscovery={}, compensate={}",
				ex.getClass(), ex.getMessage(), doDiscovery, compensate, ex);

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
	
	public SearchResultList<PrismObject<ShadowType>> searchObjects(ObjectQuery query,
			Collection<SelectorOptions<GetOperationOptions>> options,
			final boolean readFromRepository, Task task, final OperationResult parentResult)
					throws SchemaException, ObjectNotFoundException, CommunicationException,
					ConfigurationException, SecurityViolationException, ExpressionEvaluationException {

		SearchResultList<PrismObject<ShadowType>> list = new SearchResultList<>();
		SearchResultMetadata metadata = searchObjectsIterative(query, options, (shadow,result) -> list.add(shadow), readFromRepository, task, parentResult);
		list.setMetadata(metadata);
		return list;
		
	}

	public SearchResultMetadata searchObjectsIterative(ObjectQuery query,
			Collection<SelectorOptions<GetOperationOptions>> options, final ResultHandler<ShadowType> handler,
			final boolean readFromRepository, Task task, final OperationResult parentResult)
					throws SchemaException, ObjectNotFoundException, CommunicationException,
					ConfigurationException, SecurityViolationException, ExpressionEvaluationException {

		ResourceShadowDiscriminator coordinates = ObjectQueryUtil.getCoordinates(query != null ? query.getFilter() : null);
		final ProvisioningContext ctx = ctxFactory.create(coordinates, task, parentResult);
		ctx.setGetOperationOptions(options);
		ctx.assertDefinition();

		return searchObjectsIterative(ctx, query, options, handler, readFromRepository, parentResult);
	}

	public SearchResultMetadata searchObjectsIterative(final ProvisioningContext ctx, ObjectQuery query,
			Collection<SelectorOptions<GetOperationOptions>> options, final ResultHandler<ShadowType> handler,
			final boolean readFromRepository, final OperationResult parentResult)
					throws SchemaException, ObjectNotFoundException, CommunicationException,
					ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
		applyDefinition(ctx, query);

		GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);
		if (ProvisioningUtil.shouldDoRepoSearch(rootOptions)) {
			return searchObjectsIterativeRepository(ctx, query, options, handler, parentResult);
		}

		// We need to record the fetch down here. Now it is certain that we are
		// going to fetch from resource
		// (we do not have raw/noFetch option)
		InternalMonitor.recordCount(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

		ObjectQuery attributeQuery = createAttributeQuery(query);

		ResultHandler<ShadowType> resultHandler = (PrismObject<ShadowType> resourceShadow, OperationResult objResult) -> {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Found resource object\n{}", resourceShadow.debugDump(1));
				}
			PrismObject<ShadowType> resultShadow;
				try {
					// The shadow does not have any kind or intent at this
					// point.
					// But at least locate the definition using object classes.
					ProvisioningContext estimatedShadowCtx = shadowCaretaker.reapplyDefinitions(ctx, resourceShadow);
					// Try to find shadow that corresponds to the resource
					// object.
					if (readFromRepository) {
						PrismObject<ShadowType> repoShadow = lookupOrCreateShadowInRepository(
								estimatedShadowCtx, resourceShadow, true, parentResult);

						// This determines the definitions exactly. How the repo
						// shadow should have proper kind/intent
						ProvisioningContext shadowCtx = shadowCaretaker.applyAttributesDefinition(ctx, repoShadow);

						repoShadow = shadowManager.updateShadow(shadowCtx, resourceShadow, repoShadow,
								parentResult);
						
						resultShadow = completeShadow(shadowCtx, resourceShadow, repoShadow, objResult);
						
					} else {
						resultShadow = resourceShadow;
					}

					validateShadow(resultShadow, readFromRepository);
					
				} catch (SchemaException e) {
					objResult.recordFatalError("Schema error: " + e.getMessage(), e);
					LOGGER.error("Schema error: {}", e.getMessage(), e);
					return false;
				} catch (ConfigurationException e) {
					objResult.recordFatalError("Configuration error: " + e.getMessage(), e);
					LOGGER.error("Configuration error: {}", e.getMessage(), e);
					return false;
				} catch (ObjectNotFoundException | ObjectAlreadyExistsException | CommunicationException
						| SecurityViolationException | GenericConnectorException | ExpressionEvaluationException | EncryptionException e) {
					objResult.recordFatalError(e.getMessage(), e);
					LOGGER.error("{}", e.getMessage(), e);
					return false;
				}

				boolean doContinue;
				try {
					
					doContinue =  handler.handle(resultShadow, objResult);
					
					objResult.computeStatus();
					objResult.recordSuccessIfUnknown();

                    if (!objResult.isSuccess() && !objResult.isHandledError()) {
                        Collection<? extends ItemDelta> shadowModificationType = PropertyDelta
                                .createModificationReplacePropertyCollection(ShadowType.F_RESULT,
                                        getResourceObjectShadowDefinition(), objResult.createOperationResultType());
                        try {
							ConstraintsChecker.onShadowModifyOperation(shadowModificationType);
							repositoryService.modifyObject(ShadowType.class, resultShadow.getOid(),
                                    shadowModificationType, objResult);
                        } catch (ObjectNotFoundException ex) {
                        	objResult.recordFatalError("Saving of result to " + resultShadow
                                    + " shadow failed: Not found: " + ex.getMessage(), ex);
                        } catch (ObjectAlreadyExistsException ex) {
                        	objResult.recordFatalError("Saving of result to " + resultShadow
                                    + " shadow failed: Already exists: " + ex.getMessage(), ex);
                        } catch (SchemaException ex) {
                        	objResult.recordFatalError("Saving of result to " + resultShadow
                                    + " shadow failed: Schema error: " + ex.getMessage(), ex);
                        } catch (RuntimeException e) {
                        	objResult.recordFatalError("Saving of result to " + resultShadow
                                    + " shadow failed: " + e.getMessage(), e);
                        	throw e;
                        }
                    }
                } catch (RuntimeException | Error e) {
                	objResult.recordFatalError(e);
                	throw e;
                } finally {
                	objResult.computeStatus();
                	objResult.recordSuccessIfUnknown();
                    // FIXME: hack. Hardcoded ugly summarization of successes. something like
                    // AbstractSummarizingResultHandler [lazyman]
                    if (objResult.isSuccess()) {
                    	objResult.getSubresults().clear();
                    }
                    parentResult.summarize();
                }

				return doContinue;
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
			} else {
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
			if (f instanceof PropertyValueFilter) { // TODO
				ItemPath parentPath = ((PropertyValueFilter) f).getParentPath();
				if (parentPath.isEmpty()) {
					QName elementName = ((PropertyValueFilter) f).getElementName();
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
				} else {
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
			final ResultHandler<ShadowType> shadowHandler, OperationResult parentResult)
					throws SchemaException, ConfigurationException, ObjectNotFoundException,
					CommunicationException, ExpressionEvaluationException {

		ResultHandler<ShadowType> repoHandler = (PrismObject<ShadowType> shadow, OperationResult objResult) -> {
				try {
					shadowCaretaker.applyAttributesDefinition(ctx, shadow);
					// fixing MID-1640; hoping that the protected object filter uses only identifiers
					// (that are stored in repo)
					ProvisioningUtil.setProtectedFlag(ctx, shadow, matchingRuleRegistry);
					
					validateShadow(shadow, true);
					
					if (GetOperationOptions.isMaxStaleness(SelectorOptions.findRootOptions(options))) {
						CachingMetadataType cachingMetadata = shadow.asObjectable().getCachingMetadata();
						if (cachingMetadata == null) {
							objResult.recordFatalError("Requested cached data but no cached data are available in the shadow");
						}
					}
					
					boolean cont = shadowHandler.handle(shadow, objResult);
					
					objResult.computeStatus();
					objResult.recordSuccessIfUnknown();
					if (!objResult.isSuccess()) {
						OperationResultType resultType = objResult.createOperationResultType();
						shadow.asObjectable().setFetchResult(resultType);
					}
					
					return cont;
				} catch (RuntimeException e) {
					objResult.recordFatalError(e);
					throw e;
				} catch (SchemaException | ConfigurationException | ObjectNotFoundException
						| CommunicationException | ExpressionEvaluationException e) {
					objResult.recordFatalError(e);
					shadow.asObjectable().setFetchResult(objResult.createOperationResultType());
					throw new SystemException(e);
				}
			};

		return shadowManager.searchObjectsIterativeRepository(ctx, query, options, repoHandler, parentResult);
	}
	
	private void validateShadow(PrismObject<ShadowType> shadow, boolean requireOid) {
		if (requireOid) {
			Validate.notNull(shadow.getOid(), "null shadow OID");
		}
		if (InternalsConfig.encryptionChecks) {
			CryptoUtil.checkEncrypted(shadow);
		}
	}
	
	private PrismObject<ShadowType> lookupOrCreateShadowInRepository(ProvisioningContext ctx,
			PrismObject<ShadowType> resourceShadow, boolean unknownIntent, OperationResult parentResult)
					throws SchemaException, ConfigurationException, ObjectNotFoundException,
					CommunicationException, SecurityViolationException, GenericConnectorException, ExpressionEvaluationException, EncryptionException {
		PrismObject<ShadowType> repoShadow = shadowManager.lookupLiveShadowInRepository(ctx, resourceShadow,
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
					CommunicationException, SecurityViolationException, GenericConnectorException, ExpressionEvaluationException, EncryptionException {

		PrismObject<ShadowType> repoShadow;
		PrismObject<ShadowType> conflictingShadow = shadowManager.lookupConflictingShadowBySecondaryIdentifiers(ctx,
				resourceShadow, parentResult);
		if (conflictingShadow != null) {
			shadowCaretaker.applyAttributesDefinition(ctx, conflictingShadow);
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
			SecurityViolationException, ExpressionEvaluationException {

		ResourceShadowDiscriminator coordinates = ObjectQueryUtil.getCoordinates(query.getFilter());
		final ProvisioningContext ctx = ctxFactory.create(coordinates, null, result);
		ctx.assertDefinition();
		applyDefinition(ctx, query);

		RefinedObjectClassDefinition objectClassDef = ctx.getObjectClassDefinition();
		ResourceType resourceType = ctx.getResource();
		CountObjectsCapabilityType countObjectsCapabilityType = objectClassDef
				.getEffectiveCapability(CountObjectsCapabilityType.class, resourceType);
		if (countObjectsCapabilityType == null) {
			// Unable to count. Return null which means "I do not know"
			result.recordNotApplicableIfUnknown();
			return null;
		} else {
			CountObjectsSimulateType simulate = countObjectsCapabilityType.getSimulate();
			if (simulate == null) {
				// We have native capability

				ConnectorInstance connector = ctx.getConnector(ReadCapabilityType.class, result);
				try {
					ObjectQuery attributeQuery = createAttributeQuery(query);
					int count;
					try {
						count = connector.count(objectClassDef.getObjectClassDefinition(), attributeQuery,
								objectClassDef.getPagedSearches(resourceType), ctx, result);
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

				if (!objectClassDef.isPagedSearchEnabled(resourceType)) {
					throw new ConfigurationException(
							"Configured count object capability to be simulated using a paged search but paged search capability is not present");
				}

				final Holder<Integer> countHolder = new Holder<>(0);

				final ResultHandler<ShadowType> handler = new ResultHandler<ShadowType>() {
					@Override
					public boolean handle(PrismObject<ShadowType> shadow, OperationResult objResult) {
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
				final Holder<Integer> countHolder = new Holder<>(0);

				final ResultHandler<ShadowType> handler = new ResultHandler<ShadowType>() {

					@Override
					public boolean handle(PrismObject<ShadowType> shadow, OperationResult objResult) {
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
					SecurityViolationException, ObjectAlreadyExistsException, ExpressionEvaluationException, EncryptionException {

		InternalMonitor.recordCount(InternalCounters.PROVISIONING_ALL_EXT_OPERATION_COUNT);

		final ProvisioningContext ctx = ctxFactory.create(shadowCoordinates, task, parentResult);

		List<Change> changes;
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
					task.incrementProgressAndStoreStatsIfNeeded();
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
					// get updated token from change, create property modification from new token and replace old token with the new one
					task.setExtensionProperty(change.getToken());
					processedChanges++;
					task.incrementProgressAndStoreStatsIfNeeded();
				}
			}

			// also if no changes was detected, update token
			if (changes.isEmpty() && lastToken != null) {
				LOGGER.trace("No changes to synchronize on {}", ctx.getResource());
				task.setExtensionProperty(lastToken);
			}
			task.savePendingModifications(parentResult);
			return processedChanges;

		} catch (SchemaException | CommunicationException | GenericFrameworkException | ConfigurationException | 
				ObjectNotFoundException | ObjectAlreadyExistsException | ExpressionEvaluationException | EncryptionException | RuntimeException | Error ex) {
			parentResult.recordFatalError(ex);
			throw ex;
		}
	}

	@SuppressWarnings("rawtypes")
	boolean processSynchronization(ProvisioningContext ctx, Change change, OperationResult parentResult)
			throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException,
			CommunicationException, ConfigurationException, ExpressionEvaluationException {

		OperationResult result = parentResult.createSubresult(OP_PROCESS_SYNCHRONIZATION);
		
		boolean successfull = false;
		try {
			ResourceObjectShadowChangeDescription shadowChangeDescription = createResourceShadowChangeDescription(
					change, ctx.getResource(), ctx.getChannel());
	
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("**PROVISIONING: Created resource object shadow change description {}",
						SchemaDebugUtil.prettyPrint(shadowChangeDescription));
			}
			OperationResult notifyChangeResult = new OperationResult(
					ShadowCache.class.getName() + "notifyChange");
			notifyChangeResult.addParam("resourceObjectShadowChangeDescription", shadowChangeDescription.toString());
	
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
			
			if (notifyChangeResult.isSuccess() || notifyChangeResult.isHandledError()) {
				deleteShadowFromRepoIfNeeded(change, result);
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
	
			if (result.isUnknown()) {
				result.computeStatus();
			}
			
		} catch (SchemaException | ObjectNotFoundException | ObjectAlreadyExistsException |
				CommunicationException | ConfigurationException | ExpressionEvaluationException | RuntimeException | Error e) {
			result.recordFatalError(e);
			throw e;
		}
			
		return successfull;
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

		Collection<ItemDelta> modifications = new ArrayList<>();
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

	private void deleteShadowFromRepoIfNeeded(Change change, OperationResult parentResult)
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
					GenericConnectorException, ObjectAlreadyExistsException, ExpressionEvaluationException, EncryptionException {

		if (oldShadow == null) {
			oldShadow = shadowManager.findOrAddShadowFromChange(ctx, change, parentResult);
		}

		if (oldShadow != null) {
			shadowCaretaker.applyAttributesDefinition(ctx, oldShadow);

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
					SchemaException, ConfigurationException, ExpressionEvaluationException {
		Validate.notNull(parentResult, "Operation result must not be null.");

		InternalMonitor.recordCount(InternalCounters.PROVISIONING_ALL_EXT_OPERATION_COUNT);

		ProvisioningContext ctx = ctxFactory.create(shadowCoordinates, null, parentResult);

		LOGGER.trace("Getting last token");
		PrismProperty<?> lastToken = null;
		try {
			lastToken = resouceObjectConverter.fetchCurrentToken(ctx, parentResult);
		} catch (ObjectNotFoundException | CommunicationException | SchemaException | ConfigurationException | ExpressionEvaluationException e) {
			parentResult.recordFatalError(e.getMessage(), e);
			throw e;
		}

		LOGGER.trace("Got last token: {}", SchemaDebugUtil.prettyPrint(lastToken));
		parentResult.recordSuccess();
		return lastToken;
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
					CommunicationException, SecurityViolationException, GenericConnectorException, ExpressionEvaluationException, EncryptionException {

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
		
		// exists, dead
		resultShadowType.setExists(resourceShadowType.isExists());

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
							.findAssociationDefinition(associationName);
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
					ShadowKindType entitlementKind = rEntitlementAssociation.getKind();
					if (entitlementKind == null) {
						entitlementKind = ShadowKindType.ENTITLEMENT;
					}
					for (String entitlementIntent : rEntitlementAssociation.getIntents()) {
						ProvisioningContext ctxEntitlement = ctx.spawn(entitlementKind, entitlementIntent);

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
									entitlementRepoShadow = shadowManager.lookupLiveShadowInRepository(
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
										"The entitlement identified by {} referenced from {} violates the schema. Skipping. Original error: {}-{}",
										new Object[] { associationCVal, resourceShadow, e.getMessage(), e });
								continue;
							}
						} else {
							entitlementRepoShadow = lookupOrCreateShadowInRepository(ctxEntitlement,
									entitlementShadow, false, parentResult);
						}
						ObjectReferenceType shadowRefType = ObjectTypeUtil.createObjectRef(entitlementRepoShadow);
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
					ConfigurationException, CommunicationException, ExpressionEvaluationException {
		try {
			shadow.accept(
					(visitable) -> {
						try {
							preprocessEntitlement(ctx, (PrismContainerValue<ShadowAssociationType>) visitable,
									shadow.toString(), result);
						} catch (SchemaException | ObjectNotFoundException | ConfigurationException
								| CommunicationException | ExpressionEvaluationException e) {
							throw new TunnelException(e);
						}
					},
					new ItemPath(new NameItemPathSegment(ShadowType.F_ASSOCIATION),
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
			} else if (cause instanceof ExpressionEvaluationException) {
				throw (ExpressionEvaluationException) cause;
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
					CommunicationException, ExpressionEvaluationException {
		try {
			ItemDelta.accept(modifications, 
					(visitable) -> {
						try {
							preprocessEntitlement(ctx, (PrismContainerValue<ShadowAssociationType>) visitable, desc,
									result);
						} catch (SchemaException | ObjectNotFoundException | ConfigurationException
								| CommunicationException | ExpressionEvaluationException e) {
							throw new TunnelException(e);
						}						
					}, 
					new ItemPath(new NameItemPathSegment(ShadowType.F_ASSOCIATION), IdItemPathSegment.WILDCARD), false);
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
			} else if (cause instanceof ExpressionEvaluationException) {
				throw (ExpressionEvaluationException) cause;
			} else {
				throw new SystemException("Unexpected exception " + cause, cause);
			}
		}
	}

	private void preprocessEntitlement(ProvisioningContext ctx,
			PrismContainerValue<ShadowAssociationType> association, String desc, OperationResult result)
					throws SchemaException, ObjectNotFoundException, ConfigurationException,
					CommunicationException, ExpressionEvaluationException {
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
		shadowCaretaker.applyAttributesDefinition(ctx, repoShadow);
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
	
	public void propagateOperations(PrismObject<ResourceType> resource, PrismObject<ShadowType> shadow, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException, GenericFrameworkException, ObjectAlreadyExistsException, SecurityViolationException {
		ResourceConsistencyType resourceConsistencyType = resource.asObjectable().getConsistency();
		if (resourceConsistencyType == null) {
			LOGGER.warn("Skipping propagation of {} because no there is no consistency definition in resource", shadow);
			return;
		}
		Duration operationGroupingInterval = resourceConsistencyType.getOperationGroupingInterval();
		if (operationGroupingInterval == null) {
			LOGGER.warn("Skipping propagation of {} because no there is no operationGroupingInterval defined in resource", shadow);
			return;
		}
		XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
		
		List<PendingOperationType> pendingExecutionOperations = new ArrayList<>();
		boolean triggered = false;
		for (PendingOperationType pendingOperation: shadow.asObjectable().getPendingOperation()) {
			PendingOperationExecutionStatusType executionStatus = pendingOperation.getExecutionStatus();
			if (executionStatus == PendingOperationExecutionStatusType.EXECUTION_PENDING) {
				pendingExecutionOperations.add(pendingOperation);
				if (isPropagationTriggered(pendingOperation, operationGroupingInterval, now)) {
					triggered = true;
				}
			}
		}
		if (!triggered) {
			LOGGER.debug("Skipping propagation of {} because no pending operation triggered propagation", shadow);
			return;
		}
		if (pendingExecutionOperations.isEmpty()) {
			LOGGER.debug("Skipping propagation of {} because there are no pending executions", shadow);
			return;
		}
		LOGGER.debug("Propagating {} pending operations in {} ", pendingExecutionOperations.size(), shadow);

		ObjectDelta<ShadowType> operationDelta = null;
		List<PendingOperationType> sortedOperations = shadowCaretaker.sortPendingOperations(pendingExecutionOperations);
		for (PendingOperationType pendingOperation: sortedOperations) {
			ObjectDeltaType pendingDeltaType = pendingOperation.getDelta();
			ObjectDelta<ShadowType> pendingDelta = DeltaConvertor.createObjectDelta(pendingDeltaType, prismContext);
			applyDefinition(pendingDelta, shadow.asObjectable(), result);
			if (operationDelta == null) {
				operationDelta = pendingDelta;
			} else {
				operationDelta.merge(pendingDelta);
			}
		}
		
		ProvisioningContext ctx = ctxFactory.create(shadow, task, result);
		shadowCaretaker.applyAttributesDefinition(ctx, shadow);
		shadowCaretaker.applyAttributesDefinition(ctx, operationDelta);
		LOGGER.trace("Merged operation for {}:\n{} ", shadow, operationDelta.debugDumpLazily(1));
		
		if (operationDelta.isAdd()) {
			PrismObject<ShadowType> shadowToAdd = operationDelta.getObjectToAdd();
			// Do not "compensate" here. Compensate actually means that an exception will be re-throws from operation
			// TODO: This really needs consistency mechanism cleanup
			ProvisioningOperationState<AsynchronousOperationReturnValue<PrismObject<ShadowType>>> opState =
					executeDelayedResourceAdd(ctx, shadowToAdd, null, null, false, task, result);
			opState.determineExecutionStatusFromResult();
			
			shadowManager.updatePendingOperations(ctx, shadow, opState, pendingExecutionOperations, now, result);
			
			notifyAfterAdd(ctx, opState.getAsyncResult().getReturnValue(), opState, task, result);
			
		} else if (operationDelta.isModify()) {
			Collection<? extends ItemDelta<?,?>> modifications = operationDelta.getModifications();
			ProvisioningOperationState<AsynchronousOperationReturnValue<Collection<PropertyDelta<PrismPropertyValue>>>> opState =
					executeResourceModify(ctx, shadow, modifications, null, null, now, task, result);
			opState.determineExecutionStatusFromResult();
			
			shadowManager.updatePendingOperations(ctx, shadow, opState, pendingExecutionOperations, now, result);
			
			notifyAfterModify(ctx, shadow, modifications, opState, task, result);
			
		} else if (operationDelta.isDelete()) {
			ProvisioningOperationState<AsynchronousOperationResult> opState = executeResourceDelete(ctx, shadow, null, null, task, result);
			opState.determineExecutionStatusFromResult();
			
			shadowManager.updatePendingOperations(ctx, shadow, opState, pendingExecutionOperations, now, result);
			
			notifyAfterDelete(ctx, shadow, opState, task, result);
			
		} else {
			throw new IllegalStateException("Delta from outer space: "+operationDelta);
		}
		
		// do we need to modify exists/dead flags?

	}

	private boolean isPropagationTriggered(PendingOperationType pendingOperation, Duration operationGroupingInterval, XMLGregorianCalendar now) {
		XMLGregorianCalendar requestTimestamp = pendingOperation.getRequestTimestamp();
		if (requestTimestamp == null) {
			return false;
		}
		return XmlTypeConverter.isAfterInterval(requestTimestamp, operationGroupingInterval, now);
	}

	public <T> ItemComparisonResult compare(PrismObject<ShadowType> repositoryShadow, ItemPath path, T expectedValue, Task task, OperationResult parentResult) 
				throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException, EncryptionException {
		
		if (!path.equals(SchemaConstants.PATH_PASSWORD_VALUE)) {
			throw new UnsupportedOperationException("Only password comparison is supported");
		}
		
		ProvisioningContext ctx = ctxFactory.create(repositoryShadow, task, parentResult);
		try {
			ctx.assertDefinition();
			shadowCaretaker.applyAttributesDefinition(ctx, repositoryShadow);
		} catch (ObjectNotFoundException | SchemaException | CommunicationException
				| ConfigurationException | ExpressionEvaluationException e) {
			throw e;
		}
		
		ResourceType resource = ctx.getResource();

		PasswordCompareStrategyType passwordCompareStrategy = getPasswordCompareStrategy(ctx.getObjectClassDefinition());
		if (passwordCompareStrategy == PasswordCompareStrategyType.ERROR) {
			throw new UnsupportedOperationException("Password comparison is not supported on "+resource);
		}
		
		PrismProperty<T> repoProperty = repositoryShadow.findProperty(path);
		if (repoProperty == null) {
			if (passwordCompareStrategy == PasswordCompareStrategyType.CACHED) {
				if (expectedValue == null) {
					return ItemComparisonResult.MATCH;
				} else {
					return ItemComparisonResult.MISMATCH;
				}
			} else {
				// AUTO
				return ItemComparisonResult.NOT_APPLICABLE;
			}
		}
		
		ProtectedStringType repoProtectedString = (ProtectedStringType) repoProperty.getRealValue();
		ProtectedStringType expectedProtectedString;
		if (expectedValue instanceof ProtectedStringType) {
			expectedProtectedString = (ProtectedStringType) expectedValue;
		} else {
			expectedProtectedString = new ProtectedStringType();
			expectedProtectedString.setClearValue((String) expectedValue);
		}
		if (protector.compare(repoProtectedString, expectedProtectedString)) {
			return ItemComparisonResult.MATCH;
		} else {
			return ItemComparisonResult.MISMATCH;
		}
	}
	
	private PasswordCompareStrategyType getPasswordCompareStrategy(RefinedObjectClassDefinition objectClassDefinition) {
		ResourcePasswordDefinitionType passwordDefinition = objectClassDefinition.getPasswordDefinition();
		if (passwordDefinition == null) {
			return null;
		}
		return passwordDefinition.getCompareStrategy();
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected ObjectDelta mergeDeltas(PrismObject<ShadowType> shadow, Collection<? extends ItemDelta> modifications)
			throws SchemaException {
		ShadowType shadowType = shadow.asObjectable();
		if (shadowType.getObjectChange() != null) {

			ObjectDeltaType deltaType = shadowType.getObjectChange();
			Collection<? extends ItemDelta> pendingModifications = DeltaConvertor.toModifications(
					deltaType.getItemDelta(), shadow.getDefinition());

            // pendingModifications must come before modifications, otherwise REPLACE of value X (pending),
            // followed by ADD of value Y (current) would become "REPLACE X", which is obviously wrong.
            // See e.g. MID-1709.
			return ObjectDelta.summarize(
                    ObjectDelta.createModifyDelta(shadow.getOid(), pendingModifications,
                            ShadowType.class, getPrismContext()),
                    ObjectDelta.createModifyDelta(shadow.getOid(), modifications,
                            ShadowType.class, getPrismContext()));
		}
		return null;
	}
}
