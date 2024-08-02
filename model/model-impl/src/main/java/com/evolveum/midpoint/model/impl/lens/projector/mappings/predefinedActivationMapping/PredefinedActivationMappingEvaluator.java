/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.mappings.predefinedActivationMapping;

import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.ProjectionMappingLoader;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;

import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.ItemDeltaItem;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractPredefinedActivationMappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceActivationDefinitionType;

/**
 * Abstract class for predefined activation mapping evaluators.
 * Evaluator define existence value and create deltas for activation mapping. Current supported only administrator status.
 * And return time for creating of triggers.
 */
public abstract class PredefinedActivationMappingEvaluator {

    private final ResourceActivationDefinitionType activationDefinitionBean;
    private boolean initialized = false;

    PredefinedActivationMappingEvaluator(ResourceActivationDefinitionType activationDefinitionBean) {
        this.activationDefinitionBean = activationDefinitionBean;
    }

    void initialize() {
        initialized = true;
    }

    void initializeIfNeeded() {
        if (!initialized) {
            initialize();
        }
    }

    ResourceActivationDefinitionType getActivationDefinitionBean() {
        return activationDefinitionBean;
    }

    /**
     * @return existence value for account
     */
    public abstract <F extends FocusType> boolean defineExistence(LensContext<F> context, LensProjectionContext projCtx);

    /**
     * Create a delta for specified account activation property (`path`), if needed.
     *
     * As a side effect, it may load the full shadow (to check if the value is not already there).
     *
     * @param path define path for activation attribute of account
     */
    public <F extends FocusType> void defineActivationProperty(
            LensContext<F> context, LensProjectionContext projCtx, ItemPath path, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        if (SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS.equivalent(path)) {
            getLogger().trace("Start evaluating predefined activation mapping for administrative status");
            defineAdministratorStatus(context, projCtx, task, result);
        }
    }

    /**
     * @return time for creating of trigger for shadow
     */
    public <F extends FocusType> XMLGregorianCalendar getNextRecomputeTime(
            LensContext<F> context, LensProjectionContext projCtx, ItemPath path, XMLGregorianCalendar now)
            throws SchemaException, ConfigurationException {
        if (SchemaConstants.PATH_ACTIVATION_EXISTENCE.equivalent(path)) {
            getLogger().trace("Starting determining next recompute time for existence mapping");
            return getNextRecomputeTimeForExistence(context, projCtx, now);
        }

        if (SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS.equivalent(path)) {
            getLogger().trace("Starting determining next recompute time for administrative status mapping");
            return getNextRecomputeTimeForAdministrativeStatus(context, projCtx, now); // currently doing nothing
        }
        return null;
    }

    protected <F extends FocusType> XMLGregorianCalendar getNextRecomputeTimeForExistence(
            LensContext<F> context, LensProjectionContext projCtx, XMLGregorianCalendar now)
            throws SchemaException, ConfigurationException {
        return null; // nothing here by default
    }

    private <F extends FocusType> XMLGregorianCalendar getNextRecomputeTimeForAdministrativeStatus(
            LensContext<F> ignoredContext, LensProjectionContext ignoredProjCtx, XMLGregorianCalendar ignoredNow) {
        return null; // maybe needed in the future
    }

    protected <F extends FocusType> void defineAdministratorStatus(
            LensContext<F> context, LensProjectionContext projCtx, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        // nothing here by default
    }

    /**
     * @return return true when {@link #activationDefinitionBean} contains configuration for current evaluator
     */
    public boolean isConfigured(Task task) {
        AbstractPredefinedActivationMappingType config = getConfiguration();
        if (config == null) {
            return false; // no need to log, as this will be quite common situation
        }
        if (!task.canSee(config.getLifecycleState())) {
            getLogger().trace("Not applicable to the execution mode, skipping");
            return false;
        }
        return true;
    }

    abstract @Nullable AbstractPredefinedActivationMappingType getConfiguration();

    /**
     * @return return true when evaluator is applicable for current shadow (because of some condition, time constraint, etc.)
     */
    public abstract <F extends FocusType> boolean isApplicable(
            LensContext<F> context, LensProjectionContext projCtx, XMLGregorianCalendar now) throws SchemaException, ConfigurationException;

    /**
     * @return return true when support activation attribute defined by projectionPropertyPath
     */
    public boolean supportsActivationProperty(ItemPath projectionPropertyPath) {
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

    private boolean supportsLockoutStatus() {
        return false; // for future extension
    }

    private boolean supportsValidTo() {
        return false; // for future extension
    }

    private boolean supportsValidFrom() {
        return false; // for future extension
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

        //noinspection RedundantIfStatement
        if (expectedValues.contains(bean)) {
            return true;
        }

        return false;
    }

    /**
     * Sets the property (by emitting the delta); but checking for phantom changes.
     * Loads the full shadow, if necessary!
     */
    @SuppressWarnings("SameParameterValue")
    <V extends PrismValue, D extends ItemDefinition<?>> void setTargetPropertyValue(
            LensProjectionContext projCtx, ItemPath path, Object value, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {

        loadFullShadowIfNeeded(projCtx, task, result);

        if (projCtx.isActivationLoaded()) {
            // Note that we also accept the "new" state; if there is a match, then there is no need to add a delta again.
            var object = projCtx.getObjectNewOrCurrentRequired();
            var property = object.findProperty(path);
            if (property != null && value.equals(property.getRealValue())) {
                getLogger().trace("Property {} already has the value of {}, nothing to do", path, value);
                return;
            }
        } else {
            getLogger().trace("Cannot check current value of property {}, as there's no full shadow even after loading", path);
        }

        ItemDefinition<?> targetItemDefinition = projCtx.getObjectDefinition().findItemDefinition(path);

        //noinspection unchecked
        ItemDelta<V, D> targetItemDelta = (ItemDelta<V, D>) targetItemDefinition.createEmptyDelta(path);

        //noinspection unchecked
        targetItemDelta.setValuesToReplace(
                (V) PrismContext.get().itemFactory().createPropertyValue(value));

        getLogger().trace("Adding a delta: {}", targetItemDelta);
        projCtx.swallowToSecondaryDelta(targetItemDelta);
    }

    private void loadFullShadowIfNeeded(LensProjectionContext projCtx, Task task, OperationResult result)
            throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        if (!projCtx.isActivationLoaded()) {
            var loader = new ProjectionMappingLoader(projCtx, ModelBeans.get().contextLoader, projCtx::isActivationLoaded);
            String loadReason = "target property going to be set by " + getName();
            loader.load(loadReason, task, result);
            getLogger().trace("Projection was loaded because of: {}", loadReason);
        }
    }

    abstract Trace getLogger();

    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public String toString() {
        return getName();
    }
}
