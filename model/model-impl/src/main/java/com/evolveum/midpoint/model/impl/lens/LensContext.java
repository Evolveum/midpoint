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
package com.evolveum.midpoint.model.impl.lens;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ProgressInformation;
import com.evolveum.midpoint.model.api.ProgressListener;
import com.evolveum.midpoint.model.api.context.*;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.ConflictWatcher;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.PolicyRuleTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * @author semancik
 *
 */
public class LensContext<F extends ObjectType> implements ModelContext<F> {

	private static final long serialVersionUID = -778283437426659540L;
	private static final String DOT_CLASS = LensContext.class.getName() + ".";
	
	private static final Trace LOGGER = TraceManager.getTrace(LensContext.class);

	private ModelState state = ModelState.INITIAL;

	@NotNull private final transient List<ConflictWatcher> conflictWatchers = new ArrayList<>();

	private int conflictResolutionAttemptNumber;

	/**
	 * Channel that is the source of primary change (GUI, live sync, import,
	 * ...)
	 */
	private String channel;

	private LensFocusContext<F> focusContext;
	private Collection<LensProjectionContext> projectionContexts = new ArrayList<>();

	/**
	 * EXPERIMENTAL. A trace of resource objects that once existed but were
	 * unlinked or deleted, and the corresponding contexts were rotten and
	 * removed afterwards.
	 *
	 * Necessary to evaluate old state of hasLinkedAccount.
	 *
	 * TODO implement as non-transient. TODO consider storing whole projection
	 * contexts here.
	 */
	transient private Collection<ResourceShadowDiscriminator> historicResourceObjects;

	private Class<F> focusClass;

	private boolean lazyAuditRequest = false; // should be the request audited
												// just before the execution is
												// audited?
	private boolean requestAudited = false; // was the request audited?
	private boolean executionAudited = false; // was the execution audited?
	private LensContextStatsType stats = new LensContextStatsType();

	/**
	 * Metadata of the request. Metadata recorded when the operation has
	 * started. Currently only the requestTimestamp and requestorRef are
	 * meaningful. But later other metadata may be used.
	 */
	private MetadataType requestMetadata;

	/*
	 * Executed deltas from rotten contexts.
	 */
	private List<LensObjectDeltaOperation<?>> rottenExecutedDeltas = new ArrayList<>();

	transient private ObjectTemplateType focusTemplate;
	transient private ProjectionPolicyType accountSynchronizationSettings;

	transient private DeltaSetTriple<EvaluatedAssignmentImpl<?>> evaluatedAssignmentTriple;

	/**
	 * Just a cached copy. Keep it in context so we do not need to reload it all
	 * the time.
	 */
	transient private PrismObject<SystemConfigurationType> systemConfiguration;

	/**
	 * True if we want to reconcile all accounts in this context.
	 */
	private boolean doReconciliationForAllProjections = false;
	
	/**
	 * If set to true then all operations are considered to be
	 * in execution phase - for the purpose of authorizations and auditing.
	 * This is used in case that the whole operation (context) is a
	 * secondary change, e.g. in case that persona is provisioned. 
	 */
	private boolean executionPhaseOnly = false;

	/**
	 * Current wave of computation and execution.
	 */
	int projectionWave = 0;

	/**
	 * Current wave of execution.
	 */
	int executionWave = 0;

	private String triggeredResourceOid;

	/**
	 * At this level, isFresh == false means that deeper recomputation has to be
	 * carried out.
	 */
	transient private boolean isFresh = false;
	private boolean isRequestAuthorized = false;

	/**
	 * Cache of resource instances. It is used to reduce the number of read
	 * (getObject) calls for ResourceType objects.
	 */
	transient private Map<String, ResourceType> resourceCache;

	transient private PrismContext prismContext;

	transient private ProvisioningService provisioningService;

	private ModelExecuteOptions options;

	/**
	 * Used mostly in unit tests.
	 */
	transient private LensDebugListener debugListener;

	/**
	 * User feedback.
	 */
	transient private Collection<ProgressListener> progressListeners;

	private Map<String, Long> sequences = new HashMap<>();

	/**
	 * Moved from ProjectionValuesProcessor TODO consider if necessary to
	 * serialize to XML
	 */
	private List<LensProjectionContext> conflictingProjectionContexts = new ArrayList<>();

	transient private Map<String,Collection<Containerable>> hookPreviewResultsMap;

	public LensContext(Class<F> focusClass, PrismContext prismContext,
			ProvisioningService provisioningService) {
		Validate.notNull(prismContext, "No prismContext");

		this.prismContext = prismContext;
		this.provisioningService = provisioningService;
		this.focusClass = focusClass;
	}

	protected LensContext(PrismContext prismContext) {
		this.prismContext = prismContext;
	}

	public PrismContext getPrismContext() {
		return prismContext;
	}

	protected PrismContext getNotNullPrismContext() {
		if (prismContext == null) {
			throw new IllegalStateException(
					"Null prism context in " + this + "; the context was not adopted (most likely)");
		}
		return prismContext;
	}

	public ProvisioningService getProvisioningService() {
		return provisioningService;
	}

	public void setTriggeredResource(ResourceType triggeredResource) {
		if (triggeredResource != null) {
			this.triggeredResourceOid = triggeredResource.getOid();
		}
	}

	public String getTriggeredResourceOid() {
		return triggeredResourceOid;
	}

	@Override
	public ModelState getState() {
		return state;
	}

	public void setState(ModelState state) {
		this.state = state;
	}

	@Override
	public LensFocusContext<F> getFocusContext() {
		return focusContext;
	}

	public void setFocusContext(LensFocusContext<F> focusContext) {
		this.focusContext = focusContext;
	}

	public LensFocusContext<F> createFocusContext() {
		return createFocusContext(null);
	}

	public LensFocusContext<F> createFocusContext(Class<F> explicitFocusClass) {
		if (explicitFocusClass != null) {
			this.focusClass = explicitFocusClass;
		}
		focusContext = new LensFocusContext<>(focusClass, this);
		return focusContext;
	}

	public LensFocusContext<F> getOrCreateFocusContext() {
		return getOrCreateFocusContext(null);
	}

	public LensFocusContext<F> getOrCreateFocusContext(Class<F> explicitFocusClass) {
		if (focusContext == null) {
			createFocusContext(explicitFocusClass);
		}
		return focusContext;
	}

	@Override
	public Collection<LensProjectionContext> getProjectionContexts() {
		return projectionContexts;
	}

	public Iterator<LensProjectionContext> getProjectionContextsIterator() {
		return projectionContexts.iterator();
	}

	public void addProjectionContext(LensProjectionContext projectionContext) {
		projectionContexts.add(projectionContext);
	}

	public LensProjectionContext findProjectionContextByOid(String oid) {
		for (LensProjectionContext projCtx : getProjectionContexts()) {
			if (oid.equals(projCtx.getOid())) {
				return projCtx;
			}
		}
		return null;
	}

	public LensProjectionContext findProjectionContext(ResourceShadowDiscriminator rat) {
		Validate.notNull(rat);
		for (LensProjectionContext projCtx : getProjectionContexts()) {
			if (projCtx.compareResourceShadowDiscriminator(rat, true)) {
				return projCtx;
			}
		}
		return null;
	}

	public LensProjectionContext findOrCreateProjectionContext(ResourceShadowDiscriminator rat) {
		LensProjectionContext projectionContext = findProjectionContext(rat);
		if (projectionContext == null) {
			projectionContext = createProjectionContext(rat);
		}
		return projectionContext;
	}

	public ObjectTemplateType getFocusTemplate() {
		return focusTemplate;
	}

	public void setFocusTemplate(ObjectTemplateType focusTemplate) {
		this.focusTemplate = focusTemplate;
	}

	public LensProjectionContext findProjectionContext(ResourceShadowDiscriminator rat, String oid) {
		LensProjectionContext projectionContext = findProjectionContext(rat);

		if (projectionContext == null || projectionContext.getOid() == null
				|| !oid.equals(projectionContext.getOid())) {
			return null;
		}

		return projectionContext;
	}

	public PrismObject<SystemConfigurationType> getSystemConfiguration() {
		return systemConfiguration;
	}

	public void setSystemConfiguration(PrismObject<SystemConfigurationType> systemConfiguration) {
		this.systemConfiguration = systemConfiguration;
	}

	public ProjectionPolicyType getAccountSynchronizationSettings() {
		return accountSynchronizationSettings;
	}

	public void setAccountSynchronizationSettings(ProjectionPolicyType accountSynchronizationSettings) {
		this.accountSynchronizationSettings = accountSynchronizationSettings;
	}

	public int getProjectionWave() {
		return projectionWave;
	}

	public void setProjectionWave(int wave) {
		this.projectionWave = wave;
	}

	public void incrementProjectionWave() {
		projectionWave++;
	}

	public void resetProjectionWave() {
		projectionWave = executionWave;
	}

	public int getExecutionWave() {
		return executionWave;
	}

	public void setExecutionWave(int executionWave) {
		this.executionWave = executionWave;
	}

	public void incrementExecutionWave() {
		executionWave++;
	}

	public int getMaxWave() {
		int maxWave = 0;
		for (LensProjectionContext projContext : projectionContexts) {
			if (projContext.getWave() > maxWave) {
				maxWave = projContext.getWave();
			}
		}
		return maxWave;
	}

	public boolean isFresh() {
		return isFresh;
	}

	public void setFresh(boolean isFresh) {
		this.isFresh = isFresh;
	}

	public boolean isRequestAuthorized() {
		return isRequestAuthorized;
	}

	public void setRequestAuthorized(boolean isRequestAuthorized) {
		this.isRequestAuthorized = isRequestAuthorized;
	}

	/**
	 * Makes the context and all sub-context non-fresh.
	 */
	public void rot(String reason) {
		LOGGER.debug("Rotting context because of {}", reason);
		setFresh(false);
		if (focusContext != null) {
			focusContext.setFresh(false);
		}
		for (LensProjectionContext projectionContext : projectionContexts) {
			projectionContext.setFresh(false);
			projectionContext.setFullShadow(false);
		}
	}

	public String getChannel() {
		return channel;
	}

	public void setChannel(String channelUri) {
		this.channel = channelUri;
	}

	public void setChannel(QName channelQName) {
		this.channel = QNameUtil.qNameToUri(channelQName);
	}
	
	public boolean isDoReconciliationForAllProjections() {
		return doReconciliationForAllProjections;
	}

	public void setDoReconciliationForAllProjections(boolean doReconciliationForAllProjections) {
		this.doReconciliationForAllProjections = doReconciliationForAllProjections;
	}
	
	public boolean isReconcileFocus() {
		return doReconciliationForAllProjections ||  ModelExecuteOptions.isReconcileFocus(options);
	}

	public boolean isExecutionPhaseOnly() {
		return executionPhaseOnly;
	}

	public void setExecutionPhaseOnly(boolean executionPhaseOnly) {
		this.executionPhaseOnly = executionPhaseOnly;
	}

	public DeltaSetTriple<EvaluatedAssignmentImpl<?>> getEvaluatedAssignmentTriple() {
		return evaluatedAssignmentTriple;
	}

	public void setEvaluatedAssignmentTriple(
			DeltaSetTriple<EvaluatedAssignmentImpl<?>> evaluatedAssignmentTriple) {
		this.evaluatedAssignmentTriple = evaluatedAssignmentTriple;
	}

	@Override
	public ModelExecuteOptions getOptions() {
		return options;
	}

	public void setOptions(ModelExecuteOptions options) {
		this.options = options;
	}

	// be sure to use this method during clockwork processing, instead of directly accessing options.getPartialProcessing
	@Override
	@NotNull
	public PartialProcessingOptionsType getPartialProcessingOptions() {
		if (state == ModelState.INITIAL && options != null && options.getInitialPartialProcessing() != null) {
			return options.getInitialPartialProcessing();
		}
		if (options == null || options.getPartialProcessing() == null) {
			return new PartialProcessingOptionsType();
		} else {
			return options.getPartialProcessing();
		}
	}

	public MetadataType getRequestMetadata() {
		return requestMetadata;
	}

	public void setRequestMetadata(MetadataType requestMetadata) {
		this.requestMetadata = requestMetadata;
	}

	public LensDebugListener getDebugListener() {
		return debugListener;
	}

	public void setDebugListener(LensDebugListener debugListener) {
		this.debugListener = debugListener;
	}

	/**
	 * If set to true then the request will be audited right before execution.
	 * If no execution takes place then no request will be audited.
	 */
	public boolean isLazyAuditRequest() {
		return lazyAuditRequest;
	}

	public void setLazyAuditRequest(boolean lazyAuditRequest) {
		this.lazyAuditRequest = lazyAuditRequest;
	}

	public boolean isRequestAudited() {
		return requestAudited;
	}

	public void setRequestAudited(boolean requestAudited) {
		this.requestAudited = requestAudited;
	}

	public boolean isExecutionAudited() {
		return executionAudited;
	}

	public void setExecutionAudited(boolean executionAudited) {
		this.executionAudited = executionAudited;
	}

	public LensContextStatsType getStats() {
		return stats;
	}

	public void setStats(LensContextStatsType stats) {
		this.stats = stats;
	}

	public OperationBusinessContextType getRequestBusinessContext() {
		if (options == null) {
			return null;
		}
		return options.getRequestBusinessContext();
	}

	/**
	 * Returns all changes, user and all accounts. Both primary and secondary
	 * changes are returned, but these are not merged. TODO: maybe it would be
	 * better to merge them.
	 */
	public Collection<ObjectDelta<? extends ObjectType>> getAllChanges() throws SchemaException {
		Collection<ObjectDelta<? extends ObjectType>> allChanges = new ArrayList<>();
		if (focusContext != null) {
			addChangeIfNotNull(allChanges, focusContext.getPrimaryDelta());
			addChangeIfNotNull(allChanges, focusContext.getSecondaryDelta());
		}
		for (LensProjectionContext projCtx : getProjectionContexts()) {
			addChangeIfNotNull(allChanges, projCtx.getPrimaryDelta());
			addChangeIfNotNull(allChanges, projCtx.getSecondaryDelta());
		}
		return allChanges;
	}

	public boolean hasAnyPrimaryChange() throws SchemaException {
		if (focusContext != null) {
			if (!ObjectDelta.isNullOrEmpty(focusContext.getPrimaryDelta())) {
				return true;
			}
		}
		for (LensProjectionContext projCtx : getProjectionContexts()) {
			if (!ObjectDelta.isNullOrEmpty(projCtx.getPrimaryDelta())) {
				return true;
			}
		}
		return false;
	}

	public Collection<ObjectDelta<? extends ObjectType>> getPrimaryChanges() throws SchemaException {
		Collection<ObjectDelta<? extends ObjectType>> allChanges = new ArrayList<>();
		if (focusContext != null) {
			addChangeIfNotNull(allChanges, focusContext.getPrimaryDelta());
		}
		for (LensProjectionContext projCtx : getProjectionContexts()) {
			addChangeIfNotNull(allChanges, projCtx.getPrimaryDelta());
		}
		return allChanges;
	}

	private <T extends ObjectType> void addChangeIfNotNull(
			Collection<ObjectDelta<? extends ObjectType>> changes, ObjectDelta<T> change) {
		if (change != null) {
			changes.add(change);
		}
	}

	public void replacePrimaryFocusDelta(ObjectDelta<F> newDelta) {
		focusContext.setPrimaryDelta(newDelta);
		// todo any other changes have to be done?
	}

	public void replacePrimaryFocusDeltas(List<ObjectDelta<F>> deltas) throws SchemaException {
		replacePrimaryFocusDelta(null);
		if (deltas != null) {
			for (ObjectDelta<F> delta : deltas) {
				focusContext.addPrimaryDelta(delta);
			}
		}
		// todo any other changes have to be done?
	}

	/**
	 * Returns all executed deltas, user and all accounts.
	 */
	public Collection<ObjectDeltaOperation<? extends ObjectType>> getExecutedDeltas() throws SchemaException {
		return getExecutedDeltas(null);
	}

	/**
	 * Returns all executed deltas, user and all accounts.
	 */
	public Collection<ObjectDeltaOperation<? extends ObjectType>> getUnauditedExecutedDeltas()
			throws SchemaException {
		return getExecutedDeltas(false);
	}

	/**
	 * Returns all executed deltas, user and all accounts.
	 */
	Collection<ObjectDeltaOperation<? extends ObjectType>> getExecutedDeltas(Boolean audited)
			throws SchemaException {
		Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas = new ArrayList<>();
		if (focusContext != null) {
			executedDeltas.addAll(focusContext.getExecutedDeltas(audited));
		}
		for (LensProjectionContext projCtx : getProjectionContexts()) {
			executedDeltas.addAll(projCtx.getExecutedDeltas(audited));
		}
		if (audited == null) {
			executedDeltas.addAll((Collection<? extends ObjectDeltaOperation<? extends ObjectType>>) getRottenExecutedDeltas());
		}
		return executedDeltas;
	}

	public void markExecutedDeltasAudited() {
		if (focusContext != null) {
			focusContext.markExecutedDeltasAudited();
		}
		for (LensProjectionContext projCtx : getProjectionContexts()) {
			projCtx.markExecutedDeltasAudited();
		}
	}

	public List<LensObjectDeltaOperation<?>> getRottenExecutedDeltas() {
		return rottenExecutedDeltas;
	}

	public void recompute() throws SchemaException {
		recomputeFocus();
		recomputeProjections();
	}

	// mainly computes new state based on old state and delta(s)
	public void recomputeFocus() throws SchemaException {
		if (focusContext != null) {
			focusContext.recompute();
		}
	}

	public void recomputeProjections() throws SchemaException {
		for (LensProjectionContext projCtx : getProjectionContexts()) {
			projCtx.recompute();
		}
	}

	public void refreshAuxiliaryObjectClassDefinitions() throws SchemaException {
		for (LensProjectionContext projCtx : getProjectionContexts()) {
			projCtx.refreshAuxiliaryObjectClassDefinitions();
		}
	}

	public void checkAbortRequested() {
		if (isAbortRequested()) {
			throw new RuntimeException("Aborted on user request"); // TODO more
																	// meaningful
																	// exception
																	// + message
		}
	}

	public void checkConsistence() {
		checkAbortRequested();
		if (focusContext != null) {
			focusContext.checkConsistence();
		}
		for (LensProjectionContext projectionContext : projectionContexts) {
			projectionContext.checkConsistence(this.toString(), isFresh,
					ModelExecuteOptions.isForce(options));
		}
	}

	public void checkEncrypted() {
		if (focusContext != null && !focusContext.isDelete()) {
			focusContext.checkEncrypted();
		}
		for (LensProjectionContext projectionContext : projectionContexts) {
			if (!projectionContext.isDelete()) {
				projectionContext.checkEncrypted();
			}
		}
	}

	public LensProjectionContext createProjectionContext() {
		return createProjectionContext(null);
	}

	public LensProjectionContext createProjectionContext(ResourceShadowDiscriminator rat) {
		LensProjectionContext projCtx = new LensProjectionContext(this, rat);
		addProjectionContext(projCtx);
		return projCtx;
	}

	private Map<String, ResourceType> getResourceCache() {
		if (resourceCache == null) {
			resourceCache = new HashMap<>();
		}
		return resourceCache;
	}

	/**
	 * Returns a resource for specified account type. This is supposed to be
	 * efficient, taking the resource from the cache. It assumes the resource is
	 * in the cache.
	 *
	 * @see LensContext#rememberResource(ResourceType)
	 */
	public ResourceType getResource(ResourceShadowDiscriminator rat) {
		return getResource(rat.getResourceOid());
	}

	/**
	 * Returns a resource for specified account type. This is supposed to be
	 * efficient, taking the resource from the cache. It assumes the resource is
	 * in the cache.
	 *
	 * @see LensContext#rememberResource(ResourceType)
	 */
	public ResourceType getResource(String resourceOid) {
		return getResourceCache().get(resourceOid);
	}

	/**
	 * Puts resources in the cache for later use. The resources should be
	 * fetched from provisioning and have pre-parsed schemas. So the next time
	 * just reuse them without the other overhead.
	 */
	public void rememberResources(Collection<ResourceType> resources) {
		for (ResourceType resourceType : resources) {
			rememberResource(resourceType);
		}
	}

	/**
	 * Puts resource in the cache for later use. The resource should be fetched
	 * from provisioning and have pre-parsed schemas. So the next time just
	 * reuse it without the other overhead.
	 */
	public void rememberResource(ResourceType resourceType) {
		getResourceCache().put(resourceType.getOid(), resourceType);
	}

	/**
	 * Cleans up the contexts by removing some of the working state. The current
	 * wave number is retained. Otherwise it ends up in endless loop.
	 */
	public void cleanup() throws SchemaException {
		if (focusContext != null) {
			focusContext.cleanup();
		}
		for (LensProjectionContext projectionContext : projectionContexts) {
			projectionContext.cleanup();
		}
		recompute();
	}

	public void adopt(PrismContext prismContext) throws SchemaException {
		this.prismContext = prismContext;

		if (focusContext != null) {
			focusContext.adopt(prismContext);
		}
		for (LensProjectionContext projectionContext : projectionContexts) {
			projectionContext.adopt(prismContext);
		}
	}

	public void normalize() {
		if (focusContext != null) {
			focusContext.normalize();
		}
		if (projectionContexts != null) {
			for (LensProjectionContext projectionContext : projectionContexts) {
				projectionContext.normalize();
			}
		}
	}

	public LensContext<F> clone() {
		LensContext<F> clone = new LensContext<>(focusClass, prismContext, provisioningService);
		copyValues(clone);
		return clone;
	}

	protected void copyValues(LensContext<F> clone) {
		clone.state = this.state;
		clone.channel = this.channel;
		clone.doReconciliationForAllProjections = this.doReconciliationForAllProjections;
		clone.executionPhaseOnly = this.executionPhaseOnly;
		clone.focusClass = this.focusClass;
		clone.isFresh = this.isFresh;
		clone.isRequestAuthorized = this.isRequestAuthorized;
		clone.prismContext = this.prismContext;
		clone.resourceCache = cloneResourceCache();
		// User template is de-facto immutable, OK to just pass reference here.
		clone.focusTemplate = this.focusTemplate;
		clone.projectionWave = this.projectionWave;
		if (options != null) {
			clone.options = this.options.clone();
		}

		if (this.focusContext != null) {
			clone.focusContext = this.focusContext.clone(this);
		}

		for (LensProjectionContext thisProjectionContext : this.projectionContexts) {
			clone.projectionContexts.add(thisProjectionContext.clone(this));
		}
	}

	private Map<String, ResourceType> cloneResourceCache() {
		if (resourceCache == null) {
			return null;
		}
		Map<String, ResourceType> clonedMap = new HashMap<>();
		for (Entry<String, ResourceType> entry : resourceCache.entrySet()) {
			clonedMap.put(entry.getKey(), entry.getValue());
		}
		return clonedMap;
	}

	public void distributeResource() {
		for (LensProjectionContext projCtx : getProjectionContexts()) {
			projCtx.distributeResource();
		}
	}

	@Override
	public Class<F> getFocusClass() {
		return focusClass;
	}

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	public String dump(boolean showTriples) {
		return debugDump(0, showTriples);
	}

	@Override
	public String debugDump(int indent) {
		return debugDump(indent, true);
	}

	public String debugDump(int indent, boolean showTriples) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append("LensContext: state=").append(state);
		sb.append(", Wave(e=").append(executionWave);
		sb.append(",p=").append(projectionWave);
		sb.append(",max=").append(getMaxWave());
		sb.append("), ");
		if (focusContext != null) {
			sb.append("focus, ");
		}
		sb.append(projectionContexts.size());
		sb.append(" projections, ");
		try {
			Collection<ObjectDelta<? extends ObjectType>> allChanges = getAllChanges();
			sb.append(allChanges.size());
		} catch (SchemaException e) {
			sb.append("[ERROR]");
		}
		sb.append(" changes, ");
		sb.append("fresh=").append(isFresh);
		sb.append(", reqAutz=").append(isRequestAuthorized);
		if (systemConfiguration == null) {
			sb.append(" null-system-configuration");
		}
		if (executionPhaseOnly) {
			sb.append(" execution-phase-only");
		}
		sb.append("\n");

		DebugUtil.debugDumpLabel(sb, "Channel", indent + 1);
		sb.append(" ").append(channel).append("\n");
		DebugUtil.debugDumpLabel(sb, "Options", indent + 1);
		sb.append(" ").append(options).append("\n");
		DebugUtil.debugDumpLabel(sb, "Settings", indent + 1);
		sb.append(" ");
		if (accountSynchronizationSettings != null) {
			sb.append("assignments=");
			sb.append(accountSynchronizationSettings.getAssignmentPolicyEnforcement());
		} else {
			sb.append("null");
		}
		sb.append("\n");

		DebugUtil.debugDumpWithLabel(sb, "FOCUS", focusContext, indent + 1);

		sb.append("\n");
		if (DebugUtil.isDetailedDebugDump()) {
			DebugUtil.debugDumpWithLabel(sb, "EvaluatedAssignments", evaluatedAssignmentTriple, indent + 3);
		} else {
			DebugUtil.indentDebugDump(sb, indent + 3);
			sb.append("Evaluated assignments:");
			if (evaluatedAssignmentTriple != null) {
				dumpEvaluatedAssignments(sb, "Zero", (Collection) evaluatedAssignmentTriple.getZeroSet(),
						indent + 4);
				dumpEvaluatedAssignments(sb, "Plus", (Collection) evaluatedAssignmentTriple.getPlusSet(),
						indent + 4);
				dumpEvaluatedAssignments(sb, "Minus", (Collection) evaluatedAssignmentTriple.getMinusSet(),
						indent + 4);
			} else {
				sb.append(" (null)");
			}
		}
		sb.append("\n");
		DebugUtil.indentDebugDump(sb, indent + 1);
		sb.append("PROJECTIONS:");
		if (projectionContexts.isEmpty()) {
			sb.append(" none");
		} else {
			sb.append(" (").append(projectionContexts.size()).append("):");
			for (LensProjectionContext projCtx : projectionContexts) {
				sb.append("\n");
				sb.append(projCtx.debugDump(indent + 2, showTriples));
			}
		}
		if (historicResourceObjects != null && !historicResourceObjects.isEmpty()) {
			sb.append("\n");
			DebugUtil.debugDumpWithLabel(sb, "Deleted/unlinked resource objects",
					historicResourceObjects.toString(), indent + 1); // temporary
																		// impl
		}

		return sb.toString();
	}

	@Override
	public String dumpPolicyRules(int indent) {
		if (evaluatedAssignmentTriple == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		evaluatedAssignmentTriple.debugDumpSets(sb, assignment -> {
			DebugUtil.indentDebugDump(sb, indent);
			sb.append(assignment.toHumanReadableString());
			@SuppressWarnings("unchecked")
			Collection<EvaluatedPolicyRule> thisTargetPolicyRules = assignment.getThisTargetPolicyRules();
			dumpPolicyRulesCollection("thisTargetPolicyRules", indent + 1, sb, thisTargetPolicyRules);
			@SuppressWarnings({ "unchecked", "raw" })
			Collection<EvaluatedPolicyRule> otherTargetsPolicyRules = assignment.getOtherTargetsPolicyRules();
			dumpPolicyRulesCollection("otherTargetsPolicyRules", indent + 1, sb, otherTargetsPolicyRules);
			@SuppressWarnings({ "unchecked", "raw" })
			Collection<EvaluatedPolicyRule> focusPolicyRules = assignment.getFocusPolicyRules();
			dumpPolicyRulesCollection("focusPolicyRules", indent + 1, sb, focusPolicyRules);
		}, 1);
		return sb.toString();
	}

	private void dumpPolicyRulesCollection(String label, int indent, StringBuilder sb, Collection<EvaluatedPolicyRule> rules) {
		sb.append("\n");
		DebugUtil.indentDebugDump(sb, indent);
		sb.append(label).append(" (").append(rules.size()).append("):");
		for (EvaluatedPolicyRule rule : rules) {
			sb.append("\n");
			DebugUtil.indentDebugDump(sb, indent + 1);
			if (rule.isGlobal()) {
				sb.append("global ");
			}
			sb.append("rule: ").append(rule.getName());
			for (EvaluatedPolicyRuleTrigger trigger : rule.getTriggers()) {
				sb.append("\n");
				DebugUtil.indentDebugDump(sb, indent + 2);
				sb.append("trigger: ").append(trigger);
				if (trigger instanceof EvaluatedExclusionTrigger
						&& ((EvaluatedExclusionTrigger) trigger).getConflictingAssignment() != null) {
					sb.append("\n");
					DebugUtil.indentDebugDump(sb, indent + 3);
					sb.append("conflict: ")
							.append(((EvaluatedAssignmentImpl) ((EvaluatedExclusionTrigger) trigger)
									.getConflictingAssignment()).toHumanReadableString());
				}
			}
			for (PolicyExceptionType exc : rule.getPolicyExceptions()) {
				sb.append("\n");
				DebugUtil.indentDebugDump(sb, indent + 2);
				sb.append("exception: ").append(exc);
			}
		}
	}

	// <F> of LensContext is of ObjectType; so we need to override it (but using another name to avoid IDE warnings)
	private <FT extends FocusType> void dumpEvaluatedAssignments(StringBuilder sb, String label,
			Collection<EvaluatedAssignmentImpl<FT>> set, int indent) {
		sb.append("\n");
		DebugUtil.debugDumpLabel(sb, label, indent);
		for (EvaluatedAssignmentImpl<FT> assignment : set) {
			sb.append("\n");
			DebugUtil.indentDebugDump(sb, indent + 1);
			sb.append("-> ").append(assignment.getTarget());
			dumpRules(sb, "focus rules", indent + 3, assignment.getFocusPolicyRules());
			dumpRules(sb, "this target rules", indent + 3, assignment.getThisTargetPolicyRules());
			dumpRules(sb, "other targets rules", indent + 3, assignment.getOtherTargetsPolicyRules());
		}
	}

	private void dumpRules(StringBuilder sb, String label, int indent, Collection<EvaluatedPolicyRule> policyRules) {
		if (policyRules.isEmpty()) {
			return;
		}
		sb.append("\n");
		DebugUtil.debugDumpLabel(sb, "- " + label + " (" + policyRules.size() + ", triggered " + getTriggeredRulesCount(policyRules) + ")", indent);
		boolean first = true;
		for (EvaluatedPolicyRule rule : policyRules) {
			if (first) {
				first = false;
				sb.append(" ");
			} else {
				sb.append("; ");
			}
			if (rule.isGlobal()) {
				sb.append("G:");
			}
			if (rule.getName() != null) {
				sb.append(rule.getName());
			}
			sb.append("(").append(PolicyRuleTypeUtil.toShortString(rule.getPolicyConstraints())).append(")");
			sb.append("->");
			sb.append("(").append(PolicyRuleTypeUtil.toShortString(rule.getActions())).append(")");
			if (!rule.getTriggers().isEmpty()) {
				sb.append("=>T:(");
				sb.append(rule.getTriggers().stream().map(EvaluatedPolicyRuleTrigger::toDiagShortcut)
						.collect(Collectors.joining(", ")));
				sb.append(")");
			}
		}
	}

	static int getTriggeredRulesCount(Collection<EvaluatedPolicyRule> policyRules) {
		return (int) policyRules.stream().filter(r -> !r.getTriggers().isEmpty()).count();
	}

	public LensContextType toLensContextType() throws SchemaException {
		return toLensContextType(false);
	}

	/**
	 * 'reduced' means
	 * - no projection contexts except those with primary or sync deltas
	 * - no full object values (focus, shadow).
	 *
	 * This mode is to be used for re-starting operation after primary-stage approval (here all data are re-loaded; maybe
	 * except for objectOld, but let's neglect it for the time being).
	 *
	 * It is also to be used for the FINAL stage, where we need the context basically for information about executed deltas.
	 */
	public LensContextType toLensContextType(boolean reduced) throws SchemaException {

		PrismContainer<LensContextType> lensContextTypeContainer = PrismContainer
				.newInstance(getPrismContext(), LensContextType.COMPLEX_TYPE);
		LensContextType lensContextType = lensContextTypeContainer.createNewValue().asContainerable();

		lensContextType.setState(state != null ? state.toModelStateType() : null);
		lensContextType.setChannel(channel);

		if (focusContext != null) {
			PrismContainer<LensFocusContextType> lensFocusContextTypeContainer = lensContextTypeContainer
					.findOrCreateContainer(LensContextType.F_FOCUS_CONTEXT);
			focusContext.addToPrismContainer(lensFocusContextTypeContainer, reduced);
		}

		PrismContainer<LensProjectionContextType> lensProjectionContextTypeContainer = lensContextTypeContainer
				.findOrCreateContainer(LensContextType.F_PROJECTION_CONTEXT);
		for (LensProjectionContext lensProjectionContext : projectionContexts) {
			// primary delta can be null because of delta reduction algorithm (when approving associations)
			if (!reduced || lensProjectionContext.getPrimaryDelta() != null || !ObjectDelta.isNullOrEmpty(lensProjectionContext.getSyncDelta())) {
				lensProjectionContext.addToPrismContainer(lensProjectionContextTypeContainer, reduced);
			}
		}
		lensContextType.setFocusClass(focusClass != null ? focusClass.getName() : null);
		lensContextType.setDoReconciliationForAllProjections(doReconciliationForAllProjections);
		lensContextType.setExecutionPhaseOnly(executionPhaseOnly);
		lensContextType.setProjectionWave(projectionWave);
		lensContextType.setExecutionWave(executionWave);
		lensContextType.setOptions(options != null ? options.toModelExecutionOptionsType() : null);
		lensContextType.setLazyAuditRequest(lazyAuditRequest);
		lensContextType.setRequestAudited(requestAudited);
		lensContextType.setExecutionAudited(executionAudited);
		lensContextType.setRequestAuthorized(isRequestAuthorized);
		lensContextType.setStats(stats);
		lensContextType.setRequestMetadata(requestMetadata);

		for (LensObjectDeltaOperation<?> executedDelta : rottenExecutedDeltas) {
			lensContextType.getRottenExecutedDeltas().add(simplifyExecutedDelta(executedDelta).toLensObjectDeltaOperationType());
		}

		return lensContextType;
	}

	static <T extends ObjectType> LensObjectDeltaOperation<T> simplifyExecutedDelta(LensObjectDeltaOperation<T> executedDelta) {
		LensObjectDeltaOperation<T> rv = executedDelta.clone();	// TODO something more optimized (no need to clone things deeply, just create new object with replaced operation result)
		rv.setExecutionResult(OperationResult.keepRootOnly(executedDelta.getExecutionResult()));
		return rv;
	}

	@SuppressWarnings({"unchecked", "raw"})
	public static LensContext fromLensContextType(LensContextType lensContextType, PrismContext prismContext,
			ProvisioningService provisioningService, Task task, OperationResult parentResult)
			throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {

		OperationResult result = parentResult.createSubresult(DOT_CLASS + "fromLensContextType");

		String focusClassString = lensContextType.getFocusClass();

		if (StringUtils.isEmpty(focusClassString)) {
			throw new SystemException("Focus class is undefined in LensContextType");
		}

		LensContext lensContext;
		try {
			lensContext = new LensContext(Class.forName(focusClassString), prismContext, provisioningService);
		} catch (ClassNotFoundException e) {
			throw new SystemException(
					"Couldn't instantiate LensContext because focus or projection class couldn't be found",
					e);
		}

		lensContext.setState(ModelState.fromModelStateType(lensContextType.getState()));
		lensContext.setChannel(lensContextType.getChannel());
		lensContext.setFocusContext(LensFocusContext
				.fromLensFocusContextType(lensContextType.getFocusContext(), lensContext, task, result));
		for (LensProjectionContextType lensProjectionContextType : lensContextType.getProjectionContext()) {
			lensContext.addProjectionContext(LensProjectionContext
					.fromLensProjectionContextType(lensProjectionContextType, lensContext, task, result));
		}
		lensContext.setDoReconciliationForAllProjections(
				lensContextType.isDoReconciliationForAllProjections() != null
						? lensContextType.isDoReconciliationForAllProjections() : false);
		lensContext.setExecutionPhaseOnly(
				lensContextType.isExecutionPhaseOnly() != null
						? lensContextType.isExecutionPhaseOnly() : false);
		lensContext.setProjectionWave(
				lensContextType.getProjectionWave() != null ? lensContextType.getProjectionWave() : 0);
		lensContext.setExecutionWave(
				lensContextType.getExecutionWave() != null ? lensContextType.getExecutionWave() : 0);
		lensContext
				.setOptions(ModelExecuteOptions.fromModelExecutionOptionsType(lensContextType.getOptions()));
		if (lensContextType.isLazyAuditRequest() != null) {
			lensContext.setLazyAuditRequest(lensContextType.isLazyAuditRequest());
		}
		if (lensContextType.isRequestAudited() != null) {
			lensContext.setRequestAudited(lensContextType.isRequestAudited());
		}
		if (lensContextType.isExecutionAudited() != null) {
			lensContext.setExecutionAudited(lensContextType.isExecutionAudited());
		}
		lensContext.setRequestAuthorized(Boolean.TRUE.equals(lensContextType.isRequestAuthorized()));
		lensContext.setStats(lensContextType.getStats());
		lensContext.setRequestMetadata(lensContextType.getRequestMetadata());

		for (LensObjectDeltaOperationType eDeltaOperationType : lensContextType.getRottenExecutedDeltas()) {
			LensObjectDeltaOperation objectDeltaOperation = LensObjectDeltaOperation
					.fromLensObjectDeltaOperationType(eDeltaOperationType, lensContext.getPrismContext());
			if (objectDeltaOperation.getObjectDelta() != null) {
				lensContext.fixProvisioningTypeInDelta(objectDeltaOperation.getObjectDelta(), task, result);
			}
			lensContext.rottenExecutedDeltas.add(objectDeltaOperation);
		}

		if (result.isUnknown()) {
			result.computeStatus();
		}
		return lensContext;
	}

	protected void fixProvisioningTypeInDelta(ObjectDelta delta, Task task, OperationResult result)
			throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		if (delta != null && delta.getObjectTypeClass() != null
				&& (ShadowType.class.isAssignableFrom(delta.getObjectTypeClass())
						|| ResourceType.class.isAssignableFrom(delta.getObjectTypeClass()))) {
			getProvisioningService().applyDefinition(delta, task, result);
		}
	}

	@Override
	public String toString() {
		return "LensContext(s=" + state + ", W(e=" + executionWave + ",p=" + projectionWave + "): "
				+ focusContext + ", " + projectionContexts + ")";
	}

	public void setProgressListeners(Collection<ProgressListener> progressListeners) {
		this.progressListeners = progressListeners;
	}

	public Collection<ProgressListener> getProgressListeners() {
		return progressListeners;
	}

	@Override
	public void reportProgress(ProgressInformation progress) {
		if (progressListeners == null) {
			return;
		}

		for (ProgressListener listener : progressListeners) {
			listener.onProgressAchieved(this, progress);
		}
	}

	public boolean isAbortRequested() {
		if (progressListeners == null) {
			return false;
		}
		for (ProgressListener progressListener : progressListeners) {
			if (progressListener.isAbortRequested()) {
				return true;
			}
		}
		return false;
	}

	public Collection<ResourceShadowDiscriminator> getHistoricResourceObjects() {
		if (historicResourceObjects == null) {
			historicResourceObjects = new ArrayList<>();
		}
		return historicResourceObjects;
	}

	public Map<String, Long> getSequences() {
		return sequences;
	}

	public Long getSequenceCounter(String sequenceOid) {
		return sequences.get(sequenceOid);
	}

	public void setSequenceCounter(String sequenceOid, long counter) {
		sequences.put(sequenceOid, counter);
	}

	public List<LensProjectionContext> getConflictingProjectionContexts() {
		return conflictingProjectionContexts;
	}

	public void addConflictingProjectionContext(LensProjectionContext conflictingContext) {
		conflictingProjectionContexts.add(conflictingContext);
	}

	public void clearConflictingProjectionContexts() {
		conflictingProjectionContexts.clear();
	}

	public boolean hasExplosiveProjection() throws SchemaException {
		for (LensProjectionContext projectionContext : projectionContexts) {
			if (projectionContext.getVolatility() == ResourceObjectVolatilityType.EXPLOSIVE) {
				return true;
			}
		}
		return false;
	}

	@NotNull
	public Map<String, Collection<Containerable>> getHookPreviewResultsMap() {
		if (hookPreviewResultsMap == null) {
			hookPreviewResultsMap = new HashMap<>();
		}
		return hookPreviewResultsMap;
	}

	public void addHookPreviewResults(String hookUri, Collection<Containerable> results) {
		getHookPreviewResultsMap().put(hookUri, results);
	}

	@NotNull
	@Override
	public <T> List<T> getHookPreviewResults(@NotNull Class<T> clazz) {
		List<T> rv = new ArrayList<>();
		for (Collection<Containerable> collection : getHookPreviewResultsMap().values()) {
			for (Containerable item : CollectionUtils.emptyIfNull(collection)) {
				if (item != null && clazz.isAssignableFrom(item.getClass())) {
					rv.add((T) item);
				}
			}
		}
		return rv;
	}

	@Nullable
	@Override
	public <T> T getHookPreviewResult(@NotNull Class<T> clazz) {
		List<T> results = getHookPreviewResults(clazz);
		if (results.size() > 1) {
			throw new IllegalStateException("More than one preview result of type " + clazz);
		} else if (results.size() == 1) {
			return results.get(0);
		} else {
			return null;
		}
	}

	public int getConflictResolutionAttemptNumber() {
		return conflictResolutionAttemptNumber;
	}

	public void setConflictResolutionAttemptNumber(int conflictResolutionAttemptNumber) {
		this.conflictResolutionAttemptNumber = conflictResolutionAttemptNumber;
	}

	@NotNull
	public List<ConflictWatcher> getConflictWatchers() {
		return conflictWatchers;
	}

	public ConflictWatcher createAndRegisterConflictWatcher(String oid, RepositoryService repositoryService) {
		ConflictWatcher watcher = repositoryService.createAndRegisterConflictWatcher(oid);
		conflictWatchers.add(watcher);
		return watcher;
	}

	public void unregisterConflictWatchers(RepositoryService repositoryService) {
		conflictWatchers.forEach(w -> repositoryService.unregisterConflictWatcher(w));
		conflictWatchers.clear();
	}
}
