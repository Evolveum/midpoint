/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.correlator;

import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CorrelatorsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSynchronizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;

/**
 * TODO Rename/delete this class
 */
@Experimental
public class CorrelatorInstantiationContext {

    @NotNull public final ShadowType shadow;
    @NotNull public final ResourceType resource;
    @NotNull public final ResourceObjectTypeDefinition typeDefinition;
    @NotNull public final ObjectSynchronizationType synchronizationBean;
    @NotNull public final CorrelatorsType correlators;

    public CorrelatorInstantiationContext(
            @NotNull ShadowType shadow,
            @NotNull ResourceType resource,
            @NotNull ResourceObjectTypeDefinition typeDefinition,
            @NotNull ObjectSynchronizationType synchronizationBean,
            @NotNull CorrelatorsType correlators) {
        this.shadow = shadow;
        this.resource = resource;
        this.typeDefinition = typeDefinition;
        this.synchronizationBean = synchronizationBean;
        this.correlators = correlators;
    }
}
