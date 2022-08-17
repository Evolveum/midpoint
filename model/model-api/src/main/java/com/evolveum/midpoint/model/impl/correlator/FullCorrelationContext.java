/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator;

import com.evolveum.midpoint.model.api.correlation.CorrelationContext;
import com.evolveum.midpoint.model.api.correlator.CorrelatorContext;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.SynchronizationPolicy;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.annotation.Experimental;

import org.jetbrains.annotations.Nullable;

/**
 * Full context needed to carry out correlation-related operations on a shadow.
 *
 * TODO decide on the fate of this class
 *
 * @see CorrelatorContext
 * @see CorrelationContext
 */
@Experimental
public class FullCorrelationContext {

    @NotNull public final ShadowType shadow;
    @NotNull public final ResourceType resource;
    @NotNull public final ResourceObjectDefinition resourceObjectDefinition;
    @NotNull public final SynchronizationPolicy synchronizationPolicy;
    @NotNull public final FocusType preFocus;
    @Nullable public final ObjectTemplateType objectTemplate;
    @Nullable public final SystemConfigurationType systemConfiguration;

    public FullCorrelationContext(
            @NotNull ShadowType shadow,
            @NotNull ResourceType resource,
            @NotNull ResourceObjectDefinition resourceObjectDefinition,
            @NotNull SynchronizationPolicy synchronizationPolicy,
            @NotNull FocusType preFocus,
            @Nullable ObjectTemplateType objectTemplate,
            @Nullable SystemConfigurationType systemConfiguration) {
        this.shadow = shadow;
        this.resource = resource;
        this.resourceObjectDefinition = resourceObjectDefinition;
        this.synchronizationPolicy = synchronizationPolicy;
        this.preFocus = preFocus;
        this.objectTemplate = objectTemplate;
        this.systemConfiguration = systemConfiguration;
    }

    /**
     * Returned definition contains legacy correlation definition, if there's any.
     */
    public @NotNull CorrelationDefinitionType getCorrelationDefinitionBean() {
        return synchronizationPolicy.getCorrelationDefinition();
    }
}
