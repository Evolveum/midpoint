/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl;

import com.evolveum.midpoint.schema.processor.ShadowLikeValue;
import com.evolveum.midpoint.model.api.correlation.CorrelationContext;
import com.evolveum.midpoint.model.impl.sync.SynchronizationContext;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.util.AbstractShadow;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A context generally useful for the manipulation of a shadow, e.g. correlation or synchronization.
 *
 * We use it to avoid repeating the contained data throughout various "context" classes
 * ({@link SynchronizationContext}, {@link CorrelationContext}, ...).
 */
@Experimental
public interface ResourceObjectProcessingContext {

    /**
     * Returns shadowed resource object, or - at least - so-called "combined object" in the sense
     * used in ResourceObjectClassifier FIXME
     */
    @NotNull ShadowLikeValue getShadowLikeValue();

    default @Nullable ShadowType getShadowIfPresent() {
        return getShadowLikeValue() instanceof AbstractShadow shadow ? shadow.getBean() : null;
    }

    default @NotNull ShadowType getShadowRequired() {
        var shadowLikeValue = getShadowLikeValue();
        if (shadowLikeValue instanceof AbstractShadow shadow) {
            return shadow.getBean();
        } else {
            throw new IllegalStateException("Expected a shadow, got " + shadowLikeValue);
        }
    }

    /** This is always the sync delta (if not null). */
    @Nullable ObjectDelta<ShadowType> getResourceObjectDelta();

    @NotNull ResourceType getResource();

    @Nullable SystemConfigurationType getSystemConfiguration();

    /**
     * Returns the channel relevant to the current operation.
     *
     * It may be a channel from {@link ResourceObjectShadowChangeDescription} or from a task.
     */
    @Nullable String getChannel();

    @NotNull Task getTask();

    /**
     * Returns {@link VariablesMap} relevant for the current context.
     */
    default @NotNull VariablesMap createVariablesMap() {
        return createDefaultVariablesMap();
    }

    /** To be used in implementations of {@link #createVariablesMap()}. */
    default @NotNull VariablesMap createDefaultVariablesMap() {
        return ModelImplUtils.getDefaultVariablesMap(null, getShadowIfPresent(), getResource(), getSystemConfiguration());
    }
}
