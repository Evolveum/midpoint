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
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.Nullable;

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
    public void initialize() {
        super.initialize();
        timeEvaluation = new TimeConstraintEvaluation(
                ItemPath.create(FocusType.F_ACTIVATION, ActivationType.F_VALID_FROM),
                getActivationDefinitionBean().getPreProvision().getCreateBefore());
    }

    public <F extends FocusType> boolean defineExistence(LensContext<F> context, LensProjectionContext projCtx) {
        return false;
    }

    @Override
    public <F extends FocusType> XMLGregorianCalendar getNextRecomputeTimeForExistence(
            LensContext<F> context, LensProjectionContext projCtx, XMLGregorianCalendar now)
            throws SchemaException {
        initializeIfNeeded();

        LensFocusContext<F> focusContext = context.getFocusContextRequired();

        if (!timeEvaluation.isTimeValidityEstablished()) {
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
    @Nullable
    AbstractPredefinedActivationMappingType getConfiguration() {
        return getActivationDefinitionBean().getPreProvision();
    }

    @Override
    public <F extends FocusType> boolean isApplicable(
            LensContext<F> context, LensProjectionContext projCtx, XMLGregorianCalendar now)
            throws SchemaException, ConfigurationException {
        initializeIfNeeded();

        LensFocusContext<F> focusContext = context.getFocusContextRequired();

        timeEvaluation.evaluateTo(focusContext.getObjectDeltaObjectAbsolute(), now);
        if (!timeEvaluation.isTimeConstraintValid()) {
            LOGGER.trace("Time constraint isn't valid -> not applicable");
            return false;
        }

        if (!isExpectedValueOfItem(
                focusContext.getObjectDeltaObjectAbsolute(),
                ItemPath.create(FocusType.F_ACTIVATION, ActivationType.F_VALID_FROM),
                List.of())) {
            LOGGER.trace("'Valid from' focus property is empty -> not applicable");
            return false;
        }

        return true;
    }

    @Override
    Trace getLogger() {
        return LOGGER;
    }
}
