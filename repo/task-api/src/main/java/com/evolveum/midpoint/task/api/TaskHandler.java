/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.task.api;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import java.util.Collection;
import java.util.List;

/**
 * @author Radovan Semancik
 *
 */
public interface TaskHandler {

    default TaskRunResult run(RunningTask task) {
        return run(task, null);
    }

    @Experimental
    TaskRunResult run(RunningTask task, TaskPartitionDefinitionType partitionDefinition);

    default Long heartbeat(Task task) {
        return null;
    }

    // TODO: fix signature
    default void refreshStatus(Task task) {
    }

    /**
     * Returns a category name for a given task. In most cases, the name would be independent of concrete task.
     * @param task a task, whose category is to be determined; if getCategoryNames() returns null, this method
     *             has to accept null value as this parameter, and return the (one) category name that it gives
     *             to all tasks
     * @return a user-understandable name, like "LiveSync" or "Workflow"
     */
    @Deprecated
    String getCategoryName(Task task);

    /**
     * Returns names of task categories provided by this handler. Usually it will be one-item list.
     * @return a list of category names; may be null - in that case the category info is given by getCategoryName(null)
     */
    @Deprecated
    default List<String> getCategoryNames() {
        return null;
    }

    @NotNull
    default StatisticsCollectionStrategy getStatisticsCollectionStrategy() {
        return new StatisticsCollectionStrategy();
    }

    /**
     * @return Archetype OID for tasks that are powered by this handler.
     */
    String getArchetypeOid();
}
