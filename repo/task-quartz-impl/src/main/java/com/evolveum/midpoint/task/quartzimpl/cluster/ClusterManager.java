/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.task.quartzimpl.cluster;

import com.evolveum.midpoint.common.LoggingConfigurationManager;
import com.evolveum.midpoint.common.ProfilingConfigurationManager;
import com.evolveum.midpoint.common.SystemConfigurationHolder;
import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.SystemConfigurationTypeUtil;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.task.api.TaskManagerInitializationException;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerQuartzImpl;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;

import java.util.List;

import org.apache.commons.configuration.Configuration;

/**
 * Responsible for keeping the cluster consistent.
 * (Clusterwide task management operations are in ExecutionManager.)
 *
 * @author Pavol Mederly
 */
public class ClusterManager {

    private static final transient Trace LOGGER = TraceManager.getTrace(ClusterManager.class);

    private static final String CLASS_DOT = ClusterManager.class.getName() + ".";
    private static final String CHECK_SYSTEM_CONFIGURATION_CHANGED = CLASS_DOT + "checkSystemConfigurationChanged";
    private static final String CHECK_WAITING_TASKS = CLASS_DOT + "checkWaitingTasks";

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


    public void deleteNode(String nodeOid, OperationResult result) throws SchemaException, ObjectNotFoundException {
        nodeRegistrar.deleteNode(nodeOid, result);
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

            long delay = taskManager.getConfiguration().getNodeRegistrationCycleTime() * 1000L;
            while (canRun) {

                OperationResult result = new OperationResult(ClusterManagerThread.class + ".run");

                try {

                    checkSystemConfigurationChanged(result);

                    // these checks are separate in order to prevent a failure in one method blocking execution of others
                    try {
                        checkClusterConfiguration(result);                          // if error, the scheduler will be stopped
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

                } catch(Throwable t) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Unexpected exception in ClusterManager thread; continuing execution.", t);
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

    private OperationResult createOperationResult(String methodName) {
        return new OperationResult(ClusterManager.class.getName() + "." + methodName);
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
     *
     * @param parentResult
     */

    public void checkSystemConfigurationChanged(OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(CHECK_SYSTEM_CONFIGURATION_CHANGED);

        PrismObject<SystemConfigurationType> systemConfiguration;
        try {
            PrismObject<SystemConfigurationType> config = getRepositoryService().getObject(SystemConfigurationType.class,
                    SystemObjectsType.SYSTEM_CONFIGURATION.value(), null, result);

            String versionInRepo = config.getVersion();
            String versionApplied = LoggingConfigurationManager.getCurrentlyUsedVersion();

            // we do not try to determine which one is "newer" - we simply use the one from repo
            if (!versionInRepo.equals(versionApplied)) {
                Configuration systemConfigFromFile = taskManager.getMidpointConfiguration()
                        .getConfiguration(MidpointConfiguration.SYSTEM_CONFIGURATION_SECTION);
                if (systemConfigFromFile != null && versionApplied == null && systemConfigFromFile
						.getBoolean(LoggingConfigurationManager.SYSTEM_CONFIGURATION_SKIP_REPOSITORY_LOGGING_SETTINGS, false)) {
                    LOGGER.warn("Skipping application of repository logging configuration because {}=true (version={})",
                            LoggingConfigurationManager.SYSTEM_CONFIGURATION_SKIP_REPOSITORY_LOGGING_SETTINGS, versionInRepo);
                    // But pretend that this was applied so the next update works normally
                    LoggingConfigurationManager.setCurrentlyUsedVersion(versionInRepo);
                } else {
                    LoggingConfigurationType loggingConfig = ProfilingConfigurationManager
                            .checkSystemProfilingConfiguration(config);
                    LoggingConfigurationManager.configure(loggingConfig, versionInRepo, result);
                }

                SystemConfigurationHolder.setCurrentConfiguration(
                        config.asObjectable());       // we rely on LoggingConfigurationManager to correctly record the current version
                SecurityUtil.setRemoteHostAddressHeaders(config.asObjectable());

				getRepositoryService().applyFullTextSearchConfiguration(config.asObjectable().getFullTextSearch());
                SystemConfigurationTypeUtil.applyOperationResultHandling(config.asObjectable());
            } else {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("System configuration change check: version in repo = version currently applied = {}", versionApplied);
                }
            }

            if (result.isUnknown()) {
                result.computeStatus();
            }

        } catch (ObjectNotFoundException e) {
            LoggingConfigurationManager.resetCurrentlyUsedVersion();        // because the new config (if any) will have version number probably starting at 1 - so to be sure to read it when it comes [hope this never occurs :)]
            String message = "No system configuration found, skipping application of system settings";
            LOGGER.error(message + ": " + e.getMessage(), e);
            result.recordWarning(message, e);
        } catch (SchemaException e) {
            String message = "Schema error in system configuration, skipping application of system settings";
            LOGGER.error(message + ": " + e.getMessage(), e);
            result.recordWarning(message, e);
        } catch (RuntimeException e) {
            String message = "Runtime exception in system configuration processing, skipping application of system settings";
            LOGGER.error(message + ": " + e.getMessage(), e);
            result.recordWarning(message, e);
        }

    }


    private long lastCheckedWaitingTasks = 0L;

    public void checkWaitingTasks(OperationResult result) throws SchemaException {
        if (System.currentTimeMillis() > lastCheckedWaitingTasks + taskManager.getConfiguration().getWaitingTasksCheckInterval() * 1000L) {
            lastCheckedWaitingTasks = System.currentTimeMillis();
            taskManager.checkWaitingTasks(result);
        }
    }

    private long lastCheckedStalledTasks = 0L;

    public void checkStalledTasks(OperationResult result) throws SchemaException {
        if (System.currentTimeMillis() > lastCheckedStalledTasks + taskManager.getConfiguration().getStalledTasksCheckInterval() * 1000L) {
            lastCheckedStalledTasks = System.currentTimeMillis();
            taskManager.checkStalledTasks(result);
        }
    }



}

/*

        if (configurationError) {
            LOGGER.info("Previous configuration error was not resolved. Please check your cluster configuration.");
            return false;
        }
*/