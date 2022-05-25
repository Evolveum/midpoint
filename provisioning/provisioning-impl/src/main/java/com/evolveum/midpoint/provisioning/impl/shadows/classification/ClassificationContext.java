/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.classification;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.provisioning.impl.CommonBeans;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * TEMPORARY, copied from older implementation - TODO decide on this class fate
 */
@Experimental
public class ClassificationContext {

    @NotNull private final ShadowType shadowedResourceObject;

    @NotNull private final ResourceType resource;
    @Nullable private final SystemConfigurationType systemConfiguration;
    @NotNull private final Task task;
    @NotNull private final CommonBeans beans;

    private ClassificationContext(Builder builder) {
        shadowedResourceObject = Objects.requireNonNull(builder.shadowedResourceObject, "shadowedResourceObject is null");
        resource = Objects.requireNonNull(builder.resource, "resource is null");
        systemConfiguration = builder.systemConfiguration;
        task = Objects.requireNonNull(builder.task, "task is null");
        beans = Objects.requireNonNull(builder.beans, "beans object is null");
    }

    public @NotNull ShadowType getShadowedResourceObject() {
        return shadowedResourceObject;
    }

    public @NotNull ResourceType getResource() {
        return resource;
    }

    public @Nullable SystemConfigurationType getSystemConfiguration() {
        return systemConfiguration;
    }

    public @Nullable String getChannel() {
        return task.getChannel();
    }

    public @NotNull Task getTask() {
        return task;
    }

    public @NotNull CommonBeans getBeans() {
        return beans;
    }

    public @NotNull VariablesMap createVariablesMap() {
        VariablesMap variables =
                ExpressionUtil.getDefaultVariablesMap(null, shadowedResourceObject, resource, systemConfiguration);
        variables.put(ExpressionConstants.VAR_CHANNEL, getChannel(), String.class);
        return variables;
    }

    public static final class Builder {
        @NotNull private final ShadowType shadowedResourceObject;
        @NotNull private final ResourceType resource;
        private SystemConfigurationType systemConfiguration;
        @NotNull private final Task task;
        @NotNull private final CommonBeans beans;

        private Builder(
                @NotNull ShadowType shadowedResourceObject,
                @NotNull ResourceType resource,
                @NotNull Task task,
                @NotNull CommonBeans beans) {
            this.shadowedResourceObject = shadowedResourceObject;
            this.resource = resource;
            this.task = task;
            this.beans = beans;
        }

        // We include all obligatory parameters here to make sure they are not forgotten during initialization.
        static Builder aClassificationContext(
                @NotNull ShadowType shadow,
                @NotNull ResourceType resource,
                @NotNull Task task,
                @NotNull CommonBeans beans) {
            return new Builder(shadow, resource, task, beans);
        }

        Builder withSystemConfiguration(SystemConfigurationType systemConfiguration) {
            this.systemConfiguration = systemConfiguration;
            return this;
        }

        public ClassificationContext build() {
            return new ClassificationContext(this);
        }
    }
}
