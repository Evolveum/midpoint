/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.task.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * @author Radovan Semancik
 *
 */
public interface TaskHandler {

    /**
     * Executes a task handler.
     *
     * TODO better description
     */
    TaskRunResult run(@NotNull RunningTask task) throws TaskException;

    default Long heartbeat(Task task) {
        return null;
    }

    // TODO: fix signature
    default void refreshStatus(Task task) {
    }

    /**
     * @return Channel URI for tasks managed by this handler, if applicable.
     */
    default String getDefaultChannel() {
        return null;
    }

    @NotNull
    default StatisticsCollectionStrategy getStatisticsCollectionStrategy() {
        return new StatisticsCollectionStrategy();
    }

    /**
     * TODO Decide on the fate of this method.
     *
     * @param handlerUri One the handler URIs supported by this handler.
     *
     * @return Archetype OID for tasks that are powered by this handler and have the specified handler URI.
     */
    @Nullable String getArchetypeOid(@Nullable String handlerUri);

    /**
     * Call to update the state of the task (or related tasks) when the node on which this task executed
     * was found down.
     *
     * Currently this means releasing buckets allocated to this task.
     *
     * In the future we plan to execute this method within a dynamic repo transaction.
     */
    default void cleanupOnNodeDown(@NotNull TaskType task, @NotNull OperationResult result) throws CommonException {
    }
}
