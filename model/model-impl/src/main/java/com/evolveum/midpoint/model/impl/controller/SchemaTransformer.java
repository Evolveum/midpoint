/*
 * Copyright (c) 2014-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.controller;

import static com.evolveum.midpoint.schema.GetOperationOptions.createReadOnlyCollection;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.crypto.CryptoUtil;
import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.common.archetypes.ArchetypeManager;
import com.evolveum.midpoint.model.impl.controller.transformer.DataAccessProcessor;
import com.evolveum.midpoint.model.impl.controller.transformer.DataPolicyProcessor;
import com.evolveum.midpoint.model.impl.controller.transformer.DefinitionAccessProcessor;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensElementContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.schema.transform.DefinitionsToTransformable;
import com.evolveum.midpoint.model.impl.schema.transform.TransformableObjectDefinition;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.ItemDefinitionTransformer.TransformableItem;
import com.evolveum.midpoint.prism.ItemDefinitionTransformer.TransformableValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.PathSet;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.security.enforcer.api.*;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Transforms the schema and objects by applying security constraints,
 * object template schema refinements, etc.
 *
 * @author semancik
 */
@Component
public class SchemaTransformer {

    private static final Trace LOGGER = TraceManager.getTrace(SchemaTransformer.class);

    private static final String OP_APPLY_SCHEMAS_AND_SECURITY_TO_OBJECT =
            SchemaTransformer.class.getName() + ".applySchemasAndSecurityToObject";
    private static final String OP_APPLY_SECURITY_TO_LENS_CONTEXT =
            SchemaTransformer.class.getName() + ".applySecurityToLensContext";

    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;
    @Autowired private SecurityEnforcer securityEnforcer;
    @Autowired private ArchetypeManager archetypeManager;

    @Autowired private DataAccessProcessor dataAccessProcessor;
    @Autowired private DefinitionAccessProcessor definitionAccessProcessor;
    @Autowired private DataPolicyProcessor dataPolicyProcessor;

    /**
     * Applies security to a {@link SearchResultList} of prism objects.
     *
     * Assumes some items may be immutable, and does lazy object or object list cloning:
     *
     * - Some objects may be mutated or cloned-and-mutated (if the object was immutable).
     * - This may result in modifying the original `objects` list, or returning its modified copy (if the list was immutable).
     *
     * Exceptions (except for {@link SecurityViolationException}) are currently *not thrown*. Instead, they are
     * recorded in {@link ObjectType#F_FETCH_RESULT} - *to be discussed if it's safe*.
     */
    @VisibleForTesting // public because of the reference from TestPerformance
    public <T extends ObjectType> SearchResultList<PrismObject<T>> applySchemasAndSecurityToObjects(
            SearchResultList<PrismObject<T>> objects,
            ParsedGetOperationOptions options,
            Task task,
            OperationResult result)
            throws SecurityViolationException {
        List<PrismObject<T>> newList = null; // created if needed
        for (int i = 0; i < objects.size(); i++) {
            PrismObject<T> object = objects.get(i);
            PrismObject<T> objectAfter = applySchemasAndSecurityToObjectInList(object, options, task, result);
            if (newList != null) {
                newList.add(objectAfter);
            } else {
                if (objectAfter != object) {
                    if (objects.isImmutable()) {
                        newList = new ArrayList<>(objects.size());
                        newList.addAll(objects.subList(0, i));
                        newList.add(objectAfter);
                    } else {
                        objects.set(i, objectAfter);
                    }
                } else {
                    // the object is in its place
                }
            }
        }
        if (newList != null) {
            return new SearchResultList<>(newList, objects.getMetadata());
        } else {
            return objects;
        }
    }

    private <T extends ObjectType> PrismObject<T> applySchemasAndSecurityToObjectInList(
            PrismObject<T> object,
            ParsedGetOperationOptions options,
            Task task,
            OperationResult parentResult) throws SecurityViolationException {
        OperationResult result = parentResult.createMinorSubresult(OP_APPLY_SCHEMAS_AND_SECURITY_TO_OBJECT);
        try {
            return applySchemasAndSecurityToObject(object, options, task, result);
        } catch (IllegalArgumentException | IllegalStateException | SchemaException | ConfigurationException |
                ObjectNotFoundException | ExpressionEvaluationException | CommunicationException e) {
            LOGGER.error("Error post-processing object {}: {}", object, e.getMessage(), e);
            result.recordFatalError(e);
            result.close();
            // todo is it safe to keep the object in the result set if it was not completely post-processed?
            return updateFetchResult(object, result);
        } catch (SecurityViolationException e) {
            // We cannot go on and leave this object in the result set. The object was not post-processed.
            // Leaving it in the result set may leak information.
            result.recordFatalError(e);
            throw e;
        } finally {
            result.close();
        }
    }

    // TODO why is this method used only when processing objects lists and not for individual objects?
    private static <T extends ObjectType> PrismObject<T> updateFetchResult(PrismObject<T> object, OperationResult result) {
        var clonedObject = object.cloneIfImmutable();
        OperationResultType fetchResult = clonedObject.asObjectable().getFetchResult();
        if (fetchResult == null) {
            clonedObject.asObjectable().setFetchResult(result.createBeanReduced());
        } else {
            fetchResult.getPartialResults().add(result.createBeanReduced());
            fetchResult.setStatus(OperationResultStatusType.FATAL_ERROR);
        }
        return clonedObject;
    }

    /**
     * Applies the security and schemas to the *whole objects* that contain the respective values.
     *
     * Fails if there is a value without the containing object.
     *
     * The values and their containing objects must be mutable, so they can be pruned in place.
     * (Otherwise, we would have problems with re-constructing the values list from the cloned objects.)
     */
    <C extends Containerable> void applySchemasAndSecurityToContainerValues(
            @NotNull SearchResultList<C> values,
            @NotNull ParsedGetOperationOptions parsedOptions,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SecurityViolationException, SchemaException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {

        Collection<PrismObject<? extends ObjectType>> roots = ObjectTypeUtil.getRootsForContainerables(values);

        applySchemasAndSecurityToMutableObjects(roots, parsedOptions, task, result);

        // We have to check that no value got detached in the process of applying the constraints.
        var detached = ObjectTypeUtil.getDetachedContainerables(values);
        if (!detached.isEmpty()) {
            throw new SecurityViolationException( // TODO do better diagnostics
                    "%d containerable(s) returned by a search are not allowed by #get authorization: %s"
                            .formatted(
                                    detached.size(),
                                    MiscUtil.getDiagInfo(detached, 10, 200)));
        }
    }

    /**
     * As {@link #applySchemasAndSecurityToObjects(SearchResultList, ParsedGetOperationOptions, Task, OperationResult)} but:
     *
     * . assumes that the list and the objects in it are mutable;
     * . relaxes the type restriction on the list;
     * . reports all errors as exceptions, as we do not have `fetchResult` to provide the failure information in.
     */
    private void applySchemasAndSecurityToMutableObjects(
            Collection<PrismObject<? extends ObjectType>> objects,
            ParsedGetOperationOptions options,
            Task task,
            OperationResult parentResult)
            throws SecurityViolationException, SchemaException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        for (PrismObject<? extends ObjectType> object : objects) {
            assert !object.isImmutable();
            OperationResult result = parentResult.createMinorSubresult(OP_APPLY_SCHEMAS_AND_SECURITY_TO_OBJECT);
            try {
                var returned = applySchemasAndSecurityToObject(object, options, task, result);
                stateCheck(returned == object, "Transformation returned a different object: %s", returned);
            } catch (Throwable t) {
                result.recordException(t);
                throw t;
            } finally {
                result.close();
            }
        }
    }

    /**
     * Validates the object, removes any non-visible properties (security), applies object template definitions and so on.
     * This method is called for any object that is returned from the Model Service.
     *
     * Intentionally does NOT apply the authorizations to the object and sub-items definitions.
     * This is done in {@link ModelInteractionService#getEditObjectDefinition(PrismObject, AuthorizationPhaseType, Task,
     * OperationResult)} only.
     *
     * See also https://github.com/Evolveum/docs/blob/master/midpoint/devel/design/apply-schemas-and-security-4.8/design-meetings.adoc.
     *
     * Creates an object clone, if necessary (i.e. if the object is immutable, and there are changes to be made).
     * [Currently, always clones immutable objects. But this will be changed eventually.]
     */
    <O extends ObjectType> @NotNull PrismObject<O> applySchemasAndSecurityToObject(
            @NotNull PrismObject<O> object,
            @NotNull ParsedGetOperationOptions options,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, SecurityViolationException, ConfigurationException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException {
        LOGGER.trace("applySchemasAndSecurityToObject({}) starting", object);

        authorizeRawOption(object, options, task, result);
        validateObject(object, options);

        DefinitionUpdateOption definitionUpdateOption = options.getDefinitionUpdate();

        ObjectTemplateType objectTemplate;
        // we must determine the template before object items are stripped off by authorizations
        if (!options.isRaw() && definitionUpdateOption != DefinitionUpdateOption.NONE) {
            objectTemplate = determineObjectTemplate(object, result);
        } else {
            objectTemplate = null;
        }

        AuthorizationPhaseType phase = options.isExecutionPhase() ? AuthorizationPhaseType.EXECUTION : null;
        var readConstraints =
                securityEnforcer.compileOperationConstraints(
                        securityEnforcer.getMidPointPrincipal(),
                        object.getValue(),
                        phase,
                        ModelAuthorizationAction.AUTZ_ACTIONS_URLS_GET,
                        SecurityEnforcer.Options.create(),
                        CompileConstraintsOptions.create(),
                        task, result);
        PrismObject<O> objectAfter = dataAccessProcessor.applyReadConstraints(object, readConstraints);

        if (definitionUpdateOption == DefinitionUpdateOption.NONE || objectTemplate == null) {
            return objectAfter;
        } else {
            // The following can be optimized further. For example, we do not have to switch all definitions
            // to transformable ones. But this will be dealt with later - if needed.

            var mutable = objectAfter.cloneIfImmutable();
            transform(mutable, new DefinitionsToTransformable());

            // we do not need to process object template when processing in raw mode
            // TODO cache the definitions
            dataPolicyProcessor.applyObjectTemplateToObject(mutable, objectTemplate, definitionUpdateOption, task, result);

            // TODO consider removing this as it shouldn't be needed after definitions caching is implemented
            applyDefinitionProcessingOption(mutable, options);

            return mutable;
        }
    }

    private <O extends ObjectType> void authorizeRawOption(
            PrismObject<O> object,
            ParsedGetOperationOptions options,
            Task task,
            OperationResult result)
            throws SchemaException, SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException {
        if (options.isRaw()) {
            securityEnforcer.authorize(
                    ModelAuthorizationAction.RAW_OPERATION.getUrl(),
                    null,
                    AuthorizationParameters.Builder.buildObject(object),
                    task, result);
        }
    }

    private <T extends ObjectType> void validateObject(PrismObject<T> object, ParsedGetOperationOptions options) {
        if (InternalsConfig.readEncryptionChecks) {
            CryptoUtil.checkEncrypted(object);
        }
        if (InternalsConfig.consistencyChecks) {
            boolean tolerateRaw;
            if (ObjectTypeUtil.hasFetchError(object)) {
                // If there is an error then the object might not be complete.
                // E.g. we do not have a complete dynamic schema to apply to the object
                // Tolerate some raw meat in that case.
                tolerateRaw = true;
            } else {
                Class<T> type = object.getCompileTimeClass();
                if (ResourceType.class.equals(type) || ShadowType.class.equals(type) || ReportType.class.equals(type)) {
                    // We tolerate raw values for resource and shadows in case the user has requested so
                    tolerateRaw = options.isRaw();
                } else {
                    tolerateRaw = options.isTolerateRawData();
                }
            }
            object.checkConsistence(true, !tolerateRaw, ConsistencyCheckScope.THOROUGH);
        }
    }

    private void applyDefinitionProcessingOption(PrismObject<? extends ObjectType> object, ParsedGetOperationOptions options) {
        if (options.isEmpty()) {
            return;
        }
        Map<DefinitionProcessingOption, PathSet> definitionProcessing = options.getDefinitionProcessingMap();
        if (CollectionUtils.isNotEmpty(definitionProcessing.get(DefinitionProcessingOption.NONE))) {
            throw new UnsupportedOperationException("'NONE' definition processing is not supported now");
        }
        PathSet onlyIfExists = definitionProcessing.get(DefinitionProcessingOption.ONLY_IF_EXISTS);
        if (CollectionUtils.isNotEmpty(onlyIfExists)) {
            if (onlyIfExists.size() != 1 || !ItemPath.isEmpty(onlyIfExists.iterator().next())) {
                throw new UnsupportedOperationException("'ONLY_IF_EXISTS' definition processing is currently"
                        + " supported on root level only; not on " + onlyIfExists);
            }
            PathSet full = definitionProcessing.get(DefinitionProcessingOption.FULL);
            object.trimDefinitionTree(full);
        }
    }

    private void transform(Item<?,?> object, ItemDefinitionTransformer transformation) {
        Preconditions.checkArgument(
                object instanceof TransformableItem,
                "Value must be %s", TransformableValue.class.getSimpleName());
        ((TransformableItem) object).transformDefinition(null, transformation);
    }

    void applySecurityToLensContext(@NotNull LensContext<? extends ObjectType> context, Task task, OperationResult parentResult)
            throws SecurityViolationException, SchemaException, ConfigurationException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException {
        LOGGER.trace("applySecurityToLensContext({}) starting", context);
        OperationResult result = parentResult.createMinorSubresult(OP_APPLY_SECURITY_TO_LENS_CONTEXT);
        try {
            applySecurityToLensContextUnchecked(context, task, result);
        } catch (Throwable e) {
            result.recordException(e);
            throw e;
        } finally {
            result.close();
        }
        LOGGER.trace("applySecurityToLensContext finishing");
    }
    private <O extends ObjectType> void applySecurityToLensContextUnchecked(
            LensContext<O> context, Task task, OperationResult result)
            throws SecurityViolationException, SchemaException, ConfigurationException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException {

        ParsedGetOperationOptions getOptions = ParsedGetOperationOptions.of(
                ModelExecuteOptions.toGetOperationOptions(context.getOptions()));

        if (context.hasFocusContext() && context.getFocusContext().getObjectAny() != null) {
            PrismObject<O> focusObject = context.getFocusContext().getObjectAny();
            var readConstraints = getLensReadConstraints(focusObject, task, result);
            LensFocusContext<O> securedFocus = applySecurityToFocusContext(context.getFocusContext(), readConstraints,
                    getOptions, task, result);
            context.setFocusContext(securedFocus);
            applySecurityToEvaluatedAssignments(context, readConstraints, result);
        }
        Collection<LensProjectionContext> securedProjections = applySecurityToProjectionsContexts(
                context.getProjectionContexts(), getOptions, task, result);

        context.replaceProjectionContexts(securedProjections);
    }

    private <O extends ObjectType> void applySecurityToEvaluatedAssignments(LensContext<O> context,
            PrismEntityOpConstraints.ForValueContent readConstraints, OperationResult result) {
        PrismObject<O> focusObject = context.getFocusContext().getObjectAny();
        AccessDecision assignmentDecision = readConstraints
                .getValueConstraints(FocusType.F_ASSIGNMENT).getDecision();
        if (assignmentDecision != AccessDecision.ALLOW) {
            LOGGER.trace("Logged in user isn't authorized to read (or get) assignment item of the object: {}", focusObject);
            result.recordWarning("Logged in user isn't authorized to read (or get) assignment item of the object: " + focusObject);
            context.setEvaluatedAssignmentTriple(null);
        }
    }

    private <O extends ObjectType> LensFocusContext<O> applySecurityToFocusContext(
            LensFocusContext<O> focusContext, PrismEntityOpConstraints.ForValueContent readConstraints,
            ParsedGetOperationOptions getOptions, Task task, OperationResult result)
            throws SecurityViolationException, SchemaException, ConfigurationException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException {
        LensFocusContext<O> focusContextClone = focusContext.clone(focusContext.getLensContext());

        applySecurityToElementContext(focusContextClone, readConstraints, getOptions, task, result);
        return focusContextClone;
    }

    private Collection<LensProjectionContext> applySecurityToProjectionsContexts(
            Collection<LensProjectionContext> projections, ParsedGetOperationOptions getOptions, Task task,
            OperationResult result)
            throws SecurityViolationException, SchemaException, ConfigurationException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException {
        List<LensProjectionContext> securedProjections = new ArrayList<>();
        for (LensProjectionContext projCtx : projections) {
            if (projCtx.getObjectAny() != null) {
                LensProjectionContext projection = projCtx.clone(projCtx.getLensContext());
                var readConstraints = getLensReadConstraints(projection.getObjectAny(), task, result);

                applySecurityToElementContext(projection, readConstraints, getOptions, task, result);
                securedProjections.add(projection);
            }
        }
        return securedProjections;
    }

    private <O extends ObjectType> void applySecurityToElementContext(
            LensElementContext<O> elementContext, PrismEntityOpConstraints.ForValueContent readConstraints,
            ParsedGetOperationOptions getOptions, Task task, OperationResult result)
            throws SecurityViolationException, SchemaException, ConfigurationException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException {

        PrismObject<O> object = elementContext.getObjectAny();
        authorizeRawOption(object, getOptions, task, result);

        dataAccessProcessor.applyReadConstraints(elementContext, readConstraints);
    }

    /**
     * Create read constraints for use when applying security on LensContext
     *
     * Note that the application of read constraints to an objects in the context and to the deltas may be imprecise:
     * the data being processed may differ (various objects and the deltas) from the data upon which the authorizations
     * are derived (one of the objects). Hence, we can safely deal only with selectors that do not distinguish between
     * item values. This is achieved by ignoring sub-object selectors, see
     * {@link CompileConstraintsOptions#skipSubObjectSelectors}.
     */
    private <O extends ObjectType> PrismEntityOpConstraints.ForValueContent getLensReadConstraints(
            PrismObject<O> object, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
                    ConfigurationException, SecurityViolationException {
        return securityEnforcer.compileOperationConstraints(
                securityEnforcer.getMidPointPrincipal(),
                object.getValue(),
                null,
                ModelAuthorizationAction.AUTZ_ACTIONS_URLS_GET,
                SecurityEnforcer.Options.create(),
                CompileConstraintsOptions.skipSubObjectSelectors(),
                task, result);
    }

    <D extends ItemDefinition<?>> void applySecurityConstraintsToItemDef(
            @NotNull D itemDefinition,
            @NotNull ObjectSecurityConstraints securityConstraints,
            @Nullable AuthorizationPhaseType phase) {
        definitionAccessProcessor.applySecurityConstraintsToItemDef(itemDefinition, securityConstraints, phase);
    }

    private <O extends ObjectType> ObjectTemplateType determineObjectTemplate(PrismObject<O> object, OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException {
        ArchetypePolicyType archetypePolicy = archetypeManager.determineArchetypePolicy(object, result);
        if (archetypePolicy == null) {
            return null;
        }
        ObjectReferenceType objectTemplateRef = archetypePolicy.getObjectTemplateRef();
        if (objectTemplateRef == null || objectTemplateRef.getOid() == null) {
            return null;
        }
        PrismObject<ObjectTemplateType> template =
                repositoryService.getObject(
                        ObjectTemplateType.class, objectTemplateRef.getOid(), createReadOnlyCollection(), result);
        return template.asObjectable();
    }

    <O extends ObjectType> void applyObjectTemplateToDefinition(
            PrismObjectDefinition<O> objectDefinition, ObjectTemplateType objectTemplate, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ConfigurationException {
        dataPolicyProcessor.applyObjectTemplateToDefinition(objectDefinition, objectTemplate, task, result);
    }

    <O extends ObjectType> void applyItemsConstraints(
            @NotNull PrismContainerDefinition<O> objectDefinition,
            @NotNull ArchetypePolicyType archetypePolicy) throws SchemaException {
        dataPolicyProcessor.applyItemsConstraints(objectDefinition, archetypePolicy);
    }

    <O extends Objectable> @NotNull TransformableObjectDefinition<O> transformableDefinition(PrismObjectDefinition<O> definition) {
        return TransformableObjectDefinition.of(definition);
    }
}
