/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.definition;

import com.evolveum.midpoint.repo.common.activity.policy.ActivityPoliciesDefinition;
import com.evolveum.midpoint.repo.common.activity.run.CommonTaskBeans;
import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;

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
 * the complete information about particular activity in the context of given task, e.g. various defaults resolved.
 *
 * Should not contain any data related to the execution of the activity.
 */
public class ActivityDefinition<WD extends WorkDefinition> implements DebugDumpable, Cloneable {

    /**
     * This is how the user want to identify the activity. If not provided, the identifier is generated automatically.
     */
    @Nullable private final String explicitlyDefinedIdentifier;

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

    /** Definition for activity policies. Currently, not tailorable. */
    @NotNull private final ActivityPoliciesDefinition policiesDefinition;

    private ActivityDefinition(
            @Nullable String explicitlyDefinedIdentifier,
            @NotNull WD workDefinition,
            @NotNull ActivityControlFlowDefinition controlFlowDefinition,
            @NotNull ActivityDistributionDefinition distributionDefinition,
            @NotNull ActivityReportingDefinition reportingDefinition,
            @NotNull ActivityExecutionModeDefinition executionModeDefinition,
            @NotNull ActivityPoliciesDefinition policiesDefinition) {
        this.explicitlyDefinedIdentifier = explicitlyDefinedIdentifier;
        this.workDefinition = workDefinition;
        this.controlFlowDefinition = controlFlowDefinition;
        this.distributionDefinition = distributionDefinition;
        this.reportingDefinition = reportingDefinition;
        this.executionModeDefinition = executionModeDefinition;
        this.policiesDefinition = policiesDefinition;
    }

    /**
     * Creates a definition for the root activity in the task. It is taken from the {@link ActivityDefinitionType}, as legacy
     * handler URIs are no longer supported (since 4.8).
     */
    public static <WD extends AbstractWorkDefinition> ActivityDefinition<WD> createRoot(Task rootTask)
            throws SchemaException, ConfigurationException {
        ActivityDefinitionType definitionBean = rootTask.getRootActivityDefinitionOrClone();
        if (definitionBean == null) {
            throw new ConfigurationException("No activity definition in " + rootTask);
        }

        ConfigurationItemOrigin origin = ConfigurationItemOrigin.inObjectApproximate(
                rootTask.getRawTaskObjectClonedIfNecessary().asObjectable(), TaskType.F_ACTIVITY);

        ActivityDefinition<WD> definition = createActivityDefinition(definitionBean, origin);
        if (definition == null) {
            throw new ConfigurationException(
                    "Root work definition cannot be obtained for %s: no supported activity definition is provided.".formatted(
                            rootTask));
        }
        return definition;
    }

    public static <WD extends AbstractWorkDefinition> ActivityDefinition<WD> createActivityDefinition(
            @NotNull ActivityDefinitionType definitionBean, @NotNull ConfigurationItemOrigin origin)
            throws SchemaException, ConfigurationException {
        WD rootWorkDefinition = WorkDefinition.fromBean(definitionBean, origin);
        if (rootWorkDefinition == null) {
            return null;
        }
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
                ActivityExecutionModeDefinition.create(definitionBean),
                ActivityPoliciesDefinition.create(definitionBean));
    }

    /**
     * Creates a definition for a child of a custom composite activity.
     *
     * Currently, it is taken solely from the child activity definition bean.
     */
    public static @NotNull ActivityDefinition<?> createChild(
            @NotNull ActivityDefinitionType childDefBean, @NotNull ConfigurationItemOrigin childDefOrigin) {
        try {
            AbstractWorkDefinition definition = WorkDefinition.fromBean(childDefBean, childDefOrigin);
            // TODO enhance with defaultWorkDefinition
            if (definition == null) {
                throw new ConfigurationException("Child work definition is not present for " + childDefBean);
            }
            return createActivityDefinition(definition, childDefBean);
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

    public @NotNull ActivityPoliciesDefinition getPoliciesDefinition() {
        return policiesDefinition;
    }

    @Override
    public String toString() {
        return "ActivityDefinition{" +
                "workDefinition=" + workDefinition +
                ", controlFlowDefinition: " + controlFlowDefinition +
                ", distributionDefinition: " + distributionDefinition +
                ", reportingDefinition: " + reportingDefinition +
                ", executionModeDefinition: " + executionModeDefinition +
                ", policiesDefinition: " + policiesDefinition +
                '}';
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpLabelLn(sb, getClass().getSimpleName(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "work", workDefinition, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "control flow", controlFlowDefinition, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "distribution", distributionDefinition, indent + 1);
        return sb.toString();
    }

    public @Nullable String getExplicitlyDefinedIdentifier() {
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
                policiesDefinition.clone());
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

    public @NotNull AffectedObjectsInformation getAffectedObjectsInformation(@Nullable AbstractActivityWorkStateType state) throws SchemaException, ConfigurationException {
        if (workDefinition instanceof AffectedObjectsProvider affectedObjectsProvider) {
            // If the definition does provide everything needed, we can use it.
            return affectedObjectsProvider.getAffectedObjectsInformation(state);
        } else {
            // Otherwise, we need to complete the information ourselves.
            var objectSets = workDefinition.getListOfAffectedObjectSetInformation(state);
            return AffectedObjectsInformation.complex(
                    objectSets.stream()
                            .map(objectSet ->
                                    AffectedObjectsInformation.simple(
                                            workDefinition.getActivityTypeName(),
                                            objectSet,
                                            executionModeDefinition.getMode(),
                                            executionModeDefinition.getPredefinedConfiguration()))
                            .toList());
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean shouldCreateSimulationResult() {
        var explicit = reportingDefinition.getExplicitSimulationResultCreationInstruction();
        if (explicit != null) {
            return explicit;
        }

        if (!CommonTaskBeans.get().repositoryService.isNative()) {
            return false;
        }

        ExecutionModeType mode = executionModeDefinition.getMode();
        return mode == ExecutionModeType.PREVIEW
                || mode == ExecutionModeType.SHADOW_MANAGEMENT_PREVIEW;
    }
}
