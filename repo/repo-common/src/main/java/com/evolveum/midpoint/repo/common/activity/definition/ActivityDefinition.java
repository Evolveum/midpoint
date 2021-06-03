/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.definition;

import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.task.CommonTaskBeans;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivitiesTailoringType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ErrorSelectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExecutionModeType;

/**
 * Definition of an activity. It is analogous to ActivityDefinitionType, but contains the complete information
 * about particular activity in the context of given task.
 */
public class ActivityDefinition<WD extends WorkDefinition> implements DebugDumpable {

    private final ActivityDefinitionType definitionBean;

    @NotNull private final WD workDefinition;

    @NotNull private final ActivityControlFlowDefinition controlFlowDefinition;

    @NotNull private final ActivityDistributionDefinition distributionDefinition;

    @NotNull private final WorkDefinitionFactory workDefinitionFactory;

    private ActivityDefinition(ActivityDefinitionType definitionBean, @NotNull WD workDefinition,
            @NotNull ActivityControlFlowDefinition controlFlowDefinition,
            @NotNull ActivityDistributionDefinition distributionDefinition,
            @NotNull WorkDefinitionFactory workDefinitionFactory) {
        this.definitionBean = definitionBean;
        this.workDefinition = workDefinition;
        this.controlFlowDefinition = controlFlowDefinition;
        this.distributionDefinition = distributionDefinition;
        this.workDefinitionFactory = workDefinitionFactory;
        workDefinition.setOwningActivityDefinition(this);
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
        rootWorkDefinition.addTailoring(getTailoring(bean));

        ActivityControlFlowDefinition controlFlowDefinition = ActivityControlFlowDefinition.create(bean);
        ActivityDistributionDefinition distributionDefinition = ActivityDistributionDefinition.create(bean);

        return new ActivityDefinition<>(bean, rootWorkDefinition, controlFlowDefinition, distributionDefinition, factory);
    }

    /**
     * Creates a definition for a child of a pure-composite activity.
     *
     * It is taken from the "activity" bean, combined with (compatible) information from "defaultWorkDefinition"
     * beans all the way up.
     */
    public static ActivityDefinition<?> createChild(@NotNull ActivityDefinitionType activityBean, ActivityDefinition<?> parent)
            throws SchemaException {
        AbstractWorkDefinition definition = createFromBean(activityBean, parent.workDefinitionFactory);

        // TODO enhance with defaultWorkDefinition
        if (definition == null) {
            throw new SchemaException("Child work definition is not present for " + activityBean + " in " + parent);
        }

        definition.setExecutionMode(determineExecutionMode(activityBean, () -> ExecutionModeType.EXECUTE));
        definition.addTailoring(getTailoring(activityBean));

        ActivityControlFlowDefinition controlFlowDefinition = ActivityControlFlowDefinition.create(activityBean);
        ActivityDistributionDefinition distributionDefinition = ActivityDistributionDefinition.create(activityBean);

        return new ActivityDefinition<>(activityBean, definition, controlFlowDefinition, distributionDefinition,
                parent.workDefinitionFactory);
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

    private static ExecutionModeType determineExecutionMode(ActivityDefinitionType activityBean,
            Supplier<ExecutionModeType> defaultValueSupplier) {
        ExecutionModeType explicitMode = activityBean != null ? activityBean.getExecutionMode() : null;
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
            return Boolean.TRUE.equals(taskDryRun) ? ExecutionModeType.DRY_RUN : ExecutionModeType.EXECUTE;
        };
    }

    private static ActivitiesTailoringType getTailoring(ActivityDefinitionType local) {
        return local != null ? local.getTailoring() : null;
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
        if (definitionBean != null) {
            return definitionBean.getIdentifier();
        } else {
            return null;
        }
    }
}
