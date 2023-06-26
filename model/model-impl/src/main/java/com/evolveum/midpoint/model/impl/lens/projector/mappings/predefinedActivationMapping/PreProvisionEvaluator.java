/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.mappings.predefinedActivationMapping;

import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.List;

/**
 * This evaluator pre-provision account base on duration from configuration {@link PreProvisionActivationMappingType}.
 * As reference time is used value of attribute activation/validFrom from focus.
 */
public class PreProvisionEvaluator extends PredefinedActivationMappingEvaluator {

    private static final Trace LOGGER = TraceManager.getTrace(PreProvisionEvaluator.class);

    private TimeConstraintEvaluation timeEvaluation;

    public PreProvisionEvaluator(ResourceActivationDefinitionType activationDefinitionBean) {
        super(activationDefinitionBean);
    }

    @Override
    public void init() {
        super.init();
        timeEvaluation = new TimeConstraintEvaluation(
                ItemPath.create(FocusType.F_ACTIVATION, ActivationType.F_VALID_FROM),
                getActivationDefinitionBean().getPreProvision().getCreateBefore());
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

        LensFocusContext<F> focusContext = context.getFocusContext();
        if (focusContext == null) {
            return null;
        }

        if (timeEvaluation.isTimeConstraintValid() == null) {
            timeEvaluation.evaluateTo(focusContext.getObjectDeltaObjectAbsolute(), now);
        }

        if (timeEvaluation.isTimeConstraintValid()) {
            return null;
        }

        if (isExpectedValueOfItem(
                focusContext.getObjectDeltaObjectAbsolute(),
                ItemPath.create(FocusType.F_ACTIVATION, ActivationType.F_VALID_FROM),
                List.of())) {
            return timeEvaluation.getNextRecomputeTime();
        }

        return null;
    }

    @Override
    public <F extends FocusType> boolean isConfigured(Task task) {
        if (getActivationDefinitionBean().getPreProvision() == null) {
            LOGGER.trace(
                    "PreProvisionEvaluator: non-exist configuration for preProvisionAccount in: {}, skipping",
                    getActivationDefinitionBean());
            return false;
        }

        PreProvisionActivationMappingType preProvisionAccount = getActivationDefinitionBean().getPreProvision();
        if (!task.canSee(preProvisionAccount.getLifecycleState())) {
            LOGGER.trace("PreProvisionEvaluator: not applicable to the execution mode, skipping");
            return false;
        }
        return true;
    }

    @Override
    public <F extends FocusType> boolean isApplicable(
            LensContext<F> context, LensProjectionContext projCtx, XMLGregorianCalendar now)
            throws SchemaException, ConfigurationException {
        checkInitialization();

        LensFocusContext<F> focusContext = context.getFocusContext();
        if (focusContext == null) {
            LOGGER.trace("PreProvisionEvaluator: couldn't find focus context in {}, skipping", context.debugDump());
            return false;
        }

        timeEvaluation.evaluateTo(focusContext.getObjectDeltaObjectAbsolute(), now);
        if (!timeEvaluation.isTimeConstraintValid()) {
            LOGGER.trace("PreProvisionEvaluator: time constraint isn't valid, skipping");
            return false;
        }

        if (!isExpectedValueOfItem(
                focusContext.getObjectDeltaObjectAbsolute(),
                ItemPath.create(FocusType.F_ACTIVATION, ActivationType.F_VALID_FROM),
                List.of())) {
            LOGGER.trace("PreProvisionEvaluator: valid from attribute for user is empty, skipping", context.debugDump());
            return false;
        }

        return true;
    }
}
