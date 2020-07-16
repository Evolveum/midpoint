/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus;

import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.jetbrains.annotations.NotNull;

/**
 * Helps AssignmentHolderProcessor with iteration-related activities.
 * 1. keeps iteration state
 * 2. evaluates pre/post and uniqueness conditions (including name presence)
 * 3. manages fetching and storing of iteration information from/to lens context
 *
 * And nothing more.
 */
class IterationHelper<AH extends AssignmentHolderType> {

    private static final Trace LOGGER = TraceManager.getTrace(IterationHelper.class);

    @NotNull private final AssignmentHolderProcessor assignmentHolderProcessor;
    @NotNull private final LensContext<AH> context;
    @NotNull private final LensFocusContext<AH> focusContext;

    /**
     * Current iteration specification. Might change during the processing!
     */
    private IterationSpecificationType iterationSpecification;

    /**
     * We initialize iteration specification lazily. So this is the flag.
     */
    private boolean iterationSpecificationInitialized;

    /**
     * Currently we allow only a single reset when iteration specification changes
     * (to avoid endless loops, perhaps).
     */
    private boolean wasResetOnIterationSpecificationChange;

    /**
     * Current iteration number.
     */
    private int iteration;

    /**
     * Current iteration token.
     */
    private String iterationToken;

    /**
     * We allow only single reset of iteration counter (except when iteration specification
     * changes). So this is the flag.
     */
    private boolean wasResetIterationCounter;

    /**
     * Upper iterations limit.
     */
    private int maxIterations;

    /**
     * Lazily evaluated pre-iteration variables.
     */
    private ExpressionVariables variablesPreIteration;

    /**
     * Message about re-iteration reason.
     */
    private String reIterationReason;

    private static final boolean RESET_ON_RENAME = true; // make configurable some day

    IterationHelper(@NotNull AssignmentHolderProcessor assignmentHolderProcessor, @NotNull LensContext<AH> context,
            @NotNull LensFocusContext<AH> focusContext) {
        this.assignmentHolderProcessor = assignmentHolderProcessor;
        this.context = context;
        this.focusContext = focusContext;
        iteration = focusContext.getIteration();
        iterationToken = focusContext.getIterationToken();
        PrismObject<AH> focusCurrent = focusContext.getObjectCurrent();
        if (focusCurrent != null && iterationToken == null) {
            Integer focusIteration = focusCurrent.asObjectable().getIteration();
            if (focusIteration != null) {
                iteration = focusIteration;
            }
            iterationToken = focusCurrent.asObjectable().getIterationToken();
        }
    }

    void onIterationStart(Task task, OperationResult result) throws CommunicationException, ObjectNotFoundException,
            SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        reIterationReason = null;
        initializeIterationSpecificationIfNeeded();
        variablesPreIteration = null;
        computeIterationTokenIfNeeded(task, result);
        rememberIterationToken();
        LOGGER.trace("Focus {} processing, iteration {}, token '{}'", focusContext.getHumanReadableName(), iteration, iterationToken);
    }

    private void computeIterationTokenIfNeeded(Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        if (iterationToken == null) {
            createVariablesPreIterationIfNeeded();
            iterationToken = LensUtil.formatIterationToken(context, focusContext,
                    iterationSpecification, iteration, assignmentHolderProcessor.getExpressionFactory(), variablesPreIteration, task, result);
        }
    }

    private void rememberIterationToken() {
        focusContext.setIteration(iteration);
        focusContext.setIterationToken(iterationToken);
    }

    private void initializeIterationSpecificationIfNeeded() {
        if (!iterationSpecificationInitialized) {
            ObjectTemplateType objectTemplate = context.getFocusTemplate();
            iterationSpecification = LensUtil.getIterationSpecification(objectTemplate);
            maxIterations = LensUtil.determineMaxIterations(iterationSpecification);
            LOGGER.trace("maxIterations = {}, iteration specification = {} derived from template {}", maxIterations,
                    iterationSpecification, objectTemplate);
            iterationSpecificationInitialized = true;
        }
    }

    private void createVariablesPreIterationIfNeeded() {
        if (variablesPreIteration == null) {
            variablesPreIteration = ModelImplUtils.getDefaultExpressionVariables(focusContext.getObjectNew(),
                    null, null, null, context.getSystemConfiguration(), focusContext, assignmentHolderProcessor.getPrismContext());
        }
    }

    boolean doesPreIterationConditionHold(Task task, OperationResult result) throws CommunicationException,
            ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        if (iterationSpecification != null) {
            createVariablesPreIterationIfNeeded();
            if (!LensUtil.evaluateIterationCondition(context, focusContext, iterationSpecification, iteration,
                    iterationToken, true, assignmentHolderProcessor.getExpressionFactory(), variablesPreIteration, task, result)) {
                reIterationReason = "pre-iteration condition was false";
                LOGGER.debug("Skipping iteration {}, token '{}' for {} because the pre-iteration condition was false",
                        iteration, iterationToken, focusContext.getHumanReadableName());
                return false;
            }
        }
        return true;
    }

    private void resetOnIterationSpecificationChange() {
        iteration = 0;
        iterationToken = null;
        wasResetOnIterationSpecificationChange = true;
        wasResetIterationCounter = false;
        iterationSpecificationInitialized = false;
        LOGGER.trace("Resetting iteration counter and token because of iteration specification change");
    }

    private void resetOnRename() {
        iteration = 0;
        iterationToken = null;
        wasResetIterationCounter = true;
        LOGGER.trace("Resetting iteration counter and token because rename was detected");
    }

    private void resetOnConflict() {
        iteration = 0;
        iterationToken = null;
        wasResetIterationCounter = true;
        LOGGER.trace("Resetting iteration counter and token after conflict");
    }

    boolean didIterationSpecificationChange() {
        if (wasResetOnIterationSpecificationChange) {
            return false; // We won't reset on spec change twice.
        }
        IterationSpecificationType newIterationSpecification = context.getFocusTemplate() != null ?
                context.getFocusTemplate().getIterationSpecification() : null;
        if (java.util.Objects.equals(iterationSpecification, newIterationSpecification)) {
            return false;
        } else {
            resetOnIterationSpecificationChange();
            return true;
        }
    }

    boolean isIterationOk(PrismObject<AH> objectNew, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        checkNamePresence(context, this, objectNew);
        FocusConstraintsChecker<AH> checker = createChecker(context);
        if (!shouldCheckConstraints() || checker.check(objectNew, result)) {
            LOGGER.trace("Current focus satisfies uniqueness constraints. Iteration {}, token '{}'", iteration, iterationToken);
            ExpressionVariables variablesPostIteration = ModelImplUtils.getDefaultExpressionVariables(objectNew,
                    null, null, null, context.getSystemConfiguration(), focusContext, assignmentHolderProcessor.getPrismContext());
            if (LensUtil.evaluateIterationCondition(context, focusContext,
                    iterationSpecification, iteration, iterationToken, false, assignmentHolderProcessor.getExpressionFactory(), variablesPostIteration,
                    task, result)) {
                return true;
            } else {
                reIterationReason = "post-iteration condition was false";
                LOGGER.debug("Skipping iteration {}, token '{}' for {} because the post-iteration condition was false",
                        iteration, iterationToken, focusContext.getHumanReadableName());
                return false;
            }
        } else {
            LOGGER.trace("Current focus does not satisfy constraints. Conflicting object: {}; iteration={}, maxIterations={}",
                    checker.getConflictingObject(), iteration, maxIterations);
            reIterationReason = checker.getMessages();
            return false;
        }
    }

    @NotNull
    private FocusConstraintsChecker<AH> createChecker(LensContext<AH> context) {
        FocusConstraintsChecker<AH> checker = new FocusConstraintsChecker<>();
        checker.setPrismContext(assignmentHolderProcessor.getPrismContext());
        checker.setContext(context);
        checker.setRepositoryService(assignmentHolderProcessor.getCacheRepositoryService());
        checker.setCacheConfigurationManager(assignmentHolderProcessor.getCacheConfigurationManager());
        return checker;
    }

    private boolean shouldCheckConstraints() throws SchemaException {
        ConstraintsCheckingStrategyType strategy = context.getFocusConstraintsCheckingStrategy();
        boolean skipWhenNoChange = strategy != null && Boolean.TRUE.equals(strategy.isSkipWhenNoChange());
        boolean skipWhenNoIteration = strategy != null && Boolean.TRUE.equals(strategy.isSkipWhenNoIteration());

        if (skipWhenNoChange && !hasNameDelta()) {
            LOGGER.trace("Skipping constraints check because 'skipWhenNoChange' is true and there's no name delta");
            return false;
        } else if (skipWhenNoIteration && maxIterations == 0) {
            LOGGER.trace("Skipping constraints check because 'skipWhenNoIteration' is true and there is no iteration defined");
            return false;
        } else if (TaskType.class == focusContext.getObjectTypeClass()) {
            LOGGER.trace("Skipping constraints check for task, not needed because tasks names are not unique.");
            return false;
        } else {
            return true;
        }
    }

    void incrementIterationCounter() throws ObjectAlreadyExistsException {
        iteration++;
        iterationToken = null;
        LensUtil.checkMaxIterations(iteration, maxIterations, reIterationReason, focusContext.getHumanReadableName());
    }

    boolean didResetOnRenameOccur() throws SchemaException {
        if (iteration != 0 && RESET_ON_RENAME && !wasResetIterationCounter && willResetIterationCounter()) {
            // Make sure this happens only the very first time during the first recompute.
            // Otherwise it will always change the token (especially if the token expression has a random part)
            // hence the focusContext.getIterationToken() == null
            resetOnRename();
            return true;
        } else {
            return false;
        }
    }

    private boolean willResetIterationCounter() throws SchemaException {
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

    private boolean hasNameDelta() throws SchemaException {
        ObjectDelta<AH> focusDelta = focusContext.getDelta();
        return focusDelta != null && hasNameDelta(focusDelta);
    }

    private boolean hasNameDelta(ObjectDelta<AH> focusDelta) {
        PropertyDelta<Object> nameDelta = focusDelta.findPropertyDelta(FocusType.F_NAME);
        return nameDelta != null;
    }

    boolean shouldResetOnConflict() {
        if (!wasResetIterationCounter && iteration != 0) {
            resetOnConflict();
            return true;
        } else {
            return false;
        }
    }

    private void checkNamePresence(LensContext<AH> context,
            IterationHelper<AH> ctx, @NotNull PrismObject<AH> objectNew) throws NoFocusNameSchemaException {
        // Explicitly check for name. The checker would check for this also. But checking it here
        // will produce better error message
        PolyStringType objectName = objectNew.asObjectable().getName();
        if (objectName == null || objectName.getOrig().isEmpty()) {
            throw new NoFocusNameSchemaException("No name in new object " + objectName + " as produced by template " + context.getFocusTemplate() +
                    " in iteration " + ctx.iteration + ", we cannot process an object without a name");
        }
    }

    /**
     * Adds deltas for iteration and iterationToken to the focus if needed.
     */
    void createIterationTokenDeltas() throws SchemaException {
        PrismContext prismContext = assignmentHolderProcessor.getPrismContext();

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
}
