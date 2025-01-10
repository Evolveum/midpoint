/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import static com.evolveum.midpoint.model.impl.lens.LensFocusContext.fromLensFocusContextBean;
import static com.evolveum.midpoint.model.impl.lens.LensProjectionContext.fromLensProjectionContextBean;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.util.MappingInspector;

import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ObjectTreeDeltas;
import com.evolveum.midpoint.model.api.ProgressInformation;
import com.evolveum.midpoint.model.api.ProgressListener;
import com.evolveum.midpoint.model.api.context.*;
import com.evolveum.midpoint.model.api.util.ClockworkInspector;
import com.evolveum.midpoint.model.common.expression.ModelExpressionEnvironment;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.expr.SpringApplicationContextHolder;
import com.evolveum.midpoint.model.impl.lens.assignments.EvaluatedAssignmentImpl;
import com.evolveum.midpoint.model.impl.lens.construction.ConstructionTargetKey;
import com.evolveum.midpoint.model.impl.lens.projector.AssignmentOrigin;
import com.evolveum.midpoint.model.impl.lens.projector.loader.ProjectionsLoadOperation;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationContext;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.ConflictWatcher;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.TaskExecutionMode;
import com.evolveum.midpoint.schema.constants.Channel;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectDeltaSchemaLevelUtil;
import com.evolveum.midpoint.schema.util.SystemConfigurationTypeUtil;
import com.evolveum.midpoint.task.api.RunningTask;
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

    @Serial private static final long serialVersionUID = -778283437426659540L;
    private static final String DOT_CLASS = LensContext.class.getName() + ".";

    private static final Trace LOGGER = TraceManager.getTrace(LensContext.class);

    private static final int DEFAULT_MAX_CLICKS = 200;

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

    /**
     * Current state of the clockwork. Null means that the clockwork has not started yet.
     */
    private ModelState state;

    private transient ConflictWatcher focusConflictWatcher;

    private int conflictResolutionAttemptNumber;

    // For use with personas
    private String ownerOid;

    private transient PrismObject<UserType> cachedOwner;

    /** Focus template explicitly set from the outside, typically from the synchronization policy. */
    private String explicitFocusTemplateOid;

    private transient SecurityPolicyType globalSecurityPolicy;

    /**
     * Channel that is the source of primary change (GUI, live sync, import, ...)
     */
    private String channel;

    /** TEMPORARY. TODO. */
    @NotNull private final TaskExecutionMode taskExecutionMode;

    private LensFocusContext<F> focusContext;

    /**
     * Projection contexts. No null elements are allowed here.
     */
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
    private transient Collection<ProjectionContextKey> historicResourceObjects;

    private Class<F> focusClass;

    private boolean lazyAuditRequest = false; // should be the request audited just before the execution is audited?
    private boolean requestAudited = false; // was the request audited?
    private boolean executionAudited = false; // was the execution audited?
    private LensContextStatsType stats = new LensContextStatsType();

    /** Metadata of the request. Metadata recorded when the operation has started. */
    private RequestMetadata requestMetadata;

    /**
     * Executed deltas from rotten projection contexts.
     */
    @NotNull private final List<LensObjectDeltaOperation<?>> rottenExecutedDeltas = new ArrayList<>();

    private transient ProjectionPolicyType accountSynchronizationSettings;

    /**
     * Delta set triple (plus, minus, zero) for evaluated assignments.
     * Relativity is determined by looking at current delta (i.e. objectCurrent -> objectNew).
     *
     * If you want to know whether the assignment was added or deleted with regards to objectOld,
     * please check evaluatedAssignment.origin property - {@link AssignmentOrigin}.
     *
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

    /**
     * Resource that is triggering the current operation. This means that we are processing an object coming
     * from that resource in a synchronization operation (live sync, import, recon, and so on).
     *
     * May be null if `limitPropagation` option is not set.
     */
    @Nullable private String triggeringResourceOid;

    /**
     * At this level, isFresh == false means that deeper recomputation has to be carried out.
     *
     * @see ElementState#fresh
     */
    private transient boolean isFresh = false;

    @NotNull private AuthorizationState authorizationState = AuthorizationState.NONE;

    /**
     * Cache of resource instances. It is used to reduce the number of read
     * (getObject) calls for ResourceType objects.
     */
    private transient Map<String, ResourceType> resourceCache;

    private ModelExecuteOptions options;

    /**
     * Used mostly in unit tests.
     */
    private transient ClockworkInspector inspector;

    /**
     * User feedback.
     */
    private transient Collection<ProgressListener> progressListeners;

    /**
     * Current values of sequences used during the clockwork.
     */
    private final Map<String, Long> sequences = new HashMap<>();

    /**
     * Moved from ProjectionValuesProcessor TODO consider if necessary to
     * serialize to XML
     */
    @NotNull private final List<LensProjectionContext> conflictingProjectionContexts = new ArrayList<>();

    private transient Map<String, Collection<? extends Containerable>> hookPreviewResultsMap;

    private transient PolicyRuleEnforcerPreviewOutputType policyRuleEnforcerPreviewOutput;

    @NotNull private transient final List<ObjectReferenceType> operationApprovedBy = new ArrayList<>();
    @NotNull private transient final List<String> operationApproverComments = new ArrayList<>();

    private String taskTreeOid;

    /**
     * How many times the clockwork clicked? Used to detect endless loops.
     */
    private int clickCounter;

    /**
     * Limit for clicks.
     */
    private int clickLimit;

    private transient ModelBeans modelBeans;

    public LensContext(@NotNull TaskExecutionMode taskExecutionMode) {
        this(null, taskExecutionMode);
    }

    public LensContext(Class<F> focusClass, @NotNull TaskExecutionMode taskExecutionMode) {
        this.focusClass = focusClass;
        this.taskExecutionMode = taskExecutionMode;
    }

    /**
     * TODO not sure if this method should be here or in {@link ProjectionsLoadOperation} (that is currently the only client)
     *  The reason for being here is the similarity to {@link #getOrCreateProjectionContext(LensContext, ConstructionTargetKey, boolean)}
     *  and coupling with {@link #getOrCreateProjectionContext(LensContext, HumanReadableDescribable, Supplier, Supplier)}.
     */
    public @NotNull
    static <F extends ObjectType> GetOrCreateProjectionContextResult getOrCreateProjectionContext(
            LensContext<F> context, ProjectionContextKey contextKey) {
        return getOrCreateProjectionContext(
                context,
                contextKey,
                () -> context.findProjectionContextByKeyExact(contextKey),
                () -> context.createProjectionContext(contextKey));
    }

    public @NotNull
    static <F extends ObjectType> GetOrCreateProjectionContextResult getOrCreateProjectionContext(
            LensContext<F> context, ConstructionTargetKey targetKey, boolean acceptReaping) {
        return getOrCreateProjectionContext(
                context,
                targetKey,
                () -> context.findFirstProjectionContext(targetKey, acceptReaping),
                () -> context.createProjectionContext(targetKey.toProjectionContextKey()));
    }

    private @NotNull
    static <F extends ObjectType, K extends HumanReadableDescribable>
    GetOrCreateProjectionContextResult getOrCreateProjectionContext(
            LensContext<F> context,
            K key,
            Supplier<LensProjectionContext> finder,
            Supplier<LensProjectionContext> creator) {
        LensProjectionContext existingCtx = finder.get();
        if (existingCtx != null) {
            LOGGER.trace("Found existing context for {}: {}", key.toHumanReadableDescription(), existingCtx);
            existingCtx.setDoReconciliation(
                    context.isDoReconciliationForAllProjections()); // TODO this should be somehow automatic
            return new GetOrCreateProjectionContextResult(existingCtx, false);
        } else {
            LensProjectionContext newCtx = creator.get();
            LOGGER.trace("Created new projection context for {}: {}", key.toHumanReadableDescription(), newCtx);
            newCtx.setDoReconciliation(
                    context.isDoReconciliationForAllProjections()); // TODO this should be somehow automatic
            return new GetOrCreateProjectionContextResult(newCtx, true);
        }
    }

    /**
     * Returns contexts that have equivalent key with the reference context. Ordered by "order" in the key.
     */
    List<LensProjectionContext> findRelatedContexts(LensProjectionContext refProjCtx) {
        Comparator<LensProjectionContext> orderComparator = (ctx1, ctx2) -> {
            int order1 = ctx1.getKey().getOrder();
            int order2 = ctx2.getKey().getOrder();
            return Integer.compare(order1, order2);
        };
        return projectionContexts.stream()
                .filter(aProjCtx -> refProjCtx.getKey().equivalent(aProjCtx.getKey()))
                .sorted(orderComparator)
                .collect(Collectors.toList());
    }

    boolean hasLowerOrderContextThan(LensProjectionContext refProjCtx) {
        ProjectionContextKey refKey = refProjCtx.getKey();
        for (LensProjectionContext aProjCtx : projectionContexts) {
            ProjectionContextKey aKey = aProjCtx.getKey();
            if (refKey.equivalent(aKey) && (refKey.getOrder() > aKey.getOrder())) {
                return true;
            }
        }
        return false;
    }

    public LensProjectionContext findLowerOrderContext(LensProjectionContext refProjCtx) {
        int minOrder = -1;
        LensProjectionContext foundCtx = null;
        ProjectionContextKey refKey = refProjCtx.getKey();
        for (LensProjectionContext aProjCtx : projectionContexts) {
            ProjectionContextKey aKey = aProjCtx.getKey();
            if (refKey.equivalent(aKey) && aKey.getOrder() < refKey.getOrder()) {
                if (minOrder < 0 || aKey.getOrder() < minOrder) {
                    minOrder = aKey.getOrder();
                    foundCtx = aProjCtx;
                }
            }
        }
        return foundCtx;
    }

    public ProvisioningService getProvisioningService() {
        return ModelBeans.get().provisioningService;
    }

    public void setTriggeringResourceOid(@NotNull ResourceType triggeringResource) {
        this.triggeringResourceOid = triggeringResource.getOid();
    }

    public @Nullable String getTriggeringResourceOid() {
        return triggeringResourceOid;
    }

    @Override
    public String getRequestIdentifier() {
        return requestIdentifier;
    }

    private void setRequestIdentifier(String requestIdentifier) {
        this.requestIdentifier = requestIdentifier;
    }

    public String getItemProcessingIdentifier() {
        return itemProcessingIdentifier;
    }

    public void setItemProcessingIdentifier(String itemProcessingIdentifier) {
        this.itemProcessingIdentifier = itemProcessingIdentifier;
    }

    void generateRequestIdentifierIfNeeded() {
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

    LensFocusContext<F> createFocusContext(Class<F> explicitFocusClass) {
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
    @NotNull
    public Collection<LensProjectionContext> getProjectionContexts() {
        return projectionContexts;
    }

    public Iterator<LensProjectionContext> getProjectionContextsIterator() {
        return projectionContexts.iterator();
    }

    public void addProjectionContext(@NotNull LensProjectionContext projectionContext) {
        projectionContexts.add(projectionContext);
    }

    public void replaceProjectionContexts(Collection<LensProjectionContext> projectionContexts) {
        this.projectionContexts.clear();
        this.projectionContexts.addAll(projectionContexts);
    }

    /**
     * BEWARE!
     *
     * Before you call this method, please consider the fact that there may be more projection contexts
     * matching given shadow OID.
     */
    public LensProjectionContext findProjectionContextByOid(@NotNull String oid) {
        return projectionContexts.stream()
                .filter(ctx -> oid.equals(ctx.getOid()))
                .findFirst()
                .orElse(null);
    }

    public @NotNull List<LensProjectionContext> findProjectionContextsByOid(@NotNull String oid) {
        return projectionContexts.stream()
                .filter(ctx -> oid.equals(ctx.getOid()))
                .collect(Collectors.toList());
    }

    /** TODO describe this and verify it's OK */
    public LensProjectionContext findProjectionContextByOidAndKey(@NotNull String oid, @NotNull ProjectionContextKey key) {
        return projectionContexts.stream()
                .filter(ctx -> oid.equals(ctx.getOid()))
                .filter(ctx -> ctx.matches(key, false))
                .findFirst()
                .orElse(null);
    }

    // TODO what about unclassified cases (existing/new)?
    public LensProjectionContext findProjectionContextByKeyExact(@NotNull ProjectionContextKey key) {
        Validate.notNull(key);
        for (LensProjectionContext projCtx : projectionContexts) {
            if (projCtx.matches(key, true)) {
                return projCtx;
            }
        }
        return null;
    }

    @Override
    public @NotNull Collection<LensProjectionContext> findProjectionContexts(@NotNull ProjectionContextFilter filter) {
        return projectionContexts.stream()
                .filter(ctx -> filter.matches(ctx.getKey()))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("WeakerAccess")
    @VisibleForTesting
    public @Nullable LensProjectionContext findProjectionContext(@NotNull ProjectionContextFilter filter) {
        Collection<LensProjectionContext> matching = findProjectionContexts(filter);
        return MiscUtil.extractSingleton(
                matching,
                () -> new IllegalStateException("Multiple projection contexts matching " + filter + ": " + matching));
    }

    /**
     * TODO TODO TODO Do not use this method (yet)
     *
     * Returns the projection context this construction should be applied to.
     *
     * There may be more matching ones. We return the one that is the next one to be evaluated (according to the waves/ordering).
     *
     * TODO What is more reliable - ordering or waves? Waves may not be computed, while order should be specified. We hope...
     */
    public LensProjectionContext findFirstNotCompletedProjectionContext(@NotNull ConstructionTargetKey targetKey) {
        return getProjectionContexts().stream()
                .filter(p -> !p.isGone())
                .filter(p -> p.matches(targetKey))
                .filter(p -> !p.isCompleted())
                .min(Comparator.comparing(LensProjectionContext::getOrder))
                .orElse(null);
    }

    /**
     * TODO
     */
    public LensProjectionContext findFirstProjectionContext(
            @NotNull ConstructionTargetKey targetKey,
            boolean acceptReaping) {
        return getProjectionContexts().stream()
                .filter(p -> !p.isGone())
                .filter(p -> p.matches(targetKey))
                .filter(p -> !p.isReaping() || acceptReaping)
                .min(Comparator.comparing(LensProjectionContext::getOrder))
                .orElse(null);
    }

    public String getExplicitFocusTemplateOid() {
        return explicitFocusTemplateOid;
    }

    public void setExplicitFocusTemplateOid(String explicitFocusTemplateOid) {
        this.explicitFocusTemplateOid = explicitFocusTemplateOid;
    }

    @Override
    public ObjectTemplateType getFocusTemplate() {
        return focusContext != null ? focusContext.getFocusTemplate() : null;
    }

    public PrismObject<SystemConfigurationType> getSystemConfiguration() {
        return systemConfiguration;
    }

    public SystemConfigurationType getSystemConfigurationBean() {
        return systemConfiguration != null ? systemConfiguration.asObjectable() : null;
    }

    public InternalsConfigurationType getInternalsConfiguration() {
        return systemConfiguration != null ? systemConfiguration.asObjectable().getInternals() : null;
    }

    public void setSystemConfiguration(PrismObject<SystemConfigurationType> systemConfiguration) {
        this.systemConfiguration = systemConfiguration;
    }

    public void updateSystemConfiguration(OperationResult result) throws SchemaException {
        PrismObject<SystemConfigurationType> systemConfiguration =
                ModelBeans.get().systemObjectCache.getSystemConfiguration(result);
        if (systemConfiguration != null) {
            setSystemConfiguration(systemConfiguration);
        }
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

    private void setExecutionWave(int executionWave) {
        this.executionWave = executionWave;
    }

    void incrementExecutionWave() {
        executionWave++;
        LOGGER.trace("Incrementing lens context execution wave to {}", executionWave);
    }

    int getMaxWave() {
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

    @SuppressWarnings("WeakerAccess") // may be used from scripts
    public boolean isRequestAuthorized() {
        return getAuthorizationState() == AuthorizationState.FULL;
    }

    public @NotNull AuthorizationState getAuthorizationState() {
        return Objects.requireNonNull(authorizationState);
    }

    void setRequestAuthorized(@NotNull AuthorizationState authorizationState) {
        this.authorizationState = authorizationState;
    }

    /**
     * Makes the context and all sub-context non-fresh.
     */
    public void rot(String reason) {
        if (!taskExecutionMode.isFullyPersistent()) {
            LOGGER.trace("Not rotting the context ({}) because we are not in persistent execution mode", reason);
            return;
        }
        LOGGER.debug("Rotting context because of {}", reason);
        if (focusContext != null) {
            focusContext.rot();
        }
        for (LensProjectionContext projectionContext : projectionContexts) {
            projectionContext.rot();
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

    public boolean isExecutionPhaseOnly() {
        return executionPhaseOnly;
    }

    void setExecutionPhaseOnly(boolean executionPhaseOnly) {
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
            return evaluatedAssignmentTriple.getNonNegativeValues(); // MID-6403
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

    RequestMetadata getRequestMetadata() {
        return requestMetadata;
    }

    private void setRequestMetadata(RequestMetadata requestMetadata) {
        this.requestMetadata = requestMetadata;
    }

    /**
     * Stores request metadata in the model context. Because the operation can finish later
     * (if switched to background), we need to have these data recorded at the beginning.
     */
    void initializeRequestMetadata(XMLGregorianCalendar now, Task task) {
        setRequestMetadata(
                RequestMetadata.create(now, task));
    }

    public ClockworkInspector getInspector() {
        return inspector;
    }

    public @NotNull MappingInspector getMappingInspector() {
        return Objects.requireNonNullElse(inspector, MappingInspector.empty());
    }

    public void setInspector(ClockworkInspector inspector) {
        this.inspector = inspector;
    }

    /**
     * If set to true then the request will be audited right before execution.
     * If no execution takes place then no request will be audited.
     */
    boolean isLazyAuditRequest() {
        return lazyAuditRequest;
    }

    public void setLazyAuditRequest(boolean lazyAuditRequest) {
        this.lazyAuditRequest = lazyAuditRequest;
    }

    @SuppressWarnings("WeakerAccess") // may be used from scripts
    public boolean isRequestAudited() {
        return requestAudited;
    }

    void setRequestAudited(boolean requestAudited) {
        this.requestAudited = requestAudited;
    }

    @SuppressWarnings("WeakerAccess") // may be used from scripts
    public boolean isExecutionAudited() {
        return executionAudited;
    }

    void setExecutionAudited(boolean executionAudited) {
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

    @SuppressWarnings("WeakerAccess") // may be used from scripts
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
    @SuppressWarnings("WeakerAccess") // may be used from scripts
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

    void markExecutedDeltasAudited() {
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

    public void refreshAuxiliaryObjectClassDefinitions() throws SchemaException, ConfigurationException {
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
            projectionContext.checkConsistence(this.toString(), isFresh, isForce());
        }
    }

    boolean isForce() {
        return ModelExecuteOptions.isForce(options);
    }

    void checkEncryptedIfNeeded() {
        if (InternalsConfig.encryptionChecks && !ModelExecuteOptions.isNoCrypt(options)) {
            checkEncrypted();
        }
    }

    private void checkEncrypted() {
        if (focusContext != null && !focusContext.isDelete()) {
            focusContext.checkEncrypted();
        }
        for (LensProjectionContext projectionContext : projectionContexts) {
            if (!projectionContext.isDelete()) {
                projectionContext.checkEncrypted();
            }
        }
    }

    public LensProjectionContext createProjectionContext(@NotNull ProjectionContextKey key) {
        LensProjectionContext projCtx = createDetachedProjectionContext(key);
        if (key.getResourceOid() != null) {
            projCtx.setResource(
                    getResource(key.getResourceOid()));
        }
        addProjectionContext(projCtx);
        return projCtx;
    }

    public LensProjectionContext createDetachedProjectionContext(@NotNull ProjectionContextKey key) {
        return new LensProjectionContext(this, key);
    }

    private Map<String, ResourceType> getResourceCache() {
        if (resourceCache == null) {
            resourceCache = new HashMap<>();
        }
        return resourceCache;
    }

    /**
     * Returns a resource with given OID. This is supposed to be efficient, taking the resource from the cache.
     * It assumes the resource is in the cache.
     *
     * @see LensContext#rememberResource(ResourceType)
     */
    public ResourceType getResource(String resourceOid) {
        return getResourceCache().get(resourceOid);
    }

    /**
     * Puts resource in the cache for later use. The resource should be fetched
     * from provisioning and have pre-parsed schemas. So the next time just
     * reuse it without the other overhead.
     */
    public void rememberResource(ResourceType resource) {
        getResourceCache().put(resource.getOid(), resource);
    }

    /**
     * Cleans up the contexts by removing some of the working state. The current
     * wave number is retained. Otherwise it ends up in endless loop.
     */
    public void cleanup() throws SchemaException {
        if (focusContext != null) {
            focusContext.cleanup(); // currently no-op
        }
        for (LensProjectionContext projectionContext : projectionContexts) {
            projectionContext.cleanup();
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
        LensContext<F> clone = new LensContext<>(focusClass, taskExecutionMode);
        copyValues(clone);
        return clone;
    }

    private void copyValues(LensContext<F> clone) {
        clone.state = this.state;
        clone.channel = this.channel;
        clone.doReconciliationForAllProjections = this.doReconciliationForAllProjections;
        clone.executionPhaseOnly = this.executionPhaseOnly;
        clone.focusClass = this.focusClass;
        clone.isFresh = this.isFresh;
        clone.authorizationState = this.authorizationState;
        clone.resourceCache = resourceCache != null ?
                new HashMap<>(resourceCache) : null;
        clone.explicitFocusTemplateOid = this.explicitFocusTemplateOid;
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
        clone.sequences.putAll(this.sequences);
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
        sb.append(", reqAutz=").append(authorizationState);
        if (systemConfiguration == null) {
            sb.append(" null-system-configuration");
        }
        if (executionPhaseOnly) {
            sb.append(" execution-phase-only");
        }
        sb.append(requestMetadata != null ? ", req. metadata present" : ", req. metadata missing");
        sb.append("\n");

        DebugUtil.debugDumpWithLabelLn(sb, "Channel", channel, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "Do reconciliation for all projections", doReconciliationForAllProjections, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "Options", String.valueOf(options), indent + 1);
        DebugUtil.debugDumpLabel(sb, "Settings", indent + 1);
        sb.append(" ");
        if (accountSynchronizationSettings != null) {
            sb.append("assignments=");
            sb.append(accountSynchronizationSettings.getAssignmentPolicyEnforcement());
        } else {
            sb.append("null");
        }
        sb.append("\n");
        DebugUtil.debugDumpWithLabelLn(sb, "Explicit focus template OID", explicitFocusTemplateOid, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "FOCUS", focusContext, indent + 1);
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
        if (!sequences.isEmpty()) {
            sb.append("\n");
            DebugUtil.debugDumpWithLabel(sb, "Sequence values", sequences, indent + 1);
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
            dumpPolicyRulesCollection(
                    "targetsPolicyRules", indent + 1, sb, assignment.getAllTargetsPolicyRules(), alsoMessages);
            dumpPolicyRulesCollection(
                    "foreignPolicyRules", indent + 1, sb, assignment.getForeignPolicyRules(), alsoMessages);
            dumpPolicyRulesCollection(
                    "objectPolicyRules", indent + 1, sb, assignment.getObjectPolicyRules(), alsoMessages);
        }, 1);
        return sb.toString();
    }

    @Override
    public String dumpObjectPolicyRules(int indent, boolean alsoMessages) {
        StringBuilder sb = new StringBuilder();
        if (focusContext != null) {
            dumpPolicyRulesCollection("objectPolicyRules", indent, sb, focusContext.getObjectPolicyRules(), alsoMessages);
        }
        return sb.toString();
    }

    private void dumpPolicyRulesCollection(
            String label, int indent, StringBuilder sb, Collection<? extends AssociatedPolicyRule> rules, boolean alsoMessages) {
        sb.append("\n");
        DebugUtil.indentDebugDump(sb, indent);
        sb.append(label).append(" (").append(rules.size()).append("):");
        for (AssociatedPolicyRule rule : rules) {
            sb.append("\n");
            dumpPolicyRule(indent, sb, rule, alsoMessages);
        }
    }

    private void dumpPolicyRule(
            int indent, StringBuilder sb, AssociatedPolicyRule rule, boolean alsoMessages) {
        if (alsoMessages) {
            sb.append("=============================================== RULE ===============================================\n");
        }
        DebugUtil.indentDebugDump(sb, indent + 1);
        if (rule.getNewOwner() != null) {
            sb.append(rule.getNewOwnerShortString()).append(" ");
        }
        EvaluatedPolicyRule evaluatedRule = rule.getEvaluatedPolicyRule();
        if (evaluatedRule.isGlobal()) {
            sb.append("global ");
        }
        sb.append("rule: ").append(evaluatedRule.toShortString());
        dumpTriggersCollection(indent + 2, sb, evaluatedRule.getTriggers());
        if (alsoMessages) {
            if (evaluatedRule.isTriggered()) {
                sb.append("\n\n");
                sb.append("--------------------------------------------- MESSAGES ---------------------------------------------");
                List<TreeNode<LocalizableMessage>> messageTrees = evaluatedRule.extractMessages();
                for (TreeNode<LocalizableMessage> messageTree : messageTrees) {
                    sb.append("\n");
                    sb.append(messageTree.debugDump(indent));
                }
            }
            sb.append("\n");
        }
    }

    private void dumpTriggersCollection(int indent, StringBuilder sb, Collection<EvaluatedPolicyRuleTrigger<?>> triggers) {
        for (EvaluatedPolicyRuleTrigger<?> trigger : triggers) {
            sb.append("\n");
            DebugUtil.indentDebugDump(sb, indent);
            sb.append("trigger: ").append(trigger);
            if (trigger instanceof EvaluatedExclusionTrigger) {
                sb.append("\n");
                DebugUtil.indentDebugDump(sb, indent + 1);
                sb.append("conflict: ");
                sb.append(
                        ((EvaluatedExclusionTrigger) trigger).getConflictingAssignment()
                                .toHumanReadableString());
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
            dumpRulesIfNotEmpty(sb, "- focus rules", indent + 3, assignment.getObjectPolicyRules());
            dumpRulesIfNotEmpty(sb, "- this target rules", indent + 3, assignment.getThisTargetPolicyRules());
            dumpRulesIfNotEmpty(sb, "- other targets rules", indent + 3, assignment.getOtherTargetsPolicyRules());
            dumpRulesIfNotEmpty(sb, "- foreign rules", indent + 3, assignment.getForeignPolicyRules());
        }
    }

    private static void dumpRulesIfNotEmpty(
            StringBuilder sb, String label, int indent, Collection<? extends AssociatedPolicyRule> policyRules) {
        if (!policyRules.isEmpty()) {
            dumpRules(sb, label, indent, policyRules);
        }
    }

    static void dumpRules(StringBuilder sb, String label, int indent, Collection<? extends AssociatedPolicyRule> policyRules) {
        sb.append("\n");
        int triggered = AssociatedPolicyRule.getTriggeredRulesCount(policyRules);
        DebugUtil.debugDumpLabel(sb, label + " (total " + policyRules.size() + ", triggered " + triggered + ")", indent);
        // not triggered rules are dumped in one line
        boolean first = true;
        for (AssociatedPolicyRule rule : policyRules) {
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
        for (AssociatedPolicyRule rule : policyRules) {
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

    public LensContextType toBean() throws SchemaException {
        return toBean(ExportType.OPERATIONAL);
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
    public LensContextType toBean(ExportType exportType) throws SchemaException {

        LensContextType bean = new LensContextType();

        bean.setState(state != null ? state.toModelStateType() : null);
        if (exportType != ExportType.MINIMAL) {
            bean.setRequestIdentifier(requestIdentifier);
            bean.setChannel(channel);
        }

        if (focusContext != null) {
            bean.setFocusContext(focusContext.toBean(exportType));
        }

        for (LensProjectionContext lensProjectionContext : projectionContexts) {
            bean.getProjectionContext().add(
                    lensProjectionContext.toBean(exportType));
        }
        bean.setProjectionWave(projectionWave);
        bean.setExecutionWave(executionWave);
        if (exportType != ExportType.MINIMAL) {
            bean.setFocusClass(focusClass != null ? focusClass.getName() : null);
            bean.setDoReconciliationForAllProjections(doReconciliationForAllProjections);
            bean.setExecutionPhaseOnly(executionPhaseOnly);
            bean.setOptions(options != null ? options.toModelExecutionOptionsType() : null);
            bean.setLazyAuditRequest(lazyAuditRequest);
            bean.setRequestAudited(requestAudited);
            bean.setExecutionAudited(executionAudited);
            bean.setRequestAuthorized(isRequestAuthorized());
            bean.setStats(stats);
            if (requestMetadata != null) {
                bean.setRequestMetadata(requestMetadata.toBean());
            }
            bean.setOwnerOid(ownerOid);

            for (LensObjectDeltaOperation<?> executedDelta : rottenExecutedDeltas) {
                bean.getRottenExecutedDeltas()
                        .add(simplifyExecutedDelta(executedDelta).toLensObjectDeltaOperationBean());
            }
        }
        if (!getSequences().isEmpty()) {
            LensContextSequencesType sBean = new LensContextSequencesType();
            for (Entry<String, Long> entry : getSequences().entrySet()) {
                sBean.getSequenceValue().add(
                        new LensContextSequenceValueType()
                                .sequenceRef(entry.getKey(), SequenceType.COMPLEX_TYPE)
                                .value(entry.getValue()));
            }
            bean.setSequences(sBean);
        }

        return bean;
    }

    static <T extends ObjectType> LensObjectDeltaOperation<T> simplifyExecutedDelta(LensObjectDeltaOperation<T> executedDelta) {
        LensObjectDeltaOperation<T> rv = executedDelta.clone();    // TODO something more optimized (no need to clone things deeply, just create new object with replaced operation result)
        rv.setExecutionResult(OperationResult.keepRootOnly(executedDelta.getExecutionResult()));
        return rv;
    }

    @SuppressWarnings({ "unchecked", "raw" })
    public static <T extends ObjectType> LensContext<T> fromLensContextBean(LensContextType bean, Task task,
            OperationResult parentResult)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            ExpressionEvaluationException {

        OperationResult result = parentResult.createSubresult(DOT_CLASS + "fromLensContextType");

        String focusClassString = bean.getFocusClass();

        if (StringUtils.isEmpty(focusClassString)) {
            throw new SystemException("Focus class is undefined in LensContextType");
        }

        LensContext<T> lensContext;
        try {
            lensContext = new LensContext<>((Class<T>) Class.forName(focusClassString), task.getExecutionMode());
        } catch (ClassNotFoundException e) {
            throw new SystemException(
                    "Couldn't instantiate LensContext because focus or projection class couldn't be found",
                    e);
        }

        lensContext.setRequestIdentifier(bean.getRequestIdentifier());
        lensContext.setState(ModelState.fromModelStateType(bean.getState()));
        lensContext.setChannel(Channel.migrateUri(bean.getChannel()));
        lensContext.setFocusContext(
                fromLensFocusContextBean(bean.getFocusContext(), lensContext, task, result));
        for (LensProjectionContextType lensProjectionContextType : bean.getProjectionContext()) {
            lensContext.addProjectionContext(
                    fromLensProjectionContextBean(lensProjectionContextType, lensContext, task, result));
        }
        lensContext.setDoReconciliationForAllProjections(
                bean.isDoReconciliationForAllProjections() != null ?
                        bean.isDoReconciliationForAllProjections() : false);
        lensContext.setExecutionPhaseOnly(
                bean.isExecutionPhaseOnly() != null ?
                        bean.isExecutionPhaseOnly() : false);
        lensContext.setProjectionWave(
                bean.getProjectionWave() != null ? bean.getProjectionWave() : 0);
        lensContext.setExecutionWave(
                bean.getExecutionWave() != null ? bean.getExecutionWave() : 0);
        lensContext
                .setOptions(ModelExecuteOptions.fromModelExecutionOptionsType(bean.getOptions()));
        if (bean.isLazyAuditRequest() != null) {
            lensContext.setLazyAuditRequest(bean.isLazyAuditRequest());
        }
        if (bean.isRequestAudited() != null) {
            lensContext.setRequestAudited(bean.isRequestAudited());
        }
        if (bean.isExecutionAudited() != null) {
            lensContext.setExecutionAudited(bean.isExecutionAudited());
        }
        // Let us consider the "request authorized" as "request fully authorized".
        lensContext.setRequestAuthorized(
                Boolean.TRUE.equals(bean.isRequestAuthorized()) ? AuthorizationState.FULL : AuthorizationState.NONE);
        lensContext.setStats(bean.getStats());
        lensContext.setRequestMetadata(RequestMetadata.fromBean(bean.getRequestMetadata()));
        lensContext.setOwnerOid(bean.getOwnerOid());

        for (LensObjectDeltaOperationType eDeltaOperationType : bean.getRottenExecutedDeltas()) {
            LensObjectDeltaOperation<?> objectDeltaOperation =
                    LensObjectDeltaOperation.fromLensObjectDeltaOperationType(eDeltaOperationType);
            if (objectDeltaOperation.getObjectDelta() != null) {
                lensContext.fixProvisioningTypeInDelta(objectDeltaOperation.getObjectDelta(), task, result);
            }
            lensContext.rottenExecutedDeltas.add(objectDeltaOperation);
        }

        if (bean.getSequences() != null) {
            for (LensContextSequenceValueType seqValueBean : bean.getSequences().getSequenceValue()) {
                String oid = seqValueBean.getSequenceRef() != null ? seqValueBean.getSequenceRef().getOid() : null;
                if (oid != null) {
                    lensContext.setSequenceCounter(oid, seqValueBean.getValue());
                }
            }
        }

        if (result.isUnknown()) {
            result.computeStatus();
        }
        return lensContext;
    }

    private void fixProvisioningTypeInDelta(ObjectDelta<? extends ObjectType> delta, Task task, OperationResult result)
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

    Collection<ProgressListener> getProgressListeners() {
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

    private boolean isAbortRequested() {
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

    public Collection<ProjectionContextKey> getHistoricResourceObjects() {
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

    public void addConflictingProjectionContext(@NotNull LensProjectionContext conflictingContext) {
        conflictingProjectionContexts.add(conflictingContext);
    }

    public void clearConflictingProjectionContexts() {
        conflictingProjectionContexts.clear();
    }

    public boolean hasExplosiveProjection() throws SchemaException, ConfigurationException {
        for (LensProjectionContext projectionContext : projectionContexts) {
            if (projectionContext.getVolatility() == ResourceObjectVolatilityType.EXPLOSIVE) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    public Map<String, Collection<? extends Containerable>> getHookPreviewResultsMap() {
        if (hookPreviewResultsMap == null) {
            hookPreviewResultsMap = new HashMap<>();
        }
        return hookPreviewResultsMap;
    }

    public void addHookPreviewResults(String hookUri, Collection<? extends Containerable> results) {
        getHookPreviewResultsMap().put(hookUri, results);
    }

    @NotNull
    @Override
    public <T> List<T> getHookPreviewResults(@NotNull Class<T> clazz) {
        List<T> rv = new ArrayList<>();
        for (Collection<? extends Containerable> previewResults : getHookPreviewResultsMap().values()) {
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

    int getConflictResolutionAttemptNumber() {
        return conflictResolutionAttemptNumber;
    }

    void setConflictResolutionAttemptNumber(int conflictResolutionAttemptNumber) {
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

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean hasProjectionChange() throws SchemaException, ConfigurationException {
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
            if (projectionContext.isGone()) {
                continue;
            }
            if (!projectionContext.isVisible()) {
                // Maybe we should act on really executed deltas. But adding a linkRef to development-mode resource
                // is a real primary delta, even if not executed. That's why we check the visibility here. Later we
                // should remove this hack.
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

    String getOwnerOid() {
        return ownerOid;
    }

    void setOwnerOid(String ownerOid) {
        this.ownerOid = ownerOid;
    }

    PrismObject<UserType> getCachedOwner() {
        return cachedOwner;
    }

    void setCachedOwner(PrismObject<UserType> cachedOwner) {
        this.cachedOwner = cachedOwner;
    }

    /**
     * Finish all building activities and prepare context for regular use.
     * This should lock all values that should not be changed during recompute,
     * such as primary deltas. (This is questionable, though: we modify primary delta
     * e.g. when determining estimated old values or when deleting linkRefs.)
     * This method is invoked by context factories when context build is finished.
     */
    void finishBuild() {
        if (InternalsConfig.consistencyChecks) {
            checkConsistence();
        }
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
        ObjectTreeDeltas<F> objectTreeDeltas = new ObjectTreeDeltas<>();
        if (getFocusContext() != null && getFocusContext().getPrimaryDelta() != null) {
            objectTreeDeltas.setFocusChange(getFocusContext().getPrimaryDelta().clone());
        }
        for (ModelProjectionContext projectionContext : getProjectionContexts()) {
            if (projectionContext.getPrimaryDelta() != null) {
                objectTreeDeltas.addProjectionChange(projectionContext.getKey(), projectionContext.getPrimaryDelta());
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
        return (objectClass, oid, lResult) -> {
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

    @SuppressWarnings("WeakerAccess") // may be used from scripts
    public boolean isInPrimary() {
        return state == ModelState.PRIMARY;
    }

    public boolean isInInitial() {
        return state == ModelState.INITIAL;
    }

    public boolean hasFocusOfType(Class<? extends ObjectType> type) {
        return focusContext != null && focusContext.isOfType(type);
    }

    /**
     * Updates the context after the (real or simulated) execution.
     *
     * 1. Selected contexts are marked as rotten (not fresh). This is to force their reload for the next execution wave.
     * We try to do this only for those that were really changed. This is more intelligent than {@link #rot(String)}.
     *
     * 2. Deltas are updated (e.g. secondary ones cleared) - currently for focus only.
     */
    void updateAfterExecution() {
        LOGGER.trace("Starting context update after execution");

        // 1. Context rot
        if (isFullyPersistentExecution()) {
            rotAfterExecution();
        } else {
            LOGGER.trace("Not rotting contexts because the mode is not 'full execution'");
        }

        // 2. Treating the deltas. This is intentionally NOT done for projections, as the current clients
        // rely on seeing the secondary deltas.
        if (focusContext != null) {
            focusContext.updateDeltasAfterExecution();
        }

        LOGGER.trace("Finished context update after execution");
    }

    private void rotAfterExecution() {
        boolean projectionRotten = false;
        for (LensProjectionContext projectionContext : projectionContexts) {
            if (projectionContext.rotAfterExecution()) {
                projectionRotten = true;
            }
        }
        if (focusContext != null) {
            focusContext.rotAfterExecution(projectionRotten);
        }
    }

    public boolean primaryFocusItemDeltaExists(ItemPath path) {
        return focusContext != null && focusContext.primaryItemDeltaExists(path);
    }

    /**
     * Removes results of any previous computations from the context.
     * (Expecting that transient values are not present. So deals only with non-transient ones.
     * Currently this means deletion of secondary deltas.)
     *
     * Used e.g. when restarting operation after being approved.
     */
    public void deleteNonTransientComputationResults() {
        if (focusContext != null) {
            focusContext.deleteNonTransientComputationResults();
        }
        for (LensProjectionContext projectionContext : projectionContexts) {
            projectionContext.deleteNonTransientComputationResults();
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
     *
     * Currently we can only look at executed deltas and check whether there is something relevant
     * (i.e. not FATAL_ERROR nor NOT_APPLICABLE).
     *
     * Any solution based on operation result status will never be 100% accurate, e.g. because
     * a network timeout could occur just before returning a status value. So please use with care.
     */
    @Experimental
    boolean wasAnythingExecuted() {
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

    public boolean isForcedFocusDelete() {
        return focusContext != null && focusContext.isDelete() && isForce();
    }

    void resetClickCounter() {
        clickCounter = 0;
        clickLimit = determineClickLimit();
    }

    private int determineClickLimit() {
        return Objects.requireNonNullElse(
                SystemConfigurationTypeUtil.getMaxModelClicks(systemConfiguration),
                DEFAULT_MAX_CLICKS);
    }

    void increaseClickCounter() {
        clickCounter++;
        if (clickCounter > clickLimit) {
            throw new IllegalStateException(
                    "Model operation took too many clicks (limit is " + clickLimit + "). Is there a cycle?");
        }
    }

    public void inspectProjectorStart() {
        if (inspector != null) {
            inspector.projectorStart(this);
        }
    }

    public void inspectProjectorFinish() {
        if (inspector != null) {
            inspector.projectorFinish(this);
        }
    }

    @SuppressWarnings("WeakerAccess") // may be used from scripts
    public boolean hasStarted() {
        return state != null;
    }

    public void setStartedIfNotYet() {
        if (state == null) {
            setState(ModelState.INITIAL);
        }
    }

    void checkNotStarted(String operation, LensElementContext<?> elementContext) {
        stateCheck(!hasStarted(),
                "Trying to %s but the clockwork has already started: %s in %s", operation, elementContext, this);
    }

    void clearLastChangeExecutionResult() {
        if (focusContext != null) {
            focusContext.clearLastChangeExecutionResult();
        }
        projectionContexts.forEach(
                LensElementContext::clearLastChangeExecutionResult);
    }

    public boolean isDiscoveryChannel() {
        return SchemaConstants.CHANNEL_DISCOVERY_URI.equals(channel);
    }

    /**
     * Resource OID should correspond to the construction. (However, it may be missing in the construction bean itself,
     * e.g. if a filter is used there.)
     */
    public void markMatchingProjectionsBroken(@NotNull ConstructionType construction, @NotNull String resourceOid) {
        projectionContexts.stream()
                .filter(ctx -> ctx.matches(construction, resourceOid))
                .filter(ctx -> !ctx.isCompleted())
                .forEach(LensProjectionContext::setBroken);
    }

    public @NotNull ModelBeans getModelBeans() {
        if (modelBeans == null) {
            modelBeans = SpringApplicationContextHolder.getBean(ModelBeans.class);
        }
        return modelBeans;
    }

    @Override
    public @NotNull TaskExecutionMode getTaskExecutionMode() {
        return taskExecutionMode;
    }

    public @NotNull ProvisioningOperationContext createProvisioningOperationContext() {
        return new ProvisioningOperationContext()
                .requestIdentifier(getRequestIdentifier())
                .expressionEnvironmentSupplier((task, result) -> new ModelExpressionEnvironment<>(this, null, task, result))
                .expressionProfile(getPrivilegedExpressionProfile());
    }

    public MetadataRecordingStrategyType getShadowMetadataRecordingStrategy() {
        SystemConfigurationType config = getSystemConfigurationBean();
        if (config == null) {
            return null;
        }
        InternalsConfigurationType internals = config.getInternals();
        if (internals == null) {
            return null;
        }
        return internals.getShadowMetadataRecording();
    }

    private boolean isFullyPersistentExecution() {
        return taskExecutionMode.isFullyPersistent();
    }

    boolean isProjectionRecomputationRequested() {
        return projectionContexts.stream().anyMatch(LensProjectionContext::isProjectionRecomputationRequested);
    }

    public boolean areAccessesMetadataEnabled() {
        return SystemConfigurationTypeUtil.isAccessesMetadataEnabled(getSystemConfigurationBean());
    }

    public enum ExportType {
        MINIMAL, REDUCED, OPERATIONAL, TRACE
    }

    // FIXME temporary solution
    public static class GetOrCreateProjectionContextResult {
        public final LensProjectionContext context;
        public final boolean created;

        private GetOrCreateProjectionContextResult(LensProjectionContext context, boolean created) {
            this.context = context;
            this.created = created;
        }

        @Override
        public String toString() {
            return "GetOrCreateProjectionContextResult{" +
                    "context=" + context +
                    ", created=" + created +
                    '}';
        }
    }

    public enum AuthorizationState {
        /** No authorization was carried out yet. */
        NONE,

        /** Only preliminary authorization was carried out (without e.g. org or tenant clauses). */
        PRELIMINARY,

        /** The full authorization was carried out. */
        FULL
    }

    /**
     * Metadata recorded when the operation has started. It is not necessary to store requestor comment here,
     * as it is preserved in context.options field.
     *
     * Values are parent-less.
     */
    record RequestMetadata(
            XMLGregorianCalendar requestTimestamp,
            ObjectReferenceType requestorRef) implements Serializable, Cloneable {

        static RequestMetadata create(XMLGregorianCalendar now, Task task) {
            return new RequestMetadata(
                    now,
                    ObjectTypeUtil.createObjectRefCopy(task.getOwnerRef()));
        }

        static RequestMetadata fromBean(MetadataType requestMetadata) {
            if (requestMetadata == null) {
                return null;
            } else {
                return new RequestMetadata(
                        requestMetadata.getRequestTimestamp(),
                        CloneUtil.clone(requestMetadata.getRequestorRef()));
            }
        }

        MetadataType toBean() {
            return new MetadataType()
                    .requestTimestamp(requestTimestamp)
                    .requestorRef(CloneUtil.clone(requestorRef));
        }

        @SuppressWarnings("MethodDoesntCallSuperMethod")
        public RequestMetadata clone() {
            return new RequestMetadata(requestTimestamp, CloneUtil.clone(requestorRef));
        }
    }
}
