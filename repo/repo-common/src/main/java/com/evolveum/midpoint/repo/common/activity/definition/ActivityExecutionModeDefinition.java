/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.definition;

import java.util.Objects;

import com.evolveum.midpoint.schema.TaskExecutionMode;
import com.evolveum.midpoint.schema.util.ConfigurationSpecificationTypeUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.DebugDumpable;

import org.jetbrains.annotations.Nullable;

/**
 * Defines "execution mode" aspects of an activity: production/preview/dry-run/... plus additional information.
 *
 * Corresponds to {@link ActivityExecutionModeDefinitionType}
 */
public class ActivityExecutionModeDefinition implements DebugDumpable, Cloneable {

    /**
     * This bean is detached copy dedicated for this definition. It is therefore freely modifiable.
     *
     * The `mode` property i.e. `bean.getMode()` should not be `null`.
     */
    @NotNull private final ActivityExecutionModeDefinitionType bean;

    private ActivityExecutionModeDefinition(
            @NotNull ActivityExecutionModeDefinitionType bean) {
        this.bean = bean;
    }

    public static @NotNull ActivityExecutionModeDefinition create(@Nullable ActivityDefinitionType activityDefinitionBean) {
        if (activityDefinitionBean == null) {
            return new ActivityExecutionModeDefinition(
                    new ActivityExecutionModeDefinitionType()
                            .mode(ExecutionModeType.FULL));
        }
        ActivityExecutionModeDefinitionType originalBean = activityDefinitionBean.getExecution();
        ActivityExecutionModeDefinitionType clonedBean =
                originalBean != null ? originalBean.clone() : new ActivityExecutionModeDefinitionType();
        if (clonedBean.getMode() == null) {
            ExecutionModeType legacyMode = activityDefinitionBean.getExecutionMode();
            clonedBean.setMode(Objects.requireNonNullElse(legacyMode, ExecutionModeType.FULL));
        }
        return new ActivityExecutionModeDefinition(clonedBean);
    }

    @Override
    public String toString() {
        return getMode() + "; " + (bean.asPrismContainerValue().size()-1) + " additional item(s)";
    }

    @Override
    public String debugDump(int indent) {
        return bean.debugDump(indent);
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public ActivityExecutionModeDefinition clone() {
        return new ActivityExecutionModeDefinition(bean.clone());
    }

    public @NotNull ExecutionModeType getMode() {
        return Objects.requireNonNull(bean.getMode(), "no execution mode");
    }

    public void setMode(@NotNull ExecutionModeType mode) {
        bean.setMode(Objects.requireNonNull(mode));
    }

    void applyChangeTailoring(ActivityTailoringType tailoring) {
        ExecutionModeType tailoredMode = tailoring.getExecutionMode();
        if (tailoredMode != null) {
            setMode(tailoredMode);
        }
    }

    public @Nullable ConfigurationSpecificationType getConfigurationSpecification() {
        return bean.getConfigurationToUse();
    }

    /**
     * Returns null if the configuration cannot be matched to a predefined one. So, e.g., when default config is used,
     * {@link PredefinedConfigurationType#PRODUCTION} is returned.
     */
    @Nullable PredefinedConfigurationType getPredefinedConfiguration() {
        var configurationSpecification = getConfigurationSpecification();
        var explicit = configurationSpecification != null ? configurationSpecification.getPredefined() : null;
        // Currently, there are only two options: either DEVELOPMENT or PRODUCTION. Nothing in between.
        return Objects.requireNonNullElse(explicit, PredefinedConfigurationType.PRODUCTION);
    }

    public TaskExecutionMode getTaskExecutionMode() throws ConfigurationException {
        ExecutionModeType mode = getMode();
        switch (mode) {
            case FULL -> {
                if (isProductionConfiguration()) {
                    return TaskExecutionMode.PRODUCTION;
                } else {
                    throw new ConfigurationException("Full execution mode requires the use of production configuration");
                }
            }
            case PREVIEW -> {
                if (isProductionConfiguration()) {
                    return TaskExecutionMode.SIMULATED_PRODUCTION;
                } else {
                    return TaskExecutionMode.SIMULATED_DEVELOPMENT;
                }
            }
            case SHADOW_MANAGEMENT_PREVIEW -> {
                if (isProductionConfiguration()) {
                    return TaskExecutionMode.SIMULATED_SHADOWS_PRODUCTION;
                } else {
                    return TaskExecutionMode.SIMULATED_SHADOWS_DEVELOPMENT;
                }
            }
            case DRY_RUN, NONE, BUCKET_ANALYSIS -> {
                // These modes are treated in a special way - will be probably changed later
                // It is also unclear whether to insist on production configuration here.
                return TaskExecutionMode.PRODUCTION;
            }
            default -> throw new AssertionError(mode);
        }
    }

    private boolean isProductionConfiguration() {
        return ConfigurationSpecificationTypeUtil.isProductionConfiguration(bean.getConfigurationToUse());
    }
}
