/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector;

import static java.util.Objects.requireNonNull;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectable;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.model.impl.correlation.CorrelationServiceImpl;
import com.evolveum.midpoint.model.impl.lens.projector.loader.ContextLoader;

import com.evolveum.midpoint.util.SingleLocalizableMessage;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.model.impl.lens.RememberedElementState;
import com.evolveum.midpoint.model.impl.lens.projector.focus.AssignmentProcessor;
import com.evolveum.midpoint.model.impl.lens.projector.util.ProcessorExecution;
import com.evolveum.midpoint.model.impl.lens.projector.util.ProcessorMethod;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Processor that determines values of account attributes. It does so by taking the pre-processed information left
 * behind by the assignment processor. It also does some checks, such as check of identifier uniqueness. It tries to
 * do several iterations over the value computations if a conflict is found (and this feature is enabled).
 *
 * @author Radovan Semancik
 */
@Component
@ProcessorExecution(focusRequired = true, focusType = FocusType.class)
public class ProjectionValuesProcessor implements ProjectorProcessor {

    private static final Trace LOGGER = TraceManager.getTrace(ProjectionValuesProcessor.class);

    private static final String OP_ITERATION = ProjectionValuesProcessor.class.getName() + ".iteration";

    @Autowired private OutboundProcessor outboundProcessor;
    @Autowired private ConsolidationProcessor consolidationProcessor;
    @Autowired private AssignmentProcessor assignmentProcessor;
    @Autowired @Qualifier("cacheRepositoryService") RepositoryService repositoryService;
    @Autowired private ExpressionFactory expressionFactory;
    @Autowired private PrismContext prismContext;
    @Autowired private CorrelationServiceImpl correlationService;
    @Autowired private ContextLoader contextLoader;
    @Autowired private ProvisioningService provisioningService;

    @ProcessorMethod
    public <F extends FocusType> void process(LensContext<F> context, LensProjectionContext projectionContext,
            String activityDescription, @SuppressWarnings("unused") XMLGregorianCalendar now, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, ObjectAlreadyExistsException,
            CommunicationException, ConfigurationException, SecurityViolationException, PolicyViolationException {
        processProjectionValues(context, projectionContext, activityDescription, task, result);
        context.checkConsistenceIfNeeded();
        projectionContext.recompute();
        context.checkConsistenceIfNeeded();
    }

    private <F extends FocusType> void processProjectionValues(LensContext<F> context,
            LensProjectionContext projContext, String activityDescription, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, ObjectAlreadyExistsException,
            CommunicationException, ConfigurationException, SecurityViolationException, PolicyViolationException {

        RememberedElementState<ShadowType> rememberedProjectionState = projContext.rememberElementState();
        LOGGER.trace("Remembered projection state:\n{}", DebugUtil.debugDumpLazily(rememberedProjectionState));

        checkSchemaAndPolicies(projContext, activityDescription);

        SynchronizationPolicyDecision policyDecision = projContext.getSynchronizationPolicyDecision();
        if (policyDecision == SynchronizationPolicyDecision.UNLINK) {
            // We will not update accounts that are being unlinked.
            // we cannot skip deleted accounts here as the delete delta will be skipped as well
            LOGGER.trace("Skipping processing of values for {} because the decision is {}",
                    projContext.getHumanReadableName(), policyDecision);
            return;
        }

        context.checkConsistenceIfNeeded();

        if (!projContext.hasFullShadow() && hasIterationExpression(projContext)) {
            contextLoader.loadFullShadow(projContext, "iteration expression", task, result);
            if (projContext.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.BROKEN) {
                return;
            }
        }

        int maxIterations = determineMaxIterations(projContext);
        int iteration = 0;
        String iterationToken = null;
        boolean wasResetIterationCounter = false;

        PrismObject<ShadowType> shadowCurrent = projContext.getObjectCurrent();
        if (shadowCurrent != null) {
            Integer shadowIteration = shadowCurrent.asObjectable().getIteration();
            if (shadowIteration != null) {
                iteration = shadowIteration;
            }
            iterationToken = shadowCurrent.asObjectable().getIterationToken();
        }

        boolean skipUniquenessCheck = false;
        while (true) {

            projContext.setIteration(iteration);
            if (iterationToken == null) {
                iterationToken = formatIterationToken(context, projContext, iteration, task, result);
            }
            projContext.setIterationToken(iterationToken);

            String conflictMessage;

            // These are normally null. But there may be leftover from the previous iteration.
            // While that should not affect the algorithm (it should overwrite it) it may confuse
            // people during debugging and unnecessarily clutter the debug output.
            projContext.setEvaluatedPlainConstruction(null);
            projContext.setSqueezedAttributes(null);
            projContext.setSqueezedAssociations(null);

            OperationResult iterationResult = result.subresult(OP_ITERATION)
                    .addParam("iteration", iteration)
                    .addParam("iterationToken", iterationToken)
                    .build();
            try {
                LOGGER.trace("Projection values iteration {}, token '{}' for {}",
                        iteration, iterationToken, projContext.getHumanReadableName());
                LOGGER.trace("Original secondary delta:\n{}", DebugUtil.debugDumpLazily(rememberedProjectionState)); // todo remove

                if (!evaluateIterationCondition(
                        context, projContext, iteration, iterationToken, true, task, iterationResult)) {

                    conflictMessage = "pre-iteration condition was false";
                    LOGGER.debug("Skipping iteration {}, token '{}' for {} because the pre-iteration condition was false",
                            iteration, iterationToken, projContext.getHumanReadableName());
                } else {

                    context.checkConsistenceIfNeeded();

                    // Re-evaluates the values in the account constructions (including roles) - currently no-op!
                    assignmentProcessor.processAssignmentsAccountValues(projContext, iterationResult);

                    context.recompute();
                    context.checkConsistenceIfNeeded();

                    // Evaluates the values in outbound mappings
                    outboundProcessor.processOutbound(context, projContext, task, iterationResult);

                    // Merges the values together, processing exclusions and strong/weak mappings are needed
                    consolidationProcessor.consolidateValues(projContext, task, iterationResult);

                    // Aux object classes may have changed during consolidation. Make sure we have up-to-date definitions.
                    context.refreshAuxiliaryObjectClassDefinitions();

                    // Check if we need to reset the iteration counter (and token) e.g. because we have rename
                    // we cannot do that before because the mappings are not yet evaluated and the triples and not
                    // consolidated to deltas. We can do it only now. It means that we will waste the first run
                    // but I don't see any easier way to do it now.
                    if (iteration != 0 && !wasResetIterationCounter && willResetIterationCounter(projContext)) {
                        wasResetIterationCounter = true;
                        iteration = 0;
                        iterationToken = null;
                        cleanupContext(projContext, null, rememberedProjectionState);
                        LOGGER.trace("Resetting iteration counter and token because we have rename");
                        context.checkConsistenceIfNeeded();
                        continue;
                    }

                    if (policyDecision == SynchronizationPolicyDecision.DELETE) {
                        // No need to play the iterative game if the account is deleted
                        break;
                    }

                    // Check constraints
                    boolean conflict = true;
                    ShadowConstraintsChecker<F> checker = new ShadowConstraintsChecker<>(projContext);

                    ConstraintsCheckingStrategyType strategy = context.getProjectionConstraintsCheckingStrategy();
                    boolean skipWhenNoIteration = strategy != null && Boolean.TRUE.equals(strategy.isSkipWhenNoIteration());
                    // skipWhenNoChange is applied by the uniqueness checker itself (in provisioning)

                    if (skipWhenNoIteration && maxIterations == 0) {
                        LOGGER.trace("Skipping uniqueness checking because 'skipWhenNoIteration' is true and there are no iterations defined");
                        conflict = false;
                    } else if (skipUniquenessCheck) {
                        LOGGER.trace("Skipping uniqueness check to avoid endless loop");
                        skipUniquenessCheck = false;
                        conflict = false;
                    } else {
                        checker.setPrismContext(prismContext);
                        checker.setContext(context);
                        checker.setProvisioningService(provisioningService);
                        checker.check(task, iterationResult);
                        if (checker.isSatisfiesConstraints()) {
                            LOGGER.trace("Current shadow satisfies uniqueness constraints. Iteration {}, token '{}'", iteration, iterationToken);
                            conflict = false;
                        } else {
                            PrismObject<ShadowType> conflictingShadow = checker.getConflictingShadow();
                            if (conflictingShadow != null) {
                                LOGGER.debug("Current shadow does not satisfy constraints. It conflicts with {}. Now going to find out what's wrong.", conflictingShadow);
                                LOGGER.trace("Conflicting shadow details:\n{}", conflictingShadow.debugDumpLazily(1));
                                PrismObject<ShadowType> fullConflictingShadow = null;
                                try {
                                    var options =
                                            SchemaService.get().getOperationOptionsBuilder()
                                                    .futurePointInTime()
                                                    //.readOnly() [not yet]
                                                    .build();
                                    fullConflictingShadow = provisioningService.getObject(
                                            ShadowType.class, conflictingShadow.getOid(), options, task, iterationResult);
                                    LOGGER.trace("Full conflicting shadow = {}", fullConflictingShadow);
                                } catch (ObjectNotFoundException ex) {
                                    // Looks like the conflicting resource object no longer exists. Its shadow in repository
                                    // might have been deleted by the discovery.
                                    LOGGER.debug("Conflicting shadow was deleted by discovery. It does not exist anymore. "
                                            + "Continue with adding current shadow.");
                                    conflict = false;
                                }

                                iterationResult.computeStatus(true);
                                // if the result is fatal error, it may mean that the
                                // already exists exception occurs before..but in this
                                // scenario it means, the exception was handled and we
                                // can mute the result to give better understanding of
                                // the situation which happened
                                if (iterationResult.isError()) {
                                    iterationResult.muteError();
                                }

                                if (conflict) {
                                    PrismObject<F> conflictingShadowOwner = repositoryService.searchShadowOwner(
                                            conflictingShadow.getOid(),
                                            SelectorOptions.createCollection(GetOperationOptions.createAllowNotFound()),
                                            iterationResult);
                                    LOGGER.trace("Conflicting shadow owner = {}", conflictingShadowOwner);

                                    // The owner of the shadow exist and it is the current user. So the shadow was successfully
                                    // created, linked, and so on; no other recompute is needed.
                                    if (conflictingShadowOwner != null) {
                                        if (conflictingShadowOwner.getOid().equals(context.getFocusContext().getOid())) {
                                            treatConflictingWithTheSameOwner(projContext, rememberedProjectionState, fullConflictingShadow);
                                            skipUniquenessCheck = true;
                                            continue;
                                        } else {
                                            LOGGER.trace("Iterating to the following shadow identifier, because shadow with the"
                                                    + " current identifier exists and it belongs to other user.");
                                        }
                                    } else {
                                        LOGGER.debug("There is no owner linked with the conflicting projection, checking "
                                                + "opportunistic synchronization (if available).");
                                        if (doesMatchOpportunistically(
                                                context,
                                                projContext,
                                                fullConflictingShadow.asObjectable(),
                                                rememberedProjectionState,
                                                task,
                                                iterationResult)) {
                                            skipUniquenessCheck = true;
                                            continue;
                                        }
                                    }
                                }
                            } else {
                                LOGGER.debug("Current shadow does not satisfy constraints, but there is no conflicting shadow. Strange.");
                            }
                        }
                    }

                    if (!conflict) {
                        if (evaluateIterationCondition(context, projContext, iteration, iterationToken, false, task, iterationResult)) {
                            // stop the iterations
                            break;
                        } else {
                            conflictMessage = "post-iteration condition was false";
                            LOGGER.debug("Skipping iteration {}, token '{}' for {} because the post-iteration condition was false",
                                    iteration, iterationToken, projContext.getHumanReadableName());
                        }
                    } else {
                        conflictMessage = checker.getMessages();
                    }
                }
            } catch (Throwable t) {
                iterationResult.recordFatalError(t);
                throw t;
            } finally {
                iterationResult.recordEnd();
                iterationResult.close();
            }

            iteration++;
            iterationToken = null;
            //TODO use conflict message and human readable conflict message
            LensUtil.checkMaxIterations(iteration, maxIterations, conflictMessage, new SingleLocalizableMessage(conflictMessage), projContext.getHumanReadableName());

            cleanupContext(projContext, null, rememberedProjectionState);
            context.checkConsistenceIfNeeded();
        }

        addIterationTokenDeltas(projContext);
        context.checkConsistenceIfNeeded();
    }

    private <F extends FocusType> boolean doesMatchOpportunistically(
            LensContext<F> context,
            LensProjectionContext projContext,
            ShadowType fullConflictingShadow,
            RememberedElementState<ShadowType> rememberedProjectionState,
            Task task,
            OperationResult iterationResult)
            throws ConfigurationException, SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, SecurityViolationException {

        ResourceType resource = projContext.getResourceRequired();
        ResourceSchema schema = ResourceSchemaFactory.getCompleteSchema(resource);
        if (schema == null) {
            LOGGER.trace("No resource schema for {} -> opportunistic sync not available", resource);
            return false;
        }
        ShadowKindType kind = fullConflictingShadow.getKind();
        String intent = fullConflictingShadow.getIntent();
        if (!ShadowUtil.isClassified(kind, intent)) {
            // Full conflicting shadow has been retrieved using ProvisioningService#getObject method, so the classification
            // was already attempted for it. If it's not classified, then there is no more hope.
            LOGGER.trace("Conflicting shadow is not classified -> opportunistic sync is not available");
            return false;
        }
        SynchronizationPolicy synchronizationPolicy = SynchronizationPolicyFactory.forKindAndIntent(kind, intent, resource);
        if (synchronizationPolicy == null) {
            LOGGER.trace("No sync policy for {}/{} on {} -> opportunistic sync not available", kind, intent, resource);
            return false;
        }

        if (!synchronizationPolicy.isOpportunistic()) {
            LOGGER.trace("Opportunistic sync for {}/{} on {} is disabled", kind, intent, resource);
            return false;
        }

        F focus = asObjectable(context.getFocusContext().getObjectNew());
        if (focus == null) {    // MID-10729 focus oid might be null in case user is not yet stored
            LOGGER.trace("'object new' is null or without OID ({}) -> no opportunistic sync will be attempted", focus);
            return false;
        }

        LOGGER.trace("Checking if the owner matches (using the correlation service).");
        boolean matches = correlationService.checkCandidateOwner(
                fullConflictingShadow, resource, synchronizationPolicy, focus, task, iterationResult);

        if (matches) {
            LOGGER.trace("Object {} satisfies correlation rules.", focus);
            treatConflictWithMatchedOwner(
                    context, projContext, iterationResult, rememberedProjectionState, fullConflictingShadow);
            return true;
        } else {
            LOGGER.trace("Object {} does not satisfy correlation rules.", focus);
            return false;
        }
    }

    private <F extends FocusType> void treatConflictWithMatchedOwner(
            LensContext<F> context,
            LensProjectionContext projContext,
            OperationResult result,
            @NotNull RememberedElementState<ShadowType> rememberedProjectionState,
            ShadowType fullConflictingShadow)
            throws SchemaException {
        //check if it is add account (primary delta contains add shadow delta)..
        //if it is add account, create new context for conflicting account..
        //it ensures that conflicting account is linked to the user
        if (projContext.getPrimaryDelta() != null && projContext.getPrimaryDelta().isAdd()) {
            treatConflictForShadowAdd(context, projContext, result, fullConflictingShadow.asPrismObject());
        } else {
            treatConflictForShadowNotAdd(context, projContext, rememberedProjectionState, fullConflictingShadow.asPrismObject());
        }
    }

    private <F extends FocusType> void treatConflictForShadowAdd(LensContext<F> context, LensProjectionContext projContext,
            OperationResult result, PrismObject<ShadowType> fullConflictingShadow) {
        PrismObject<ShadowType> shadow = projContext.getPrimaryDelta().getObjectToAdd();
        LOGGER.trace("Found primary ADD delta of shadow {}.", shadow);

        LensProjectionContext conflictingCtx =
                context.findProjectionContextByOidAndKey(fullConflictingShadow.getOid(), projContext.getKey());
        if (conflictingCtx == null) {
            conflictingCtx = context.createDetachedProjectionContext(projContext.getKey());
            conflictingCtx.initializeElementState(
                    fullConflictingShadow.getOid(), fullConflictingShadow.clone(), fullConflictingShadow, null);
            conflictingCtx.setFullShadow(true);
            conflictingCtx.setSynchronizationPolicyDecision(SynchronizationPolicyDecision.KEEP);
            conflictingCtx.setResource(projContext.getResource());
            conflictingCtx.setDoReconciliation(true);
            conflictingCtx.setWave(projContext.getWave());
            // As for the dependencies, they will be automatically re-computed based on kind/intent that should be the same
            // for this context as they are for projContext. So no need of copying them (as it was here before 4.6).
            context.addConflictingProjectionContext(conflictingCtx);
        }

        projContext.setBroken();
        result.recordFatalError("Could not add account " + projContext.getObjectNew() + ", because the account with the same identifier already exists on the resource. ");
        LOGGER.error("Could not add account {}, because the account with the same identifier already exists on the resource. ", projContext.getObjectNew());

        LOGGER.trace("Will skip uniqueness check to avoid endless loop. Reason: conflicting account is being explicitly added.");
    }

    private <F extends FocusType> void treatConflictForShadowNotAdd(LensContext<F> context, LensProjectionContext projContext,
            @NotNull RememberedElementState<ShadowType> rememberedProjectionState, PrismObject<ShadowType> fullConflictingShadow)
            throws SchemaException {
        //found shadow belongs to the current user..need to link it and replace current shadow with the found shadow..
        cleanupContext(projContext, fullConflictingShadow, rememberedProjectionState);
        projContext.replaceOldAndCurrentObject(
                fullConflictingShadow.getOid(), fullConflictingShadow.clone(), fullConflictingShadow);
        projContext.setFullShadow(true);
        projContext.setSynchronizationPolicyDecision(SynchronizationPolicyDecision.KEEP);
        LOGGER.trace("User {} satisfies correlation rules.", context.getFocusContext().getObjectNew());

        // Re-do this same iteration again (do not increase iteration count).
        // It will recompute the values and therefore enforce the user deltas and enable reconciliation
        LOGGER.trace("Will skip uniqueness check to avoid endless loop. Reason: conflicting account belongs to the current user.");
    }

    private void treatConflictingWithTheSameOwner(LensProjectionContext projContext,
            @NotNull RememberedElementState<ShadowType> rememberedProjectionState, PrismObject<ShadowType> fullConflictingShadow)
            throws SchemaException {
        LOGGER.debug("Conflicting projection already linked to the current focus, no recompute needed, continue processing with conflicting projection.");
        cleanupContext(projContext, fullConflictingShadow, rememberedProjectionState);
        projContext.setSynchronizationPolicyDecision(SynchronizationPolicyDecision.KEEP);
        projContext.replaceOldAndCurrentObject(
                fullConflictingShadow.getOid(), fullConflictingShadow.clone(), fullConflictingShadow);
        projContext.setFullShadow(true);
        projContext.setExists(true);

        // Re-do this same iteration again (do not increase iteration count).
        // It will recompute the values and therefore enforce the user deltas and enable reconciliation
        LOGGER.trace("Will skip uniqueness check to avoid endless loop. "
                + "Reason: conflicting projection is already linked to the current focus.");
    }

    private boolean willResetIterationCounter(LensProjectionContext projectionContext)
            throws SchemaException, ConfigurationException {
        ObjectDelta<ShadowType> projectionDelta = projectionContext.getCurrentDelta();
        if (projectionDelta == null) {
            return false;
        }
        LOGGER.trace("willResetIterationCounter: projectionDelta is\n{}", projectionDelta.debugDumpLazily());
        ResourceObjectDefinition oOcDef = projectionContext.getCompositeObjectDefinition();
        for (ResourceAttributeDefinition<?> identifierDef: oOcDef.getPrimaryIdentifiers()) {
            ItemPath identifierPath = ItemPath.create(ShadowType.F_ATTRIBUTES, identifierDef.getItemName());
            if (projectionDelta.findPropertyDelta(identifierPath) != null) {
                return true;
            }
        }
        for (ResourceAttributeDefinition<?> identifierDef: oOcDef.getSecondaryIdentifiers()) {
            ItemPath identifierPath = ItemPath.create(ShadowType.F_ATTRIBUTES, identifierDef.getItemName());
            if (projectionDelta.findPropertyDelta(identifierPath) != null) {
                return true;
            }
        }
        return false;
    }

    private boolean hasIterationExpression(LensProjectionContext projCtx) throws SchemaException, ConfigurationException {
        IterationSpecificationType iterationSpec = getIterationSpecification(projCtx);
        if (iterationSpec == null) {
            return false;
        }
        return iterationSpec.getTokenExpression() != null
                || iterationSpec.getPostIterationCondition() != null
                || iterationSpec.getPreIterationCondition() != null;
    }

    @Nullable private IterationSpecificationType getIterationSpecification(LensProjectionContext projCtx)
            throws SchemaException, ConfigurationException {
        ResourceObjectDefinition def = projCtx.getStructuralDefinitionIfNotBroken();
        return def != null ? def.getDefinitionBean().getIteration() : null;
    }

    private int determineMaxIterations(LensProjectionContext projCtx) throws SchemaException, ConfigurationException {
        return LensUtil.determineMaxIterations(
                getIterationSpecification(projCtx));
    }

    private <F extends ObjectType> String formatIterationToken(
            LensContext<F> context,
            LensProjectionContext projCtx,
            int iteration,
            Task task,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        IterationSpecificationType iterationSpec = getIterationSpecification(projCtx);
        if (iterationSpec == null) {
            return LensUtil.formatIterationTokenDefault(iteration);
        }
        VariablesMap variables = createVariablesMap(context, projCtx);
        return LensUtil.formatIterationToken(
                projCtx, iterationSpec, iteration, expressionFactory, variables, task, result);
    }

    private <F extends ObjectType> VariablesMap createVariablesMap(LensContext<F> context,
            LensProjectionContext projectionContext) {
        return ModelImplUtils.getDefaultVariablesMap(
                context.getFocusContext().getObjectNew(),
                projectionContext.getObjectNew(),
                projectionContext.getResourceRequired().asPrismObject(),
                context.getSystemConfiguration(),
                projectionContext);
    }

    private <F extends ObjectType> boolean evaluateIterationCondition(
            LensContext<F> context,
            LensProjectionContext projCtx,
            int iteration,
            String iterationToken,
            boolean beforeIteration,
            Task task,
            OperationResult result)
            throws ExpressionEvaluationException, SchemaException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        IterationSpecificationType iterationSpec = getIterationSpecification(projCtx);
        if (iterationSpec == null) {
            return true;
        }
        VariablesMap variables = createVariablesMap(context, projCtx);
        return LensUtil.evaluateIterationCondition(
                context, projCtx, iterationSpec, iteration, iterationToken, beforeIteration, expressionFactory, variables,
                task, result);
    }

    /**
     * Check that the primary deltas do not violate schema and policies
     * TODO: implement schema check
     */
    private void checkSchemaAndPolicies(LensProjectionContext accountContext, String activityDescription)
            throws SchemaException, PolicyViolationException, ConfigurationException {
        ObjectDelta<ShadowType> primaryDelta = accountContext.getPrimaryDelta();
        if (primaryDelta == null || primaryDelta.isDelete()) {
            return;
        }

        ResourceObjectDefinition rAccountDef = accountContext.getCompositeObjectDefinition();
        if (rAccountDef == null) {
            throw new SchemaException("No definition for account type '"
                    +accountContext.getKey()+"' in "+accountContext.getResource());
        }

        if (primaryDelta.isAdd()) {
            PrismObject<ShadowType> accountToAdd = primaryDelta.getObjectToAdd();
            ResourceAttributeContainer attributesContainer = ShadowUtil.getAttributesContainer(accountToAdd);
            if (attributesContainer != null) {
                for (ResourceAttribute<?> attribute: attributesContainer.getAttributes()) {
                    ResourceAttributeDefinition<?> rAttrDef = requireNonNull(rAccountDef.findAttributeDefinition(attribute.getElementName()));
                    if (!rAttrDef.isTolerant()) {
                        throw new PolicyViolationException("Attempt to add object with non-tolerant attribute "+attribute.getElementName()+" in "+
                                "account "+accountContext.getKey()+" during "+activityDescription);
                    }
                }
            }
        } else if (primaryDelta.isModify()) {
            for(ItemDelta<?,?> modification: primaryDelta.getModifications()) {
                if (modification.getParentPath().equivalent(SchemaConstants.PATH_ATTRIBUTES)) {
                    PropertyDelta<?> attrDelta = (PropertyDelta<?>) modification;
                    ResourceAttributeDefinition<?> rAttrDef = requireNonNull(rAccountDef.findAttributeDefinition(attrDelta.getElementName()));
                    if (!rAttrDef.isTolerant()) {
                        throw new PolicyViolationException("Attempt to modify non-tolerant attribute "+attrDelta.getElementName()+" in "+
                                "account "+accountContext.getKey()+" during "+activityDescription);
                    }
                }
            }
        } else {
            throw new IllegalStateException("Whoops!");
        }
    }

    /**
     * Remove the intermediate results of values processing such as secondary deltas.
     */
    private void cleanupContext(LensProjectionContext accountContext, PrismObject<ShadowType> fullConflictingShadow,
            @NotNull RememberedElementState<ShadowType> rememberedProjectionState) throws SchemaException {
        LOGGER.trace("Cleaning up context; full conflicting shadow = {}", fullConflictingShadow);

        // We must NOT clean up activation computation here. This has happened before, it will not happen again
        // and it does not depend on iteration. That's why we stored original secondary delta.

        accountContext.restoreElementState(rememberedProjectionState);

        // TODO But, in fact we want to cleanup up activation changes if they are already applied to the new shadow.
        //  This was implemented before but it's missing now.

        accountContext.clearIntermediateResults();
        accountContext.recompute();
    }

    /**
     * Adds deltas for iteration and iterationToken to the shadow if needed.
     */
    private void addIterationTokenDeltas(LensProjectionContext accountContext) throws SchemaException {
        PrismObject<ShadowType> shadowCurrent = accountContext.getObjectCurrent();
        if (shadowCurrent != null) {
            Integer iterationOld = shadowCurrent.asObjectable().getIteration();
            String iterationTokenOld = shadowCurrent.asObjectable().getIterationToken();
            if (iterationOld != null && iterationOld == accountContext.getIteration() &&
                    iterationTokenOld != null && iterationTokenOld.equals(accountContext.getIterationToken())) {
                // Already stored
                return;
            }
        }
        PrismObjectDefinition<ShadowType> shadowDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ShadowType.class);

        PrismPropertyValue<Integer> iterationVal = prismContext.itemFactory().createPropertyValue(accountContext.getIteration());
        iterationVal.setOriginType(OriginType.OUTBOUND);
        PropertyDelta<Integer> iterationDelta = prismContext.deltaFactory().property().createReplaceDelta(shadowDef,
                ShadowType.F_ITERATION, iterationVal);
        accountContext.swallowToSecondaryDelta(iterationDelta);

        PrismPropertyValue<String> iterationTokenVal = prismContext.itemFactory().createPropertyValue(accountContext.getIterationToken());
        iterationTokenVal.setOriginType(OriginType.OUTBOUND);
        PropertyDelta<String> iterationTokenDelta = prismContext.deltaFactory().property().createReplaceDelta(shadowDef,
                ShadowType.F_ITERATION_TOKEN, iterationTokenVal);
        accountContext.swallowToSecondaryDelta(iterationTokenDelta);

    }

    @ProcessorMethod
    <F extends FocusType> void processPostRecon(
            LensContext<F> context,
            LensProjectionContext projContext,
            @SuppressWarnings("unused") String activityDescription,
            @SuppressWarnings("unused") XMLGregorianCalendar now,
            Task task,
            OperationResult result)
            throws SchemaException, ExpressionEvaluationException, PolicyViolationException, ConfigurationException {
        SynchronizationPolicyDecision policyDecision = projContext.getSynchronizationPolicyDecision();
        if (policyDecision == SynchronizationPolicyDecision.UNLINK) {
            // We will not update accounts that are being unlinked.
            // we cannot skip deleted accounts here as the delete delta will be skipped as well
            LOGGER.trace("Skipping post-recon processing of value for {} because the decision is {}",
                    projContext.getHumanReadableName(), policyDecision);
            return;
        }

        consolidationProcessor.consolidateValuesPostRecon(projContext, task, result);
        context.checkConsistenceIfNeeded();
        projContext.recompute();
        context.checkConsistenceIfNeeded();
    }
}
