/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.api;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExtensionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionEnvironmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskWorkManagementType;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

/**
 * Configuration for PartitioningTaskHandler: describes how to create subtasks (partitions) for the given master task.
 *
 * Templates can contain the following macros:
 * - {index}: number of the subtask, starting at 1
 * - {masterTaskName}: name of the master task
 * - {masterTaskHandlerUri}: URI of the master task handler
 *
 * @author mederly
 */
public interface TaskPartitionsDefinition {

    /**
     * Number of partitions.
     */
    int getCount(Task masterTask);

    /**
     * Whether the subtasks should be executed sequentially.
     */
    default boolean isSequentialExecution(Task masterTask) {
        return true;
    }

    /**
     * Whether the partitions should be durable i.e. whether they should persist through master task restarts.
     * This is useful e.g. for partitioned validity scanner because each partition keeps its own last
     * scan timestamp. (EXPERIMENTAL)
     */
    default boolean isDurablePartitions(Task masterTask) {
        return false;
    }

    /**
     * Template for the subtask name. The default is {masterTaskName} ({index})
     */
    default String getName(Task masterTask) {
        return null;
    }

    /**
     * Handler URI for the subtask. The default is {masterTaskHandlerUri}#{index}
     */
    default String getHandlerUri(Task masterTask) {
        return null;
    }

    /**
     * Work management for the subtasks.
     */
    default TaskWorkManagementType getWorkManagement(Task masterTask) {
        return null;
    }

    /**
     * Execution environment to be used in subtask.
     */
    default TaskExecutionEnvironmentType getExecutionEnvironment(Task masterTask) {
        return null;
    }

    /**
     * Whether to copy extension from master task into subtask.
     */
    default Boolean isCopyMasterExtension(Task masterTask) {
        return null;
    }

    /**
     * Deltas to be applied to subtask after its creation. Applied before strategy.otherDeltas.
     */
    @NotNull
    default Collection<ItemDelta<?, ?>> getOtherDeltas(Task masterTask) {
        return Collections.emptySet();
    }

    /**
     * Partition with a given number, starting at 1.
     */
    @NotNull
    TaskPartitionDefinition getPartition(Task masterTask, int index);

    /**
     * Description of a given partition.
     */
    interface TaskPartitionDefinition {

        /**
         * Template for the subtask name. Overrides strategy.taskNameTemplate. The default is {masterTaskName} ({index})
         */
        default String getName(Task masterTask) {
            return null;
        }

        /**
         * Template for the subtask handler URI. Overrides strategy.handlerUriTemplate. The default is {masterTaskHandlerUri}#{index}
         */
        default String getHandlerUri(Task masterTask) {
            return null;
        }

        /**
         * Work state configuration to be planted into subtask, if copyWorkStateConfiguration is not true.
         */
        default TaskWorkManagementType getWorkManagement(Task masterTask) {
            return null;
        }

        /**
         * Execution environment to be used in subtask. Overrides strategy.executionEnvironment.
         */
        default TaskExecutionEnvironmentType getExecutionEnvironment(Task masterTask) {
            return null;
        }

        /**
         * Extension to be added into the extension of subtask.
         */
        default ExtensionType getExtension(Task masterTask) {
            return null;
        }

        /**
         * Whether to copy extension from master task into subtask. Overrides strategy.copyMasterExtension.
         */
        default Boolean isCopyMasterExtension(Task masterTask) {
            return null;
        }

        /**
         * Deltas to be applied to subtask after its creation. Applied before strategy.otherDeltas.
         */
        @NotNull
        default Collection<ItemDelta<?, ?>> getOtherDeltas(Task masterTask) {
            return Collections.emptySet();
        }

        /**
         * Dependents of this subtask, i.e. subtasks that should be started only after this subtask has finished.
         * Provided as indices starting at 1.
         */
        @NotNull
        default Collection<Integer> getDependents() {
            return Collections.emptySet();
        }
    }
}
