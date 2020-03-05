/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector.focus;

import static com.evolveum.midpoint.schema.internals.InternalsConfig.consistencyChecks;
import static org.apache.commons.lang3.BooleanUtils.isTrue;

import java.util.Map;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.impl.lens.projector.Projector;
import com.evolveum.midpoint.model.impl.lens.projector.policy.PolicyRuleEnforcer;
import com.evolveum.midpoint.model.impl.lens.projector.policy.PolicyRuleProcessor;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.DefinitionUtil;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.util.exception.NoFocusNameSchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.model.impl.lens.ClockworkMedic;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.model.impl.lens.projector.ContextLoader;
import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
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
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Processor to handle everything about focus: values, assignments, etc.
 *
 * @author Radovan Semancik
 *
 */
@Component
public class AssignmentHolderProcessor {

    private static final Trace LOGGER = TraceManager.getTrace(AssignmentHolderProcessor.class);

    @Autowired private ContextLoader contextLoader;
    @Autowired private InboundProcessor inboundProcessor;
    @Autowired private AssignmentProcessor assignmentProcessor;
    @Autowired private ObjectTemplateProcessor objectTemplateProcessor;
    @Autowired private PrismContext prismContext;
    @Autowired private ExpressionFactory expressionFactory;
    @Autowired private PolicyRuleProcessor policyRuleProcessor;
    @Autowired private FocusLifecycleProcessor focusLifecycleProcessor;
    @Autowired private ClockworkMedic medic;
    @Autowired private PolicyRuleEnforcer policyRuleEnforcer;
    @Autowired private CacheConfigurationManager cacheConfigurationManager;

    @Autowired private FocusProcessor focusProcessor;

    @Autowired
    @Qualifier("cacheRepositoryService")
    private transient RepositoryService cacheRepositoryService;

    public <O extends ObjectType, AH extends AssignmentHolderType> void processFocus(LensContext<O> context, String activityDescription,
            XMLGregorianCalendar now, Task task, OperationResult result)
                    throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, PolicyViolationException,
                    ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException, PreconditionViolationException {

        LensFocusContext<O> focusContext = context.getFocusContext();
        if (focusContext == null) {
            return;
        }

        if (!AssignmentHolderType.class.isAssignableFrom(focusContext.getObjectTypeClass())) {
            LOGGER.trace("Skipping processing focus. Not assignment holder type, type {}", focusContext.getObjectTypeClass());
            return;
        }

        //noinspection unchecked
        processFocusFocus((LensContext<AH>)context, activityDescription, now, task, result);

    }

    @SuppressWarnings({ "unused", "RedundantThrows" })
    private <O extends ObjectType> void processFocusNonFocus(LensContext<O> context, String activityDescription,
            XMLGregorianCalendar now, Task task, OperationResult result)
                    throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, PolicyViolationException,
                    ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException, PreconditionViolationException {
        // This is somehow "future legacy" code. It will be removed later when we have better support for organizational structure
        // membership in resources and tasks.
        assignmentProcessor.computeTenantRefLegacy(context, task, result);
    }

    private <AH extends AssignmentHolderType> void processFocusFocus(LensContext<AH> context, String activityDescription,
            XMLGregorianCalendar now, Task task, OperationResult result)
                    throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, PolicyViolationException,
                    ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException, PreconditionViolationException {
        LensFocusContext<AH> focusContext = context.getFocusContext();
        PartialProcessingOptionsType partialProcessingOptions = context.getPartialProcessingOptions();

        checkArchetypeRefDelta(context);

        boolean resetOnRename = true; // This is fixed now. TODO: make it configurable

        boolean wasResetOnIterationSpecificationChange = false;
        boolean iterationSpecificationInitialized = false;
        IterationSpecificationType iterationSpecification = null;       // initializing just for the compiler not to complain
        int maxIterations = 0;                                          // initializing just for the compiler not to complain

        int iteration = focusContext.getIteration();
        String iterationToken = focusContext.getIterationToken();
        boolean wasResetIterationCounter = false;

        PrismObject<AH> focusCurrent = focusContext.getObjectCurrent();
        if (focusCurrent != null && iterationToken == null) {
            Integer focusIteration = focusCurrent.asObjectable().getIteration();
            if (focusIteration != null) {
                iteration = focusIteration;
            }
            iterationToken = focusCurrent.asObjectable().getIterationToken();
        }

        while (true) {

            ObjectTemplateType objectTemplate = context.getFocusTemplate();

            if (!iterationSpecificationInitialized) {
                iterationSpecification = LensUtil.getIterationSpecification(objectTemplate);
                maxIterations = LensUtil.determineMaxIterations(iterationSpecification);
                LOGGER.trace("maxIterations = {}, iteration specification = {} derived from template {}", maxIterations,
                        iterationSpecification, objectTemplate);
                iterationSpecificationInitialized = true;
            }

            ArchetypePolicyType archetypePolicy = focusContext.getArchetypePolicyType();
            LensUtil.applyObjectPolicyConstraints(focusContext, archetypePolicy, prismContext);

            ExpressionVariables variablesPreIteration = ModelImplUtils.getDefaultExpressionVariables(focusContext.getObjectNew(),
                    null, null, null, context.getSystemConfiguration(), focusContext, prismContext);
            if (iterationToken == null) {
                iterationToken = LensUtil.formatIterationToken(context, focusContext,
                        iterationSpecification, iteration, expressionFactory, variablesPreIteration, task, result);
            }

            // We have to remember the token and iteration in the context.
            // The context can be recomputed several times. But we always want
            // to use the same iterationToken if possible. If there is a random
            // part in the iterationToken expression that we need to avoid recomputing
            // the token otherwise the value can change all the time (even for the same inputs).
            // Storing the token in the secondary delta is not enough because secondary deltas can be dropped
            // if the context is re-projected.
            focusContext.setIteration(iteration);
            focusContext.setIterationToken(iterationToken);
            LOGGER.trace("Focus {} processing, iteration {}, token '{}'", focusContext.getHumanReadableName(), iteration, iterationToken);

            String conflictMessage;
            if (!LensUtil.evaluateIterationCondition(context, focusContext,
                    iterationSpecification, iteration, iterationToken, true, expressionFactory, variablesPreIteration, task, result)) {

                conflictMessage = "pre-iteration condition was false";
                LOGGER.debug("Skipping iteration {}, token '{}' for {} because the pre-iteration condition was false",
                        iteration, iterationToken, focusContext.getHumanReadableName());
            } else {

                // INBOUND

                if (consistencyChecks) context.checkConsistence();

                medic.partialExecute("inbound",
                        (result1) -> {
                            // Loop through the account changes, apply inbound expressions
                            inboundProcessor.processInbound(context, now, task, result1);
                            if (consistencyChecks) context.checkConsistence();
                            context.recomputeFocus();
                            contextLoader.updateArchetypePolicy(context, task, result1);
                            contextLoader.updateArchetype(context, task, result1);
                            medic.traceContext(LOGGER, activityDescription, "inbound", false, context, false);
                            if (consistencyChecks) context.checkConsistence();
                        },
                        partialProcessingOptions::getInbound,
                        Projector.class, context, result);

                // ACTIVATION

                medic.partialExecute("focusActivation",
                        (result1) -> focusProcessor.processActivationBeforeAssignments(context, now, result1),
                        partialProcessingOptions::getFocusActivation,
                        Projector.class, context, result);


                // OBJECT TEMPLATE (before assignments)

                if (focusContext.isDelete()) {
                    LOGGER.trace("Skipping refreshing of object template and iterator specification: focus delete");
                } else {
                    contextLoader.setFocusTemplate(context, result);
                    if (!wasResetOnIterationSpecificationChange) {
                        IterationSpecificationType newIterationSpecification = context.getFocusTemplate() != null ?
                                context.getFocusTemplate().getIterationSpecification() : null;
                        if (!java.util.Objects.equals(iterationSpecification, newIterationSpecification)) {
                            LOGGER.trace("Resetting iteration counter and token because of iteration specification change");
                            iteration = 0;
                            iterationToken = null;
                            wasResetOnIterationSpecificationChange = true;
                            wasResetIterationCounter = false;
                            iterationSpecificationInitialized = false;
                            cleanupContext(focusContext);
                            continue;
                        }
                    }
                }

                medic.partialExecute("objectTemplateBeforeAssignments",
                        (result1) -> objectTemplateProcessor.processTemplate(context,
                                ObjectTemplateMappingEvaluationPhaseType.BEFORE_ASSIGNMENTS, now, task, result1),
                        partialProcessingOptions::getObjectTemplateBeforeAssignments,
                        Projector.class, context, result);


                // process activation again. Object template might have changed it.
                context.recomputeFocus();
                medic.partialExecute("focusActivation",
                        (result1) -> focusProcessor.processActivationAfterAssignments(context, now, result1),
                        partialProcessingOptions::getFocusActivation,
                        Projector.class, context, result);

                // ASSIGNMENTS

                focusContext.clearPendingObjectPolicyStateModifications();
                focusContext.clearPendingAssignmentPolicyStateModifications();

                medic.partialExecute("assignments",
                        (result1) -> assignmentProcessor.processAssignments(context, now, task, result1),
                        partialProcessingOptions::getAssignments,
                        Projector.class, context, result);

                medic.partialExecute("assignmentsOrg",
                        (result1) -> assignmentProcessor.processOrgAssignments(context, task, result1),
                        partialProcessingOptions::getAssignmentsOrg,
                        Projector.class, context, result);


                medic.partialExecute("assignmentsMembershipAndDelegate",
                        (result1) -> assignmentProcessor.processMembershipAndDelegatedRefs(context, result1),
                        partialProcessingOptions::getAssignmentsMembershipAndDelegate,
                        Projector.class, context, result);

                context.recompute();

                medic.partialExecute("assignmentsConflicts",
                        (result1) -> assignmentProcessor.checkForAssignmentConflicts(context, result1),
                        partialProcessingOptions::getAssignmentsConflicts,
                        Projector.class, context, result);

                medic.partialExecute("focusLifecycle",
                        (result1) -> focusLifecycleProcessor.processLifecycle(context, now, task, result1),
                        partialProcessingOptions::getFocusLifecycle,
                        Projector.class, context, result);

                // OBJECT TEMPLATE (after assignments)

                medic.partialExecute("objectTemplateAfterAssignments",
                        (result1) -> objectTemplateProcessor.processTemplate(context,
                                ObjectTemplateMappingEvaluationPhaseType.AFTER_ASSIGNMENTS, now, task, result1),
                        partialProcessingOptions::getObjectTemplateBeforeAssignments,
                        Projector.class, context, result);

                context.recompute();

                // process activation again. Second pass through object template might have changed it.
                // We also need to apply assignment activation if needed
                context.recomputeFocus();
                medic.partialExecute("focusActivation",
                        (result1) -> focusProcessor.processActivationAfterAssignments(context, now, result1),
                        partialProcessingOptions::getFocusActivation,
                        Projector.class, context, result);

                // CREDENTIALS (including PASSWORD POLICY)

                medic.partialExecute("focusCredentials",
                        (result1) -> focusProcessor.processCredentials(context, now, task, result1),
                        partialProcessingOptions::getFocusCredentials,
                        Projector.class, context, result);

                // We need to evaluate this as a last step. We need to make sure we have all the
                // focus deltas so we can properly trigger the rules.

                medic.partialExecute("focusPolicyRules",
                        (result1) -> policyRuleProcessor.evaluateObjectPolicyRules(context, activityDescription, now, task, result1),
                        partialProcessingOptions::getFocusPolicyRules,
                        Projector.class, context, result);

                // to mimic operation of the original enforcer hook, we execute the following only in the initial state
                if (context.getState() == ModelState.INITIAL) {
                    // If partial execution for focus policy rules and for assignments is turned off, this method call is a no-op.
                    // So we don't need to check the partial execution flags for its invocation.
                    policyRuleEnforcer.execute(context);
                }

                // Processing done, check for success

                //noinspection ConstantConditions
                if (resetOnRename && !wasResetIterationCounter && willResetIterationCounter(focusContext)) {
                    // Make sure this happens only the very first time during the first recompute.
                    // Otherwise it will always change the token (especially if the token expression has a random part)
                    // hence the focusContext.getIterationToken() == null
                    wasResetIterationCounter = true;
                    if (iteration != 0) {
                        iteration = 0;
                        iterationToken = null;
                        LOGGER.trace("Resetting iteration counter and token because rename was detected");
                        cleanupContext(focusContext);
                        continue;
                    }
                }

                ConstraintsCheckingStrategyType strategy = context.getFocusConstraintsCheckingStrategy();
                boolean skipWhenNoChange = strategy != null && Boolean.TRUE.equals(strategy.isSkipWhenNoChange());
                boolean skipWhenNoIteration = strategy != null && Boolean.TRUE.equals(strategy.isSkipWhenNoIteration());

                boolean checkConstraints;
                if (skipWhenNoChange && !hasNameDelta(focusContext)) {
                    LOGGER.trace("Skipping constraints check because 'skipWhenNoChange' is true and there's no name delta");
                    checkConstraints = false;
                } else if (skipWhenNoIteration && maxIterations == 0) {
                    LOGGER.trace("Skipping constraints check because 'skipWhenNoIteration' is true and there is no iteration defined");
                    checkConstraints = false;
                } else if (TaskType.class == focusContext.getObjectTypeClass()) {
                    LOGGER.trace("Skipping constraints check for task, not needed because tasks names are not unique.");
                    checkConstraints = false;
                } else {
                    checkConstraints = true;
                }

                PrismObject<AH> previewObjectNew = focusContext.getObjectNew();
                if (previewObjectNew == null) {
                    // this must be delete
                } else {
                    // Explicitly check for name. The checker would check for this also. But checking it here
                    // will produce better error message
                    PolyStringType objectName = previewObjectNew.asObjectable().getName();
                    if (objectName == null || objectName.getOrig().isEmpty()) {
                        throw new NoFocusNameSchemaException("No name in new object "+objectName+" as produced by template "+objectTemplate+
                                " in iteration "+iteration+", we cannot process an object without a name");
                    }
                }

                // Check if iteration constraints are OK
                FocusConstraintsChecker<AH> checker = new FocusConstraintsChecker<>();
                checker.setPrismContext(prismContext);
                checker.setContext(context);
                checker.setRepositoryService(cacheRepositoryService);
                checker.setCacheConfigurationManager(cacheConfigurationManager);
                boolean satisfies = !checkConstraints || checker.check(previewObjectNew, result);
                if (satisfies) {
                    LOGGER.trace("Current focus satisfies uniqueness constraints. Iteration {}, token '{}'", iteration, iterationToken);
                    ExpressionVariables variablesPostIteration = ModelImplUtils.getDefaultExpressionVariables(focusContext.getObjectNew(),
                            null, null, null, context.getSystemConfiguration(), focusContext, prismContext);
                    if (LensUtil.evaluateIterationCondition(context, focusContext,
                            iterationSpecification, iteration, iterationToken, false, expressionFactory, variablesPostIteration,
                            task, result)) {
                        // stop the iterations
                        break;
                    } else {
                        conflictMessage = "post-iteration condition was false";
                        LOGGER.debug("Skipping iteration {}, token '{}' for {} because the post-iteration condition was false",
                                iteration, iterationToken, focusContext.getHumanReadableName());
                    }
                } else {
                    LOGGER.trace("Current focus does not satisfy constraints. Conflicting object: {}; iteration={}, maxIterations={}",
                            checker.getConflictingObject(), iteration, maxIterations);
                    conflictMessage = checker.getMessages();
                }

                if (!wasResetIterationCounter) {
                    wasResetIterationCounter = true;
                    if (iteration != 0) {
                        iterationToken = null;
                        iteration = 0;
                        LOGGER.trace("Resetting iteration counter and token after conflict");
                        cleanupContext(focusContext);
                        continue;
                    }
                }
            }

            // Next iteration
            iteration++;
            iterationToken = null;
            LensUtil.checkMaxIterations(iteration, maxIterations, conflictMessage, focusContext.getHumanReadableName());
            cleanupContext(focusContext);
        }

        addIterationTokenDeltas(focusContext, iteration, iterationToken);
        checkItemsLimitations(focusContext);
        if (consistencyChecks) context.checkConsistence();

    }

    private <O extends ObjectType> void checkItemsLimitations(LensFocusContext<O> focusContext)
            throws SchemaException, ConfigurationException {
        Map<UniformItemPath, ObjectTemplateItemDefinitionType> itemDefinitionsMap = focusContext.getItemDefinitionsMap();
        PrismObject<O> objectNew = null;                    // lazily evaluated
        for (Map.Entry<UniformItemPath, ObjectTemplateItemDefinitionType> entry : itemDefinitionsMap.entrySet()) {
            for (PropertyLimitationsType limitation : entry.getValue().getLimitations()) {
                if (!limitation.getLayer().contains(LayerType.MODEL)) {     // or should we apply SCHEMA-layer limitations as well?
                    continue;
                }
                if (objectNew == null) {
                    focusContext.recompute();
                    objectNew = focusContext.getObjectNew();
                    if (objectNew == null) {
                        return;         // nothing to check on DELETE operation
                    }
                }
                checkItemLimitations(objectNew, entry.getKey(), limitation);
            }
        }
    }

    private <O extends ObjectType> void checkItemLimitations(PrismObject<O> object, ItemPath path, PropertyLimitationsType limitation)
            throws SchemaException {
        Object item = object.find(path);
        if (isTrue(limitation.isIgnore())) {
            return;
        }
        int count = getValueCount(item);
        Integer min = DefinitionUtil.parseMultiplicity(limitation.getMinOccurs());
        if (min != null && min > 0 && count < min) {
            throw new SchemaException("Expected at least " + min + " values of " + path + ", got " + count);
        }
        Integer max = DefinitionUtil.parseMultiplicity(limitation.getMaxOccurs());
        if (max != null && max >= 0 && count > max) {
            throw new SchemaException("Expected at most " + max + " values of " + path + ", got " + count);
        }
    }

    private int getValueCount(Object item) {
        if (item == null) {
            return 0;
        }
        if (!(item instanceof Item)) {
            throw new IllegalStateException("Expected Item but got " + item.getClass() + " instead");
        }
        return ((Item) item).getValues().size();
    }




    private <AH extends AssignmentHolderType> boolean willResetIterationCounter(LensFocusContext<AH> focusContext) throws SchemaException {
        ObjectDelta<AH> focusDelta = focusContext.getDelta();
        if (focusDelta == null) {
            return false;
        }
        if (focusContext.isAdd() || focusContext.isDelete()) {
            return false;
        }
        if (focusDelta.findPropertyDelta(FocusType.F_ITERATION) != null) {
            // there was a reset already in previous projector runs
            return false;
        }
        // Check for rename
        return hasNameDelta(focusDelta);
    }

    private <AH extends AssignmentHolderType> boolean hasNameDelta(LensFocusContext<AH> focusContext) throws SchemaException {
        ObjectDelta<AH> focusDelta = focusContext.getDelta();
        return focusDelta != null && hasNameDelta(focusDelta);
    }

    private <AH extends AssignmentHolderType> boolean hasNameDelta(ObjectDelta<AH> focusDelta) {
        PropertyDelta<Object> nameDelta = focusDelta.findPropertyDelta(FocusType.F_NAME);
        return nameDelta != null;
    }

    /**
     * Remove the intermediate results of values processing such as secondary deltas.
     */
    private <AH extends AssignmentHolderType> void cleanupContext(LensFocusContext<AH> focusContext) throws SchemaException, ConfigurationException {
        // We must NOT clean up activation computation. This has happened before, it will not happen again
        // and it does not depend on iteration
        LOGGER.trace("Cleaning up focus context");
        focusContext.setProjectionWaveSecondaryDelta(null);

        focusContext.clearIntermediateResults();
        focusContext.recompute();
    }

//    private <F extends FocusType> void processActivationBeforeAssignments(LensContext<F> context, XMLGregorianCalendar now,
//            OperationResult result)
//            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, PolicyViolationException {
//        processActivationBasic(context, now, result);
//    }
//
//    private <F extends FocusType> void processActivationAfterAssignments(LensContext<F> context, XMLGregorianCalendar now,
//            OperationResult result)
//            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, PolicyViolationException {
//        processActivationBasic(context, now, result);
//        processAssignmentActivation(context, now, result);
//    }
//
//    private <F extends FocusType> void processActivationBasic(LensContext<F> context, XMLGregorianCalendar now,
//            OperationResult result)
//            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, PolicyViolationException {
//        LensFocusContext<F> focusContext = context.getFocusContext();
//
//        if (focusContext.isDelete()) {
//            LOGGER.trace("Skipping processing of focus activation: focus delete");
//            return;
//        }
//
//        processActivationAdministrativeAndValidity(focusContext, now, result);
//
//        if (focusContext.canRepresent(UserType.class)) {
//            processActivationLockout((LensFocusContext<UserType>) focusContext, now, result);
//        }
//    }
//
//    private <F extends FocusType> void processActivationAdministrativeAndValidity(LensFocusContext<F> focusContext, XMLGregorianCalendar now,
//            OperationResult result)
//            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, PolicyViolationException {
//
//        TimeIntervalStatusType validityStatusNew = null;
//        TimeIntervalStatusType validityStatusCurrent = null;
//        XMLGregorianCalendar validityChangeTimestamp = null;
//
//        String lifecycleStateNew = null;
//        String lifecycleStateCurrent = null;
//        ActivationType activationNew = null;
//        ActivationType activationCurrent = null;
//
//        PrismObject<F> focusNew = focusContext.getObjectNew();
//        if (focusNew != null) {
//            F focusTypeNew = focusNew.asObjectable();
//            activationNew = focusTypeNew.getActivation();
//            if (activationNew != null) {
//                validityStatusNew = activationComputer.getValidityStatus(activationNew, now);
//                validityChangeTimestamp = activationNew.getValidityChangeTimestamp();
//            }
//            lifecycleStateNew = focusTypeNew.getLifecycleState();
//        }
//
//        PrismObject<F> focusCurrent = focusContext.getObjectCurrent();
//        if (focusCurrent != null) {
//            F focusCurrentType = focusCurrent.asObjectable();
//            activationCurrent = focusCurrentType.getActivation();
//            if (activationCurrent != null) {
//                validityStatusCurrent = activationComputer.getValidityStatus(activationCurrent, validityChangeTimestamp);
//            }
//            lifecycleStateCurrent = focusCurrentType.getLifecycleState();
//        }
//
//        if (validityStatusCurrent == validityStatusNew) {
//            // No change, (almost) no work
//            if (validityStatusNew != null && activationNew.getValidityStatus() == null) {
//                // There was no validity change. But the status is not recorded. So let's record it so it can be used in searches.
//                recordValidityDelta(focusContext, validityStatusNew, now);
//            } else {
//                LOGGER.trace("Skipping validity processing because there was no change ({} -> {})", validityStatusCurrent, validityStatusNew);
//            }
//        } else {
//            LOGGER.trace("Validity change {} -> {}", validityStatusCurrent, validityStatusNew);
//            recordValidityDelta(focusContext, validityStatusNew, now);
//        }
//
//        LifecycleStateModelType lifecycleModel = focusContext.getLifecycleModel();
//        ActivationStatusType effectiveStatusNew = activationComputer.getEffectiveStatus(lifecycleStateNew, activationNew, validityStatusNew, lifecycleModel);
//        ActivationStatusType effectiveStatusCurrent = activationComputer.getEffectiveStatus(lifecycleStateCurrent, activationCurrent, validityStatusCurrent, lifecycleModel);
//
//        if (effectiveStatusCurrent == effectiveStatusNew) {
//            // No change, (almost) no work
//            if (effectiveStatusNew != null && (activationNew == null || activationNew.getEffectiveStatus() == null)) {
//                // There was no effective status change. But the status is not recorded. So let's record it so it can be used in searches.
//                recordEffectiveStatusDelta(focusContext, effectiveStatusNew, now);
//            } else {
//                if (focusContext.getPrimaryDelta() != null && focusContext.getPrimaryDelta().hasItemDelta(SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS)) {
//                    LOGGER.trace("Forcing effective status delta even though there was no change ({} -> {}) because there is explicit administrativeStatus delta", effectiveStatusCurrent, effectiveStatusNew);
//                    // We need this to force the change down to the projections later in the activation processor
//                    // some of the mappings will use effectiveStatus as a source, therefore there has to be a delta for the mapping to work correctly
//                    recordEffectiveStatusDelta(focusContext, effectiveStatusNew, now);
//                } else {
//                    //check computed effective status current with the saved one - e.g. there can be some inconsistencies so we need to check and force the change.. in other cases, effectvie status will be stored with
//                    // incorrect value. Maybe another option is to not compute effectiveStatusCurrent if there is an existing (saved) effective status in the user.. TODO
//                    if (activationCurrent != null && activationCurrent.getEffectiveStatus() != null) {
//                        ActivationStatusType effectiveStatusSaved = activationCurrent.getEffectiveStatus();
//                        if (effectiveStatusSaved != effectiveStatusNew) {
//                            recordEffectiveStatusDelta(focusContext, effectiveStatusNew, now);
//                        }
//                    }
//                    LOGGER.trace("Skipping effective status processing because there was no change ({} -> {})", effectiveStatusCurrent, effectiveStatusNew);
//                }
//            }
//        } else {
//            LOGGER.trace("Effective status change {} -> {}", effectiveStatusCurrent, effectiveStatusNew);
//            recordEffectiveStatusDelta(focusContext, effectiveStatusNew, now);
//        }
//
//
//    }
//
//    private <F extends FocusType> void processActivationLockout(LensFocusContext<UserType> focusContext, XMLGregorianCalendar now,
//            OperationResult result)
//            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, PolicyViolationException {
//
//        ObjectDelta<UserType> focusPrimaryDelta = focusContext.getPrimaryDelta();
//        if (focusPrimaryDelta != null) {
//            PropertyDelta<LockoutStatusType> lockoutStatusDelta = focusContext.getPrimaryDelta().findPropertyDelta(SchemaConstants.PATH_ACTIVATION_LOCKOUT_STATUS);
//            if (lockoutStatusDelta != null) {
//                if (lockoutStatusDelta.isAdd()) {
//                    for (PrismPropertyValue<LockoutStatusType> pval: lockoutStatusDelta.getValuesToAdd()) {
//                        if (pval.getValue() == LockoutStatusType.LOCKED) {
//                            throw new SchemaException("Lockout status cannot be changed to LOCKED value");
//                        }
//                    }
//                } else if (lockoutStatusDelta.isReplace()) {
//                    for (PrismPropertyValue<LockoutStatusType> pval: lockoutStatusDelta.getValuesToReplace()) {
//                        if (pval.getValue() == LockoutStatusType.LOCKED) {
//                            throw new SchemaException("Lockout status cannot be changed to LOCKED value");
//                        }
//                    }
//                }
//            }
//        }
//
//        ActivationType activationNew = null;
//        ActivationType activationCurrent = null;
//
//        LockoutStatusType lockoutStatusNew = null;
//        LockoutStatusType lockoutStatusCurrent = null;
//
//        PrismObject<UserType> focusNew = focusContext.getObjectNew();
//        if (focusNew != null) {
//            activationNew = focusNew.asObjectable().getActivation();
//            if (activationNew != null) {
//                lockoutStatusNew = activationNew.getLockoutStatus();
//            }
//        }
//
//        PrismObject<UserType> focusCurrent = focusContext.getObjectCurrent();
//        if (focusCurrent != null) {
//            activationCurrent = focusCurrent.asObjectable().getActivation();
//            if (activationCurrent != null) {
//                lockoutStatusCurrent = activationCurrent.getLockoutStatus();
//            }
//        }
//
//        if (lockoutStatusNew == lockoutStatusCurrent) {
//            // No change, (almost) no work
//            LOGGER.trace("Skipping lockout processing because there was no change ({} -> {})", lockoutStatusCurrent, lockoutStatusNew);
//            return;
//        }
//
//        LOGGER.trace("Lockout change {} -> {}", lockoutStatusCurrent, lockoutStatusNew);
//
//        if (lockoutStatusNew == LockoutStatusType.NORMAL) {
//
//            CredentialsType credentialsTypeNew = focusNew.asObjectable().getCredentials();
//            if (credentialsTypeNew != null) {
//                resetFailedLogins(focusContext, credentialsTypeNew.getPassword(), SchemaConstants.PATH_CREDENTIALS_PASSWORD_FAILED_LOGINS);
//                resetFailedLogins(focusContext, credentialsTypeNew.getNonce(), SchemaConstants.PATH_CREDENTIALS_NONCE_FAILED_LOGINS);
//                resetFailedLogins(focusContext, credentialsTypeNew.getSecurityQuestions(), SchemaConstants.PATH_CREDENTIALS_SECURITY_QUESTIONS_FAILED_LOGINS);
//            }
//
//            if (activationNew != null && activationNew.getLockoutExpirationTimestamp() != null) {
//                PrismContainerDefinition<ActivationType> activationDefinition = getActivationDefinition();
//                PrismPropertyDefinition<XMLGregorianCalendar> lockoutExpirationTimestampDef = activationDefinition.findPropertyDefinition(ActivationType.F_LOCKOUT_EXPIRATION_TIMESTAMP);
//                PropertyDelta<XMLGregorianCalendar> lockoutExpirationTimestampDelta
//                        = lockoutExpirationTimestampDef.createEmptyDelta(ItemPath.create(UserType.F_ACTIVATION, ActivationType.F_LOCKOUT_EXPIRATION_TIMESTAMP));
//                lockoutExpirationTimestampDelta.setValueToReplace();
//                focusContext.swallowToProjectionWaveSecondaryDelta(lockoutExpirationTimestampDelta);
//            }
//        }
//
//    }
//
//    private void resetFailedLogins(LensFocusContext<UserType> focusContext, AbstractCredentialType credentialTypeNew, ItemPath path) throws SchemaException{
//        if (credentialTypeNew != null) {
//            Integer failedLogins = credentialTypeNew.getFailedLogins();
//            if (failedLogins != null && failedLogins != 0) {
//                PrismPropertyDefinition<Integer> failedLoginsDef = getFailedLoginsDefinition();
//                PropertyDelta<Integer> failedLoginsDelta = failedLoginsDef.createEmptyDelta(path);
//                failedLoginsDelta.setValueToReplace(prismContext.itemFactory().createPropertyValue(0, OriginType.USER_POLICY, null));
//                focusContext.swallowToProjectionWaveSecondaryDelta(failedLoginsDelta);
//            }
//        }
//    }
//
//    private <F extends ObjectType> void recordValidityDelta(LensFocusContext<F> focusContext, TimeIntervalStatusType validityStatusNew,
//            XMLGregorianCalendar now) throws SchemaException {
//        PrismContainerDefinition<ActivationType> activationDefinition = getActivationDefinition();
//
//        PrismPropertyDefinition<TimeIntervalStatusType> validityStatusDef = activationDefinition.findPropertyDefinition(ActivationType.F_VALIDITY_STATUS);
//        PropertyDelta<TimeIntervalStatusType> validityStatusDelta
//                = validityStatusDef.createEmptyDelta(ItemPath.create(UserType.F_ACTIVATION, ActivationType.F_VALIDITY_STATUS));
//        if (validityStatusNew == null) {
//            validityStatusDelta.setValueToReplace();
//        } else {
//            validityStatusDelta.setValueToReplace(prismContext.itemFactory().createPropertyValue(validityStatusNew, OriginType.USER_POLICY, null));
//        }
//        focusContext.swallowToProjectionWaveSecondaryDelta(validityStatusDelta);
//
//        PrismPropertyDefinition<XMLGregorianCalendar> validityChangeTimestampDef = activationDefinition.findPropertyDefinition(ActivationType.F_VALIDITY_CHANGE_TIMESTAMP);
//        PropertyDelta<XMLGregorianCalendar> validityChangeTimestampDelta
//                = validityChangeTimestampDef.createEmptyDelta(ItemPath.create(UserType.F_ACTIVATION, ActivationType.F_VALIDITY_CHANGE_TIMESTAMP));
//        validityChangeTimestampDelta.setValueToReplace(prismContext.itemFactory().createPropertyValue(now, OriginType.USER_POLICY, null));
//        focusContext.swallowToProjectionWaveSecondaryDelta(validityChangeTimestampDelta);
//    }
//
//    private <F extends ObjectType> void recordEffectiveStatusDelta(LensFocusContext<F> focusContext,
//            ActivationStatusType effectiveStatusNew, XMLGregorianCalendar now)
//            throws SchemaException {
//        PrismContainerDefinition<ActivationType> activationDefinition = getActivationDefinition();
//
//        // We always want explicit delta for effective status even if there is no real change
//        // we want to propagate enable/disable events to all the resources, even if we are enabling
//        // already enabled user (some resources may be disabled)
//        // This may produce duplicate delta, but that does not matter too much. The duplicate delta
//        // will be filtered out later.
//        PrismPropertyDefinition<ActivationStatusType> effectiveStatusDef = activationDefinition.findPropertyDefinition(ActivationType.F_EFFECTIVE_STATUS);
//        PropertyDelta<ActivationStatusType> effectiveStatusDelta
//                = effectiveStatusDef.createEmptyDelta(PATH_ACTIVATION_EFFECTIVE_STATUS);
//        effectiveStatusDelta.setValueToReplace(prismContext.itemFactory().createPropertyValue(effectiveStatusNew, OriginType.USER_POLICY, null));
//        if (!focusContext.alreadyHasDelta(effectiveStatusDelta)){
//            focusContext.swallowToProjectionWaveSecondaryDelta(effectiveStatusDelta);
//        }
//
//        // It is not enough to check alreadyHasDelta(). The change may happen in previous waves
//        // and the secondary delta may no longer be here. When it comes to disableTimestamp we even
//        // cannot rely on natural filtering of already executed deltas as the timestamp here may
//        // be off by several milliseconds. So explicitly check for the change here.
//        PrismObject<F> objectCurrent = focusContext.getObjectCurrent();
//        if (objectCurrent != null) {
//            PrismProperty<ActivationStatusType> effectiveStatusPropCurrent = objectCurrent.findProperty(PATH_ACTIVATION_EFFECTIVE_STATUS);
//            if (effectiveStatusPropCurrent != null && effectiveStatusNew.equals(effectiveStatusPropCurrent.getRealValue())) {
//                LOGGER.trace("Skipping setting disableTimestamp because there was no change");
//                return;
//            }
//        }
//
//        PropertyDelta<XMLGregorianCalendar> timestampDelta = LensUtil.createActivationTimestampDelta(effectiveStatusNew, now, activationDefinition, OriginType.USER_POLICY,
//                prismContext);
//        if (!focusContext.alreadyHasDelta(timestampDelta)) {
//            focusContext.swallowToProjectionWaveSecondaryDelta(timestampDelta);
//        }
//    }
//
//
//    private PrismContainerDefinition<ActivationType> getActivationDefinition() {
//        if (activationDefinition == null) {
//            ComplexTypeDefinition focusDefinition = prismContext.getSchemaRegistry().findComplexTypeDefinition(FocusType.COMPLEX_TYPE);
//            activationDefinition = focusDefinition.findContainerDefinition(FocusType.F_ACTIVATION);
//        }
//        return activationDefinition;
//    }
//
//    private PrismPropertyDefinition<Integer> getFailedLoginsDefinition() {
//        if (failedLoginsDefinition == null) {
//            PrismObjectDefinition<UserType> userDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
//            failedLoginsDefinition = userDef.findPropertyDefinition(SchemaConstants.PATH_CREDENTIALS_PASSWORD_FAILED_LOGINS);
//        }
//        return failedLoginsDefinition;
//    }

    /**
     * Adds deltas for iteration and iterationToken to the focus if needed.
     */
    private <AH extends AssignmentHolderType> void addIterationTokenDeltas(LensFocusContext<AH> focusContext, int iteration, String iterationToken) throws SchemaException {
        PrismObject<AH> objectCurrent = focusContext.getObjectCurrent();
        if (objectCurrent != null) {
            Integer iterationOld = objectCurrent.asObjectable().getIteration();
            String iterationTokenOld = objectCurrent.asObjectable().getIterationToken();
            if (iterationOld != null && iterationOld == iteration &&
                    iterationTokenOld != null && iterationTokenOld.equals(iterationToken)) {
                // Already stored
                return;
            }
        }
        PrismObjectDefinition<AH> objDef = focusContext.getObjectDefinition();

        PrismPropertyValue<Integer> iterationVal = prismContext.itemFactory().createPropertyValue(iteration);
        iterationVal.setOriginType(OriginType.USER_POLICY);
        PropertyDelta<Integer> iterationDelta = prismContext.deltaFactory().property().createReplaceDelta(objDef,
                FocusType.F_ITERATION, iterationVal);
        focusContext.swallowToSecondaryDelta(iterationDelta);

        PrismPropertyValue<String> iterationTokenVal = prismContext.itemFactory().createPropertyValue(iterationToken);
        iterationTokenVal.setOriginType(OriginType.USER_POLICY);
        PropertyDelta<String> iterationTokenDelta = prismContext.deltaFactory().property().createReplaceDelta(objDef,
                FocusType.F_ITERATION_TOKEN, iterationTokenVal);
        focusContext.swallowToSecondaryDelta(iterationTokenDelta);

    }

    private <F extends ObjectType> void checkArchetypeRefDelta(LensContext<F> context) throws PolicyViolationException {
        ObjectDelta<F> focusPrimaryDelta = context.getFocusContext().getPrimaryDelta();
        if (focusPrimaryDelta != null) {
            ReferenceDelta archetypeRefDelta = focusPrimaryDelta.findReferenceModification(AssignmentHolderType.F_ARCHETYPE_REF);
            if (archetypeRefDelta != null) {
                // We want to allow this under special circumstances. E.g. we want be able to import user with archetypeRef.
                // Otherwise we won't be able to export a user and re-import it again.
                if (focusPrimaryDelta.isAdd()) {
                    String archetypeOidFromAssignments = LensUtil.determineExplicitArchetypeOidFromAssignments(focusPrimaryDelta.getObjectToAdd());
                    if (archetypeOidFromAssignments == null) {
                        throw new PolicyViolationException("Attempt add archetypeRef without a matching assignment");
                    } else {
                        boolean match = true;
                        for (PrismReferenceValue archetypeRefDeltaVal : archetypeRefDelta.getValuesToAdd()) {
                            if (!archetypeOidFromAssignments.equals(archetypeRefDeltaVal.getOid())) {
                                match = false;
                            }
                        }
                        if (match) {
                            return;
                        } else {
                            throw new PolicyViolationException("Attempt add archetypeRef that does not match assignment");
                        }
                    }
                }
                throw new PolicyViolationException("Attempt to modify archetypeRef directly");
            }
        }
    }

//    private <F extends FocusType> void processAssignmentActivation(LensContext<F> context, XMLGregorianCalendar now,
//            OperationResult result) throws SchemaException {
//        DeltaSetTriple<EvaluatedAssignmentImpl<?>> evaluatedAssignmentTriple = context.getEvaluatedAssignmentTriple();
//        if (evaluatedAssignmentTriple == null) {
//            // Code path that should not normally happen. But is used in some tests and may
//            // happen during partial processing.
//            return;
//        }
//        // We care only about existing assignments here. New assignments will be taken care of in the executor
//        // (OperationalDataProcessor). And why care about deleted assignments?
//        Collection<EvaluatedAssignmentImpl<?>> zeroSet = evaluatedAssignmentTriple.getZeroSet();
//        if (zeroSet == null) {
//            return;
//        }
//        LensFocusContext<F> focusContext = context.getFocusContext();
//        for (EvaluatedAssignmentImpl<?> evaluatedAssignment: zeroSet) {
//            if (evaluatedAssignment.isVirtual()) {
//                continue;
//            }
//            AssignmentType assignmentType = evaluatedAssignment.getAssignmentType();
//            ActivationType currentActivationType = assignmentType.getActivation();
//            ActivationStatusType expectedEffectiveStatus = activationComputer.getEffectiveStatus(assignmentType.getLifecycleState(), currentActivationType, null);
//            if (currentActivationType == null) {
//                PrismContainerDefinition<ActivationType> activationDef = focusContext.getObjectDefinition().findContainerDefinition(SchemaConstants.PATH_ASSIGNMENT_ACTIVATION);
//                ContainerDelta<ActivationType> activationDelta = activationDef.createEmptyDelta(
//                        ItemPath.create(FocusType.F_ASSIGNMENT, assignmentType.getId(), AssignmentType.F_ACTIVATION));
//                ActivationType newActivationType = new ActivationType();
//                activationDelta.setValuesToReplace(newActivationType.asPrismContainerValue());
//                newActivationType.setEffectiveStatus(expectedEffectiveStatus);
//                focusContext.swallowToSecondaryDelta(activationDelta);
//            } else {
//                ActivationStatusType currentEffectiveStatus = currentActivationType.getEffectiveStatus();
//                if (!expectedEffectiveStatus.equals(currentEffectiveStatus)) {
//                    PrismPropertyDefinition<ActivationStatusType> effectiveStatusPropertyDef = focusContext.getObjectDefinition().findPropertyDefinition(SchemaConstants.PATH_ASSIGNMENT_ACTIVATION_EFFECTIVE_STATUS);
//                    PropertyDelta<ActivationStatusType> effectiveStatusDelta = effectiveStatusPropertyDef.createEmptyDelta(
//                            ItemPath.create(FocusType.F_ASSIGNMENT, assignmentType.getId(), AssignmentType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS));
//                    effectiveStatusDelta.setRealValuesToReplace(expectedEffectiveStatus);
//                    focusContext.swallowToSecondaryDelta(effectiveStatusDelta);
//                }
//            }
//        }
//    }


}
