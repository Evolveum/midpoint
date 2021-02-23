/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.task.quartzimpl.cluster.ClusterManager;
import com.evolveum.midpoint.task.quartzimpl.quartz.LocalScheduler;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeErrorStateType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeExecutionStateType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;

import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps the state of the local node:
 *
 * - error state
 * - executing standard tasks (i.e. not lightweight ones)
 */
@Component
public class LocalNodeState {

    private static final Trace LOGGER = TraceManager.getTrace(LocalNodeState.class);

    @Autowired private ClusterManager clusterManager;
    @Autowired private LocalScheduler localScheduler;

    /**
     * Locally running task instances - here are EXACT instances of {@link TaskQuartzImpl}
     * that are used to execute handlers.
     * Use ONLY for those actions that need to work with these instances,
     * e.g. when calling heartbeat() methods on them.
     * For any other business please use LocalNodeManager.getLocallyRunningTasks(...).
     * Maps task id to the task instance.
     */
    private final Map<String, RunningTaskQuartzImpl> locallyRunningTaskInstancesMap = new ConcurrentHashMap<>();

    /**
     * Error status for this node.
     * Local Quartz scheduler is not allowed to be started if this status is not "OK".
     */
    private NodeErrorStateType errorState = NodeErrorStateType.OK;

    public boolean isInErrorState() {
        return errorState != NodeErrorStateType.OK;
    }

    public NodeErrorStateType getErrorState() {
        return errorState;
    }

    public void setErrorState(NodeErrorStateType errorState) {
        this.errorState = errorState;
    }

    /**
     * @return current local node information, updated with local node execution and error status.
     * Returned value is fresh, so it can be modified as needed.
     */
    @Nullable
    public NodeType getLocalNode() {
        PrismObject<NodeType> localNode = clusterManager.getLocalNodeObject();
        if (localNode == null) {
            return null;
        }
        NodeType node = localNode.clone().asObjectable();
        node.setExecutionState(getExecutionState());
        node.setErrorState(errorState);
        return node;
    }

    NodeExecutionStateType getExecutionState() {
        if (errorState != NodeErrorStateType.OK) {
            return NodeExecutionStateType.ERROR;
        } else {
            try {
                return localScheduler.isRunning() ? NodeExecutionStateType.RUNNING : NodeExecutionStateType.PAUSED;
            } catch (Exception e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't determine local scheduler state", e);
                return NodeExecutionStateType.COMMUNICATION_ERROR;
            }
        }
    }

    public void registerRunningTask(RunningTaskQuartzImpl task) {
        synchronized (locallyRunningTaskInstancesMap) {
            locallyRunningTaskInstancesMap.put(task.getTaskIdentifier(), task);
            LOGGER.trace("Registered task {}, locally running instances = {}", task, locallyRunningTaskInstancesMap);
        }
    }

    public void unregisterRunningTask(RunningTaskQuartzImpl task) {
        synchronized (locallyRunningTaskInstancesMap) {
            locallyRunningTaskInstancesMap.remove(task.getTaskIdentifier());
            LOGGER.trace("Unregistered task {}, locally running instances = {}", task, locallyRunningTaskInstancesMap);
        }
    }

    public RunningTaskQuartzImpl getLocallyRunningTaskByIdentifier(String lightweightIdentifier) {
        synchronized (locallyRunningTaskInstancesMap) {
            return locallyRunningTaskInstancesMap.get(lightweightIdentifier);
        }
    }

    public Map<String, RunningTaskQuartzImpl> getLocallyRunningTaskInstances() {
        synchronized (locallyRunningTaskInstancesMap) { // must be synchronized while iterating over it (addAll)
            return new HashMap<>(locallyRunningTaskInstancesMap);
        }
    }
}
