/*
 * Copyright (c) 2014-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.controller;

import com.evolveum.midpoint.common.crypto.CryptoUtil;
import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.common.archetypes.ArchetypeManager;
import com.evolveum.midpoint.prism.path.PathSet;
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
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ItemRefinedDefinitionTypeUtil;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.SimulationUtil;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.security.enforcer.api.AuthorizationParameters;
import com.evolveum.midpoint.security.enforcer.api.ObjectSecurityConstraints;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.google.common.base.Preconditions;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.evolveum.midpoint.schema.GetOperationOptions.createReadOnlyCollection;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.UserInterfaceElementVisibilityType.*;

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
    private static final String OP_APPLY_SCHEMAS_AND_SECURITY_TO_LENS_CONTEXT = SchemaTransformer.class.getName() + ".applySchemasAndSecurityToLensContext";

    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService cacheRepositoryService;
    @Autowired private SecurityEnforcer securityEnforcer;
    @Autowired private ArchetypeManager archetypeManager;
    @Autowired private PrismContext prismContext;

    /**
     * Applies security to a {@link SearchResultList} of prism objects.
     *
     * Assumes some items may be immutable, and does lazy object or object list cloning:
     *
     * - Some objects may be mutated or cloned-and-mutated (if the object was immutable).
     * - This may result in modifying the original `objects` list, or returning a its modified copy (if the list was immutable).
     */
    <T extends ObjectType> SearchResultList<PrismObject<T>> applySchemasAndSecurityToObjects(
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

    // Expecting that C is a direct child of T.
    // Expecting that container values point to their respective parents (in order to evaluate the security!)
    <C extends Containerable, T extends ObjectType>
    SearchResultList<C> applySchemasAndSecurityToContainers(
            SearchResultList<C> originalResultList,
            Class<T> parentObjectType,
            ItemName childItemName,
            ParsedGetOperationOptions parsedOptions,
            Task task,
            OperationResult result)
            throws SecurityViolationException, SchemaException, ObjectNotFoundException, ConfigurationException,
            ExpressionEvaluationException, CommunicationException {

        List<C> newValues = new ArrayList<>();
        Map<PrismObject<T>,Object> processedParents = new IdentityHashMap<>();
        for (C value: originalResultList) {
            Long originalId = value.asPrismContainerValue().getId();
            if (originalId == null) {
                throw new SchemaException("No ID in container value " + value);
            }
            PrismObject<T> parent = ObjectTypeUtil.getParentObjectOld(value);
            boolean wasProcessed;
            if (parent != null) {
                wasProcessed = processedParents.containsKey(parent);
            } else {
                // temporary solution TODO reconsider
                parent = prismContext.createObject(parentObjectType);
                PrismContainer<C> childContainer = parent.findOrCreateItem(childItemName, PrismContainer.class);
                childContainer.add(value.asPrismContainerValue());
                wasProcessed = false;
            }
            if (!wasProcessed) {
                parent = applySchemasAndSecurityToObject(parent, parsedOptions, task, result);
                processedParents.put(parent, null);
            }
            PrismContainer<C> updatedChildContainer = parent.findContainer(childItemName);
            if (updatedChildContainer != null) {
                PrismContainerValue<C> updatedChildValue = updatedChildContainer.getValue(originalId);
                if (updatedChildValue != null) {
                    newValues.add(updatedChildValue.asContainerable());
                }
            }
        }
        return new SearchResultList<>(newValues, originalResultList.getMetadata());
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
            @NotNull OperationResult parentResult)
            throws SchemaException, SecurityViolationException, ConfigurationException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException {

        LOGGER.trace("applySchemasAndSecurityToObject({}) starting", object);
        var rootOptions = parsedOptions.getRootOptions();
        OperationResult result = parentResult.createMinorSubresult(OP_APPLY_SCHEMAS_AND_SECURITY_TO_OBJECT);
        try {
            authorizeRawOption(object, rootOptions, task, result);
            validateObject(object, rootOptions);

            ObjectSecurityConstraints securityConstraints = compileSecurityConstraints(object, task, result);

            object = object.cloneIfImmutable(); // TODO clone only if really needed

            if (!GetOperationOptions.isExecutionPhase(rootOptions)) {
                applySchemasAndSecurityPhase(object, securityConstraints, AuthorizationPhaseType.REQUEST);
            }
            applySchemasAndSecurityPhase(object, securityConstraints, AuthorizationPhaseType.EXECUTION);

            // TODO is this needed?
            transform(object, new DefinitionsToTransformable());

            // we do not need to process object template when processing in raw mode
            // TODO cache the definitions
            if (!GetOperationOptions.isRaw(rootOptions)) {
                ObjectTemplateType objectTemplate = determineObjectTemplate(object, result);
                applyObjectTemplateToObject(object, objectTemplate, task, result);
            }

            // TODO consider removing this as it shouldn't be needed after definitions caching is implemented
            applyDefinitionProcessingOption(object, parsedOptions);
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
        LOGGER.trace("applySchemasAndSecurityToObject finishing");
        return object;
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

    <O extends ObjectType> void applySchemasAndSecurityToLensContext(
            @NotNull LensContext<O> context, Task task, OperationResult parentResult)
            throws SecurityViolationException, SchemaException, ConfigurationException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException {
        LOGGER.trace("applySchemasAndSecurityToLensContext({}) starting", context);
        OperationResult result = parentResult.createMinorSubresult(OP_APPLY_SCHEMAS_AND_SECURITY_TO_LENS_CONTEXT);
        try {
            applySchemasAndSecurityToFocusContext(context, task, result);
            applySchemasAndSecurityToProjectionsContexts(context, task, result);
        } catch (Throwable e) {
            result.recordException(e);
            throw e;
        } finally {
            result.close();
        }
        LOGGER.trace("applySchemasAndSecurity finishing"); // to allow folding in log viewer
    }

    private <O extends ObjectType> void applySchemasAndSecurityToFocusContext(
            LensContext<O> context, Task task, OperationResult result)
            throws SecurityViolationException, SchemaException, ConfigurationException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException {
        LensFocusContext<O> focusContext = context.getFocusContext();
        PrismObject<O> object = focusContext != null ? focusContext.getObjectAny() : null;
        if (object == null) {
            return;
        }

        ObjectSecurityConstraints securityConstraints = applySchemasAndSecurityToElementContext(focusContext, task, result);
        AuthorizationDecisionType assignmentDecision =
                securityConstraints.findItemDecision(
                        SchemaConstants.PATH_ASSIGNMENT, ModelAuthorizationAction.AUTZ_ACTIONS_URLS_GET, null);
        if (assignmentDecision != AuthorizationDecisionType.ALLOW) {
            LOGGER.trace("Logged in user isn't authorized to read (or get) assignment item of the object: {}", object);
            result.recordWarning("Logged in user isn't authorized to read (or get) assignment item of the object: " + object);
            context.setEvaluatedAssignmentTriple(null);
        }
    }

    private <O extends ObjectType> void applySchemasAndSecurityToProjectionsContexts(
            LensContext<O> context, Task task, OperationResult result)
            throws SecurityViolationException, SchemaException, ConfigurationException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException {
        for (LensProjectionContext projCtx : context.getProjectionContexts()) {
            if (projCtx != null && projCtx.getObjectAny() != null) {
                applySchemasAndSecurityToElementContext(projCtx, task, result);
            }
        }
    }

    private <O extends ObjectType> ObjectSecurityConstraints applySchemasAndSecurityToElementContext(
            LensElementContext<O> elementContext, Task task, OperationResult result)
            throws SecurityViolationException, SchemaException, ConfigurationException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException {

        PrismObject<O> object = elementContext.getObjectAny();
        assert object != null;

        GetOperationOptions getOptions = ModelExecuteOptions.toGetOperationOptions(elementContext.getLensContext().getOptions());
        authorizeRawOption(object, getOptions, task, result);

        ObjectSecurityConstraints securityConstraints = compileSecurityConstraints(object, task, result);

        AuthorizationDecisionType globalReadDecision =
                securityConstraints.findAllItemsDecision(ModelAuthorizationAction.AUTZ_ACTIONS_URLS_GET, null);
        if (globalReadDecision == AuthorizationDecisionType.DENY) {
            // shortcut
            SecurityUtil.logSecurityDeny(object, "because the authorization denies access");
            throw new AuthorizationException("Access denied");
        }

        AuthorizationDecisionType globalAddDecision =
                securityConstraints.findAllItemsDecision(ModelAuthorizationAction.ADD.getUrl(), null);
        AuthorizationDecisionType globalModifyDecision =
                securityConstraints.findAllItemsDecision(ModelAuthorizationAction.MODIFY.getUrl(), null);

        elementContext.forEachObject(focusObject ->
            applySecurityConstraintsToPcv(focusObject.getValue(), securityConstraints, null,
                    globalReadDecision, globalAddDecision, globalModifyDecision));

        elementContext.forEachDelta(focusDelta ->
            applySecurityConstraintsToDelta(focusDelta, securityConstraints,
                    globalReadDecision, globalAddDecision, globalModifyDecision));

        return securityConstraints;
    }

    private <O extends ObjectType> void applySchemasAndSecurityPhase(
            PrismObject<O> object, ObjectSecurityConstraints securityConstraints, AuthorizationPhaseType phase)
            throws SecurityViolationException {
        Validate.notNull(phase);

        AuthorizationDecisionType globalReadDecision =
                securityConstraints.findAllItemsDecision(ModelAuthorizationAction.AUTZ_ACTIONS_URLS_GET, phase);
        if (globalReadDecision == AuthorizationDecisionType.DENY) {
            // shortcut
            SecurityUtil.logSecurityDeny(object, "because the authorization denies access");
            throw new AuthorizationException("Access denied");
        }

        AuthorizationDecisionType globalAddDecision =
                securityConstraints.findAllItemsDecision(ModelAuthorizationAction.ADD.getUrl(), phase);
        AuthorizationDecisionType globalModifyDecision =
                securityConstraints.findAllItemsDecision(ModelAuthorizationAction.MODIFY.getUrl(), phase);

        applySecurityConstraintsToPcv(
                object.getValue(), securityConstraints, phase,
                globalReadDecision, globalAddDecision, globalModifyDecision);

        if (object.isEmpty()) {
            // let's make it explicit
            SecurityUtil.logSecurityDeny(object, "because the subject has not access to any item");
            throw new AuthorizationException("Access denied");
        }
    }

    private <O extends ObjectType> @NotNull ObjectSecurityConstraints compileSecurityConstraints(
            PrismObject<O> object, Task task, OperationResult result)
            throws SecurityViolationException, SchemaException, ConfigurationException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException {
        ObjectSecurityConstraints securityConstraints =
                securityEnforcer.compileSecurityConstraints(object, null, task, result);
        LOGGER.trace("Security constraints for {}:\n{}", object, DebugUtil.debugDumpLazily(securityConstraints));
        if (securityConstraints == null) {
            SecurityUtil.logSecurityDeny(object, "because no security constraints are defined (default deny)");
            throw new AuthorizationException("Access denied");
        }
        return securityConstraints;
    }

    private void applySecurityConstraintsToPcv(
            @NotNull PrismContainerValue<?> pcv, ObjectSecurityConstraints securityConstraints, AuthorizationPhaseType phase,
            AuthorizationDecisionType defaultReadDecision, AuthorizationDecisionType defaultAddDecision,
            AuthorizationDecisionType defaultModifyDecision) {
        Collection<Item<?, ?>> items = pcv.getItems();
        LOGGER.trace("applySecurityConstraintsToPcv: items={}, phase={}, defaults R={}, A={}, M={}",
                items, phase, defaultReadDecision, defaultAddDecision, defaultModifyDecision);
        if (items.isEmpty()) {
            return;
        }
        List<Item<?, ?>> itemsToRemove = new ArrayList<>();
        for (Item<?, ?> item : items) {
            ItemPath itemPath = item.getPath();
            ItemDefinition<?> itemDef = item.getDefinition();
            ItemPath nameOnlyItemPath = itemPath.namedSegmentsOnly();
            AuthorizationDecisionType itemReadDecision = computeItemDecision(securityConstraints, nameOnlyItemPath, ModelAuthorizationAction.AUTZ_ACTIONS_URLS_GET, defaultReadDecision, phase);
            AuthorizationDecisionType itemAddDecision = computeItemDecision(securityConstraints, nameOnlyItemPath, ModelAuthorizationAction.AUTZ_ACTIONS_URLS_ADD, defaultReadDecision, phase);
            AuthorizationDecisionType itemModifyDecision = computeItemDecision(securityConstraints, nameOnlyItemPath, ModelAuthorizationAction.AUTZ_ACTIONS_URLS_MODIFY, defaultReadDecision, phase);
            LOGGER.trace("applySecurityConstraints(item): {}: decisions R={}, A={}, M={}",
                    itemPath, itemReadDecision, itemAddDecision, itemModifyDecision);
            if (item instanceof PrismContainer<?>) {
                if (itemReadDecision == AuthorizationDecisionType.DENY) {
                    // Explicitly denied access to the entire container
                    itemsToRemove.add(item);
                } else if (itemDef != null && itemDef.isElaborate()) {
                    LOGGER.trace("applySecurityConstraints(item): {}: skipping deeper dive, applying decision '{}' (elaborate)",
                            itemPath, itemReadDecision);
                    if (itemReadDecision == null) {
                        // "default" (null) means we do not analyze things further, and simply deny the access completely
                        itemsToRemove.add(item);
                    } else {
                        // ALLOW means we do not analyze things further, and simply allow the access completely
                    }
                } else {
                    // No explicit decision (even ALLOW is not final here as something may be denied deeper inside)
                    AuthorizationDecisionType subDefaultReadDecision = defaultReadDecision;
                    if (itemReadDecision == AuthorizationDecisionType.ALLOW) {
                        // This means allow to all subitems unless otherwise denied.
                        subDefaultReadDecision = AuthorizationDecisionType.ALLOW;
                    }
                    List<? extends PrismContainerValue<?>> values = ((PrismContainer<?>)item).getValues();
                    reduceContainerValues(values, securityConstraints, phase, itemReadDecision, itemAddDecision, itemModifyDecision, subDefaultReadDecision);
                    if (item.hasNoValues() && itemReadDecision == null) {
                        // We have removed all the content, if there was any. So, in the default case, there's nothing that
                        // we are interested in inside this item. Therefore let's just remove it.
                        // (If itemReadDecision is ALLOW, we obviously keep this untouched.)
                        itemsToRemove.add(item);
                    }
                }
            } else {
                if (itemReadDecision == AuthorizationDecisionType.DENY || itemReadDecision == null) {
                    itemsToRemove.add(item);
                }
            }
        }
        for (Item<?, ?> itemToRemove : itemsToRemove) {
            pcv.remove(itemToRemove);
        }
    }

    void applySecurityConstraintsToInnerDefinitions(
            @NotNull PrismContainerValue<?> pcv, ObjectSecurityConstraints securityConstraints, AuthorizationPhaseType phase) {
        if (phase == null) {
            applySecurityConstraintsToInnerDefinitionsPhase(pcv, securityConstraints, AuthorizationPhaseType.REQUEST, null);
            applySecurityConstraintsToInnerDefinitionsPhase(pcv, securityConstraints, AuthorizationPhaseType.EXECUTION, null);
        } else {
            applySecurityConstraintsToInnerDefinitionsPhase(pcv, securityConstraints, phase, null);
        }
    }

    private void applySecurityConstraintsToInnerDefinitionsPhase(
            @NotNull PrismContainerValue<?> pcv, ObjectSecurityConstraints securityConstraints, AuthorizationPhaseType phase,
            AuthorizationDecisionType defaultReadDecision) {
        for (Item<?, ?> item : pcv.getItems()) {
            ItemPath itemPath = item.getPath();
            ItemPath nameOnlyItemPath = itemPath.namedSegmentsOnly();
            AuthorizationDecisionType itemReadDecision = computeItemDecision(securityConstraints, nameOnlyItemPath, ModelAuthorizationAction.AUTZ_ACTIONS_URLS_GET, defaultReadDecision, phase);
            AuthorizationDecisionType itemAddDecision = computeItemDecision(securityConstraints, nameOnlyItemPath, ModelAuthorizationAction.AUTZ_ACTIONS_URLS_ADD, defaultReadDecision, phase);
            AuthorizationDecisionType itemModifyDecision = computeItemDecision(securityConstraints, nameOnlyItemPath, ModelAuthorizationAction.AUTZ_ACTIONS_URLS_MODIFY, defaultReadDecision, phase);
            ItemDefinition<?> itemDef = item.getDefinition();
            if (itemDef != null) {
                if (itemReadDecision != AuthorizationDecisionType.ALLOW) {
                    mutable(itemDef).setCanRead(false);
                }
                if (itemAddDecision != AuthorizationDecisionType.ALLOW) {
                    mutable(itemDef).setCanAdd(false);
                }
                if (itemModifyDecision != AuthorizationDecisionType.ALLOW) {
                    mutable(itemDef).setCanModify(false);
                }
            }
            if (item instanceof PrismContainer<?>) {
                for (PrismContainerValue<?> value : ((PrismContainer<?>) item).getValues()) {
                    // No explicit decision (even ALLOW is not final here as something may be denied deeper inside)
                    AuthorizationDecisionType subDefaultReadDecision;
                    if (itemReadDecision == AuthorizationDecisionType.ALLOW) {
                        // This means allow to all subitems unless otherwise denied.
                        subDefaultReadDecision = AuthorizationDecisionType.ALLOW;
                    } else {
                        subDefaultReadDecision = defaultReadDecision;
                    }
                    applySecurityConstraintsToInnerDefinitionsPhase(value, securityConstraints, phase, subDefaultReadDecision);
                }
            }
        }
    }

    private MutableItemDefinition<?> mutable(ItemDefinition<?> itemDef) {
        return itemDef.toMutable();
    }

    private <O extends ObjectType> void applySecurityConstraintsToDelta(
            ObjectDelta<O> objectDelta, ObjectSecurityConstraints securityConstraints,
            AuthorizationDecisionType defaultReadDecision, AuthorizationDecisionType defaultAddDecision,
            AuthorizationDecisionType defaultModifyDecision) {
        LOGGER.trace("applySecurityConstraintsToDelta: items={}, phase={}, defaults R={}, A={}, M={}",
                objectDelta, null, defaultReadDecision, defaultAddDecision, defaultModifyDecision);
        if (objectDelta == null) {
            return;
        }
        if (objectDelta.isAdd()) {
            applySecurityConstraintsToPcv(
                    objectDelta.getObjectToAdd().getValue(), securityConstraints, null, defaultReadDecision,
                    defaultAddDecision, defaultModifyDecision);
            return;
        }
        if (objectDelta.isDelete()) {
            // Nothing to do
            return;
        }
        // Modify delta
        Collection<? extends ItemDelta<?,?>> modifications = objectDelta.getModifications();
        if (modifications.isEmpty()) {
            // Nothing to do
            return;
        }
        List<ItemDelta<?, ?>> itemsToRemove = new ArrayList<>();
        for (ItemDelta<?, ?> modification : modifications) {
            ItemPath itemPath = modification.getPath();
            ItemDefinition<?> itemDef = modification.getDefinition();
            ItemPath nameOnlyItemPath = itemPath.namedSegmentsOnly();
            AuthorizationDecisionType itemReadDecision = computeItemDecision(securityConstraints, nameOnlyItemPath, ModelAuthorizationAction.AUTZ_ACTIONS_URLS_GET, defaultReadDecision, null);
            AuthorizationDecisionType itemAddDecision = computeItemDecision(securityConstraints, nameOnlyItemPath, ModelAuthorizationAction.AUTZ_ACTIONS_URLS_ADD, defaultReadDecision, null);
            AuthorizationDecisionType itemModifyDecision = computeItemDecision(securityConstraints, nameOnlyItemPath, ModelAuthorizationAction.AUTZ_ACTIONS_URLS_MODIFY, defaultReadDecision, null);
            LOGGER.trace("applySecurityConstraints(item): {}: decisions R={}, A={}, M={}",
                    itemPath, itemReadDecision, itemAddDecision, itemModifyDecision);
            if (modification instanceof ContainerDelta<?>) {
                if (itemReadDecision == AuthorizationDecisionType.DENY) {
                    // Explicitly denied access to the entire container
                    itemsToRemove.add(modification);
                } else if (itemDef != null && itemDef.isElaborate()) {
                    LOGGER.trace("applySecurityConstraints(item): {}: skipping deeper dive, applying decision '{}' (elaborate)",
                            itemPath, itemReadDecision);
                    // See comments in the "PCV" version of this method (above)
                    if (itemReadDecision == null) {
                        itemsToRemove.add(modification);
                    }
                } else {
                    // No explicit decision (even ALLOW is not final here as something may be denied deeper inside)
                    AuthorizationDecisionType subDefaultReadDecision = defaultReadDecision;
                    if (itemReadDecision == AuthorizationDecisionType.ALLOW) {
                        // This means allow to all subitems unless otherwise denied.
                        subDefaultReadDecision = AuthorizationDecisionType.ALLOW;
                    }

                    reduceContainerValues((List<? extends PrismContainerValue<?>>) ((ContainerDelta<?>)modification).getValuesToAdd(),
                            securityConstraints, null, itemReadDecision, itemAddDecision, itemModifyDecision, subDefaultReadDecision);
                    reduceContainerValues((List<? extends PrismContainerValue<?>>) ((ContainerDelta<?>)modification).getValuesToDelete(),
                            securityConstraints, null, itemReadDecision, itemAddDecision, itemModifyDecision, subDefaultReadDecision);
                    reduceContainerValues((List<? extends PrismContainerValue<?>>) ((ContainerDelta<?>)modification).getValuesToReplace(),
                            securityConstraints, null, itemReadDecision, itemAddDecision, itemModifyDecision, subDefaultReadDecision);

                    if (modification.isEmpty() && itemReadDecision == null) {
                        // We have removed all the content, if there was any. So, in the default case, there's nothing that
                        // we are interested in inside this item. Therefore let's just remove it.
                        // (If itemReadDecision is ALLOW, we obviously keep this untouched.)
                        itemsToRemove.add(modification);
                    }
                }
            } else {
                if (itemReadDecision == AuthorizationDecisionType.DENY || itemReadDecision == null) {
                    itemsToRemove.add(modification);
                    break;
                }
            }
        }
        for (ItemDelta<?, ?> modificationToRemove : itemsToRemove) {
            modifications.remove(modificationToRemove);
        }
    }

    private void reduceContainerValues(
            List<? extends PrismContainerValue<?>> values, ObjectSecurityConstraints securityConstraints,
            AuthorizationPhaseType phase,
            AuthorizationDecisionType itemReadDecision, AuthorizationDecisionType itemAddDecision,
            AuthorizationDecisionType itemModifyDecision, AuthorizationDecisionType subDefaultReadDecision) {
        if (values == null) {
            return;
        }
        Iterator<? extends PrismContainerValue<?>> vi = values.iterator();
        while (vi.hasNext()) {
            PrismContainerValue<?> cval = vi.next();
            applySecurityConstraintsToPcv(cval, securityConstraints, phase,
                    subDefaultReadDecision, itemAddDecision, itemModifyDecision);
            if (cval.hasNoItems() && itemReadDecision == null) {
                // We have removed all the content, if there was any. So, in the default case, there's nothing that
                // we are interested in inside this PCV. Therefore let's just remove it.
                // (If itemReadDecision is ALLOW, we obviously keep this untouched.)
                vi.remove();
            }
        }
    }

    <D extends ItemDefinition<?>> void applySecurityConstraintsToItemDef(
            D itemDefinition, ObjectSecurityConstraints securityConstraints, AuthorizationPhaseType phase) {
        if (phase == null) {
            applySecurityConstraintsToItemDefPhase(itemDefinition, securityConstraints, AuthorizationPhaseType.REQUEST);
            applySecurityConstraintsToItemDefPhase(itemDefinition, securityConstraints, AuthorizationPhaseType.EXECUTION);
        } else {
            applySecurityConstraintsToItemDefPhase(itemDefinition, securityConstraints, phase);
        }
    }

    private <D extends ItemDefinition<?>> void applySecurityConstraintsToItemDefPhase(
            D itemDefinition, ObjectSecurityConstraints securityConstraints, AuthorizationPhaseType phase) {
        Validate.notNull(phase);
        LOGGER.trace("applySecurityConstraints(itemDefs): def={}, phase={}", itemDefinition, phase);
        applySecurityConstraintsToItemDef(
                itemDefinition, new IdentityHashMap<>(), ItemPath.EMPTY_PATH, securityConstraints,
                null, null, null, phase);

    }

    private <D extends ItemDefinition<?>> void applySecurityConstraintsToItemDef(
            D itemDefinition,
            IdentityHashMap<ItemDefinition<?>, Object> definitionsSeen,
            ItemPath nameOnlyItemPath, ObjectSecurityConstraints securityConstraints, AuthorizationDecisionType defaultReadDecision,
            AuthorizationDecisionType defaultAddDecision, AuthorizationDecisionType defaultModifyDecision,
            AuthorizationPhaseType phase) {

        boolean thisWasSeen = definitionsSeen.containsKey(itemDefinition);
        definitionsSeen.put(itemDefinition, null);

        AuthorizationDecisionType readDecision = computeItemDecision(securityConstraints, nameOnlyItemPath, ModelAuthorizationAction.AUTZ_ACTIONS_URLS_GET, defaultReadDecision, phase);
        AuthorizationDecisionType addDecision = computeItemDecision(securityConstraints, nameOnlyItemPath, ModelAuthorizationAction.AUTZ_ACTIONS_URLS_ADD, defaultAddDecision, phase);
        AuthorizationDecisionType modifyDecision = computeItemDecision(securityConstraints, nameOnlyItemPath, ModelAuthorizationAction.AUTZ_ACTIONS_URLS_MODIFY, defaultModifyDecision, phase);

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

        LOGGER.trace("applySecurityConstraintsToItemDef: {}: decisions R={}, A={}, M={}; subelements R={}, A={}, M={}",
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
            ObjectSecurityConstraints securityConstraints, ItemPath nameOnlyItemPath, String[] actionUrls,
            AuthorizationDecisionType defaultDecision, AuthorizationPhaseType phase) {
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
            PrismObject<O> object, ObjectTemplateType objectTemplate, Task task, OperationResult result)
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
            applyObjectTemplateToObject(object, subTemplate.asObjectable(), task, result);
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
            Collection<Item<?, ?>> items = object.getAllItems(itemPath);
            for (Item<?, ?> item : items) {
                ItemDefinition<?> itemDef = item.getDefinition();
                if (itemDef != itemDefFromObject) {
                    applyObjectTemplateItem(itemDef, templateItemDef, "item "+itemPath+" in " + object
                            + " as specified in item definition in "+objectTemplate);
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

    private static class VisibilityPolicyEntry {
        private final UniformItemPath path;
        private final UserInterfaceElementVisibilityType visibility;

        private VisibilityPolicyEntry(UniformItemPath path, UserInterfaceElementVisibilityType visibility) {
            this.path = path;
            this.visibility = visibility;
        }
    }

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
            if (subDefinition instanceof PrismContainerDefinition) {
                PrismContainerDefinition<?> subContainerDef = (PrismContainerDefinition<?>) subDefinition;
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
