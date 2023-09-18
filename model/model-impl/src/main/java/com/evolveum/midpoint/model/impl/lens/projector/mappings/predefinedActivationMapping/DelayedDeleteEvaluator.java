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
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

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
    public void init() {
        super.init();
        timeEvaluation = new TimeConstraintEvaluation(
                ItemPath.create(FocusType.F_ACTIVATION, ActivationType.F_DISABLE_TIMESTAMP),
                getActivationDefinitionBean().getDelayedDelete().getDeleteAfter());
    }

    public <F extends FocusType> boolean defineExistence(
            final LensContext<F> context, final LensProjectionContext projCtx) {
        return false;
    }

    @Override
    public <F extends FocusType> XMLGregorianCalendar defineTimeForTriggerOfExistence(
            LensContext<F> context, LensProjectionContext projCtx, XMLGregorianCalendar now)
            throws SchemaException, ConfigurationException {
        checkInitialization();

        if (timeEvaluation.isTimeConstraintValid() == null) {
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
    public <F extends FocusType> boolean isConfigured(Task task) {
        if (getActivationDefinitionBean().getDelayedDelete() == null) {
            LOGGER.trace(
                    "DelayedDeleteEvaluator: non-exist configuration for delayedDelete in: {}, skipping",
                    getActivationDefinitionBean());
            return false;
        }

        DelayedDeleteActivationMappingType delayedDelete = getActivationDefinitionBean().getDelayedDelete();
        if (!task.canSee(delayedDelete.getLifecycleState())) {
            LOGGER.trace("DelayedDeleteEvaluator: not applicable to the execution mode, skipping");
            return false;
        }
        return true;
    }

    @Override
    public <F extends FocusType> boolean isApplicable(
            LensContext<F> context, LensProjectionContext projCtx, XMLGregorianCalendar now)
            throws SchemaException, ConfigurationException {
        checkInitialization();

        timeEvaluation.evaluateFrom(projCtx.getObjectDeltaObject(), now);
        if (!timeEvaluation.isTimeConstraintValid()) {
            LOGGER.trace("DelayedDeleteEvaluator: time constraint isn't valid, skipping");
            return false;
        }

        if (!isConditionSatisfied(projCtx)) {
            LOGGER.trace(
                    "DelayedDeleteEvaluator: activation status isn't disabled "
                    + "or disable reason isn't mapped or deprovision, skipping");
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

        if (!isExpectedValueOfItem(
                projCtx.getObjectDeltaObject(),
                ItemPath.create(ShadowType.F_ACTIVATION, ActivationType.F_DISABLE_REASON),
                List.of(
                        SchemaConstants.MODEL_DISABLE_REASON_DEPROVISION,
                        SchemaConstants.MODEL_DISABLE_REASON_MAPPED))) {
            return false;
        }

        return true;
    }
}
