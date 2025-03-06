/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.controller;

import static com.evolveum.midpoint.schema.GetOperationOptions.readOnly;
import static com.evolveum.midpoint.schema.result.OperationResult.HANDLE_OBJECT_FOUND;
import static com.evolveum.midpoint.util.MiscUtil.argCheck;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import static com.evolveum.midpoint.util.DebugUtil.lazy;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.cases.api.util.QueryUtils;
import com.evolveum.midpoint.model.api.BulkActionExecutionOptions;
import com.evolveum.midpoint.model.impl.scripting.BulkActionsExecutor;
import com.evolveum.midpoint.schema.config.ExecuteScriptConfigItem;
import com.evolveum.midpoint.schema.processor.BareResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchemaRegistry;
import com.evolveum.midpoint.schema.util.*;
import com.evolveum.midpoint.model.impl.simulation.ProcessedObjectImpl;

import com.evolveum.midpoint.security.api.SecurityUtil;

import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.cases.api.CaseManager;
import com.evolveum.midpoint.certification.api.CertificationManager;
import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.api.*;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipalManager;
import com.evolveum.midpoint.model.api.hooks.HookRegistry;
import com.evolveum.midpoint.repo.common.AuditHelper;
import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.model.impl.importer.ObjectImporter;
import com.evolveum.midpoint.model.impl.lens.*;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.impl.sync.tasks.imp.ImportFromResourceLauncher;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.DiffUtil;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.provisioning.api.DiscoveredConfiguration;
import com.evolveum.midpoint.provisioning.api.EventDispatcher;
import com.evolveum.midpoint.provisioning.api.ExternalResourceEvent;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.repo.common.activity.TaskActivityManager;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.ObjectTypes.ObjectManager;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultRunner;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.cases.ApprovalUtils;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.security.enforcer.api.AuthorizationParameters;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.CompareResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityCollectionType;
import com.evolveum.prism.xml.ns._public.types_3.EvaluationTimeType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * This used to be an interface, but it was switched to class for simplicity. I
 * don't expect that the implementation of the controller will be ever replaced.
 * In extreme case the whole Model will be replaced by a different
 * implementation, but not just the controller.
 * <p/>
 * However, the common way to extend the functionality will be the use of hooks
 * that are implemented here.
 * <p/>
 * Great deal of code is copied from the old ModelControllerImpl.
 *
 * @author lazyman
 * @author Radovan Semancik
 * <p>
 * Note: don't autowire this bean by implementing class (ModelController), as it is proxied by Spring AOP.
 * Use its interfaces instead.
 */
@Component
public class ModelController implements ModelService, TaskService, CaseService, BulkActionsService, AccessCertificationService {

    // Constants for OperationResult
    private static final String CLASS_NAME_WITH_DOT = ModelController.class.getName() + ".";
    static final String RESOLVE_REFERENCE = CLASS_NAME_WITH_DOT + "resolveReference";
    private static final String OP_APPLY_PROVISIONING_DEFINITION = CLASS_NAME_WITH_DOT + "applyProvisioningDefinition";
    static final String OP_REEVALUATE_SEARCH_FILTERS = CLASS_NAME_WITH_DOT + "reevaluateSearchFilters";
    private static final String OP_AUTHORIZE_CHANGE_EXECUTION_START = CLASS_NAME_WITH_DOT + "authorizeChangeExecutionStart";
    @VisibleForTesting
    public static final String OP_HANDLE_OBJECT_FOUND = CLASS_NAME_WITH_DOT + HANDLE_OBJECT_FOUND;

    private static final int OID_GENERATION_ATTEMPTS = 5;

    private static final Trace LOGGER = TraceManager.getTrace(ModelController.class);

    private static final Trace OP_LOGGER = TraceManager.getTrace(ModelService.OPERATION_LOGGER_NAME);

    @Autowired private Clockwork clockwork;
    @Autowired private PrismContext prismContext;
    @Autowired private ProvisioningService provisioning;
    @Autowired private ModelObjectResolver objectResolver;
    @Autowired private ImportFromResourceLauncher importFromResourceLauncher;
    @Autowired private ObjectImporter objectImporter;
    @Autowired private HookRegistry hookRegistry;
    @Autowired private TaskManager taskManager;
    @Autowired private TaskActivityManager activityManager;
    @Autowired private BulkActionsExecutor bulkActionsExecutor;
    @Autowired private AuditHelper auditHelper;
    @Autowired private SecurityEnforcer securityEnforcer;
    @Autowired private SecurityContextManager securityContextManager;
    @Autowired private GuiProfiledPrincipalManager focusProfileService;
    @Autowired private Protector protector;
    @Autowired private LocalizationService localizationService;
    @Autowired private ContextFactory contextFactory;
    @Autowired private SchemaTransformer schemaTransformer;
    @Autowired private ObjectMerger objectMerger;
    @Autowired private SystemObjectCache systemObjectCache;
    @Autowired private ClockworkMedic clockworkMedic;
    @Autowired private ClockworkAuditHelper clockworkAuditHelper;
    @Autowired private EventDispatcher dispatcher;
    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService cacheRepositoryService;

    @Autowired(required = false)                        // not required in all circumstances
    private CaseManager caseManager;

    @Autowired(required = false)                        // not required in all circumstances
    private CertificationManager certificationManager;
    @Autowired private OperationalDataManager operationalDataManager;
    @Autowired private ResourceSchemaRegistry resourceSchemaRegistry;

    public ModelObjectResolver getObjectResolver() {
        return objectResolver;
    }

    private @NotNull CaseManager getCaseManagerRequired() {
        return Objects.requireNonNull(caseManager, "Case manager is not present");
    }

    private CertificationManager getCertificationManagerRequired() {
        return Objects.requireNonNull(certificationManager, "Certification manager is not present");
    }

    @NotNull
    @Override
    public <T extends ObjectType> PrismObject<T> getObject(
            @NotNull Class<T> clazz,
            @NotNull String oid,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> rawOptions,
            @NotNull Task task,
            @NotNull OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        Validate.notEmpty(oid, "Object oid must not be null or empty.");
        Validate.notNull(parentResult, "Operation result must not be null.");
        Validate.notNull(clazz, "Object class must not be null.");

        enterModelMethod();

        PrismObject<T> object;

        OP_LOGGER.trace("MODEL OP enter getObject({},{},{})", clazz.getSimpleName(), oid, rawOptions);

        OperationResult result = parentResult.subresult(GET_OBJECT)
                .setMinor()
                .addParam("oid", oid)
                .addArbitraryObjectCollectionAsParam("options", rawOptions)
                .addParam("class", clazz)
                .build();

        try {
            var parsedOptions = preProcessOptionsSecurity(rawOptions, task, result);
            GetOperationOptions rootOptions = parsedOptions.getRootOptions();

            if (GetOperationOptions.isRaw(rootOptions)) { // MID-2218
                QNameUtil.setTemporarilyTolerateUndeclaredPrefixes(true);
            }
            ModelImplUtils.clearRequestee(task);

            //noinspection unchecked
            object = (PrismObject<T>) objectResolver
                    .getObject(clazz, oid, parsedOptions.getCollection(), task, result)
                    .asPrismObject();

            object = schemaTransformer.applySchemasAndSecurityToObject(object, parsedOptions, task, result);
            executeResolveOptions(object.asObjectable(), parsedOptions, task, result);

        } catch (Throwable t) {
            OP_LOGGER.debug("MODEL OP error getObject({},{},{}): {}: {}",
                    clazz.getSimpleName(), oid, rawOptions, t.getClass().getSimpleName(), t.getMessage());
            ModelImplUtils.recordException(result, t);
            throw t;
        } finally {
            result.close();
            result.cleanup();
            QNameUtil.setTemporarilyTolerateUndeclaredPrefixes(false);
            exitModelMethod();
        }

        OP_LOGGER.debug("MODEL OP exit getObject({},{},{}): {}", clazz.getSimpleName(), oid, rawOptions, object);
        OP_LOGGER.trace("MODEL OP exit getObject({},{},{}):\n{}", clazz.getSimpleName(), oid, rawOptions, object.debugDumpLazily(1));
        return object;
    }

    private void executeResolveOptions(
            @NotNull Containerable base,
            @NotNull ParsedGetOperationOptions parsedOptions,
            Task task,
            OperationResult result) {
        if (!parsedOptions.isEmpty()) {
            new ResolveOptionExecutor(parsedOptions, task, objectResolver, schemaTransformer)
                    .execute(base, result);
        }
    }

    @Override
    public Collection<ObjectDeltaOperation<? extends ObjectType>> executeChanges(
            Collection<ObjectDelta<? extends ObjectType>> deltas, ModelExecuteOptions options,
            Task task, Collection<ProgressListener> statusListeners, OperationResult parentResult)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {

        enterModelMethod();

        OperationResult result = parentResult.subresult(EXECUTE_CHANGES)
                .addArbitraryObjectAsParam(OperationResult.PARAM_OPTIONS, options)
                .build();

        try {

            // Search filters treatment: if reevaluation is requested, we have to deal with three cases:
            // 1) for ADD operation: filters contained in object-to-be-added -> these are treated here
            // 2) for MODIFY operation: filters contained in existing object (not touched by deltas) -> these are treated after the modify operation
            // 3) for MODIFY operation: filters contained in deltas -> these have to be treated here, because if OID is missing from such a delta, the change would be rejected by the repository
            if (ModelExecuteOptions.isReevaluateSearchFilters(options)) {
                for (ObjectDelta<? extends ObjectType> delta : deltas) {
                    ModelImplUtils.resolveReferences(
                            delta, cacheRepositoryService,
                            false, true, EvaluationTimeType.IMPORT,
                            true, result);
                }
            } else if (ModelExecuteOptions.isIsImport(options)) {
                // if plain import is requested, we simply evaluate filters in ADD operation (and we do not force reevaluation if OID is already set)
                for (ObjectDelta<? extends ObjectType> delta : deltas) {
                    if (delta.isAdd()) {
                        ModelImplUtils.resolveReferences(
                                delta.getObjectToAdd(), cacheRepositoryService,
                                false, false, EvaluationTimeType.IMPORT,
                                true, result);
                    }
                }
            }

            // Make sure everything is encrypted as needed before logging anything.
            // But before that we need to make sure that we have proper definition, otherwise we
            // might miss some encryptable data in dynamic schemas
            applyDefinitions(deltas, options, task, result);
            ModelImplUtils.encrypt(deltas, protector, options, result);

            computePolyStrings(deltas);

            LOGGER.debug("MODEL.executeChanges with options={}:\n{}", options, lazy(() -> getDeltasOnSeparateLines(deltas)));
            LOGGER.trace("MODEL.executeChanges(\n  deltas:\n{}\n  options:{}", DebugUtil.debugDumpLazily(deltas, 2), options);

            if (InternalsConfig.consistencyChecks) {
                OperationResultRunner.run(result, () -> {
                    for (ObjectDelta<? extends ObjectType> delta : deltas) {
                        delta.checkConsistence();
                    }
                });
            }

            if (ModelExecuteOptions.isRaw(options)) {
                return new RawChangesExecutor(deltas, options, task, result)
                        .execute(result);
            } else {
                return executeChangesNonRaw(deltas, options, task, statusListeners, result);
            }

            // Note: caches are invalidated automatically via RepositoryCache.invalidateCacheEntries method

        } catch (Throwable t) {
            ModelImplUtils.recordException(result, t);
            throw t;
        } finally {
            result.close();
            exitModelMethod();
        }
    }

    private String getDeltasOnSeparateLines(Collection<? extends ObjectDelta<?>> deltas) {
        return deltas.stream()
                .map(delta -> " - " + delta)
                .collect(Collectors.joining("\n"));
    }

    private Collection<ObjectDeltaOperation<? extends ObjectType>> executeChangesNonRaw(
            Collection<ObjectDelta<? extends ObjectType>> deltas, ModelExecuteOptions options, Task task,
            Collection<ProgressListener> statusListeners, OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException, SecurityViolationException, PolicyViolationException, ObjectAlreadyExistsException {

        LensContext<? extends ObjectType> context = contextFactory.createContext(deltas, options, task, result);

        authorizeExecutionStart(context, options, task, result);

        if (ModelExecuteOptions.isReevaluateSearchFilters(options)) {
            String m = "ReevaluateSearchFilters option is not fully supported for non-raw operations yet. "
                    + "Filters already present in the object will not be touched.";
            LOGGER.warn("{} Context = {}", m, context.debugDump());
            result.createSubresult(CLASS_NAME_WITH_DOT + "reevaluateSearchFilters").recordWarning(m);
        }

        context.setProgressListeners(statusListeners);
        // Note: Request authorization happens inside clockwork

        // We generate focus OID even for generic repo, and when access metadata are not concerned.
        // It is maybe not strictly needed for these cases, but this allows us to rely on the fact that the OID is always there.
        generateFocusOidIfNeeded(context, result);

        operationalDataManager.addExternalAssignmentProvenance(context, task);

        clockwork.run(context, task, result);

        // prepare return value
        Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas = new ArrayList<>();
        if (context.getFocusContext() != null) {
            executedDeltas.addAll(context.getFocusContext().getExecutedDeltas());
        }
        for (LensProjectionContext projectionContext : context.getProjectionContexts()) {
            executedDeltas.addAll(projectionContext.getExecutedDeltas());
        }

        if (context.hasExplosiveProjection()) {
            PrismObject<? extends ObjectType> focus =
                    Objects.requireNonNull(context.getFocusContext().getObjectAny(), "no focus object");

            LOGGER.debug("Recomputing {} because there was explosive projection", focus);

            LensContext<? extends ObjectType> recomputeContext =
                    contextFactory.createRecomputeContext(focus, options, task, result);
            recomputeContext.setDoReconciliationForAllProjections(true);
            LOGGER.trace("Recomputing {}, context:\n{}", focus, recomputeContext.debugDumpLazily());
            clockwork.run(recomputeContext, task, result);
        }

        cleanupOperationResult(result);
        return executedDeltas;
    }

    private void generateFocusOidIfNeeded(LensContext<? extends ObjectType> context, OperationResult result)
            throws SchemaException {
        LensFocusContext<? extends ObjectType> focusContext = context.getFocusContext();
        if (focusContext == null) {
            return;
        }
        var primaryDelta = focusContext.getPrimaryDelta();
        if (primaryDelta == null || !primaryDelta.isAdd()) {
            return;
        }
        var objectToAdd = primaryDelta.getObjectToAdd().asObjectable();
        if (objectToAdd.getOid() != null) {
            return;
        }
        String oid = getNewOid(objectToAdd.getClass(), result);
        focusContext.modifyPrimaryDelta(d -> d.setOid(oid));
    }

    private String getNewOid(Class<? extends ObjectType> type, OperationResult result) {
        for (int attempt = 1; attempt <= OID_GENERATION_ATTEMPTS; attempt++) {
            var randomOid = UUID.randomUUID().toString();
            try {
                cacheRepositoryService.getObject(type, randomOid, GetOperationOptions.createAllowNotFoundCollection(), result);
                LOGGER.info("Random UUID is not that random? Attempt {} of {}: {}", attempt, OID_GENERATION_ATTEMPTS, randomOid);
            } catch (ObjectNotFoundException e) {
                result.clearLastSubresultError(); // e.g. because of tests
                return randomOid; // This is the good case
            } catch (SchemaException e) {
                LoggingUtils.logException(LOGGER, "Couldn't check for existence of object with OID {}", e, randomOid);
            }
        }
        throw new IllegalStateException("Couldn't generate random OID even after " + OID_GENERATION_ATTEMPTS + " attempts");
    }

    static void checkIndestructible(ObjectType object)
            throws IndestructibilityViolationException {
        if (object != null && Boolean.TRUE.equals(object.isIndestructible())) {
            throw new IndestructibilityViolationException("Attempt to delete indestructible object " + object);
        }
    }

    /**
     * This is not a complete authorization! Here we just check if the user is roughly authorized to request the operation
     * to be started, along with authorization for partial processing.
     */
    private void authorizeExecutionStart(
            LensContext<? extends ObjectType> context, ModelExecuteOptions options, Task task, OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {

        if (ModelExecuteOptions.isOperationStartPreAuthorized(options)) {
            argCheck(!task.isExecutionFullyPersistent(), "operationStartPreAuthorized option can be used only for simulations");
            return;
        }

        List<String> relevantActions = List.of(
                ModelAuthorizationAction.ADD.getUrl(),
                ModelAuthorizationAction.MODIFY.getUrl(),
                ModelAuthorizationAction.DELETE.getUrl(),
                ModelAuthorizationAction.RECOMPUTE.getUrl(),
                ModelAuthorizationAction.ASSIGN.getUrl(),
                ModelAuthorizationAction.UNASSIGN.getUrl(),
                ModelAuthorizationAction.DELEGATE.getUrl(),
                ModelAuthorizationAction.CHANGE_CREDENTIALS.getUrl());
        var result = parentResult.createSubresult(OP_AUTHORIZE_CHANGE_EXECUTION_START);
        try {
            // We do not need to check both phases here: normally, only REQUEST should be needed.
            // (For example, the #assign operation is relevant for the EXECUTION phase.)
            var phase = context.isExecutionPhaseOnly() ? AuthorizationPhaseType.EXECUTION : AuthorizationPhaseType.REQUEST;
            if (!securityEnforcer.hasAnyAllowAuthorization(relevantActions, phase)) {
                throw new SecurityViolationException("Not authorized to request execution of changes");
            }
            var partialProcessing = ModelExecuteOptions.getPartialProcessing(options);
            if (partialProcessing != null) {
                if (task.isExecutionFullyPersistent()) {
                    // TODO Note that the information about the object may be incomplete (orgs, tenants, roles) or even missing.
                    //  See MID-9454, MID-9477.
                    LensFocusContext<? extends ObjectType> focusContext = context.getFocusContext();
                    var autzParams =
                            focusContext != null ?
                                    AuthorizationParameters.Builder.buildObject(focusContext.getObjectAny()) :
                                    AuthorizationParameters.EMPTY;
                    securityEnforcer.authorize(
                            ModelAuthorizationAction.PARTIAL_EXECUTION.getUrl(),
                            phase, autzParams, task, result);
                } else {
                    LOGGER.trace("Partial processing is automatically authorized for simulation/preview mode");
                }
            }
        } catch (Throwable t) {
            result.recordException(t);
            clockworkAuditHelper.auditRequestDenied(context, task, result, parentResult);
            throw t;
        } finally {
            result.close();
        }
    }

    private void cleanupOperationResult(OperationResult result) {
        // Clockwork.run sets "in-progress" flag just at the root level
        // and result.computeStatus() would erase it.
        // So we deal with it in a special way, in order to preserve this information for the user.
        if (result.isInProgress()) {
            result.computeStatus();
            if (result.isSuccess()) {
                result.setInProgress();
            }
        } else {
            result.computeStatus();
        }

        result.cleanup();
    }

    @Override
    public <F extends ObjectType> void recompute(
            Class<F> type, String oid, ModelExecuteOptions options, Task task, OperationResult parentResult)
            throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException,
            ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException {

        OperationResult result = parentResult.subresult(RECOMPUTE)
                .setMinor()
                .addParam(OperationResult.PARAM_OID, oid)
                .addParam(OperationResult.PARAM_TYPE, type)
                .build();

        PrismObject<F> focus;

        enterModelMethod();
        try {

            ModelImplUtils.clearRequestee(task);
            // Not using read-only for now
            //noinspection unchecked
            focus = objectResolver.getObject(type, oid, null, task, result).asPrismContainer();

            executeRecompute(focus, options, task, result);

        } catch (Throwable t) {
            ModelImplUtils.recordException(result, t);
            throw t;
        } finally {
            result.close();
            result.cleanup();
            exitModelMethod();
        }

        LOGGER.trace("Recomputing of {}: {}", focus, result.getStatus());
    }

    /** Generally useful convenience method. */
    public <F extends ObjectType> void executeRecompute(
            @NotNull PrismObject<F> focus,
            @Nullable ModelExecuteOptions options,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException,
            ObjectNotFoundException, SecurityViolationException, PolicyViolationException, ObjectAlreadyExistsException {
        LOGGER.debug("Recomputing {}", focus);
        LensContext<F> lensContext = contextFactory.createRecomputeContext(focus, options, task, result);

        securityEnforcer.authorize(
                ModelAuthorizationAction.RECOMPUTE.getUrl(), AuthorizationPhaseType.REQUEST,
                AuthorizationParameters.forObject(focus.asObjectable()),
                SecurityEnforcer.Options.create(), task, result);

        LOGGER.trace("Recomputing {}, context:\n{}", focus, lensContext.debugDumpLazily());
        clockwork.run(lensContext, task, result);
    }

    private void applyDefinitions(
            Collection<ObjectDelta<? extends ObjectType>> deltas, ModelExecuteOptions options, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        for (ObjectDelta<? extends ObjectType> delta : deltas) {
            applyDefinition(delta, options, task, result);
        }
    }

    private <O extends ObjectType> void applyDefinition(
            ObjectDelta<O> delta, ModelExecuteOptions options, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        Class<O> type = delta.getObjectTypeClass();
        if (delta.hasCompleteDefinition()) {
            return;
        }
        if (type == ResourceType.class || ShadowType.class.isAssignableFrom(type)) {
            try {
                provisioning.applyDefinition(delta, task, result);
            } catch (CommonException e) {
                if (ModelExecuteOptions.isRaw(options)) {
                    ModelImplUtils.recordPartialError(result, e);
                    // just go on, this is raw, we need to continue even without complete schema
                } else {
                    ModelImplUtils.recordFatalError(result, e);
                    throw e;
                }
            }
        } else {
            PrismObjectDefinition<O> objDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
            if (objDef == null) {
                throw new SchemaException("No definition for delta object type class: " + type);
            }
            boolean tolerateNoDefinition = ModelExecuteOptions.isRaw(options);
            delta.applyDefinitionIfPresent(objDef, tolerateNoDefinition);
        }
    }

    @Override
    public <T extends ObjectType> SearchResultList<PrismObject<T>> searchObjects(
            Class<T> type,
            ObjectQuery origQuery,
            Collection<SelectorOptions<GetOperationOptions>> rawOptions,
            Task task,
            OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {

        Validate.notNull(type, "Object type must not be null.");
        Validate.notNull(parentResult, "Operation result must not be null.");
        // Using clone of object query here because authorization mechanism adds additional (security) filters to the (original)
        // query. At the end, objectQuery contains additional filters as many times as the authZ mechanism is called.
        // For more info see MID-6115.
        ObjectQuery query = origQuery != null ? origQuery.clone() : null;
        if (query != null) {
            ModelImplUtils.validatePaging(query.getPaging());
        }

        OP_LOGGER.trace("MODEL OP enter searchObjects({},{},{})", type.getSimpleName(), query, rawOptions);

        OperationResult result = parentResult.createSubresult(SEARCH_OBJECTS)
                .addParam(OperationResult.PARAM_TYPE, type)
                .addParam(OperationResult.PARAM_QUERY, query);

        try {
            var parsedOptions = preProcessOptionsSecurity(rawOptions, task, result);
            Collection<SelectorOptions<GetOperationOptions>> options = parsedOptions.getCollection();
            GetOperationOptions rootOptions = parsedOptions.getRootOptions();

            ObjectManager searchProvider = getObjectManager(type, options);
            result.addArbitraryObjectAsParam("searchProvider", searchProvider);

            if (checkNoneFilterBeforeAutz(query)) {
                return SearchResultList.empty();
            }
            ObjectQuery processedQuery = preProcessQuerySecurity(type, query, rootOptions, task, result);
            if (checkNoneFilterAfterAutz(processedQuery, result)) {
                return SearchResultList.empty();
            }

            enterModelMethod(); // outside try-catch because if this ends with an exception, cache is not entered yet
            @NotNull SearchResultList<PrismObject<T>> list;
            try {
                logQuery(processedQuery);

                try {
                    if (GetOperationOptions.isRaw(rootOptions)) { // MID-2218
                        QNameUtil.setTemporarilyTolerateUndeclaredPrefixes(true);
                    }
                    switch (searchProvider) {
                        case REPOSITORY:
                            list = cacheRepositoryService.searchObjects(type, normalizeQueryIfShadowUsed(type, processedQuery), options, result);
                            break;
                        case PROVISIONING:
                            list = provisioning.searchObjects(type, processedQuery, options, task, result);
                            break;
                        case TASK_MANAGER:
                            list = taskManager.searchObjects(type, processedQuery, options, result);
                            break;
                        default:
                            throw new AssertionError("Unexpected search provider: " + searchProvider);
                    }
                } catch (Exception e) {
                    recordSearchException(e, searchProvider, result);
                    throw e;
                } finally {
                    QNameUtil.setTemporarilyTolerateUndeclaredPrefixes(false);
                }

                LOGGER.trace("Basic search returned {} results (before hooks, security, etc.)", list.size());

                for (PrismObject<T> object : list) {
                    hookRegistry.invokeReadHooks(object, options, task, result);
                    executeResolveOptions(object.asObjectable(), parsedOptions, task, result);
                }

                // Post-processing objects that weren't handled by their correct provider (e.g. searching for ObjectType,
                // and retrieving tasks, resources, shadows). Currently, only resources and shadows are handled in this way.
                //
                // TODO generalize this approach somehow (something like "postprocess" in task/provisioning interface)
                // TODO ... or consider abandoning this functionality altogether (it is more a hack than a serious design!)
                if (searchProvider == ObjectManager.REPOSITORY && !GetOperationOptions.isRaw(rootOptions)) {
                    for (PrismObject<T> object : list) {
                        if (object.asObjectable() instanceof ResourceType || object.asObjectable() instanceof ShadowType) {
                            applyProvisioningDefinition(object, task, result);
                        }
                    }
                }
                // better to use cache here (MID-4059)
                list = schemaTransformer.applySchemasAndSecurityToObjects(list, parsedOptions, task, result);

            } catch (Throwable e) {
                result.recordException(e);
                throw e;
            } finally {
                exitModelMethod();
            }
            LOGGER.trace("Final search returned {} results (after hooks, security and all other processing)", list.size());

            // TODO: log errors

            if (OP_LOGGER.isDebugEnabled()) {
                OP_LOGGER.debug("MODEL OP exit searchObjects({},{},{}): {}", type.getSimpleName(), query, rawOptions, list.shortDump());
            }
            if (OP_LOGGER.isTraceEnabled()) {
                OP_LOGGER.trace("MODEL OP exit searchObjects({},{},{}): {}\n{}", type.getSimpleName(), query, rawOptions, list.shortDump(),
                        DebugUtil.debugDump(list.getList(), 1));
            }

            return list;
        } finally {
            result.close();
            result.cleanup();
        }
    }

    /**
     * Normalizes query if search touches shadows on concrete resource
     *
     * This normalization is neccessary for repository searches to work, if attributes / associations
     * are queried.
     *
     * @param queryType Root Query Type
     * @param processedQuery Query to be normalized
     * @return
     */
    private ObjectQuery normalizeQueryIfShadowUsed(Class<?> queryType, ObjectQuery processedQuery) {
        // FIXME: Queries should be fullscanned
        // FIXME: Rethink contract - normalization should probably happen on repository level
        //       in future, but currently provisioning expects repository to not normalize shadow
        //       queries.
        // if they contain shadow dereferencing, also nested queries should be normalized
        // if possible
        if (ShadowType.class.equals(queryType)) {
            return resourceSchemaRegistry.tryToNormalizeQuery(processedQuery);
        }
        return processedQuery;
    }

    /**
     * Returns the component that is responsible for execution of get/search/count operation for given type of objects,
     * under given options.
     *
     * Specifically, in raw mode we simply skip specialized components like provisioning or task manager, and we go
     * directly to repository. Actually it is a bit questionable if this is really correct. But this is how it has
     * been implemented for a long time.
     */
    public static @NotNull <T extends ObjectType> ObjectTypes.ObjectManager getObjectManager(
            Class<T> clazz, Collection<SelectorOptions<GetOperationOptions>> options) {
        if (GetOperationOptions.isRaw(SelectorOptions.findRootOptions(options))) {
            return ObjectManager.REPOSITORY;
        } else {
            ObjectManager objectManager = ObjectTypes.getObjectManagerForClass(clazz);
            if (objectManager == null || objectManager == ObjectManager.MODEL) {
                return ObjectManager.REPOSITORY;
            } else {
                return objectManager;
            }
        }
    }

    private <T extends ObjectType> void applyProvisioningDefinition(
            PrismObject<T> object, Task task, OperationResult parentResult) {
        OperationResult result = parentResult.subresult(OP_APPLY_PROVISIONING_DEFINITION)
                .setMinor()
                .addParam(OperationResult.PARAM_OBJECT, object)
                .build();
        try {
            provisioning.applyDefinition(object, task, result);
        } catch (Throwable t) {
            LoggingUtils.logExceptionAsWarning(
                    LOGGER, "Couldn't apply definition to {} (returning with fetchResult set)", t, object);
            result.recordPartialError(t); // This is not a fatal error: something is retrieved, but not the whole object
            object.asObjectable().setFetchResult(
                    result.createBeanReduced());
            // Intentionally not re-throwing the exception
        } finally {
            result.close();
        }
    }

    private class ContainerSearchLikeOpContext<T extends Containerable> {
        final ObjectManager manager;
        final ObjectQuery securityRestrictedQuery;
        private final boolean skipSecurityPostProcessing;

        // TODO: task and result here are ugly and probably wrong
        ContainerSearchLikeOpContext(
                Class<T> type, ObjectQuery origQuery, ParsedGetOperationOptions options, Task task, OperationResult result)
                throws SchemaException, SecurityViolationException, ObjectNotFoundException,
                ExpressionEvaluationException, CommunicationException, ConfigurationException {

            var isAssignment = AssignmentType.class.equals(type);
            var isProcessedObject = SimulationResultProcessedObjectType.class.equals(type);

            if (!AccessCertificationCaseType.class.equals(type)
                    && !AccessCertificationWorkItemType.class.equals(type)
                    && !CaseWorkItemType.class.equals(type)
                    && !OperationExecutionType.class.equals(type)
                    && !isAssignment
                    && !isProcessedObject) {
                throw new UnsupportedOperationException(
                        "searchContainers/countContainers methods are currently supported only for AccessCertificationCaseType,"
                                + " AccessCertificationWorkItemType, CaseWorkItemType, OperationExecutionType, AssignmentType,"
                                + " and SimulationResultProcessedObjectType objects");
            }

            manager = ObjectManager.REPOSITORY;
            securityRestrictedQuery = preProcessQuerySecurity(type, origQuery, options.getRootOptions(), task, result);
            skipSecurityPostProcessing = isAssignment || isProcessedObject;
        }
    }

    @Override
    public <T extends Containerable> SearchResultList<T> searchContainers(
            @NotNull Class<T> type,
            @Nullable ObjectQuery origQuery,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> rawOptions,
            @NotNull Task task,
            @NotNull OperationResult parentResult) throws SchemaException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException {

        Validate.notNull(type, "Container value type must not be null.");
        Validate.notNull(parentResult, "Result type must not be null.");
        if (origQuery != null) {
            ModelImplUtils.validatePaging(origQuery.getPaging());
        }

        OperationResult result = parentResult.createSubresult(SEARCH_CONTAINERS)
                .addParam(OperationResult.PARAM_TYPE, type)
                .addParam(OperationResult.PARAM_QUERY, origQuery);

        try {
            var rawOptionsReadWrite = GetOperationOptions.updateToReadWriteSafe(rawOptions);
            var parsedOptions = preProcessOptionsSecurity(rawOptionsReadWrite, task, result);
            var options = parsedOptions.getCollection();

            if (checkNoneFilterBeforeAutz(origQuery)) {
                return SearchResultList.empty();
            }

            var ctx = new ContainerSearchLikeOpContext<>(type, origQuery, parsedOptions, task, result);

            GetOperationOptions rootOptions = parsedOptions.getRootOptions();

            ObjectQuery query = ctx.securityRestrictedQuery;

            if (checkNoneFilterAfterAutz(query, result)) {
                return SearchResultList.empty();
            }

            enterModelMethod(); // outside try-catch because if this ends with an exception, cache is not entered yet
            SearchResultList<T> list;
            try {
                logQuery(query);

                try {
                    if (GetOperationOptions.isRaw(rootOptions)) { // MID-2218
                        QNameUtil.setTemporarilyTolerateUndeclaredPrefixes(true);
                    }
                    //noinspection SwitchStatementWithTooFewBranches
                    switch (ctx.manager) {
                        case REPOSITORY:
                            list = cacheRepositoryService.searchContainers(type, query, options, result);
                            break;
                        default:
                            throw new IllegalStateException();
                    }
                } catch (SchemaException | RuntimeException e) {
                    recordSearchException(e, ctx.manager, result);
                    throw e;
                } finally {
                    QNameUtil.setTemporarilyTolerateUndeclaredPrefixes(false);
                }

                for (T object : list) {
                    // TODO implement read hook, if necessary
                    executeResolveOptions(object, parsedOptions, task, result);
                }
            } finally {
                exitModelMethod();
            }
            if (ctx.skipSecurityPostProcessing) {
                LOGGER.debug("Objects of type '{}' do not have security constraints applied yet", type.getSimpleName());
            } else {
                schemaTransformer.applySchemasAndSecurityToContainerValues(list, parsedOptions, task, result);
            }
            return list;
        } catch (Throwable e) {
            result.recordException(e);
            throw e;
        } finally {
            result.close();
            result.cleanup();
        }
    }

    public <T extends Containerable> SearchResultMetadata searchContainersIterative(
            Class<T> type, ObjectQuery origQuery,
            ObjectHandler<T> handler, Collection<SelectorOptions<GetOperationOptions>> rawOptions,
            Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {

        Validate.notNull(type, "Container type must not be null.");
        Validate.notNull(parentResult, "Result type must not be null.");
        ObjectQuery query = origQuery != null ? origQuery.clone() : null;
        if (query != null) {
            ModelImplUtils.validatePaging(query.getPaging());
        }

        OP_LOGGER.trace("MODEL OP enter searchContainersIterative({},{},{})", type.getSimpleName(), query, rawOptions);

        OperationResult result = parentResult.createSubresult(SEARCH_CONTAINERS)
                .addParam(OperationResult.PARAM_QUERY, query);

        try {
            var rawOptionsReadWrite = GetOperationOptions.updateToReadWriteSafe(rawOptions);
            var parsedOptions = preProcessOptionsSecurity(rawOptionsReadWrite, task, result);
            var options = parsedOptions.getCollection();

            if (checkNoneFilterBeforeAutz(query)) {
                return null;
            }

            var ctx = new ContainerSearchLikeOpContext<>(type, origQuery, parsedOptions, task, result);

            GetOperationOptions rootOptions = parsedOptions.getRootOptions();

            ObjectQuery processedQuery = ctx.securityRestrictedQuery;

            if (checkNoneFilterAfterAutz(processedQuery, result)) {
                return null;
            }

            ObjectHandler<T> internalHandler = (object, lResult) -> {
                var prismContainer = object.asPrismContainerValue().cloneIfImmutable();
                object = prismContainer.getRealValue();
                try {
                    if (ctx.skipSecurityPostProcessing) {
                        LOGGER.debug("Objects of type '{}' do not have security constraints applied yet", type.getSimpleName());
                    } else {
                        SearchResultList<Containerable> list = new SearchResultList<>();
                        list.add(object);
                        schemaTransformer.applySchemasAndSecurityToContainerValues(list, parsedOptions, task, result);
                    }
                } catch (CommonException ex) {
                    lResult.recordException(ex); // We should create a subresult for this
                    throw new SystemException(ex.getMessage(), ex);
                }

                OP_LOGGER.debug("MODEL OP handle searchContainersIterative({},{},{}): {}",
                        type.getSimpleName(), query, rawOptions, object);
                if (OP_LOGGER.isTraceEnabled()) {
                    OP_LOGGER.trace("MODEL OP handle searchContainersIterative({},{},{}):\n{}",
                            type.getSimpleName(), query, rawOptions, DebugUtil.debugDump(object,1));
                }

                return handler.handle(prismContainer.getRealValue(), lResult);
            };

            SearchResultMetadata metadata;
            try {
                enterModelMethodNoRepoCache(); // skip using cache to avoid potentially many objects there (MID-4615, MID-4959)
                logQuery(processedQuery);

                if (GetOperationOptions.isRaw(rootOptions)) { // MID-2218
                    QNameUtil.setTemporarilyTolerateUndeclaredPrefixes(true);
                }
                //noinspection SwitchStatementWithTooFewBranches
                switch (ctx.manager) {
                    case REPOSITORY:
                        metadata = cacheRepositoryService.searchContainersIterative(type, processedQuery, internalHandler, options, result);
                        break;
                    default:
                        throw new IllegalStateException();
                }
            } catch (Exception e) {
                result.recordException(e);
                throw e;
            } finally {
                exitModelMethodNoRepoCache();
            }

            // TODO: log errors

            if (OP_LOGGER.isDebugEnabled()) {
                OP_LOGGER.debug("MODEL OP exit searchObjects({},{},{}): {}", type.getSimpleName(), query, rawOptions, metadata);
            }

            return metadata;
        } finally {
            result.close();
            result.cleanup();
        }
    }


    @Override
    public <T extends Containerable> Integer countContainers(Class<T> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> rawOptions, Task task, OperationResult parentResult)
            throws SchemaException, SecurityViolationException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException {

        Validate.notNull(type, "Container value type must not be null.");
        Validate.notNull(parentResult, "Result type must not be null.");

        OperationResult result = parentResult.createSubresult(COUNT_CONTAINERS)
                .addParam(OperationResult.PARAM_TYPE, type)
                .addParam(OperationResult.PARAM_QUERY, query);

        try {
            var parsedOptions = preProcessOptionsSecurity(rawOptions, task, result);

            if (checkNoneFilterBeforeAutz(query)) {
                return 0;
            }
            var ctx = new ContainerSearchLikeOpContext<>(type, query, parsedOptions, task, result);
            query = ctx.securityRestrictedQuery;

            if (checkNoneFilterAfterAutz(query, result)) {
                return 0;
            }

            enterModelMethod(); // outside try-catch because if this ends with an exception, cache is not entered yet
            try {
                logQuery(query);

                var options = parsedOptions.getCollection();
                //noinspection SwitchStatementWithTooFewBranches
                switch (ctx.manager) {
                    case REPOSITORY:
                        return cacheRepositoryService.countContainers(type, query, options, result);
                    default:
                        throw new IllegalStateException();
                }
            } catch (RuntimeException e) {
                recordSearchException(e, ctx.manager, result);
                throw e;
            } finally {
                exitModelMethod();
            }
        } finally {
            result.close();
            result.cleanup();
        }
    }

    // See MID-6323

    private boolean checkNoneFilterBeforeAutz(ObjectQuery query) {
        if (ObjectQueryUtil.isNoneQuery(query)) {
            LOGGER.trace("Skipping the search/count operation, as the NONE filter was requested");
            return true;
        }
        return false;
    }

    private boolean checkNoneFilterAfterAutz(ObjectQuery query, OperationResult result) {
        if (ObjectQueryUtil.isNoneQuery(query)) {
            LOGGER.trace("Security denied the search/coount operation");
            result.setNotApplicable("Denied"); // TODO really do we want "not applicable" here?
            return true;
        }
        return false;
    }

    private void logQuery(ObjectQuery query) {
        if (!LOGGER.isTraceEnabled()) {
            return;
        }
        if (query != null) {
            if (query.getPaging() == null) {
                LOGGER.trace("Searching objects with null paging. Processed query:\n{}", query.debugDump(1));
            } else {
                LOGGER.trace("Searching objects from {} to {} ordered {} by {}. Processed query:\n{}",
                        query.getPaging().getOffset(), query.getPaging().getMaxSize(),
                        query.getPaging().getPrimaryOrderingDirection(), query.getPaging().getPrimaryOrderingPath(),
                        query.debugDump(1));
            }
        } else {
            LOGGER.trace("Searching objects with null paging and null (processed) query.");
        }
    }

    @Override
    public <T extends ObjectType> SearchResultMetadata searchObjectsIterative(Class<T> type, ObjectQuery origQuery,
            ResultHandler<T> handler, Collection<SelectorOptions<GetOperationOptions>> rawOptions,
            Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {

        Validate.notNull(type, "Object type must not be null.");
        Validate.notNull(parentResult, "Result type must not be null.");
        ObjectQuery query = origQuery != null ? origQuery.clone() : null;
        if (query != null) {
            ModelImplUtils.validatePaging(query.getPaging());
        }

        OP_LOGGER.trace("MODEL OP enter searchObjectsIterative({},{},{})", type.getSimpleName(), query, rawOptions);

        OperationResult result = parentResult.createSubresult(SEARCH_OBJECTS)
                .addParam(OperationResult.PARAM_QUERY, query);

        try {
            var parsedOptions = preProcessOptionsSecurity(rawOptions, task, result);
            var options = parsedOptions.getCollection();
            var rootOptions = SelectorOptions.findRootOptions(options);
            ObjectManager searchProvider = getObjectManager(type, options);
            result.addArbitraryObjectAsParam("searchProvider", searchProvider);

            if (checkNoneFilterBeforeAutz(query)) {
                return null;
            }

            // see MID-6115
            ObjectQuery processedQuery = preProcessQuerySecurity(type, query, rootOptions, task, result);
            if (checkNoneFilterAfterAutz(processedQuery, result)) {
                return null;
            }

            ResultHandler<T> internalHandler = (object, lResult) -> {
                try {
                    object = object.cloneIfImmutable();
                    hookRegistry.invokeReadHooks(object, options, task, lResult);
                    executeResolveOptions(object.asObjectable(), parsedOptions, task, lResult);
                    schemaTransformer.applySchemasAndSecurityToObject(object, parsedOptions, task, lResult);
                } catch (CommonException ex) {
                    lResult.recordException(ex);
                    throw new SystemException(ex.getMessage(), ex);
                }

                OP_LOGGER.debug("MODEL OP handle searchObjects({},{},{}): {}", type.getSimpleName(), query, rawOptions, object);
                if (OP_LOGGER.isTraceEnabled()) {
                    OP_LOGGER.trace("MODEL OP handle searchObjects({},{},{}):\n{}", type.getSimpleName(), query, rawOptions, object.debugDump(1));
                }

                return handler.handle(object, lResult);
            };
            var resultProvidingHandler = internalHandler.providingOwnOperationResult(OP_HANDLE_OBJECT_FOUND);

            // This is to correctly report time spent as "model" time
            // (the objParentResult can come from provisioning or repository).

            SearchResultMetadata metadata;
            try {
                enterModelMethodNoRepoCache(); // skip using cache to avoid potentially many objects there (MID-4615, MID-4959)
                logQuery(processedQuery);

                switch (searchProvider) {
                    case REPOSITORY:
                        metadata = cacheRepositoryService.searchObjectsIterative(
                                type, normalizeQueryIfShadowUsed(type, processedQuery),
                                resultProvidingHandler, options, true, result);
                        break;
                    case PROVISIONING:
                        metadata = provisioning.searchObjectsIterative(
                                type, processedQuery, options, resultProvidingHandler, task, result);
                        break;
                    case TASK_MANAGER:
                        metadata = taskManager.searchObjectsIterative(
                                type, processedQuery, options, resultProvidingHandler, result);
                        break;
                    default:
                        throw new AssertionError("Unexpected search provider: " + searchProvider);
                }
            } catch (Exception e) {
                recordSearchException(e, searchProvider, result);
                throw e;
            } finally {
                exitModelMethodNoRepoCache();
            }

            // TODO: log errors

            if (OP_LOGGER.isDebugEnabled()) {
                OP_LOGGER.debug("MODEL OP exit searchObjects({},{},{}): {}", type.getSimpleName(), query, rawOptions, metadata);
            }

            return metadata;
        } finally {
            result.close();
            result.cleanup();
        }
    }

    private void recordSearchException(
            Throwable e, ObjectManager searchProvider, OperationResult result) {
        String message;
        switch (searchProvider) {
            case REPOSITORY:
                message = "Couldn't search objects in repository";
                break;
            case PROVISIONING:
                message = "Couldn't search objects in provisioning";
                break;
            case TASK_MANAGER:
                message = "Couldn't search objects in task manager";
                break;
            default:
                message = "Couldn't search objects";
                break; // should not occur
        }
        LoggingUtils.logExceptionAsWarning(LOGGER, message, e);
        result.recordException(message, e);
    }

    @Override
    public <T extends ObjectType> Integer countObjects(Class<T> type, ObjectQuery origQuery,
            Collection<SelectorOptions<GetOperationOptions>> rawOptions, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, ConfigurationException,
            SecurityViolationException, CommunicationException, ExpressionEvaluationException {

        // see MID-6115
        ObjectQuery query = origQuery != null ? origQuery.clone() : null;
        OperationResult result = parentResult.createMinorSubresult(COUNT_OBJECTS);
        result.addParam(OperationResult.PARAM_QUERY, query);

        enterModelMethod(); // outside try-catch because if this ends with an exception, cache is not entered yet
        Integer count;
        try {

            var parsedOptions = preProcessOptionsSecurity(rawOptions, task, result);
            var rootOptions = parsedOptions.getRootOptions();
            var options = parsedOptions.getCollection();

            if (checkNoneFilterBeforeAutz(query)) {
                return 0;
            }

            ObjectQuery processedQuery = preProcessQuerySecurity(type, query, rootOptions, task, result);
            if (checkNoneFilterAfterAutz(processedQuery, result)) {
                return 0;
            }

            ObjectManager objectManager = getObjectManager(type, options);
            count = switch (objectManager) {
                case PROVISIONING -> provisioning.countObjects(type, processedQuery, options, task, parentResult);
                case REPOSITORY -> cacheRepositoryService.countObjects(type, normalizeQueryIfShadowUsed(type,processedQuery), options, parentResult);
                case TASK_MANAGER -> taskManager.countObjects(type, processedQuery, parentResult);
                default -> throw new AssertionError("Unexpected objectManager: " + objectManager);
            };
        } catch (Throwable t) {
            ModelImplUtils.recordException(result, t);
            throw t;
        } finally {
            exitModelMethod();
            result.close();
            result.cleanup();
        }
        return count;
    }

    @Override
    public PrismObject<? extends FocusType> searchShadowOwner(
            String shadowOid, Collection<SelectorOptions<GetOperationOptions>> rawOptions, Task task, OperationResult parentResult)
            throws ObjectNotFoundException, SecurityViolationException, SchemaException,
            ConfigurationException, ExpressionEvaluationException, CommunicationException {
        Validate.notEmpty(shadowOid, "Account oid must not be null or empty.");
        Validate.notNull(parentResult, "Result type must not be null.");

        enterModelMethod();

        PrismObject<? extends FocusType> focus;

        LOGGER.trace("Listing account shadow owner for account with oid {}.", shadowOid);

        OperationResult result = parentResult.createSubresult(LIST_ACCOUNT_SHADOW_OWNER);
        result.addParam("shadowOid", shadowOid);

        try {
            var parsedOptions = preProcessOptionsSecurity(rawOptions, task, result);
            var options = parsedOptions.getCollection();
            focus = cacheRepositoryService.searchShadowOwner(shadowOid, options, result);

            if (focus != null) {
                try {
                    focus = schemaTransformer.applySchemasAndSecurityToObject(
                            focus, ParsedGetOperationOptions.empty(), task, result);
                } catch (SchemaException | SecurityViolationException | ConfigurationException
                        | ObjectNotFoundException | CommunicationException ex) {
                    LoggingUtils.logException(LOGGER, "Couldn't list account shadow owner from repository"
                            + " for account with oid {}", ex, shadowOid);
                    result.recordFatalError("Couldn't list account shadow owner for account with oid '"
                            + shadowOid + "'.", ex);
                    throw ex;
                }
            }

            return focus;
        } catch (Throwable t) {
            LoggingUtils.logException(LOGGER, "Couldn't list account shadow owner from repository"
                    + " for account with oid {}", t, shadowOid);
            result.recordFatalError("Couldn't list account shadow owner for account with oid '" + shadowOid + "'.", t);
            throw t;
        } finally {
            exitModelMethod();
            result.close();
            result.cleanup();
        }
    }

    // region search-references
    @Override
    public SearchResultList<ObjectReferenceType> searchReferences(
            ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> rawOptions,
            Task task, OperationResult parentResult) throws SchemaException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException {

        Objects.requireNonNull(query, "Query must be provided for reference search");
        Validate.notNull(parentResult, "Result type must not be null.");

        ModelImplUtils.validatePaging(query.getPaging());

        OperationResult result = parentResult.createSubresult(SEARCH_REFERENCES)
                .addParam(OperationResult.PARAM_QUERY, query);

        try {
            var parsedOptions = preProcessOptionsSecurity(rawOptions, task, result);
            var options = parsedOptions.getCollection();

            if (checkNoneFilterBeforeAutz(query)) {
                return SearchResultList.empty();
            }

            query = preProcessReferenceQuerySecurity(query, task, result); // TODO not implemented yet!

            if (checkNoneFilterAfterAutz(query, result)) {
                return SearchResultList.empty();
            }

            SearchResultList<ObjectReferenceType> list;
            // TODO caching and reference search are probably unknown territory at this moment.
            enterModelMethod(); // outside try-catch because if this ends with an exception, cache is not entered yet
            try {
                logQuery(query);

                list = cacheRepositoryService.searchReferences(query, options, result);
            } catch (SchemaException | RuntimeException e) {
                recordSearchException(e, ObjectManager.REPOSITORY, result);
                throw e;
            } finally {
                exitModelMethod();
            }

            // TODO how does schemaTransformer.applySchemasAndSecurityToContainers apply to reference result?
            return list;
        } finally {
            result.close();
            result.cleanup();
        }
    }

    @Override
    public Integer countReferences(ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> rawOptions, Task task, OperationResult parentResult)
            throws SchemaException, SecurityViolationException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException {

        Objects.requireNonNull(query, "Query must be provided for reference search");
        Validate.notNull(parentResult, "Result type must not be null.");

        OperationResult result = parentResult.createSubresult(COUNT_REFERENCES)
                .addParam(OperationResult.PARAM_QUERY, query);

        try {
            var parsedOptions = preProcessOptionsSecurity(rawOptions, task, result);
            var options = parsedOptions.getCollection();

            if (checkNoneFilterBeforeAutz(query)) {
                return 0;
            }

            query = preProcessReferenceQuerySecurity(query, task, result); // TODO not implemented yet!

            if (checkNoneFilterAfterAutz(query, result)) {
                return 0;
            }

            enterModelMethod(); // outside try-catch because if this ends with an exception, cache is not entered yet
            try {
                logQuery(query);

                return cacheRepositoryService.countReferences(query, options, result);
            } catch (RuntimeException e) {
                recordSearchException(e, ObjectManager.REPOSITORY, result);
                throw e;
            } finally {
                exitModelMethod();
            }
        } finally {
            result.close();
            result.cleanup();
        }
    }

    private ObjectQuery preProcessReferenceQuerySecurity(ObjectQuery query, Task task, OperationResult options) {
        // TODO:
        // 1. extract owner object type from OWNED-BY query (if it's a container type, follow up to the object type)
        // 2. use it for securityEnforcer.preProcessObjectFilter()
        // 3. add filters to the owned-by filter as necessary
        return query;
    }

    @Override
    public SearchResultMetadata searchReferencesIterative(
            @NotNull ObjectQuery query, @NotNull ObjectHandler<ObjectReferenceType> handler,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> rawOptions,
            Task task, OperationResult parentResult) throws SchemaException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException {

        Objects.requireNonNull(query, "Query must be provided for reference search");
        Validate.notNull(parentResult, "Result type must not be null.");
        OP_LOGGER.trace("MODEL OP enter searchReferencesIterative({}, {})", query, rawOptions);

        ModelImplUtils.validatePaging(query.getPaging());
        OperationResult result = parentResult.createSubresult(SEARCH_REFERENCES)
                .addParam(OperationResult.PARAM_QUERY, query);

        try {
            var parsedOptions = preProcessOptionsSecurity(rawOptions, task, result);
            var options = parsedOptions.getCollection();

            if (checkNoneFilterBeforeAutz(query)) {
                return null;
            }
            ObjectQuery processedQuery = preProcessReferenceQuerySecurity(query, task, result); // TODO not implemented yet!
            if (checkNoneFilterAfterAutz(processedQuery, result)) {
                return null;
            }

            ObjectHandler<ObjectReferenceType> internalHandler = (ref, lResult) -> {
                // TODO how does schemaTransformer.applySchemasAndSecurityToContainers apply to reference result?

                PrismReferenceValue refValue = ref.asReferenceValue();
                if (OP_LOGGER.isTraceEnabled()) {
                    OP_LOGGER.trace("MODEL OP handle searchReferencesIterative({}, {}):\n{}",
                            query, options, refValue.debugDump(1));
                } else {
                    OP_LOGGER.debug("MODEL OP handle searchReferencesIterative({}, {}): {}",
                            query, options, refValue);
                }

                return handler.handle(ref, lResult);
            };

            SearchResultMetadata metadata;
            try {
                enterModelMethodNoRepoCache(); // skip using cache to avoid potentially many objects there (MID-4615, MID-4959)
                logQuery(processedQuery);

                metadata = cacheRepositoryService.searchReferencesIterative(
                        processedQuery, internalHandler, options, result);
            } catch (SchemaException | RuntimeException e) {
                recordSearchException(e, ObjectManager.REPOSITORY, result);
                throw e;
            } finally {
                exitModelMethodNoRepoCache();
            }

            return metadata;
        } finally {
            result.close();
            result.cleanup();
        }
    }
    // endregion

    @Override
    public OperationResult testResource(String resourceOid, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ConfigurationException, ExpressionEvaluationException,
            CommunicationException, SecurityViolationException {
        Validate.notEmpty(resourceOid, "Resource oid must not be null or empty.");
        LOGGER.trace("Testing resource OID: {}", resourceOid);

        enterModelMethod();
        try {
            authorizeResourceOperation(ModelAuthorizationAction.TEST, resourceOid, task, result);
            OperationResult testResult = provisioning.testResource(resourceOid, task, result);
            LOGGER.debug("Finished testing resource OID: {}, result: {} ", resourceOid, testResult.getStatus());
            LOGGER.trace("Test result:\n{}", lazy(() -> testResult.dump(false)));
            return testResult;
        } catch (Throwable ex) {
            LOGGER.error("Error testing resource OID: {}: {}: {}",
                    resourceOid, ex.getClass().getSimpleName(), ex.getMessage(), ex);
            throw ex;
        } finally {
            exitModelMethod();
        }
    }

    private ResourceType authorizeResourceOperation(
            @NotNull ModelAuthorizationAction action, @NotNull String resourceOid,
            @NotNull Task task, @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        // Retrieved in full in order to evaluate the authorization
        var fullResource = provisioning.getObject(ResourceType.class, resourceOid, readOnly(), task, result);
        securityEnforcer.authorize(
                action.getUrl(),
                null,
                AuthorizationParameters.forObject(fullResource.asObjectable()),
                task, result);
        return fullResource.asObjectable();
    }

    @Override
    public OperationResult testResource(PrismObject<ResourceType> resource, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ConfigurationException {
        Validate.notNull(resource, "Resource must not be null.");
        LOGGER.trace("Testing resource: {}", resource);

        enterModelMethod();
        try {
            OperationResult testResult = provisioning.testResource(resource, task, result);
            LOGGER.debug("Finished testing resource: {}, result: {} ", resource, testResult.getStatus());
            LOGGER.trace("Test result:\n{}", lazy(() -> testResult.dump(false)));
            return testResult;
        } finally {
            exitModelMethod();
        }
    }

    @Override
    public OperationResult testResourcePartialConfiguration(PrismObject<ResourceType> resource, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ConfigurationException {
        Validate.notNull(resource, "Resource must not be null.");
        LOGGER.trace("Testing partial configuration of resource: {}", resource);

        enterModelMethodNoRepoCache();
        try {
            OperationResult testResult = provisioning.testPartialConfiguration(resource, task, result);
            LOGGER.debug("Finished testing partial configuration of resource: {}, result: {} ", resource, testResult.getStatus());
            LOGGER.trace("Test result:\n{}", lazy(() -> testResult.dump(false)));
            return testResult;
        } finally {
            exitModelMethodNoRepoCache();
        }
    }

    @Override
    public DiscoveredConfiguration discoverResourceConnectorConfiguration(
            PrismObject<ResourceType> resource, OperationResult result) {

        Validate.notNull(resource, "Resource must not be null.");
        LOGGER.trace("Discover connector configuration for resource: {}", resource);

        enterModelMethodNoRepoCache();
        try {
            @NotNull DiscoveredConfiguration discoverConfiguration = provisioning.discoverConfiguration(resource, result);
            LOGGER.debug("Finished discover connector configuration for resource: {}, result: {} ", resource, result.getStatus());
            LOGGER.trace("Discover connector configuration result:\n{}", lazy(() -> result.dump(false)));
            return discoverConfiguration;
        } finally {
            exitModelMethodNoRepoCache();
        }
    }

    @Override
    public @Nullable BareResourceSchema fetchSchema(
            @NotNull PrismObject<ResourceType> resource, @NotNull OperationResult result) {
        Validate.notNull(resource, "Resource must not be null.");
        LOGGER.trace("Fetch schema by connector configuration from resource: {}", resource);

        enterModelMethodNoRepoCache();
        try {
            @Nullable BareResourceSchema schema = provisioning.fetchSchema(resource, result);
            LOGGER.debug(
                    "Finished fetch schema by connector configuration from resource: {}, result: {} ",
                    resource,
                    result.getStatus());
            LOGGER.trace("Fetch schema by connector configuration result:\n{}", lazy(() -> result.dump(false)));
            return schema;
        } finally {
            exitModelMethodNoRepoCache();
        }
    }

    @Override
    public @NotNull CapabilityCollectionType getNativeCapabilities(@NotNull String connOid, OperationResult result)
            throws SchemaException, CommunicationException, ConfigurationException, ObjectNotFoundException {
        Validate.notNull(connOid, "ConnOid must not be null.");
        LOGGER.trace("Getting native capabilities by connector oid: {}", connOid);

        enterModelMethodNoRepoCache();
        try {
            CapabilityCollectionType capabilities = provisioning.getNativeCapabilities(connOid, result);
            LOGGER.debug(
                    "Finished getting native capabilities by connector oid: {}, result: {} ",
                    connOid,
                    result.getStatus());
            LOGGER.trace("Getting native capabilities by connector oid:\n{}", lazy(() -> result.dump(false)));
            return capabilities;
        } finally {
            exitModelMethodNoRepoCache();
        }
    }

    // Note: The result is in the task. No need to pass it explicitly
    @Override
    public void importFromResource(String resourceOid, QName objectClass, Task task, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        Validate.notEmpty(resourceOid, "Resource oid must not be null or empty.");
        Validate.notNull(objectClass, "Object class must not be null.");
        Validate.notNull(task, "Task must not be null.");
        enterModelMethod();
        LOGGER.trace("Launching import from resource with oid {} for object class {}.", resourceOid, objectClass);

        OperationResult result = parentResult.createSubresult(IMPORT_ACCOUNTS_FROM_RESOURCE);
        result.addParam("resourceOid", resourceOid);
        result.addParam("objectClass", objectClass);
        result.addArbitraryObjectAsParam(OperationResult.PARAM_TASK, task);
        // TODO: add context to the result

        try {
            var resource = authorizeResourceOperation(ModelAuthorizationAction.IMPORT_FROM_RESOURCE, resourceOid, task, result);

            // Here was a check on synchronization configuration, providing a warning if there is no configuration set up.
            // But with changes in 4.6 it is not so easy to definitely tell that there's no synchronization set up,
            // so - maybe temporarily - we removed these checks.

            result.recordStatus(OperationResultStatus.IN_PROGRESS, "Task running in background");

            importFromResourceLauncher.launch(resource, objectClass, task, result);

            // The launch should switch task to asynchronous. It is in/out, so no other action is needed
            if (!task.isAsynchronous()) {
                result.recordSuccess();
            }

        } catch (Throwable t) {
            ModelImplUtils.recordException(result, t);
            throw t;
        } finally {
            exitModelMethod();
            result.close();
            result.cleanup();
        }
    }

    @Override
    public void importFromResource(String shadowOid, Task task, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        Validate.notNull(shadowOid, "Shadow OID must not be null.");
        Validate.notNull(task, "Task must not be null.");
        enterModelMethod();
        LOGGER.trace("Launching importing shadow {} from resource.", shadowOid);

        OperationResult result = parentResult.createSubresult(IMPORT_ACCOUNTS_FROM_RESOURCE);
        result.addParam(OperationResult.PARAM_OID, shadowOid);
        result.addArbitraryObjectAsParam(OperationResult.PARAM_TASK, task);
        // TODO: add context to the result

        try {
            // fetching the shadow just to get the resource OID (for the authorization)
            var shadow = cacheRepositoryService.getObject(ShadowType.class, shadowOid, readOnly(), result);
            var resourceOid = ShadowUtil.getResourceOidRequired(shadow.asObjectable());
            authorizeResourceOperation(ModelAuthorizationAction.IMPORT_FROM_RESOURCE, resourceOid, task, result);

            importFromResourceLauncher.importSingleShadow(shadowOid, task, result);
        } catch (Throwable t) {
            ModelImplUtils.recordException(result, t);
            throw t;
        } finally {
            exitModelMethod();
            result.close();
            result.cleanup();
        }
    }

    @Override
    public void importObjectsFromFile(File input, ImportOptionsType options, Task task,
            OperationResult parentResult) throws FileNotFoundException {
        OperationResult result = parentResult.createSubresult(IMPORT_OBJECTS_FROM_FILE);

        try (FileInputStream fis = new FileInputStream(input)) {
            importObjectsFromStream(fis, PrismContext.LANG_XML, options, task, parentResult);
        } catch (FileNotFoundException e) {
            String msg = "Error reading from file " + input + ": " + e.getMessage();
            result.recordFatalError(msg, e);
            throw e;
        } catch (RuntimeException e) {
            result.recordFatalError(e);
            throw e;
        } catch (IOException e) {
            LOGGER.error("Error closing file " + input + ": " + e.getMessage(), e);
        } finally {
            result.close();
        }
    }

    @Override
    public void importObjectsFromStream(InputStream input, String language, ImportOptionsType options, Task task, OperationResult parentResult) {
        enterModelMethod();
        OperationResult result = parentResult.createSubresult(IMPORT_OBJECTS_FROM_STREAM);
        result.addArbitraryObjectAsParam(OperationResult.PARAM_OPTIONS, options);
        result.addParam(OperationResult.PARAM_LANGUAGE, language);
        try {
            objectImporter.importObjects(input, language, options, task, result);
            LOGGER.trace("Import result:\n{}", result.debugDumpLazily());
            // No need to compute status. The validator inside will do it.
            // result.computeStatus("Couldn't import object from input stream.");
        } catch (RuntimeException e) {
            result.recordException(e); // shouldn't really occur
        } finally {
            exitModelMethod();
            result.close();
            result.cleanup();
        }
    }

    @Override
    public <O extends ObjectType> void importObject(
            PrismObject<O> object, ImportOptionsType options, Task task, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(IMPORT_OBJECTS_FROM_STREAM);
        result.addArbitraryObjectAsParam(OperationResult.PARAM_OPTIONS, options);
        enterModelMethod();
        try {
            objectImporter.importObject(object, options, task, result);
        } catch (RuntimeException e) {
            result.recordException(e);
            throw e;
        } finally {
            exitModelMethod();
            result.close();
            result.cleanup();
        }
    }

    @Override
    public Set<ConnectorType> discoverConnectors(ConnectorHostType hostType, Task task, OperationResult parentResult)
            throws CommunicationException, SecurityViolationException {
        enterModelMethod();
        OperationResult result = parentResult.createSubresult(DISCOVER_CONNECTORS);
        try {
            Set<ConnectorType> discoveredConnectors = provisioning.discoverConnectors(hostType, result);
            SearchResultList<PrismObject<ConnectorType>> objects = discoveredConnectors.stream()
                    .map(c -> c.asPrismObject())
                    .collect(Collectors.toCollection(SearchResultList::new));
            return schemaTransformer.applySchemasAndSecurityToObjects(objects, null, task, result).stream()
                    .map(o -> o.asObjectable())
                    .collect(Collectors.toSet());
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            exitModelMethod();
            result.close();
            result.cleanup();
        }
    }

    @Override
    public void postInit(OperationResult parentResult) {
        // necessary for testing situations where we re-import different system configurations with the same version
        // (on system init)
        systemObjectCache.invalidateCaches();

        enterModelMethod();
        OperationResult result = parentResult.createSubresult(POST_INIT);
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ModelController.class);

        try {
            // Repository service itself might have been initialized.
            // But there are situations (e.g. in tests or after factory reset) in which only this method is called.
            // So let's be conservative and rather execute repository postInit twice than zero times.
            cacheRepositoryService.postInit(result);

            securityContextManager.setUserProfileService(focusProfileService);

            provisioning.postInit(result);

        } catch (SchemaException e) {
            result.recordFatalError(e);
            throw new SystemException(e.getMessage(), e);
        } finally {
            exitModelMethod();
            result.close();
            result.cleanup();
        }
    }

    @Override
    public void shutdown() {

        enterModelMethod();

        provisioning.shutdown();

//        taskManager.shutdown();

        exitModelMethod();
    }

    @Override
    public <T extends ObjectType> CompareResultType compareObject(PrismObject<T> provided,
            Collection<SelectorOptions<GetOperationOptions>> rawReadOptions, ModelCompareOptions compareOptions,
            @NotNull List<? extends ItemPath> ignoreItems, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException {
        Validate.notNull(provided, "Object must not be null or empty.");
        Validate.notNull(parentResult, "Operation result must not be null.");

        OperationResult result = parentResult.createMinorSubresult(COMPARE_OBJECT);
        result.addParam(OperationResult.PARAM_OID, provided.getOid());
        result.addParam(OperationResult.PARAM_NAME, provided.getName());
        result.addArbitraryObjectCollectionAsParam("readOptions", rawReadOptions);
        result.addArbitraryObjectAsParam("compareOptions", compareOptions);
        result.addArbitraryObjectCollectionAsParam("ignoreItems", ignoreItems);

        CompareResultType rv = new CompareResultType();

        try {
            var parsedReadOptions = preProcessOptionsSecurity(rawReadOptions, task, result);
            var readOptions = parsedReadOptions.getCollection();

            boolean c2p = ModelCompareOptions.isComputeCurrentToProvided(compareOptions);
            boolean p2c = ModelCompareOptions.isComputeProvidedToCurrent(compareOptions);
            boolean returnC = ModelCompareOptions.isReturnCurrent(compareOptions);
            boolean returnP = ModelCompareOptions.isReturnNormalized(compareOptions);
            boolean ignoreOperational = ModelCompareOptions.isIgnoreOperationalItems(compareOptions);

            if (!c2p && !p2c && !returnC && !returnP) {
                return rv;
            }
            PrismObject<T> current = null;
            if (c2p || p2c || returnC) {
                current = fetchCurrentObject(provided.getCompileTimeClass(), provided.getOid(), provided.getName(), readOptions, task, result);
                removeIgnoredItems(current, ignoreItems);
                if (ignoreOperational) {
                    removeOperationalItems(current);
                }
            }
            removeIgnoredItems(provided, ignoreItems);
            if (ignoreOperational) {
                removeOperationalItems(provided);
            }

            if (c2p) {
                rv.setCurrentToProvided(DeltaConvertor.toObjectDeltaType(DiffUtil.diff(current, provided)));
            }
            if (p2c) {
                rv.setProvidedToCurrent(DeltaConvertor.toObjectDeltaType(DiffUtil.diff(provided, current)));
            }
            if (returnC && current != null) {
                rv.setCurrentObject(current.asObjectable());
            }
            if (returnP) {
                rv.setNormalizedObject(provided.asObjectable());
            }
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
            result.cleanup();
        }
        return rv;
    }

    private <T extends ObjectType> void removeIgnoredItems(PrismObject<T> object, List<? extends ItemPath> ignoreItems) {
        if (object != null) {
            object.getValue().removeItems(ignoreItems);
        }
    }

    private <T extends ObjectType> void removeOperationalItems(PrismObject<T> object) {
        if (object != null) {
            object.getValue().removeOperationalItems();
        }
    }

    private <T extends ObjectType> PrismObject<T> fetchCurrentObject(Class<T> type, String oid, PolyString name,
            Collection<SelectorOptions<GetOperationOptions>> readOptions, Task task,
            OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {

        if (readOptions == null) {
            readOptions = new ArrayList<>();
        }
        GetOperationOptions root = SelectorOptions.findRootOptions(readOptions);
        if (root == null) {
            readOptions.add(SelectorOptions.create(GetOperationOptions.createAllowNotFound()));
        } else {
            root.setAllowNotFound(true);
        }

        if (oid != null) {
            try {
                return getObject(type, oid, readOptions, task, result);
            } catch (ObjectNotFoundException e) {
                return null;
            }
        }
        if (name == null || name.getOrig() == null) {
            throw new IllegalArgumentException("Neither OID nor name of the object is known.");
        }
        ObjectQuery nameQuery = prismContext.queryFor(type)
                .item(ObjectType.F_NAME).eqPoly(name.getOrig())
                .build();
        List<PrismObject<T>> objects = searchObjects(type, nameQuery, readOptions, task, result);
        if (objects.isEmpty()) {
            return null;
        } else if (objects.size() == 1) {
            return objects.get(0);
        } else {
            throw new SchemaException("More than 1 object of type " + type + " with the name of " + name + ": There are " + objects.size() + " of them.");
        }
    }

    private @NotNull ParsedGetOperationOptions preProcessOptionsSecurity(
            Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);
        if (GetOperationOptions.isAttachDiagData(rootOptions)
                && !securityEnforcer.isAuthorizedAll(task, result)) {
            Collection<SelectorOptions<GetOperationOptions>> reducedOptions = CloneUtil.cloneCollectionMembers(options);
            SelectorOptions.findRootOptions(reducedOptions).setAttachDiagData(false);
            return ParsedGetOperationOptions.of(reducedOptions);
        } else {
            return ParsedGetOperationOptions.of(options);
        }
    }

    private <T> ObjectQuery preProcessQuerySecurity(
            Class<T> objectType, ObjectQuery origQuery, GetOperationOptions rootOptions, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        ObjectFilter origFilter = origQuery != null ? origQuery.getFilter() : null;
        AuthorizationPhaseType phase =
                GetOperationOptions.isExecutionPhase(rootOptions) ? AuthorizationPhaseType.EXECUTION : null;
        ObjectFilter secFilter = securityEnforcer.preProcessObjectFilter(
                securityEnforcer.getMidPointPrincipal(),
                ModelAuthorizationAction.AUTZ_ACTIONS_URLS_SEARCH,
                ModelAuthorizationAction.AUTZ_ACTIONS_URLS_SEARCH_BY,
                phase, objectType, origFilter, null, List.of(), SecurityEnforcer.Options.create(), task, result);
        return updateObjectQuery(origQuery, secFilter);
    }

    private ObjectQuery updateObjectQuery(ObjectQuery origQuery, ObjectFilter updatedFilter) {
        if (origQuery != null) {
            origQuery.setFilter(updatedFilter);
            return origQuery;
        } else if (updatedFilter == null) {
            return null;
        } else {
            return getPrismContext().queryFactory().createQuery(updatedFilter);
        }
    }

    //region Task-related operations

    @Override
    public boolean suspendTasks(Collection<String> taskOids, long waitForStop, Task operationTask, OperationResult parentResult)
            throws SecurityViolationException, ObjectNotFoundException, SchemaException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException {
        List<PrismObject<TaskType>> resolvedTasks = preprocessTaskCollectionOperation(
                taskOids, ModelAuthorizationAction.SUSPEND_TASK, AuditEventType.SUSPEND_TASK, operationTask, parentResult);

        OperationResult taskOperationResult = parentResult.createSubresult(CLASS_NAME_WITH_DOT + "suspendTasks"); // details filled in taskManager
        try {
            boolean suspended = taskManager.suspendTasks(taskOids, waitForStop, taskOperationResult);

            postprocessTaskCollectionOperation(resolvedTasks,
                    AuditEventType.SUSPEND_TASK, operationTask, parentResult, taskOperationResult);
            return suspended;
        } catch (Throwable t) {
            taskOperationResult.recordFatalError(t);
            throw t;
        } finally {
            taskOperationResult.close();
        }
    }

    @Override
    public boolean suspendTask(String taskOid, long waitForStop, Task operationTask, OperationResult parentResult)
            throws SecurityViolationException, ObjectNotFoundException, SchemaException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException {
        List<PrismObject<TaskType>> resolvedTasks = preprocessTaskOperation(taskOid,
                ModelAuthorizationAction.SUSPEND_TASK, AuditEventType.SUSPEND_TASK, operationTask, parentResult);

        OperationResult taskOperationResult = parentResult.createSubresult(CLASS_NAME_WITH_DOT + "suspendTasks"); // details filled in taskManager
        try {
            boolean suspended = taskManager.suspendTask(taskOid, waitForStop, taskOperationResult);

            postprocessTaskCollectionOperation(resolvedTasks,
                    AuditEventType.SUSPEND_TASK, operationTask, parentResult, taskOperationResult);

            return suspended;
        } catch (Throwable t) {
            taskOperationResult.recordFatalError(t);
            throw t;
        } finally {
            taskOperationResult.close();
        }
    }

    @Override
    public boolean suspendTaskTree(String taskOid, long waitForStop, Task operationTask, OperationResult parentResult)
            throws SecurityViolationException, ObjectNotFoundException, SchemaException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException {
        List<PrismObject<TaskType>> resolvedTasks = preprocessTaskOperation(taskOid,
                ModelAuthorizationAction.SUSPEND_TASK, AuditEventType.SUSPEND_TASK, operationTask, parentResult);

        OperationResult taskOperationResult = parentResult.createSubresult(CLASS_NAME_WITH_DOT + "suspendTaskTree"); // details filled in taskManager
        try {
            boolean suspended = taskManager.suspendTaskTree(taskOid, waitForStop, taskOperationResult);

            postprocessTaskCollectionOperation(resolvedTasks,
                    AuditEventType.SUSPEND_TASK, operationTask, parentResult, taskOperationResult);

            return suspended;
        } catch (Throwable t) {
            taskOperationResult.recordFatalError(t);
            throw t;
        } finally {
            taskOperationResult.close();
        }
    }

    @Override
    public void suspendAndDeleteTasks(Collection<String> taskOids,
            long waitForStop, boolean alsoSubtasks, Task operationTask, OperationResult parentResult)
            throws SecurityViolationException, ObjectNotFoundException, SchemaException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException {
        List<PrismObject<TaskType>> resolvedTasks = preprocessTaskCollectionOperation(taskOids,
                ModelAuthorizationAction.DELETE, AuditEventType.DELETE_OBJECT, operationTask, parentResult);
        List<String> indestructibleTaskOids = getIndestructibleTaskOids(resolvedTasks, parentResult);
        taskOids.removeAll(indestructibleTaskOids);

        OperationResult taskOperationResult = parentResult.createSubresult(CLASS_NAME_WITH_DOT + "suspendAndDeleteTasks"); // details filled in taskManager
        try {
            if (!taskOids.isEmpty()) {
                taskManager.suspendAndDeleteTasks(taskOids, waitForStop, alsoSubtasks, taskOperationResult);
            }

            postprocessTaskCollectionOperation(resolvedTasks,
                    AuditEventType.DELETE_OBJECT, operationTask, parentResult, taskOperationResult);
        } catch (Throwable t) {
            taskOperationResult.recordFatalError(t);
            throw t;
        } finally {
            taskOperationResult.close();
        }
    }

    @Override
    public void suspendAndDeleteTask(String taskOid,
            long waitForStop, boolean alsoSubtasks, Task operationTask, OperationResult parentResult)
            throws SecurityViolationException, ObjectNotFoundException, SchemaException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException {
        List<PrismObject<TaskType>> resolvedTasks = preprocessTaskOperation(taskOid,
                ModelAuthorizationAction.DELETE, AuditEventType.DELETE_OBJECT, operationTask, parentResult);
        List<String> indestructibleTaskOids = getIndestructibleTaskOids(resolvedTasks, parentResult);

        OperationResult taskOperationResult = parentResult.createSubresult(CLASS_NAME_WITH_DOT + "suspendAndDeleteTask"); // details filled in taskManager
        try {
            if (indestructibleTaskOids.isEmpty() || !indestructibleTaskOids.contains(taskOid)) {
                taskManager.suspendAndDeleteTask(taskOid, waitForStop, alsoSubtasks, taskOperationResult);
            }

            postprocessTaskCollectionOperation(
                    resolvedTasks, AuditEventType.DELETE_OBJECT, operationTask, parentResult, taskOperationResult);
        } catch (Throwable t) {
            taskOperationResult.recordFatalError(t);
            throw t;
        } finally {
            taskOperationResult.close();
        }
    }

    private List<String> getIndestructibleTaskOids(
            List<PrismObject<TaskType>> allTasks, OperationResult parentResult) {
        List<String> indestructible = new ArrayList<>();
        for (PrismObject<TaskType> taskType : allTasks) {
            OperationResult result = parentResult.createMinorSubresult(CHECK_INDESTRUCTIBLE);
            try {
                checkIndestructible(taskType.asObjectable());
            } catch (IndestructibilityViolationException e) {
                indestructible.add(taskType.getOid());
                result.recordException(e);
            } finally {
                result.close();
            }
        }
        return indestructible;
    }

    @Override
    public void resumeTasks(Collection<String> taskOids, Task operationTask, OperationResult parentResult)
            throws SecurityViolationException, ObjectNotFoundException, SchemaException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException {
        List<PrismObject<TaskType>> resolvedTasks = preprocessTaskCollectionOperation(taskOids,
                ModelAuthorizationAction.RESUME_TASK, AuditEventType.RESUME_TASK, operationTask, parentResult);

        OperationResult taskOperationResult = parentResult.createSubresult(CLASS_NAME_WITH_DOT + "resumeTasks"); // details filled in taskManager
        try {
            taskManager.resumeTasks(taskOids, taskOperationResult);

            postprocessTaskCollectionOperation(resolvedTasks,
                    AuditEventType.RESUME_TASK, operationTask, parentResult, taskOperationResult);
        } catch (Throwable t) {
            taskOperationResult.recordFatalError(t);
            throw t;
        } finally {
            taskOperationResult.close();
        }
    }

    @Override
    public void resumeTask(String taskOid, Task operationTask, OperationResult parentResult)
            throws SecurityViolationException, ObjectNotFoundException, SchemaException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException {
        List<PrismObject<TaskType>> resolvedTasks = preprocessTaskOperation(taskOid,
                ModelAuthorizationAction.RESUME_TASK, AuditEventType.RESUME_TASK, operationTask, parentResult);

        OperationResult taskOperationResult = parentResult.createSubresult(CLASS_NAME_WITH_DOT + "resumeTasks"); // details filled in taskManager
        try {
            taskManager.resumeTask(taskOid, taskOperationResult);

            postprocessTaskCollectionOperation(resolvedTasks,
                    AuditEventType.RESUME_TASK, operationTask, parentResult, taskOperationResult);
        } catch (Throwable t) {
            taskOperationResult.recordFatalError(t);
            throw t;
        } finally {
            taskOperationResult.close();
        }
    }

    @Override
    public void resumeTaskTree(String coordinatorOid, Task operationTask, OperationResult parentResult)
            throws SecurityViolationException, ObjectNotFoundException, SchemaException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException {
        List<PrismObject<TaskType>> resolvedTasks = preprocessTaskOperation(coordinatorOid,
                ModelAuthorizationAction.RESUME_TASK, AuditEventType.RESUME_TASK, operationTask, parentResult);

        OperationResult taskOperationResult = parentResult.createSubresult(CLASS_NAME_WITH_DOT + "resumeTaskTree"); // details filled in taskManager
        try {
            taskManager.resumeTaskTree(coordinatorOid, taskOperationResult);

            postprocessTaskCollectionOperation(resolvedTasks,
                    AuditEventType.RESUME_TASK, operationTask, parentResult, taskOperationResult);
        } catch (Throwable t) {
            taskOperationResult.recordFatalError(t);
            throw t;
        } finally {
            taskOperationResult.close();
        }
    }

    @Override
    public void scheduleTasksNow(Collection<String> taskOids, Task operationTask, OperationResult parentResult)
            throws SecurityViolationException, ObjectNotFoundException, SchemaException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException {
        List<PrismObject<TaskType>> resolvedTasks = preprocessTaskCollectionOperation(taskOids,
                ModelAuthorizationAction.RUN_TASK_IMMEDIATELY, AuditEventType.RUN_TASK_IMMEDIATELY, operationTask, parentResult);

        OperationResult taskOperationResult = parentResult.createSubresult(CLASS_NAME_WITH_DOT + "scheduleTasksNow"); // details filled in taskManager
        try {
            taskManager.scheduleTasksNow(taskOids, taskOperationResult);

            postprocessTaskCollectionOperation(resolvedTasks,
                    AuditEventType.RUN_TASK_IMMEDIATELY, operationTask, parentResult, taskOperationResult);
        } catch (Throwable t) {
            taskOperationResult.recordFatalError(t);
            throw t;
        } finally {
            taskOperationResult.close();
        }
    }

    @Override
    public void scheduleTaskNow(String taskOid, Task operationTask, OperationResult parentResult)
            throws SecurityViolationException, ObjectNotFoundException, SchemaException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException {
        List<PrismObject<TaskType>> resolvedTasks = preprocessTaskOperation(taskOid,
                ModelAuthorizationAction.RUN_TASK_IMMEDIATELY, AuditEventType.RUN_TASK_IMMEDIATELY, operationTask, parentResult);

        OperationResult taskOperationResult = parentResult.createSubresult(CLASS_NAME_WITH_DOT + "scheduleTasksNow"); // details filled in taskManager
        try {
            taskManager.scheduleTaskNow(taskOid, taskOperationResult);

            postprocessTaskCollectionOperation(resolvedTasks,
                    AuditEventType.RUN_TASK_IMMEDIATELY, operationTask, parentResult, taskOperationResult);
        } catch (Throwable t) {
            taskOperationResult.recordFatalError(t);
            throw t;
        } finally {
            taskOperationResult.close();
        }
    }

    @Override
    public PrismObject<TaskType> getTaskByIdentifier(String identifier,
            Collection<SelectorOptions<GetOperationOptions>> rawOptions, Task operationTask, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException, CommunicationException {
        var parsedOptions = preProcessOptionsSecurity(rawOptions, operationTask, parentResult);
        var options = parsedOptions.getCollection();
        PrismObject<TaskType> task = taskManager.getTaskTypeByIdentifier(identifier, options, parentResult);
        return schemaTransformer.applySchemasAndSecurityToObject(task, parsedOptions, operationTask, parentResult);
    }

    @Override
    public boolean deactivateServiceThreads(long timeToWait, Task operationTask, OperationResult parentResult)
            throws SchemaException, SecurityViolationException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException {
        securityEnforcer.authorize(
                ModelAuthorizationAction.STOP_SERVICE_THREADS.getUrl(), operationTask, parentResult);
        return taskManager.deactivateServiceThreads(timeToWait, parentResult);
    }

    @Override
    public void reactivateServiceThreads(Task operationTask, OperationResult parentResult)
            throws SchemaException, SecurityViolationException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException {
        securityEnforcer.authorize(
                ModelAuthorizationAction.START_SERVICE_THREADS.getUrl(), operationTask, parentResult);
        taskManager.reactivateServiceThreads(parentResult);
    }

    @Override
    public boolean getServiceThreadsActivationState() {
        return taskManager.getServiceThreadsActivationState();
    }

    @Override
    public void stopSchedulers(Collection<String> nodeIdentifiers, Task operationTask, OperationResult parentResult)
            throws SecurityViolationException, ObjectNotFoundException, SchemaException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException {
        authorizeNodeCollectionOperation(ModelAuthorizationAction.STOP_TASK_SCHEDULER, nodeIdentifiers, operationTask, parentResult);
        taskManager.stopSchedulers(nodeIdentifiers, parentResult);
    }

    @Override
    public boolean stopSchedulersAndTasks(
            Collection<String> nodeIdentifiers, long waitTime, Task operationTask, OperationResult parentResult)
            throws SecurityViolationException, ObjectNotFoundException, SchemaException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException {
        authorizeNodeCollectionOperation(ModelAuthorizationAction.STOP_TASK_SCHEDULER, nodeIdentifiers, operationTask, parentResult);
        return taskManager.stopSchedulersAndTasks(nodeIdentifiers, waitTime, parentResult);
    }

    @Override
    public void startSchedulers(Collection<String> nodeIdentifiers, Task operationTask, OperationResult parentResult)
            throws SecurityViolationException, ObjectNotFoundException, SchemaException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException {
        authorizeNodeCollectionOperation(ModelAuthorizationAction.START_TASK_SCHEDULER, nodeIdentifiers, operationTask, parentResult);
        taskManager.startSchedulers(nodeIdentifiers, parentResult);
    }

    @Override
    public void synchronizeTasks(Task operationTask, OperationResult parentResult)
            throws SchemaException, SecurityViolationException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException {
        securityEnforcer.authorize(
                ModelAuthorizationAction.SYNCHRONIZE_TASKS.getUrl(), operationTask, parentResult);
        taskManager.synchronizeTasks(parentResult);
    }

    @Override
    public void reconcileWorkers(String oid, Task opTask, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException,
            SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        securityEnforcer.authorizeAll(opTask, result); // temporary
        activityManager.reconcileWorkers(oid, result);
    }

    @Override
    public void deleteActivityStateAndWorkers(String rootTaskOid,
            boolean deleteWorkers, long subtasksWaitTime, Task operationTask, OperationResult parentResult)
            throws SecurityViolationException, ObjectNotFoundException, SchemaException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException {
        securityEnforcer.authorizeAll(operationTask, parentResult); // temporary
        activityManager.deleteActivityStateAndWorkers(rootTaskOid, deleteWorkers, subtasksWaitTime, parentResult);
    }

    private List<PrismObject<TaskType>> preprocessTaskOperation(
            String oid, ModelAuthorizationAction action, AuditEventType event, Task task, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException,
            SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        return preprocessTaskCollectionOperation(singletonList(oid), action, event, task, result);
    }

    private List<PrismObject<TaskType>> preprocessTaskCollectionOperation(Collection<String> oids,
            ModelAuthorizationAction action, AuditEventType eventType, Task task, OperationResult parentResult)
            throws CommunicationException, ObjectNotFoundException, SchemaException,
            SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        List<PrismObject<TaskType>> tasks = createTaskList(oids, parentResult);
        authorizeResolvedTaskCollectionOperation(action, tasks, task, parentResult);
        auditTaskCollectionOperation(tasks, eventType, AuditEventStage.REQUEST, task, parentResult, null);
        return tasks;
    }

    private void postprocessTaskCollectionOperation(Collection<PrismObject<TaskType>> resolvedTasks,
            AuditEventType eventType, Task task, OperationResult result, OperationResult taskOperationResult) {
        result.computeStatusIfUnknown();
        auditTaskCollectionOperation(resolvedTasks, eventType, AuditEventStage.EXECUTION, task, result, taskOperationResult);
    }

    private void authorizeResolvedTaskCollectionOperation(ModelAuthorizationAction action,
            Collection<PrismObject<TaskType>> existingTasks, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException {
        if (securityEnforcer.isAuthorizedAll(task, parentResult)) {
            return;
        }
        for (PrismObject<TaskType> existingObject : existingTasks) {
            securityEnforcer.authorize(
                    action.getUrl(), null,
                    AuthorizationParameters.Builder.buildObject(existingObject), task, parentResult);
        }
    }

    private void authorizeNodeCollectionOperation(
            ModelAuthorizationAction action, Collection<String> identifiers, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException {
        if (securityEnforcer.isAuthorizedAll(task, parentResult)) {
            return;
        }
        for (String identifier : identifiers) {
            PrismObject<NodeType> existingObject;
            ObjectQuery q = ObjectQueryUtil.createNameQuery(NodeType.class, identifier);
            List<PrismObject<NodeType>> nodes = cacheRepositoryService.searchObjects(NodeType.class, q, readOnly(), parentResult);
            if (nodes.isEmpty()) {
                throw new ObjectNotFoundException(
                        "Node with identifier '" + identifier + "' couldn't be found.",
                        NodeType.class,
                        identifier);
            } else if (nodes.size() > 1) {
                throw new SystemException("Multiple nodes with identifier '" + identifier + "'");
            }
            existingObject = nodes.get(0);
            securityEnforcer.authorize(
                    action.getUrl(), null,
                    AuthorizationParameters.Builder.buildObject(existingObject), task, parentResult);
        }
    }

    private List<PrismObject<TaskType>> createTaskList(Collection<String> taskOids, OperationResult parentResult) throws SchemaException, ObjectNotFoundException {
        List<PrismObject<TaskType>> taskRefs = new ArrayList<>(taskOids.size());

        for (String taskOid : taskOids) {
            var options = SchemaService.get().getOperationOptionsBuilder()
                    .raw() // why?
                    .readOnly()
                    .build();
            PrismObject<TaskType> task = cacheRepositoryService.getObject(TaskType.class, taskOid, options, parentResult);
            taskRefs.add(task);
        }
        return taskRefs;
    }

    private void auditTaskCollectionOperation(Collection<PrismObject<TaskType>> existingTasks, AuditEventType eventType,
            AuditEventStage stage, Task task, OperationResult result, @Nullable OperationResult taskOperationResult) {
        for (PrismObject<TaskType> existingTask : existingTasks) {
            PrismReferenceValue taskRef = ObjectTypeUtil.createObjectRef(existingTask, SchemaConstants.ORG_DEFAULT).asReferenceValue();
            auditTaskOperation(taskRef, eventType, stage, task, result, taskOperationResult);
        }
    }

    private void auditTaskOperation(PrismReferenceValue taskRef, AuditEventType event,
            AuditEventStage stage, Task operationTask, OperationResult parentResult, @Nullable OperationResult taskOperationResult) {
        AuditEventRecord auditRecord = new AuditEventRecord(event, stage);
        String requestIdentifier = ModelImplUtils.generateRequestIdentifier();
        auditRecord.setRequestIdentifier(requestIdentifier);
        auditRecord.setTargetRef(taskRef);
        ObjectDelta<TaskType> delta;
        if (AuditEventType.DELETE_OBJECT == event) {
            delta = prismContext.deltaFactory().object().createDeleteDelta(TaskType.class, taskRef.getOid());
        } else {
            //TODO should we somehow indicate deltas which are executed in taskManager?
            delta = prismContext.deltaFactory().object().createEmptyModifyDelta(TaskType.class, taskRef.getOid());
        }
        ObjectDeltaOperation<TaskType> odo = new ObjectDeltaOperation<>(delta, taskOperationResult);
        auditRecord.addDelta(odo);
        if (taskOperationResult != null) { // for EXECUTION stage
            auditRecord.setOutcome(taskOperationResult.getStatus());
        }
        auditHelper.audit(auditRecord, null, operationTask, parentResult);
    }

    //endregion

    //region Case-related operations
    // TODO move this method to more appropriate place
    @Override
    public void completeWorkItem(
            @NotNull WorkItemId workItemId,
            @NotNull AbstractWorkItemOutputType output,
            @Nullable ObjectDelta<?> additionalDelta,
            @NotNull Task task,
            @NotNull OperationResult parentResult)
            throws SecurityViolationException, SchemaException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException {
        AbstractWorkItemOutputType outputToUse;
        if (additionalDelta != null && ApprovalUtils.isApproved(output)) {
            ObjectDeltaType additionalDeltaBean = DeltaConvertor.toObjectDeltaType(additionalDelta);
            ObjectTreeDeltasType treeDeltas = new ObjectTreeDeltasType();
            treeDeltas.setFocusPrimaryDelta(additionalDeltaBean);

            WorkItemResultType newOutput = new WorkItemResultType();
            //noinspection unchecked
            newOutput.asPrismContainerValue().mergeContent(output.asPrismContainerValue(), emptyList());
            newOutput.setAdditionalDeltas(treeDeltas);
            outputToUse = newOutput;
        } else {
            outputToUse = output;
        }
        getCaseManagerRequired().completeWorkItem(workItemId, outputToUse, null, task, parentResult);
    }

    // TODO move this method to more appropriate place
    @Override
    public void completeWorkItem(
            @NotNull WorkItemId workItemId,
            @NotNull AbstractWorkItemOutputType output,
            @NotNull Task task,
            @NotNull OperationResult parentResult)
            throws SecurityViolationException, SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException {
        getCaseManagerRequired().completeWorkItem(workItemId, output, null, task, parentResult);
    }

    @Override
    public void cancelCase(@NotNull String caseOid, @NotNull Task task, @NotNull OperationResult parentResult) throws SchemaException,
            ObjectNotFoundException, SecurityViolationException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, ObjectAlreadyExistsException {
        getCaseManagerRequired().cancelCase(caseOid, task, parentResult);
    }

    @Override
    public void claimWorkItem(@NotNull WorkItemId workItemId, @NotNull Task task, @NotNull OperationResult parentResult)
            throws SecurityViolationException, ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        getCaseManagerRequired().claimWorkItem(workItemId, task, parentResult);
    }

    @Override
    public void releaseWorkItem(@NotNull WorkItemId workItemId, @NotNull Task task, @NotNull OperationResult parentResult)
            throws ObjectNotFoundException, SecurityViolationException, SchemaException, ObjectAlreadyExistsException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        getCaseManagerRequired().releaseWorkItem(workItemId, task, parentResult);
    }

    @Override
    public void delegateWorkItem(@NotNull WorkItemId workItemId, @NotNull WorkItemDelegationRequestType delegationRequest,
            @NotNull Task task, @NotNull OperationResult parentResult) throws ObjectNotFoundException, SecurityViolationException, SchemaException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException {
        getCaseManagerRequired().delegateWorkItem(workItemId, delegationRequest, task, parentResult);
    }

    //endregion

    //region Scripting (bulk actions)
    @Override
    public BulkActionExecutionResult executeBulkAction(
            @NotNull ExecuteScriptConfigItem scriptExecutionCommand,
            @NotNull VariablesMap initialVariables,
            @NotNull BulkActionExecutionOptions options,
            @NotNull Task task, @NotNull OperationResult result)
            throws SchemaException, SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, PolicyViolationException, ObjectAlreadyExistsException {
        // Authorization checking was moved to bulk action executor.
        ExecutionContext executionContext =
                bulkActionsExecutor.execute(
                        scriptExecutionCommand,
                        initialVariables,
                        options,
                        task, result);
        return executionContext.toExecutionResult();
    }
    //endregion

    //region Certification

    @Override
    public AccessCertificationCasesStatisticsType getCampaignStatistics(String campaignOid, boolean currentStageOnly, Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, SecurityViolationException, ObjectAlreadyExistsException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
        return getCertificationManagerRequired().getCampaignStatistics(campaignOid, currentStageOnly, task, parentResult);
    }

    @Override
    public void cleanupCampaigns(@NotNull CleanupPolicyType policy, Task task, OperationResult result) {
        getCertificationManagerRequired().cleanupCampaigns(policy, task, result);
    }

    @Override
    public void recordDecision(
            String campaignOid,
            long caseId,
            long workItemId,
            AccessCertificationResponseType response,
            String comment,
            Task task,
            OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, SecurityViolationException, ObjectAlreadyExistsException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException {
        getCertificationManagerRequired().recordDecision(
                AccessCertificationWorkItemId.of(campaignOid, caseId, workItemId),
                response, comment, false, task, parentResult);
    }

    @Override
    public List<AccessCertificationWorkItemType> searchOpenWorkItems(
            ObjectQuery baseWorkItemsQuery,
            boolean notDecidedOnly,
            boolean allItems,
            Collection<SelectorOptions<GetOperationOptions>> options,
            Task task,
            OperationResult result)
            throws ObjectNotFoundException, SchemaException, SecurityViolationException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException {
        return searchContainers(
                AccessCertificationWorkItemType.class,
                QueryUtils.createQueryForOpenWorkItems(
                        baseWorkItemsQuery,
                        allItems ? null : SecurityUtil.getPrincipalRequired(),
                        notDecidedOnly),
                options,
                task,
                result);
    }

    @Override
    public int countOpenWorkItems(
            ObjectQuery baseWorkItemsQuery,
            boolean notDecidedOnly,
            boolean allItems,
            Collection<SelectorOptions<GetOperationOptions>> options,
            Task task,
            OperationResult result)
            throws ObjectNotFoundException, SchemaException, SecurityViolationException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException {
        return countContainers(
                AccessCertificationWorkItemType.class,
                QueryUtils.createQueryForOpenWorkItems(
                        baseWorkItemsQuery,
                        allItems ? null : SecurityUtil.getPrincipalRequired(),
                        notDecidedOnly),
                options,
                task,
                result);
    }

    @Override
    public void closeCampaign(String campaignOid, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, SecurityViolationException, ObjectAlreadyExistsException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
//        getCertificationManagerRequired().closeCampaignTask(campaignOid, task, result);
        getCertificationManagerRequired().closeCampaign(campaignOid, task, result);
    }

    @Override
    public void reiterateCampaign(String campaignOid, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, SecurityViolationException, ObjectAlreadyExistsException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
        getCertificationManagerRequired().reiterateCampaignTask(campaignOid, task, result);
    }

    @Override
    public void startRemediation(String campaignOid, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, SecurityViolationException, ObjectAlreadyExistsException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
        getCertificationManagerRequired().startRemediation(campaignOid, task, result);
    }

    @Override
    public void closeCurrentStage(String campaignOid, Task task, OperationResult parentResult) throws SchemaException, SecurityViolationException, ObjectNotFoundException, ObjectAlreadyExistsException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
        getCertificationManagerRequired().closeCurrentStageTask(campaignOid, task, parentResult);
    }

    @Override
    public void openNextStage(String campaignOid, Task task, OperationResult parentResult) throws SchemaException, SecurityViolationException, ObjectNotFoundException, ObjectAlreadyExistsException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
        getCertificationManagerRequired().createNextStageTask(campaignOid, task, parentResult);
    }

    @Override
    public void openNextStage(AccessCertificationCampaignType campaign, Task task, OperationResult parentResult) throws SchemaException, SecurityViolationException, ObjectNotFoundException, ObjectAlreadyExistsException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
        getCertificationManagerRequired().createNextStageTask(campaign, task, parentResult);
    }


    @Override
    public AccessCertificationCampaignType createCampaign(String definitionOid, Task task, OperationResult parentResult) throws SchemaException, SecurityViolationException, ObjectNotFoundException, ObjectAlreadyExistsException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
        return getCertificationManagerRequired().createCampaign(definitionOid, task, parentResult);
    }
    //endregion

    @Override
    public <O extends ObjectType> Collection<ObjectDeltaOperation<? extends ObjectType>> mergeObjects(Class<O> type,
            String leftOid, String rightOid, String mergeConfigurationName, Task task, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ConfigurationException,
            ObjectAlreadyExistsException, ExpressionEvaluationException, CommunicationException,
            PolicyViolationException, SecurityViolationException {

        OperationResult result = parentResult.createSubresult(MERGE_OBJECTS);
        result.addParam("leftOid", leftOid);
        result.addParam("rightOid", rightOid);
        result.addParam("class", type);

        enterModelMethod();

        try {
            return objectMerger.mergeObjects(type, leftOid, rightOid, mergeConfigurationName, task, result);
        } catch (ObjectNotFoundException | SchemaException | ConfigurationException | ObjectAlreadyExistsException |
                ExpressionEvaluationException | CommunicationException | PolicyViolationException | SecurityViolationException |
                RuntimeException | Error e) {
            ModelImplUtils.recordFatalError(result, e);
            throw e;
        } finally {
            QNameUtil.setTemporarilyTolerateUndeclaredPrefixes(false);
            exitModelMethod();
            result.close();
        }
    }

    @NotNull
    @Override
    public PrismContext getPrismContext() {
        return prismContext;
    }

    private void enterModelMethod() {
        clockworkMedic.enterModelMethod(true);
    }

    private void enterModelMethodNoRepoCache() {
        clockworkMedic.enterModelMethod(false);
    }

    private void exitModelMethod() {
        clockworkMedic.exitModelMethod(true);
    }

    private void exitModelMethodNoRepoCache() {
        clockworkMedic.exitModelMethod(false);
    }

    //region Case Management

    @Override
    public String getThreadsDump(@NotNull Task task, @NotNull OperationResult parentResult)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        securityEnforcer.authorize(
                ModelAuthorizationAction.READ_THREADS.getUrl(), task, parentResult);
        return MiscUtil.takeThreadDump(null);
    }

    @Override
    public String getRunningTasksThreadsDump(@NotNull Task task, @NotNull OperationResult parentResult)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        securityEnforcer.authorize(
                ModelAuthorizationAction.READ_THREADS.getUrl(), task, parentResult);
        return taskManager.getRunningTasksThreadsDump(parentResult);
    }

    @Override
    public String recordRunningTasksThreadsDump(String cause, @NotNull Task task, @NotNull OperationResult parentResult)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException, ObjectAlreadyExistsException {
        securityEnforcer.authorize(
                ModelAuthorizationAction.READ_THREADS.getUrl(), task, parentResult);
        return taskManager.recordRunningTasksThreadsDump(cause, parentResult);
    }

    @Override
    public String getTaskThreadsDump(@NotNull String taskOid, @NotNull Task task, @NotNull OperationResult parentResult)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        securityEnforcer.authorize(
                ModelAuthorizationAction.READ_THREADS.getUrl(), task, parentResult);
        return taskManager.getTaskThreadsDump(taskOid, parentResult);
    }

    //endregion

    public void notifyChange(ResourceObjectShadowChangeDescriptionType changeDescription, Task task, OperationResult parentResult)
            throws CommonException {

        OperationResult result = parentResult.createSubresult(NOTIFY_CHANGE);
        try {
            PrismObject<ShadowType> oldRepoShadow = getOldRepoShadow(changeDescription, task, result);
            PrismObject<ShadowType> resourceObject = getResourceObject(changeDescription);
            ObjectDelta<ShadowType> objectDelta = getObjectDelta(changeDescription, result);

            securityEnforcer.authorize(ModelAuthorizationAction.NOTIFY_CHANGE.getUrl(), task, result);

            ExternalResourceEvent event = new ExternalResourceEvent(objectDelta, resourceObject,
                    oldRepoShadow, changeDescription.getChannel());

            LOGGER.trace("Created event description:\n{}", event.debugDumpLazily());
            dispatcher.notifyEvent(event, task, result);
        } catch (TunnelException te) {
            Throwable cause = te.getCause();
            if (cause instanceof CommonException) {
                result.recordFatalError(cause);
                throw (CommonException) cause;
            } else if (cause != null) {
                result.recordFatalError(cause);
                throw new SystemException(cause);
            } else {
                // very strange case
                result.recordFatalError(te);
                throw te;
            }
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.close();
        }
    }

    @Nullable
    private ObjectDelta<ShadowType> getObjectDelta(ResourceObjectShadowChangeDescriptionType changeDescription,
            OperationResult parentResult) throws SchemaException {

        ObjectDeltaType deltaBean = changeDescription.getObjectDelta();
        if (deltaBean != null) {
            PrismObject<ShadowType> shadowToAdd;
            ObjectDelta<ShadowType> objectDelta = prismContext.deltaFactory().object().createEmptyDelta(ShadowType.class, deltaBean.getOid(),
                    ChangeType.toChangeType(deltaBean.getChangeType()));

            // Couldn't we use simply use delta convertor here?
            if (objectDelta.getChangeType() == ChangeType.ADD) {
                if (deltaBean.getObjectToAdd() == null) {
                    throw new IllegalArgumentException("No object to add specified. Check your delta. Add delta must contain object to add");
                }
                Object objToAdd = deltaBean.getObjectToAdd();
                if (!(objToAdd instanceof ShadowType)) {
                    throw new IllegalArgumentException("Wrong object specified in change description. Expected on the the shadow type, but got " + objToAdd.getClass().getSimpleName());
                }
                prismContext.adopt((ShadowType) objToAdd); // really needed?
                shadowToAdd = ((ShadowType) objToAdd).asPrismObject();
                objectDelta.setObjectToAdd(shadowToAdd);
            } else {
                Collection<? extends ItemDelta<?, ?>> modifications = DeltaConvertor.toModifications(
                        deltaBean.getItemDelta(),
                        prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ShadowType.class));
                objectDelta.addModifications(modifications);
            }
            ModelImplUtils.encrypt(Collections.singletonList(objectDelta), protector, null, parentResult);
            return objectDelta;
        } else {
            return null;
        }
    }

    @Nullable
    private PrismObject<ShadowType> getResourceObject(ResourceObjectShadowChangeDescriptionType changeDescription) throws SchemaException {
        ShadowType resourceObjectBean = changeDescription.getCurrentShadow();
        if (resourceObjectBean != null) {
            PrismObject<ShadowType> resourceObject = resourceObjectBean.asPrismObject();
            prismContext.adopt(resourceObject); // really needed?
            return resourceObject;
        } else {
            return null;
        }
    }

    private PrismObject<ShadowType> getOldRepoShadow(ResourceObjectShadowChangeDescriptionType change, Task task,
            OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        String oid = change.getOldShadowOid();
        if (oid != null) {
            return getObject(ShadowType.class, oid, GetOperationOptions.createNoFetchReadOnlyCollection(), task, result);
        } else {
            return null;
        }
    }

    private void computePolyStrings(Collection<ObjectDelta<? extends ObjectType>> deltas) {
        for (ObjectDelta<? extends ObjectType> delta : deltas) {
            //noinspection unchecked
            delta.accept(this::computePolyStringVisit);
        }
    }

    private void computePolyStringVisit(Visitable<?> visitable) {
        if (!(visitable instanceof PrismProperty<?>)) {
            return;
        }
        PrismPropertyDefinition<?> definition = ((PrismProperty<?>) visitable).getDefinition();
        if (definition == null) {
            return;
        }
        if (!QNameUtil.match(PolyStringType.COMPLEX_TYPE, definition.getTypeName())) {
            return;
        }
        for (PrismPropertyValue<PolyString> pval : ((PrismProperty<PolyString>) visitable).getValues()) {
            PolyString polyString = pval.getValue();
            if (polyString != null && polyString.getOrig() == null) {
                String orig = localizationService.translate(polyString);
                LOGGER.trace("PPPP1: Filling out orig value of polyString {}: {}", polyString, orig);
                polyString.setComputedOrig(orig);
                polyString.recompute(prismContext.getDefaultPolyStringNormalizer());
                LOGGER.trace("PPPP2: Resulting polyString: {}", polyString);
            }
        }
    }

    @Override
    public boolean isSupportedByRepository(@NotNull Class<? extends ObjectType> type) {
        return cacheRepositoryService.supports(type);
    }

    @Override
    public <O extends ObjectType> ProcessedObjectImpl<O> parseProcessedObject(
            @NotNull SimulationResultProcessedObjectType bean, @NotNull Task task, @NotNull OperationResult result)
            throws SchemaException {
        //noinspection unchecked
        PrismContainerValue<SimulationResultProcessedObjectType> pcv = bean.asPrismContainerValue();
        Object existing = pcv.getUserData(ProcessedObjectImpl.KEY_PARSED);
        if (existing != null) {
            //noinspection unchecked
            return (ProcessedObjectImpl<O>) existing;
        }

        ProcessedObjectImpl<O> parsed = ProcessedObjectImpl.parse(bean);
        try {
            parsed.applyDefinitions(task, result);
        } catch (CommonException e) {
            LoggingUtils.logException(LOGGER, "Couldn't apply definitions on {}", e, parsed);
        }
        pcv.setUserData(ProcessedObjectImpl.KEY_PARSED, parsed);
        return parsed;
    }
}
