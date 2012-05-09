/**
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted 2011 [name of copyright owner]"
 *
 */
package com.evolveum.midpoint.task.quartzimpl.cluster;

import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Node;
import com.evolveum.midpoint.task.api.TaskManagerInitializationException;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerQuartzImpl;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.NodeType;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;

import java.util.List;

/**
 * Responsible for keeping the cluster consistent.
 * (Clusterwide task management operations are in ExecutionManager.)
 *
 * @author Pavol Mederly
 */
public class ClusterManager {

    private static final transient Trace LOGGER = TraceManager.getTrace(ClusterManager.class);

    private TaskManagerQuartzImpl taskManager;
    private NodeRegistrar nodeRegistrar;

    private ClusterManagerThread clusterManagerThread;

    public ClusterManager(TaskManagerQuartzImpl taskManager) {
        this.taskManager = taskManager;
        this.nodeRegistrar = new NodeRegistrar(taskManager, this);
    }

    /**
     * Verifies cluster consistency (currently checks whether there is no other node with the same ID, and whether clustered/non-clustered nodes are OK).

     * @param result
     * @return
     */
    public void checkClusterConfiguration(OperationResult result) {

//        LOGGER.trace("taskManager = " + taskManager);
//        LOGGER.trace("taskManager.getNodeRegistrar() = " + taskManager.getNodeRegistrar());

        nodeRegistrar.verifyNodeObject(result);     // if error, sets the error state and stops the scheduler
        nodeRegistrar.checkNonClusteredNodes(result); // the same
    }

    public boolean isClusterManagerThreadActive() {
        return clusterManagerThread != null && clusterManagerThread.isAlive();
    }

    public void recordNodeShutdown(OperationResult result) {
        nodeRegistrar.recordNodeShutdown(result);
    }

    public String getNodeId() {
        return nodeRegistrar.getNodeId();
    }

    public boolean isCurrentNode(PrismObject<NodeType> node) {
        return nodeRegistrar.isCurrentNode(node);
    }

    public boolean isCurrentNode(String node) {
        return nodeRegistrar.isCurrentNode(node);
    }


    public void deleteNode(String nodeIdentifier, OperationResult result) {
        nodeRegistrar.deleteNode(nodeIdentifier, result);
    }

    public void createNodeObject(OperationResult result) throws TaskManagerInitializationException {
        nodeRegistrar.createNodeObject(result);
    }

    public PrismObject<NodeType> getNodePrism() {
        return nodeRegistrar.getNodePrism();
    }

    public boolean isUp(NodeType nodeType) {
        return nodeRegistrar.isUp(nodeType);
    }


    class ClusterManagerThread extends Thread {

        boolean canRun = true;

        @Override
        public void run() {
            LOGGER.info("ClusterManager thread starting.");

            OperationResult result = new OperationResult(ClusterManagerThread.class + ".run");

            long delay = taskManager.getConfiguration().getNodeRegistrationCycleTime() * 1000L;
            while (canRun) {

                try {

                    checkClusterConfiguration(result);                          // if error, the scheduler will be stopped
                    nodeRegistrar.updateNodeObject(result);    // however, we want to update repo even in that case

                } catch(Throwable t) {
                    LoggingUtils.logException(LOGGER, "Unexpected exception in ClusterManager thread; continuing execution.", t);
                }

                LOGGER.trace("ClusterManager thread sleeping for " + delay + " msec");
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    LOGGER.trace("ClusterManager thread interrupted.");
                }
            }

            LOGGER.info("ClusterManager thread stopping.");
        }

        public void signalShutdown() {
            canRun = false;
            this.interrupt();
        }

    }

    public void stopClusterManagerThread(long waitTime, OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(ClusterManager.class.getName() + ".stopClusterManagerThread");
        result.addParam("waitTime", waitTime);

        if (clusterManagerThread != null) {
            clusterManagerThread.signalShutdown();
            try {
                clusterManagerThread.join(waitTime);
            } catch (InterruptedException e) {
                LoggingUtils.logException(LOGGER, "Waiting for ClusterManagerThread shutdown was interrupted", e);
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


    public String dumpNodeInfo(Node nodeInfo) {
        NodeType node = nodeInfo.getNodeType().asObjectable();
        return node.getNodeIdentifier() + " (" + node.getHostname() + ")";
    }

    private OperationResult createOperationResult(String methodName) {
        return new OperationResult(ClusterManager.class.getName() + "." + methodName);
    }


    public List<PrismObject<NodeType>> getAllNodes(OperationResult result) {
        try {
            return getRepositoryService().searchObjects(NodeType.class, QueryUtil.createAllObjectsQuery(), new PagingType(), result);
        } catch (SchemaException e) {       // should not occur
            throw new SystemException("Cannot get the list of nodes from the repository", e);
        }
    }

    public PrismObject<NodeType> getNodeById(String nodeIdentifier, OperationResult result) throws ObjectNotFoundException {
        try {
            QueryType q = QueryUtil.createNameQuery(nodeIdentifier);        // TODO change to query-by-node-id
            List<PrismObject<NodeType>> nodes = taskManager.getRepositoryService().searchObjects(NodeType.class, q, new PagingType(), result);
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



}

/*

        if (configurationError) {
            LOGGER.info("Previous configuration error was not resolved. Please check your cluster configuration.");
            return false;
        }
*/