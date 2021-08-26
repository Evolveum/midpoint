/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.definition;

import static com.evolveum.midpoint.util.MiscUtil.or0;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Defines monitoring features of the activity, like tracing and profiling.
 */
public class ActivityMonitoringDefinition implements DebugDumpable, Cloneable {

    /**
     * This bean is detached copy dedicated for this definition. It is therefore freely modifiable.
     */
    @NotNull private ActivityMonitoringDefinitionType bean;

    private ActivityMonitoringDefinition(@NotNull ActivityMonitoringDefinitionType bean) {
        this.bean = bean;
    }

    /**
     * The task can be null for children of pure-composite activities.
     */
    @NotNull
    public static ActivityMonitoringDefinition create(@Nullable ActivityDefinitionType definitionBean, @Nullable Task task) {
        ActivityMonitoringDefinitionType bean = definitionBean != null && definitionBean.getMonitoring() != null ?
                definitionBean.getMonitoring() : null;

        ActivityMonitoringDefinitionType monitoring = new ActivityMonitoringDefinitionType(PrismContext.get());

        ProcessTracingConfigurationType tracing;
        if (bean != null && bean.getTracing() != null) {
            tracing = bean.getTracing().clone();
        } else {
            tracing = createTracingDefinitionFromTask(task);
        }
        monitoring.setTracing(tracing);

        ProcessProfilingConfigurationType profiling;
        if (bean != null && bean.getProfiling() != null) {
            profiling = bean.getProfiling().clone();
        } else {
            profiling = createProfilingConfigurationFromTask(task);
        }
        monitoring.setProfiling(profiling);

        return new ActivityMonitoringDefinition(monitoring);
    }

    /**
     * Creates a detached configuration from a task (if not null).
     */
    private static @NotNull ProcessTracingConfigurationType createTracingDefinitionFromTask(Task task) {
        if (task == null) {
            return new ProcessTracingConfigurationType(PrismContext.get());
        }

        ProcessTracingConfigurationType bean =
                task.getContainerableOrClone(SchemaConstants.MODEL_EXTENSION_TRACING, ProcessTracingConfigurationType.class);
        if (bean != null) {
            return bean.clone();
        }

        // Creating artificial configuration from components
        Integer interval = task.getExtensionPropertyRealValue(SchemaConstants.MODEL_EXTENSION_TRACING_INTERVAL);
        TracingProfileType tracingProfile = task.getExtensionContainerRealValueOrClone(SchemaConstants.MODEL_EXTENSION_TRACING_PROFILE);
        PrismProperty<TracingRootType> tracingRoots = task.getExtensionPropertyOrClone(SchemaConstants.MODEL_EXTENSION_TRACING_ROOT);

        ProcessTracingConfigurationType newBean = new ProcessTracingConfigurationType(PrismContext.get())
                .interval(interval)
                .tracingProfile(CloneUtil.clone(tracingProfile));
        if (tracingRoots != null) {
            newBean.getTracingPoint().addAll(tracingRoots.getRealValues());
        }

        return newBean;
    }

    private static @NotNull ProcessProfilingConfigurationType createProfilingConfigurationFromTask(Task task) {
        if (task == null) {
            return new ProcessProfilingConfigurationType(PrismContext.get());
        }

        Integer interval = task.getExtensionPropertyRealValue(SchemaConstants.MODEL_EXTENSION_PROFILING_INTERVAL);
        return new ProcessProfilingConfigurationType(PrismContext.get())
                .interval(interval);
    }

    public int getTracingInterval() {
        return or0(getTracing().getInterval());
    }

    public int getDynamicProfilingInterval() {
        return or0(getProfiling().getInterval());
    }

    public @NotNull ProcessTracingConfigurationType getTracing() {
        return java.util.Objects.requireNonNullElseGet(
                bean.getTracing(),
                () -> new ProcessTracingConfigurationType(PrismContext.get()));
    }

    public @NotNull ProcessProfilingConfigurationType getProfiling() {
        return java.util.Objects.requireNonNullElseGet(
                bean.getProfiling(),
                () -> new ProcessProfilingConfigurationType(PrismContext.get()));
    }

    void applyChangeTailoring(@NotNull ActivityTailoringType tailoring) {
        if (tailoring.getMonitoring() != null) {
            bean = TailoringUtil.getTailoredBean(bean, tailoring.getMonitoring());
        } else {
            // null means we do not want it to change.
        }
    }

    @Override
    public String toString() {
        return "tracing: " + getTracing().asPrismContainerValue().size() + " item(s), "
                + "profiling: " + getProfiling().asPrismContainerValue().size() + " item(s)";
    }

    @Override
    public String debugDump(int indent) {
        return bean.debugDump();
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public ActivityMonitoringDefinition clone() {
        return new ActivityMonitoringDefinition(bean.clone());
    }
}
