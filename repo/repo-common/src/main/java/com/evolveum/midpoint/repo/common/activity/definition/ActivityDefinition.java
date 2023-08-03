/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.definition;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
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

    private ActivityDefinition(String explicitlyDefinedIdentifier,
            @NotNull WD workDefinition,
            @NotNull ActivityControlFlowDefinition controlFlowDefinition,
            @NotNull ActivityDistributionDefinition distributionDefinition,
            @NotNull ActivityReportingDefinition reportingDefinition,
            @NotNull ActivityExecutionModeDefinition executionModeDefinition) {
        this.explicitlyDefinedIdentifier = explicitlyDefinedIdentifier;
        this.workDefinition = workDefinition;
        this.controlFlowDefinition = controlFlowDefinition;
        this.distributionDefinition = distributionDefinition;
        this.reportingDefinition = reportingDefinition;
        this.executionModeDefinition = executionModeDefinition;
    }

    /**
     * Creates a definition for the root activity in the task. It is taken from the {@link ActivityDefinitionType}, as legacy
     * handler URIs are no longer supported (since 4.8).
     */
    public static <WD extends AbstractWorkDefinition> ActivityDefinition<WD> createRoot(Task rootTask)
            throws SchemaException, ConfigurationException {
        ActivityDefinitionType definitionBean = rootTask.getRootActivityDefinitionOrClone();

        WD rootWorkDefinition = WorkDefinition.getWorkDefinitionFromBean(definitionBean);
        if (rootWorkDefinition == null) {
            throw new SchemaException(
                    "Root work definition cannot be obtained for %s: no activity definition is provided.".formatted(rootTask));
        }
        assert definitionBean != null;

        return createActivityDefinition(rootWorkDefinition, definitionBean);
    }

    private static @NotNull <WD extends AbstractWorkDefinition> ActivityDefinition<WD> createActivityDefinition(
            WD workDefinition, ActivityDefinitionType definitionBean) {

        workDefinition.addTailoringFrom(definitionBean);

        return new ActivityDefinition<>(
                definitionBean.getIdentifier(),
                workDefinition,
                ActivityControlFlowDefinition.create(definitionBean),
                ActivityDistributionDefinition.create(definitionBean),
                ActivityReportingDefinition.create(definitionBean),
                ActivityExecutionModeDefinition.create(definitionBean));
    }

    /**
     * Creates a definition for a child of a custom composite activity.
     *
     * It is taken from the "activity" bean, combined with (compatible) information from "defaultWorkDefinition"
     * beans all the way up.
     */
    public static ActivityDefinition<?> createChild(@NotNull ActivityDefinitionType bean) {
        try {
            AbstractWorkDefinition definition = WorkDefinition.getWorkDefinitionFromBean(bean);
            // TODO enhance with defaultWorkDefinition
            if (definition == null) {
                throw new SchemaException("Child work definition is not present for " + bean);
            }
            return createActivityDefinition(definition, bean);
        } catch (SchemaException | ConfigurationException e) {
            throw new IllegalArgumentException("Couldn't create activity definition from a bean: " + e.getMessage(), e);
        }
    }

    public @NotNull ExecutionModeType getExecutionMode() {
        return executionModeDefinition.getMode();
    }

    @Deprecated
    public ErrorSelectorType getErrorCriticality() {
        return null; // FIXME
    }

    public @NotNull WD getWorkDefinition() {
        return workDefinition;
    }

    public @NotNull Class<WD> getWorkDefinitionClass() {
        //noinspection unchecked
        return (Class<WD>) workDefinition.getClass();
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
                executionModeDefinition.clone());
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
