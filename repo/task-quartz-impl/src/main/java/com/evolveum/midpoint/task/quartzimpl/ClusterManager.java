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
package com.evolveum.midpoint.task.quartzimpl;

import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Node;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.NodeType;

import java.util.*;

/**
 * Responsible for keeping the cluster consistent.
 * (Clusterwide task management operations are in GlobalExecutionManager.)
 *
 * @author Pavol Mederly
 */
public class ClusterManager {

    private static final transient Trace LOGGER = TraceManager.getTrace(ClusterManager.class);

    private TaskManagerQuartzImpl taskManager;

    private ClusterManagerThread clusterManagerThread;

    public ClusterManager(TaskManagerQuartzImpl taskManager) {
        this.taskManager = taskManager;
    }

    /**
     * Verifies cluster consistency (currently checks whether there is no other node with the same ID, and whether clustered/non-clustered nodes are OK).

     * @param result
     * @return
     */
    void checkClusterConfiguration(OperationResult result) {

//        LOGGER.trace("taskManager = " + taskManager);
//        LOGGER.trace("taskManager.getNodeRegistrar() = " + taskManager.getNodeRegistrar());

        taskManager.getNodeRegistrar().verifyNodeObject(result);     // if error, sets the error state and stops the scheduler
        taskManager.getNodeRegistrar().checkNonClusteredNodes(result); // the same
    }

    public boolean isClusterManagerThreadActive() {
        return clusterManagerThread != null && clusterManagerThread.isAlive();
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
                    taskManager.getNodeRegistrar().updateNodeObject(result);    // however, we want to update repo even in that case

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

    void stopClusterManagerThread(long waitTime, OperationResult parentResult) {

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
                parentResult.recordWarning("ClusterManagerThread shutdown requested but after " + waitTime + " ms it is still running.");
            } else {
                parentResult.recordSuccess();
            }
        } else {
            result.recordSuccess();
        }
    }

    void startClusterManagerThread() {
        clusterManagerThread = new ClusterManagerThread();
        clusterManagerThread.setName("ClusterManagerThread");
        clusterManagerThread.start();
    }



    private RepositoryService getRepositoryService() {
        return taskManager.getRepositoryService();
    }


    String dumpNodeInfo(Node nodeInfo) {
        NodeType node = nodeInfo.getNodeType().asObjectable();
        return node.getNodeIdentifier() + " (" + node.getHostname() + ")";
    }

    private OperationResult createOperationResult(String methodName) {
        return new OperationResult(ClusterManager.class.getName() + "." + methodName);
    }




}

/*

        if (configurationError) {
            LOGGER.info("Previous configuration error was not resolved. Please check your cluster configuration.");
            return false;
        }
*/