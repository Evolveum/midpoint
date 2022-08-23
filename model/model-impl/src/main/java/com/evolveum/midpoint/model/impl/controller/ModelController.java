/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.controller;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import static com.evolveum.midpoint.schema.GetOperationOptions.createReadOnlyCollection;
import static com.evolveum.midpoint.util.DebugUtil.lazy;

import java.io.*;
import java.util.Objects;
import java.util.*;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.cases.api.CaseManager;

import com.evolveum.midpoint.provisioning.api.*;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.util.*;

import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityCollectionType;

import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.certification.api.CertificationManager;
import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.api.*;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipalManager;
import com.evolveum.midpoint.model.api.hooks.HookRegistry;
import com.evolveum.midpoint.model.api.hooks.ReadHook;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.model.impl.importer.ObjectImporter;
import com.evolveum.midpoint.model.impl.lens.*;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.impl.scripting.ScriptingExpressionEvaluator;
import com.evolveum.midpoint.model.impl.sync.tasks.imp.ImportFromResourceLauncher;
import com.evolveum.midpoint.model.common.util.AuditHelper;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.DiffUtil;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.activity.TaskActivityManager;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultRunner;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
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
import com.evolveum.midpoint.schema.util.cases.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.CompareResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ScriptingExpressionType;
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
public class ModelController implements ModelService, TaskService, CaseService, ScriptingService, AccessCertificationService {

    // Constants for OperationResult
    private static final String CLASS_NAME_WITH_DOT = ModelController.class.getName() + ".";
    private static final String RESOLVE_REFERENCE = CLASS_NAME_WITH_DOT + "resolveReference";
    private static final String OP_APPLY_PROVISIONING_DEFINITION = CLASS_NAME_WITH_DOT + "applyProvisioningDefinition";

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
    @Autowired private ScriptingExpressionEvaluator scriptingExpressionEvaluator;
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
    @Autowired private EventDispatcher dispatcher;
    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService cacheRepositoryService;

    @Autowired(required = false)                        // not required in all circumstances
    private CaseManager caseManager;

    @Autowired(required = false)                        // not required in all circumstances
    private CertificationManager certificationManager;

    public ModelObjectResolver getObjectResolver() {
        return objectResolver;
    }

    private CaseManager getCaseManagerChecked() {
        if (caseManager == null) {
            throw new SystemException("Case manager not present");
        }
        return caseManager;
    }

    private CertificationManager getCertificationManagerChecked() {
        if (certificationManager == null) {
            throw new SystemException("Certification manager not present");
        }
        return certificationManager;
    }

    @NotNull
    @Override
    public <T extends ObjectType> PrismObject<T> getObject(Class<T> clazz, String oid,
            Collection<SelectorOptions<GetOperationOptions>> rawOptions, Task task, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        Validate.notEmpty(oid, "Object oid must not be null or empty.");
        Validate.notNull(parentResult, "Operation result must not be null.");
        Validate.notNull(clazz, "Object class must not be null.");
        enterModelMethod();

        PrismObject<T> object;

        OP_LOGGER.trace("MODEL OP enter getObject({},{},{})", clazz.getSimpleName(), oid, rawOptions);

        GetOperationOptions rootOptions = null;

        OperationResult result = parentResult.subresult(GET_OBJECT)
                .setMinor()
                .addParam("oid", oid)
                .addArbitraryObjectCollectionAsParam("options", rawOptions)
                .addParam("class", clazz)
                .build();
        try {
            Collection<SelectorOptions<GetOperationOptions>> options = preProcessOptionsSecurity(rawOptions, task, parentResult);
            rootOptions = SelectorOptions.findRootOptions(options);

            if (GetOperationOptions.isRaw(rootOptions)) { // MID-2218
                QNameUtil.setTemporarilyTolerateUndeclaredPrefixes(true);
            }
            ModelImplUtils.clearRequestee(task);

//            Special-purpose code to hunt down read-write resource fetch from GUI.
//            Normally the code is not active. It is too brutal. Just for MID-3424.
//            if (ResourceType.class == clazz && !GetOperationOptions.isRaw(rootOptions) && !GetOperationOptions.isReadOnly(rootOptions)) {
//                LOGGER.info("READWRITE resource get: {} {}:\n{}", oid, options,
//                        LoggingUtils.dumpStackTrace());
//            }

            //noinspection unchecked
            object = (PrismObject<T>) objectResolver.getObject(clazz, oid, options, task, result).asPrismObject();

            object = object.cloneIfImmutable();
            schemaTransformer.applySchemasAndSecurity(object, rootOptions, options, null, task, result);
            executeResolveOptions(object.asObjectable(), options, task, result);

        } catch (SchemaException | CommunicationException | ConfigurationException | SecurityViolationException
                | ExpressionEvaluationException | RuntimeException | Error e) {
            OP_LOGGER.debug("MODEL OP error getObject({},{},{}): {}: {}", clazz.getSimpleName(), oid, rawOptions, e.getClass().getSimpleName(), e.getMessage());
            ModelImplUtils.recordFatalError(result, e);
            throw e;
        } catch (ObjectNotFoundException e) {
            OP_LOGGER.debug("MODEL OP error getObject({},{},{}): {}: {}", clazz.getSimpleName(), oid, rawOptions, e.getClass().getSimpleName(), e.getMessage());
            if (GetOperationOptions.isAllowNotFound(rootOptions)) {
                // TODO check if this is really needed (lower layers shouldn't produce FATAL_ERROR if "allow not found" is true)
                // FIXME there is no "last subresult" if the called method throws this exception because of object type mismatch!
                result.getLastSubresult().setStatus(OperationResultStatus.HANDLED_ERROR);
            } else {
                ModelImplUtils.recordFatalError(result, e);
            }
            throw e;
        } finally {
            result.computeStatusIfUnknown();
            QNameUtil.setTemporarilyTolerateUndeclaredPrefixes(false);
            exitModelMethod();
        }

        result.cleanupResult();

        OP_LOGGER.debug("MODEL OP exit getObject({},{},{}): {}", clazz.getSimpleName(), oid, rawOptions, object);
        if (OP_LOGGER.isTraceEnabled()) {
            OP_LOGGER.trace("MODEL OP exit getObject({},{},{}):\n{}", clazz.getSimpleName(), oid, rawOptions, object.debugDump(1));
        }
        return object;
    }

    private void executeResolveOptions(@NotNull Containerable containerable, Collection<SelectorOptions<GetOperationOptions>> options,
            Task task, OperationResult result) {
        if (options == null) {
            return;
        }
        for (SelectorOptions<GetOperationOptions> option : options) {
            if (GetOperationOptions.isResolve(option.getOptions())) {
                ObjectSelector selector = option.getSelector();
                if (selector != null) {
                    ItemPath path = selector.getPath();
                    ItemPath.checkNoSpecialSymbolsExceptParent(path);
                    executeResolveOption(containerable, path, option, task, result);
                }
            }
        }
    }

    // TODO clean this mess
    private <O extends ObjectType> void executeResolveOption(Containerable containerable, ItemPath path,
            SelectorOptions<GetOperationOptions> option, Task task, OperationResult result) {
        if (path == null || path.isEmpty()) {
            return;
        }
        Object first = path.first();
        ItemPath rest = path.rest();
        PrismContainerValue<?> containerValue = containerable.asPrismContainerValue();
        if (ItemPath.isName(first)) {
            QName firstName = ItemPath.toName(first);
            PrismReference reference = containerValue.findReferenceByCompositeObjectElementName(firstName);
            if (reference == null) {
                reference = containerValue.findReference(firstName);    // alternatively look up by reference name (e.g. linkRef)
            }
            if (reference != null) {
                for (PrismReferenceValue refVal : reference.getValues()) {
                    //noinspection unchecked
                    PrismObject<O> refObject = refVal.getObject();
                    if (refObject == null) {
                        refObject = resolveReferenceUsingOption(refVal, option, containerable, task, result);
                    }
                    if (!rest.isEmpty() && refObject != null) {
                        executeResolveOption(refObject.asObjectable(), rest, option, task, result);
                    }
                }
                return;
            }
        }
        if (rest.isEmpty()) {
            return;
        }
        if (ItemPath.isParent(first)) {
            PrismContainerValue<?> parent = containerValue.getParentContainerValue();
            if (parent != null) {
                executeResolveOption(parent.asContainerable(), rest, option, task, result);
            }
        } else {
            QName nextName = ItemPath.toName(first);
            PrismContainer<?> nextContainer = containerValue.findContainer(nextName);
            if (nextContainer != null) {
                for (PrismContainerValue<?> pcv : nextContainer.getValues()) {
                    executeResolveOption(pcv.asContainerable(), rest, option, task, result);
                }
            }
        }
    }

    private <O extends ObjectType> PrismObject<O> resolveReferenceUsingOption(@NotNull PrismReferenceValue refVal,
            SelectorOptions<GetOperationOptions> option, Containerable containerable, Task task, OperationResult parentResult) {
        OperationResult result = parentResult.createMinorSubresult(RESOLVE_REFERENCE);
        try {
            PrismObject<O> refObject;
            refObject = objectResolver.resolve(refVal, containerable.toString(), option.getOptions(), task, result);
            if (refObject != null) {
                refObject = refObject.cloneIfImmutable();
                schemaTransformer.applySchemasAndSecurity(refObject, option.getOptions(),
                        SelectorOptions.createCollection(option.getOptions()), null, task, result);
                refVal.setObject(refObject);
            }
            return refObject;
        } catch (CommonException e) {
            result.recordWarning("Couldn't resolve reference to " + ObjectTypeUtil.toShortString(refVal) + ": " + e.getMessage(), e);
            return null;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public Collection<ObjectDeltaOperation<? extends ObjectType>> executeChanges(
            Collection<ObjectDelta<? extends ObjectType>> deltas, ModelExecuteOptions options, Task task, OperationResult parentResult)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
        return executeChanges(deltas, options, task, null, parentResult);
    }

    @Override
    public Collection<ObjectDeltaOperation<? extends ObjectType>> executeChanges(
            Collection<ObjectDelta<? extends ObjectType>> deltas, ModelExecuteOptions options,
            Task task, Collection<ProgressListener> statusListeners, OperationResult parentResult)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {

        enterModelMethod();

        OperationResult result = parentResult.createSubresult(EXECUTE_CHANGES);
        result.addArbitraryObjectAsParam(OperationResult.PARAM_OPTIONS, options);

        try {

            // Search filters treatment: if reevaluation is requested, we have to deal with three cases:
            // 1) for ADD operation: filters contained in object-to-be-added -> these are treated here
            // 2) for MODIFY operation: filters contained in existing object (not touched by deltas) -> these are treated after the modify operation
            // 3) for MODIFY operation: filters contained in deltas -> these have to be treated here, because if OID is missing from such a delta, the change would be rejected by the repository
            if (ModelExecuteOptions.isReevaluateSearchFilters(options)) {
                for (ObjectDelta<? extends ObjectType> delta : deltas) {
                    ModelImplUtils.resolveReferences(delta, cacheRepositoryService, false, true, EvaluationTimeType.IMPORT, true, prismContext, result);
                }
            } else if (ModelExecuteOptions.isIsImport(options)) {
                // if plain import is requested, we simply evaluate filters in ADD operation (and we do not force reevaluation if OID is already set)
                for (ObjectDelta<? extends ObjectType> delta : deltas) {
                    if (delta.isAdd()) {
                        ModelImplUtils.resolveReferences(delta.getObjectToAdd(), cacheRepositoryService, false, false, EvaluationTimeType.IMPORT, true, prismContext, result);
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
                return executeChangesRaw(deltas, options, task, result);
            } else {
                return executeChangesNonRaw(deltas, options, task, statusListeners, result);
            }

            // Note: caches are invalidated automatically via RepositoryCache.invalidateCacheEntries method

        } catch (Throwable t) {
            ModelImplUtils.recordFatalError(result, t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
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
        try {
            LensContext<? extends ObjectType> context = contextFactory.createContext(deltas, options, task, result);

            authorizePartialExecution(context, options, task, result);

            if (ModelExecuteOptions.isReevaluateSearchFilters(options)) {
                String m = "ReevaluateSearchFilters option is not fully supported for non-raw operations yet. Filters already present in the object will not be touched.";
                LOGGER.warn("{} Context = {}", m, context.debugDump());
                result.createSubresult(CLASS_NAME_WITH_DOT + "reevaluateSearchFilters").recordWarning(m);
            }

            context.setProgressListeners(statusListeners);
            // Note: Request authorization happens inside clockwork

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

        } catch (ObjectAlreadyExistsException | ObjectNotFoundException | SchemaException
                | ExpressionEvaluationException | CommunicationException | ConfigurationException
                | PolicyViolationException | SecurityViolationException | RuntimeException e) {
            ModelImplUtils.recordFatalError(result, e);
            throw e;
        }
    }

    private Collection<ObjectDeltaOperation<? extends ObjectType>> executeChangesRaw(Collection<ObjectDelta<? extends ObjectType>> deltas,
            ModelExecuteOptions options, Task task, OperationResult result)
            throws ExpressionEvaluationException, PolicyViolationException, SecurityViolationException, SchemaException,
            ObjectNotFoundException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException {

        String requestIdentifier = ModelImplUtils.generateRequestIdentifier();
        PrismReferenceValue targetRef = ModelImplUtils.determineAuditTarget(deltas, prismContext);
        AuditEventRecord auditRecordRequest = createAuditEventRecordRaw(AuditEventStage.REQUEST, requestIdentifier,
                targetRef, ObjectDeltaOperation.cloneDeltaCollection(deltas));
        // we don't know auxiliary information (resource, objectName) at this moment -- so we do nothing

        ExpressionType eventRecordingExpression = null;

        PrismObject<SystemConfigurationType> config = systemObjectCache.getSystemConfiguration(result);
        if (config != null && config.asObjectable().getAudit() != null
                && config.asObjectable().getAudit().getEventRecording() != null) {
            SystemConfigurationAuditEventRecordingType eventRecording = config.asObjectable().getAudit().getEventRecording();
            eventRecordingExpression = eventRecording.getExpression();
        }

        if (eventRecordingExpression != null) {
            // MID-6839
            auditRecordRequest = auditHelper.evaluateRecordingExpression(eventRecordingExpression,
                    auditRecordRequest, null, null, task, result);
        }
        if (auditRecordRequest != null) {
            auditHelper.audit(auditRecordRequest, null, task, result);
        }

        Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas = new ArrayList<>();
        try {
            for (ObjectDelta<? extends ObjectType> delta : deltas) {
                executeChangeRaw(executedDeltas, delta, options, task, result);
            }
            return executedDeltas;
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            cleanupOperationResult(result);
            AuditEventRecord auditRecordExecution = createAuditEventRecordRaw(AuditEventStage.EXECUTION, requestIdentifier, targetRef, executedDeltas);
            auditRecordExecution.setTimestamp(System.currentTimeMillis());
            auditRecordExecution.setOutcome(result.getStatus());
            if (eventRecordingExpression != null) {
                // MID-6839
                auditRecordExecution = auditHelper.evaluateRecordingExpression(eventRecordingExpression,
                        auditRecordExecution, null, null, task, result);
            }
            if (auditRecordExecution != null) {
                auditHelper.audit(auditRecordExecution, null, task, result);
            }
        }
    }

    private AuditEventRecord createAuditEventRecordRaw(AuditEventStage stage, String requestIdentifier,
            PrismReferenceValue targetRef, Collection<ObjectDeltaOperation<? extends ObjectType>> deltas) {
        AuditEventRecord auditRecord = new AuditEventRecord(AuditEventType.EXECUTE_CHANGES_RAW, stage);
        auditRecord.setRequestIdentifier(requestIdentifier);
        auditRecord.setTargetRef(targetRef);
        auditRecord.addDeltas(deltas);
        return auditRecord;
    }

    private void executeChangeRaw(Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas,
            ObjectDelta<? extends ObjectType> delta, ModelExecuteOptions options, Task task, OperationResult parentResult)
            throws CommunicationException, ObjectNotFoundException, ObjectAlreadyExistsException, PolicyViolationException,
            SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        OperationResult result = parentResult.createSubresult(EXECUTE_CHANGE);
        PrismObject<? extends ObjectType> objectToDetermineDetailsForAudit = null;
        try {

            applyDefinitionsIfNeeded(delta, task, result);

            objectToDetermineDetailsForAudit = executeChangeRawInternal(delta, options, task, result);
        } catch (Throwable t) {
            ModelImplUtils.recordFatalError(result, t);
            throw t;
        } finally { // to have a record with the failed delta as well
            result.computeStatusIfUnknown();
            ObjectDeltaOperation<? extends ObjectType> odoToAudit = prepareObjectDeltaOperation(delta, objectToDetermineDetailsForAudit, result);
            executedDeltas.add(odoToAudit);
        }
    }

    private void applyDefinitionsIfNeeded(ObjectDelta<? extends ObjectType> delta, Task task, OperationResult result1)
            throws ExpressionEvaluationException {
        // MID-2486
        if (delta.getObjectTypeClass() == ShadowType.class || delta.getObjectTypeClass() == ResourceType.class) {
            try {
                provisioning.applyDefinition(delta, task, result1);
            } catch (SchemaException | ObjectNotFoundException | CommunicationException | ConfigurationException | RuntimeException e) {
                // we can tolerate this - if there's a real problem with definition, repo call below will fail
                LoggingUtils.logExceptionAsWarning(LOGGER, "Couldn't apply definition on shadow/resource raw-mode delta {} -- continuing the operation.", e, delta);
                result1.muteLastSubresultError();
            }
        }
    }

    private ObjectDeltaOperation<? extends ObjectType> prepareObjectDeltaOperation(ObjectDelta<? extends ObjectType> delta,
            PrismObject<? extends ObjectType> objectToDetermineDetailsForAudit, OperationResult result) {
        ObjectDeltaOperation<? extends ObjectType> odoToAudit = new ObjectDeltaOperation<>(delta, result);
        if (objectToDetermineDetailsForAudit != null) {
            odoToAudit.setObjectName(objectToDetermineDetailsForAudit.getName());
            if (objectToDetermineDetailsForAudit.asObjectable() instanceof ShadowType) {
                ShadowType shadow = (ShadowType) objectToDetermineDetailsForAudit.asObjectable();
                odoToAudit.setResourceOid(ShadowUtil.getResourceOid(shadow));
                odoToAudit.setResourceName(ShadowUtil.getResourceName(shadow));
            }
        }
        return odoToAudit;
    }

    private PrismObject<? extends ObjectType> executeChangeRawInternal(
            ObjectDelta<? extends ObjectType> delta, ModelExecuteOptions options, Task task, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, ObjectAlreadyExistsException, SchemaException,
            SecurityViolationException, ConfigurationException, ExpressionEvaluationException, PolicyViolationException {
        final boolean preAuthorized = ModelExecuteOptions.isPreAuthorized(options);
        PrismObject<? extends ObjectType> objectToDetermineDetailsForAudit;
        if (delta.isAdd()) {
            objectToDetermineDetailsForAudit = executeAddDeltaRaw(delta, preAuthorized, options, task, result);
        } else if (delta.isDelete()) {
            objectToDetermineDetailsForAudit = executeDeleteDeltaRaw(delta, preAuthorized, task, result);
        } else if (delta.isModify()) {
            objectToDetermineDetailsForAudit = executeModifyDeltaRaw(delta, preAuthorized, options, task, result);
        } else {
            throw new IllegalArgumentException("Wrong delta type " + delta.getChangeType() + " in " + delta);
        }
        return objectToDetermineDetailsForAudit;
    }

    private PrismObject<? extends ObjectType> executeAddDeltaRaw(ObjectDelta<? extends ObjectType> delta,
            boolean preAuthorized, ModelExecuteOptions options, Task task, OperationResult result1)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException, ObjectAlreadyExistsException {
        RepoAddOptions repoOptions = new RepoAddOptions();
        if (ModelExecuteOptions.isNoCrypt(options)) {
            repoOptions.setAllowUnencryptedValues(true);
        }
        if (ModelExecuteOptions.isOverwrite(options)) {
            repoOptions.setOverwrite(true);
        }
        PrismObject<? extends ObjectType> objectToAdd = delta.getObjectToAdd();
        if (!preAuthorized) {
            securityEnforcer.authorize(ModelAuthorizationAction.RAW_OPERATION.getUrl(),
                    null, AuthorizationParameters.Builder.buildObjectAdd(objectToAdd), null, task, result1);
            securityEnforcer.authorize(ModelAuthorizationAction.ADD.getUrl(),
                    null, AuthorizationParameters.Builder.buildObjectAdd(objectToAdd), null, task, result1);
        }
        String oid;
        try {
            if (objectToAdd.canRepresent(TaskType.class)) {
                //noinspection unchecked
                oid = taskManager.addTask((PrismObject<TaskType>) objectToAdd, result1);
            } else {
                oid = cacheRepositoryService.addObject(objectToAdd, repoOptions, result1);
            }
            task.recordObjectActionExecuted(objectToAdd, null, oid, ChangeType.ADD, task.getChannel(), null);
        } catch (Throwable t) {
            task.recordObjectActionExecuted(objectToAdd, null, null, ChangeType.ADD, task.getChannel(), t);
            throw t;
        }
        delta.setOid(oid);
        return objectToAdd;
    }

    private PrismObject<? extends ObjectType> executeDeleteDeltaRaw(
            ObjectDelta<? extends ObjectType> delta, boolean preAuthorized, Task task, OperationResult result1)
            throws PolicyViolationException, CommunicationException, ObjectNotFoundException, SchemaException,
            SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        QNameUtil.setTemporarilyTolerateUndeclaredPrefixes(true);  // MID-2218
        PrismObject objectToDetermineDetailsForAudit = null;
        try {
            PrismObject<? extends ObjectType> existingObject;
            try {
                existingObject = cacheRepositoryService.getObject(delta.getObjectTypeClass(), delta.getOid(), createReadOnlyCollection(), result1);
                objectToDetermineDetailsForAudit = existingObject;
            } catch (Throwable t) {
                if (!securityEnforcer.isAuthorized(AuthorizationConstants.AUTZ_ALL_URL, null, AuthorizationParameters.EMPTY, null, task, result1)) {
                    throw t;
                } else {
                    existingObject = prismContext.createObject(delta.getObjectTypeClass());
                    existingObject.setOid(delta.getOid());
                    existingObject.asObjectable().setName(PolyStringType.fromOrig("Unreadable object"));
                    // in case of administrator's request we continue - in order to allow deleting malformed (unreadable) objects
                    // creating "shadow" existing object for auditing needs.
                }
            }
            checkIndestructible(existingObject, task, result1);
            if (!preAuthorized) {
                securityEnforcer.authorize(ModelAuthorizationAction.RAW_OPERATION.getUrl(),
                        null, AuthorizationParameters.Builder.buildObjectDelete(existingObject), null, task, result1);
                securityEnforcer.authorize(ModelAuthorizationAction.DELETE.getUrl(),
                        null, AuthorizationParameters.Builder.buildObjectDelete(existingObject), null, task, result1);
            }
            try {
                if (ObjectTypes.isClassManagedByProvisioning(delta.getObjectTypeClass())) {
                    ModelImplUtils.clearRequestee(task);
                    provisioning.deleteObject(delta.getObjectTypeClass(), delta.getOid(),
                            ProvisioningOperationOptions.createRaw(), null, task, result1);
                } else if (TaskType.class.isAssignableFrom(delta.getObjectTypeClass())) {
                    // Maybe we should check if the task is not running. However, this is raw processing.
                    // (But, actually, this is better than simply deleting the task from repository.)
                    taskManager.deleteTask(delta.getOid(), result1);
                } else {
                    cacheRepositoryService.deleteObject(delta.getObjectTypeClass(), delta.getOid(), result1);
                }
                task.recordObjectActionExecuted(objectToDetermineDetailsForAudit,
                        delta.getObjectTypeClass(), delta.getOid(), ChangeType.DELETE, task.getChannel(), null);
            } catch (Throwable t) {
                task.recordObjectActionExecuted(objectToDetermineDetailsForAudit,
                        delta.getObjectTypeClass(), delta.getOid(), ChangeType.DELETE, task.getChannel(), t);
                throw t;
            }
        } finally {
            QNameUtil.setTemporarilyTolerateUndeclaredPrefixes(false);
        }
        return objectToDetermineDetailsForAudit;
    }

    private PrismObject<? extends ObjectType> executeModifyDeltaRaw(ObjectDelta<? extends ObjectType> delta,
            boolean preAuthorized, ModelExecuteOptions options, Task task, OperationResult result1)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ConfigurationException,
            CommunicationException, SecurityViolationException, ExpressionEvaluationException {
        QNameUtil.setTemporarilyTolerateUndeclaredPrefixes(true); // MID-2218
        PrismObject<? extends ObjectType> objectToDetermineDetailsForAudit;
        try {
            PrismObject existingObject = cacheRepositoryService.getObject(
                    delta.getObjectTypeClass(), delta.getOid(), createReadOnlyCollection(), result1);
            objectToDetermineDetailsForAudit = existingObject;
            if (!preAuthorized) {
                AuthorizationParameters autzParams = AuthorizationParameters.Builder.buildObjectDelta(existingObject, delta);
                securityEnforcer.authorize(ModelAuthorizationAction.RAW_OPERATION.getUrl(), null, autzParams, null, task, result1);
                securityEnforcer.authorize(ModelAuthorizationAction.MODIFY.getUrl(), null, autzParams, null, task, result1);
            }
            try {
                if (TaskType.class.isAssignableFrom(delta.getObjectTypeClass())) {
                    taskManager.modifyTask(delta.getOid(), delta.getModifications(), result1);
                } else {
                    cacheRepositoryService.modifyObject(delta.getObjectTypeClass(), delta.getOid(),
                            delta.getModifications(), result1);
                }
                task.recordObjectActionExecuted(existingObject, ChangeType.MODIFY, null);
            } catch (Throwable t) {
                task.recordObjectActionExecuted(existingObject, ChangeType.MODIFY, t);
                throw t;
            }
        } finally {
            QNameUtil.setTemporarilyTolerateUndeclaredPrefixes(false);
        }
        if (ModelExecuteOptions.isReevaluateSearchFilters(options)) { // treat filters that already exist in the object (case #2 above)
            reevaluateSearchFilters(delta.getObjectTypeClass(), delta.getOid(), task, result1);
        }
        return objectToDetermineDetailsForAudit;
    }

    private <O extends ObjectType> void checkIndestructible(PrismObject<O> existingObject, Task task, OperationResult result)
            throws IndestructibilityViolationException {
        if (existingObject != null && Boolean.TRUE.equals(existingObject.asObjectable().isIndestructible())) {
            IndestructibilityViolationException e =
                    new IndestructibilityViolationException("Attempt to delete indestructible object " + existingObject);
            ModelImplUtils.recordFatalError(result, e);
            throw e;
        }
    }

    private void authorizePartialExecution(LensContext<? extends ObjectType> context,
            ModelExecuteOptions options, Task task, OperationResult result)
            throws SecurityViolationException, SchemaException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException {
        PartialProcessingOptionsType partialProcessing = ModelExecuteOptions.getPartialProcessing(options);
        if (partialProcessing != null) {
            PrismObject<? extends ObjectType> object = context.getFocusContext().getObjectAny();
            securityEnforcer.authorize(ModelAuthorizationAction.PARTIAL_EXECUTION.getUrl(),
                    null, AuthorizationParameters.Builder.buildObject(object), null, task, result);
        }
    }

    protected void cleanupOperationResult(OperationResult result) {
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

        result.cleanupResult();
    }

    private <T extends ObjectType> void reevaluateSearchFilters(
            Class<T> objectTypeClass, String oid, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        OperationResult result = parentResult.createSubresult(CLASS_NAME_WITH_DOT + "reevaluateSearchFilters");
        try {
            PrismObject<T> storedObject = cacheRepositoryService.getObject(objectTypeClass, oid, createReadOnlyCollection(), result);
            PrismObject<T> updatedObject = storedObject.clone();
            ModelImplUtils.resolveReferences(updatedObject, cacheRepositoryService, false, true, EvaluationTimeType.IMPORT, true, prismContext, result);
            ObjectDelta<T> delta = storedObject.diff(updatedObject);
            LOGGER.trace("reevaluateSearchFilters found delta: {}", delta.debugDumpLazily());
            if (!delta.isEmpty()) {
                try {
                    cacheRepositoryService.modifyObject(objectTypeClass, oid, delta.getModifications(), result);
                    task.recordObjectActionExecuted(updatedObject, ChangeType.MODIFY, null);
                } catch (Throwable t) {
                    task.recordObjectActionExecuted(updatedObject, ChangeType.MODIFY, t);
                    throw t;
                }
            }
            result.recordSuccess();
        } catch (SchemaException | ObjectNotFoundException | ObjectAlreadyExistsException | RuntimeException e) {
            result.recordFatalError("Couldn't reevaluate search filters: " + e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public <F extends ObjectType> void recompute(
            Class<F> type, String oid, ModelExecuteOptions options, Task task, OperationResult parentResult)
            throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException,
            ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException {

        OperationResult result = parentResult.createMinorSubresult(RECOMPUTE);
        result.addParam(OperationResult.PARAM_OID, oid);
        result.addParam(OperationResult.PARAM_TYPE, type);

        enterModelMethod();

        try {

            ModelImplUtils.clearRequestee(task);
            // Not using read-only for now
            //noinspection unchecked
            PrismObject<F> focus = objectResolver.getObject(type, oid, null, task, result).asPrismContainer();

            LOGGER.debug("Recomputing {}", focus);

            LensContext<F> lensContext = contextFactory.createRecomputeContext(focus, options, task, result);
            LOGGER.trace("Recomputing {}, context:\n{}", focus, lensContext.debugDumpLazily());
            clockwork.run(lensContext, task, result);

            result.computeStatus();

            LOGGER.trace("Recomputing of {}: {}", focus, result.getStatus());

            result.cleanupResult();

        } catch (ExpressionEvaluationException | SchemaException | PolicyViolationException | ObjectNotFoundException |
                ObjectAlreadyExistsException | CommunicationException | ConfigurationException | SecurityViolationException |
                RuntimeException | Error e) {
            ModelImplUtils.recordFatalError(result, e);
            throw e;
        } finally {
            exitModelMethod();
        }
    }

    private void applyDefinitions(
            Collection<ObjectDelta<? extends ObjectType>> deltas, ModelExecuteOptions options, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        for (ObjectDelta<? extends ObjectType> delta : deltas) {
            Class<? extends ObjectType> type = delta.getObjectTypeClass();
            if (delta.hasCompleteDefinition()) {
                continue;
            }
            if (type == ResourceType.class || ShadowType.class.isAssignableFrom(type)) {
                try {
                    provisioning.applyDefinition(delta, task, result);
                } catch (SchemaException | ObjectNotFoundException | CommunicationException | ConfigurationException | ExpressionEvaluationException e) {
                    if (ModelExecuteOptions.isRaw(options)) {
                        ModelImplUtils.recordPartialError(result, e);
                        // just go on, this is raw, we need to continue even without complete schema
                    } else {
                        ModelImplUtils.recordFatalError(result, e);
                        throw e;
                    }
                }
            } else {
                PrismObjectDefinition objDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(delta.getObjectTypeClass());
                if (objDef == null) {
                    throw new SchemaException("No definition for delta object type class: " + delta.getObjectTypeClass());
                }
                boolean tolerateNoDefinition = ModelExecuteOptions.isRaw(options);
                delta.applyDefinitionIfPresent(objDef, tolerateNoDefinition);
            }
        }
    }

    @Override
    public <T extends ObjectType> SearchResultList<PrismObject<T>> searchObjects(Class<T> type, ObjectQuery origQuery,
            Collection<SelectorOptions<GetOperationOptions>> rawOptions, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {

        Validate.notNull(type, "Object type must not be null.");
        Validate.notNull(parentResult, "Operation result must not be null.");
        // using clone of object query here because authorization mechanism adds additional (secuirty) filters to the (original) query
        // at the end objectQuery contains additional filters as many times as the authZ mechanism is called.
        // for more info see MID-6115
        ObjectQuery query = origQuery != null ? origQuery.clone() : null;
        if (query != null) {
            ModelImplUtils.validatePaging(query.getPaging());
        }

        OP_LOGGER.trace("MODEL OP enter searchObjects({},{},{})", type.getSimpleName(), query, rawOptions);

        OperationResult result = parentResult.createSubresult(SEARCH_OBJECTS);
        result.addParam(OperationResult.PARAM_TYPE, type);
        result.addParam(OperationResult.PARAM_QUERY, query);

        Collection<SelectorOptions<GetOperationOptions>> options = preProcessOptionsSecurity(rawOptions, task, result);
        GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);

        ObjectTypes.ObjectManager searchProvider = ObjectTypes.getObjectManagerForClass(type);
        if (searchProvider == null || searchProvider == ObjectTypes.ObjectManager.MODEL || GetOperationOptions.isRaw(rootOptions)) {
            searchProvider = ObjectTypes.ObjectManager.REPOSITORY;
        }
        result.addArbitraryObjectAsParam("searchProvider", searchProvider);

        ObjectQuery processedQuery = preProcessQuerySecurity(type, query, rootOptions, task, result);
        if (isFilterNone(processedQuery, result)) {
            return new SearchResultList<>(new ArrayList<>());
        }

        boolean exceptionInSearch = false;

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
                        list = cacheRepositoryService.searchObjects(type, processedQuery, options, result);
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
            } catch (CommunicationException | ConfigurationException | SchemaException
                    | SecurityViolationException | RuntimeException | ObjectNotFoundException e) {
                exceptionInSearch = true;
                throw e;
            } finally {
                QNameUtil.setTemporarilyTolerateUndeclaredPrefixes(false);
            }

            LOGGER.trace("Basic search returned {} results (before hooks, security, etc.)", list.size());

            for (PrismObject<T> object : list) {
                if (hookRegistry != null) {
                    for (ReadHook hook : hookRegistry.getAllReadHooks()) {
                        hook.invoke(object, options, task, result);
                    }
                }
                executeResolveOptions(object.asObjectable(), options, task, result);
            }

            // Post-processing objects that weren't handled by their correct provider (e.g. searching for ObjectType,
            // and retrieving tasks, resources, shadows). Currently, only resources and shadows are handled in this way.
            //
            // TODO generalize this approach somehow (something like "postprocess" in task/provisioning interface)
            // TODO ... or consider abandoning this functionality altogether (it is more a hack than a serious design!)
            if (searchProvider == ObjectTypes.ObjectManager.REPOSITORY && !GetOperationOptions.isRaw(rootOptions)) {
                for (PrismObject<T> object : list) {
                    if (object.asObjectable() instanceof ResourceType || object.asObjectable() instanceof ShadowType) {
                        applyProvisioningDefinition(object, task, result);
                    }
                }
            }
            // TODO Clone objects and the list lazily. Now we do the cloning just to fix MID-6825.
            if (list.isImmutable()) {
                list = list.deepClone();
            }
            // better to use cache here (MID-4059)
            schemaTransformer.applySchemasAndSecurityToObjects(list, rootOptions, options, null, task, result);

        } catch (Throwable e) {
            if (exceptionInSearch) {
                processSearchException(e, searchProvider, result);
            } else {
                result.recordFatalError(e);
            }
            throw e;
        } finally {
            exitModelMethod();
            result.computeStatusIfUnknown();
            result.cleanupResult();
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

    private class ContainerOperationContext<T extends Containerable> {
        final boolean isCertCase;
        final boolean isCaseMgmtWorkItem;
        final boolean isOperationExecution;
        final ObjectTypes.ObjectManager manager;
        final ObjectQuery refinedQuery;
        private boolean isAssignment;

        // TODO: task and result here are ugly and probably wrong
        ContainerOperationContext(Class<T> type, ObjectQuery query, Task task, OperationResult result)
                throws SchemaException, SecurityViolationException, ObjectNotFoundException,
                ExpressionEvaluationException, CommunicationException, ConfigurationException {

            isCertCase = AccessCertificationCaseType.class.equals(type);
            isCaseMgmtWorkItem = CaseWorkItemType.class.equals(type);
            isOperationExecution = OperationExecutionType.class.equals(type);
            isAssignment = AssignmentType.class.equals(type);

            if (!isCertCase && !isCaseMgmtWorkItem && !isOperationExecution && !isAssignment) {
                throw new UnsupportedOperationException("searchContainers/countContainers methods are currently supported only for AccessCertificationCaseType, CaseWorkItemType and AssignmentType classes");
            }

            manager = ObjectTypes.ObjectManager.REPOSITORY;
            if (isCertCase) {
                refinedQuery = preProcessSubobjectQuerySecurity(
                        AccessCertificationCaseType.class, AccessCertificationCampaignType.class, query, task, result);
            } else {
                refinedQuery = query;
            }
        }
    }

    @Override
    public <T extends Containerable> SearchResultList<T> searchContainers(
            Class<T> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> rawOptions,
            Task task, OperationResult parentResult) throws SchemaException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException {

        Validate.notNull(type, "Container value type must not be null.");
        Validate.notNull(parentResult, "Result type must not be null.");
        if (query != null) {
            ModelImplUtils.validatePaging(query.getPaging());
        }

        final OperationResult result = parentResult.createSubresult(SEARCH_CONTAINERS);
        result.addParam(OperationResult.PARAM_TYPE, type);
        result.addParam(OperationResult.PARAM_QUERY, query);

        final ContainerOperationContext<T> ctx = new ContainerOperationContext<>(type, query, task, result);

        Collection<SelectorOptions<GetOperationOptions>> options = preProcessOptionsSecurity(rawOptions, task, result);
        final GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);

        query = ctx.refinedQuery;

        if (isFilterNone(query, result)) {
            return new SearchResultList<>(new ArrayList<>());
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
                result.computeStatus();
                result.cleanupResult();
            } catch (SchemaException | RuntimeException e) {
                processSearchException(e, ctx.manager, result);
                throw e;
            } finally {
                QNameUtil.setTemporarilyTolerateUndeclaredPrefixes(false);
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace(result.dump(false));
                }
            }

            if (list == null) {
                list = new SearchResultList<>(new ArrayList<>());
            }

            for (T object : list) {
                // TODO implement read hook, if necessary
                executeResolveOptions(object, options, task, result);
            }
        } finally {
            exitModelMethod();
        }

        if (ctx.isCertCase) {
            list = schemaTransformer.applySchemasAndSecurityToContainers(list, AccessCertificationCampaignType.class,
                    AccessCertificationCampaignType.F_CASE, rootOptions, options, null, task, result);
        } else if (ctx.isCaseMgmtWorkItem || ctx.isOperationExecution || ctx.isAssignment) {
            // TODO implement security post processing for CaseWorkItems
        } else {
            throw new IllegalStateException();
        }

        return list;
    }

    @Override
    public <T extends Containerable> Integer countContainers(Class<T> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> rawOptions, Task task, OperationResult parentResult)
            throws SchemaException, SecurityViolationException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException {

        Validate.notNull(type, "Container value type must not be null.");
        Validate.notNull(parentResult, "Result type must not be null.");

        final OperationResult result = parentResult.createSubresult(COUNT_CONTAINERS);
        result.addParam(OperationResult.PARAM_TYPE, type);
        result.addParam(OperationResult.PARAM_QUERY, query);

        final ContainerOperationContext<T> ctx = new ContainerOperationContext<>(type, query, task, result);

        final Collection<SelectorOptions<GetOperationOptions>> options = preProcessOptionsSecurity(rawOptions, task, result);

        query = ctx.refinedQuery;

        if (isFilterNone(query, result)) {
            return 0;
        }

        enterModelMethod(); // outside try-catch because if this ends with an exception, cache is not entered yet
        int count;
        try {
            logQuery(query);

            try {
                //noinspection SwitchStatementWithTooFewBranches
                switch (ctx.manager) {
                    case REPOSITORY:
                        count = cacheRepositoryService.countContainers(type, query, options, result);
                        break;
                    default:
                        throw new IllegalStateException();
                }
                result.computeStatus();
                result.cleanupResult();
            } catch (RuntimeException e) {
                processSearchException(e, ctx.manager, result);
                throw e;
            } finally {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace(result.dump(false));
                }
            }
        } finally {
            exitModelMethod();
        }

        return count;
    }

    // See MID-6323 in Jira
//    // TODO - fix this temporary implementation (perhaps by storing 'groups' in user context on logon)
//    // TODO: currently we check only the direct assignments, we need to implement more complex mechanism
//    public List<PrismReferenceValue> getGroupsForUser(UserType user) {
//        List<PrismReferenceValue> retval = new ArrayList<>();
//        for (AssignmentType assignmentType : user.getAssignmentNew()) {
//            ObjectReferenceType ref = assignmentType.getTargetRef();
//            if (ref != null) {
//                retval.add(ref.clone().asReferenceValue());
//            }
//        }
//        return retval;
//    }

    protected boolean isFilterNone(ObjectQuery query, OperationResult result) {
        if (query != null && query.getFilter() != null && query.getFilter() instanceof NoneFilter) {
            LOGGER.trace("Security denied the search");
            result.recordStatus(OperationResultStatus.NOT_APPLICABLE, "Denied");
            return true;
        }
        return false;
    }

    protected void logQuery(ObjectQuery query) {
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

        final OperationResult result = parentResult.createSubresult(SEARCH_OBJECTS);
        result.addParam(OperationResult.PARAM_QUERY, query);

        final Collection<SelectorOptions<GetOperationOptions>> options = preProcessOptionsSecurity(rawOptions, task, result);
        final GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);
        ObjectTypes.ObjectManager searchProvider = ObjectTypes.getObjectManagerForClass(type);
        if (searchProvider == null || searchProvider == ObjectTypes.ObjectManager.MODEL || GetOperationOptions.isRaw(rootOptions)) {
            searchProvider = ObjectTypes.ObjectManager.REPOSITORY;
        }
        result.addArbitraryObjectAsParam("searchProvider", searchProvider);

        // see MID-6115
        ObjectQuery processedQuery = preProcessQuerySecurity(type, query, rootOptions, task, result);
        if (isFilterNone(processedQuery, result)) {
            LOGGER.trace("Skipping search because filter is NONE");
            return null;
        }

        ResultHandler<T> internalHandler = (object, parentResult1) -> {
            try {
                object = object.cloneIfImmutable();
                if (hookRegistry != null) {
                    for (ReadHook hook : hookRegistry.getAllReadHooks()) {
                        hook.invoke(object, options, task, result); // TODO result or parentResult??? [med]
                    }
                }
                executeResolveOptions(object.asObjectable(), options, task, result);
                schemaTransformer.applySchemasAndSecurity(object, rootOptions, options, null, task, parentResult1);
            } catch (SchemaException | ObjectNotFoundException | SecurityViolationException | ExpressionEvaluationException
                    | CommunicationException | ConfigurationException ex) {
                parentResult1.recordFatalError(ex);
                throw new SystemException(ex.getMessage(), ex);
            }

            OP_LOGGER.debug("MODEL OP handle searchObjects({},{},{}): {}", type.getSimpleName(), query, rawOptions, object);
            if (OP_LOGGER.isTraceEnabled()) {
                OP_LOGGER.trace("MODEL OP handle searchObjects({},{},{}):\n{}", type.getSimpleName(), query, rawOptions, object.debugDump(1));
            }

            return handler.handle(object, parentResult1);
        };

        SearchResultMetadata metadata;
        try {
            enterModelMethodNoRepoCache(); // skip using cache to avoid potentially many objects there (MID-4615, MID-4959)
            logQuery(processedQuery);

            try {
                switch (searchProvider) {
                    case REPOSITORY:
                        metadata = cacheRepositoryService.searchObjectsIterative(type, processedQuery, internalHandler, options, true, result);
                        break;
                    case PROVISIONING:
                        metadata = provisioning.searchObjectsIterative(type, processedQuery, options, internalHandler, task, result);
                        break;
                    case TASK_MANAGER:
                        metadata = taskManager.searchObjectsIterative(type, processedQuery, options, internalHandler, result);
                        break;
                    default:
                        throw new AssertionError("Unexpected search provider: " + searchProvider);
                }
                result.computeStatusIfUnknown();
                result.cleanupResult();
            } catch (CommunicationException | ConfigurationException | ObjectNotFoundException | SchemaException
                    | SecurityViolationException | ExpressionEvaluationException | RuntimeException | Error e) {
                processSearchException(e, searchProvider, result);
                throw e;
            } finally {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace(result.dump(false));
                }
            }
        } finally {
            exitModelMethodNoRepoCache();
        }

        // TODO: log errors

        if (OP_LOGGER.isDebugEnabled()) {
            OP_LOGGER.debug("MODEL OP exit searchObjects({},{},{}): {}", type.getSimpleName(), query, rawOptions, metadata);
        }

        return metadata;
    }

    private void processSearchException(
            Throwable e, ObjectTypes.ObjectManager searchProvider, OperationResult result) {
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
        LoggingUtils.logUnexpectedException(LOGGER, message, e);
        result.recordFatalError(message, e);
        result.cleanupResult(e);
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

            Collection<SelectorOptions<GetOperationOptions>> options = preProcessOptionsSecurity(rawOptions, task, result);
            GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);

            ObjectQuery processedQuery = preProcessQuerySecurity(type, query, rootOptions, task, result);
            if (isFilterNone(processedQuery, result)) {
                LOGGER.trace("Skipping count because filter is NONE");
                return 0;
            }

            ObjectTypes.ObjectManager objectManager = ObjectTypes.getObjectManagerForClass(type);
            if (GetOperationOptions.isRaw(rootOptions) || objectManager == null || objectManager == ObjectTypes.ObjectManager.MODEL) {
                objectManager = ObjectTypes.ObjectManager.REPOSITORY;
            }
            switch (objectManager) {
                case PROVISIONING:
                    count = provisioning.countObjects(type, processedQuery, options, task, parentResult);
                    break;
                case REPOSITORY:
                    count = cacheRepositoryService.countObjects(type, processedQuery, options, parentResult);
                    break;
                case TASK_MANAGER:
                    count = taskManager.countObjects(type, processedQuery, parentResult);
                    break;
                default:
                    throw new AssertionError("Unexpected objectManager: " + objectManager);
            }
        } catch (ConfigurationException | SecurityViolationException | SchemaException | ObjectNotFoundException
                | CommunicationException | ExpressionEvaluationException | RuntimeException | Error e) {
            ModelImplUtils.recordFatalError(result, e);
            throw e;
        } finally {
            exitModelMethod();
        }

        result.computeStatus();
        result.cleanupResult();
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
            Collection<SelectorOptions<GetOperationOptions>> options = preProcessOptionsSecurity(rawOptions, task, result);
            focus = cacheRepositoryService.searchShadowOwner(shadowOid, options, result);
            result.recordSuccess();
        } catch (RuntimeException | Error ex) {
            LoggingUtils.logException(LOGGER, "Couldn't list account shadow owner from repository"
                    + " for account with oid {}", ex, shadowOid);
            result.recordFatalError("Couldn't list account shadow owner for account with oid '"
                    + shadowOid + "'.", ex);
            throw ex;
        } finally {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(result.dump(false));
            }
            exitModelMethod();
            result.cleanupResult();
        }

        if (focus != null) {
            try {
                focus = focus.cloneIfImmutable();
                schemaTransformer.applySchemasAndSecurity(focus, null, null, null, task, result);
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
    }

    @Override
    public OperationResult testResource(String resourceOid, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ConfigurationException {
        Validate.notEmpty(resourceOid, "Resource oid must not be null or empty.");
        LOGGER.trace("Testing resource OID: {}", resourceOid);

        enterModelMethod();
        try {
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
    public @Nullable ResourceSchema fetchSchema(
            @NotNull PrismObject<ResourceType> resource, @NotNull OperationResult result) {
        Validate.notNull(resource, "Resource must not be null.");
        LOGGER.trace("Fetch schema by connector configuration from resource: {}", resource);

        enterModelMethodNoRepoCache();
        try {
            @Nullable ResourceSchema schema = provisioning.fetchSchema(resource, result);
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
    public @Nullable CapabilityCollectionType getNativeCapabilities(@NotNull String connOid, OperationResult result)
            throws SchemaException, CommunicationException, ConfigurationException, ObjectNotFoundException {
        Validate.notNull(connOid, "ConnOid must not be null.");
        LOGGER.trace("Getting native capabilities by connector oid: {}", connOid);

        enterModelMethodNoRepoCache();
        try {
            CapabilityCollectionType capability = provisioning.getNativeCapabilities(connOid, result);
            LOGGER.debug(
                    "Finished getting native capabilities by connector oid: {}, result: {} ",
                    connOid,
                    result.getStatus());
            LOGGER.trace("Getting native capabilities by connector oid:\n{}", lazy(() -> result.dump(false)));
            return capability;
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

        // Fetch resource definition from the repo/provisioning
        ResourceType resource;
        try {
            resource = getObject(ResourceType.class, resourceOid, createReadOnlyCollection(), task, result).asObjectable();

            // Here was a check on synchronization configuration, providing a warning if there is no configuration set up.
            // But with changes in 4.6 it is not so easy to definitely tell that there's no synchronization set up,
            // so - maybe temporarily - we removed these checks.

            result.recordStatus(OperationResultStatus.IN_PROGRESS, "Task running in background");

            importFromResourceLauncher.launch(resource, objectClass, task, result);

            // The launch should switch task to asynchronous. It is in/out, so no
            // other action is needed

            if (!task.isAsynchronous()) {
                result.recordSuccess();
            }

            result.cleanupResult();

        } catch (ObjectNotFoundException | CommunicationException | ConfigurationException
                | SecurityViolationException | ExpressionEvaluationException | RuntimeException | Error ex) {
            ModelImplUtils.recordFatalError(result, ex);
            throw ex;
        } finally {
            exitModelMethod();
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
            boolean wasOk = importFromResourceLauncher.importSingleShadow(shadowOid, task, result);

            if (wasOk) {
                result.recordSuccess();
            } else {
                // the error should be in the result already, compute should reveal that to the top-level
                result.computeStatus();
            }

            result.cleanupResult();

        } catch (ObjectNotFoundException | CommunicationException | ConfigurationException
                | SecurityViolationException | ExpressionEvaluationException | RuntimeException | Error ex) {
            ModelImplUtils.recordFatalError(result, ex);
            throw ex;
        } finally {
            exitModelMethod();
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
        }
        result.computeStatus();
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
            result.recordFatalError(e.getMessage(), e); // shouldn't really occur
        } finally {
            exitModelMethod();
        }
        result.cleanupResult();
    }

    @Override
    public void importObject(PrismObject object, ImportOptionsType options, Task task, OperationResult parentResult) {
        enterModelMethod();
        OperationResult result = parentResult.createSubresult(IMPORT_OBJECTS_FROM_STREAM);
        result.addArbitraryObjectAsParam(OperationResult.PARAM_OPTIONS, options);
        try {
            objectImporter.importObject(object, options, task, result);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Import result:\n{}", result.debugDump());
            }
            // No need to compute status. The validator inside will do it.
            // result.computeStatus("Couldn't import object from input stream.");
        } catch (RuntimeException e) {
            result.recordFatalError(e.getMessage(), e); // shouldn't really occur
        } finally {
            exitModelMethod();
        }
        result.cleanupResult();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.evolveum.midpoint.model.api.ModelService#discoverConnectors(com.evolveum
     * .midpoint.xml.ns._public.common.common_1.ConnectorHostType,
     * com.evolveum.midpoint.common.result.OperationResult)
     */
    @Override
    public Set<ConnectorType> discoverConnectors(ConnectorHostType hostType, Task task, OperationResult parentResult)
            throws CommunicationException, SecurityViolationException, SchemaException,
            ConfigurationException, ObjectNotFoundException, ExpressionEvaluationException {
        enterModelMethod();
        OperationResult result = parentResult.createSubresult(DISCOVER_CONNECTORS);
        Set<ConnectorType> discoverConnectors;
        try {
            discoverConnectors = provisioning.discoverConnectors(hostType, result);
        } catch (CommunicationException | RuntimeException | Error e) {
            result.recordFatalError(e.getMessage(), e);
            exitModelMethod();
            throw e;
        }
        List<ConnectorType> connectorList = new ArrayList<>(discoverConnectors);
        schemaTransformer.applySchemasAndSecurityToObjectTypes(connectorList, null, null, null, task, result);
        result.computeStatus("Connector discovery failed");
        exitModelMethod();
        result.cleanupResult();
        return new HashSet<>(connectorList);
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
        } catch (SchemaException e) {
            result.recordFatalError(e);
            throw new SystemException(e.getMessage(), e);
        }

        securityContextManager.setUserProfileService(focusProfileService);

        // Initialize provisioning
        provisioning.postInit(result);

        if (result.isUnknown()) {
            result.computeStatus();
        }

        exitModelMethod();
        result.cleanupResult();
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

        Collection<SelectorOptions<GetOperationOptions>> readOptions = preProcessOptionsSecurity(rawReadOptions, task, result);

        CompareResultType rv = new CompareResultType();

        try {
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
        } finally {
            result.computeStatus();
            result.cleanupResult();
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

    private Collection<SelectorOptions<GetOperationOptions>> preProcessOptionsSecurity(
            Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);
        if (GetOperationOptions.isAttachDiagData(rootOptions) &&
                !securityEnforcer.isAuthorized(AuthorizationConstants.AUTZ_ALL_URL, null, AuthorizationParameters.EMPTY, null, task, result)) {
            Collection<SelectorOptions<GetOperationOptions>> reducedOptions = CloneUtil.cloneCollectionMembers(options);
            SelectorOptions.findRootOptions(reducedOptions).setAttachDiagData(false);
            return reducedOptions;
        } else {
            return options;
        }
    }

    private <O extends ObjectType> ObjectQuery preProcessQuerySecurity(
            Class<O> objectType, ObjectQuery origQuery, GetOperationOptions rootOptions, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        ObjectFilter origFilter = null;
        if (origQuery != null) {
            origFilter = origQuery.getFilter();
        }
        AuthorizationPhaseType phase = null;
        if (GetOperationOptions.isExecutionPhase(rootOptions)) {
            phase = AuthorizationPhaseType.EXECUTION;
        }
        ObjectFilter secFilter = securityEnforcer.preProcessObjectFilter(ModelAuthorizationAction.AUTZ_ACTIONS_URLS_SEARCH, phase, objectType, null, origFilter, null, null, task, result);
        return updateObjectQuery(origQuery, secFilter);
    }

    // we expect that objectType is a direct parent of containerType
    private <C extends Containerable, O extends ObjectType> ObjectQuery preProcessSubobjectQuerySecurity(
            Class<C> containerType, Class<O> objectType, ObjectQuery origQuery, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        // Search containers is an operation on one object. Therefore even if it works with a search filter, it requires GET authorizations
        ObjectFilter secParentFilter = securityEnforcer.preProcessObjectFilter(ModelAuthorizationAction.AUTZ_ACTIONS_URLS_GET, null, objectType, null, null, null, null, task, result);
        if (secParentFilter == null || secParentFilter instanceof AllFilter) {
            return origQuery; // no need to update the query
        }
        ObjectFilter secChildFilter;
        if (secParentFilter instanceof NoneFilter) {
            secChildFilter = FilterCreationUtil.createNone(prismContext);
        } else {
            ObjectFilter origChildFilter = origQuery != null ? origQuery.getFilter() : null;
            ObjectFilter secChildFilterParentPart = prismContext.queryFactory().createExists(ItemName.fromQName(PrismConstants.T_PARENT), // fixme
                    containerType, prismContext, secParentFilter);
            if (origChildFilter == null) {
                secChildFilter = secChildFilterParentPart;
            } else {
                secChildFilter = prismContext.queryFactory().createAnd(origChildFilter, secChildFilterParentPart);
            }
        }
        return updateObjectQuery(origQuery, secChildFilter);
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
        boolean suspended;
        try {
            suspended = taskManager.suspendTasks(taskOids, waitForStop, taskOperationResult);
        } catch (Throwable t) {
            taskOperationResult.recordFatalError(t);
            throw t;
        } finally {
            taskOperationResult.close();
        }

        postprocessTaskCollectionOperation(resolvedTasks,
                AuditEventType.SUSPEND_TASK, operationTask, parentResult, taskOperationResult);
        return suspended;
    }

    @Override
    public boolean suspendTask(String taskOid, long waitForStop, Task operationTask, OperationResult parentResult)
            throws SecurityViolationException, ObjectNotFoundException, SchemaException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException {
        List<PrismObject<TaskType>> resolvedTasks = preprocessTaskOperation(taskOid,
                ModelAuthorizationAction.SUSPEND_TASK, AuditEventType.SUSPEND_TASK, operationTask, parentResult);

        OperationResult taskOperationResult = parentResult.createSubresult(CLASS_NAME_WITH_DOT + "suspendTasks"); // details filled in taskManager
        boolean suspended;
        try {
            suspended = taskManager.suspendTask(taskOid, waitForStop, taskOperationResult);
        } catch (Throwable t) {
            taskOperationResult.recordFatalError(t);
            throw t;
        } finally {
            taskOperationResult.close();
        }

        postprocessTaskCollectionOperation(resolvedTasks,
                AuditEventType.SUSPEND_TASK, operationTask, parentResult, taskOperationResult);
        return suspended;
    }

    @Override
    public boolean suspendTaskTree(String taskOid, long waitForStop, Task operationTask, OperationResult parentResult)
            throws SecurityViolationException, ObjectNotFoundException, SchemaException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException {
        List<PrismObject<TaskType>> resolvedTasks = preprocessTaskOperation(taskOid,
                ModelAuthorizationAction.SUSPEND_TASK, AuditEventType.SUSPEND_TASK, operationTask, parentResult);

        OperationResult taskOperationResult = parentResult.createSubresult(CLASS_NAME_WITH_DOT + "suspendTaskTree"); // details filled in taskManager
        boolean suspended;
        try {
            suspended = taskManager.suspendTaskTree(taskOid, waitForStop, taskOperationResult);
        } catch (Throwable t) {
            taskOperationResult.recordFatalError(t);
            throw t;
        } finally {
            taskOperationResult.close();
        }

        postprocessTaskCollectionOperation(resolvedTasks,
                AuditEventType.SUSPEND_TASK, operationTask, parentResult, taskOperationResult);
        return suspended;
    }

    @Override
    public void suspendAndDeleteTasks(Collection<String> taskOids,
            long waitForStop, boolean alsoSubtasks, Task operationTask, OperationResult parentResult)
            throws SecurityViolationException, ObjectNotFoundException, SchemaException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException {
        List<PrismObject<TaskType>> resolvedTasks = preprocessTaskCollectionOperation(taskOids,
                ModelAuthorizationAction.DELETE, AuditEventType.DELETE_OBJECT, operationTask, parentResult);
        List<String> indestructibleTaskOids = getIndestructibleTaskOids(resolvedTasks, operationTask, parentResult);
        taskOids.removeAll(indestructibleTaskOids);

        OperationResult taskOperationResult = parentResult.createSubresult(CLASS_NAME_WITH_DOT + "suspendAndDeleteTasks"); // details filled in taskManager
        try {
            if (!taskOids.isEmpty()) {
                taskManager.suspendAndDeleteTasks(taskOids, waitForStop, alsoSubtasks, taskOperationResult);
            }
        } catch (Throwable t) {
            taskOperationResult.recordFatalError(t);
            throw t;
        } finally {
            taskOperationResult.close();
        }

        postprocessTaskCollectionOperation(resolvedTasks,
                AuditEventType.DELETE_OBJECT, operationTask, parentResult, taskOperationResult);
    }

    @Override
    public void suspendAndDeleteTask(String taskOid,
            long waitForStop, boolean alsoSubtasks, Task operationTask, OperationResult parentResult)
            throws SecurityViolationException, ObjectNotFoundException, SchemaException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException {
        List<PrismObject<TaskType>> resolvedTasks = preprocessTaskOperation(taskOid,
                ModelAuthorizationAction.DELETE, AuditEventType.DELETE_OBJECT, operationTask, parentResult);
        List<String> indestructibleTaskOids = getIndestructibleTaskOids(resolvedTasks, operationTask, parentResult);

        OperationResult taskOperationResult = parentResult.createSubresult(CLASS_NAME_WITH_DOT + "suspendAndDeleteTask"); // details filled in taskManager
        try {
            if (indestructibleTaskOids.isEmpty() || !indestructibleTaskOids.contains(taskOid)) {
                taskManager.suspendAndDeleteTask(taskOid, waitForStop, alsoSubtasks, taskOperationResult);
            }
        } catch (Throwable t) {
            taskOperationResult.recordFatalError(t);
            throw t;
        } finally {
            taskOperationResult.close();
        }

        postprocessTaskCollectionOperation(resolvedTasks,
                AuditEventType.DELETE_OBJECT, operationTask, parentResult, taskOperationResult);
    }

    private List<String> getIndestructibleTaskOids(
            List<PrismObject<TaskType>> allTasks, Task operationTask, OperationResult parentResult) {
        List<String> indestructible = new ArrayList<>();
        for (PrismObject<TaskType> taskType : allTasks) {
            OperationResult result = parentResult.createMinorSubresult(CHECK_INDESTRUCTIBLE);
            try {
                checkIndestructible(taskType, operationTask, result);
                result.recordSuccessIfUnknown();
            } catch (IndestructibilityViolationException e) {
                indestructible.add(taskType.getOid());
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
        } catch (Throwable t) {
            taskOperationResult.recordFatalError(t);
            throw t;
        } finally {
            taskOperationResult.close();
        }

        postprocessTaskCollectionOperation(resolvedTasks,
                AuditEventType.RESUME_TASK, operationTask, parentResult, taskOperationResult);
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
        } catch (Throwable t) {
            taskOperationResult.recordFatalError(t);
            throw t;
        } finally {
            taskOperationResult.close();
        }

        postprocessTaskCollectionOperation(resolvedTasks,
                AuditEventType.RESUME_TASK, operationTask, parentResult, taskOperationResult);
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
        } catch (Throwable t) {
            taskOperationResult.recordFatalError(t);
            throw t;
        } finally {
            taskOperationResult.close();
        }

        postprocessTaskCollectionOperation(resolvedTasks,
                AuditEventType.RESUME_TASK, operationTask, parentResult, taskOperationResult);
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
        } catch (Throwable t) {
            taskOperationResult.recordFatalError(t);
            throw t;
        } finally {
            taskOperationResult.close();
        }

        postprocessTaskCollectionOperation(resolvedTasks,
                AuditEventType.RUN_TASK_IMMEDIATELY, operationTask, parentResult, taskOperationResult);
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
        } catch (Throwable t) {
            taskOperationResult.recordFatalError(t);
            throw t;
        } finally {
            taskOperationResult.close();
        }

        postprocessTaskCollectionOperation(resolvedTasks,
                AuditEventType.RUN_TASK_IMMEDIATELY, operationTask, parentResult, taskOperationResult);
    }

    @Override
    public PrismObject<TaskType> getTaskByIdentifier(String identifier,
            Collection<SelectorOptions<GetOperationOptions>> rawOptions, Task operationTask, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException, CommunicationException {
        Collection<SelectorOptions<GetOperationOptions>> options = preProcessOptionsSecurity(rawOptions, operationTask, parentResult);
        PrismObject<TaskType> task = taskManager.getTaskTypeByIdentifier(identifier, options, parentResult);
        GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);
        task = task.cloneIfImmutable();
        schemaTransformer.applySchemasAndSecurity(task, rootOptions, options, null, null, parentResult);
        return task;
    }

    @Override
    public boolean deactivateServiceThreads(long timeToWait, Task operationTask, OperationResult parentResult)
            throws SchemaException, SecurityViolationException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException {
        securityEnforcer.authorize(ModelAuthorizationAction.STOP_SERVICE_THREADS.getUrl(), null, AuthorizationParameters.EMPTY, null, operationTask, parentResult);
        return taskManager.deactivateServiceThreads(timeToWait, parentResult);
    }

    @Override
    public void reactivateServiceThreads(Task operationTask, OperationResult parentResult)
            throws SchemaException, SecurityViolationException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException {
        securityEnforcer.authorize(ModelAuthorizationAction.START_SERVICE_THREADS.getUrl(),
                null, AuthorizationParameters.EMPTY, null, operationTask, parentResult);
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
        securityEnforcer.authorize(ModelAuthorizationAction.SYNCHRONIZE_TASKS.getUrl(),
                null, AuthorizationParameters.EMPTY, null, operationTask, parentResult);
        taskManager.synchronizeTasks(parentResult);
    }

    @Override
    public void reconcileWorkers(String oid, Task opTask, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException,
            SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        securityEnforcer.authorize(AuthorizationConstants.AUTZ_ALL_URL, null, AuthorizationParameters.EMPTY, null, opTask, result);
        activityManager.reconcileWorkers(oid, result);
    }

    @Override
    public void deleteActivityStateAndWorkers(String rootTaskOid,
            boolean deleteWorkers, long subtasksWaitTime, Task operationTask, OperationResult parentResult)
            throws SecurityViolationException, ObjectNotFoundException, SchemaException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException {
        securityEnforcer.authorize(AuthorizationConstants.AUTZ_ALL_URL,
                null, AuthorizationParameters.EMPTY, null, operationTask, parentResult);
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
        if (securityEnforcer.isAuthorized(AuthorizationConstants.AUTZ_ALL_URL,
                null, AuthorizationParameters.EMPTY, null, task, parentResult)) {
            return;
        }
        for (PrismObject<TaskType> existingObject : existingTasks) {
            securityEnforcer.authorize(action.getUrl(), null,
                    AuthorizationParameters.Builder.buildObject(existingObject), null, task, parentResult);
        }
    }

    private void authorizeNodeCollectionOperation(
            ModelAuthorizationAction action, Collection<String> identifiers, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException {
        if (securityEnforcer.isAuthorized(AuthorizationConstants.AUTZ_ALL_URL, null, AuthorizationParameters.EMPTY, null, task, parentResult)) {
            return;
        }
        for (String identifier : identifiers) {
            PrismObject<NodeType> existingObject;
            ObjectQuery q = ObjectQueryUtil.createNameQuery(NodeType.class, prismContext, identifier);
            List<PrismObject<NodeType>> nodes = cacheRepositoryService.searchObjects(NodeType.class, q, createReadOnlyCollection(), parentResult);
            if (nodes.isEmpty()) {
                throw new ObjectNotFoundException("Node with identifier '" + identifier + "' couldn't be found.");
            } else if (nodes.size() > 1) {
                throw new SystemException("Multiple nodes with identifier '" + identifier + "'");
            }
            existingObject = nodes.get(0);
            securityEnforcer.authorize(action.getUrl(), null, AuthorizationParameters.Builder.buildObject(existingObject), null, task, parentResult);
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

            WorkItemResultType newOutput = new WorkItemResultType(prismContext);
            //noinspection unchecked
            newOutput.asPrismContainerValue().mergeContent(output.asPrismContainerValue(), emptyList());
            newOutput.setAdditionalDeltas(treeDeltas);
            outputToUse = newOutput;
        } else {
            outputToUse = output;
        }
        getCaseManagerChecked().completeWorkItem(workItemId, outputToUse, null, task, parentResult);
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
        getCaseManagerChecked().completeWorkItem(workItemId, output, null, task, parentResult);
    }

    @Override
    public void cancelCase(@NotNull String caseOid, @NotNull Task task, @NotNull OperationResult parentResult) throws SchemaException,
            ObjectNotFoundException, SecurityViolationException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, ObjectAlreadyExistsException {
        getCaseManagerChecked().cancelCase(caseOid, task, parentResult);
    }

    @Override
    public void claimWorkItem(@NotNull WorkItemId workItemId, @NotNull Task task, @NotNull OperationResult parentResult)
            throws SecurityViolationException, ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        getCaseManagerChecked().claimWorkItem(workItemId, task, parentResult);
    }

    @Override
    public void releaseWorkItem(@NotNull WorkItemId workItemId, @NotNull Task task, @NotNull OperationResult parentResult)
            throws ObjectNotFoundException, SecurityViolationException, SchemaException, ObjectAlreadyExistsException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        getCaseManagerChecked().releaseWorkItem(workItemId, task, parentResult);
    }

    @Override
    public void delegateWorkItem(@NotNull WorkItemId workItemId, @NotNull WorkItemDelegationRequestType delegationRequest,
            @NotNull Task task, @NotNull OperationResult parentResult) throws ObjectNotFoundException, SecurityViolationException, SchemaException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException {
        getCaseManagerChecked().delegateWorkItem(workItemId, delegationRequest, task, parentResult);
    }

    //endregion

    //region Scripting (bulk actions)
    @Override
    public void evaluateExpressionInBackground(ScriptingExpressionType expression, Task task, OperationResult parentResult) throws SchemaException, SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
        checkScriptingAuthorization(task, parentResult);
        scriptingExpressionEvaluator.evaluateExpressionInBackground(expression, task, parentResult);
    }

    @Override
    public void evaluateExpressionInBackground(ExecuteScriptType executeScriptCommand, Task task, OperationResult parentResult) throws SchemaException, SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
        checkScriptingAuthorization(task, parentResult);
        scriptingExpressionEvaluator.evaluateExpressionInBackground(executeScriptCommand, task, parentResult);
    }

    @Override
    public ScriptExecutionResult evaluateExpression(ScriptingExpressionType expression, Task task, OperationResult result) throws ScriptExecutionException, SchemaException, SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
        checkScriptingAuthorization(task, result);
        ExecutionContext executionContext = scriptingExpressionEvaluator.evaluateExpression(expression, task, result);
        return executionContext.toExecutionResult();
    }

    @Override
    public ScriptExecutionResult evaluateExpression(@NotNull ExecuteScriptType scriptExecutionCommand,
            @NotNull VariablesMap initialVariables, boolean recordProgressAndIterationStatistics, @NotNull Task task,
            @NotNull OperationResult result)
            throws ScriptExecutionException, SchemaException, SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
        checkScriptingAuthorization(task, result);
        ExecutionContext executionContext = scriptingExpressionEvaluator.evaluateExpression(scriptExecutionCommand, initialVariables,
                recordProgressAndIterationStatistics, task, result);
        return executionContext.toExecutionResult();
    }

    private void checkScriptingAuthorization(Task task, OperationResult parentResult) throws SchemaException, SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
        securityEnforcer.authorize(ModelAuthorizationAction.EXECUTE_SCRIPT.getUrl(), null, AuthorizationParameters.EMPTY, null, task, parentResult);
    }
    //endregion

    //region Certification

    @Override
    public AccessCertificationCasesStatisticsType getCampaignStatistics(String campaignOid, boolean currentStageOnly, Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, SecurityViolationException, ObjectAlreadyExistsException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
        return getCertificationManagerChecked().getCampaignStatistics(campaignOid, currentStageOnly, task, parentResult);
    }

    @Override
    public void cleanupCampaigns(@NotNull CleanupPolicyType policy, Task task, OperationResult result) {
        getCertificationManagerChecked().cleanupCampaigns(policy, task, result);
    }

    @Override
    public void recordDecision(String campaignOid, long caseId, long workItemId, AccessCertificationResponseType response, String comment, Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, SecurityViolationException, ObjectAlreadyExistsException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
        getCertificationManagerChecked().recordDecision(campaignOid, caseId, workItemId, response, comment, task, parentResult);
    }

    @Override
    public List<AccessCertificationWorkItemType> searchOpenWorkItems(ObjectQuery baseWorkItemsQuery, boolean notDecidedOnly,
            boolean allItems, Collection<SelectorOptions<GetOperationOptions>> rawOptions, Task task, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, SecurityViolationException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException {
        Collection<SelectorOptions<GetOperationOptions>> options = preProcessOptionsSecurity(rawOptions, task, parentResult);
        return getCertificationManagerChecked().searchOpenWorkItems(baseWorkItemsQuery, notDecidedOnly, allItems, options, task, parentResult);
    }

    @Override
    public int countOpenWorkItems(ObjectQuery baseWorkItemsQuery, boolean notDecidedOnly, boolean allItems,
            Collection<SelectorOptions<GetOperationOptions>> rawOptions, Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, SecurityViolationException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
        Collection<SelectorOptions<GetOperationOptions>> options = preProcessOptionsSecurity(rawOptions, task, parentResult);
        return getCertificationManagerChecked().countOpenWorkItems(baseWorkItemsQuery, notDecidedOnly, allItems, options, task, parentResult);
    }

    @Override
    public void closeCampaign(String campaignOid, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, SecurityViolationException, ObjectAlreadyExistsException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
        getCertificationManagerChecked().closeCampaign(campaignOid, task, result);
    }

    @Override
    public void reiterateCampaign(String campaignOid, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, SecurityViolationException, ObjectAlreadyExistsException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
        getCertificationManagerChecked().reiterateCampaign(campaignOid, task, result);
    }

    @Override
    public void startRemediation(String campaignOid, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, SecurityViolationException, ObjectAlreadyExistsException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
        getCertificationManagerChecked().startRemediation(campaignOid, task, result);
    }

    @Override
    public void closeCurrentStage(String campaignOid, Task task, OperationResult parentResult) throws SchemaException, SecurityViolationException, ObjectNotFoundException, ObjectAlreadyExistsException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
        getCertificationManagerChecked().closeCurrentStage(campaignOid, task, parentResult);
    }

    @Override
    public void openNextStage(String campaignOid, Task task, OperationResult parentResult) throws SchemaException, SecurityViolationException, ObjectNotFoundException, ObjectAlreadyExistsException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
        getCertificationManagerChecked().openNextStage(campaignOid, task, parentResult);
    }

    @Override
    public AccessCertificationCampaignType createCampaign(String definitionOid, Task task, OperationResult parentResult) throws SchemaException, SecurityViolationException, ObjectNotFoundException, ObjectAlreadyExistsException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
        return getCertificationManagerChecked().createCampaign(definitionOid, task, parentResult);
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

            Collection<ObjectDeltaOperation<? extends ObjectType>> deltas =
                    objectMerger.mergeObjects(type, leftOid, rightOid, mergeConfigurationName, task, result);

            result.computeStatus();
            return deltas;

        } catch (ObjectNotFoundException | SchemaException | ConfigurationException | ObjectAlreadyExistsException | ExpressionEvaluationException | CommunicationException | PolicyViolationException | SecurityViolationException | RuntimeException | Error e) {
            ModelImplUtils.recordFatalError(result, e);
            throw e;
        } finally {
            QNameUtil.setTemporarilyTolerateUndeclaredPrefixes(false);
            exitModelMethod();
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
        securityEnforcer.authorize(ModelAuthorizationAction.READ_THREADS.getUrl(), null, AuthorizationParameters.EMPTY, null, task, parentResult);
        return MiscUtil.takeThreadDump(null);
    }

    @Override
    public String getRunningTasksThreadsDump(@NotNull Task task, @NotNull OperationResult parentResult)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        securityEnforcer.authorize(ModelAuthorizationAction.READ_THREADS.getUrl(), null, AuthorizationParameters.EMPTY, null, task, parentResult);
        return taskManager.getRunningTasksThreadsDump(parentResult);
    }

    @Override
    public String recordRunningTasksThreadsDump(String cause, @NotNull Task task, @NotNull OperationResult parentResult)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException, ObjectAlreadyExistsException {
        securityEnforcer.authorize(ModelAuthorizationAction.READ_THREADS.getUrl(), null, AuthorizationParameters.EMPTY, null, task, parentResult);
        return taskManager.recordRunningTasksThreadsDump(cause, parentResult);
    }

    @Override
    public String getTaskThreadsDump(@NotNull String taskOid, @NotNull Task task, @NotNull OperationResult parentResult)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        securityEnforcer.authorize(ModelAuthorizationAction.READ_THREADS.getUrl(), null, AuthorizationParameters.EMPTY, null, task, parentResult);
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
            result.computeStatusIfUnknown();
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
                Collection<? extends ItemDelta> modifications = DeltaConvertor.toModifications(
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
            delta.accept(this::computePolyStringVisit);
        }
    }

    private void computePolyStringVisit(Visitable visitable) {
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
}
