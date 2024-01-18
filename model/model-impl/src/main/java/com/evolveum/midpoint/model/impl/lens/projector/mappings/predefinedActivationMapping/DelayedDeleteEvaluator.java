/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
 * This evaluator delayed delete base on duration from configuration {@link DelayedDeleteActivationMappingType}.
 * As reference time is used value of attribute activation/disableTimestamp from shadow.
 * We should combine with {@link DisableInsteadOfDeleteEvaluator}.
 */
public class DelayedDeleteEvaluator extends PredefinedActivationMappingEvaluator {

    private static final Trace LOGGER = TraceManager.getTrace(DelayedDeleteEvaluator.class);

    private TimeConstraintEvaluation timeEvaluation;

    public DelayedDeleteEvaluator(ResourceActivationDefinitionType activationDefinitionBean) {
        super(activationDefinitionBean);
    }

    @Override
    public void initialize() {
        super.initialize();
        timeEvaluation = new TimeConstraintEvaluation(
                ItemPath.create(FocusType.F_ACTIVATION, ActivationType.F_DISABLE_TIMESTAMP),
                getActivationDefinitionBean().getDelayedDelete().getDeleteAfter());
    }

    public <F extends FocusType> boolean defineExistence(LensContext<F> context, LensProjectionContext projCtx) {
        return false;
    }

    @Override
    public <F extends FocusType> XMLGregorianCalendar getNextRecomputeTimeForExistence(
            LensContext<F> context, LensProjectionContext projCtx, XMLGregorianCalendar now)
            throws SchemaException, ConfigurationException {
        initializeIfNeeded();

        if (!timeEvaluation.isTimeValidityEstablished()) {
            timeEvaluation.evaluateFrom(projCtx.getObjectDeltaObject(), now);
        }

        if (timeEvaluation.isTimeConstraintValid()) {
            return null;
        }

        if (isConditionSatisfied(projCtx)) {
            return timeEvaluation.getNextRecomputeTime();
        }

        return null;
    }

    @Override
    @Nullable AbstractPredefinedActivationMappingType getConfiguration() {
        return getActivationDefinitionBean().getDelayedDelete();
    }

    @Override
    public <F extends FocusType> boolean isApplicable(
            LensContext<F> context, LensProjectionContext projCtx, XMLGregorianCalendar now)
            throws SchemaException, ConfigurationException {
        initializeIfNeeded();

        timeEvaluation.evaluateFrom(projCtx.getObjectDeltaObject(), now);
        if (!timeEvaluation.isTimeConstraintValid()) {
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
