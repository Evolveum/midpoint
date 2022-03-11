/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator;

import com.evolveum.midpoint.model.api.correlator.CorrelationContext;
import com.evolveum.midpoint.model.api.correlator.CorrelatorConfiguration;
import com.evolveum.midpoint.model.api.correlator.CorrelatorContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CompositeCorrelatorType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSynchronizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

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
    @NotNull public final ResourceObjectTypeDefinition typeDefinition;
    @NotNull public final ObjectSynchronizationType synchronizationBean;
    @NotNull public final CompositeCorrelatorType correlators;

    public FullCorrelationContext(
            @NotNull ShadowType shadow,
            @NotNull ResourceType resource,
            @NotNull ResourceObjectTypeDefinition typeDefinition,
            @NotNull ObjectSynchronizationType synchronizationBean,
            @NotNull CompositeCorrelatorType correlators) {
        this.shadow = shadow;
        this.resource = resource;
        this.typeDefinition = typeDefinition;
        this.synchronizationBean = synchronizationBean;
        this.correlators = correlators;
    }

    public @NotNull CorrelatorConfiguration getCorrelatorConfiguration() {
        return CorrelatorConfiguration.getConfiguration(correlators);
    }
}
