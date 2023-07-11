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
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractPredefinedActivationMappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceActivationDefinitionType;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 * This evaluator change midpoint default behaviour and disable account.
 * Disabling the account instead of deleting is a common requirement.
 */
public class DisableInsteadOfDeleteEvaluator extends PredefinedActivationMappingEvaluator {

    private static final Trace LOGGER = TraceManager.getTrace(DisableInsteadOfDeleteEvaluator.class);

    public DisableInsteadOfDeleteEvaluator(ResourceActivationDefinitionType activationDefinitionBean) {
        super(activationDefinitionBean);
    }

    public <F extends FocusType> boolean defineExistence(final LensContext<F> context, final LensProjectionContext projCtx) {
        LensFocusContext<F> focusContext = context.getFocusContext();
        if (focusContext == null) {
            LOGGER.trace(
                    "DisableInsteadDeleteEvaluator: couldn't find focus context, return legal {} for existence",
                    projCtx.isLegal());
            return projCtx.isLegal();
        }

        if (focusContext.isDelete()) {
            LOGGER.trace("DisableInsteadDeleteEvaluator: focus is deleting, return false for existence");
            return false;
        }
        return true;
    }

    @Override
    public <V extends PrismValue, D extends ItemDefinition<?>, F extends FocusType> void defineAdministratorStatus(
            LensContext<F> context, LensProjectionContext projCtx) throws SchemaException {
        ItemDefinition<?> targetItemDefinition =
                projCtx.getObjectDefinition().findItemDefinition(SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);

        //noinspection unchecked
        ItemDelta<V, D> targetItemDelta =
                (ItemDelta<V, D>) targetItemDefinition.createEmptyDelta(SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);

        V value = (V) PrismContext.get().itemFactory().createPropertyValue(ActivationStatusType.DISABLED);
        targetItemDelta.setValuesToReplace(value);

        LOGGER.trace("DisableInsteadDeleteEvaluator adds new delta for {}: {}", projCtx, targetItemDelta);
        projCtx.swallowToSecondaryDelta(targetItemDelta);
    }

    @Override
    public <F extends FocusType> boolean isConfigured(Task task) {
        if (getActivationDefinitionBean().getDisableInsteadOfDelete() == null) {
            LOGGER.trace(
                    "DisableInsteadDeleteEvaluator: non-exist configuration for disableInsteadDelete in: {}, skipping",
                    getActivationDefinitionBean());
            return false;
        }

        AbstractPredefinedActivationMappingType disableInsteadDeleteBean = getActivationDefinitionBean().getDisableInsteadOfDelete();
        if (!task.canSee(disableInsteadDeleteBean.getLifecycleState())) {
            LOGGER.trace("DisableInsteadDeleteEvaluator: not applicable to the execution mode, skipping");
            return false;
        }
        return true;
    }

    @Override
    public <F extends FocusType> boolean isApplicable(
            LensContext<F> context, LensProjectionContext projCtx, XMLGregorianCalendar now) {
        LensFocusContext<F> focusContext = context.getFocusContext();
        if (focusContext == null) {
            LOGGER.trace("DisableInsteadDeleteEvaluator: couldn't find focus context in {}, skipping", context.debugDump());
            return false;
        }

        if (Boolean.TRUE.equals(projCtx.isLegal())) {
            LOGGER.trace("DisableInsteadDeleteEvaluator: focus is deleting, skipping");
            return false;
        }
        return true;
    }

    @Override
    protected boolean supportsAdministratorStatus() {
        return true;
    }
}
