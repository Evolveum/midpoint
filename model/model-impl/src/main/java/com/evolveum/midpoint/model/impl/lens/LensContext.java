/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import java.util.*;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.stream.Stream;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.task.api.RunningTask;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ProgressInformation;
import com.evolveum.midpoint.model.api.ProgressListener;
import com.evolveum.midpoint.model.api.context.*;
import com.evolveum.midpoint.model.api.util.ClockworkInspector;
import com.evolveum.midpoint.model.impl.lens.assignments.EvaluatedAssignmentImpl;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.ConflictWatcher;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.ObjectTreeDeltas;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.constants.Channel;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectDeltaSchemaLevelUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.*;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author semancik
 */
public class LensContext<F extends ObjectType> implements ModelContext<F>, Cloneable {

    private static final long serialVersionUID = -778283437426659540L;
    private static final String DOT_CLASS = LensContext.class.getName() + ".";

    private static final Trace LOGGER = TraceManager.getTrace(LensContext.class);

    public enum ExportType {
        MINIMAL, REDUCED, OPERATIONAL, TRACE
    }

    /**
     * Unique identifier of a request (operation).
     * Used to correlate request and execution audit records of a single operation.
     * Note: taskId cannot be used for that, as task can have many operations (e.g. reconciliation task).
     */
    private String requestIdentifier = null;

    /**
     * Identifier of the current "item processing". Set by iterative tasks.
     * Should not be propagated to any other lens context!
     */
    @Experimental // maybe will be removed later
    private String itemProcessingIdentifier;

    private ModelState state = ModelState.INITIAL;

    private transient ConflictWatcher focusConflictWatcher;

    private int conflictResolutionAttemptNumber;

    // For use with personas
    private String ownerOid;

    private transient PrismObject<UserType> cachedOwner;

    private transient SecurityPolicyType globalSecurityPolicy;

    /**
     * Channel that is the source of primary change (GUI, live sync, import, ...)
     */
    private String channel;

    private LensFocusContext<F> focusContext;
    @NotNull private final Collection<LensProjectionContext> projectionContexts = new ArrayList<>();

    /**
     * EXPERIMENTAL. A trace of resource objects that once existed but were
     * unlinked or deleted, and the corresponding contexts were rotten and
     * removed afterwards.
     * <p>
     * Necessary to evaluate old state of hasLinkedAccount.
     * <p>
     * TODO implement as non-transient. TODO consider storing whole projection
     * contexts here.
     */
    private transient Collection<ResourceShadowDiscriminator> historicResourceObjects;

    private Class<F> focusClass;

    private boolean lazyAuditRequest = false; // should be the request audited just before the execution is audited?
    private boolean requestAudited = false; // was the request audited?
    private boolean executionAudited = false; // was the execution audited?
    private LensContextStatsType stats = new LensContextStatsType();

    /**
     * Metadata of the request. Metadata recorded when the operation has
     * started. Currently only the requestor related data (requestTimestamp, requestorRef)
     * are collected. But later other metadata may be used.
     */
    private MetadataType requestMetadata;

    /**
     * Executed deltas from rotten projection contexts.
     */
    @NotNull private final List<LensObjectDeltaOperation<?>> rottenExecutedDeltas = new ArrayList<>();

    private transient ObjectTemplateType focusTemplate;
    private boolean focusTemplateExternallySet;       // todo serialize this
    private transient ProjectionPolicyType accountSynchronizationSettings;

    /**
     * Delta set triple (plus, minus, zero) for evaluated assignments.
     * Relativity is determined by looking at current delta (i.e. objectCurrent -> objectNew).
     * <p>
     * If you want to know whether the assignment was added or deleted with regards to objectOld,
     * please check evaluatedAssignment.origin property -
     * {@link com.evolveum.midpoint.model.impl.lens.projector.AssignmentOrigin}.
     * <p>
     * TODO verify if this is really true
     */
    private transient DeltaSetTriple<EvaluatedAssignmentImpl<?>> evaluatedAssignmentTriple;

    /**
     * Just a cached copy. Keep it in context so we do not need to reload it all
     * the time.
     */
    private transient PrismObject<SystemConfigurationType> systemConfiguration;

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
    private int projectionWave = 0;

    /**
     * Current wave of execution.
     */
    private int executionWave = 0;

    private String triggeredResourceOid;

    /**
     * At this level, isFresh == false means that deeper recomputation has to be
     * carried out.
     */
    private transient boolean isFresh = false;
    private boolean isRequestAuthorized = false;

    /**
     * Cache of resource instances. It is used to reduce the number of read
     * (getObject) calls for ResourceType objects.
     */
    private transient Map<String, ResourceType> resourceCache;

    private transient PrismContext prismContext;

    private transient ProvisioningService provisioningService;

    private ModelExecuteOptions options;

    /**
     * Used mostly in unit tests.
     */
    private transient ClockworkInspector inspector;

    /**
     * User feedback.
     */
    private transient Collection<ProgressListener> progressListeners;

    private final Map<String, Long> sequences = new HashMap<>();

    /**
     * Moved from ProjectionValuesProcessor TODO consider if necessary to
     * serialize to XML
     */
    @NotNull private final List<LensProjectionContext> conflictingProjectionContexts = new ArrayList<>();

    private transient boolean preview;

    private transient Map<String, Collection<Containerable>> hookPreviewResultsMap;

    private transient PolicyRuleEnforcerPreviewOutputType policyRuleEnforcerPreviewOutput;

    @NotNull private transient final List<ObjectReferenceType> operationApprovedBy = new ArrayList<>();
    @NotNull private transient final List<String> operationApproverComments = new ArrayList<>();

    private String taskTreeOid;

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
    public String getRequestIdentifier() {
        return requestIdentifier;
    }

    public void setRequestIdentifier(String requestIdentifier) {
        this.requestIdentifier = requestIdentifier;
    }

    public String getItemProcessingIdentifier() {
        return itemProcessingIdentifier;
    }

    public void setItemProcessingIdentifier(String itemProcessingIdentifier) {
        this.itemProcessingIdentifier = itemProcessingIdentifier;
    }

    public void generateRequestIdentifierIfNeeded() {
        if (requestIdentifier != null) {
            return;
        }
        requestIdentifier = ModelImplUtils.generateRequestIdentifier();
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

    public boolean hasFocusContext() {
        return focusContext != null;
    }

    @Override
    @NotNull
    public LensFocusContext<F> getFocusContextRequired() {
        return Objects.requireNonNull(focusContext, "No focus context");
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

    @Override
    public ObjectTemplateType getFocusTemplate() {
        return focusTemplate;
    }

    public void setFocusTemplate(ObjectTemplateType focusTemplate) {
        this.focusTemplate = focusTemplate;
    }

    public boolean isFocusTemplateExternallySet() {
        return focusTemplateExternallySet;
    }

    public void setFocusTemplateExternallySet(boolean focusTemplateExternallySet) {
        this.focusTemplateExternallySet = focusTemplateExternallySet;
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

    public SystemConfigurationType getSystemConfigurationType() {
        return systemConfiguration != null ? systemConfiguration.asObjectable() : null;
    }

    public InternalsConfigurationType getInternalsConfiguration() {
        return systemConfiguration != null ? systemConfiguration.asObjectable().getInternals() : null;
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

    private void setProjectionWave(int wave) {
        this.projectionWave = wave;
    }

    public void incrementProjectionWave() {
        projectionWave++;
        LOGGER.trace("Incrementing projection wave to {}", projectionWave);
    }

    public void resetProjectionWave() {
        LOGGER.trace("Resetting projection wave from {} to {}", projectionWave, executionWave);
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
        LOGGER.trace("Incrementing lens context execution wave to {}", executionWave);
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

    // TODO This method is currently used only when previewing changes. Is that OK?
    public int computeMaxWaves() {
        if (getPartialProcessingOptions().getInbound() != PartialProcessingTypeType.SKIP) {
            // Let's do one extra wave with no accounts in it. This time we expect to get the results of the execution to the user
            // via inbound, e.g. identifiers generated by the resource, DNs and similar things. Hence the +2 instead of +1
            return getMaxWave() + 2;
        } else {
            return getMaxWave() + 1;
        }
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
            focusContext.rot();
        }
        for (LensProjectionContext projectionContext : projectionContexts) {
            projectionContext.rot();
        }
    }

    /**
     * Force recompute for the next execution wave. Recompute only those contexts that were changed.
     * This is more intelligent than rot()
     */
    void rotIfNeeded() throws SchemaException {
        Holder<Boolean> rotHolder = new Holder<>(false);
        rotProjectionContextsIfNeeded(rotHolder);
        rotFocusContextIfNeeded(rotHolder);
        if (rotHolder.getValue()) {
            setFresh(false);
        }
    }

    private void rotProjectionContextsIfNeeded(Holder<Boolean> rotHolder) throws SchemaException {
        for (LensProjectionContext projectionContext : projectionContexts) {
            if (projectionContext.getWave() != executionWave) {
                LOGGER.trace("Context rot: projection {} NOT rotten because of wrong wave number", projectionContext);
            } else {
                ObjectDelta<ShadowType> execDelta = projectionContext.getExecutableDelta();
                if (isShadowDeltaSignificant(execDelta)) {
                    LOGGER.debug("Context rot: projection {} rotten because of executable delta {}", projectionContext, execDelta);
                    projectionContext.rot();
                    rotHolder.setValue(true);
                    // Propagate to higher-order projections
                    for (LensProjectionContext relCtx : LensUtil.findRelatedContexts(this, projectionContext)) {
                        relCtx.rot();
                    }
                } else {
                    LOGGER.trace("Context rot: projection {} NOT rotten because no delta", projectionContext);
                }
            }
        }
    }

    private <P extends ObjectType> boolean isShadowDeltaSignificant(ObjectDelta<P> delta) {
        if (delta == null || delta.isEmpty()) {
            return false;
        }
        if (delta.isAdd() || delta.isDelete()) {
            return true;
        } else {
            Collection<? extends ItemDelta<?, ?>> attrDeltas = delta.findItemDeltasSubPath(ShadowType.F_ATTRIBUTES);
            return attrDeltas != null && !attrDeltas.isEmpty();
        }
    }

    private void rotFocusContextIfNeeded(Holder<Boolean> rotHolder) {
        if (focusContext != null) {
            ObjectDelta<F> execDelta = focusContext.getCurrentDelta(); // TODO!!!
            if (!ObjectDelta.isEmpty(execDelta)) {
                LOGGER.debug("Context rot: context rotten because of focus execution delta {}", execDelta);
                rotHolder.setValue(true);
            }
            if (rotHolder.getValue()) {
                // It is OK to refresh focus all the time there was any change. This is cheap.
                focusContext.rot();
                // This would be nice but break some tests ... TODO check it
//                focusContext.setObjectCurrent(null);
//                focusContext.setObjectNew(null);
            }
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
        return doReconciliationForAllProjections || ModelExecuteOptions.isReconcileFocus(options);
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

    @NotNull
    @Override
    public Stream<EvaluatedAssignmentImpl<?>> getEvaluatedAssignmentsStream() {
        return evaluatedAssignmentTriple != null ? evaluatedAssignmentTriple.stream() : Stream.empty();
    }

    @NotNull
    @Override
    public Collection<EvaluatedAssignmentImpl<?>> getNonNegativeEvaluatedAssignments() {
        if (evaluatedAssignmentTriple != null) {
            return evaluatedAssignmentTriple.getNonNegativeValues();
        } else {
            return Collections.emptySet();
        }
    }

    @NotNull
    @Override
    public Collection<EvaluatedAssignmentImpl<?>> getAllEvaluatedAssignments() {
        if (evaluatedAssignmentTriple != null) {
            return evaluatedAssignmentTriple.getAllValues();
        } else {
            return Collections.emptySet();
        }
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

    public ClockworkInspector getInspector() {
        return inspector;
    }

    public void setInspector(ClockworkInspector inspector) {
        this.inspector = inspector;
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
        return ModelExecuteOptions.getRequestBusinessContext(options);
    }

    /**
     * Number of all changes. TODO reconsider this.
     */
    public int getAllChanges() {
        Collection<ObjectDelta<? extends ObjectType>> allChanges = new ArrayList<>();
        if (focusContext != null) {
            addChangeIfNotNull(allChanges, focusContext.getPrimaryDelta());
            addChangeIfNotNull(allChanges, focusContext.getSecondaryDelta());
        }
        for (LensProjectionContext projCtx : getProjectionContexts()) {
            addChangeIfNotNull(allChanges, projCtx.getPrimaryDelta());
            addChangeIfNotNull(allChanges, projCtx.getSecondaryDelta());
        }
        return allChanges.size();
    }

    public boolean hasAnyPrimaryChange() {
        if (focusContext != null) {
            if (!ObjectDelta.isEmpty(focusContext.getPrimaryDelta())) {
                return true;
            }
        }
        for (LensProjectionContext projCtx : getProjectionContexts()) {
            if (!ObjectDelta.isEmpty(projCtx.getPrimaryDelta())) {
                return true;
            }
        }
        return false;
    }

    public Collection<ObjectDelta<? extends ObjectType>> getPrimaryChanges() {
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

    /**
     * Returns all executed deltas, user and all accounts.
     */
    @NotNull
    public Collection<ObjectDeltaOperation<? extends ObjectType>> getExecutedDeltas() {
        return getExecutedDeltas(null);
    }

    /**
     * Returns all executed deltas, user and all accounts.
     */
    @NotNull
    public Collection<ObjectDeltaOperation<? extends ObjectType>> getUnauditedExecutedDeltas() {
        return getExecutedDeltas(false);
    }

    /**
     * Returns all executed deltas, user and all accounts.
     */
    @NotNull
    private Collection<ObjectDeltaOperation<? extends ObjectType>> getExecutedDeltas(Boolean audited) {
        Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas = new ArrayList<>();
        if (focusContext != null) {
            executedDeltas.addAll(focusContext.getExecutedDeltas(audited));
        }
        for (LensProjectionContext projCtx : getProjectionContexts()) {
            executedDeltas.addAll(projCtx.getExecutedDeltas(audited));
        }
        if (audited == null) {
            executedDeltas.addAll(getRottenExecutedDeltas());
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

    public @NotNull List<LensObjectDeltaOperation<?>> getRottenExecutedDeltas() {
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
            throw new RuntimeException("Aborted on user request"); // TODO more meaningful exception + message
        }
    }

    public void checkConsistenceIfNeeded() {
        if (InternalsConfig.consistencyChecks) {
            try {
                checkConsistence();
            } catch (IllegalStateException e) {
                throw new IllegalStateException(e.getMessage() + " in clockwork, state=" + state, e);
            }
        } else {
            checkAbortRequested(); // useful to check as often as realistically possible
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

    public void checkEncryptedIfNeeded() {
        if (InternalsConfig.encryptionChecks && !ModelExecuteOptions.isNoCrypt(options)) {
            checkEncrypted();
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

    public LensProjectionContext createProjectionContext(ResourceShadowDiscriminator rsd) {
        LensProjectionContext projCtx = new LensProjectionContext(this, rsd);
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
        for (LensProjectionContext projectionContext : projectionContexts) {
            projectionContext.normalize();
        }
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
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
        if (requestMetadata != null) {
            clone.requestMetadata = requestMetadata.clone();
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

    @Override
    public Class<F> getFocusClass() {
        return focusClass;
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
        if (ownerOid != null) {
            sb.append(", owner=");
            if (cachedOwner != null) {
                sb.append(cachedOwner);
            } else {
                sb.append(ownerOid);
            }
        }
        sb.append("), ");
        if (focusContext != null) {
            sb.append("focus, ");
        }
        sb.append(projectionContexts.size());
        sb.append(" projections, ");
        sb.append(getAllChanges());
        sb.append(" changes, ");
        sb.append("fresh=").append(isFresh);
        sb.append(", reqAutz=").append(isRequestAuthorized);
        if (systemConfiguration == null) {
            sb.append(" null-system-configuration");
        }
        if (executionPhaseOnly) {
            sb.append(" execution-phase-only");
        }
        sb.append(requestMetadata != null ? ", req. metadata present" : ", req. metadata missing");
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
        DebugUtil.debugDumpLabel(sb, "Focus template", indent + 1);
        sb.append(" ").append(focusTemplate).append("\n");

        DebugUtil.debugDumpWithLabel(sb, "FOCUS", focusContext, indent + 1);

        sb.append("\n");
        if (DebugUtil.isDetailedDebugDump()) {
            DebugUtil.debugDumpWithLabel(sb, "EvaluatedAssignments", evaluatedAssignmentTriple, indent + 3);
        } else {
            DebugUtil.indentDebugDump(sb, indent + 3);
            sb.append("Evaluated assignments:");
            if (evaluatedAssignmentTriple != null) {
                dumpEvaluatedAssignments(sb, "Zero", evaluatedAssignmentTriple.getZeroSet(), indent + 4);
                dumpEvaluatedAssignments(sb, "Plus", evaluatedAssignmentTriple.getPlusSet(), indent + 4);
                dumpEvaluatedAssignments(sb, "Minus", evaluatedAssignmentTriple.getMinusSet(), indent + 4);
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
    public String dumpAssignmentPolicyRules(int indent, boolean alsoMessages) {
        if (evaluatedAssignmentTriple == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        evaluatedAssignmentTriple.debugDumpSets(sb, assignment -> {
            DebugUtil.indentDebugDump(sb, indent);
            sb.append(assignment.toHumanReadableString());
            Collection<EvaluatedPolicyRuleImpl> thisTargetPolicyRules = assignment.getThisTargetPolicyRules();
            dumpPolicyRulesCollection("thisTargetPolicyRules", indent + 1, sb, thisTargetPolicyRules, alsoMessages);
            @SuppressWarnings({ "raw" })
            Collection<EvaluatedPolicyRuleImpl> otherTargetsPolicyRules = assignment.getOtherTargetsPolicyRules();
            dumpPolicyRulesCollection("otherTargetsPolicyRules", indent + 1, sb, otherTargetsPolicyRules, alsoMessages);
            @SuppressWarnings({ "raw" })
            Collection<EvaluatedPolicyRuleImpl> focusPolicyRules = assignment.getFocusPolicyRules();
            dumpPolicyRulesCollection("focusPolicyRules", indent + 1, sb, focusPolicyRules, alsoMessages);
        }, 1);
        return sb.toString();
    }

    @Override
    public String dumpFocusPolicyRules(int indent, boolean alsoMessages) {
        StringBuilder sb = new StringBuilder();
        if (focusContext != null) {
            dumpPolicyRulesCollection("objectPolicyRules", indent, sb, focusContext.getPolicyRules(), alsoMessages);
        }
        return sb.toString();
    }

    private void dumpPolicyRulesCollection(String label, int indent, StringBuilder sb, Collection<? extends EvaluatedPolicyRule> rules,
            boolean alsoMessages) {
        sb.append("\n");
        DebugUtil.indentDebugDump(sb, indent);
        sb.append(label).append(" (").append(rules.size()).append("):");
        for (EvaluatedPolicyRule rule : rules) {
            sb.append("\n");
            dumpPolicyRule(indent, sb, rule, alsoMessages);
        }
    }

    private void dumpPolicyRule(int indent, StringBuilder sb, EvaluatedPolicyRule rule, boolean alsoMessages) {
        if (alsoMessages) {
            sb.append("=============================================== RULE ===============================================\n");
        }
        DebugUtil.indentDebugDump(sb, indent + 1);
        if (rule.isGlobal()) {
            sb.append("global ");
        }
        sb.append("rule: ").append(rule.toShortString());
        dumpTriggersCollection(indent + 2, sb, rule.getTriggers());
        for (PolicyExceptionType exc : rule.getPolicyExceptions()) {
            sb.append("\n");
            DebugUtil.indentDebugDump(sb, indent + 2);
            sb.append("exception: ").append(exc);
        }
        if (alsoMessages) {
            if (rule.isTriggered()) {
                sb.append("\n\n");
                sb.append("--------------------------------------------- MESSAGES ---------------------------------------------");
                List<TreeNode<LocalizableMessage>> messageTrees = rule.extractMessages();
                for (TreeNode<LocalizableMessage> messageTree : messageTrees) {
                    sb.append("\n");
                    sb.append(messageTree.debugDump(indent));
                }
            }
            sb.append("\n");
        }
    }

    private void dumpTriggersCollection(int indent, StringBuilder sb, Collection<EvaluatedPolicyRuleTrigger<?>> triggers) {
        for (EvaluatedPolicyRuleTrigger trigger : triggers) {
            sb.append("\n");
            DebugUtil.indentDebugDump(sb, indent);
            sb.append("trigger: ").append(trigger);
            if (trigger instanceof EvaluatedExclusionTrigger
                    && ((EvaluatedExclusionTrigger) trigger).getConflictingAssignment() != null) {
                sb.append("\n");
                DebugUtil.indentDebugDump(sb, indent + 1);
                sb.append("conflict: ")
                        .append(((EvaluatedAssignmentImpl) ((EvaluatedExclusionTrigger) trigger)
                                .getConflictingAssignment()).toHumanReadableString());
            }
            if (trigger instanceof EvaluatedCompositeTrigger) {
                dumpTriggersCollection(indent + 1, sb, ((EvaluatedCompositeTrigger) trigger).getInnerTriggers());
            } else if (trigger instanceof EvaluatedTransitionTrigger) {
                dumpTriggersCollection(indent + 1, sb, ((EvaluatedTransitionTrigger) trigger).getInnerTriggers());
            }
        }
    }

    private void dumpEvaluatedAssignments(StringBuilder sb, String label, Collection<EvaluatedAssignmentImpl<?>> set, int indent) {
        sb.append("\n");
        DebugUtil.debugDumpLabel(sb, label, indent);
        for (EvaluatedAssignmentImpl<?> assignment : set) {
            sb.append("\n");
            DebugUtil.indentDebugDump(sb, indent + 1);
            sb.append("-> ");
            assignment.shortDump(sb);
            dumpRulesIfNotEmpty(sb, "- focus rules", indent + 3, assignment.getFocusPolicyRules());
            dumpRulesIfNotEmpty(sb, "- this target rules", indent + 3, assignment.getThisTargetPolicyRules());
            dumpRulesIfNotEmpty(sb, "- other targets rules", indent + 3, assignment.getOtherTargetsPolicyRules());
        }
    }

    private static void dumpRulesIfNotEmpty(StringBuilder sb, String label, int indent, Collection<? extends EvaluatedPolicyRule> policyRules) {
        if (!policyRules.isEmpty()) {
            dumpRules(sb, label, indent, policyRules);
        }
    }

    static void dumpRules(StringBuilder sb, String label, int indent, Collection<? extends EvaluatedPolicyRule> policyRules) {
        sb.append("\n");
        int triggered = getTriggeredRulesCount(policyRules);
        DebugUtil.debugDumpLabel(sb, label + " (total " + policyRules.size() + ", triggered " + triggered + ")", indent);
        // not triggered rules are dumped in one line
        boolean first = true;
        for (EvaluatedPolicyRule rule : policyRules) {
            if (rule.isTriggered()) {
                continue;
            }
            if (first) {
                first = false;
                sb.append(" ");
            } else {
                sb.append("; ");
            }
            sb.append(rule.toShortString());
        }
        // now triggered rules, each on separate line
        for (EvaluatedPolicyRule rule : policyRules) {
            if (rule.isTriggered()) {
                sb.append("\n");
                DebugUtil.indentDebugDump(sb, indent + 1);
                sb.append("- triggered: ").append(rule.toShortString());
            }
        }
        if (policyRules.isEmpty()) {
            sb.append(" (none)");
        }
    }

    public static int getTriggeredRulesCount(Collection<? extends EvaluatedPolicyRule> policyRules) {
        return (int) policyRules.stream().filter(EvaluatedPolicyRule::isTriggered).count();
    }

    public LensContextType toLensContextType() throws SchemaException {
        return toLensContextType(ExportType.OPERATIONAL);
    }

    /**
     * 'reduced' means
     * - no full object values (focus, shadow).
     * <p>
     * This mode is to be used for re-starting operation after primary-stage approval (here all data are re-loaded; maybe
     * except for objectOld, but let's neglect it for the time being).
     * <p>
     * It is also to be used for the FINAL stage, where we need the context basically for information about executed deltas.
     */
    public LensContextType toLensContextType(ExportType exportType) throws SchemaException {

        PrismContainer<LensContextType> lensContextTypeContainer = PrismContainer
                .newInstance(getPrismContext(), LensContextType.COMPLEX_TYPE);
        LensContextType lensContextType = lensContextTypeContainer.createNewValue().asContainerable();

        lensContextType.setState(state != null ? state.toModelStateType() : null);
        if (exportType != ExportType.MINIMAL) {
            lensContextType.setRequestIdentifier(requestIdentifier);
            lensContextType.setChannel(channel);
        }

        if (focusContext != null) {
            lensContextType.setFocusContext(focusContext.toLensFocusContextType(prismContext, exportType));
        }

        PrismContainer<LensProjectionContextType> lensProjectionContextTypeContainer = lensContextTypeContainer
                .findOrCreateContainer(LensContextType.F_PROJECTION_CONTEXT);
        for (LensProjectionContext lensProjectionContext : projectionContexts) {
            lensProjectionContext.addToPrismContainer(lensProjectionContextTypeContainer, exportType);
        }
        lensContextType.setProjectionWave(projectionWave);
        lensContextType.setExecutionWave(executionWave);
        if (exportType != ExportType.MINIMAL) {
            lensContextType.setFocusClass(focusClass != null ? focusClass.getName() : null);
            lensContextType.setDoReconciliationForAllProjections(doReconciliationForAllProjections);
            lensContextType.setExecutionPhaseOnly(executionPhaseOnly);
            lensContextType.setOptions(options != null ? options.toModelExecutionOptionsType() : null);
            lensContextType.setLazyAuditRequest(lazyAuditRequest);
            lensContextType.setRequestAudited(requestAudited);
            lensContextType.setExecutionAudited(executionAudited);
            lensContextType.setRequestAuthorized(isRequestAuthorized);
            lensContextType.setStats(stats);
            lensContextType.setRequestMetadata(requestMetadata);
            lensContextType.setOwnerOid(ownerOid);

            for (LensObjectDeltaOperation<?> executedDelta : rottenExecutedDeltas) {
                lensContextType.getRottenExecutedDeltas()
                        .add(simplifyExecutedDelta(executedDelta).toLensObjectDeltaOperationType());
            }
        }

        return lensContextType;
    }

    static <T extends ObjectType> LensObjectDeltaOperation<T> simplifyExecutedDelta(LensObjectDeltaOperation<T> executedDelta) {
        LensObjectDeltaOperation<T> rv = executedDelta.clone();    // TODO something more optimized (no need to clone things deeply, just create new object with replaced operation result)
        rv.setExecutionResult(OperationResult.keepRootOnly(executedDelta.getExecutionResult()));
        return rv;
    }

    @SuppressWarnings({ "unchecked", "raw" })
    public static <T extends ObjectType> LensContext<T> fromLensContextType(LensContextType lensContextType, PrismContext prismContext,
            ProvisioningService provisioningService, Task task, OperationResult parentResult)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {

        OperationResult result = parentResult.createSubresult(DOT_CLASS + "fromLensContextType");

        String focusClassString = lensContextType.getFocusClass();

        if (StringUtils.isEmpty(focusClassString)) {
            throw new SystemException("Focus class is undefined in LensContextType");
        }

        LensContext<T> lensContext;
        try {
            lensContext = new LensContext<>((Class<T>) Class.forName(focusClassString), prismContext, provisioningService);
        } catch (ClassNotFoundException e) {
            throw new SystemException(
                    "Couldn't instantiate LensContext because focus or projection class couldn't be found",
                    e);
        }

        lensContext.setRequestIdentifier(lensContextType.getRequestIdentifier());
        lensContext.setState(ModelState.fromModelStateType(lensContextType.getState()));
        lensContext.setChannel(Channel.migrateUri(lensContextType.getChannel()));
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
        lensContext.setOwnerOid(lensContextType.getOwnerOid());

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

    private void fixProvisioningTypeInDelta(ObjectDelta delta, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        if (delta != null && delta.getObjectTypeClass() != null
                && (ShadowType.class.isAssignableFrom(delta.getObjectTypeClass())
                || ResourceType.class.isAssignableFrom(delta.getObjectTypeClass()))) {
            // TODO exception can be thrown here (MID-4391) e.g. if resource does not exist any more; consider what to do
            // Currently we are on the safe side by making whole conversion fail
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

    @NotNull
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
        for (Collection<Containerable> previewResults : getHookPreviewResultsMap().values()) {
            for (Containerable previewResult : CollectionUtils.emptyIfNull(previewResults)) {
                if (previewResult != null && clazz.isAssignableFrom(previewResult.getClass())) {
                    //noinspection unchecked
                    rv.add((T) previewResult);
                }
            }
        }
        return rv;
    }

    @Nullable
    @Override
    public PolicyRuleEnforcerPreviewOutputType getPolicyRuleEnforcerPreviewOutput() {
        return policyRuleEnforcerPreviewOutput;
    }

    public void setPolicyRuleEnforcerPreviewOutput(PolicyRuleEnforcerPreviewOutputType policyRuleEnforcerPreviewOutput) {
        this.policyRuleEnforcerPreviewOutput = policyRuleEnforcerPreviewOutput;
    }

    public int getConflictResolutionAttemptNumber() {
        return conflictResolutionAttemptNumber;
    }

    public void setConflictResolutionAttemptNumber(int conflictResolutionAttemptNumber) {
        this.conflictResolutionAttemptNumber = conflictResolutionAttemptNumber;
    }

    ConflictWatcher getFocusConflictWatcher() {
        return focusConflictWatcher;
    }

    ConflictWatcher createAndRegisterFocusConflictWatcher(@NotNull String oid, RepositoryService repositoryService) {
        if (focusConflictWatcher != null) {
            throw new IllegalStateException("Focus conflict watcher defined twice");
        }
        return focusConflictWatcher = repositoryService.createAndRegisterConflictWatcher(oid);
    }

    void unregisterConflictWatcher(RepositoryService repositoryService) {
        if (focusConflictWatcher != null) {
            repositoryService.unregisterConflictWatcher(focusConflictWatcher);
            focusConflictWatcher = null;
        }
    }

    public boolean hasProjectionChange() {
        for (LensProjectionContext projectionContext : getProjectionContexts()) {
            if (projectionContext.getWave() != getExecutionWave()) {
                continue;
            }
            if (!projectionContext.isCanProject()) {
                continue;
            }
            if (projectionContext.isCompleted()) {
                continue;
            }
            if (projectionContext.isTombstone()) {
                continue;
            }
            if (projectionContext.hasPrimaryDelta() || projectionContext.hasSecondaryDelta()) {
                return true;
            }
        }
        return false;
    }

    public SecurityPolicyType getGlobalSecurityPolicy() {
        return globalSecurityPolicy;
    }

    public void setGlobalSecurityPolicy(SecurityPolicyType globalSecurityPolicy) {
        this.globalSecurityPolicy = globalSecurityPolicy;
    }

    public String getOwnerOid() {
        return ownerOid;
    }

    public void setOwnerOid(String ownerOid) {
        this.ownerOid = ownerOid;
    }

    public PrismObject<UserType> getCachedOwner() {
        return cachedOwner;
    }

    public void setCachedOwner(PrismObject<UserType> cachedOwner) {
        this.cachedOwner = cachedOwner;
    }

    @Override
    public boolean isPreview() {
        return preview;
    }

    public void setPreview(boolean preview) {
        this.preview = preview;
    }

    /**
     * Finish all building activities and prepare context for regular use.
     * This should lock all values that should not be changed during recompute,
     * such as primary deltas.
     * This method is invoked by context factories when context build is finished.
     */
    public void finishBuild() {
        if (focusContext != null) {
            focusContext.finishBuild();
        }
        for (LensProjectionContext projectionContext : projectionContexts) {
            projectionContext.finishBuild();
        }
    }

    @NotNull
    public List<ObjectReferenceType> getOperationApprovedBy() {
        return operationApprovedBy;
    }

    @NotNull
    public List<String> getOperationApproverComments() {
        return operationApproverComments;
    }

    @Override
    @NotNull
    public ObjectTreeDeltas<F> getTreeDeltas() {
        ObjectTreeDeltas<F> objectTreeDeltas = new ObjectTreeDeltas<>(getPrismContext());
        if (getFocusContext() != null && getFocusContext().getPrimaryDelta() != null) {
            objectTreeDeltas.setFocusChange(getFocusContext().getPrimaryDelta().clone());
        }
        for (ModelProjectionContext projectionContext : getProjectionContexts()) {
            if (projectionContext.getPrimaryDelta() != null) {
                objectTreeDeltas.addProjectionChange(projectionContext.getResourceShadowDiscriminator(), projectionContext.getPrimaryDelta());
            }
        }
        return objectTreeDeltas;
    }

    /**
     * Expression profile to use for "privileged" operations, such as scripting hooks.
     */
    public ExpressionProfile getPrivilegedExpressionProfile() {
        // TODO: determine from system configuration.
        return MiscSchemaUtil.getExpressionProfile();
    }

    public ConstraintsCheckingStrategyType getFocusConstraintsCheckingStrategy() {
        PrismObject<SystemConfigurationType> systemConfiguration = getSystemConfiguration();
        if (systemConfiguration != null) {
            InternalsConfigurationType internals = systemConfiguration.asObjectable().getInternals();
            return internals != null ? internals.getFocusConstraintsChecking() : null;
        } else {
            return null;
        }
    }

    public ConstraintsCheckingStrategyType getProjectionConstraintsCheckingStrategy() {
        PrismObject<SystemConfigurationType> systemConfiguration = getSystemConfiguration();
        if (systemConfiguration != null) {
            InternalsConfigurationType internals = systemConfiguration.asObjectable().getInternals();
            return internals != null ? internals.getProjectionConstraintsChecking() : null;
        } else {
            return null;
        }
    }

    public String getOperationQualifier() {
        return getState() + ".e" + getExecutionWave() + "p" + getProjectionWave();
    }

    ObjectDeltaSchemaLevelUtil.NameResolver getNameResolver() {
        return (objectClass, oid) -> {
            if (ResourceType.class.equals(objectClass) || ShadowType.class.equals(objectClass)) {
                for (LensProjectionContext projectionContext : projectionContexts) {
                    PolyString name = projectionContext.resolveNameIfKnown(objectClass, oid);
                    if (name != null) {
                        return name;
                    }
                }
            } else {
                // We could consider resolving e.g. users or roles here, but if the were fetched, they will presumably
                // be in the local (or, for roles, even global) repository cache. However, we can reconsider this assumption
                // if needed.
            }
            return null;
        };
    }

    public void removeIgnoredContexts() {
        projectionContexts.removeIf(projCtx -> projCtx.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.IGNORE);
    }

    public boolean isInPrimary() {
        return state == ModelState.PRIMARY;
    }

    public boolean hasFocusOfType(Class<? extends ObjectType> type) {
        return focusContext != null && focusContext.isOfType(type);
    }

    public void resetDeltasAfterExecution() {
        if (focusContext != null) {
            focusContext.resetDeltasAfterExecution();
        }
        // nothing like this for projections (yet)
    }

    public boolean primaryFocusItemDeltaExists(ItemPath path) {
        return focusContext != null && focusContext.primaryItemDeltaExists(path);
    }

    public void deleteSecondaryDeltas() {
        if (focusContext != null) {
            focusContext.deleteSecondaryDeltas();
        }
        for (LensProjectionContext projectionContext : projectionContexts) {
            projectionContext.deleteSecondaryDeltas();
        }
    }

    public boolean isExperimentalCodeEnabled() {
        return systemConfiguration != null && systemConfiguration.asObjectable().getInternals() != null &&
                Boolean.TRUE.equals(systemConfiguration.asObjectable().getInternals().isEnableExperimentalCode());
    }

    public String getTaskTreeOid(Task task, OperationResult result) {
        if (taskTreeOid == null) {
            if (task instanceof RunningTask) {
                taskTreeOid = ((RunningTask) task).getRootTaskOid();
            }
        }
        return taskTreeOid;
    }

    public ObjectDeltaObject<F> getFocusOdoAbsolute() {
        return focusContext != null ? focusContext.getObjectDeltaObjectAbsolute() : null;
    }

    /**
     * Checks if there was anything (at least partially) executed.
     * <p>
     * Currently we can only look at executed deltas and check whether there is something relevant
     * (i.e. not FATAL_ERROR nor NOT_APPLICABLE).
     * <p>
     * Any solution based on operation result status will never be 100% accurate, e.g. because
     * a network timeout could occur just before returning a status value. So please use with care.
     */
    @Experimental
    public boolean wasAnythingExecuted() {
        if (focusContext != null && focusContext.wasAnythingReallyExecuted()) {
            return true;
        }
        if (hasRottenReallyExecutedDelta()) {
            return true;
        }
        for (LensProjectionContext projectionContext : projectionContexts) {
            if (projectionContext.wasAnythingReallyExecuted()) {
                return true;
            }
        }
        return false;
    }

    @Experimental
    private boolean hasRottenReallyExecutedDelta() {
        return rottenExecutedDeltas.stream().anyMatch(ObjectDeltaOperation::wasReallyExecuted);
    }
}
