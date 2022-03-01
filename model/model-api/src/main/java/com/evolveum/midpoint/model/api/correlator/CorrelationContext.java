/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.correlator;

import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The context of the correlation and correlator state update operations.
 * (Both work on an object being synchronized. The use in the latter case is experimental, though.)
 *
 * Created by _the caller_ of {@link Correlator#correlate(CorrelationContext, OperationResult)} method, but then updated
 * by the method implementation(s) themselves.
 *
 * Not to be confused with {@link CorrelatorContext} which describes the context of the whole {@link Correlator} lifespan.
 */
public class CorrelationContext implements DebugDumpable, Cloneable {

    /**
     * Shadowed resource object to be correlated.
     */
    @NotNull private final ShadowType resourceObject;

    /**
     * Focus that was created using pre-mappings.
     * May be empty (but not null) e.g. if there are no such mappings.
     */
    @NotNull private final FocusType preFocus;

    /**
     * Resource on which the correlated shadow resides.
     */
    @NotNull private final ResourceType resource;

    /**
     * Resource object type definition (~ schemaHandling section).
     */
    @NotNull private final ResourceObjectTypeDefinition objectTypeDefinition;

    /**
     * System configuration to use during the correlation.
     */
    @Nullable private final SystemConfigurationType systemConfiguration;

    /**
     * Task in which the correlation takes place.
     */
    @NotNull private final Task task;

    /**
     * Information about the current state of the correlator used.
     * Usually provided by upstream (parent) correlator.
     */
    private AbstractCorrelatorStateType correlatorState;

    /**
     * User scripts can request manual correlation here.
     * TODO adapt / remove
     */
    @NotNull private final ManualCorrelationContext manualCorrelationContext = new ManualCorrelationContext();

    public CorrelationContext(
            @NotNull ShadowType resourceObject,
            @NotNull FocusType preFocus,
            @NotNull ResourceType resource,
            @NotNull ResourceObjectTypeDefinition objectTypeDefinition,
            @Nullable SystemConfigurationType systemConfiguration,
            @NotNull Task task) {
        this.resourceObject = resourceObject;
        this.preFocus = preFocus;
        this.resource = resource;
        this.objectTypeDefinition = objectTypeDefinition;
        this.systemConfiguration = systemConfiguration;
        this.task = task;
    }

    public @NotNull ShadowType getResourceObject() {
        return resourceObject;
    }

    public @NotNull FocusType getPreFocus() {
        return preFocus;
    }

    public @NotNull Class<? extends ObjectType> getFocusType() {
        return preFocus.getClass();
    }

    public @NotNull ResourceType getResource() {
        return resource;
    }

    public @NotNull ResourceObjectTypeDefinition getObjectTypeDefinition() {
        return objectTypeDefinition;
    }

    public @Nullable SystemConfigurationType getSystemConfiguration() {
        return systemConfiguration;
    }

    public AbstractCorrelatorStateType getCorrelatorState() {
        return correlatorState;
    }

    public void setCorrelatorState(AbstractCorrelatorStateType correlatorState) {
        this.correlatorState = correlatorState;
    }

    public @NotNull ManualCorrelationContext getManualCorrelationContext() {
        return manualCorrelationContext;
    }

    /**
     * Instructs the correlator that the manual correlation should be carried out. If there's only one option,
     * an error should be signalled.
     */
    @SuppressWarnings("unused") // called from scripts
    public void requestManualCorrelation() {
        manualCorrelationContext.setRequested(true);
    }

    /**
     * Instructs the correlator that the manual correlation should be carried out. Provides explicit list of potential matches
     * to display.
     *
     * If there's only one option, an error should be signalled.
     */
    @SuppressWarnings("unused") // called from scripts
    public void requestManualCorrelation(List<ResourceObjectOwnerOptionType> potentialMatches) {
        manualCorrelationContext.setRequested(true);
        manualCorrelationContext.setPotentialMatches(potentialMatches);
    }

    public @NotNull Task getTask() {
        return task;
    }

    @Override
    public String toString() {
        return "CorrelationContext("
                + getFocusType().getSimpleName() + ", "
                + objectTypeDefinition.getHumanReadableName() + "@" + resource
                + ')';
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "resourceObject", resourceObject, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "preFocus", preFocus, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "focusType", getFocusType(), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "resource", String.valueOf(resource), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "objectTypeDefinition", String.valueOf(objectTypeDefinition), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "systemConfiguration", String.valueOf(systemConfiguration), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "correlatorState", correlatorState, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "manualCorrelationContext", manualCorrelationContext, indent + 1);
        return sb.toString();
    }

    /**
     * A simple shallow clone. Use with care.
     */
    @Override
    public CorrelationContext clone() {
        try {
            return (CorrelationContext) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new SystemException(e);
        }
    }

    public @NotNull ObjectType getSourceObject(@NotNull SourceObjectType type) {
        switch (type) {
            case FOCUS:
                return preFocus;
            case PROJECTION:
                return resourceObject;
            default:
                throw new AssertionError(type);
        }
    }
}
