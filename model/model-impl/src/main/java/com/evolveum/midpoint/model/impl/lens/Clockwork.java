/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import static com.evolveum.midpoint.model.api.ProgressInformation.ActivityType.CLOCKWORK;
import static com.evolveum.midpoint.model.api.ProgressInformation.ActivityType.WAITING;
import static com.evolveum.midpoint.model.api.ProgressInformation.StateType.ENTERING;
import static com.evolveum.midpoint.model.api.ProgressInformation.StateType.EXITING;
import static com.evolveum.midpoint.model.impl.lens.LensUtil.getExportType;
import static com.evolveum.midpoint.model.impl.lens.LensUtil.getExportTypeTraceOrReduced;
import static java.util.Collections.emptyList;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.impl.util.AuditHelper;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.schema.cache.CacheType;
import com.evolveum.midpoint.schema.result.OperationResultBuilder;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.util.logging.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ProgressInformation;
import com.evolveum.midpoint.model.api.ProgressListener;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.api.hooks.ChangeHook;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.api.hooks.HookRegistry;
import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.model.common.expression.evaluator.caching.AssociationSearchExpressionEvaluatorCache;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpression;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpressionFactory;
import com.evolveum.midpoint.model.impl.lens.projector.ContextLoader;
import com.evolveum.midpoint.model.impl.lens.projector.Projector;
import com.evolveum.midpoint.model.impl.lens.projector.focus.FocusConstraintsChecker;
import com.evolveum.midpoint.model.impl.lens.projector.policy.PolicyRuleScriptExecutor;
import com.evolveum.midpoint.model.impl.lens.projector.policy.PolicyRuleSuspendTaskExecutor;
import com.evolveum.midpoint.model.impl.migrator.Migrator;
import com.evolveum.midpoint.model.impl.sync.RecomputeTaskHandler;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.api.ChangeNotificationDispatcher;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener;
import com.evolveum.midpoint.provisioning.api.ResourceOperationListener;
import com.evolveum.midpoint.repo.api.ConflictWatcher;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.SystemConfigurationTypeUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

/**
 * @author semancik
 *
 */
@Component
public class Clockwork {

    private static final int DEFAULT_NUMBER_OF_RESULTS_TO_KEEP = 5;

    private static final int DEFAULT_MAX_CONFLICT_RESOLUTION_ATTEMPTS = 1;          // synchronize with common-core-3.xsd
    private static final int DEFAULT_CONFLICT_RESOLUTION_DELAY_UNIT = 5000;          // synchronize with common-core-3.xsd
    private static final int MAX_PRECONDITION_CONFLICT_RESOLUTION_ATTEMPTS = 3;

    private static final Trace LOGGER = TraceManager.getTrace(Clockwork.class);

    @Autowired private Projector projector;
    @Autowired private ContextLoader contextLoader;
    @Autowired private ChangeExecutor changeExecutor;
    @Autowired private AuditHelper auditHelper;
    @Autowired private Clock clock;
    @Autowired private SystemObjectCache systemObjectCache;
    @Autowired private transient ProvisioningService provisioningService;
    @Autowired private transient ChangeNotificationDispatcher changeNotificationDispatcher;
    @Autowired private ExpressionFactory expressionFactory;
    @Autowired private ScriptExpressionFactory scriptExpressionFactory;
    @Autowired private PersonaProcessor personaProcessor;
    @Autowired private PrismContext prismContext;
    @Autowired private TaskManager taskManager;
    @Autowired private Tracer tracer;
    @Autowired private OperationalDataManager metadataManager;
    @Autowired private ContextFactory contextFactory;
    @Autowired private Migrator migrator;
    @Autowired private ClockworkMedic medic;
    @Autowired private PolicyRuleScriptExecutor policyRuleScriptExecutor;
    @Autowired private PolicyRuleSuspendTaskExecutor policyRuleSuspendTaskExecutor;
    @Autowired private ClockworkAuthorizationHelper clockworkAuthorizationHelper;
    @Autowired private CacheConfigurationManager cacheConfigurationManager;

    @Autowired(required = false)
    private HookRegistry hookRegistry;

    @Autowired
    @Qualifier("cacheRepositoryService")
    private transient RepositoryService repositoryService;

    private static final int DEFAULT_MAX_CLICKS = 200;

    public <F extends ObjectType> HookOperationMode run(LensContext<F> context, Task task, OperationResult parentResult)
            throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException,
            ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException, PreconditionViolationException {

        OperationResultBuilder builder = parentResult.subresult(Clockwork.class.getName() + ".run");
        boolean tracingRequested = startTracingIfRequested(context, task, builder, parentResult);
        OperationResult result = builder.build();
        ClockworkRunTraceType trace = null;
        try {
            if (result.isTraced()) {
                trace = recordTraceAtStart(context, task, result);
            }

            LOGGER.trace("Running clockwork for context {}", context);
            if (InternalsConfig.consistencyChecks) {
                context.checkConsistence();
            }

            int clicked = 0;
            boolean focusConflictPresent = false;
            ConflictResolutionType conflictResolutionPolicy = null;
            HookOperationMode finalMode;

            try {
                context.reportProgress(new ProgressInformation(CLOCKWORK, ENTERING));
                if (context.getFocusContext() != null && context.getFocusContext().getOid() != null) {
                    context.createAndRegisterFocusConflictWatcher(context.getFocusContext().getOid(), repositoryService);
                }
                FocusConstraintsChecker
                        .enterCache(cacheConfigurationManager.getConfiguration(CacheType.LOCAL_FOCUS_CONSTRAINT_CHECKER_CACHE));
                enterAssociationSearchExpressionEvaluatorCache();
                //enterDefaultSearchExpressionEvaluatorCache();
                provisioningService.enterConstraintsCheckerCache();

                while (context.getState() != ModelState.FINAL) {

                    // TODO implement in model context (as transient or even non-transient attribute) to allow for checking in more complex scenarios
                    int maxClicks = getMaxClicks(context, result);
                    if (clicked >= maxClicks) {
                        throw new IllegalStateException(
                                "Model operation took too many clicks (limit is " + maxClicks + "). Is there a cycle?");
                    }
                    clicked++;

                    HookOperationMode mode = click(context, task, result);

                    if (mode == HookOperationMode.BACKGROUND) {
                        result.recordInProgress();
                        return mode;
                    } else if (mode == HookOperationMode.ERROR) {
                        return mode;
                    }
                }
                // One last click in FINAL state
                finalMode = click(context, task, result);
                if (finalMode == HookOperationMode.FOREGROUND) {
                    conflictResolutionPolicy = ModelImplUtils.getConflictResolution(context);
                    focusConflictPresent = checkFocusConflicts(context, conflictResolutionPolicy, task, result);
                }
            } finally {
                context.unregisterConflictWatchers(repositoryService);
                FocusConstraintsChecker.exitCache();
                //exitDefaultSearchExpressionEvaluatorCache();
                exitAssociationSearchExpressionEvaluatorCache();
                provisioningService.exitConstraintsCheckerCache();
                context.reportProgress(new ProgressInformation(CLOCKWORK, EXITING));
            }

            // intentionally outside the "try-finally" block to start with clean caches
            if (focusConflictPresent) {
                assert finalMode == HookOperationMode.FOREGROUND;
                finalMode = resolveFocusConflict(context, conflictResolutionPolicy, task, result);
            } else if (context.getConflictResolutionAttemptNumber() > 0) {
                LOGGER.info("Resolved update conflict on attempt number {}", context.getConflictResolutionAttemptNumber());
            }
            result.computeStatusIfUnknown();
            return finalMode;
        } catch (Throwable t) {
            result.recordFatalError(t.getMessage(), t);
            throw t;
        } finally {
            recordTraceAtEnd(context, trace, result);
            if (tracingRequested) {
                tracer.storeTrace(task, result, parentResult);
                TracingAppender.terminateCollecting();  // todo reconsider
                LevelOverrideTurboFilter.cancelLoggingOverride();   // todo reconsider
            }
            result.computeStatusIfUnknown();
        }
    }

    // todo check authorization in this method
    private <F extends ObjectType> boolean startTracingIfRequested(LensContext<F> context, Task task,
            OperationResultBuilder builder, OperationResult parentResult) throws SchemaException {
        // If the result is already traced, we could abstain from recording the final trace ourselves.
        // But I think it's more reasonable to do that, because e.g. if there is clockwork-inside-clockwork processing,
        // we would like to have two traces, even if the second one is contained also within the first one.
        TracingProfileType tracingProfile = ModelExecuteOptions.getTracingProfile(context.getOptions());
        if (tracingProfile != null) {
            builder.tracingProfile(tracer.compileProfile(tracingProfile, parentResult));
            return true;
        } else if (task.getTracingRequestedFor().contains(TracingRootType.CLOCKWORK_RUN)) {
            TracingProfileType profile = task.getTracingProfile() != null ? task.getTracingProfile() : tracer.getDefaultProfile();
            builder.tracingProfile(tracer.compileProfile(profile, parentResult));
            return true;
        } else {
            return false;
        }
    }

    private <F extends ObjectType> ClockworkRunTraceType recordTraceAtStart(LensContext<F> context, Task task,
            OperationResult result) throws SchemaException {
        ClockworkRunTraceType trace = new ClockworkRunTraceType(prismContext);
        trace.setInputLensContextText(context.debugDump());
        trace.setInputLensContext(context.toLensContextType(getExportTypeTraceOrReduced(trace, result)));
        result.addTrace(trace);
        return trace;
    }

    private <F extends ObjectType> void recordTraceAtEnd(LensContext<F> context, ClockworkRunTraceType trace,
            OperationResult result) throws SchemaException {
        if (trace != null) {
            trace.setOutputLensContextText(context.debugDump());
            trace.setOutputLensContext(context.toLensContextType(getExportTypeTraceOrReduced(trace, result)));
            if (context.getFocusContext() != null) {    // todo reconsider this
                PrismObject<F> objectAny = context.getFocusContext().getObjectAny();
                if (objectAny != null) {
                    trace.setFocusName(PolyString.getOrig(objectAny.getName()));
                }
            }
        }
    }

    public <F extends ObjectType> LensContext<F> previewChanges(LensContext<F> context, Collection<ProgressListener> listeners, Task task, OperationResult result)
            throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException {
        try {
            context.setPreview(true);

//            context.setOptions(options);
            LOGGER.trace("Preview changes context:\n{}", context.debugDumpLazily());
            context.setProgressListeners(listeners);

            projector.projectAllWaves(context, "preview", task, result);

            if (hookRegistry != null) {
                for (ChangeHook hook : hookRegistry.getAllChangeHooks()) {
                    hook.invokePreview(context, task, result);
                }
            }

            policyRuleSuspendTaskExecutor.execute(context, task, result);

        } catch (ConfigurationException | SecurityViolationException | ObjectNotFoundException | SchemaException |
                CommunicationException | PolicyViolationException | RuntimeException | ObjectAlreadyExistsException |
                ExpressionEvaluationException e) {
            ModelImplUtils.recordFatalError(result, e);
            throw e;

        } catch (PreconditionViolationException e) {
            ModelImplUtils.recordFatalError(result, e);
            // TODO: Temporary fix for 3.6.1
            // We do not want to propagate PreconditionViolationException to model API as that might break compatibility
            // ... and we do not really need that in 3.6.1
            // TODO: expose PreconditionViolationException in 3.7
            throw new SystemException(e);

        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Preview changes output:\n{}", context.debugDump());
        }

        result.computeStatus();
        result.cleanupResult();

        return context;
    }

    private <F extends ObjectType> boolean checkFocusConflicts(LensContext<F> context, ConflictResolutionType resolutionPolicy,
            Task task, OperationResult result) {
        ConflictWatcher watcher = context.getFocusConflictWatcher();
        if (watcher != null && resolutionPolicy != null && resolutionPolicy.getAction() != ConflictResolutionActionType.NONE &&
                repositoryService.hasConflict(watcher, result)) {
            LOGGER.debug("Found modify-modify conflict on {}", watcher);
            return true;
        } else {
            return false;
        }
    }

    private <F extends ObjectType> HookOperationMode resolveFocusConflict(LensContext<F> context,
            ConflictResolutionType resolutionPolicy, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, ConfigurationException,
            CommunicationException, SecurityViolationException, PolicyViolationException, ObjectAlreadyExistsException {
        if (resolutionPolicy == null || resolutionPolicy.getAction() == ConflictResolutionActionType.NONE) {
            return HookOperationMode.FOREGROUND;
        }
        PrismObject<F> focusObject = context.getFocusContext() != null ? context.getFocusContext().getObjectAny() : null;
        ModelExecuteOptions options = new ModelExecuteOptions();
        switch (resolutionPolicy.getAction()) {
            case FAIL: throw new SystemException("Conflict detected while updating " + focusObject);
            case LOG:
                LOGGER.warn("Conflict detected while updating {}", focusObject);
                return HookOperationMode.FOREGROUND;
            case RECOMPUTE:
                break;
            case RECONCILE:
                options.setReconcile();
                break;
            default:
                throw new IllegalStateException("Unsupported conflict resolution action: " + resolutionPolicy.getAction());
        }

        // so, recompute is the action
        LOGGER.debug("CONFLICT: Conflict detected while updating {}, recomputing (options={})", focusObject, options);

        if (context.getFocusContext() == null) {
            LOGGER.warn("No focus context, not possible to resolve conflict by focus recomputation");       // should really never occur
            return HookOperationMode.FOREGROUND;
        }
        String oid = context.getFocusContext().getOid();
        if (oid == null) {
            LOGGER.warn("No focus OID, not possible to resolve conflict by focus recomputation");       // should really never occur
            return HookOperationMode.FOREGROUND;
        }
        Class<F> focusClass = context.getFocusContext().getObjectTypeClass();
        if (focusClass == null) {
            LOGGER.warn("Focus class not known, not possible to resolve conflict by focus recomputation");       // should really never occur
            return HookOperationMode.FOREGROUND;
        }
        if (TaskType.class.isAssignableFrom(focusClass)) {
            return HookOperationMode.FOREGROUND;        // this is actually quite expected, so don't bother anyone with that
        }
        if (!FocusType.class.isAssignableFrom(focusClass)) {
            LOGGER.warn("Focus is not of FocusType (it is {}); not possible to resolve conflict by focus recomputation", focusClass.getName());
            return HookOperationMode.FOREGROUND;
        }

        ConflictResolutionType focusConflictResolution = new ConflictResolutionType();
        focusConflictResolution.setAction(ConflictResolutionActionType.ERROR);
        options.setFocusConflictResolution(focusConflictResolution);

        int preconditionAttempts = 0;
        while (true) {

            int attemptOld = context.getConflictResolutionAttemptNumber();
            int attemptNew = attemptOld + 1;
            boolean shouldExecuteAttempt = shouldExecuteAttempt(context, resolutionPolicy, attemptNew);
            if (!shouldExecuteAttempt) {
                LOGGER.warn("CONFLICT: Couldn't resolve conflict even after {} resolution attempt(s), giving up.", attemptOld);
                return HookOperationMode.FOREGROUND;
            }

            delay(context, resolutionPolicy, attemptNew + preconditionAttempts);

            PrismObject<F> focus = repositoryService.getObject(focusClass, oid, null, result);
            LensContext<FocusType> contextNew = contextFactory.createRecomputeContext(focus, options, task, result);
            contextNew.setProgressListeners(new ArrayList<>(emptyIfNull(context.getProgressListeners())));
            contextNew.setConflictResolutionAttemptNumber(attemptNew);

            LOGGER.debug("CONFLICT: Recomputing {} as reaction to conflict (options={}, attempts={},{}, readVersion={})",
                    context.getFocusContext().getHumanReadableName(), options, attemptNew, preconditionAttempts, contextNew.getFocusContext().getObjectReadVersion());

            try {

                // this is a recursion; but limited to max attempts which should not be a large number
                HookOperationMode hookOperationMode = run(contextNew, task, result);

                // This may be in fact a giveup after recompute that was not able to cleanly proceed.
                LOGGER.debug("CONFLICT: Clean recompute (or giveup) of {} achieved (options={}, attempts={},{})",
                        context.getFocusContext().getHumanReadableName(), options, attemptNew, preconditionAttempts);

                return hookOperationMode;

            } catch (PreconditionViolationException e) {
                preconditionAttempts++;
                LOGGER.debug("CONFLICT: Recompute precondition failed (attempt {}, precondition attempt {}), trying again", attemptNew, preconditionAttempts);
                if (preconditionAttempts < MAX_PRECONDITION_CONFLICT_RESOLUTION_ATTEMPTS) {
                    continue;
                }
                LOGGER.warn("CONFLICT: Couldn't resolve conflict even after {} resolution attempt(s) and {} precondition attempts, giving up.",
                        attemptOld, preconditionAttempts);
                return HookOperationMode.FOREGROUND;
            }
        }
    }

    private <F extends ObjectType> boolean shouldExecuteAttempt(LensContext<F> context, @NotNull ConflictResolutionType resolutionPolicy, int attemptNew) {
        int maxAttempts = defaultIfNull(resolutionPolicy.getMaxAttempts(), DEFAULT_MAX_CONFLICT_RESOLUTION_ATTEMPTS);
        if (attemptNew > maxAttempts) {
            return false;
        }
        return true;
    }

    private <F extends ObjectType> void delay(LensContext<F> context, @NotNull ConflictResolutionType resolutionPolicy, int attempts) {
        long delayUnit = defaultIfNull(resolutionPolicy.getDelayUnit(), DEFAULT_CONFLICT_RESOLUTION_DELAY_UNIT);
        for (int i = 0; i < attempts; i++) {
            delayUnit *= 2;
        }
        long delay = (long) (Math.random() * delayUnit);
        String message = "CONFLICT: Waiting "+delay+" milliseconds before starting conflict resolution (delay exponent: "+attempts+")";
        // TODO convey information about waiting time after some GUI mechanism for displaying it is available
        // (showing text messages is currently really ugly)
        context.reportProgress(new ProgressInformation(WAITING, EXITING));
        LOGGER.debug(message);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            // ignore
        }
        context.reportProgress(new ProgressInformation(WAITING, EXITING));
    }

    private void enterAssociationSearchExpressionEvaluatorCache() {
        AssociationSearchExpressionEvaluatorCache cache = AssociationSearchExpressionEvaluatorCache.enterCache(
                cacheConfigurationManager.getConfiguration(CacheType.LOCAL_ASSOCIATION_TARGET_SEARCH_EVALUATOR_CACHE));
        if (cache.getClientContextInformation() == null) {
            AssociationSearchExpressionCacheInvalidator invalidator = new AssociationSearchExpressionCacheInvalidator(cache);
            cache.setClientContextInformation(invalidator);
            changeNotificationDispatcher.registerNotificationListener((ResourceObjectChangeListener) invalidator);
            changeNotificationDispatcher.registerNotificationListener((ResourceOperationListener) invalidator);
        }
    }

    private void exitAssociationSearchExpressionEvaluatorCache() {
        AssociationSearchExpressionEvaluatorCache cache = AssociationSearchExpressionEvaluatorCache.exitCache();
        if (cache == null) {
            LOGGER.error("exitAssociationSearchExpressionEvaluatorCache: cache instance was not found for the current thread");
            return;
        }
        if (cache.getEntryCount() <= 0) {
            Object invalidator = cache.getClientContextInformation();
            if (!(invalidator instanceof AssociationSearchExpressionCacheInvalidator)) {
                LOGGER.error("exitAssociationSearchExpressionEvaluatorCache: expected {}, got {} instead",
                        AssociationSearchExpressionCacheInvalidator.class, invalidator);
                return;
            }
            changeNotificationDispatcher.unregisterNotificationListener((ResourceObjectChangeListener) invalidator);
            changeNotificationDispatcher.unregisterNotificationListener((ResourceOperationListener) invalidator);
        }
    }

    private void enterDefaultSearchExpressionEvaluatorCache() {
        //DefaultSearchExpressionEvaluatorCache.enterCache(cacheConfigurationManager.getConfiguration(CacheType.LOCAL_DEFAULT_SEARCH_EVALUATOR_CACHE));
    }

    private void exitDefaultSearchExpressionEvaluatorCache() {
        //DefaultSearchExpressionEvaluatorCache.exitCache();
    }

    private <F extends ObjectType> int getMaxClicks(LensContext<F> context, OperationResult result) throws SchemaException, ObjectNotFoundException {
        PrismObject<SystemConfigurationType> systemConfiguration = systemObjectCache.getSystemConfiguration(result);
        Integer maxClicks = SystemConfigurationTypeUtil.getMaxModelClicks(systemConfiguration);
        if (maxClicks == null) {
            return DEFAULT_MAX_CLICKS;
        } else {
            return maxClicks;
        }
    }

    public <F extends ObjectType> HookOperationMode click(LensContext<F> context, Task task, OperationResult parentResult)
            throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException,
            ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException, PreconditionViolationException {

        // DO NOT CHECK CONSISTENCY of the context here. The context may not be fresh and consistent yet. Project will fix
        // that. Check consistency afterwards (and it is also checked inside projector several times).

        if (context.getInspector() == null) {
            context.setInspector(medic.getClockworkInspector());
        }

        OperationResult result = parentResult.subresult(Clockwork.class.getName() + ".click")
                .addQualifier(context.getOperationQualifier())
                .addArbitraryObjectAsContext("context", context)
                .addArbitraryObjectAsContext("task", task)
                .build();

        ClockworkClickTraceType trace;
        if (result.isTraced()) {
            trace = new ClockworkClickTraceType(prismContext);
            if (result.isTracingNormal(ClockworkClickTraceType.class)) {
                trace.setInputLensContextText(context.debugDump());
            }
            trace.setInputLensContext(context.toLensContextType(getExportType(trace, result)));
            result.getTraces().add(trace);
        } else {
            trace = null;
        }
        try {

            XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();

            // We need to determine focus before auditing. Otherwise we will not know user
            // for the accounts (unless there is a specific delta for it).
            // This is ugly, but it is the easiest way now (TODO: cleanup).
            contextLoader.determineFocusContext(context, task, result);

            ModelState state = context.getState();
            if (state == ModelState.INITIAL) {
                medic.clockworkStart(context);
                metadataManager.setRequestMetadataInContext(context, now, task);
                context.getStats().setRequestTimestamp(now);
                context.generateRequestIdentifierIfNeeded();
                // We need to do this BEFORE projection. If we would do that after projection
                // there will be secondary changes that are not part of the request.
                audit(context, AuditEventStage.REQUEST, task, result, parentResult);        // we need to take the overall ("run" operation result) not the current one
            }

            boolean recompute = false;
            if (!context.isFresh()) {
                LOGGER.trace("Context is not fresh -- forcing cleanup and recomputation");
                recompute = true;
            } else if (context.getExecutionWave() > context.getProjectionWave()) {        // should not occur
                LOGGER.warn("Execution wave is greater than projection wave -- forcing cleanup and recomputation");
                recompute = true;
            } else if (state == ModelState.PRIMARY && ModelExecuteOptions.getInitialPartialProcessing(context.getOptions()) != null) {
                LOGGER.trace("Initial phase was run with initialPartialProcessing option -- forcing cleanup and recomputation");
                recompute = true;
            }

            if (recompute) {
                context.cleanup();
                LOGGER.trace("Running projector with cleaned-up context for execution wave {}", context.getExecutionWave());
                projector.project(context, "PROJECTOR ("+state+")", task, result);
            } else if (context.getExecutionWave() == context.getProjectionWave()) {
                LOGGER.trace("Resuming projector for execution wave {}", context.getExecutionWave());
                projector.resume(context, "PROJECTOR ("+state+")", task, result);
            } else {
                LOGGER.trace("Skipping projection because the context is fresh and projection for current wave has already run");
            }

            if (!context.isRequestAuthorized()) {
                clockworkAuthorizationHelper.authorizeContextRequest(context, task, result);
            }

            medic.traceContext(LOGGER, "CLOCKWORK (" + state + ")", "before processing", true, context, false);
            if (InternalsConfig.consistencyChecks) {
                try {
                    context.checkConsistence();
                } catch (IllegalStateException e) {
                    throw new IllegalStateException(e.getMessage()+" in clockwork, state="+state, e);
                }
            }
            if (InternalsConfig.encryptionChecks && !ModelExecuteOptions.isNoCrypt(context.getOptions())) {
                context.checkEncrypted();
            }

    //        LOGGER.info("CLOCKWORK: {}: {}", state, context);

            switch (state) {
                case INITIAL:
                    processInitialToPrimary(context, task, result);
                    break;
                case PRIMARY:
                    processPrimaryToSecondary(context, task, result);
                    break;
                case SECONDARY:
                    processSecondary(context, task, result, parentResult);
                    break;
                case FINAL:
                    HookOperationMode mode = processFinal(context, task, result, parentResult);
                    medic.clockworkFinish(context);
                    return mode;
            }
            return invokeHooks(context, task, result);

        } catch (CommunicationException | ConfigurationException | ExpressionEvaluationException | ObjectNotFoundException |
                PolicyViolationException | SchemaException | SecurityViolationException | RuntimeException | Error |
                ObjectAlreadyExistsException | PreconditionViolationException e) {
            processClockworkException(context, e, task, result, parentResult);
            throw e;
        } finally {
            if (trace != null) {
                if (result.isTracingNormal(ClockworkClickTraceType.class)) {
                    trace.setOutputLensContextText(context.debugDump());
                }
                trace.setOutputLensContext(context.toLensContextType(getExportType(trace, result)));
            }
            result.computeStatusIfUnknown();
            result.cleanupResultDeeply();
        }
    }

    /**
     * Invokes hooks, if there are any.
     *
     * @return
     *  - ERROR, if any hook reported error; otherwise returns
     *  - BACKGROUND, if any hook reported switching to background; otherwise
     *  - FOREGROUND (if all hooks reported finishing on foreground)
     */
    private HookOperationMode invokeHooks(LensContext context, Task task, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, PolicyViolationException, CommunicationException, ConfigurationException, SecurityViolationException {
        // TODO: following two parts should be merged together in later versions

        // Execute configured scripting hooks
        PrismObject<SystemConfigurationType> systemConfiguration = systemObjectCache.getSystemConfiguration(result);
        // systemConfiguration may be null in some tests
        if (systemConfiguration != null) {
            ModelHooksType modelHooks = systemConfiguration.asObjectable().getModelHooks();
            if (modelHooks != null) {
                HookListType changeHooks = modelHooks.getChange();
                if (changeHooks != null) {
                    for (HookType hookType: changeHooks.getHook()) {
                        String shortDesc;
                        if (hookType.getName() != null) {
                            shortDesc = "hook '"+hookType.getName()+"'";
                        } else {
                            shortDesc = "scripting hook in system configuration";
                        }
                        if (hookType.isEnabled() != null && !hookType.isEnabled()) {
                            // Disabled hook, skip
                            continue;
                        }
                        if (hookType.getState() != null) {
                            if (!context.getState().toModelStateType().equals(hookType.getState())) {
                                continue;
                            }
                        }
                        if (hookType.getFocusType() != null) {
                            if (context.getFocusContext() == null) {
                                continue;
                            }
                            QName hookFocusTypeQname = hookType.getFocusType();
                            ObjectTypes hookFocusType = ObjectTypes.getObjectTypeFromTypeQName(hookFocusTypeQname);
                            if (hookFocusType == null) {
                                throw new SchemaException("Unknown focus type QName "+hookFocusTypeQname+" in "+shortDesc);
                            }
                            Class focusClass = context.getFocusClass();
                            Class<? extends ObjectType> hookFocusClass = hookFocusType.getClassDefinition();
                            if (!hookFocusClass.isAssignableFrom(focusClass)) {
                                continue;
                            }
                        }

                        ScriptExpressionEvaluatorType scriptExpressionEvaluatorType = hookType.getScript();
                        if (scriptExpressionEvaluatorType == null) {
                            continue;
                        }
                        try {
                            evaluateScriptingHook(context, hookType, scriptExpressionEvaluatorType, shortDesc, task, result);
                        } catch (ExpressionEvaluationException e) {
                            LOGGER.error("Evaluation of {} failed: {}", shortDesc, e.getMessage(), e);
                            throw new ExpressionEvaluationException("Evaluation of "+shortDesc+" failed: "+e.getMessage(), e);
                        } catch (ObjectNotFoundException e) {
                            LOGGER.error("Evaluation of {} failed: {}", shortDesc, e.getMessage(), e);
                            throw new ObjectNotFoundException("Evaluation of "+shortDesc+" failed: "+e.getMessage(), e);
                        } catch (SchemaException e) {
                            LOGGER.error("Evaluation of {} failed: {}", shortDesc, e.getMessage(), e);
                            throw new SchemaException("Evaluation of "+shortDesc+" failed: "+e.getMessage(), e);
                        } catch (CommunicationException e) {
                            LOGGER.error("Evaluation of {} failed: {}", shortDesc, e.getMessage(), e);
                            throw new CommunicationException("Evaluation of "+shortDesc+" failed: "+e.getMessage(), e);
                        } catch (ConfigurationException e) {
                            LOGGER.error("Evaluation of {} failed: {}", shortDesc, e.getMessage(), e);
                            throw new ConfigurationException("Evaluation of "+shortDesc+" failed: "+e.getMessage(), e);
                        } catch (SecurityViolationException e) {
                            LOGGER.error("Evaluation of {} failed: {}", shortDesc, e.getMessage(), e);
                            throw new SecurityViolationException("Evaluation of "+shortDesc+" failed: "+e.getMessage(), e);
                        }
                    }
                }
            }
        }

        // Execute registered Java hooks
        HookOperationMode resultMode = HookOperationMode.FOREGROUND;
        if (hookRegistry != null) {
            for (ChangeHook hook : hookRegistry.getAllChangeHooks()) {
                HookOperationMode mode = hook.invoke(context, task, result);
                if (mode == HookOperationMode.ERROR) {
                    resultMode = HookOperationMode.ERROR;
                } else if (mode == HookOperationMode.BACKGROUND) {
                    if (resultMode != HookOperationMode.ERROR) {
                        resultMode = HookOperationMode.BACKGROUND;
                    }
                }
            }
        }
        return resultMode;
    }


    private void evaluateScriptingHook(LensContext context, HookType hookType,
            ScriptExpressionEvaluatorType scriptExpressionEvaluatorType, String shortDesc, Task task, OperationResult result)
                    throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {

        LOGGER.trace("Evaluating {}", shortDesc);
        // TODO: it would be nice to cache this
        // null output definition: this script has no output
        ScriptExpression scriptExpression = scriptExpressionFactory.createScriptExpression(scriptExpressionEvaluatorType, null, context.getPrivilegedExpressionProfile(), expressionFactory, shortDesc, task, result);

        ExpressionVariables variables = new ExpressionVariables();
        variables.put(ExpressionConstants.VAR_PRISM_CONTEXT, prismContext, PrismContext.class);
        variables.put(ExpressionConstants.VAR_MODEL_CONTEXT, context, ModelContext.class);
        LensFocusContext focusContext = context.getFocusContext();
        PrismObject focus = null;
        if (focusContext != null) {
            variables.put(ExpressionConstants.VAR_FOCUS, focusContext.getObjectAny(), focusContext.getObjectDefinition());
        } else {
            PrismObjectDefinition<ObjectType> def = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ObjectType.class);
            variables.put(ExpressionConstants.VAR_FOCUS, null, def);
        }


        ModelImplUtils.evaluateScript(scriptExpression, context, variables, false, shortDesc, task, result);
        LOGGER.trace("Finished evaluation of {}", shortDesc);
    }

    private <F extends ObjectType> void switchState(LensContext<F> context, ModelState newState) {
        medic.clockworkStateSwitch(context, newState);
        context.setState(newState);
    }

    private <F extends ObjectType> void processInitialToPrimary(LensContext<F> context, Task task, OperationResult result) {
        // Context loaded, nothing special do. Bump state to PRIMARY.
        switchState(context, ModelState.PRIMARY);
    }

    private <F extends ObjectType> void processPrimaryToSecondary(LensContext<F> context, Task task, OperationResult result) throws PolicyViolationException, ObjectNotFoundException, SchemaException {
        // Nothing to do now. The context is already recomputed.
        switchState(context, ModelState.SECONDARY);

        policyRuleSuspendTaskExecutor.execute(context, task, result);

    }

    private <F extends ObjectType> void processSecondary(LensContext<F> context, Task task, OperationResult result, OperationResult overallResult)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException, PolicyViolationException, PreconditionViolationException {
        if (context.getExecutionWave() > context.getMaxWave() + 1) {
            processSecondaryToFinal(context, task, result);
            return;
        }

        Holder<Boolean> restartRequestedHolder = new Holder<>();

        medic.partialExecute("execution",
                (result1) -> {
                    boolean restartRequested = changeExecutor.executeChanges(context, task, result1);
                    restartRequestedHolder.setValue(restartRequested);
                },
                context.getPartialProcessingOptions()::getExecution,
                Clockwork.class, context, result);

        audit(context, AuditEventStage.EXECUTION, task, result, overallResult);

        rotContextIfNeeded(context);

        boolean restartRequested = false;
        if (restartRequestedHolder.getValue() != null) {
            restartRequested = restartRequestedHolder.getValue();
        }

        if (!restartRequested) {
            // TODO what if restart is requested indefinitely?
            context.incrementExecutionWave();
        } else {
            // explicitly rot context?
        }

        medic.traceContext(LOGGER, "CLOCKWORK (" + context.getState() + ")", "change execution", false, context, false);
    }


    private <F extends ObjectType> void processSecondaryToFinal(LensContext<F> context, Task task, OperationResult result) throws PolicyViolationException {
        switchState(context, ModelState.FINAL);
        policyRuleScriptExecutor.execute(context, task, result);
    }

    /**
     * Force recompute for the next execution wave. Recompute only those contexts that were changed.
     * This is more intelligent than context.rot()
     */
    private <F extends ObjectType> void rotContextIfNeeded(LensContext<F> context) throws SchemaException {
        boolean rot = false;
        for (LensProjectionContext projectionContext: context.getProjectionContexts()) {
            if (projectionContext.getWave() != context.getExecutionWave()) {
                LOGGER.trace("Context rot: projection {} NOT rotten because of wrong wave number", projectionContext);
                continue;
            }
//            if (!projectionContext.isDoReconciliation()) {    // meaning volatility is NONE
//                LOGGER.trace("Context rot: projection {} NOT rotten because the resource is non-volatile", projectionContext);
//                continue;
//            }
            ObjectDelta<ShadowType> execDelta = projectionContext.getExecutableDelta();
            if (isShadowDeltaSignificant(execDelta)) {

                LOGGER.debug("Context rot: projection {} rotten because of executable delta {}", projectionContext, execDelta);
                   projectionContext.setFresh(false);
                  projectionContext.setFullShadow(false);
                   rot = true;
                   // Propagate to higher-order projections
                   for (LensProjectionContext relCtx: LensUtil.findRelatedContexts(context, projectionContext)) {
                       relCtx.setFresh(false);
                       relCtx.setFullShadow(false);
                  }

            } else {
                LOGGER.trace("Context rot: projection {} NOT rotten because no delta", projectionContext);
            }
        }
        LensFocusContext<F> focusContext = context.getFocusContext();
        if (focusContext != null) {
            ObjectDelta<F> execDelta = focusContext.getWaveDelta(context.getExecutionWave());
            if (execDelta != null && !execDelta.isEmpty()) {
                LOGGER.debug("Context rot: context rotten because of focus execution delta {}", execDelta);
                rot = true;
            }
            if (rot) {
                // It is OK to refresh focus all the time there was any change. This is cheap.
                focusContext.setFresh(false);
            }
            //remove secondary deltas from other than execution wave - we need to recompute them..
//            cleanUpSecondaryDeltas(context);


        }
        if (rot) {
            context.setFresh(false);
        }
    }

//    // TODO this is quite unclear. Originally here was keeping the delta from the current wave (plus delta from wave #1).
//    // The reason was not clear.
//    // Let us erase everything.
//    private <F extends ObjectType> void cleanUpSecondaryDeltas(LensContext<F> context){
//        LensFocusContext focusContext = context.getFocusContext();
//        ObjectDeltaWaves<F> executionWaveDeltaList = focusContext.getSecondaryDeltas();
//        executionWaveDeltaList.clear();
//    }

    private <P extends ObjectType> boolean isShadowDeltaSignificant(ObjectDelta<P> delta) {
        if (delta == null || delta.isEmpty()) {
            return false;
        }
        if (delta.isAdd() || delta.isDelete()) {
            return true;
        }
        Collection<? extends ItemDelta<?,?>> attrDeltas = delta.findItemDeltasSubPath(ShadowType.F_ATTRIBUTES);
        if (attrDeltas != null && !attrDeltas.isEmpty()) {
            return true;
        }
        return false;
    }

    private <F extends ObjectType> HookOperationMode processFinal(LensContext<F> context, Task task, OperationResult result, OperationResult overallResult)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException, PolicyViolationException, PreconditionViolationException {
        auditFinalExecution(context, task, result, overallResult);
        logFinalReadable(context, task, result);
        recordOperationExecution(context, null, task, result);
        migrator.executeAfterOperationMigration(context, result);

        HookOperationMode opmode = personaProcessor.processPersonaChanges(context, task, result);
        if (opmode == HookOperationMode.BACKGROUND) {
            return opmode;
        }

        return triggerReconcileAffected(context, task, result);
    }

    private <F extends ObjectType> void recordOperationExecution(LensContext<F> context, Throwable clockworkException,
            Task task, OperationResult result) {
        boolean skip = context.getInternalsConfiguration() != null &&
                context.getInternalsConfiguration().getOperationExecutionRecording() != null &&
                Boolean.TRUE.equals(context.getInternalsConfiguration().getOperationExecutionRecording().isSkip());
        if (!skip) {
            XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
            try {
                LOGGER.trace("recordOperationExecution starting; task = {}, clockworkException = {}", task, clockworkException);
                boolean opRecordedIntoFocus = recordFocusOperationExecution(context, now, clockworkException, task, result);
                for (LensProjectionContext projectionContext : context.getProjectionContexts()) {
                    Throwable exceptionToProjection;
                    if (clockworkException != null && !opRecordedIntoFocus && projectionContext.isSynchronizationSource()) {
                        // We need to record the exception somewhere. Because we were not able to put it into focus,
                        // we have to do it into sync-source projection.
                        exceptionToProjection = clockworkException;
                    } else {
                        exceptionToProjection = null;
                    }
                    recordProjectionOperationExecution(context, projectionContext, now, exceptionToProjection, task, result);
                }
            } catch (Throwable t) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't record operation execution. Model context:\n{}", t,
                        context.debugDump());
                // Let us ignore this for the moment. It should not have happened, sure. But it's not that crucial.
                // Administrator will be able to learn about the problem from the log.
            }
        } else {
            LOGGER.trace("Skipping operation execution recording (as set in system configuration)");
        }
    }

    /**
     * @return true if the operation execution was recorded (or would be recorded, but skipped because of the configuration)
     */
    private <F extends ObjectType> boolean recordFocusOperationExecution(LensContext<F> context, XMLGregorianCalendar now,
            Throwable clockworkException, Task task, OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
        LensFocusContext<F> focusContext = context.getFocusContext();
        if (focusContext == null || focusContext.isDelete()) {
            LOGGER.trace("focusContext is null or 'delete', not recording focus operation execution");
            return false;
        }
        PrismObject<F> objectNew = focusContext.getObjectNew();
        Validate.notNull(objectNew, "No focus object even if the context is not of 'delete' type");

        //noinspection unchecked
        List<LensObjectDeltaOperation<F>> executedDeltas = getExecutedDeltas(focusContext,
                (Class<F>) objectNew.asObjectable().getClass(), clockworkException, result);
        LOGGER.trace("recordFocusOperationExecution: executedDeltas: {}", executedDeltas.size());
        return recordOperationExecution(objectNew, false, executedDeltas, now, context.getChannel(),
                getSkipWhenSuccess(context), task, result);
    }

    @NotNull
    private <O extends ObjectType> List<LensObjectDeltaOperation<O>> getExecutedDeltas(LensElementContext<O> elementContext,
            Class<O> objectClass, Throwable clockworkException, OperationResult result) {
        List<LensObjectDeltaOperation<O>> executedDeltas;
        if (clockworkException != null) {
            executedDeltas = new ArrayList<>(elementContext.getExecutedDeltas());
            LensObjectDeltaOperation<O> odo = new LensObjectDeltaOperation<>();
            ObjectDelta<O> primaryDelta = elementContext.getPrimaryDelta();
            if (primaryDelta != null) {
                odo.setObjectDelta(primaryDelta);
            } else {
                ObjectDelta<O> fakeDelta = prismContext.deltaFactory().object().create(objectClass, ChangeType.MODIFY);
                odo.setObjectDelta(fakeDelta);
            }
            odo.setExecutionResult(result);        // we rely on the fact that 'result' already contains record of the exception
            executedDeltas.add(odo);
        } else {
            executedDeltas = elementContext.getExecutedDeltas();
        }
        return executedDeltas;
    }

    private <F extends ObjectType> boolean getSkipWhenSuccess(LensContext<F> context) {
        return context.getInternalsConfiguration() != null &&
                context.getInternalsConfiguration().getOperationExecutionRecording() != null &&
                Boolean.TRUE.equals(context.getInternalsConfiguration().getOperationExecutionRecording().isSkipWhenSuccess());
    }

    private <F extends ObjectType> void recordProjectionOperationExecution(LensContext<F> context,
            LensProjectionContext projectionContext, XMLGregorianCalendar now, Throwable clockworkException,
            Task task, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
        PrismObject<ShadowType> object = projectionContext.getObjectAny();
        if (object == null) {
            return;            // this can happen
        }
        List<LensObjectDeltaOperation<ShadowType>> executedDeltas = getExecutedDeltas(projectionContext, ShadowType.class,
                clockworkException, result);
        recordOperationExecution(object, true, executedDeltas, now,
                context.getChannel(), getSkipWhenSuccess(context), task, result);
    }

    /**
     * @return true if the operation execution was recorded (or would be recorded, but skipped because of the configuration)
     */
    private <F extends ObjectType> boolean recordOperationExecution(PrismObject<F> object, boolean deletedOk,
            List<LensObjectDeltaOperation<F>> executedDeltas, XMLGregorianCalendar now,
            String channel, boolean skipWhenSuccess, Task task, OperationResult result)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationExecutionType operation = new OperationExecutionType(prismContext);
        OperationResult summaryResult = new OperationResult("recordOperationExecution");
        String oid = object.getOid();
        for (LensObjectDeltaOperation<F> deltaOperation : executedDeltas) {
            operation.getOperation().add(createObjectDeltaOperation(deltaOperation));
            if (deltaOperation.getExecutionResult() != null) {
                summaryResult.addSubresult(deltaOperation.getExecutionResult().clone());        // todo eliminate this clone (but beware of modifying the subresult)
            }
            if (oid == null && deltaOperation.getObjectDelta() != null) {
                oid = deltaOperation.getObjectDelta().getOid();
            }
        }
        if (oid == null) {        // e.g. if there is an exception in provisioning.addObject method
            LOGGER.trace("recordOperationExecution: skipping because oid is null for object = {}", object);
            return false;
        }
        summaryResult.computeStatus();
        OperationResultStatusType overallStatus = summaryResult.getStatus().createStatusType();
        setOperationContext(operation, overallStatus, now, channel, task);
        storeOperationExecution(object, oid, operation, deletedOk, skipWhenSuccess, result);
        return true;
    }

    private <F extends ObjectType> void storeOperationExecution(@NotNull PrismObject<F> object, @NotNull String oid,
            @NotNull OperationExecutionType executionToAdd, boolean deletedOk, boolean skipWhenSuccess, OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
        Integer recordsToKeep;
        Long deleteBefore;
        boolean keepNoExecutions = false;
        PrismObject<SystemConfigurationType> systemConfiguration = systemObjectCache.getSystemConfiguration(result);
        if (systemConfiguration != null && systemConfiguration.asObjectable().getCleanupPolicy() != null
                && systemConfiguration.asObjectable().getCleanupPolicy().getObjectResults() != null) {
            CleanupPolicyType policy = systemConfiguration.asObjectable().getCleanupPolicy().getObjectResults();
            recordsToKeep = policy.getMaxRecords();
            if (recordsToKeep != null && recordsToKeep == 0) {
                LOGGER.trace("objectResults.recordsToKeep is 0, will skip storing operationExecutions");
                keepNoExecutions = true;
            }
            if (policy.getMaxAge() != null) {
                XMLGregorianCalendar limit = XmlTypeConverter.addDuration(
                        XmlTypeConverter.createXMLGregorianCalendar(new Date()), policy.getMaxAge().negate());
                deleteBefore = XmlTypeConverter.toMillis(limit);
            } else {
                deleteBefore = null;
            }
        } else {
            recordsToKeep = DEFAULT_NUMBER_OF_RESULTS_TO_KEEP;
            deleteBefore = null;
        }

        String taskOid = executionToAdd.getTaskRef() != null ? executionToAdd.getTaskRef().getOid() : null;
        if (executionToAdd.getStatus() == OperationResultStatusType.SUCCESS && skipWhenSuccess) {
            // We want to skip writing operationExecution. But let's check if there are some older non-success results
            // related to the current task
            if (taskOid != null) {
                boolean hasNonSuccessFromCurrentTask = object.asObjectable().getOperationExecution().stream()
                        .anyMatch(oe -> oe.getTaskRef() != null && taskOid.equals(oe.getTaskRef().getOid()) &&
                                oe.getStatus() != OperationResultStatusType.SUCCESS);
                if (hasNonSuccessFromCurrentTask) {
                    LOGGER.trace("Cannot skip OperationExecution recording because there's an older non-success record from the current task");
                } else {
                    LOGGER.trace("Skipping OperationExecution recording because status is SUCCESS and skipWhenSuccess is true "
                            + "(and no older non-success records for current task {} exist)", taskOid);
                    return;
                }
            } else {
                LOGGER.trace("Skipping OperationExecution recording because status is SUCCESS and skipWhenSuccess is true");
                return;
            }
        }
        List<OperationExecutionType> executionsToDelete = new ArrayList<>();
        List<OperationExecutionType> executions = new ArrayList<>(object.asObjectable().getOperationExecution());
        // delete all executions related to current task and all old ones
        for (Iterator<OperationExecutionType> iterator = executions.iterator(); iterator.hasNext(); ) {
            OperationExecutionType execution = iterator.next();
            boolean isPreviousTaskResult = taskOid != null && execution.getTaskRef() != null && taskOid.equals(execution.getTaskRef().getOid());
            boolean isOld = deleteBefore != null && XmlTypeConverter.toMillis(execution.getTimestamp()) < deleteBefore;
            if (isPreviousTaskResult || isOld) {
                executionsToDelete.add(execution);
                iterator.remove();
            }
        }

        // delete all surplus executions
        if (recordsToKeep != null && executions.size() > recordsToKeep - 1) {
            if (keepNoExecutions) {
                executionsToDelete.addAll(executions);
            } else {
                executions.sort(Comparator.nullsFirst(Comparator.comparing(e -> XmlTypeConverter.toDate(e.getTimestamp()))));
                executionsToDelete.addAll(executions.subList(0, executions.size() - (recordsToKeep - 1)));
            }
        }
        // construct and execute the delta
        Class<? extends ObjectType> objectClass = object.asObjectable().getClass();
        List<ItemDelta<?, ?>> deltas = new ArrayList<>();
        if (!keepNoExecutions) {
            deltas.add(prismContext.deltaFor(objectClass)
                    .item(ObjectType.F_OPERATION_EXECUTION)
                    .add(executionToAdd)
                    .asItemDelta());
        }
        if (!executionsToDelete.isEmpty()) {
            deltas.add(prismContext.deltaFor(objectClass)
                    .item(ObjectType.F_OPERATION_EXECUTION)
                    .delete(PrismContainerValue.toPcvList(CloneUtil.cloneCollectionMembers(executionsToDelete)))
                    .asItemDelta());
        }
        LOGGER.trace("Operation execution delta:\n{}", DebugUtil.debugDumpLazily(deltas));
        try {
            if (!deltas.isEmpty()) {
                repositoryService.modifyObject(objectClass, oid, deltas, result);
            }
        } catch (ObjectNotFoundException e) {
            if (!deletedOk) {
                throw e;
            } else {
                LOGGER.trace("Object {} deleted but this was expected.", oid);
                result.deleteLastSubresultIfError();
            }
        }
    }

    private void setOperationContext(OperationExecutionType operation,
            OperationResultStatusType overallStatus, XMLGregorianCalendar now, String channel, Task task) {
        if (task instanceof RunningTask && ((RunningTask) task).getParentForLightweightAsynchronousTask() != null) {
            task = ((RunningTask) task).getParentForLightweightAsynchronousTask();
        }
        if (task.isPersistent()) {
            operation.setTaskRef(task.getSelfReference());
        }
        operation.setStatus(overallStatus);
        operation.setInitiatorRef(ObjectTypeUtil.createObjectRef(task.getOwner(), prismContext));        // TODO what if the real initiator is different? (e.g. when executing approved changes)
        operation.setChannel(channel);
        operation.setTimestamp(now);
    }

    private <F extends ObjectType> ObjectDeltaOperationType createObjectDeltaOperation(LensObjectDeltaOperation<F> deltaOperation) {
        ObjectDeltaOperationType odo;
        try {
            odo = simplifyOperation(deltaOperation).toLensObjectDeltaOperationType().getObjectDeltaOperation();
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't create operation information", e);
            odo = new ObjectDeltaOperationType();
            OperationResult r = new OperationResult(Clockwork.class.getName() + ".createObjectDeltaOperation");
            r.recordFatalError("Couldn't create operation information: " + e.getMessage(), e);
            odo.setExecutionResult(r.createOperationResultType());
        }
        return odo;
    }

    private <F extends ObjectType> LensObjectDeltaOperation<F> simplifyOperation(ObjectDeltaOperation<F> operation) {
        LensObjectDeltaOperation<F> rv = new LensObjectDeltaOperation<>();
        rv.setObjectDelta(simplifyDelta(operation.getObjectDelta()));
        rv.setExecutionResult(OperationResult.keepRootOnly(operation.getExecutionResult()));
        rv.setObjectName(operation.getObjectName());
        rv.setResourceName(operation.getResourceName());
        rv.setResourceOid(operation.getResourceOid());
        return rv;
    }

    private <F extends ObjectType> ObjectDelta<F> simplifyDelta(ObjectDelta<F> delta) {
        return prismContext.deltaFactory().object().create(delta.getObjectTypeClass(), delta.getChangeType());
    }

    private <F extends ObjectType> HookOperationMode triggerReconcileAffected(LensContext<F> context, Task task, OperationResult result) throws SchemaException {
        // check applicability
        if (!ModelExecuteOptions.isReconcileAffected(context.getOptions())) {
            return HookOperationMode.FOREGROUND;
        }
        if (context.getFocusClass() == null || !RoleType.class.isAssignableFrom(context.getFocusClass())) {
            LOGGER.warn("ReconcileAffected requested but not available for {}. Doing nothing.", context.getFocusClass());
            return HookOperationMode.FOREGROUND;
        }

        // check preconditions
        if (context.getFocusContext() == null) {
            throw new IllegalStateException("No focus context when expected it");
        }
        PrismObject<RoleType> role = (PrismObject) context.getFocusContext().getObjectAny();
        if (role == null) {
            throw new IllegalStateException("No role when expected it");
        }

        // preparing the recompute/reconciliation task
        Task reconTask;
        if (task.isPersistent()) {
            reconTask = task.createSubtask();
        } else {
            reconTask = task;
        }
        assert !reconTask.isPersistent();

        // creating object query
        PrismPropertyDefinition propertyDef = prismContext.getSchemaRegistry()
                .findPropertyDefinitionByElementName(SchemaConstants.MODEL_EXTENSION_OBJECT_QUERY);
        PrismReferenceValue referenceValue = prismContext.itemFactory().createReferenceValue(context.getFocusContext().getOid(), RoleType.COMPLEX_TYPE);
        ObjectFilter refFilter = prismContext.queryFor(FocusType.class)
                .item(FocusType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF).ref(referenceValue)
                .buildFilter();
        SearchFilterType filterType = prismContext.getQueryConverter().createSearchFilterType(refFilter);
        QueryType queryType = new QueryType();
        queryType.setFilter(filterType);
        PrismProperty<QueryType> property = propertyDef.instantiate();
        property.setRealValue(queryType);
        reconTask.addExtensionProperty(property);

        // other parameters
        reconTask.setName("Recomputing users after changing role " + role.asObjectable().getName());
        reconTask.setBinding(TaskBinding.LOOSE);
        reconTask.setInitialExecutionStatus(TaskExecutionStatus.RUNNABLE);
        reconTask.setHandlerUri(RecomputeTaskHandler.HANDLER_URI);
        reconTask.setCategory(TaskCategory.RECOMPUTATION);
        taskManager.switchToBackground(reconTask, result);
        result.setBackgroundTaskOid(reconTask.getOid());
        result.recordStatus(OperationResultStatus.IN_PROGRESS, "Reconciliation task switched to background");
        return HookOperationMode.BACKGROUND;
    }

    // "overallResult" covers the whole clockwork run
    // while "result" is - most of the time - related to the current clockwork click
    private <F extends ObjectType> void audit(LensContext<F> context, AuditEventStage stage, Task task, OperationResult result, OperationResult overallResult) throws SchemaException {
        if (context.isLazyAuditRequest()) {
            if (stage == AuditEventStage.REQUEST) {
                // We skip auditing here, we will do it before execution
            } else if (stage == AuditEventStage.EXECUTION) {
                Collection<ObjectDeltaOperation<? extends ObjectType>> unauditedExecutedDeltas = context.getUnauditedExecutedDeltas();
                if ((unauditedExecutedDeltas == null || unauditedExecutedDeltas.isEmpty())) {
                    // No deltas, nothing to audit in this wave
                    return;
                }
                if (!context.isRequestAudited()) {
                    auditEvent(context, AuditEventStage.REQUEST, context.getStats().getRequestTimestamp(), false, task, result, overallResult);
                }
                auditEvent(context, stage, null, false, task, result, overallResult);
            }
        } else {
            auditEvent(context, stage, null, false, task, result, overallResult);
        }
    }

    /**
     * Make sure that at least one execution is audited if a request was already audited. We don't want
     * request without execution in the audit logs.
     */
    private <F extends ObjectType> void auditFinalExecution(LensContext<F> context, Task task, OperationResult result,
            OperationResult overallResult) throws SchemaException {
        if (!context.isRequestAudited()) {
            return;
        }
        if (context.isExecutionAudited()) {
            return;
        }
        auditEvent(context, AuditEventStage.EXECUTION, null, true, task, result, overallResult);
    }

    private <F extends ObjectType> void processClockworkException(LensContext<F> context, Throwable e, Task task, OperationResult result, OperationResult overallResult)
            throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
        LOGGER.trace("Processing clockwork exception {}", e.toString());
        result.recordFatalErrorNotFinish(e);
        auditEvent(context, AuditEventStage.EXECUTION, null, true, task, result, overallResult);
        recordOperationExecution(context, e, task, result);
        LensUtil.reclaimSequences(context, repositoryService, task, result);
        result.recordEnd();
    }

    // "overallResult" covers the whole clockwork run
    // while "result" is - most of the time - related to the current clockwork click
    //
    // We provide "result" here just for completeness - if any of the called methods would like to record to it.
    private <F extends ObjectType> void auditEvent(LensContext<F> context, AuditEventStage stage,
            XMLGregorianCalendar timestamp, boolean alwaysAudit, Task task, OperationResult result,
            OperationResult overallResult) throws SchemaException {

        PrismObject<? extends ObjectType> primaryObject;
        ObjectDelta<? extends ObjectType> primaryDelta;
        if (context.getFocusContext() != null) {
            primaryObject = context.getFocusContext().getObjectOld();
            if (primaryObject == null) {
                primaryObject = context.getFocusContext().getObjectNew();
            }
            primaryDelta = context.getFocusContext().getDelta();
        } else {
            Collection<LensProjectionContext> projectionContexts = context.getProjectionContexts();
            if (projectionContexts == null || projectionContexts.isEmpty()) {
                throw new IllegalStateException("No focus and no projections in "+context);
            }
            if (projectionContexts.size() > 1) {
                throw new IllegalStateException("No focus and more than one projection in "+context);
            }
            LensProjectionContext projection = projectionContexts.iterator().next();
            primaryObject = projection.getObjectOld();
            if (primaryObject == null) {
                primaryObject = projection.getObjectNew();
            }
            primaryDelta = projection.getDelta();
        }

        AuditEventType eventType;
        if (primaryDelta == null) {
            eventType = AuditEventType.SYNCHRONIZATION;
        } else if (primaryDelta.isAdd()) {
            eventType = AuditEventType.ADD_OBJECT;
        } else if (primaryDelta.isModify()) {
            eventType = AuditEventType.MODIFY_OBJECT;
        } else if (primaryDelta.isDelete()) {
            eventType = AuditEventType.DELETE_OBJECT;
        } else {
            throw new IllegalStateException("Unknown state of delta "+primaryDelta);
        }

        AuditEventRecord auditRecord = new AuditEventRecord(eventType, stage);
        auditRecord.setRequestIdentifier(context.getRequestIdentifier());

        boolean recordResourceOids;
        List<SystemConfigurationAuditEventRecordingPropertyType> propertiesToRecord;
        SystemConfigurationType config = context.getSystemConfigurationType();
        if (config != null && config.getAudit() != null && config.getAudit().getEventRecording() != null) {
            SystemConfigurationAuditEventRecordingType eventRecording = config.getAudit().getEventRecording();
            recordResourceOids = Boolean.TRUE.equals(eventRecording.isRecordResourceOids());
            propertiesToRecord = eventRecording.getProperty();
        } else {
            recordResourceOids = false;
            propertiesToRecord = emptyList();
        }

        if (primaryObject != null) {
            auditRecord.setTarget(primaryObject, prismContext);
            if (recordResourceOids) {
                if (primaryObject.getRealValue() instanceof FocusType) {
                    FocusType focus = (FocusType) primaryObject.getRealValue();
                    for (ObjectReferenceType shadowRef : focus.getLinkRef()) {
                        LensProjectionContext projectionContext = context.findProjectionContextByOid(shadowRef.getOid());
                        if (projectionContext != null && StringUtils.isNotBlank(projectionContext.getResourceOid())) {
                            auditRecord.addResourceOid(projectionContext.getResourceOid());
                        }
                    }
                } else if (primaryObject.getRealValue() instanceof ShadowType) {
                    ObjectReferenceType resource = ((ShadowType) primaryObject.getRealValue()).getResourceRef();
                    if (resource != null && resource.getOid() != null) {
                        auditRecord.addResourceOid(resource.getOid());
                    }
                }
            }
        }

        auditRecord.setChannel(context.getChannel());

        // This is a brutal hack -- FIXME: create some "compute in-depth preview" method on operation result
        OperationResult clone = overallResult.clone(2, false);
        for (OperationResult subresult : clone.getSubresults()) {
            subresult.computeStatusIfUnknown();
        }
        clone.computeStatus();

        if (stage == AuditEventStage.REQUEST) {
            Collection<ObjectDeltaOperation<? extends ObjectType>> clonedDeltas = ObjectDeltaOperation.cloneDeltaCollection(context.getPrimaryChanges());
            checkNamesArePresent(clonedDeltas, primaryObject);
            auditRecord.addDeltas(clonedDeltas);
            if (auditRecord.getTarget() == null) {
                auditRecord.setTarget(ModelImplUtils.determineAuditTargetDeltaOps(clonedDeltas, context.getPrismContext()));
            }
        } else if (stage == AuditEventStage.EXECUTION) {
            auditRecord.setOutcome(clone.getStatus());
            Collection<ObjectDeltaOperation<? extends ObjectType>> unauditedExecutedDeltas = context.getUnauditedExecutedDeltas();
            if (!alwaysAudit && (unauditedExecutedDeltas == null || unauditedExecutedDeltas.isEmpty())) {
                // No deltas, nothing to audit in this wave
                return;
            }
            Collection<ObjectDeltaOperation<? extends ObjectType>> clonedDeltas = ObjectDeltaOperation.cloneCollection(unauditedExecutedDeltas);
            checkNamesArePresent(clonedDeltas, primaryObject);
            auditRecord.addDeltas(clonedDeltas);
        } else {
            throw new IllegalStateException("Unknown audit stage "+stage);
        }

        if (timestamp != null) {
            auditRecord.setTimestamp(XmlTypeConverter.toMillis(timestamp));
        }

        addRecordMessage(auditRecord, clone.getMessage());

        for (SystemConfigurationAuditEventRecordingPropertyType property : propertiesToRecord) {
            String name = property.getName();
            if (StringUtils.isBlank(name)) {
                throw new IllegalArgumentException("Name of SystemConfigurationAuditEventRecordingPropertyType is empty or null in " + property);
            }
            ExpressionType expression = property.getExpression();
            if (expression != null) {
                ExpressionVariables variables = new ExpressionVariables();
                variables.put(ExpressionConstants.VAR_TARGET, primaryObject, PrismObject.class);
                variables.put(ExpressionConstants.VAR_AUDIT_RECORD, auditRecord, AuditEventRecord.class);
                String shortDesc = "value for custom column of audit table";
                try {
                    Collection<String> values = ExpressionUtil.evaluateStringExpression(variables, prismContext, expression, context.getPrivilegedExpressionProfile(), expressionFactory, shortDesc, task, result);
                    if (values != null && !values.isEmpty()) {
                        if (values.size() > 1) {
                            throw new IllegalArgumentException("Collection of expression result contains more as one value");
                        }
                        auditRecord.getCustomColumnProperty().put(name, values.iterator().next());
                    }
                } catch (CommonException e) {
                    LOGGER.error("Couldn't evaluate Expression " + expression.toString(), e);
                }
            }
        }

        auditHelper.audit(auditRecord, context.getNameResolver(), task, result);

        if (stage == AuditEventStage.EXECUTION) {
            // We need to clean up so these deltas will not be audited again in next wave
            context.markExecutedDeltasAudited();
            context.setExecutionAudited(true);
        } else {
            assert stage == AuditEventStage.REQUEST;
            context.setRequestAudited(true);
        }
    }

    private void checkNamesArePresent(Collection<ObjectDeltaOperation<? extends ObjectType>> deltas, PrismObject<? extends ObjectType> primaryObject) {
        if (primaryObject != null) {
            for (ObjectDeltaOperation<? extends ObjectType> delta : deltas) {
                if (delta.getObjectName() == null) {
                    delta.setObjectName(primaryObject.getName());
                }
            }
        }
    }

    /**
     * Adds a message to the record by pulling the messages from individual delta results.
     */
    private void addRecordMessage(AuditEventRecord auditRecord, String message) {
        if (auditRecord.getMessage() != null) {
            return;
        }
        if (!StringUtils.isEmpty(message)) {
            auditRecord.setMessage(message);
            return;
        }
        Collection<ObjectDeltaOperation<? extends ObjectType>> deltas = auditRecord.getDeltas();
        if (deltas == null || deltas.isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (ObjectDeltaOperation<? extends ObjectType> delta: deltas) {
            OperationResult executionResult = delta.getExecutionResult();
            if (executionResult != null) {
                String deltaMessage = executionResult.getMessage();
                if (!StringUtils.isEmpty(deltaMessage)) {
                    if (sb.length() != 0) {
                        sb.append("; ");
                    }
                    sb.append(deltaMessage);
                }
            }
        }
        auditRecord.setMessage(sb.toString());
    }

    public static void throwException(Throwable e) throws ObjectAlreadyExistsException, ObjectNotFoundException {
        if (e instanceof ObjectAlreadyExistsException) {
            throw (ObjectAlreadyExistsException)e;
        } else if (e instanceof ObjectNotFoundException) {
            throw (ObjectNotFoundException)e;
        } else if (e instanceof SystemException) {
            throw (SystemException)e;
        } else {
            throw new SystemException("Unexpected exception "+e.getClass()+" "+e.getMessage(), e);
        }
    }

    /**
     * Logs the entire operation in a human-readable fashion.
     */
    private <F extends ObjectType> void logFinalReadable(LensContext<F> context, Task task, OperationResult result) throws SchemaException {
        if (!LOGGER.isDebugEnabled()) {
            return;
        }

        // a priori: sync delta
        boolean hasSyncDelta = false;
        for (LensProjectionContext projectionContext: context.getProjectionContexts()) {
            ObjectDelta<ShadowType> syncDelta = projectionContext.getSyncDelta();
            if (syncDelta != null) {
                hasSyncDelta = true;
            }
        }

        Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas = context.getExecutedDeltas();
        if (!hasSyncDelta && executedDeltas == null || executedDeltas.isEmpty()) {
            // Not worth mentioning
            return;
        }

        StringBuilder sb = new StringBuilder();
        String channel = context.getChannel();
        if (channel != null) {
            sb.append("Channel: ").append(channel).append("\n");
        }


        if (hasSyncDelta) {
            sb.append("Triggered by synchronization delta\n");
            for (LensProjectionContext projectionContext: context.getProjectionContexts()) {
                ObjectDelta<ShadowType> syncDelta = projectionContext.getSyncDelta();
                if (syncDelta != null) {
                    sb.append(syncDelta.debugDump(1));
                    sb.append("\n");
                }
                DebugUtil.debugDumpLabel(sb, "Situation", 1);
                sb.append(" ");
                sb.append(projectionContext.getSynchronizationSituationDetected());
                sb.append(" -> ");
                sb.append(projectionContext.getSynchronizationSituationResolved());
                sb.append("\n");
            }
        }
        for (LensProjectionContext projectionContext: context.getProjectionContexts()) {
            if (projectionContext.isSyncAbsoluteTrigger()) {
                sb.append("Triggered by absolute state of ").append(projectionContext.getHumanReadableName());
                sb.append(": ");
                sb.append(projectionContext.getSynchronizationSituationDetected());
                sb.append(" -> ");
                sb.append(projectionContext.getSynchronizationSituationResolved());
                sb.append("\n");
            }
        }

        // focus primary
        LensFocusContext<F> focusContext = context.getFocusContext();
        if (focusContext != null) {
            ObjectDelta<F> focusPrimaryDelta = focusContext.getPrimaryDelta();
            if (focusPrimaryDelta != null) {
                sb.append("Triggered by focus primary delta\n");
                DebugUtil.indentDebugDump(sb, 1);
                sb.append(focusPrimaryDelta.toString());
                sb.append("\n");
            }
        }

        // projection primary
        Collection<ObjectDelta<ShadowType>> projPrimaryDeltas = new ArrayList<>();
        for (LensProjectionContext projectionContext: context.getProjectionContexts()) {
            ObjectDelta<ShadowType> projPrimaryDelta = projectionContext.getPrimaryDelta();
            if (projPrimaryDelta != null) {
                projPrimaryDeltas.add(projPrimaryDelta);
            }
        }
        if (!projPrimaryDeltas.isEmpty()) {
            sb.append("Triggered by projection primary delta\n");
            for (ObjectDelta<ShadowType> projDelta: projPrimaryDeltas) {
                DebugUtil.indentDebugDump(sb, 1);
                sb.append(projDelta.toString());
                sb.append("\n");
            }
        }

        if (focusContext != null) {
            sb.append("Focus: ").append(focusContext.getHumanReadableName()).append("\n");
        }
        if (!context.getProjectionContexts().isEmpty()) {
            sb.append("Projections (").append(context.getProjectionContexts().size()).append("):\n");
            for (LensProjectionContext projectionContext: context.getProjectionContexts()) {
                DebugUtil.indentDebugDump(sb, 1);
                sb.append(projectionContext.getHumanReadableName());
                if (projectionContext.isTombstone()) {
                    sb.append(" THOMBSTONE");
                }
                sb.append(": ");
                sb.append(projectionContext.getSynchronizationPolicyDecision());
                sb.append("\n");
            }
        }

        if (executedDeltas == null || executedDeltas.isEmpty()) {
            sb.append("Executed: nothing\n");
        } else {
            sb.append("Executed:\n");
            for (ObjectDeltaOperation<? extends ObjectType> executedDelta: executedDeltas) {
                ObjectDelta<? extends ObjectType> delta = executedDelta.getObjectDelta();
                OperationResult deltaResult = executedDelta.getExecutionResult();
                DebugUtil.indentDebugDump(sb, 1);
                sb.append(delta.toString());
                sb.append(": ");
                sb.append(deltaResult.getStatus());
                sb.append("\n");
            }
        }

        LOGGER.debug("\n###[ CLOCKWORK SUMMARY ]######################################\n{}" +
                       "##############################################################",
                sb.toString());
    }

}
