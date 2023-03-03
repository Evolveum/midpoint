/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.definition;

import java.util.Objects;
import java.util.function.Supplier;

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

    public static @NotNull ActivityExecutionModeDefinition create(
            @Nullable ActivityDefinitionType activityDefinitionBean,
            @NotNull Supplier<ExecutionModeType> defaultModeSupplier) {
        if (activityDefinitionBean == null) {
            return new ActivityExecutionModeDefinition(
                    new ActivityExecutionModeDefinitionType()
                            .mode(defaultModeSupplier.get()));
        }
        ActivityExecutionModeDefinitionType originalBean = activityDefinitionBean.getExecution();
        ActivityExecutionModeDefinitionType clonedBean =
                originalBean != null ? originalBean.clone() : new ActivityExecutionModeDefinitionType();
        if (clonedBean.getMode() == null) {
            ExecutionModeType legacyMode = activityDefinitionBean.getExecutionMode();
            if (legacyMode != null) {
                clonedBean.setMode(legacyMode);
            } else {
                clonedBean.setMode(defaultModeSupplier.get());
            }
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

    public TaskExecutionMode getTaskExecutionMode() throws ConfigurationException {
        ExecutionModeType mode = getMode();
        switch (mode) {
            case FULL:
                if (isProductionConfiguration()) {
                    return TaskExecutionMode.PRODUCTION;
                } else {
                    throw new ConfigurationException("Full execution mode requires the use of production configuration");
                }
            case PREVIEW:
                if (isProductionConfiguration()) {
                    return TaskExecutionMode.SIMULATED_PRODUCTION;
                } else {
                    return TaskExecutionMode.SIMULATED_DEVELOPMENT;
                }
            case SHADOW_MANAGEMENT_PREVIEW:
                if (isProductionConfiguration()) {
                    return TaskExecutionMode.SIMULATED_SHADOWS_PRODUCTION;
                } else {
                    return TaskExecutionMode.SIMULATED_SHADOWS_DEVELOPMENT;
                }
            case DRY_RUN:
            case NONE:
            case BUCKET_ANALYSIS:
                // These modes are treated in a special way - will be probably changed later
                // It is also unclear whether to insist on production configuration here.
                return TaskExecutionMode.PRODUCTION;
            default:
                throw new AssertionError(mode);
        }
    }

    private boolean isProductionConfiguration() {
        return ConfigurationSpecificationTypeUtil.isProductionConfiguration(bean.getConfigurationToUse());
    }
}
