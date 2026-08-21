/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.lens.projector.mappings.predefinedActivationMapping;

import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.Nullable;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.List;

/**
 * Implements the predefined activation mapping for delayed shadow deletion.
 *
 * The evaluator postpones deletion until a configured delay has elapsed after the shadow was disabled
 * for a deprovisioning reason. It uses the shadow's {@code activation/disableTimestamp} as the reference
 * point and is intended to be used together with {@link DisableInsteadOfDeleteEvaluator}, because it only
 * triggers deletion after the account has already been disabled instead of being removed immediately.
 */
public class DelayedDeleteEvaluator extends PredefinedActivationMappingEvaluator {

    private static final Trace LOGGER = TraceManager.getTrace(DelayedDeleteEvaluator.class);

    private TimeConstraintEvaluation timeConstraintEvaluation;

    /**
     * Creates a delayed-delete evaluator for the given activation definition.
     *
     * @param activationDefinitionBean activation definition that contains the delayed-delete configuration
     */
    public DelayedDeleteEvaluator(ResourceActivationDefinitionType activationDefinitionBean) {
        super(activationDefinitionBean);
    }

    /**
     * Initializes the evaluator and prepares the time-constraint helper from the configured delay.
     */
    @Override
    public void initialize() {
        super.initialize();
        timeConstraintEvaluation = new TimeConstraintEvaluation(
                ItemPath.create(FocusType.F_ACTIVATION, ActivationType.F_DISABLE_TIMESTAMP),
                getActivationDefinitionBean().getDelayedDelete().getDeleteAfter());
    }

    /**
     * Returns the existence value used for the projection when delayed delete is active.
     *
     * Delayed delete always makes the projection non-existent once the configured delay has expired and the
     * deprovisioning condition is still satisfied.
     */
    public <F extends FocusType> boolean defineExistence(LensContext<F> context, LensProjectionContext projCtx) {
        return false;
    }

    /**
     * Computes the next recompute time for delayed delete - needed when the time did not yet come, so we have to plan
     * the future evaluation.
     *
     * @return the time when the projection should be recomputed again, or {@code null} if no trigger is needed
     */
    @Override
    public <F extends FocusType> XMLGregorianCalendar getNextRecomputeTimeForExistence(
            LensContext<F> context, LensProjectionContext projCtx, XMLGregorianCalendar now)
            throws SchemaException, ConfigurationException {
        initializeIfNeeded();

        if (!timeConstraintEvaluation.isTimeValidityEstablished()) {
            timeConstraintEvaluation.evaluateAsValidFrom(projCtx.getObjectDeltaObject(), now);
        }

        if (timeConstraintEvaluation.isTimeConstraintValid()) {
            return null;
        }

        if (isConditionSatisfied(projCtx)) {
            return timeConstraintEvaluation.getNextRecomputeTime();
        }

        return null;
    }

    /**
     * Returns the delayed-delete configuration block from the activation definition.
     */
    @Override
    @Nullable AbstractPredefinedActivationMappingType getConfiguration() {
        return getActivationDefinitionBean().getDelayedDelete();
    }

    /**
     * Checks whether delayed delete is currently applicable - both general condition and the time.
     */
    @Override
    public <F extends FocusType> boolean isApplicable(
            LensContext<F> context, LensProjectionContext projCtx, XMLGregorianCalendar now)
            throws SchemaException, ConfigurationException {
        initializeIfNeeded();

        timeConstraintEvaluation.evaluateAsValidFrom(projCtx.getObjectDeltaObject(), now);
        if (!timeConstraintEvaluation.isTimeConstraintValid()) {
            LOGGER.trace("Time constraint isn't valid -> not applicable");
            return false;
        }

        if (!isConditionSatisfied(projCtx)) {
            LOGGER.trace("Activation status isn't 'disabled' or disable reason isn't 'deprovision' -> not applicable");
            return false;
        }

        return true;
    }

    private boolean isConditionSatisfied(LensProjectionContext projCtx) throws SchemaException, ConfigurationException {
        if (!isExpectedValueOfItem(
                projCtx.getObjectDeltaObject(),
                ItemPath.create(ShadowType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS),
                List.of(ActivationStatusType.DISABLED))) {
            return false;
        }

        // We want to delete only those accounts that were disabled because of de-provisioning
        // (e.g. using disable-instead-of-delete feature). Not those that were simply deactivated.
        // See MID-9143. In the future, the list of reasons may be configurable.
        if (!isExpectedValueOfItem(
                projCtx.getObjectDeltaObject(),
                SchemaConstants.PATH_ACTIVATION_DISABLE_REASON,
                List.of(SchemaConstants.MODEL_DISABLE_REASON_DEPROVISION))) {
            return false;
        }

        return true;
    }

    @Override
    Trace getLogger() {
        return LOGGER;
    }
}
