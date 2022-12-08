/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.task.quartzimpl.cluster;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.api.SystemConfigurationChangeDispatcher;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerConfiguration;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerQuartzImpl;
import com.evolveum.midpoint.task.quartzimpl.TaskQuartzImpl;
import com.evolveum.midpoint.task.quartzimpl.quartz.LocalScheduler;
import com.evolveum.midpoint.task.quartzimpl.tasks.TaskStateManager;
import com.evolveum.midpoint.task.quartzimpl.tasks.TaskRetriever;
import com.evolveum.midpoint.task.quartzimpl.execution.StalledTasksWatcher;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClusterStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeOperationalStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskWaitingReasonType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Responsible for keeping the cluster consistent.
 *
 * TODO finish review of this class
 */
@Component
public class ClusterManager {

    private static final Trace LOGGER = TraceManager.getTrace(ClusterManager.class);

    private static final String CLASS_DOT = ClusterManager.class.getName() + ".";
    private static final String CHECK_SYSTEM_CONFIGURATION_CHANGED = CLASS_DOT + "checkSystemConfigurationChanged";

    @Autowired private TaskManagerQuartzImpl taskManager;
    @Autowired private NodeRegistrar nodeRegistrar;
    @Autowired private TaskManagerConfiguration configuration;
    @Autowired private StalledTasksWatcher stalledTasksWatcher;
    @Autowired private TaskRetriever taskRetriever;
    @Autowired private SystemConfigurationChangeDispatcher systemConfigurationChangeDispatcher;
    @Autowired private TaskStateManager taskStateManager;
    @Autowired private LocalScheduler localScheduler;

    private ClusterManagerThread clusterManagerThread;

    private static boolean updateNodeExecutionLimitations = true; // turned off when testing

    public static void setUpdateNodeExecutionLimitations(boolean value) {
        updateNodeExecutionLimitations = value;
    }

    /**
     * Verifies cluster consistency (currently checks whether there is no other node with the same ID,
     * and whether clustered/non-clustered nodes are OK).
     *
     * @return Current node record from repository, if everything is OK. Otherwise returns null.
     */
    @Nullable
    public NodeType checkClusterConfiguration(OperationResult result) {
        NodeType currentNode = nodeRegistrar.verifyNodeObject(result);     // if error, sets the error state and stops the scheduler
        nodeRegistrar.checkNonClusteredNodes(result);                       // the same
        return currentNode;
    }

    public boolean isClusterManagerThreadActive() {
        return clusterManagerThread != null && clusterManagerThread.isAlive();
    }

    public void recordNodeShutdown(OperationResult result) {
        nodeRegistrar.recordNodeShutdown(result);
    }

    public boolean isCurrentNode(PrismObject<NodeType> node) {
        return nodeRegistrar.isCurrentNode(node);
    }

    public boolean isCurrentNode(String node) {
        return nodeRegistrar.isCurrentNode(node);
    }

    public void deleteNode(String nodeOid, OperationResult result) throws SchemaException, ObjectNotFoundException {
        nodeRegistrar.deleteNode(nodeOid, result);
    }

    public @NotNull PrismObject<NodeType> getLocalNodeObject() {
        return nodeRegistrar.getCachedLocalNodeObjectRequired();
    }

    /** Returns null only in case of error. */
    public @Nullable NodeType getFreshVerifiedLocalNodeObject(OperationResult result) {
        return nodeRegistrar.verifyNodeObject(result);
    }

    public boolean isUpAndAlive(NodeType nodeType) {
        return nodeRegistrar.isUpAndAlive(nodeType);
    }

    public boolean isCheckingIn(NodeType nodeType) {
        return nodeRegistrar.isCheckingIn(nodeType);
    }

    public void registerNodeUp(OperationResult result) {
        LOGGER.info("Registering the node as started");
        nodeRegistrar.registerNodeUp(result);
    }

    public @NotNull ClusterStateType determineClusterState(OperationResult result) throws SchemaException {
        // We do not want to query cluster nodes at this moment. We rely on the repository information.
        SearchResultList<PrismObject<NodeType>> nodes =
                getRepositoryService().searchObjects(NodeType.class, null, null, result);
        ClusterStateType clusterState = new ClusterStateType();
        // TODO use query after making operationalState indexed
        for (PrismObject<NodeType> node : nodes) {
            String nodeIdentifier = node.asObjectable().getNodeIdentifier();
            if (node.asObjectable().getOperationalState() == NodeOperationalStateType.UP) {
                clusterState.getNodeUp().add(nodeIdentifier);
            }
            if (taskManager.isUpAndAlive(node.asObjectable())) {
                clusterState.getNodeUpAndAlive().add(nodeIdentifier);
            }
        }
        return clusterState;
    }

    class ClusterManagerThread extends Thread {

        private volatile boolean canRun = true;

        @Override
        public void run() {
            LOGGER.info("ClusterManager thread starting.");

            long nodeAlivenessCheckInterval = configuration.getNodeAlivenessCheckInterval() * 1000L;
            long lastNodeAlivenessCheck = 0;

            long delay = configuration.getNodeRegistrationCycleTime() * 1000L;
            while (canRun) {

                OperationResult result = new OperationResult(ClusterManagerThread.class + ".run");

                try {
                    checkSystemConfigurationChanged(result);

                    // these checks are separate in order to prevent a failure in one method blocking execution of others
                    try {
                        NodeType node = checkClusterConfiguration(result); // if error, the scheduler will be stopped
                        if (updateNodeExecutionLimitations && node != null) {
                            // we want to set limitations ONLY if the cluster configuration passes (i.e. node object is not inadvertently overwritten)
                            localScheduler.setLocalExecutionLimitations(node.getTaskExecutionLimitations());
                        }
                        nodeRegistrar.updateNodeObject(result);    // however, we want to update repo even in that case
                    } catch (Throwable t) {
                        LoggingUtils.logUnexpectedException(LOGGER, "Unexpected exception while checking cluster configuration; continuing execution.", t);
                    }

                    try {
                        checkWaitingTasks(result);
                    } catch (Throwable t) {
                        LoggingUtils.logUnexpectedException(LOGGER, "Unexpected exception while checking waiting tasks; continuing execution.", t);
                    }

                    try {
                        checkStalledTasks(result);
                    } catch (Throwable t) {
                        LoggingUtils.logUnexpectedException(LOGGER, "Unexpected exception while checking stalled tasks; continuing execution.", t);
                    }

                    if (System.currentTimeMillis() - lastNodeAlivenessCheck >= nodeAlivenessCheckInterval) {
                        try {
                            checkNodeAliveness(result);
                            lastNodeAlivenessCheck = System.currentTimeMillis();
                        } catch (Throwable t) {
                            LoggingUtils.logUnexpectedException(LOGGER, "Unexpected exception while checking node aliveness; continuing execution.", t);
                        }
                    }

                } catch (Throwable t) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Unexpected exception in ClusterManager thread; continuing execution.", t);
                }

                LOGGER.trace("ClusterManager thread sleeping for {} ms", delay);
                MiscUtil.sleepCatchingInterruptedException(delay);
            }

            LOGGER.info("ClusterManager thread stopping.");
        }

        private void signalShutdown() {
            canRun = false;
            this.interrupt();
        }

    }

    private void checkNodeAliveness(OperationResult result) throws SchemaException {
        SearchResultList<PrismObject<NodeType>> nodes = getRepositoryService()
                .searchObjects(NodeType.class, null, null, result);
        Set<String> nodesMarkedAsDown = new HashSet<>();
        for (PrismObject<NodeType> nodeObject : nodes) {
            NodeType node = nodeObject.asObjectable();
            if (isRemoteNode(node)) {
                if (shouldBeMarkedAsDown(node)) {
                    if (markNodeAsDown(node, result)) {
                        LOGGER.warn("Node {} is down, marked it as such", node);
                        nodesMarkedAsDown.add(node.getNodeIdentifier());
                    }
                } else if (isStartingForTooLong(node)) {
                    LOGGER.warn("Node {} is starting for too long. Last check-in time = {}", node, node.getLastCheckInTime());
                    // TODO should we mark this node as down?
                }
            }
        }
        taskStateManager.markTasksAsNotRunning(nodesMarkedAsDown, result);
    }

    /**
     * @return true if we were the one that marked the node as down. This is to avoid processing tasks related to the dead
     * node on more than one surviving nodes.
     */
    private boolean markNodeAsDown(NodeType node, OperationResult result) {
        Holder<Boolean> wasUpHolder = new Holder<>();
        try {
            getRepositoryService().modifyObjectDynamically(NodeType.class, node.getOid(), null,
                    currentNode -> {
                        if (currentNode.getOperationalState() == NodeOperationalStateType.UP) {
                            wasUpHolder.setValue(true);
                            return PrismContext.get().deltaFor(NodeType.class)
                                    .item(NodeType.F_OPERATIONAL_STATE).replace(NodeOperationalStateType.DOWN)
                                    .asItemDeltas();
                        } else {
                            wasUpHolder.setValue(false);
                            return List.of();
                        }
                    }, null, result);
        } catch (ObjectNotFoundException | ObjectAlreadyExistsException | SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't mark node {} as down", e, node);
        }
        return wasUpHolder.getValue();
    }

    private boolean isRemoteNode(NodeType node) {
        return !taskManager.getNodeId().equals(node.getNodeIdentifier());
    }

    private boolean shouldBeMarkedAsDown(NodeType node) {
        return node.getOperationalState() == NodeOperationalStateType.UP &&
                isCheckInTimeLagging(node, configuration.getNodeAlivenessTimeout());
    }

    private boolean isStartingForTooLong(NodeType node) {
        return node.getOperationalState() == NodeOperationalStateType.STARTING &&
                isCheckInTimeLagging(node, configuration.getNodeStartupTimeout());
    }

    private boolean isCheckInTimeLagging(NodeType node, int secondsLimit) {
        Long lastCheckInTimestamp = MiscUtil.asLong(node.getLastCheckInTime());
        return lastCheckInTimestamp == null ||
                System.currentTimeMillis() > lastCheckInTimestamp + secondsLimit * 1000L;
    }

    public void stopClusterManagerThread(long waitTime, OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(ClusterManager.class.getName() + ".stopClusterManagerThread");
        result.addParam("waitTime", waitTime);

        if (clusterManagerThread != null) {
            clusterManagerThread.signalShutdown();
            try {
                clusterManagerThread.join(waitTime);
            } catch (InterruptedException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Waiting for ClusterManagerThread shutdown was interrupted", e);
            }
            if (clusterManagerThread.isAlive()) {
                result.recordWarning("ClusterManagerThread shutdown requested but after " + waitTime + " ms it is still running.");
            } else {
                result.recordSuccess();
            }
        } else {
            result.recordSuccess();
        }
    }

    public void startClusterManagerThread() {
        clusterManagerThread = new ClusterManagerThread();
        clusterManagerThread.setName("ClusterManagerThread");
        clusterManagerThread.start();
    }

    private RepositoryService getRepositoryService() {
        return taskManager.getRepositoryService();
    }

    public String dumpNodeInfo(NodeType node) {
        return node.getNodeIdentifier() + " (" + node.getHostname() + ")";
    }

    public List<PrismObject<NodeType>> getAllNodes(OperationResult result) {
        try {
            return getRepositoryService().searchObjects(NodeType.class, null, null, result);
        } catch (SchemaException e) {       // should not occur
            throw new SystemException("Cannot get the list of nodes from the repository", e);
        }
    }

    public PrismObject<NodeType> getNode(String nodeOid, OperationResult result) throws SchemaException, ObjectNotFoundException {
        return getRepositoryService().getObject(NodeType.class, nodeOid, null, result);
    }


    public PrismObject<NodeType> getNodeById(String nodeIdentifier, OperationResult result) throws ObjectNotFoundException {
        try {
            ObjectQuery q = ObjectQueryUtil.createNameQuery(NodeType.class, taskManager.getPrismContext(), nodeIdentifier);
            List<PrismObject<NodeType>> nodes = taskManager.getRepositoryService().searchObjects(NodeType.class, q, null, result);
            if (nodes.isEmpty()) {
                throw new ObjectNotFoundException(
                        "A node with identifier " + nodeIdentifier + " does not exist.",
                        NodeType.class, nodeIdentifier);
            } else if (nodes.size() > 1) {
                throw new SystemException("Multiple nodes with the same identifier '" + nodeIdentifier + "' in the repository.");
            } else {
                return nodes.get(0);
            }
        } catch (SchemaException e) {       // should not occur
            throw new SystemException("Cannot get the list of nodes from the repository", e);
        }
    }

    /**
     * Check whether system configuration has not changed in repository (e.g. by another node in cluster).
     * Applies new configuration if so.
     */
    private void checkSystemConfigurationChanged(OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(CHECK_SYSTEM_CONFIGURATION_CHANGED);
        try {
            systemConfigurationChangeDispatcher.dispatch(false, false, result);
            result.computeStatus();
        } catch (Throwable t) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't apply system configuration", t);
            result.recordFatalError("Couldn't apply system configuration: " + t.getMessage(), t);
        }
    }

    private long lastCheckedWaitingTasks = 0L;

    private void checkWaitingTasks(OperationResult result) throws SchemaException {
        if (System.currentTimeMillis() > lastCheckedWaitingTasks + configuration.getWaitingTasksCheckInterval() * 1000L) {
            lastCheckedWaitingTasks = System.currentTimeMillis();
            doCheckWaitingTasks(result);
        }
    }

    private void doCheckWaitingTasks(OperationResult result) throws SchemaException {
        int count = 0;
        List<? extends Task> tasks = taskRetriever.listWaitingTasks(TaskWaitingReasonType.OTHER_TASKS, result);
        for (Task task : tasks) {
            try {
                taskStateManager.unpauseIfPossible((TaskQuartzImpl) task, result);
                count++;
            } catch (SchemaException | ObjectNotFoundException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't check dependencies for task {}", e, task);
            }
        }
        LOGGER.trace("Check waiting tasks completed; {} tasks checked.", count);
    }

    private long lastCheckedStalledTasks = 0L;

    private void checkStalledTasks(OperationResult result) {
        if (System.currentTimeMillis() > lastCheckedStalledTasks + configuration.getStalledTasksCheckInterval() * 1000L) {
            lastCheckedStalledTasks = System.currentTimeMillis();
            stalledTasksWatcher.checkStalledTasks(result);
        }
    }
}
