/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl.run;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.quartzimpl.RunningTaskQuartzImpl;
import com.evolveum.midpoint.task.quartzimpl.TaskBeans;
import com.evolveum.midpoint.task.quartzimpl.cluster.ClusterStatusInformation;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionConstraintsType;

import org.jetbrains.annotations.NotNull;

import javax.xml.datatype.Duration;
import java.util.*;

/**
 * Checks clusterwide task group limits.
 */
class GroupLimitsChecker {

    private static final Trace LOGGER = TraceManager.getTrace(GroupLimitsChecker.class);

    private static final int DEFAULT_RESCHEDULE_TIME_FOR_GROUP_LIMIT = 60;
    private static final int RESCHEDULE_TIME_RANDOMIZATION_INTERVAL = 3;

    private final RunningTaskQuartzImpl task;
    private final TaskBeans beans;

    public GroupLimitsChecker(RunningTaskQuartzImpl task, TaskBeans beans) {

        this.task = task;
        this.beans = beans;
    }

    /**
     * Checks if the task is allowed to execute (due to group limits).
     */
    public RescheduleTime checkIfAllowed(OperationResult result) {

        TaskExecutionConstraintsType executionConstraints = task.getExecutionConstraints();
        if (executionConstraints == null) {
            return null;
        }

        Map<String, GroupExecInfo> groupMap = createGroupMap(task, result);
        LOGGER.trace("groupMap = {}", groupMap);

        for (Map.Entry<String, GroupExecInfo> entry : groupMap.entrySet()) {
            String group = entry.getKey();
            int limit = entry.getValue().limit;
            Collection<Task> tasksInGroup = entry.getValue().tasks;
            if (tasksInGroup.size() >= limit) {
                RescheduleTime rescheduleTime = getRescheduleTime(executionConstraints,
                        task.getNextRunStartTime(result));
                LOGGER.info("Limit of {} task(s) in group {} would be exceeded if task {} would start. Existing tasks: {}."
                                + " Will try again at {}{}.", limit, group, task, tasksInGroup, rescheduleTime.asDate(),
                        rescheduleTime.regular ? " (i.e. at the next regular run time)" : "");
                return rescheduleTime;
            }
        }
        return null;
    }

    @NotNull
    private Map<String, GroupExecInfo> createGroupMap(RunningTaskQuartzImpl task, OperationResult result) {
        Map<String, GroupExecInfo> groupMap = new HashMap<>();
        Map<String, Integer> groupsWithLimits = task.getGroupsWithLimits();
        if (!groupsWithLimits.isEmpty()) {
            groupsWithLimits.forEach((g, l) -> groupMap.put(g, new GroupExecInfo(l)));
            ClusterStatusInformation csi = beans.clusterStatusInformationRetriever
                    .getClusterStatusInformation(true, false, result);
            for (ClusterStatusInformation.TaskInfo taskInfo : csi.getTasks()) {
                if (task.getOid().equals(taskInfo.getOid())) {
                    continue;
                }
                Task otherTask;
                try {
                    otherTask = beans.taskRetriever.getTaskPlain(taskInfo.getOid(), result);
                } catch (ObjectNotFoundException e) {
                    LOGGER.debug("Couldn't find running task {} when checking execution constraints: {}", taskInfo.getOid(),
                            e.getMessage());
                    continue;
                } catch (SchemaException e) {
                    LoggingUtils.logUnexpectedException(LOGGER,
                            "Couldn't retrieve running task {} when checking execution constraints", e, taskInfo.getOid());
                    continue;
                }
                addToGroupMap(groupMap, otherTask);
            }
        }
        return groupMap;
    }

    private void addToGroupMap(Map<String, GroupExecInfo> groupMap, Task otherTask) {
        for (Map.Entry<String, Integer> otherGroupWithLimit : otherTask.getGroupsWithLimits().entrySet()) {
            String otherGroup = otherGroupWithLimit.getKey();
            GroupExecInfo groupExecInfo = groupMap.get(otherGroup);
            if (groupExecInfo != null) {
                Integer otherLimit = otherGroupWithLimit.getValue();
                groupExecInfo.accept(otherLimit, otherTask);
            }
        }
    }

    private RescheduleTime getRescheduleTime(TaskExecutionConstraintsType executionConstraints, Long nextTaskRunTime) {
        long retryAt;
        Duration retryAfter = executionConstraints != null ? executionConstraints.getRetryAfter() : null;
        if (retryAfter != null) {
            retryAt = XmlTypeConverter.toMillis(
                    XmlTypeConverter.addDuration(
                            XmlTypeConverter.createXMLGregorianCalendar(new Date()), retryAfter));
        } else {
            retryAt = System.currentTimeMillis() + DEFAULT_RESCHEDULE_TIME_FOR_GROUP_LIMIT * 1000L;
        }
        retryAt += Math.random() * RESCHEDULE_TIME_RANDOMIZATION_INTERVAL * 1000.0; // to avoid endless collisions
        if (nextTaskRunTime != null && nextTaskRunTime < retryAt) {
            return new RescheduleTime(nextTaskRunTime, true);
        } else {
            return new RescheduleTime(retryAt, false);
        }
    }

    static class RescheduleTime {

        final long timestamp;
        final boolean regular;

        RescheduleTime(long timestamp, boolean regular) {
            this.timestamp = timestamp;
            this.regular = regular;
        }

        public Date asDate() {
            return new Date(timestamp);
        }
    }

    static class GroupExecInfo {
        private int limit;
        private final Collection<Task> tasks = new ArrayList<>();

        private GroupExecInfo(Integer l) {
            limit = l != null ? l : Integer.MAX_VALUE;
        }

        public void accept(Integer limit, Task task) {
            if (limit != null && limit < this.limit) {
                this.limit = limit;
            }
            if (tasks.stream().noneMatch(t -> Objects.equals(t.getOid(), task.getOid()))) {    // just for sure
                tasks.add(task);
            }
        }

        @Override
        public String toString() {
            return "{limit=" + limit + ", tasks=" + tasks + "}";
        }
    }
}
