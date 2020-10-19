/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector;

import java.util.Collection;

import com.evolveum.midpoint.model.impl.lens.projector.util.ProcessorExecution;
import com.evolveum.midpoint.model.impl.lens.projector.util.ProcessorMethod;
import com.evolveum.midpoint.model.impl.sync.SynchronizationService;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.model.impl.lens.projector.focus.AssignmentProcessor;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.PointInTimeType;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import javax.xml.datatype.XMLGregorianCalendar;

import static java.util.Objects.requireNonNull;

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
    @Autowired private SynchronizationService synchronizationService;
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

        ObjectDelta<ShadowType> originalSecondaryDelta = CloneUtil.clone(projContext.getSecondaryDelta());
        LOGGER.trace("Original secondary delta:\n{}", DebugUtil.debugDumpLazily(originalSecondaryDelta));

        checkSchemaAndPolicies(projContext, activityDescription);

        SynchronizationPolicyDecision policyDecision = projContext.getSynchronizationPolicyDecision();
        if (policyDecision == SynchronizationPolicyDecision.UNLINK) {
            // We will not update accounts that are being unlinked.
            // we cannot skip deleted accounts here as the delete delta will be skipped as well
            LOGGER.trace("Skipping processing of value for {} because the decision is {}", projContext.getHumanReadableName(), policyDecision);
            return;
        }

        context.checkConsistenceIfNeeded();

        if (!projContext.hasFullShadow() && hasIterationExpression(projContext)) {
            contextLoader.loadFullShadow(context, projContext, "iteration expression", task, result);
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
                LOGGER.trace("Original secondary delta:\n{}", DebugUtil.debugDumpLazily(originalSecondaryDelta)); // todo remove

                if (!evaluateIterationCondition(context, projContext, iteration, iterationToken, true, task, iterationResult)) {

                    conflictMessage = "pre-iteration condition was false";
                    LOGGER.debug("Skipping iteration {}, token '{}' for {} because the pre-iteration condition was false",
                            iteration, iterationToken, projContext.getHumanReadableName());
                } else {

                    context.checkConsistenceIfNeeded();

                    // Re-evaluates the values in the account constructions (including roles)
                    assignmentProcessor.processAssignmentsAccountValues(projContext, iterationResult);

                    context.recompute();
                    context.checkConsistenceIfNeeded();

//                policyRuleProcessor.evaluateShadowPolicyRules(context, projContext, activityDescription, task, iterationResult);

                    // Evaluates the values in outbound mappings
                    outboundProcessor.processOutbound(context, projContext, task, iterationResult);

                    // Merges the values together, processing exclusions and strong/weak mappings are needed
                    consolidationProcessor.consolidateValues(context, projContext, task, iterationResult);

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
                        cleanupContext(projContext, null, originalSecondaryDelta);
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
                            PrismObject conflictingShadow = checker.getConflictingShadow();
                            if (conflictingShadow != null) {
                                LOGGER.debug("Current shadow does not satisfy constraints. It conflicts with {}. Now going to find out what's wrong.", conflictingShadow);
                                PrismObject<ShadowType> fullConflictingShadow = null;
                                try {
                                    Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE));
                                    fullConflictingShadow = provisioningService.getObject(ShadowType.class, conflictingShadow.getOid(), options, task, iterationResult);
                                    LOGGER.trace("Full conflicting shadow = {}", fullConflictingShadow);
                                } catch (ObjectNotFoundException ex) {
                                    //if object not found exception occurred, its ok..the account was deleted by the discovery, so there esits no more conflicting shadow
                                    LOGGER.debug("Conflicting shadow was deleted by discovery. It does not exist anymore. Continue with adding current shadow.");
                                    conflict = false;
                                }

                                iterationResult.computeStatus(false);
                                // if the result is fatal error, it may mean that the
                                // already exists exception occurs before..but in this
                                // scenario it means, the exception was handled and we
                                // can mute the result to give better understanding of
                                // the situation which happened
                                if (iterationResult.isError()) {
                                    iterationResult.muteError();
                                }

                                if (conflict) {
                                    PrismObject<F> conflictingShadowOwner = repositoryService.searchShadowOwner(conflictingShadow.getOid(),
                                            SelectorOptions.createCollection(GetOperationOptions.createAllowNotFound()), iterationResult);
                                    LOGGER.trace("Conflicting shadow owner = {}", conflictingShadowOwner);

                                    //the owner of the shadow exist and it is a current user..so the shadow was successfully created, linked etc..no other recompute is needed..
                                    if (conflictingShadowOwner != null) {
                                        if (conflictingShadowOwner.getOid().equals(context.getFocusContext().getOid())) {
                                            treatConflictingWithTheSameOwner(projContext, originalSecondaryDelta, fullConflictingShadow);
                                            skipUniquenessCheck = true;
                                            continue;
                                        } else {
                                            LOGGER.trace("Iterating to the following shadow identifier, because shadow with the current identifier exists and it belongs to other user.");
                                        }
                                    } else {
                                        LOGGER.debug("There is no owner linked with the conflicting projection.");
                                        ResourceType resource = projContext.getResource();

                                        if (ResourceTypeUtil.isSynchronizationOpportunistic(resource)) {
                                            LOGGER.trace("Trying to find owner using correlation expression.");
                                            boolean match = synchronizationService.matchUserCorrelationRule(fullConflictingShadow,
                                                    context.getFocusContext().getObjectNew(), resource, context.getSystemConfiguration(), task, iterationResult);

                                            if (match) {
                                                treatConflictWithMatchedOwner(context, projContext, iterationResult, originalSecondaryDelta, fullConflictingShadow);
                                                skipUniquenessCheck = true;
                                                continue;
                                            } else {
                                                LOGGER.trace("User {} does not satisfy correlation rules.", context.getFocusContext().getObjectNew());
                                            }
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
                iterationResult.computeStatusIfUnknown();
            }

            iteration++;
            iterationToken = null;
            LensUtil.checkMaxIterations(iteration, maxIterations, conflictMessage, projContext.getHumanReadableName());

            cleanupContext(projContext, null, originalSecondaryDelta);
            context.checkConsistenceIfNeeded();
        }

        addIterationTokenDeltas(projContext);
        context.checkConsistenceIfNeeded();
    }

    private <F extends FocusType> void treatConflictWithMatchedOwner(LensContext<F> context, LensProjectionContext projContext,
            OperationResult result, ObjectDelta<ShadowType> originalSecondaryDelta, PrismObject<ShadowType> fullConflictingShadow)
            throws SchemaException {
        //check if it is add account (primary delta contains add shadow delta)..
        //if it is add account, create new context for conflicting account..
        //it ensures that conflicting account is linked to the user
        if (projContext.getPrimaryDelta() != null && projContext.getPrimaryDelta().isAdd()) {
            treatConflictForShadowAdd(context, projContext, result, fullConflictingShadow);
        } else {
            treatConflictForShadowNotAdd(context, projContext, originalSecondaryDelta, fullConflictingShadow);
        }
    }

    private <F extends FocusType> void treatConflictForShadowAdd(LensContext<F> context, LensProjectionContext projContext,
            OperationResult result, PrismObject<ShadowType> fullConflictingShadow) {
        PrismObject<ShadowType> shadow = projContext.getPrimaryDelta().getObjectToAdd();
        LOGGER.trace("Found primary ADD delta of shadow {}.", shadow);

        LensProjectionContext conflictingAccountContext = context.findProjectionContext(projContext.getResourceShadowDiscriminator(), fullConflictingShadow.getOid());
        if (conflictingAccountContext == null) {
            conflictingAccountContext = LensUtil.createAccountContext(context, projContext.getResourceShadowDiscriminator());
//                                                    conflictingAccountContext = context.createProjectionContext(accountContext.getResourceShadowDiscriminator());
            conflictingAccountContext.setOid(fullConflictingShadow.getOid());
            conflictingAccountContext.setObjectOld(fullConflictingShadow.clone());
            conflictingAccountContext.setObjectCurrent(fullConflictingShadow);
            conflictingAccountContext.setFullShadow(true);
            conflictingAccountContext.setSynchronizationPolicyDecision(SynchronizationPolicyDecision.KEEP);
            conflictingAccountContext.setResource(projContext.getResource());
            conflictingAccountContext.setDoReconciliation(true);
            conflictingAccountContext.getDependencies().clear();
            conflictingAccountContext.getDependencies().addAll(projContext.getDependencies());
            conflictingAccountContext.setWave(projContext.getWave());
            context.addConflictingProjectionContext(conflictingAccountContext);
        }

        projContext.setSynchronizationPolicyDecision(SynchronizationPolicyDecision.BROKEN);
        result.recordFatalError("Could not add account " + projContext.getObjectNew() + ", because the account with the same identifier already exists on the resource. ");
        LOGGER.error("Could not add account {}, because the account with the same identifier already exists on the resource. ", projContext.getObjectNew());

        LOGGER.trace("Will skip uniqueness check to avoid endless loop. Reason: conflicting account is being explicitly added.");
    }

    private <F extends FocusType> void treatConflictForShadowNotAdd(LensContext<F> context, LensProjectionContext projContext,
            ObjectDelta<ShadowType> originalSecondaryDelta, PrismObject<ShadowType> fullConflictingShadow)
            throws SchemaException {
        //found shadow belongs to the current user..need to link it and replace current shadow with the found shadow..
        cleanupContext(projContext, fullConflictingShadow, originalSecondaryDelta);
        projContext.setObjectOld(fullConflictingShadow.clone());
        projContext.setObjectCurrent(fullConflictingShadow);
        projContext.setOid(fullConflictingShadow.getOid());
        projContext.setFullShadow(true);
        projContext.setSynchronizationPolicyDecision(SynchronizationPolicyDecision.KEEP);
        ObjectDelta<ShadowType> secondaryDelta = projContext.getSecondaryDelta();
        if (secondaryDelta != null && projContext.getOid() != null) {
            secondaryDelta.setOid(projContext.getOid());
        }
        LOGGER.trace("User {} satisfies correlation rules.", context.getFocusContext().getObjectNew());

        // Re-do this same iteration again (do not increase iteration count).
        // It will recompute the values and therefore enforce the user deltas and enable reconciliation
        LOGGER.trace("Will skip uniqueness check to avoid endless loop. Reason: conflicting account belongs to the current user.");
    }

    private void treatConflictingWithTheSameOwner(LensProjectionContext projContext,
            ObjectDelta<ShadowType> originalSecondaryDelta, PrismObject<ShadowType> fullConflictingShadow)
            throws SchemaException {
        LOGGER.debug("Conflicting projection already linked to the current focus, no recompute needed, continue processing with conflicting projection.");
        cleanupContext(projContext, fullConflictingShadow, originalSecondaryDelta);
        projContext.setSynchronizationPolicyDecision(SynchronizationPolicyDecision.KEEP);
        projContext.setObjectOld(fullConflictingShadow.clone());
        projContext.setObjectCurrent(fullConflictingShadow);
        projContext.setOid(fullConflictingShadow.getOid());
        projContext.setFullShadow(true);
//                                    result.computeStatus();
//                                    // if the result is fatal error, it may mean that the
//                                    // already exists exception occurs before..but in this
//                                    // scenario it means, the exception was handled and we
//                                    // can mute the result to give better understanding of
//                                    // the situation which happened
//                                    if (result.isError()){
//                                        result.muteError();
//                                    }
        // Re-do this same iteration again (do not increase iteration count).
        // It will recompute the values and therefore enforce the user deltas and enable reconciliation
        LOGGER.trace("Will skip uniqueness check to avoid endless loop. Reason: conflicting projection is already linked to the current focus.");
    }

    private boolean willResetIterationCounter(LensProjectionContext projectionContext) throws SchemaException {
        ObjectDelta<ShadowType> projectionDelta = projectionContext.getCurrentDelta();
        if (projectionDelta == null) {
            return false;
        }
        LOGGER.trace("willResetIterationCounter: projectionDelta is\n{}", projectionDelta.debugDumpLazily());
        RefinedObjectClassDefinition oOcDef = projectionContext.getCompositeObjectClassDefinition();
        for (RefinedAttributeDefinition identifierDef: oOcDef.getPrimaryIdentifiers()) {
            ItemPath identifierPath = ItemPath.create(ShadowType.F_ATTRIBUTES, identifierDef.getItemName());
            if (projectionDelta.findPropertyDelta(identifierPath) != null) {
                return true;
            }
        }
        for (RefinedAttributeDefinition identifierDef: oOcDef.getSecondaryIdentifiers()) {
            ItemPath identifierPath = ItemPath.create(ShadowType.F_ATTRIBUTES, identifierDef.getItemName());
            if (projectionDelta.findPropertyDelta(identifierPath) != null) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("RedundantIfStatement")
    private boolean hasIterationExpression(LensProjectionContext accountContext) {
        ResourceObjectTypeDefinitionType accDef = accountContext.getResourceObjectTypeDefinitionType();
        if (accDef == null) {
            return false;
        }
        IterationSpecificationType iterationType = accDef.getIteration();
        if (iterationType == null) {
            return false;
        }
        if (iterationType.getTokenExpression() != null) {
            return true;
        }
        if (iterationType.getPostIterationCondition() != null) {
            return true;
        }
        if (iterationType.getPreIterationCondition() != null) {
            return true;
        }
        return false;
    }

    private int determineMaxIterations(LensProjectionContext accountContext) {
        ResourceObjectTypeDefinitionType accDef = accountContext.getResourceObjectTypeDefinitionType();
        if (accDef != null) {
            IterationSpecificationType iteration = accDef.getIteration();
            return LensUtil.determineMaxIterations(iteration);
        } else {
            return LensUtil.determineMaxIterations(null);
        }
    }

    private <F extends ObjectType> String formatIterationToken(LensContext<F> context,
            LensProjectionContext accountContext, int iteration, Task task, OperationResult result)
                    throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        ResourceObjectTypeDefinitionType accDef = accountContext.getResourceObjectTypeDefinitionType();
        if (accDef == null) {
            return LensUtil.formatIterationTokenDefault(iteration);
        }
        IterationSpecificationType iterationType = accDef.getIteration();
        ExpressionVariables variables = createExpressionVariables(context, accountContext);
        return LensUtil.formatIterationToken(context, accountContext, iterationType, iteration,
                expressionFactory, variables, task, result);
    }

    private <F extends ObjectType> ExpressionVariables createExpressionVariables(LensContext<F> context,
            LensProjectionContext projectionContext) {
        return ModelImplUtils.getDefaultExpressionVariables(context.getFocusContext().getObjectNew(), projectionContext.getObjectNew(),
                projectionContext.getResourceShadowDiscriminator(), projectionContext.getResource().asPrismObject(),
                context.getSystemConfiguration(), projectionContext, prismContext);
    }

    private <F extends ObjectType> boolean evaluateIterationCondition(LensContext<F> context,
            LensProjectionContext accountContext, int iteration, String iterationToken,
            boolean beforeIteration, Task task, OperationResult result)
                    throws ExpressionEvaluationException, SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
        ResourceObjectTypeDefinitionType accDef = accountContext.getResourceObjectTypeDefinitionType();
        if (accDef == null) {
            return true;
        }
        IterationSpecificationType iterationType = accDef.getIteration();
        ExpressionVariables variables = createExpressionVariables(context, accountContext);
        return LensUtil.evaluateIterationCondition(context, accountContext, iterationType,
                iteration, iterationToken, beforeIteration, expressionFactory, variables, task, result);
    }

    /**
     * Check that the primary deltas do not violate schema and policies
     * TODO: implement schema check
     */
    private void checkSchemaAndPolicies(LensProjectionContext accountContext, String activityDescription)
            throws SchemaException, PolicyViolationException {
        ObjectDelta<ShadowType> primaryDelta = accountContext.getPrimaryDelta();
        if (primaryDelta == null || primaryDelta.isDelete()) {
            return;
        }

        RefinedObjectClassDefinition rAccountDef = accountContext.getCompositeObjectClassDefinition();
        if (rAccountDef == null) {
            throw new SchemaException("No definition for account type '"
                    +accountContext.getResourceShadowDiscriminator()+"' in "+accountContext.getResource());
        }

        if (primaryDelta.isAdd()) {
            PrismObject<ShadowType> accountToAdd = primaryDelta.getObjectToAdd();
            ResourceAttributeContainer attributesContainer = ShadowUtil.getAttributesContainer(accountToAdd);
            if (attributesContainer != null) {
                for (ResourceAttribute<?> attribute: attributesContainer.getAttributes()) {
                    RefinedAttributeDefinition rAttrDef = requireNonNull(rAccountDef.findAttributeDefinition(attribute.getElementName()));
                    if (!rAttrDef.isTolerant()) {
                        throw new PolicyViolationException("Attempt to add object with non-tolerant attribute "+attribute.getElementName()+" in "+
                                "account "+accountContext.getResourceShadowDiscriminator()+" during "+activityDescription);
                    }
                }
            }
        } else if (primaryDelta.isModify()) {
            for(ItemDelta<?,?> modification: primaryDelta.getModifications()) {
                if (modification.getParentPath().equivalent(SchemaConstants.PATH_ATTRIBUTES)) {
                    PropertyDelta<?> attrDelta = (PropertyDelta<?>) modification;
                    RefinedAttributeDefinition rAttrDef = requireNonNull(rAccountDef.findAttributeDefinition(attrDelta.getElementName()));
                    if (!rAttrDef.isTolerant()) {
                        throw new PolicyViolationException("Attempt to modify non-tolerant attribute "+attrDelta.getElementName()+" in "+
                                "account "+accountContext.getResourceShadowDiscriminator()+" during "+activityDescription);
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
            ObjectDelta<ShadowType> originalSecondaryDelta) throws SchemaException {
        LOGGER.trace("Cleaning up context; full conflicting shadow = {}", fullConflictingShadow);

        // We must NOT clean up activation computation here. This has happened before, it will not happen again
        // and it does not depend on iteration. That's why we stored original secondary delta.

        accountContext.setSecondaryDelta(CloneUtil.clone(originalSecondaryDelta));

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
    <F extends FocusType> void processPostRecon(LensContext<F> context, LensProjectionContext projContext,
            @SuppressWarnings("unused") String activityDescription, @SuppressWarnings("unused") XMLGregorianCalendar now,
            Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, PolicyViolationException {
        SynchronizationPolicyDecision policyDecision = projContext.getSynchronizationPolicyDecision();
        if (policyDecision == SynchronizationPolicyDecision.UNLINK) {
            // We will not update accounts that are being unlinked.
            // we cannot skip deleted accounts here as the delete delta will be skipped as well
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Skipping post-recon processing of value for {} because the decision is {}", projContext.getHumanReadableName(), policyDecision);
            }
            return;
        }

        consolidationProcessor.consolidateValuesPostRecon(context, projContext, task, result);
        context.checkConsistenceIfNeeded();
        projContext.recompute();
        context.checkConsistenceIfNeeded();
    }
}
