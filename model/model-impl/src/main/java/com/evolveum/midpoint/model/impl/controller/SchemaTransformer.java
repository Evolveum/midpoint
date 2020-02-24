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
import com.evolveum.midpoint.model.common.ArchetypeManager;
import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensElementContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.prism.*;
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
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.Contract;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Transforms the schema and objects by applying security constraints,
 * object template schema refinements, etc.
 *
 * @author semancik
 */
@Component
public class SchemaTransformer {

    private static final Trace LOGGER = TraceManager.getTrace(SchemaTransformer.class);

    private static final String OPERATION_APPLY_SCHEMAS_AND_SECURITY = SchemaTransformer.class.getName()+".applySchemasAndSecurity";

    @Autowired @Qualifier("cacheRepositoryService") private transient RepositoryService cacheRepositoryService;
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

    public <T extends ObjectType> void applySchemasAndSecurityToObjects(List<PrismObject<T>> objects,
            GetOperationOptions rootOptions, Collection<SelectorOptions<GetOperationOptions>> options,
            AuthorizationPhaseType phase, Task task, OperationResult result)
                    throws SecurityViolationException, SchemaException {
        for (int i = 0; i < objects.size(); i++) {
            PrismObject<T> object = objects.get(i);
            object = object.cloneIfImmutable();
            objects.set(i, object);
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
            PrismObject<T> parent = ObjectTypeUtil.getParentObject(value);
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
        OperationResult subresult = new OperationResult(SchemaTransformer.class.getName()+".applySchemasAndSecurityToObject");
        try {
            applySchemasAndSecurity(object, rootOptions, options, phase, task, subresult);
        } catch (IllegalArgumentException | IllegalStateException | SchemaException |ConfigurationException |ObjectNotFoundException | ExpressionEvaluationException | CommunicationException e) {
            LOGGER.error("Error post-processing object {}: {}", object, e.getMessage(), e);
            OperationResultType fetchResult = object.asObjectable().getFetchResult();
            if (fetchResult == null) {
                fetchResult = subresult.createOperationResultType();
                object.asObjectable().setFetchResult(fetchResult);
            } else {
                fetchResult.getPartialResults().add(subresult.createOperationResultType());
            }
            fetchResult.setStatus(OperationResultStatusType.FATAL_ERROR);
        } catch (SecurityViolationException e) {
            // We cannot go on and leave this object in the result set. The object was not post-processed.
            // Leaving it in the result set may leak information.
            result.recordFatalError(e);
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
    public <O extends ObjectType> void applySchemasAndSecurity(PrismObject<O> object, GetOperationOptions rootOptions,
            Collection<SelectorOptions<GetOperationOptions>> options,
            AuthorizationPhaseType phase, Task task, OperationResult parentResult)
                    throws SchemaException, SecurityViolationException, ConfigurationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException {
        LOGGER.trace("applySchemasAndSecurity({}) starting", object);
        OperationResult result = parentResult.createMinorSubresult(SchemaTransformer.class.getName()+".applySchemasAndSecurity");
        authorizeOptions(rootOptions, object, null, phase, task, result);
        validateObject(object, rootOptions, result);

        ObjectSecurityConstraints securityConstraints = compileSecurityConstraints(object, task, result);
        PrismObjectDefinition<O> objectDefinition = object.deepCloneDefinition(true, this::setFullAccessFlags);

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
            applyObjectTemplateToObject(object, objectTemplateType, result);
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
        LOGGER.trace("applySchemasAndSecurity finishing");            // to allow folding in log viewer
    }

    public <O extends ObjectType> void applySchemasAndSecurity(LensContext<O> context,
            AuthorizationPhaseType phase, Task task, OperationResult parentResult) throws SecurityViolationException, SchemaException, ConfigurationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException {
        LOGGER.trace("applySchemasAndSecurity({}) starting", context);
        OperationResult result = parentResult.createMinorSubresult(OPERATION_APPLY_SCHEMAS_AND_SECURITY);

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

        if (object == null ) {
            if (elementContext.getDelta() == null) {
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

    public void setFullAccessFlags(ItemDefinition<?> itemDef) {
        itemDef.toMutable().setCanRead(true);
        itemDef.toMutable().setCanAdd(true);
        itemDef.toMutable().setCanModify(true);
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
                if (itemReadDecision != AuthorizationDecisionType.ALLOW) {
                    itemDef.toMutable().setCanRead(false);
                }
                if (itemAddDecision != AuthorizationDecisionType.ALLOW) {
                    itemDef.toMutable().setCanAdd(false);
                }
                if (itemModifyDecision != AuthorizationDecisionType.ALLOW) {
                    itemDef.toMutable().setCanModify(false);
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

    private <O extends ObjectType> void applySecurityConstraints(ObjectDelta<O> objectDelta, ObjectSecurityConstraints securityConstraints, AuthorizationPhaseType phase,
            AuthorizationDecisionType defaultReadDecision, AuthorizationDecisionType defaultAddDecision, AuthorizationDecisionType defaultModifyDecision) {
        LOGGER.trace("applySecurityConstraints(objectDelta): items={}, phase={}, defaults R={}, A={}, M={}",
                objectDelta, phase, defaultReadDecision, defaultAddDecision, defaultModifyDecision);
        if (objectDelta == null) {
            return;
        }
        if (objectDelta.isAdd()) {
            applySecurityConstraints(objectDelta.getObjectToAdd().getValue(), securityConstraints, phase, defaultReadDecision, defaultAddDecision, defaultModifyDecision, false);
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
            itemDefinition.toMutable().setCanRead(false);
        }
        if (addDecision != AuthorizationDecisionType.ALLOW) {
            itemDefinition.toMutable().setCanAdd(false);
        }
        if (modifyDecision != AuthorizationDecisionType.ALLOW) {
            itemDefinition.toMutable().setCanModify(false);
        }

        if (anySubElementRead) {
            itemDefinition.toMutable().setCanRead(true);
        }
        if (anySubElementAdd) {
            itemDefinition.toMutable().setCanAdd(true);
        }
        if (anySubElementModify) {
            itemDefinition.toMutable().setCanModify(true);
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
        PrismObject<ObjectTemplateType> template = cacheRepositoryService.getObject(ObjectTemplateType.class, objectTemplateRef.getOid(), null, result);
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
        PrismObject<ObjectTemplateType> template = cacheRepositoryService.getObject(ObjectTemplateType.class, objectTemplateRef.getOid(), null, result);
        return template.asObjectable();
    }

    public <O extends ObjectType> void applyObjectTemplateToDefinition(PrismObjectDefinition<O> objectDefinition, ObjectTemplateType objectTemplateType, OperationResult result) throws ObjectNotFoundException, SchemaException {
        if (objectTemplateType == null) {
            return;
        }
        for (ObjectReferenceType includeRef: objectTemplateType.getIncludeRef()) {
            PrismObject<ObjectTemplateType> subTemplate = cacheRepositoryService.getObject(ObjectTemplateType.class, includeRef.getOid(), null, result);
            applyObjectTemplateToDefinition(objectDefinition, subTemplate.asObjectable(), result);
        }
        for (ObjectTemplateItemDefinitionType templateItemDefType: objectTemplateType.getItem()) {
                ItemPathType ref = templateItemDefType.getRef();
                if (ref == null) {
                throw new SchemaException("No 'ref' in item definition in "+objectTemplateType);
                }
                ItemPath itemPath = prismContext.toPath(ref);
                ItemDefinition itemDef = objectDefinition.findItemDefinition(itemPath);
                if (itemDef != null) {
                    applyObjectTemplateItem(itemDef, templateItemDefType, "item " + itemPath + " in object type " + objectDefinition.getTypeName() + " as specified in item definition in " + objectTemplateType);
                } else {
                    OperationResult subResult = result.createMinorSubresult(SchemaTransformer.class.getName() + ".applyObjectTemplateToDefinition");
                    subResult.recordPartialError("No definition for item " + itemPath + " in object type " + objectDefinition.getTypeName() + " as specified in item definition in " + objectTemplateType);
                    continue;
                }
        }
    }

    private <O extends ObjectType> void applyObjectTemplateToObject(PrismObject<O> object, ObjectTemplateType objectTemplateType, OperationResult result) throws ObjectNotFoundException, SchemaException {
        if (objectTemplateType == null) {
            return;
        }
        for (ObjectReferenceType includeRef: objectTemplateType.getIncludeRef()) {
            PrismObject<ObjectTemplateType> subTemplate = cacheRepositoryService.getObject(ObjectTemplateType.class, includeRef.getOid(), null, result);
            applyObjectTemplateToObject(object, subTemplate.asObjectable(), result);
        }
        for (ObjectTemplateItemDefinitionType templateItemDefType: objectTemplateType.getItem()) {
            ItemPathType ref = templateItemDefType.getRef();
            if (ref == null) {
                throw new SchemaException("No 'ref' in item definition in "+objectTemplateType);
            }
            ItemPath itemPath = prismContext.toPath(ref);
            ItemDefinition itemDefFromObject = object.getDefinition().findItemDefinition(itemPath);
            if (itemDefFromObject != null) {
                applyObjectTemplateItem(itemDefFromObject, templateItemDefType, "item " + itemPath + " in " + object
                        + " as specified in item definition in " + objectTemplateType);
            } else {
                OperationResult subResult = result.createMinorSubresult(SchemaTransformer.class.getName() + ".applyObjectTemplateToObject");
                subResult.recordPartialError("No definition for item " + itemPath + " in " + object
                        + " as specified in item definition in " + objectTemplateType);
                continue;
            }
            Item<?, ?> item = object.findItem(itemPath);
            if (item != null) {
                ItemDefinition itemDef = item.getDefinition();
                if (itemDef != itemDefFromObject) {
                    applyObjectTemplateItem(itemDef, templateItemDefType, "item "+itemPath+" in " + object
                            + " as specified in item definition in "+objectTemplateType);
                }
            }

        }
    }

    private <IV extends PrismValue,ID extends ItemDefinition> void applyObjectTemplateItem(ID itemDef,
            ObjectTemplateItemDefinitionType templateItemDefType, String desc) throws SchemaException {
        if (itemDef == null) {
            throw new SchemaException("No definition for "+desc);
        }

        MutableItemDefinition<?> mutableDef = itemDef.toMutable();

        String displayName = templateItemDefType.getDisplayName();
        if (displayName != null) {
            mutableDef.setDisplayName(displayName);
        }

        String help = templateItemDefType.getHelp();
        if (help != null) {
            mutableDef.setHelp(help);
        }

        Integer displayOrder = templateItemDefType.getDisplayOrder();
        if (displayOrder != null) {
            mutableDef.setDisplayOrder(displayOrder);
        }

        Boolean emphasized = templateItemDefType.isEmphasized();
        if (emphasized != null) {
            mutableDef.setEmphasized(emphasized);
        }

        Boolean deprecated = templateItemDefType.isDeprecated();
        if (deprecated != null) {
            mutableDef.setDeprecated(deprecated);
        }

        Boolean experimental = templateItemDefType.isExperimental();
        if (experimental != null) {
            mutableDef.setExperimental(experimental);
        }

        List<PropertyLimitationsType> limitations = templateItemDefType.getLimitations();
        if (limitations != null) {
            PropertyLimitationsType limitationsType = MiscSchemaUtil.getLimitationsType(limitations, LayerType.PRESENTATION);
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

    public <O extends ObjectType> void applyItemsConstraints(PrismObjectDefinition<O> objectDefinition, ArchetypePolicyType archetypePolicy, OperationResult result) throws ObjectNotFoundException, SchemaException {
        if (archetypePolicy == null) {
            return;
        }
        Map<ItemPath, UserInterfaceElementVisibilityType> visibilityMap = new HashMap<>();
        for (ItemConstraintType itemConstraint: archetypePolicy.getItemConstraint()) {
            UserInterfaceElementVisibilityType visibility = itemConstraint.getVisibility();
            if (visibility == null) {
                continue;
            }
            ItemPathType itemPathType = itemConstraint.getPath();
            if (itemPathType == null) {
                throw new SchemaException("No 'path' in item definition in archetype policy for "+objectDefinition);
            }
            ItemPath itemPath = prismContext.toPath(itemPathType);
            visibilityMap.put(itemPath, visibility);
        }
        if (visibilityMap.isEmpty()) {
            return;
        }
        reduceItems(objectDefinition, ItemPath.EMPTY_PATH, visibilityMap);
    }

    private UserInterfaceElementVisibilityType reduceItems(PrismContainerDefinition containerDefinition, ItemPath containerPath, Map<ItemPath, UserInterfaceElementVisibilityType> visibilityMap) {
        UserInterfaceElementVisibilityType containerVisibility = determineVisibility(visibilityMap, containerPath);
        if (containerVisibility == UserInterfaceElementVisibilityType.HIDDEN) {
            containerDefinition.getDefinitions().clear();
            return containerVisibility;
        }
        Iterator<ItemDefinition> iterator = containerDefinition.getDefinitions().iterator();
        while (iterator.hasNext()) {
            ItemDefinition subDefinition = iterator.next();
            ItemPath itemPath = containerPath.append(subDefinition.getItemName());
            if (subDefinition instanceof PrismContainerDefinition) {
                PrismContainerDefinition subContainerDef = (PrismContainerDefinition)subDefinition;
                UserInterfaceElementVisibilityType itemVisibility = reduceItems(subContainerDef, itemPath, visibilityMap);
                if (subContainerDef.isEmpty() && itemVisibility != UserInterfaceElementVisibilityType.VISIBLE) {
                    iterator.remove();
                }
            } else {
                UserInterfaceElementVisibilityType itemVisibility = determineVisibility(visibilityMap, itemPath);
                if (itemVisibility == UserInterfaceElementVisibilityType.VACANT || itemVisibility == UserInterfaceElementVisibilityType.HIDDEN) {
                    iterator.remove();
                }
            }
        }
        return containerVisibility;
    }

    private UserInterfaceElementVisibilityType determineVisibility(Map<ItemPath, UserInterfaceElementVisibilityType> visibilityMap, ItemPath itemPath) {
        if (itemPath == null || itemPath.isEmpty()) {
            return UserInterfaceElementVisibilityType.AUTOMATIC;
        }
        UserInterfaceElementVisibilityType visibility = visibilityMap.get(itemPath);
        if (visibility != null) {
            return visibility;
        }
        return determineVisibility(visibilityMap, itemPath.allExceptLast());
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

}
