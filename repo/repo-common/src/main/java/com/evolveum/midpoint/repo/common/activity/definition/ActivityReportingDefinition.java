/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.definition;

import static com.evolveum.midpoint.util.MiscUtil.or0;

import com.evolveum.midpoint.prism.Containerable;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.util.List;

/**
 * Defines monitoring features of the activity, like tracing and profiling.
 */
public class ActivityReportingDefinition implements DebugDumpable, Cloneable {

    /**
     * This bean is detached copy dedicated for this definition. It is therefore freely modifiable.
     */
    @NotNull private ActivityReportingDefinitionType bean;

    private ActivityReportingDefinition(@NotNull ActivityReportingDefinitionType bean) {
        this.bean = bean;
    }

    /**
     * The task can be null for children of pure-composite activities.
     */
    @NotNull
    public static ActivityReportingDefinition create(@Nullable ActivityDefinitionType definitionBean, @Nullable Task task) {
        ActivityReportingDefinitionType bean =
                definitionBean != null && definitionBean.getReporting() != null ?
                        definitionBean.getReporting().clone() :
                        new ActivityReportingDefinitionType(PrismContext.get());

        if (bean.getTracing() == null) {
            bean.setTracing(createTracingDefinitionFromTask(task));
        }

        if (bean.getProfiling() != null) {
            bean.setProfiling(createProfilingConfigurationFromTask(task));
        }

        return new ActivityReportingDefinition(bean);
    }

    /**
     * Creates a detached configuration from a task (if not null).
     */
    private static @Nullable ProcessTracingConfigurationType createTracingDefinitionFromTask(Task task) {
        if (task == null) {
            return null;
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

        if (interval == null && tracingProfile == null && tracingRoots == null) {
            return null;
        } else {
            ProcessTracingConfigurationType newBean = new ProcessTracingConfigurationType(PrismContext.get())
                    .interval(interval)
                    .tracingProfile(CloneUtil.clone(tracingProfile));
            if (tracingRoots != null) {
                newBean.getTracingPoint().addAll(tracingRoots.getRealValues());
            }
            return newBean;
        }
    }

    private static @Nullable ProcessProfilingConfigurationType createProfilingConfigurationFromTask(Task task) {
        if (task == null) {
            return null;
        }

        Integer interval = task.getExtensionPropertyRealValue(SchemaConstants.MODEL_EXTENSION_PROFILING_INTERVAL);
        if (interval == null) {
            return null;
        } else {
            return new ProcessProfilingConfigurationType(PrismContext.get())
                    .interval(interval);
        }
    }

    public int getTracingInterval() {
        return bean.getTracing() != null ? or0(bean.getTracing().getInterval()) : 0;
    }

    public @Nullable TracingProfileType getTracingProfile() {
        return bean.getTracing() != null ? bean.getTracing().getTracingProfile() : null;
    }

    public @NotNull List<TracingRootType> getTracingPoint() {
        return bean.getTracing() != null ? bean.getTracing().getTracingPoint() : List.of();
    }

    public int getDynamicProfilingInterval() {
        return bean.getProfiling() != null ? or0(bean.getProfiling().getInterval()) : 0;
    }

    void applyChangeTailoring(@NotNull ActivityTailoringType tailoring) {
        if (tailoring.getReporting() != null) {
            bean = TailoringUtil.getTailoredBean(bean, tailoring.getReporting());
        } else {
            // null means we do not want it to change.
        }
    }

    @Override
    public String toString() {
        return "logging: " + size(bean.getLogging()) + " item(s), "
                + "tracing: @" + getTracingInterval() + ", " + size(bean.getTracing()) + " item(s), "
                + "profiling: @" + getDynamicProfilingInterval() + ", " + size(bean.getProfiling()) + " item(s)";
    }

    private int size(Containerable containerable) {
        return containerable != null ? containerable.asPrismContainerValue().size() : 0;
    }

    @Override
    public String debugDump(int indent) {
        return bean.debugDump();
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public ActivityReportingDefinition clone() {
        return new ActivityReportingDefinition(bean.clone());
    }

    public @NotNull ActivityReportingDefinitionType getBean() {
        return bean;
    }
}
