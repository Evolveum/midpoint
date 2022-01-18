/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.correlator;

import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The context of the correlation operation(s).
 *
 * TODO decide on the exact use of this class -- will it be only at API level? Or will the correlator write its own notes here?
 *
 * TODO resolve naming conflict with CorrelationContextType
 */
public class CorrelationContext implements DebugDumpable {

    /**
     * What type of focus object(s) are we correlating against.
     */
    @NotNull private final Class<? extends ObjectType> focusType;

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
     * Information about the current state of the correlation process.
     * Usually provided by upstream (parent) correlator.
     */
    private AbstractCorrelationStateType correlationState;

    /**
     * User scripts can request manual correlation here.
     */
    @NotNull private final ManualCorrelationContext manualCorrelationContext = new ManualCorrelationContext();

    public CorrelationContext(
            @NotNull Class<? extends ObjectType> focusType,
            @NotNull ResourceType resource,
            @NotNull ResourceObjectTypeDefinition objectTypeDefinition,
            @Nullable SystemConfigurationType systemConfiguration) {
        this.focusType = focusType;
        this.resource = resource;
        this.objectTypeDefinition = objectTypeDefinition;
        this.systemConfiguration = systemConfiguration;
    }

    @NotNull public Class<? extends ObjectType> getFocusType() {
        return focusType;
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

    public AbstractCorrelationStateType getCorrelationState() {
        return correlationState;
    }

    public void setCorrelationState(AbstractCorrelationStateType correlationState) {
        this.correlationState = correlationState;
    }

    public @NotNull ManualCorrelationContext getManualCorrelationContext() {
        return manualCorrelationContext;
    }

    public void setManualCorrelationConfiguration(ManualCorrelationConfigurationType configuration) {
        manualCorrelationContext.setConfiguration(configuration);
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
    public void requestManualCorrelation(List<BuiltInCorrelationPotentialMatchType> potentialMatches) {
        manualCorrelationContext.setRequested(true);
        manualCorrelationContext.setPotentialMatches(potentialMatches);
    }

    @Override
    public String toString() {
        return "CorrelationContext("
                + focusType.getSimpleName() + ", "
                + objectTypeDefinition.getHumanReadableName() + "@" + resource
                + ')';
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "focusType", focusType, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "resource", String.valueOf(resource), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "objectTypeDefinition", String.valueOf(objectTypeDefinition), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "systemConfiguration", String.valueOf(systemConfiguration), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "correlationState", correlationState, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "manualCorrelationContext", manualCorrelationContext, indent + 1);
        return sb.toString();
    }
}
