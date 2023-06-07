/*
 * Copyright (c) 2014-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.controller;

import static com.evolveum.midpoint.schema.GetOperationOptions.createReadOnlyCollection;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.UserInterfaceElementVisibilityType.*;

import java.util.*;

import com.evolveum.midpoint.security.enforcer.api.*;

import com.google.common.base.Preconditions;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Contract;
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
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensElementContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.schema.transform.DefinitionsToTransformable;
import com.evolveum.midpoint.model.impl.schema.transform.TransformableItemDefinition;
import com.evolveum.midpoint.model.impl.schema.transform.TransformableObjectDefinition;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.ItemDefinitionTransformer.TransformableItem;
import com.evolveum.midpoint.prism.ItemDefinitionTransformer.TransformableValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.PathSet;
import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ItemRefinedDefinitionTypeUtil;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.SimulationUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * Transforms the schema and objects by applying security constraints,
 * object template schema refinements, etc.
 *
 * @author semancik
 */
@Component
public class SchemaTransformer {

    private static final Trace LOGGER = TraceManager.getTrace(SchemaTransformer.class);

    private static final String OP_APPLY_SCHEMAS_AND_SECURITY_TO_OBJECT = SchemaTransformer.class.getName() + ".applySchemasAndSecurityToObject";
    private static final String OP_APPLY_SECURITY_TO_LENS_CONTEXT = SchemaTransformer.class.getName() + ".applySecurityToLensContext";

    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService cacheRepositoryService;
    @Autowired private SecurityEnforcer securityEnforcer;
    @Autowired private ArchetypeManager archetypeManager;
    @Autowired private PrismContext prismContext;

    @Autowired private ReadConstraintsApplicator readConstraintsApplicator;

    /**
     * Applies security to a {@link SearchResultList} of prism objects.
     *
     * Assumes some items may be immutable, and does lazy object or object list cloning:
     *
     * - Some objects may be mutated or cloned-and-mutated (if the object was immutable).
     * - This may result in modifying the original `objects` list, or returning a its modified copy (if the list was immutable).
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

        // We have to check that no value was disconnected in the process of applying the constraints.
        ObjectTypeUtil.getRootsForContainerables(values);
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
            @NotNull ParsedGetOperationOptions parsedOptions,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, SecurityViolationException, ConfigurationException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException {
        LOGGER.trace("applySchemasAndSecurityToObject2({}) starting", object);
        var rootOptions = parsedOptions.getRootOptions();
        DefinitionUpdateOption definitionUpdateOption = parsedOptions.getDefinitionUpdate();
        boolean raw = GetOperationOptions.isRaw(rootOptions);
        ObjectTemplateType objectTemplate;

        authorizeRawOption(object, rootOptions, task, result);
        validateObject(object, rootOptions);
        // we must determine the template before object items are stripped off by authorizations
        if (!raw && definitionUpdateOption != DefinitionUpdateOption.NONE) {
            objectTemplate = determineObjectTemplate(object, result);
        } else {
            objectTemplate = null;
        }

        AuthorizationPhaseType phase =
                GetOperationOptions.isExecutionPhase(rootOptions) ? AuthorizationPhaseType.EXECUTION : null;
        var readConstraints =
                securityEnforcer.compileOperationConstraints(
                        object.getValue(),
                        phase,
                        null,
                        ModelAuthorizationAction.AUTZ_ACTIONS_URLS_GET,
                        CompileConstraintsOptions.defaultOnes(),
                        task, result);
        PrismObject<O> objectAfter = readConstraintsApplicator.applyReadConstraints(object, readConstraints);

        if (definitionUpdateOption == DefinitionUpdateOption.NONE || objectTemplate == null) {
            return objectAfter;
        } else {
            // The following can be optimized further. For example, we do not have to switch all definitions
            // to transformable ones. But this will be dealt with later - if needed.

            var mutable = objectAfter.cloneIfImmutable();
            transform(mutable, new DefinitionsToTransformable());

            // we do not need to process object template when processing in raw mode
            // TODO cache the definitions
            applyObjectTemplateToObject(mutable, objectTemplate, definitionUpdateOption, task, result);

            // TODO consider removing this as it shouldn't be needed after definitions caching is implemented
            applyDefinitionProcessingOption(mutable, parsedOptions);

            return mutable;
        }
    }

    private <O extends ObjectType> void authorizeRawOption(
            PrismObject<O> object, GetOperationOptions rootOptions,
            Task task,
            OperationResult result)
            throws SchemaException, SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException {
        if (GetOperationOptions.isRaw(rootOptions)) {
            securityEnforcer.authorize(
                    ModelAuthorizationAction.RAW_OPERATION.getUrl(),
                    null,
                    AuthorizationParameters.Builder.buildObject(object),
                    null, task, result);
        }
    }

    private <T extends ObjectType> void validateObject(PrismObject<T> object, GetOperationOptions options) {
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
                    tolerateRaw = GetOperationOptions.isRaw(options);
                } else {
                    tolerateRaw = GetOperationOptions.isTolerateRawData(options);
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

    <O extends ObjectType> void applySecurityToLensContext(
            @NotNull LensContext<O> context, Task task, OperationResult parentResult)
            throws SecurityViolationException, SchemaException, ConfigurationException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException {
        LOGGER.trace("applySecurityToLensContext({}) starting", context);
        OperationResult result = parentResult.createMinorSubresult(OP_APPLY_SECURITY_TO_LENS_CONTEXT);
        try {
            applySecurityToFocusContext(context, task, result);
            applySecurityToProjectionsContexts(context, task, result);
        } catch (Throwable e) {
            result.recordException(e);
            throw e;
        } finally {
            result.close();
        }
        LOGGER.trace("applySecurityToLensContext finishing"); // to allow folding in log viewer
    }

    private <O extends ObjectType> void applySecurityToFocusContext(
            LensContext<O> context, Task task, OperationResult result)
            throws SecurityViolationException, SchemaException, ConfigurationException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException {
        LensFocusContext<O> focusContext = context.getFocusContext();
        PrismObject<O> object = focusContext != null ? focusContext.getObjectAny() : null;
        if (object == null) {
            return;
        }

        var readConstraints = applySecurityToElementContext(focusContext, task, result);
        var assignmentDecision = readConstraints.getValueConstraints(FocusType.F_ASSIGNMENT).getDecision();
        if (assignmentDecision != AccessDecision.ALLOW) {
            LOGGER.trace("Logged in user isn't authorized to read (or get) assignment item of the object: {}", object);
            result.recordWarning("Logged in user isn't authorized to read (or get) assignment item of the object: " + object);
            context.setEvaluatedAssignmentTriple(null);
        }
    }

    private <O extends ObjectType> void applySecurityToProjectionsContexts(
            LensContext<O> context, Task task, OperationResult result)
            throws SecurityViolationException, SchemaException, ConfigurationException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException {
        for (LensProjectionContext projCtx : context.getProjectionContexts()) {
            if (projCtx != null && projCtx.getObjectAny() != null) {
                applySecurityToElementContext(projCtx, task, result);
            }
        }
    }

    /**
     * Note that the application of read constraints to a objects in the context and to the deltas may be imprecise:
     * the data being processed may differ (various objects and the deltas) from the data upon which the authorizations
     * are derived (one of the objects). Hence, we can safely deal only with selectors that do not distinguish between
     * item values. This is achieved by ignoring sub-object selectors, see
     * {@link CompileConstraintsOptions#skipSubObjectSelectors}.
     */
    private <O extends ObjectType> PrismEntityOpConstraints.ForValueContent applySecurityToElementContext(
            LensElementContext<O> elementContext, Task task, OperationResult result)
            throws SecurityViolationException, SchemaException, ConfigurationException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException {

        PrismObject<O> object = elementContext.getObjectAny();
        assert object != null;

        GetOperationOptions getOptions = ModelExecuteOptions.toGetOperationOptions(elementContext.getLensContext().getOptions());
        authorizeRawOption(object, getOptions, task, result);

        var readConstraints = securityEnforcer.compileOperationConstraints(
                object.getValue(),
                null,
                null,
                ModelAuthorizationAction.AUTZ_ACTIONS_URLS_GET,
                CompileConstraintsOptions.skipSubObjectSelectors(),
                task, result);

        return readConstraintsApplicator.applyReadConstraints(elementContext, readConstraints);
    }

    private MutableItemDefinition<?> mutable(ItemDefinition<?> itemDef) {
        return itemDef.toMutable();
    }

    <D extends ItemDefinition<?>> void applySecurityConstraintsToItemDef(
            @NotNull D itemDefinition,
            @NotNull ObjectSecurityConstraints securityConstraints,
            @Nullable AuthorizationPhaseType phase) {
        if (phase == null) {
            applySecurityConstraintsToItemDefPhase(itemDefinition, securityConstraints, AuthorizationPhaseType.REQUEST);
            applySecurityConstraintsToItemDefPhase(itemDefinition, securityConstraints, AuthorizationPhaseType.EXECUTION);
        } else {
            applySecurityConstraintsToItemDefPhase(itemDefinition, securityConstraints, phase);
        }
    }

    private <D extends ItemDefinition<?>> void applySecurityConstraintsToItemDefPhase(
            @NotNull D itemDefinition,
            @NotNull ObjectSecurityConstraints securityConstraints,
            @NotNull AuthorizationPhaseType phase) {
        Validate.notNull(phase);
        LOGGER.trace("applySecurityConstraints(itemDefs): def={}, phase={}", itemDefinition, phase);
        applySecurityConstraintsToItemDef(
                itemDefinition, new IdentityHashMap<>(), ItemPath.EMPTY_PATH, securityConstraints,
                null, null, null, phase);
    }

    private <D extends ItemDefinition<?>> void applySecurityConstraintsToItemDef(
            @NotNull D itemDefinition,
            @NotNull IdentityHashMap<ItemDefinition<?>, Object> definitionsSeen,
            @NotNull ItemPath nameOnlyItemPath,
            @NotNull ObjectSecurityConstraints securityConstraints,
            @Nullable AuthorizationDecisionType defaultReadDecision,
            @Nullable AuthorizationDecisionType defaultAddDecision,
            @Nullable AuthorizationDecisionType defaultModifyDecision,
            @NotNull AuthorizationPhaseType phase) {

        boolean thisWasSeen = definitionsSeen.containsKey(itemDefinition);
        definitionsSeen.put(itemDefinition, null);

        AuthorizationDecisionType readDecision = computeItemDecision(
                securityConstraints, nameOnlyItemPath, ModelAuthorizationAction.AUTZ_ACTIONS_URLS_GET, defaultReadDecision, phase);
        AuthorizationDecisionType addDecision = computeItemDecision(
                securityConstraints, nameOnlyItemPath, ModelAuthorizationAction.AUTZ_ACTIONS_URLS_ADD, defaultAddDecision, phase);
        AuthorizationDecisionType modifyDecision = computeItemDecision(
                securityConstraints, nameOnlyItemPath, ModelAuthorizationAction.AUTZ_ACTIONS_URLS_MODIFY, defaultModifyDecision, phase);

        boolean anySubElementRead = false;
        boolean anySubElementAdd = false;
        boolean anySubElementModify = false;
        if (itemDefinition instanceof PrismContainerDefinition<?>) {
            if (thisWasSeen) {
                LOGGER.trace("applySecurityConstraintsToItemDef: {}: skipping (already seen)", nameOnlyItemPath);
            } else if (itemDefinition.isElaborate()) {
                LOGGER.trace("applySecurityConstraintsToItemDef: {}: skipping (elaborate)", nameOnlyItemPath);
            } else {
                PrismContainerDefinition<?> containerDefinition = (PrismContainerDefinition<?>) itemDefinition;
                List<? extends ItemDefinition<?>> subDefinitions = containerDefinition.getDefinitions();
                for (ItemDefinition<?> subDef : subDefinitions) {
                    ItemPath subPath = ItemPath.create(nameOnlyItemPath, subDef.getItemName());
                    if (!subDef.getItemName().equals(ShadowType.F_ATTRIBUTES)) { // Shadow attributes have special handling
                        applySecurityConstraintsToItemDef(
                                subDef, definitionsSeen, subPath, securityConstraints,
                                readDecision, addDecision, modifyDecision, phase);
                    }
                    if (subDef.canRead()) {
                        anySubElementRead = true;
                    }
                    if (subDef.canAdd()) {
                        anySubElementAdd = true;
                    }
                    if (subDef.canModify()) {
                        anySubElementModify = true;
                    }
                }
            }
        }

        LOGGER.trace("applySecurityConstraintsToItemDef: {}: decisions R={}, A={}, M={}; sub-elements R={}, A={}, M={}",
                nameOnlyItemPath, readDecision, addDecision, modifyDecision, anySubElementRead, anySubElementAdd, anySubElementModify);

        if (readDecision != AuthorizationDecisionType.ALLOW) {
            mutable(itemDefinition).setCanRead(false);
        }
        if (addDecision != AuthorizationDecisionType.ALLOW) {
            mutable(itemDefinition).setCanAdd(false);
        }
        if (modifyDecision != AuthorizationDecisionType.ALLOW) {
            mutable(itemDefinition).setCanModify(false);
        }

        if (anySubElementRead) {
            mutable(itemDefinition).setCanRead(true);
        }
        if (anySubElementAdd) {
            mutable(itemDefinition).setCanAdd(true);
        }
        if (anySubElementModify) {
            mutable(itemDefinition).setCanModify(true);
        }
    }

    @Contract("_, _, _, !null, _ -> !null")
    AuthorizationDecisionType computeItemDecision(
            @NotNull ObjectSecurityConstraints securityConstraints,
            @NotNull ItemPath nameOnlyItemPath,
            @NotNull String[] actionUrls,
            @Nullable AuthorizationDecisionType defaultDecision,
            @Nullable AuthorizationPhaseType phase) {
        AuthorizationDecisionType explicitDecision = securityConstraints.findItemDecision(nameOnlyItemPath, actionUrls, phase);
        return explicitDecision != null ? explicitDecision : defaultDecision;
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
                cacheRepositoryService.getObject(
                        ObjectTemplateType.class, objectTemplateRef.getOid(), createReadOnlyCollection(), result);
        return template.asObjectable();
    }

    <O extends ObjectType> void applyObjectTemplateToDefinition(
            PrismObjectDefinition<O> objectDefinition, ObjectTemplateType objectTemplate, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ConfigurationException {
        if (objectTemplate == null) {
            return;
        }
        if (!SimulationUtil.isVisible(objectTemplate, task.getExecutionMode())) {
            LOGGER.trace("Ignoring template {} as it is not visible for the current task", objectTemplate);
            return;
        }
        for (ObjectReferenceType includeRef: objectTemplate.getIncludeRef()) {
            PrismObject<ObjectTemplateType> subTemplate = cacheRepositoryService.getObject(ObjectTemplateType.class,
                    includeRef.getOid(), createReadOnlyCollection(), result);
            applyObjectTemplateToDefinition(objectDefinition, subTemplate.asObjectable(), task, result);
        }
        for (ObjectTemplateItemDefinitionType templateItemDef: objectTemplate.getItem()) {
            ItemPath itemPath = ItemRefinedDefinitionTypeUtil.getRef(templateItemDef);
            ItemDefinition<?> itemDef = objectDefinition.findItemDefinition(itemPath);
            if (itemDef != null) {
                applyObjectTemplateItem(itemDef, templateItemDef, "item " + itemPath + " in object type " + objectDefinition.getTypeName() + " as specified in item definition in " + objectTemplate);
            } else {
                OperationResult subResult = result.createMinorSubresult(SchemaTransformer.class.getName() + ".applyObjectTemplateToDefinition");
                subResult.recordPartialError("No definition for item " + itemPath + " in object type " + objectDefinition.getTypeName() + " as specified in item definition in " + objectTemplate);
            }
        }
    }

    private <O extends ObjectType> void applyObjectTemplateToObject(
            PrismObject<O> object, ObjectTemplateType objectTemplate, DefinitionUpdateOption option,
            Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ConfigurationException {
        if (objectTemplate == null) {
            return;
        }
        if (!SimulationUtil.isVisible(objectTemplate, task.getExecutionMode())) {
            LOGGER.trace("Ignoring template {} as it is not visible for the current task", objectTemplate);
            return;
        }
        for (ObjectReferenceType includeRef: objectTemplate.getIncludeRef()) {
            PrismObject<ObjectTemplateType> subTemplate = cacheRepositoryService.getObject(
                    ObjectTemplateType.class, includeRef.getOid(), createReadOnlyCollection(), result);
            applyObjectTemplateToObject(object, subTemplate.asObjectable(), option, task, result);
        }
        for (ObjectTemplateItemDefinitionType templateItemDef: objectTemplate.getItem()) {
            ItemPath itemPath = ItemRefinedDefinitionTypeUtil.getRef(templateItemDef);
            ItemDefinition<?> itemDefFromObject = object.getDefinition().findItemDefinition(itemPath);
            if (itemDefFromObject != null) {
                applyObjectTemplateItem(itemDefFromObject, templateItemDef, "item " + itemPath + " in " + object
                        + " as specified in item definition in " + objectTemplate);
            } else {
                OperationResult subResult = result.createMinorSubresult(SchemaTransformer.class.getName() + ".applyObjectTemplateToObject");
                subResult.recordPartialError("No definition for item " + itemPath + " in " + object
                        + " as specified in item definition in " + objectTemplate);
                continue;
            }
            if (option == DefinitionUpdateOption.DEEP) {
                Collection<Item<?, ?>> items = object.getAllItems(itemPath);
                for (Item<?, ?> item : items) {
                    ItemDefinition<?> itemDef = item.getDefinition();
                    if (itemDef != itemDefFromObject) {
                        applyObjectTemplateItem(itemDef, templateItemDef, "item " + itemPath + " in " + object
                                + " as specified in item definition in " + objectTemplate);
                    }
                }
            }
        }
    }

    private <ID extends ItemDefinition<?>> void applyObjectTemplateItem(
            ID itemDef, ObjectTemplateItemDefinitionType templateItemDefType, String desc) throws SchemaException {
        if (itemDef == null) {
            throw new SchemaException("No definition for "+desc);
        }

        TransformableItemDefinition<?,?> mutableDef = TransformableItemDefinition.access(itemDef);

        mutableDef.applyTemplate(templateItemDefType);

        List<PropertyLimitationsType> limitations = templateItemDefType.getLimitations();
        if (limitations != null) {
            // TODO review as part of MID-7929 resolution
            PropertyLimitationsType limitationsType = MiscSchemaUtil.getLimitationsLabeled(limitations, LayerType.PRESENTATION);
            if (limitationsType != null) {
                if (limitationsType.getMinOccurs() != null) {
                    mutableDef.setMinOccurs(XsdTypeMapper.multiplicityToInteger(limitationsType.getMinOccurs()));
                }
                if (limitationsType.getMaxOccurs() != null) {
                    mutableDef.setMaxOccurs(XsdTypeMapper.multiplicityToInteger(limitationsType.getMaxOccurs()));
                }
                if (limitationsType.getProcessing() != null) {
                    mutableDef.setProcessing(MiscSchemaUtil.toItemProcessing(limitationsType.getProcessing()));
                }
                PropertyAccessType accessType = limitationsType.getAccess();
                if (accessType != null) {
                    if (accessType.isAdd() != null) {
                        mutableDef.setCanAdd(accessType.isAdd());
                    }
                    if (accessType.isModify() != null) {
                        mutableDef.setCanModify(accessType.isModify());
                    }
                    if (accessType.isRead() != null) {
                        mutableDef.setCanRead(accessType.isRead());
                    }
                }
            }
        }

        ObjectReferenceType valueEnumerationRef = templateItemDefType.getValueEnumerationRef();
        if (valueEnumerationRef != null) {
            PrismReferenceValue valueEnumerationRVal = MiscSchemaUtil.objectReferenceTypeToReferenceValue(valueEnumerationRef, prismContext);
            mutableDef.setValueEnumerationRef(valueEnumerationRVal);
        }

        FormItemValidationType templateValidation = templateItemDefType.getValidation();
        if (templateValidation != null) {
            itemDef.setAnnotation(ItemRefinedDefinitionType.F_VALIDATION, templateValidation.clone());
        }
    }

    private record VisibilityPolicyEntry(UniformItemPath path, UserInterfaceElementVisibilityType visibility) { }

    <O extends ObjectType> void applyItemsConstraints(@NotNull PrismContainerDefinition<O> objectDefinition,
            @NotNull ArchetypePolicyType archetypePolicy) throws SchemaException {
        List<VisibilityPolicyEntry> visibilityPolicy = getVisibilityPolicy(archetypePolicy, objectDefinition);
        if (!visibilityPolicy.isEmpty()) {
            // UniformItemPath.EMPTY_PATH is null here. WHY?!?
            reduceItems(objectDefinition, prismContext.emptyPath(), visibilityPolicy);
        }
    }

    @NotNull
    private List<VisibilityPolicyEntry> getVisibilityPolicy(
            ArchetypePolicyType archetypePolicy, Object contextDesc) throws SchemaException {
        List<VisibilityPolicyEntry> visibilityPolicy = new ArrayList<>();
        for (ItemConstraintType itemConstraint: archetypePolicy.getItemConstraint()) {
            UserInterfaceElementVisibilityType visibility = itemConstraint.getVisibility();
            if (visibility != null) {
                ItemPathType itemPathType = itemConstraint.getPath();
                if (itemPathType == null) {
                    throw new SchemaException("No 'path' in item definition in archetype policy for " + contextDesc);
                }
                UniformItemPath itemPath = prismContext.toUniformPath(itemPathType);
                visibilityPolicy.add(new VisibilityPolicyEntry(itemPath, visibility));
            }
        }
        return visibilityPolicy;
    }

    @NotNull
    private UserInterfaceElementVisibilityType reduceItems(PrismContainerDefinition<?> containerDefinition,
            UniformItemPath containerPath, List<VisibilityPolicyEntry> visibilityPolicy) {
        UserInterfaceElementVisibilityType containerVisibility = determineVisibility(visibilityPolicy, containerPath);
        if (containerDefinition.isElaborate()) {
            return containerVisibility;
        }

        Collection<ItemName> itemsToDelete;
        if (containerVisibility == HIDDEN) {
            // Delete everything
            itemsToDelete = containerDefinition.getItemNames();
        } else {
            // Use item visibility to select individual items
            itemsToDelete = selectItemsToDelete(containerDefinition, containerPath, visibilityPolicy);
        }
        MutableComplexTypeDefinition mutableContainerCtDef = containerDefinition.getComplexTypeDefinition().toMutable();
        for (ItemName itemName : itemsToDelete) {
            LOGGER.trace("Removing item {}/{} due to visibility constraint", containerPath, itemName.getLocalPart());
            mutableContainerCtDef.delete(itemName);
        }
        return containerVisibility;
    }

    @NotNull
    private List<ItemName> selectItemsToDelete(PrismContainerDefinition<?> containerDefinition,
            UniformItemPath containerPath, List<VisibilityPolicyEntry> visibilityPolicy) {
        List<ItemName> itemsToDelete = new ArrayList<>();
        for (ItemDefinition<?> subDefinition : containerDefinition.getDefinitions()) {
            UniformItemPath itemPath = containerPath.append(subDefinition.getItemName());
            if (subDefinition instanceof PrismContainerDefinition<?> subContainerDef) {
                UserInterfaceElementVisibilityType itemVisibility = reduceItems(subContainerDef, itemPath, visibilityPolicy);
                if (subContainerDef.isEmpty()) {
                    /*
                     * Empty sub-containers are treated in this way:
                     * - "completely defined" ones (no xsd:any) are hidden, unless explicitly set
                     *    to VISIBLE i.e. if VACANT, HIDDEN, or AUTOMATIC
                     * - "open" ones (xsd:any) are dealt with just like properties: hidden if VACANT or HIDDEN
                     *
                     * Primary motivation for this behavior is the fact that we need to keep assignment/extension definition
                     * in the object. It is required for normal model operation, specifically for the construction of "magic
                     * assignment".
                     *
                     * Note that this somehow mixes presentation requirements (hiding/showing items) with the requirements of
                     * business logic. This is because the current solution is a temporary one, to be replaced by something
                     * more serious.
                     */
                    if (itemVisibility == VACANT || itemVisibility == HIDDEN ||
                            itemVisibility == AUTOMATIC && subContainerDef.isCompletelyDefined()) {
                        itemsToDelete.add(subDefinition.getItemName());
                    }
                }
            } else {
                UserInterfaceElementVisibilityType itemVisibility = determineVisibility(visibilityPolicy, itemPath);
                if (itemVisibility == VACANT || itemVisibility == HIDDEN) {
                    itemsToDelete.add(subDefinition.getItemName());
                }
            }
        }
        return itemsToDelete;
    }

    @NotNull
    private UserInterfaceElementVisibilityType determineVisibility(List<VisibilityPolicyEntry> visibilityPolicy, UniformItemPath itemPath) {
        if (itemPath == null || itemPath.isEmpty()) {
            return AUTOMATIC;
        }
        UserInterfaceElementVisibilityType visibility = getVisibilityPolicy(visibilityPolicy, itemPath);
        if (visibility != null) {
            return visibility;
        }
        return determineVisibility(visibilityPolicy, itemPath.allExceptLast());
    }

    private UserInterfaceElementVisibilityType getVisibilityPolicy(List<VisibilityPolicyEntry> visibilityPolicy, UniformItemPath itemPath) {
        for (VisibilityPolicyEntry entry : visibilityPolicy) {
            if (itemPath.equivalent(entry.path)) {
                return entry.visibility;
            }
        }
        return null;
    }

    @NotNull
    <O extends Objectable> TransformableObjectDefinition<O> transformableDefinition(PrismObjectDefinition<O> definition) {
        return TransformableObjectDefinition.of(definition);
    }
}
