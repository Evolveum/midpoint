/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl.tasks;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.builder.S_ItemEntry;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.S_MatchingRuleEntry;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.quartzimpl.TaskHandlerRegistry;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * Treats a situation when a node is found to be dead.
 */
@Component
class NodeFoundDeadHelper {

    private static final Trace LOGGER = TraceManager.getTrace(NodeFoundDeadHelper.class);

    @Autowired private RepositoryService repositoryService;
    @Autowired private TaskHandlerRegistry taskHandlerRegistry;

    /**
     * We know that the specified nodes went from `UP` to the `DOWN` state. So we want to mark the relevant
     * tasks as not running on these nodes.
     */
    void markTasksAsNotRunning(Set<String> nodes, OperationResult result) throws SchemaException {
        Iterator<String> iterator = nodes.iterator();
        if (!iterator.hasNext()) {
            return;
        }
        // TODO create a utility method to create multi-valued "eq" disjunction
        S_MatchingRuleEntry q = PrismContext.get().queryFor(TaskType.class)
                .item(TaskType.F_NODE).eq(iterator.next());
        while (iterator.hasNext()) {
            q = q.or().item(TaskType.F_NODE).eq(iterator.next());
        }
        ObjectQuery query = q.build();

        List<PrismObject<TaskType>> tasksOnDeadNodes =
                repositoryService.searchObjects(TaskType.class, query, null, result);

        LOGGER.info("Going to mark tasks as not running:\n{}\n{}",
                DebugUtil.debugDumpLazily(query, 1), DebugUtil.debugDumpLazily(tasksOnDeadNodes, 1));

        for (PrismObject<TaskType> task : tasksOnDeadNodes) {
            try {
                markTaskAsNotRunning(task, nodes, result);
            } catch (Exception e) {
                LoggingUtils.logException(LOGGER, "Task couldn't be marked as not running: {}", e, task);
                // We continue to mark the other tasks as not running
            }
        }
    }

    private void markTaskAsNotRunning(PrismObject<TaskType> task, Set<String> nodes, OperationResult result)
            throws CommonException {

        Holder<Boolean> changed = new Holder<>();

        repositoryService.modifyObjectDynamically(TaskType.class, task.getOid(), null,
                currentTask -> {
                    changed.setValue(false);
                    if (currentTask.getNode() != null && nodes.contains(currentTask.getNode())) {
                        S_ItemEntry builder =
                                PrismContext.get().deltaFor(TaskType.class)
                                        .item(TaskType.F_NODE).replace();

                        // The following should be always true. But let's check that just to be sure.
                        if (currentTask.getExecutionState() == TaskExecutionStateType.RUNNING) {
                            builder = builder.item(TaskType.F_EXECUTION_STATE).replace(TaskExecutionStateType.RUNNABLE);
                            changed.setValue(true);
                        }

                        return builder.asItemDeltas();
                    } else {
                        return List.of();
                    }
                }, null, result);

        // We invoke specific cleanup actions only if the task state was really changed from RUNNING to RUNNABLE by us.
        if (changed.getValue()) {
            Objects.requireNonNull(
                    taskHandlerRegistry.getHandler(task.asObjectable().getHandlerUri()),
                    "No handler")
                    .onNodeDown(task.asObjectable(), result);
        }
    }
}
