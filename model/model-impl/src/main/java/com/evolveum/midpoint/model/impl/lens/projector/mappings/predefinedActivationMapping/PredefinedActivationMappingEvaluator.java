/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.mappings.predefinedActivationMapping;

import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.projector.ActivationProcessor;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.ItemDeltaItem;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceActivationDefinitionType;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.List;

/**
 * Abstract class for predefined activation mapping evaluators.
 * Evaluator define existence value and create deltas for activation mapping. Current supported only administrator status.
 * And return time for creating of triggers.
 */
public abstract class PredefinedActivationMappingEvaluator {

    private static final Trace LOGGER = TraceManager.getTrace(ActivationProcessor.class);

    private final ResourceActivationDefinitionType activationDefinitionBean;
    private boolean initialized = false;

    protected PredefinedActivationMappingEvaluator(ResourceActivationDefinitionType activationDefinitionBean) {
        this.activationDefinitionBean = activationDefinitionBean;
    }

    void init() {
        initialized = true;
    }

    void checkInitialization(){
        if (!initialized) {
            init();
        }
    }


    protected ResourceActivationDefinitionType getActivationDefinitionBean() {
        return activationDefinitionBean;
    }

    /**
     * @return existence value for account
     */
    public abstract <F extends FocusType> boolean defineExistence(
            final LensContext<F> context, final LensProjectionContext projCtx);

    /**
     *  create delta for account activation
     * @param path define path for activation attribute of account
     */
    public <F extends FocusType> void defineActivation(
            final LensContext<F> context, final LensProjectionContext projCtx, ItemPath path) throws SchemaException {
        if (SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS.equivalent(path)) {
            LOGGER.trace("Start evaluating predefined activation mapping for administrative status");
            defineAdministratorStatus(context, projCtx);
        }
    }

    /**
     * @return time for creating of trigger for shadow
     */
    public <F extends FocusType> XMLGregorianCalendar defineTimeForTriggerOfActivation(
            final LensContext<F> context, final LensProjectionContext projCtx, ItemPath path, XMLGregorianCalendar now)
            throws SchemaException, ConfigurationException {
        if (SchemaConstants.PATH_ACTIVATION_EXISTENCE.equivalent(path)) {
            LOGGER.trace("Start define recompute time for trigger of administrative status");
            return defineTimeForTriggerOfExistence(context, projCtx, now);
        }

        if (SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS.equivalent(path)) {
            LOGGER.trace("Start define recompute time for trigger of administrative status");
            return defineTimeForTriggerOfAdministratorStatus(context, projCtx, now);
        }
        return null;
    }

    protected  <F extends FocusType> XMLGregorianCalendar defineTimeForTriggerOfExistence(
            final LensContext<F> context, final LensProjectionContext projCtx, XMLGregorianCalendar now)
            throws SchemaException, ConfigurationException {
        return null;
    }

    protected  <F extends FocusType> XMLGregorianCalendar defineTimeForTriggerOfAdministratorStatus(
            LensContext<F> context, LensProjectionContext projCtx, XMLGregorianCalendar now) {
        return null;
    }

    protected <V extends PrismValue, D extends ItemDefinition<?>, F extends FocusType> void defineAdministratorStatus(
            final LensContext<F> context, final LensProjectionContext projCtx) throws SchemaException {
    }

    /**
     * @return return true when {@link ResourceActivationDefinitionType} contains configuration for current evaluator
     */
    public abstract <F extends FocusType> boolean isConfigured(Task task);

    /**
     * @return return true when evaluator is applicable for current shadow (because of some condition, time constraint, etc.)
     */
    public abstract <F extends FocusType> boolean isApplicable(
            LensContext<F> context, LensProjectionContext projCtx, XMLGregorianCalendar now) throws SchemaException, ConfigurationException;

    /**
     * @return return true when support activation attribute defined by projectionPropertyPath
     */
    public boolean supportsActivation(ItemPath projectionPropertyPath) {
        if (SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS.equivalent(projectionPropertyPath)) {
            return supportsAdministratorStatus();
        }
        if (SchemaConstants.PATH_ACTIVATION_VALID_FROM.equivalent(projectionPropertyPath)) {
            return supportsValidFrom();
        }
        if (SchemaConstants.PATH_ACTIVATION_VALID_FROM.equivalent(projectionPropertyPath)) {
            return supportsValidTo();
        }
        if (SchemaConstants.PATH_ACTIVATION_LOCKOUT_STATUS.equivalent(projectionPropertyPath)) {
            return supportsLockoutStatus();
        }
        return false;
    }

    protected boolean supportsLockoutStatus() {
        return false;
    }

    protected boolean supportsValidTo() {
        return false;
    }

    protected boolean supportsValidFrom() {
        return false;
    }

    protected boolean supportsAdministratorStatus() {
        return false;
    }

    boolean isExpectedValueOfItem(
            ObjectDeltaObject<?> odo, ItemPath itemPath, List<Object> expectedValues)
            throws SchemaException{
        ItemDeltaItem<?, ?> idi = odo.findIdi(itemPath);
        if (idi == null) {
            return false;
        }

        Item<?, ?> item = idi.getItemNew();
        if (item == null) {
            return false;
        }

        Object bean = item.getRealValue();
        if (bean == null) {
            return false;
        }

        if (expectedValues.isEmpty()) {
            return true;
        }

        if (expectedValues.contains(bean)) {
            return true;
        }

        return false;
    }
}
