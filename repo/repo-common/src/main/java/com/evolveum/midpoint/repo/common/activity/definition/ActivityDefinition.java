/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.definition;

import java.util.function.Supplier;

import com.evolveum.midpoint.util.exception.ConfigurationException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.repo.common.activity.run.CommonTaskBeans;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Definition of an activity.
 *
 * It is analogous to (and primarily filled-in from) {@link ActivityDefinitionType}, but contains
 * the complete information about particular activity in the context of given task, e.g. legacy definition data filled-in
 * from task extension items.
 *
 * Should not contain any data related to the execution of the activity.
 */
public class ActivityDefinition<WD extends WorkDefinition> implements DebugDumpable, Cloneable {

    /**
     * This is how the user want to identify the activity. If not provided, the identifier is generated automatically.
     */
    private final String explicitlyDefinedIdentifier;

    /** The work definition. Currently not tailorable. */
    @NotNull private final WD workDefinition;

    /** The control flow aspects. Tailorable. */
    @NotNull private final ActivityControlFlowDefinition controlFlowDefinition;

    /** The distribution aspects. Tailorable. */
    @NotNull private final ActivityDistributionDefinition distributionDefinition;

    /** Definition for activity reporting. Tailorable. */
    @NotNull private final ActivityReportingDefinition reportingDefinition;

    /** Definition for execution mode. Currently only partially tailorable (setting the mode) but that may change. */
    @NotNull private final ActivityExecutionModeDefinition executionModeDefinition;

    @NotNull private final WorkDefinitionFactory workDefinitionFactory;

    private ActivityDefinition(String explicitlyDefinedIdentifier,
            @NotNull WD workDefinition,
            @NotNull ActivityControlFlowDefinition controlFlowDefinition,
            @NotNull ActivityDistributionDefinition distributionDefinition,
            @NotNull ActivityReportingDefinition reportingDefinition,
            @NotNull ActivityExecutionModeDefinition executionModeDefinition,
            @NotNull WorkDefinitionFactory workDefinitionFactory) {
        this.explicitlyDefinedIdentifier = explicitlyDefinedIdentifier;
        this.workDefinition = workDefinition;
        this.controlFlowDefinition = controlFlowDefinition;
        this.distributionDefinition = distributionDefinition;
        this.reportingDefinition = reportingDefinition;
        this.executionModeDefinition = executionModeDefinition;
        this.workDefinitionFactory = workDefinitionFactory;
    }

    /**
     * Creates a definition for the root activity in the task. It is taken from:
     *
     * 1. "activity" bean
     * 2. handler URI
     */
    public static <WD extends AbstractWorkDefinition> ActivityDefinition<WD> createRoot(Task rootTask, CommonTaskBeans beans)
            throws SchemaException, ConfigurationException {
        WorkDefinitionFactory factory = beans.workDefinitionFactory;

        ActivityDefinitionType bean = rootTask.getRootActivityDefinitionOrClone();
        WD rootWorkDefinition = createRootWorkDefinition(bean, rootTask, factory);
        rootWorkDefinition.addTailoringFrom(bean);

        ActivityControlFlowDefinition controlFlowDefinition = ActivityControlFlowDefinition.create(bean);
        ActivityDistributionDefinition distributionDefinition =
                ActivityDistributionDefinition.create(bean, getWorkerThreadsSupplier(rootTask));
        ActivityReportingDefinition reportingDefinition = ActivityReportingDefinition.create(bean, rootTask);
        ActivityExecutionModeDefinition executionModeDefinition =
                ActivityExecutionModeDefinition.create(bean, getDefaultModeSupplier(rootTask));

        return new ActivityDefinition<>(
                bean != null ? bean.getIdentifier() : null,
                rootWorkDefinition,
                controlFlowDefinition,
                distributionDefinition,
                reportingDefinition,
                executionModeDefinition,
                factory);
    }

    /**
     * Creates a definition for a child of a custom composite activity.
     *
     * It is taken from the "activity" bean, combined with (compatible) information from "defaultWorkDefinition"
     * beans all the way up.
     */
    public static ActivityDefinition<?> createChild(
            @NotNull ActivityDefinitionType bean, @NotNull WorkDefinitionFactory workDefinitionFactory) {
        try {
            AbstractWorkDefinition definition = createFromBean(bean, workDefinitionFactory);
            // TODO enhance with defaultWorkDefinition
            if (definition == null) {
                throw new SchemaException("Child work definition is not present for " + bean);
            }
            definition.addTailoringFrom(bean);

            ActivityControlFlowDefinition controlFlowDefinition = ActivityControlFlowDefinition.create(bean);
            ActivityDistributionDefinition distributionDefinition = ActivityDistributionDefinition.create(bean, () -> null);
            ActivityReportingDefinition monitoringDefinition = ActivityReportingDefinition.create(bean, null);
            ActivityExecutionModeDefinition executionModeDefinition =
                    ActivityExecutionModeDefinition.create(bean, () -> ExecutionModeType.FULL);

            return new ActivityDefinition<>(
                    bean.getIdentifier(),
                    definition,
                    controlFlowDefinition,
                    distributionDefinition,
                    monitoringDefinition,
                    executionModeDefinition,
                    workDefinitionFactory);
        } catch (SchemaException | ConfigurationException e) {
            throw new IllegalArgumentException("Couldn't create activity definition from a bean: " + e.getMessage(), e);
        }
    }

    @NotNull
    private static <WD extends AbstractWorkDefinition> WD createRootWorkDefinition(ActivityDefinitionType activityBean,
            Task rootTask, WorkDefinitionFactory factory) throws SchemaException, ConfigurationException {

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
            WorkDefinitionFactory factory) throws SchemaException, ConfigurationException {
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

    public @NotNull ExecutionModeType getExecutionMode() {
        return executionModeDefinition.getMode();
    }

    private static Supplier<ExecutionModeType> getDefaultModeSupplier(Task task) {
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

    public @NotNull ActivityExecutionModeDefinition getExecutionModeDefinition() {
        return executionModeDefinition;
    }

    @Override
    public String toString() {
        return "ActivityDefinition{" +
                "workDefinition=" + workDefinition +
                ", controlFlowDefinition: " + controlFlowDefinition +
                ", distributionDefinition: " + distributionDefinition +
                ", reportingDefinition: " + reportingDefinition +
                ", executionModeDefinition: " + executionModeDefinition +
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

    public String getExplicitlyDefinedIdentifier() {
        return explicitlyDefinedIdentifier;
    }

    public void applyChangeTailoring(@NotNull ActivityTailoringType tailoring) {
        controlFlowDefinition.applyChangeTailoring(tailoring);
        distributionDefinition.applyChangeTailoring(tailoring);
        reportingDefinition.applyChangeTailoring(tailoring);
        executionModeDefinition.applyChangeTailoring(tailoring);
    }

    public void applySubtaskTailoring(@NotNull ActivitySubtaskDefinitionType subtaskSpecification) {
        distributionDefinition.applySubtaskTailoring(subtaskSpecification);
    }

    /**
     * Does the deep clone. The goal is to be able to modify cloned definition freely.
     *
     * BEWARE: Do not use this method to create a definition clone to be used for child activities.
     * Use {@link #cloneWithoutId()} instead. See MID-7894.
     */
    @SuppressWarnings({ "MethodDoesntCallSuperMethod" })
    @Override
    public ActivityDefinition<WD> clone() {
        return cloneInternal(explicitlyDefinedIdentifier);
    }

    /**
     * As {@link #clone()} but discards the {@link #explicitlyDefinedIdentifier} value.
     */
    public ActivityDefinition<WD> cloneWithoutId() {
        return cloneInternal(null);
    }

    @NotNull
    private ActivityDefinition<WD> cloneInternal(String explicitlyDefinedIdentifier) {
        //noinspection unchecked
        return new ActivityDefinition<>(
                explicitlyDefinedIdentifier,
                (WD) workDefinition.clone(),
                controlFlowDefinition.clone(),
                distributionDefinition.clone(),
                reportingDefinition.clone(),
                executionModeDefinition.clone(),
                workDefinitionFactory);
    }

    public @NotNull ActivityReportingDefinition getReportingDefinition() {
        return reportingDefinition;
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
