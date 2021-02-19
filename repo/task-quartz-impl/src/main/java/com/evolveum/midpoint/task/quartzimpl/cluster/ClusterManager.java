/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.task.quartzimpl.cluster;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.task.api.TaskManagerInitializationException;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerQuartzImpl;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeOperationalStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Responsible for keeping the cluster consistent.
 * (Clusterwide task management operations are in ExecutionManager.)
 *
 * @author Pavol Mederly
 */
public class ClusterManager {

    private static final Trace LOGGER = TraceManager.getTrace(ClusterManager.class);

    private static final String CLASS_DOT = ClusterManager.class.getName() + ".";
    private static final String CHECK_SYSTEM_CONFIGURATION_CHANGED = CLASS_DOT + "checkSystemConfigurationChanged";

    private final TaskManagerQuartzImpl taskManager;
    private final NodeRegistrar nodeRegistrar;

    private ClusterManagerThread clusterManagerThread;

    private static boolean updateNodeExecutionLimitations = true;           // turned off when testing

    public ClusterManager(TaskManagerQuartzImpl taskManager) {
        this.taskManager = taskManager;
        this.nodeRegistrar = new NodeRegistrar(taskManager, this);
    }

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

    public NodeType createOrUpdateNodeInRepo(OperationResult result) throws TaskManagerInitializationException {
        return nodeRegistrar.createOrUpdateNodeInRepo(result);
    }

    public PrismObject<NodeType> getLocalNodeObject() {
        return nodeRegistrar.getCachedLocalNodeObject();
    }

    public NodeType getFreshVerifiedLocalNodeObject(OperationResult result) {
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

    public void postConstruct() {
        nodeRegistrar.postConstruct();
    }

    public void preDestroy() {
        nodeRegistrar.preDestroy();
    }

    class ClusterManagerThread extends Thread {

        private boolean canRun = true;

        @Override
        public void run() {
            LOGGER.info("ClusterManager thread starting.");

            long nodeAlivenessCheckInterval = taskManager.getConfiguration().getNodeAlivenessCheckInterval() * 1000L;
            long lastNodeAlivenessCheck = 0;

            long delay = taskManager.getConfiguration().getNodeRegistrationCycleTime() * 1000L;
            while (canRun) {

                OperationResult result = new OperationResult(ClusterManagerThread.class + ".run");

                try {
                    checkSystemConfigurationChanged(result);

                    // these checks are separate in order to prevent a failure in one method blocking execution of others
                    try {
                        NodeType node = checkClusterConfiguration(result);                              // if error, the scheduler will be stopped
                        if (updateNodeExecutionLimitations && node != null) {
                            taskManager.getExecutionManager().setLocalExecutionLimitations(node);    // we want to set limitations ONLY if the cluster configuration passes (i.e. node object is not inadvertently overwritten)
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

                LOGGER.trace("ClusterManager thread sleeping for {} msec", delay);
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    LOGGER.trace("ClusterManager thread interrupted.");
                }
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
        for (PrismObject<NodeType> nodeObject : nodes) {
            NodeType node = nodeObject.asObjectable();
            if (isRemoteNode(node)) {
                if (shouldBeMarkedAsDown(node)) {
                    LOGGER.warn("Node {} is down, marking it as such", node);
                    List<ItemDelta<?, ?>> modifications = taskManager.getPrismContext().deltaFor(NodeType.class)
                            .item(NodeType.F_RUNNING).replace(false)
                            .item(NodeType.F_OPERATIONAL_STATUS).replace(NodeOperationalStateType.DOWN)
                            .asItemDeltas();
                    try {
                        getRepositoryService().modifyObject(NodeType.class, node.getOid(), modifications, result);
                    } catch (ObjectNotFoundException | ObjectAlreadyExistsException e) {
                        LoggingUtils.logUnexpectedException(LOGGER, "Couldn't mark node {} as down", e, node);
                    }
                } else if (startingForTooLong(node)) {
                    LOGGER.warn("Node {} is starting for too long. Last check-in time = {}", node, node.getLastCheckInTime());
                    // TODO should we mark this node as down?
                }
            }
        }
    }

    private boolean isRemoteNode(NodeType node) {
        return taskManager.getNodeId() == null || !taskManager.getNodeId().equals(node.getNodeIdentifier());
    }

    private boolean shouldBeMarkedAsDown(NodeType node) {
        return node.getOperationalStatus() == NodeOperationalStateType.UP && (node.getLastCheckInTime() == null ||
                System.currentTimeMillis() - node.getLastCheckInTime().toGregorianCalendar().getTimeInMillis()
                        > taskManager.getConfiguration().getNodeAlivenessTimeout() * 1000L);
    }

    private boolean startingForTooLong(NodeType node) {
        return node.getOperationalStatus() == NodeOperationalStateType.STARTING && (node.getLastCheckInTime() == null ||
                System.currentTimeMillis() - node.getLastCheckInTime().toGregorianCalendar().getTimeInMillis()
                        > taskManager.getConfiguration().getNodeStartupTimeout() * 1000L);
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
//            QueryType q = QueryUtil.createNameQuery(nodeIdentifier);        // TODO change to query-by-node-id
            ObjectQuery q = ObjectQueryUtil.createNameQuery(NodeType.class, taskManager.getPrismContext(), nodeIdentifier);
            List<PrismObject<NodeType>> nodes = taskManager.getRepositoryService().searchObjects(NodeType.class, q, null, result);
            if (nodes.isEmpty()) {
//                result.recordFatalError("A node with identifier " + nodeIdentifier + " does not exist.");
                throw new ObjectNotFoundException("A node with identifier " + nodeIdentifier + " does not exist.");
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
            taskManager.getSystemConfigurationChangeDispatcher().dispatch(false, false, result);
            result.computeStatus();
        } catch (Throwable t) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't apply system configuration", t);
            result.recordFatalError("Couldn't apply system configuration: " + t.getMessage(), t);
        }
    }

    private long lastCheckedWaitingTasks = 0L;

    private void checkWaitingTasks(OperationResult result) throws SchemaException {
        if (System.currentTimeMillis() > lastCheckedWaitingTasks + taskManager.getConfiguration().getWaitingTasksCheckInterval() * 1000L) {
            lastCheckedWaitingTasks = System.currentTimeMillis();
            taskManager.checkWaitingTasks(result);
        }
    }

    private long lastCheckedStalledTasks = 0L;

    private void checkStalledTasks(OperationResult result) {
        if (System.currentTimeMillis() > lastCheckedStalledTasks + taskManager.getConfiguration().getStalledTasksCheckInterval() * 1000L) {
            lastCheckedStalledTasks = System.currentTimeMillis();
            taskManager.checkStalledTasks(result);
        }
    }
}
