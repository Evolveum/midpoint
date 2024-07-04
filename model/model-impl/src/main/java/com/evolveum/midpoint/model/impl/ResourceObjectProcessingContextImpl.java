/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl;

import java.util.Objects;

import com.evolveum.midpoint.schema.processor.ShadowLikeValue;

import com.evolveum.midpoint.schema.util.AbstractShadow;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * The default implementation of {@link ResourceObjectProcessingContext}.
 */
@Experimental
public class ResourceObjectProcessingContextImpl implements ResourceObjectProcessingContext {

    @NotNull private final ShadowType shadowedResourceObject;

    /**
     * Original delta that triggered the synchronization. (If known and if applicable.)
     */
    private final ObjectDelta<ShadowType> resourceObjectDelta;

    @NotNull private final ResourceType resource;
    @Nullable private final SystemConfigurationType systemConfiguration;
    @Nullable private final String explicitChannel;
    @NotNull private final Task task;

    private ResourceObjectProcessingContextImpl(ResourceObjectProcessingContextBuilder builder) {
        shadowedResourceObject = Objects.requireNonNull(builder.shadowedResourceObject, "shadowedResourceObject is null");
        resourceObjectDelta = builder.resourceObjectDelta;
        resource = Objects.requireNonNull(builder.resource, "resource is null");
        systemConfiguration = builder.systemConfiguration;
        explicitChannel = builder.explicitChannel;
        task = Objects.requireNonNull(builder.task, "task is null");
    }

    @Override
    public @NotNull ShadowLikeValue getShadowLikeValue() {
        return AbstractShadow.of(shadowedResourceObject);
    }

    @Override
    public ObjectDelta<ShadowType> getResourceObjectDelta() {
        return resourceObjectDelta;
    }

    @Override
    public @NotNull ResourceType getResource() {
        return resource;
    }

    @Override
    public @Nullable SystemConfigurationType getSystemConfiguration() {
        return systemConfiguration;
    }

    @Override
    public @Nullable String getChannel() {
        return explicitChannel != null ? explicitChannel : task.getChannel();
    }

    @Override
    public @NotNull Task getTask() {
        return task;
    }

    @Override
    public @NotNull VariablesMap createVariablesMap() {
        VariablesMap variables = createDefaultVariablesMap();
        variables.put(ExpressionConstants.VAR_CHANNEL, getChannel(), String.class);
        return variables;
    }

    public static final class ResourceObjectProcessingContextBuilder {
        @NotNull private final ShadowType shadowedResourceObject;
        private ObjectDelta<ShadowType> resourceObjectDelta;
        @NotNull private final ResourceType resource;
        private SystemConfigurationType systemConfiguration;
        private String explicitChannel;
        @NotNull private final Task task;
        @NotNull private final ModelBeans beans;

        private ResourceObjectProcessingContextBuilder(
                @NotNull ShadowType shadowedResourceObject,
                @NotNull ResourceType resource,
                @NotNull Task task,
                @NotNull ModelBeans beans) {
            this.shadowedResourceObject = shadowedResourceObject;
            this.resource = resource;
            this.task = task;
            this.beans = beans;
        }

        // We include all obligatory parameters here to make sure they are not forgotten during initialization.
        public static ResourceObjectProcessingContextBuilder aResourceObjectProcessingContext(
                @NotNull ShadowType shadow,
                @NotNull ResourceType resource,
                @NotNull Task task,
                @NotNull ModelBeans beans) {
            return new ResourceObjectProcessingContextBuilder(shadow, resource, task, beans);
        }

        public ResourceObjectProcessingContextBuilder withResourceObjectDelta(ObjectDelta<ShadowType> resourceObjectDelta) {
            this.resourceObjectDelta = resourceObjectDelta;
            return this;
        }

        public ResourceObjectProcessingContextBuilder withSystemConfiguration(SystemConfigurationType systemConfiguration) {
            this.systemConfiguration = systemConfiguration;
            return this;
        }

        public ResourceObjectProcessingContextBuilder withExplicitChannel(String explicitChannel) {
            this.explicitChannel = explicitChannel;
            return this;
        }

        public ResourceObjectProcessingContextImpl build() {
            return new ResourceObjectProcessingContextImpl(this);
        }
    }
}
