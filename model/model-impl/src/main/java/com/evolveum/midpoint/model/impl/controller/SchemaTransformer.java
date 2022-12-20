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
import com.evolveum.midpoint.model.common.archetypes.ArchetypeManager;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
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
import com.evolveum.midpoint.schema.DefinitionProcessingOption;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.SimulationUtil;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.security.enforcer.api.AuthorizationParameters;
import com.evolveum.midpoint.security.enforcer.api.ObjectSecurityConstraints;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.google.common.base.Preconditions;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
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

    private static final String OP_APPLY_SCHEMAS_AND_SECURITY = SchemaTransformer.class.getName() + ".applySchemasAndSecurity";
    private static final String OP_APPLY_SCHEMAS_AND_SECURITY_TO_OBJECT = SchemaTransformer.class.getName() + ".applySchemasAndSecurityToObject";

    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService cacheRepositoryService;
    @Autowired private SecurityEnforcer securityEnforcer;
    @Autowired private SystemObjectCache systemObjectCache;
    @Autowired private ArchetypeManager archetypeManager;

    @Autowired
    private PrismContext prismContext;

    // TODO why are the following two methods distinct? Clarify their names.
    public <T extends ObjectType> void applySchemasAndSecurityToObjectTypes(List<T> objectTypes,
            GetOperationOptions rootOptions, Collection<SelectorOptions<GetOperationOptions>> options,
            AuthorizationPhaseType phase, Task task, OperationResult result)
                    throws SecurityViolationException, SchemaException, ConfigurationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException {
        for (int i = 0; i < objectTypes.size(); i++) {
            PrismObject<T> object = (PrismObject<T>) objectTypes.get(i).asPrismObject();
            object = object.cloneIfImmutable();
            objectTypes.set(i, object.asObjectable());
            applySchemasAndSecurity(object, rootOptions, options, phase, task, result);
        }
    }

    <T extends ObjectType> void applySchemasAndSecurityToObjects(SearchResultList<PrismObject<T>> objects,
            GetOperationOptions rootOptions, Collection<SelectorOptions<GetOperationOptions>> options,
            AuthorizationPhaseType phase, Task task, OperationResult result)
                    throws SecurityViolationException {
        assert !objects.isImmutable();
        for (int i = 0; i < objects.size(); i++) {
            PrismObject<T> object = objects.get(i);
            if (object.isImmutable()) {
                object = object.clone();
                objects.set(i, object);
            }
            applySchemaAndSecurityToObject(object, rootOptions, options, phase, task, result);
        }
    }

    // Expecting that C is a direct child of T.
    // Expecting that container values point to their respective parents (in order to evaluate the security!)
    public <C extends Containerable, T extends ObjectType>
    SearchResultList<C> applySchemasAndSecurityToContainers(SearchResultList<C> originalResultList, Class<T> parentObjectType, ItemName childItemName,
            GetOperationOptions rootOptions, Collection<SelectorOptions<GetOperationOptions>> options, AuthorizationPhaseType phase, Task task, OperationResult result)
            throws SecurityViolationException, SchemaException, ObjectNotFoundException, ConfigurationException, ExpressionEvaluationException, CommunicationException {

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
                // TODO what if parent is immutable?
                applySchemasAndSecurity(parent, rootOptions, options, phase, task, result);
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

    private <T extends ObjectType> void applySchemaAndSecurityToObject(PrismObject<T> object, GetOperationOptions rootOptions,
            Collection<SelectorOptions<GetOperationOptions>> options, AuthorizationPhaseType phase, Task task, OperationResult result) throws SecurityViolationException {
        OperationResult subresult = result.createMinorSubresult(OP_APPLY_SCHEMAS_AND_SECURITY_TO_OBJECT);
        try {
            applySchemasAndSecurity(object, rootOptions, options, phase, task, subresult);
            subresult.computeStatus();
        } catch (IllegalArgumentException | IllegalStateException | SchemaException |ConfigurationException |ObjectNotFoundException | ExpressionEvaluationException | CommunicationException e) {
            LOGGER.error("Error post-processing object {}: {}", object, e.getMessage(), e);
            OperationResultType fetchResult = object.asObjectable().getFetchResult();
            if (fetchResult == null) {
                fetchResult = subresult.createBeanReduced();
                object.asObjectable().setFetchResult(fetchResult);
            } else {
                fetchResult.getPartialResults().add(subresult.createBeanReduced());
            }
            fetchResult.setStatus(OperationResultStatusType.FATAL_ERROR);
            subresult.recordFatalError(e); // todo is it safe to keep the object in the result set if it was not completely post-processed?
        } catch (SecurityViolationException e) {
            // We cannot go on and leave this object in the result set. The object was not post-processed.
            // Leaving it in the result set may leak information.
            subresult.recordFatalError(e);
            throw e;
        }
    }

    private <O extends ObjectType> void authorizeOptions(GetOperationOptions rootOptions, PrismObject<O> object, ObjectDelta<O> delta, AuthorizationPhaseType phase, Task task, OperationResult result)
            throws SchemaException, SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
        if (GetOperationOptions.isRaw(rootOptions)) {
            securityEnforcer.authorize(ModelAuthorizationAction.RAW_OPERATION.getUrl(), phase, AuthorizationParameters.Builder.buildObjectDelta(object, delta), null, task, result);
        }
    }

    /**
     * Validate the objects, apply security to the object definition, remove any non-visible properties (security),
     * apply object template definitions and so on. This method is called for
     * any object that is returned from the Model Service.
     */
    <O extends ObjectType> void applySchemasAndSecurity(PrismObject<O> object, GetOperationOptions rootOptions,
            Collection<SelectorOptions<GetOperationOptions>> options,
            AuthorizationPhaseType phase, Task task, OperationResult parentResult)
                    throws SchemaException, SecurityViolationException, ConfigurationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException {
        LOGGER.trace("applySchemasAndSecurity({}) starting", object);
        OperationResult result = parentResult.createMinorSubresult(OP_APPLY_SCHEMAS_AND_SECURITY);
        authorizeOptions(rootOptions, object, null, phase, task, result);
        validateObject(object, rootOptions, result);

        ObjectSecurityConstraints securityConstraints = compileSecurityConstraints(object, task, result);

        transform(object, new DefinitionsToTransformable());
        PrismObjectDefinition<O> objectDefinition = object.getDefinition();

        if (phase == null) {
            if (!GetOperationOptions.isExecutionPhase(rootOptions)) {
                applySchemasAndSecurityPhase(object, securityConstraints, objectDefinition, rootOptions, AuthorizationPhaseType.REQUEST, task, result);
            }
            applySchemasAndSecurityPhase(object, securityConstraints, objectDefinition, rootOptions, AuthorizationPhaseType.EXECUTION, task, result);
        } else {
            if (phase == AuthorizationPhaseType.REQUEST && GetOperationOptions.isExecutionPhase(rootOptions)) {
                // Skip application of security constraints for request phase.
                // The caller asked to skip evaluation of request authorization, so everything is allowed here.
            } else {
                applySchemasAndSecurityPhase(object, securityConstraints, objectDefinition, rootOptions, phase, task, result);
            }
        }

        // we do not need to process object template when processing REQUEST in RAW mode.
        if (!GetOperationOptions.isRaw(rootOptions)) {
            ObjectTemplateType objectTemplateType;
            try {
                objectTemplateType = determineObjectTemplate(object, AuthorizationPhaseType.REQUEST, result);
            } catch (ConfigurationException | SchemaException | ObjectNotFoundException e) {
                result.recordFatalError(e);
                throw e;
            }
            applyObjectTemplateToObject(object, objectTemplateType, task, result);
        }

        if (CollectionUtils.isNotEmpty(options)) {
            Map<DefinitionProcessingOption, Collection<UniformItemPath>> definitionProcessing = SelectorOptions.extractOptionValues(options, (o) -> o.getDefinitionProcessing(), prismContext);
            if (CollectionUtils.isNotEmpty(definitionProcessing.get(DefinitionProcessingOption.NONE))) {
                throw new UnsupportedOperationException("'NONE' definition processing is not supported now");
            }
            Collection<UniformItemPath> onlyIfExists = definitionProcessing.get(DefinitionProcessingOption.ONLY_IF_EXISTS);
            if (CollectionUtils.isNotEmpty(onlyIfExists)) {
                if (onlyIfExists.size() != 1 || !ItemPath.isEmpty(onlyIfExists.iterator().next())) {
                    throw new UnsupportedOperationException("'ONLY_IF_EXISTS' definition processing is currently supported on root level only; not on " + onlyIfExists);
                }
                Collection<UniformItemPath> full = definitionProcessing.get(DefinitionProcessingOption.FULL);
                object.trimDefinitionTree(full);
            }
        }

        result.computeStatus();
        result.recordSuccessIfUnknown();
        LOGGER.trace("applySchemasAndSecurity finishing");
    }

    private void transform(Item<?,?> object, ItemDefinitionTransformer transformation) {
        Preconditions.checkArgument(object instanceof TransformableItem, "Value must be %s", TransformableValue.class.getSimpleName());
        ((TransformableItem) object).transformDefinition(null, transformation);
    }

    <O extends ObjectType> void applySchemasAndSecurity(LensContext<O> context,
            AuthorizationPhaseType phase, Task task, OperationResult parentResult) throws SecurityViolationException, SchemaException, ConfigurationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException {
        LOGGER.trace("applySchemasAndSecurity({}) starting", context);
        OperationResult result = parentResult.createMinorSubresult(OP_APPLY_SCHEMAS_AND_SECURITY);

        try {
            applySchemasAndSecurityFocus(context, phase, task, result);
            applySchemasAndSecurityProjections(context, phase, task, result);

            result.computeStatus();
            result.recordSuccessIfUnknown();

        } catch (Throwable e) {
            result.recordFatalError(e);
            throw e;
        }

        LOGGER.trace("applySchemasAndSecurity finishing");            // to allow folding in log viewer
    }

    private <O extends ObjectType> void applySchemasAndSecurityFocus(LensContext<O> context,
            AuthorizationPhaseType phase, Task task, OperationResult result) throws SecurityViolationException, SchemaException, ConfigurationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException {
        LensFocusContext<O> focusContext = context.getFocusContext();
        if (focusContext == null) {
            return;
        }

        ObjectSecurityConstraints securityConstraints = applySchemasAndSecurityElementContext(context, focusContext, phase, task, result);

        AuthorizationDecisionType assignmentDecision = securityConstraints.findItemDecision(SchemaConstants.PATH_ASSIGNMENT, ModelAuthorizationAction.AUTZ_ACTIONS_URLS_GET, phase);
        if (!AuthorizationDecisionType.ALLOW.equals(assignmentDecision)) {
            PrismObject<O> object = focusContext.getObjectAny();
            LOGGER.trace("Logged in user isn't authorized to read (or get) assignment item of the object: {}", object);
            result.recordWarning("Logged in user isn't authorized to read (or get) assignment item of the object: " + object);
            context.setEvaluatedAssignmentTriple(null);
        }
    }

    private <O extends ObjectType> void applySchemasAndSecurityProjections(LensContext<O> context,
            AuthorizationPhaseType phase, Task task, OperationResult result) throws SecurityViolationException, SchemaException, ConfigurationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException {
        for (LensProjectionContext projCtx : context.getProjectionContexts()) {
            if (projCtx != null && projCtx.getObjectAny() != null) {
                applySchemasAndSecurityElementContext(context, projCtx, phase, task, result);
            }
        }
    }

    private <F extends ObjectType, O extends ObjectType> ObjectSecurityConstraints applySchemasAndSecurityElementContext(LensContext<F> context, LensElementContext<O> elementContext,
            AuthorizationPhaseType phase, Task task, OperationResult result) throws SecurityViolationException, SchemaException, ConfigurationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException {

        PrismObject<O> object = elementContext.getObjectAny();

        if (object == null) {
            if (elementContext.getSummaryDelta() == null) { // TODO check this
                return null;
            } else {
                throw new IllegalArgumentException("Cannot apply schema and security of null object");
            }
        }
        GetOperationOptions getOptions = ModelExecuteOptions.toGetOperationOptions(context.getOptions());
        authorizeOptions(getOptions, object, null, phase, task, result);

        ObjectSecurityConstraints securityConstraints = compileSecurityConstraints(object, task, result);

        AuthorizationDecisionType globalReadDecision = securityConstraints.findAllItemsDecision(ModelAuthorizationAction.AUTZ_ACTIONS_URLS_GET, phase);
        if (globalReadDecision == AuthorizationDecisionType.DENY) {
            // shortcut
            SecurityUtil.logSecurityDeny(object, "because the authorization denies access");
            throw new AuthorizationException("Access denied");
        }

        AuthorizationDecisionType globalAddDecision = securityConstraints.findAllItemsDecision(ModelAuthorizationAction.ADD.getUrl(), phase);
        AuthorizationDecisionType globalModifyDecision = securityConstraints.findAllItemsDecision(ModelAuthorizationAction.MODIFY.getUrl(), phase);

        elementContext.forEachObject(focusObject ->
            applySecurityConstraints(focusObject.getValue(), securityConstraints, phase,
                    globalReadDecision, globalAddDecision, globalModifyDecision,
                    false));

        elementContext.forEachDelta(focusDelta ->
            applySecurityConstraints(focusDelta, securityConstraints, phase,
                    globalReadDecision, globalAddDecision, globalModifyDecision));

        return securityConstraints;
    }

    private <O extends ObjectType> void applySchemasAndSecurityPhase(PrismObject<O> object, ObjectSecurityConstraints securityConstraints, PrismObjectDefinition<O> objectDefinition,
            GetOperationOptions rootOptions, AuthorizationPhaseType phase, Task task, OperationResult result)
                    throws SchemaException, SecurityViolationException, ConfigurationException, ObjectNotFoundException {
        Validate.notNull(phase);
        try {
            AuthorizationDecisionType globalReadDecision = securityConstraints.findAllItemsDecision(ModelAuthorizationAction.AUTZ_ACTIONS_URLS_GET, phase);
            if (globalReadDecision == AuthorizationDecisionType.DENY) {
                // shortcut
                SecurityUtil.logSecurityDeny(object, "because the authorization denies access");
                throw new AuthorizationException("Access denied");
            }

            AuthorizationDecisionType globalAddDecision = securityConstraints.findAllItemsDecision(ModelAuthorizationAction.ADD.getUrl(), phase);
            AuthorizationDecisionType globalModifyDecision = securityConstraints.findAllItemsDecision(ModelAuthorizationAction.MODIFY.getUrl(), phase);
            applySecurityConstraints(object.getValue(), securityConstraints, phase,
                    globalReadDecision, globalAddDecision, globalModifyDecision, true);
            if (object.isEmpty()) {
                // let's make it explicit
                SecurityUtil.logSecurityDeny(object, "because the subject has not access to any item");
                throw new AuthorizationException("Access denied");
            }

            applySecurityConstraintsItemDef(objectDefinition, new IdentityHashMap<>(), ItemPath.EMPTY_PATH, securityConstraints, globalReadDecision, globalAddDecision, globalModifyDecision, phase);
        } catch (SecurityViolationException | RuntimeException e) {
            result.recordFatalError(e);
            throw e;
        }
    }

    private <O extends ObjectType> ObjectSecurityConstraints compileSecurityConstraints(PrismObject<O> object, Task task, OperationResult result) throws SecurityViolationException, SchemaException, ConfigurationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException {
        try {
            ObjectSecurityConstraints securityConstraints = securityEnforcer.compileSecurityConstraints(object, null, task, result);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Security constraints for {}:\n{}", object, securityConstraints==null?"null":securityConstraints.debugDump());
            }
            if (securityConstraints == null) {
                SecurityUtil.logSecurityDeny(object, "because no security constraints are defined (default deny)");
                throw new AuthorizationException("Access denied");
            }
            return securityConstraints;
        } catch (Throwable e) {
            result.recordFatalError(e);
            throw e;
        }
    }

    private void applySecurityConstraints(PrismContainerValue<?> pcv, ObjectSecurityConstraints securityConstraints, AuthorizationPhaseType phase,
            AuthorizationDecisionType defaultReadDecision, AuthorizationDecisionType defaultAddDecision,
            AuthorizationDecisionType defaultModifyDecision, boolean applyToDefinitions) {
        LOGGER.trace("applySecurityConstraints(items): items={}, phase={}, defaults R={}, A={}, M={}",
                pcv.getItems(), phase, defaultReadDecision, defaultAddDecision, defaultModifyDecision);
        if (pcv.hasNoItems()) {
            return;
        }
        List<Item> itemsToRemove = new ArrayList<>();
        for (Item<?, ?> item : pcv.getItems()) {
            ItemPath itemPath = item.getPath();
            ItemDefinition<?> itemDef = item.getDefinition();
            if (itemDef != null && itemDef.isElaborate()) {
                LOGGER.trace("applySecurityConstraints(item): {}: skip (elaborate)", itemPath);
                continue;
            }
            ItemPath nameOnlyItemPath = itemPath.namedSegmentsOnly();
            AuthorizationDecisionType itemReadDecision = computeItemDecision(securityConstraints, nameOnlyItemPath, ModelAuthorizationAction.AUTZ_ACTIONS_URLS_GET, defaultReadDecision, phase);
            AuthorizationDecisionType itemAddDecision = computeItemDecision(securityConstraints, nameOnlyItemPath, ModelAuthorizationAction.AUTZ_ACTIONS_URLS_ADD, defaultReadDecision, phase);
            AuthorizationDecisionType itemModifyDecision = computeItemDecision(securityConstraints, nameOnlyItemPath, ModelAuthorizationAction.AUTZ_ACTIONS_URLS_MODIFY, defaultReadDecision, phase);
            LOGGER.trace("applySecurityConstraints(item): {}: decisions R={}, A={}, M={}",
                    itemPath, itemReadDecision, itemAddDecision, itemModifyDecision);
            if (applyToDefinitions && itemDef != null) {
                itemDef = ensureMutableDefinition((Item) item);
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
                if (itemReadDecision == AuthorizationDecisionType.DENY) {
                    // Explicitly denied access to the entire container
                    itemsToRemove.add(item);
                } else {
                    // No explicit decision (even ALLOW is not final here as something may be denied deeper inside)
                    AuthorizationDecisionType subDefaultReadDecision = defaultReadDecision;
                    if (itemReadDecision == AuthorizationDecisionType.ALLOW) {
                        // This means allow to all subitems unless otherwise denied.
                        subDefaultReadDecision = AuthorizationDecisionType.ALLOW;
                    }
                    List<? extends PrismContainerValue<?>> values = ((PrismContainer<?>)item).getValues();
                    reduceContainerValues(values, securityConstraints, phase, itemReadDecision, itemAddDecision, itemModifyDecision, subDefaultReadDecision, applyToDefinitions);
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
        for (Item itemToRemove : itemsToRemove) {
            pcv.remove(itemToRemove);
        }
    }

    private <D extends ItemDefinition<?>> D ensureMutableDefinition(Item<?, D> item) {
        D original = item.getDefinition();
        if (TransformableItemDefinition.isMutableAccess(original)) {
            return original;
        }
        if (true) {
            throw new IllegalStateException("Transformer did not applied transformable properly");
        }

        D replace = TransformableItemDefinition.publicFrom(original);
        try {
            item.applyDefinition(replace, true);
        } catch (SchemaException e) {
            throw new IllegalStateException("Can not replace definition with wrapper");
        }
        return replace;
    }

    private MutableItemDefinition<?> mutable(ItemDefinition<?> itemDef) {
        return itemDef.toMutable();
    }

    private <O extends ObjectType> void applySecurityConstraints(ObjectDelta<O> objectDelta, ObjectSecurityConstraints securityConstraints, AuthorizationPhaseType phase,
            AuthorizationDecisionType defaultReadDecision, AuthorizationDecisionType defaultAddDecision, AuthorizationDecisionType defaultModifyDecision) {
        LOGGER.trace("applySecurityConstraints(objectDelta): items={}, phase={}, defaults R={}, A={}, M={}",
                objectDelta, phase, defaultReadDecision, defaultAddDecision, defaultModifyDecision);
        if (objectDelta == null) {
            return;
        }
        if (objectDelta.isAdd()) {
            applySecurityConstraints(objectDelta.getObjectToAdd().getValue(), securityConstraints, phase, defaultReadDecision,
                    defaultAddDecision, defaultModifyDecision, false);
            return;
        }
        if (objectDelta.isDelete()) {
            // Nothing to do
            return;
        }
        // Modify delta
        Collection<? extends ItemDelta<?,?>> modifications = objectDelta.getModifications();
        if (modifications == null || modifications.isEmpty()) {
            // Nothing to do
            return;
        }
        List<ItemDelta<?, ?>> itemsToRemove = new ArrayList<>();
        for (ItemDelta<?, ?> modification : modifications) {
            ItemPath itemPath = modification.getPath();
            ItemDefinition<?> itemDef = modification.getDefinition();
            if (itemDef != null && itemDef.isElaborate()) {
                LOGGER.trace("applySecurityConstraints(item): {}: skip (elaborate)", itemPath);
                continue;
            }
            ItemPath nameOnlyItemPath = itemPath.namedSegmentsOnly();
            AuthorizationDecisionType itemReadDecision = computeItemDecision(securityConstraints, nameOnlyItemPath, ModelAuthorizationAction.AUTZ_ACTIONS_URLS_GET, defaultReadDecision, phase);
            AuthorizationDecisionType itemAddDecision = computeItemDecision(securityConstraints, nameOnlyItemPath, ModelAuthorizationAction.AUTZ_ACTIONS_URLS_ADD, defaultReadDecision, phase);
            AuthorizationDecisionType itemModifyDecision = computeItemDecision(securityConstraints, nameOnlyItemPath, ModelAuthorizationAction.AUTZ_ACTIONS_URLS_MODIFY, defaultReadDecision, phase);
            LOGGER.trace("applySecurityConstraints(item): {}: decisions R={}, A={}, M={}",
                    itemPath, itemReadDecision, itemAddDecision, itemModifyDecision);
            if (modification instanceof ContainerDelta<?>) {
                if (itemReadDecision == AuthorizationDecisionType.DENY) {
                    // Explicitly denied access to the entire container
                    itemsToRemove.add(modification);
                } else {
                    // No explicit decision (even ALLOW is not final here as something may be denied deeper inside)
                    AuthorizationDecisionType subDefaultReadDecision = defaultReadDecision;
                    if (itemReadDecision == AuthorizationDecisionType.ALLOW) {
                        // This means allow to all subitems unless otherwise denied.
                        subDefaultReadDecision = AuthorizationDecisionType.ALLOW;
                    }

                    reduceContainerValues((List<? extends PrismContainerValue<?>>) ((ContainerDelta<?>)modification).getValuesToAdd(),
                            securityConstraints, phase, itemReadDecision, itemAddDecision, itemModifyDecision, subDefaultReadDecision, false);
                    reduceContainerValues((List<? extends PrismContainerValue<?>>) ((ContainerDelta<?>)modification).getValuesToDelete(),
                            securityConstraints, phase, itemReadDecision, itemAddDecision, itemModifyDecision, subDefaultReadDecision, false);
                    reduceContainerValues((List<? extends PrismContainerValue<?>>) ((ContainerDelta<?>)modification).getValuesToReplace(),
                            securityConstraints, phase, itemReadDecision, itemAddDecision, itemModifyDecision, subDefaultReadDecision, false);

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

    private boolean reduceContainerValues(List<? extends PrismContainerValue<?>> values, ObjectSecurityConstraints securityConstraints, AuthorizationPhaseType phase,
            AuthorizationDecisionType itemReadDecision, AuthorizationDecisionType itemAddDecision, AuthorizationDecisionType itemModifyDecision, AuthorizationDecisionType subDefaultReadDecision,
            boolean applyToDefinitions) {
        if (values == null) {
            return false;
        }
        boolean removedSomething = false;
        Iterator<? extends PrismContainerValue<?>> vi = values.iterator();
        while (vi.hasNext()) {
            PrismContainerValue<?> cval = vi.next();
            applySecurityConstraints(cval, securityConstraints, phase,
                    subDefaultReadDecision, itemAddDecision, itemModifyDecision, applyToDefinitions);
            if (cval.hasNoItems() && itemReadDecision == null) {
                // We have removed all the content, if there was any. So, in the default case, there's nothing that
                // we are interested in inside this PCV. Therefore let's just remove it.
                // (If itemReadDecision is ALLOW, we obviously keep this untouched.)
                vi.remove();
                removedSomething = true;
            }
        }
        return removedSomething;
    }

    public <D extends ItemDefinition> void applySecurityConstraints(D itemDefinition, ObjectSecurityConstraints securityConstraints,
            AuthorizationPhaseType phase) {
        if (phase == null) {
            applySecurityConstraintsPhase(itemDefinition, securityConstraints, AuthorizationPhaseType.REQUEST);
            applySecurityConstraintsPhase(itemDefinition, securityConstraints, AuthorizationPhaseType.EXECUTION);
        } else {
            applySecurityConstraintsPhase(itemDefinition, securityConstraints, phase);
        }
    }

    private <D extends ItemDefinition> void applySecurityConstraintsPhase(D itemDefinition, ObjectSecurityConstraints securityConstraints,
            AuthorizationPhaseType phase) {
        Validate.notNull(phase);
        LOGGER.trace("applySecurityConstraints(itemDefs): def={}, phase={}", itemDefinition, phase);
        applySecurityConstraintsItemDef(itemDefinition, new IdentityHashMap<>(), ItemPath.EMPTY_PATH, securityConstraints,
                null, null, null, phase);

    }

    private <D extends ItemDefinition> void applySecurityConstraintsItemDef(D itemDefinition,
            IdentityHashMap<ItemDefinition, Object> definitionsSeen,
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
        if (itemDefinition instanceof PrismContainerDefinition<?> && !thisWasSeen) {
            PrismContainerDefinition<?> containerDefinition = (PrismContainerDefinition<?>)itemDefinition;
            List<? extends ItemDefinition> subDefinitions = ((PrismContainerDefinition<?>)containerDefinition).getDefinitions();
            for (ItemDefinition subDef: subDefinitions) {
                ItemPath subPath = ItemPath.create(nameOnlyItemPath, subDef.getItemName());
                if (subDef.isElaborate()) {
                    LOGGER.trace("applySecurityConstraints(itemDef): {}: skip (elaborate)", subPath);
                    continue;
                }
                if (!subDef.getItemName().equals(ShadowType.F_ATTRIBUTES)) { // Shadow attributes have special handling
                    applySecurityConstraintsItemDef(subDef, definitionsSeen, subPath, securityConstraints,
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

        LOGGER.trace("applySecurityConstraints(itemDef): {}: decisions R={}, A={}, M={}; subelements R={}, A={}, M={}",
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
    public AuthorizationDecisionType computeItemDecision(ObjectSecurityConstraints securityConstraints, ItemPath nameOnlyItemPath, String[] actionUrls,
            AuthorizationDecisionType defaultDecision, AuthorizationPhaseType phase) {
        AuthorizationDecisionType explicitDecision = securityConstraints.findItemDecision(nameOnlyItemPath, actionUrls, phase);
//        LOGGER.trace("Explicit decision for {} ({} {}): {}", nameOnlyItemPath, actionUrl, phase, explicitDecision);
        if (explicitDecision != null) {
            return explicitDecision;
        } else {
            return defaultDecision;
        }
    }

    public <O extends ObjectType> ObjectTemplateType determineObjectTemplate(PrismObject<O> object, AuthorizationPhaseType phase, OperationResult result) throws SchemaException, ConfigurationException, ObjectNotFoundException {
        ArchetypePolicyType archetypePolicy = archetypeManager.determineArchetypePolicy(object, result);
        if (archetypePolicy == null) {
            return null;
        }
        ObjectReferenceType objectTemplateRef = archetypePolicy.getObjectTemplateRef();
        if (objectTemplateRef == null || StringUtils.isEmpty(objectTemplateRef.getOid())) {
            return null;
        }
        PrismObject<ObjectTemplateType> template = cacheRepositoryService.getObject(ObjectTemplateType.class,
                objectTemplateRef.getOid(), createReadOnlyCollection(), result);
        return template.asObjectable();
    }

    public <O extends ObjectType> ObjectTemplateType determineObjectTemplate(Class<O> objectClass, AuthorizationPhaseType phase, OperationResult result) throws SchemaException, ConfigurationException, ObjectNotFoundException {
        PrismObject<SystemConfigurationType> systemConfiguration = systemObjectCache.getSystemConfiguration(result);
        if (systemConfiguration == null) {
            return null;
        }
        ObjectPolicyConfigurationType objectPolicyConfiguration = ArchetypeManager.determineObjectPolicyConfiguration(objectClass, null, systemConfiguration.asObjectable());
        if (objectPolicyConfiguration == null) {
            return null;
        }
        ObjectReferenceType objectTemplateRef = objectPolicyConfiguration.getObjectTemplateRef();
        if (objectTemplateRef == null) {
            return null;
        }
        PrismObject<ObjectTemplateType> template = cacheRepositoryService.getObject(
                ObjectTemplateType.class, objectTemplateRef.getOid(), createReadOnlyCollection(), result);
        return template.asObjectable();
    }

    <O extends ObjectType> void applyObjectTemplateToDefinition(
            PrismObjectDefinition<O> objectDefinition, ObjectTemplateType objectTemplate, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
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
        for (ObjectTemplateItemDefinitionType templateItemDefType: objectTemplate.getItem()) {
                ItemPathType ref = templateItemDefType.getRef();
                if (ref == null) {
                    throw new SchemaException("No 'ref' in item definition in "+objectTemplate);
                }
                ItemPath itemPath = prismContext.toPath(ref);
                ItemDefinition itemDef = objectDefinition.findItemDefinition(itemPath);
                if (itemDef != null) {
                    applyObjectTemplateItem(itemDef, templateItemDefType, "item " + itemPath + " in object type " + objectDefinition.getTypeName() + " as specified in item definition in " + objectTemplate);
                } else {
                    OperationResult subResult = result.createMinorSubresult(SchemaTransformer.class.getName() + ".applyObjectTemplateToDefinition");
                    subResult.recordPartialError("No definition for item " + itemPath + " in object type " + objectDefinition.getTypeName() + " as specified in item definition in " + objectTemplate);
                    continue;
                }
        }
    }

    private <O extends ObjectType> void applyObjectTemplateToObject(
            PrismObject<O> object, ObjectTemplateType objectTemplate, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
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
        for (ObjectTemplateItemDefinitionType templateItemDefType: objectTemplate.getItem()) {
            ItemPathType ref = templateItemDefType.getRef();
            if (ref == null) {
                throw new SchemaException("No 'ref' in item definition in "+objectTemplate);
            }
            ItemPath itemPath = prismContext.toPath(ref);
            ItemDefinition itemDefFromObject = object.getDefinition().findItemDefinition(itemPath);
            if (itemDefFromObject != null) {
                applyObjectTemplateItem(itemDefFromObject, templateItemDefType, "item " + itemPath + " in " + object
                        + " as specified in item definition in " + objectTemplate);
            } else {
                OperationResult subResult = result.createMinorSubresult(SchemaTransformer.class.getName() + ".applyObjectTemplateToObject");
                subResult.recordPartialError("No definition for item " + itemPath + " in " + object
                        + " as specified in item definition in " + objectTemplate);
                continue;
            }
            Collection<Item<?, ?>> items = object.getAllItems(itemPath);
            for (Item<?, ?> item : items) {
                ItemDefinition itemDef = item.getDefinition();
                if (itemDef != itemDefFromObject) {
                    applyObjectTemplateItem(itemDef, templateItemDefType, "item "+itemPath+" in " + object
                            + " as specified in item definition in "+objectTemplate);
                }
            }
        }
    }

    private <IV extends PrismValue,ID extends ItemDefinition> void applyObjectTemplateItem(ID itemDef,
            ObjectTemplateItemDefinitionType templateItemDefType, String desc) throws SchemaException {
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
    private <O extends ObjectType> List<VisibilityPolicyEntry> getVisibilityPolicy(
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

    private <T extends ObjectType> void validateObject(PrismObject<T> object, GetOperationOptions options, OperationResult result) {
        try {
            if (InternalsConfig.readEncryptionChecks) {
                CryptoUtil.checkEncrypted(object);
            }
            if (!InternalsConfig.consistencyChecks) {
                return;
            }
            Class<T> type = object.getCompileTimeClass();
            boolean tolerateRaw = GetOperationOptions.isTolerateRawData(options);
            if (type == ResourceType.class || ShadowType.class.isAssignableFrom(type) || type == ReportType.class) {
                // We tolerate raw values for resource and shadows in case the user has requested so
                tolerateRaw = GetOperationOptions.isRaw(options);
            }
            if (hasError(object, result)) {
                // If there is an error then the object might not be complete.
                // E.g. we do not have a complete dynamic schema to apply to the object
                // Tolerate some raw meat in that case.
                tolerateRaw = true;
            }
            if (InternalsConfig.consistencyChecks) {
                object.checkConsistence(true, !tolerateRaw, ConsistencyCheckScope.THOROUGH);
            }
        } catch (RuntimeException e) {
            result.recordFatalError(e);
            throw e;
        }
    }

    private <T extends ObjectType> boolean hasError(PrismObject<T> object, OperationResult result) {
        if (result != null && result.isError()) {        // actually, result is pretty tiny here - does not include object fetch/get operation
            return true;
        }
        OperationResultType fetchResult = object.asObjectable().getFetchResult();
        if (fetchResult != null &&
                (fetchResult.getStatus() == OperationResultStatusType.FATAL_ERROR ||
                fetchResult.getStatus() == OperationResultStatusType.PARTIAL_ERROR)) {
            return true;
        }
        return false;
    }

    public <O extends Objectable> TransformableObjectDefinition<O> transformableDefinition(PrismObjectDefinition<O> definition) {
        return TransformableObjectDefinition.of(definition);
    }

}
