/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.mappings.predefinedActivationMapping;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

import com.evolveum.midpoint.util.exception.*;

import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractPredefinedActivationMappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceActivationDefinitionType;

/**
 * This evaluator change midpoint default behaviour and disable account.
 * Disabling the account instead of deleting is a common requirement.
 */
public class DisableInsteadOfDeleteEvaluator extends PredefinedActivationMappingEvaluator {

    private static final Trace LOGGER = TraceManager.getTrace(DisableInsteadOfDeleteEvaluator.class);

    public DisableInsteadOfDeleteEvaluator(ResourceActivationDefinitionType activationDefinitionBean) {
        super(activationDefinitionBean);
    }

    public <F extends FocusType> boolean defineExistence(LensContext<F> context, LensProjectionContext projCtx) {
        LensFocusContext<F> focusContext = context.getFocusContextRequired();
        if (focusContext.isDelete()) {
            LOGGER.trace("Focus is being deleted, returning 'false' for projection existence");
            return false;
        } else {
            LOGGER.trace("Focus is not being deleted, returning 'true' for projection existence");
            return true;
        }
    }

    @Override
    public <F extends FocusType> void defineAdministratorStatus(
            LensContext<F> context, LensProjectionContext projCtx, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        setTargetPropertyValue(
                projCtx,
                SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, ActivationStatusType.DISABLED,
                task, result);
    }

    @Override
    @Nullable AbstractPredefinedActivationMappingType getConfiguration() {
        return getActivationDefinitionBean().getDisableInsteadOfDelete();
    }

    @Override
    public <F extends FocusType> boolean isApplicable(
            LensContext<F> context, LensProjectionContext projCtx, XMLGregorianCalendar now) {
        if (Boolean.TRUE.equals(projCtx.isLegal())) { // actually, the null value should not be returned
            LOGGER.trace("Projection is legal -> not applicable");
            return false;
        } else {
            LOGGER.trace("Projection is illegal -> applicable");
            return true;
        }
    }

    @Override
    protected boolean supportsAdministratorStatus() {
        return true;
    }

    @Override
    Trace getLogger() {
        return LOGGER;
    }
}
