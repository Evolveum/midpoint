/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.definition;

import java.util.function.Supplier;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.task.CommonTaskBeans;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.jetbrains.annotations.Nullable;

/**
 * Definition of an activity. It is analogous to ActivityDefinitionType, but contains the complete information
 * about particular activity in the context of given task.
 */
public class ActivityDefinition<WD extends WorkDefinition> implements DebugDumpable, Cloneable {

    private final String identifierFromDefinition;

    /** Currently not tailorable. */
    @NotNull private final WD workDefinition;

    /** Tailorable. */
    @NotNull private final ActivityControlFlowDefinition controlFlowDefinition;

    /** Tailorable. */
    @NotNull private final ActivityDistributionDefinition distributionDefinition;

    /** Definition for tracing and profiling. Currently not tailorable. */
    @NotNull private final ActivityMonitoringDefinition monitoringDefinition;

    /**
     * Reporting options specified for specific activity. These are merged with default reporting options
     * hardcoded in activity execution code.
     *
     * Currently not tailorable.
     */
    @NotNull private final TaskReportingOptionsType specificReportingOptions;

    @NotNull private final WorkDefinitionFactory workDefinitionFactory;

    private ActivityDefinition(String identifierFromDefinition,
            @NotNull WD workDefinition,
            @NotNull ActivityControlFlowDefinition controlFlowDefinition,
            @NotNull ActivityDistributionDefinition distributionDefinition,
            @NotNull ActivityMonitoringDefinition monitoringDefinition,
            @NotNull TaskReportingOptionsType specificReportingOptions,
            @NotNull WorkDefinitionFactory workDefinitionFactory) {
        this.identifierFromDefinition = identifierFromDefinition;
        this.workDefinition = workDefinition;
        this.controlFlowDefinition = controlFlowDefinition;
        this.distributionDefinition = distributionDefinition;
        this.monitoringDefinition = monitoringDefinition;
        this.specificReportingOptions = specificReportingOptions;
        this.workDefinitionFactory = workDefinitionFactory;
    }

    /**
     * Creates a definition for the root activity in the task. It is taken from:
     *
     * 1. "activity" bean
     * 2. handler URI
     */
    public static <WD extends AbstractWorkDefinition> ActivityDefinition<WD> createRoot(Task rootTask, CommonTaskBeans beans)
            throws SchemaException {
        WorkDefinitionFactory factory = beans.workDefinitionFactory;

        ActivityDefinitionType bean = rootTask.getRootActivityDefinitionOrClone();
        WD rootWorkDefinition = createRootWorkDefinition(bean, rootTask, factory);
        rootWorkDefinition.setExecutionMode(determineExecutionMode(bean, getModeSupplier(rootTask)));
        rootWorkDefinition.addTailoringFrom(bean);

        ActivityControlFlowDefinition controlFlowDefinition = ActivityControlFlowDefinition.create(bean);
        ActivityDistributionDefinition distributionDefinition =
                ActivityDistributionDefinition.create(bean, getWorkerThreadsSupplier(rootTask));
        ActivityMonitoringDefinition monitoringDefinition = ActivityMonitoringDefinition.create(bean, rootTask);

        return new ActivityDefinition<>(
                bean != null ? bean.getIdentifier() : null,
                rootWorkDefinition,
                controlFlowDefinition,
                distributionDefinition,
                monitoringDefinition,
                getReportingOptionsCloned(bean, rootTask),
                factory);
    }

    private static @NotNull TaskReportingOptionsType getReportingOptionsCloned(ActivityDefinitionType bean, Task task) {
        if (bean != null && bean.getReporting() != null) {
            return bean.getReporting().clone();
        }

        if (task != null) {
            TaskReportingOptionsType fromTask =
                    task.getExtensionContainerRealValueOrClone(SchemaConstants.MODEL_EXTENSION_REPORTING_OPTIONS);
            if (fromTask != null) {
                return fromTask.clone();
            }
        }

        return new TaskReportingOptionsType(PrismContext.get());
    }

    /**
     * Creates a definition for a child of a pure-composite activity.
     *
     * It is taken from the "activity" bean, combined with (compatible) information from "defaultWorkDefinition"
     * beans all the way up.
     */
    public static ActivityDefinition<?> createChild(@NotNull ActivityDefinitionType bean,
            @NotNull WorkDefinitionFactory workDefinitionFactory)
            throws SchemaException {
        AbstractWorkDefinition definition = createFromBean(bean, workDefinitionFactory);

        // TODO enhance with defaultWorkDefinition
        if (definition == null) {
            throw new SchemaException("Child work definition is not present for " + bean);
        }

        definition.setExecutionMode(determineExecutionMode(bean, () -> ExecutionModeType.FULL));
        definition.addTailoringFrom(bean);

        ActivityControlFlowDefinition controlFlowDefinition = ActivityControlFlowDefinition.create(bean);
        ActivityDistributionDefinition distributionDefinition = ActivityDistributionDefinition.create(bean, () -> null);
        ActivityMonitoringDefinition monitoringDefinition = ActivityMonitoringDefinition.create(bean, null);

        return new ActivityDefinition<>(
                bean.getIdentifier(),
                definition,
                controlFlowDefinition,
                distributionDefinition,
                monitoringDefinition,
                getReportingOptionsCloned(bean, null),
                workDefinitionFactory);
    }

    @NotNull
    private static <WD extends AbstractWorkDefinition> WD createRootWorkDefinition(ActivityDefinitionType activityBean,
            Task rootTask, WorkDefinitionFactory factory) throws SchemaException {

        if (activityBean != null) {
            WD def = createFromBean(activityBean, factory);
            if (def != null) {
                return def;
            }
        }

        //noinspection unchecked
        WD def = (WD) factory.getWorkFromTaskLegacy(rootTask);
        if (def != null) {
            return def;
        }

        throw new SchemaException("Root work definition cannot be obtained for " + rootTask + ": no activity nor "
                + "known task handler URI is provided. Handler URI = " + rootTask.getHandlerUri());
    }

    private static <WD extends AbstractWorkDefinition> WD createFromBean(ActivityDefinitionType bean,
            WorkDefinitionFactory factory) throws SchemaException {
        if (bean.getComposition() != null) {
            //noinspection unchecked
            return (WD) new CompositeWorkDefinition(bean.getComposition());
        }

        if (bean.getWork() != null) {
            //noinspection unchecked
            return (WD) factory.getWorkFromBean(bean.getWork()); // returned value can be null
        }

        return null;
    }

    private static ExecutionModeType determineExecutionMode(ActivityDefinitionType bean,
            Supplier<ExecutionModeType> defaultValueSupplier) {
        ExecutionModeType explicitMode = bean != null ? bean.getExecutionMode() : null;
        if (explicitMode != null) {
            return explicitMode;
        } else {
            return defaultValueSupplier.get();
        }
    }

    @NotNull
    public ExecutionModeType getExecutionMode() {
        return workDefinition.getExecutionMode();
    }

    private static Supplier<ExecutionModeType> getModeSupplier(Task task) {
        return () -> {
            Boolean taskDryRun = task.getExtensionPropertyRealValue(SchemaConstants.MODEL_EXTENSION_DRY_RUN);
            return Boolean.TRUE.equals(taskDryRun) ? ExecutionModeType.DRY_RUN : ExecutionModeType.FULL;
        };
    }

    private static Supplier<Integer> getWorkerThreadsSupplier(Task task) {
        return () -> task.getExtensionPropertyRealValue(SchemaConstants.MODEL_EXTENSION_WORKER_THREADS);
    }

    @Deprecated
    public ErrorSelectorType getErrorCriticality() {
        return null; // FIXME
    }

    public @NotNull WD getWorkDefinition() {
        return workDefinition;
    }

    public @NotNull ActivityDistributionDefinition getDistributionDefinition() {
        return distributionDefinition;
    }

    public @NotNull ActivityControlFlowDefinition getControlFlowDefinition() {
        return controlFlowDefinition;
    }

    @Override
    public String toString() {
        return "ActivityDefinition{" +
                "workDefinition=" + workDefinition +
                ", controlFlowDefinition: " + controlFlowDefinition +
                ", distributionDefinition: " + distributionDefinition +
                '}';
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpLabelLn(sb, getClass().getSimpleName(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "work", workDefinition, indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, "control flow", controlFlowDefinition, indent+1);
        DebugUtil.debugDumpWithLabel(sb, "distribution", distributionDefinition, indent+1);
        return sb.toString();
    }

    public String getIdentifier() {
        return identifierFromDefinition;
    }

    public void applyChangeTailoring(@NotNull ActivityTailoringType tailoring) {
        controlFlowDefinition.applyChangeTailoring(tailoring);
        distributionDefinition.applyChangeTailoring(tailoring);
        monitoringDefinition.applyChangeTailoring(tailoring);
        applyExecutionModeTailoring(tailoring);
    }

    public void applySubtaskTailoring(@NotNull ActivitySubtaskSpecificationType subtaskSpecification) {
        distributionDefinition.applySubtaskTailoring(subtaskSpecification);
    }

    private void applyExecutionModeTailoring(@NotNull ActivityTailoringType tailoring) {
        if (tailoring.getExecutionMode() != null) {
            ((AbstractWorkDefinition) workDefinition).setExecutionMode(tailoring.getExecutionMode());
        }
    }

    /**
     * Does the deep clone. The goal is to be able to modify cloned definition freely.
     */
    @SuppressWarnings({ "MethodDoesntCallSuperMethod" })
    @Override
    public ActivityDefinition<WD> clone() {
        //noinspection unchecked
        return new ActivityDefinition<>(
                identifierFromDefinition,
                (WD) workDefinition.clone(),
                controlFlowDefinition.clone(),
                distributionDefinition.clone(),
                monitoringDefinition.clone(),
                specificReportingOptions.clone(),
                workDefinitionFactory);
    }

    public @NotNull ActivityMonitoringDefinition getMonitoringDefinition() {
        return monitoringDefinition;
    }

    public @NotNull TaskReportingOptionsType getSpecificReportingOptions() {
        return specificReportingOptions;
    }

    public @Nullable FailedObjectsSelectorType getFailedObjectsSelector() {
        // In the future the selector can be present somewhere else (maybe in "error handling"-like section).
        if (workDefinition instanceof FailedObjectsSelectorProvider) {
            return ((FailedObjectsSelectorProvider) workDefinition).getFailedObjectsSelector();
        } else {
            return null;
        }
    }
}
