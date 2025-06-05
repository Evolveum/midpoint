/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.definition.ActivityDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;
import com.evolveum.midpoint.repo.common.activity.run.CommonTaskBeans;
import com.evolveum.midpoint.repo.common.activity.run.state.ActivityTreePurger;
import com.evolveum.midpoint.repo.common.activity.run.task.ActivityBasedTaskRun;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityTreeRealizationStateType;

/**
 * Represents the tree of activities that comprise a logical task.
 *
 * Basically, binds together the root activity ({@link StandaloneActivity}) and the dynamic object representing
 * the state of the whole tree ({@link ActivityTreeStateOverview}).
 */
public class ActivityTree implements DebugDumpable {

    /**
     * The root activity. Children are referenced from it.
     */
    @NotNull private final StandaloneActivity<?, ?> rootActivity;

    @NotNull private final ActivityTreeStateOverview treeStateOverview;

    @NotNull private final CommonTaskBeans beans = CommonTaskBeans.get();

    private <WD extends WorkDefinition, AH extends ActivityHandler<WD, AH>> ActivityTree(
            @NotNull ActivityDefinition<WD> rootDefinition,
            @NotNull Task rootTask) {
        AH handler = beans.activityHandlerRegistry.getHandlerRequired(rootDefinition);
        this.rootActivity = StandaloneActivity.createRoot(rootDefinition, handler, this);
        this.treeStateOverview = new ActivityTreeStateOverview(rootTask, beans);
    }

    public static ActivityTree create(Task rootTask) throws SchemaException, ConfigurationException {
        ActivityDefinition<?> rootDefinition = ActivityDefinition.createRoot(rootTask);
        return new ActivityTree(rootDefinition, rootTask);
    }

    @NotNull
    public Activity<?, ?> getRootActivity() {
        return rootActivity;
    }

    @NotNull
    public CommonTaskBeans getBeans() {
        return beans;
    }

    @Override
    public String toString() {
        return "ActivityTree{" +
                "rootActivity=" + rootActivity +
                '}';
    }

    @Override
    public String debugDump(int indent) {
        return rootActivity.debugDump(indent);
    }

    @NotNull
    public Activity<?, ?> getActivity(ActivityPath path) throws SchemaException {
        Activity<?, ?> current = rootActivity;
        for (String identifier : path.getIdentifiers()) {
            current = current.getChild(identifier);
        }
        return current;
    }

    public @NotNull ActivityTreeStateOverview getTreeStateOverview() {
        return treeStateOverview;
    }

    public ActivityTreeRealizationStateType getRealizationState() {
        return treeStateOverview.getRealizationState();
    }

    public void updateRealizationState(ActivityTreeRealizationStateType value, OperationResult result)
            throws ActivityRunException {
        treeStateOverview.updateRealizationState(value, result);
    }

    /** Purges the activity state (usually before new realization). */
    public void purgeState(ActivityBasedTaskRun taskRun, OperationResult result) throws ActivityRunException {
        purgeTreeStateOverview(result);
        purgeDetailedStateAndTaskStatistics(taskRun, result);
    }

    private void purgeTreeStateOverview(OperationResult result) throws ActivityRunException {
        treeStateOverview.purge(result);
    }

    /**
     * Purges detailed state of the activities: including worker and delegator tasks!
     */
    private void purgeDetailedStateAndTaskStatistics(ActivityBasedTaskRun taskRun, OperationResult result)
            throws ActivityRunException {
        new ActivityTreePurger(taskRun, beans)
                .purge(result);
    }
}
