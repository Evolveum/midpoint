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

package com.evolveum.midpoint.task.quartzimpl.execution;/*
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
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.ClusterStatusInformation;
import com.evolveum.midpoint.task.api.Node;
import com.evolveum.midpoint.task.api.NodeExecutionStatus;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerConfiguration;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerQuartzImpl;
import com.evolveum.midpoint.task.quartzimpl.cluster.ClusterManager;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.NodeType;

import org.quartz.Scheduler;
import org.quartz.core.jmx.QuartzSchedulerMBean;

import javax.management.*;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Manages remote nodes. Concerned mainly with
 * - stopping threads and querying their state,
 * - starting/stopping scheduler and querying its state.
 *
 * @author Pavol Mederly
 */

public class RemoteNodesManager {

    private static final transient Trace LOGGER = TraceManager.getTrace(RemoteNodesManager.class);

    private TaskManagerQuartzImpl taskManager;

    public RemoteNodesManager(TaskManagerQuartzImpl taskManager) {
        this.taskManager = taskManager;
    }




    /**
     * Used exclusively for collecting running task information.
     *
     * @see ExecutionManager#getClusterStatusInformation(boolean, com.evolveum.midpoint.schema.result.OperationResult)
     * @param info A structure to which information should be added
     * @param node Node which to query
     */
    void addNodeStatusFromRemoteNode(ClusterStatusInformation info, PrismObject<NodeType> node, OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(RemoteNodesManager.class.getName() + ".addNodeStatusFromRemoteNode");
        result.addParam("node", node);

        String nodeName = node.asObjectable().getNodeIdentifier();
        String address = node.asObjectable().getHostname() + ":" + node.asObjectable().getJmxPort();
        Node nodeInfo = new Node(node);

        if (!taskManager.getClusterManager().isUp(node.asObjectable())) {
            nodeInfo.setNodeExecutionStatus(NodeExecutionStatus.DOWN);
            info.addNodeInfo(nodeInfo);
            result.recordStatus(OperationResultStatus.SUCCESS, "Node is down");
            return;
        }

        JMXConnector connector = null;

        try {
            MBeanServerConnection mbsc;

            try {
                connector = connectViaJmx(address);
                mbsc = connector.getMBeanServerConnection();
            } catch (IOException e) {
                LoggingUtils.logException(LOGGER, "Cannot connect to the remote node {} at {}", e, nodeName, address);
                nodeInfo.setNodeExecutionStatus(NodeExecutionStatus.COMMUNICATION_ERROR);
                nodeInfo.setConnectionError("Cannot connect to the remote node: " + e.getMessage());
                info.addNodeInfo(nodeInfo);
                result.recordWarning("Cannot connect to the remote node " + nodeName + " at " + address, e);
                return;
            }

            try {

                QuartzSchedulerMBean mbeanProxy = getMBeanProxy(nodeName, mbsc);

                boolean running = false, down = true;
                if (mbeanProxy != null) {
                    try {
                        running = mbeanProxy.isStarted() && !mbeanProxy.isShutdown() && !mbeanProxy.isStandbyMode();
                        down = mbeanProxy.isShutdown();
                    } catch (Exception e) {     // was: InstanceNotFoundException but it does not seem to work
                        String message = "Cannot get information from scheduler " + nodeName + " because it does not exist or is shut down.";
                        LoggingUtils.logException(LOGGER, message, e);
                        result.recordWarning(message);
                    }
                } else {
                    result.recordWarning("Cannot get information from node " + nodeName + " at " + address + " because the JMX object for scheduler cannot be found on that node.");
                }

                LOGGER.trace(" - scheduler found = " + (mbeanProxy != null) + ", running = " + running + ", shutdown = " + down);

                if (down) {
                    nodeInfo.setNodeExecutionStatus(NodeExecutionStatus.ERROR);         // this is a mark of error situation (we expect that during ordinary shutdown the node quickly goes down so there is little probability of getting this status on that occasion)
                } else if (running) {
                    nodeInfo.setNodeExecutionStatus(NodeExecutionStatus.RUNNING);
                } else {
                    nodeInfo.setNodeExecutionStatus(NodeExecutionStatus.PAUSED);
                }

                List<ClusterStatusInformation.TaskInfo> taskInfoList = new ArrayList<ClusterStatusInformation.TaskInfo>();
                if (mbeanProxy != null) {
                    TabularData jobs = mbeanProxy.getCurrentlyExecutingJobs();
                    for (CompositeData job : (Collection<CompositeData>) jobs.values()) {
                        String oid = (String) job.get("jobName");
                        LOGGER.trace(" - task oid = " + oid);
                        taskInfoList.add(new ClusterStatusInformation.TaskInfo(oid));
                    }
                }

                if (result.isUnknown()) {
                    result.recordStatus(OperationResultStatus.SUCCESS, "Node " + nodeName + ": status = " + nodeInfo.getNodeExecutionStatus() + ", # of running tasks: " + taskInfoList.size());
                }
                info.addNodeAndTaskInfo(nodeInfo, taskInfoList);
            }
            catch (Exception e) {
                LoggingUtils.logException(LOGGER, "Cannot get information from the remote node {} at {}", e, nodeName, address);
                nodeInfo.setNodeExecutionStatus(NodeExecutionStatus.COMMUNICATION_ERROR);
                nodeInfo.setConnectionError("Cannot get information from the remote node: " + e.getMessage());
                result.recordWarning("Cannot get information from the remote node " + nodeName + " at " + address, e);
                info.addNodeInfo(nodeInfo);
                return;
            }

        }
        finally {
            try {
                if (connector != null) {
                    connector.close();
                }
            } catch (IOException e) {
                LoggingUtils.logException(LOGGER, "Cannot close JMX connection to {}", e, address);
            }

            result.recordSuccessIfUnknown();
        }
    }

    private NodeType getNode(String nodeIdentifier, OperationResult result) {
        try {
            return taskManager.getClusterManager().getNodeById(nodeIdentifier, result).asObjectable();
        } catch (ObjectNotFoundException e) {
            result.recordFatalError("A node with identifier " + nodeIdentifier + " does not exist.");
            return null;
        }
    }

    public void stopRemoteScheduler(String nodeIdentifier, OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(RemoteNodesManager.class.getName() + ".stopRemoteScheduler");
        result.addParam("nodeIdentifier", nodeIdentifier);

        NodeType node = getNode(nodeIdentifier, result);
        if (node == null) {
            return;
        }

        String nodeName = node.getNodeIdentifier();
        String address = node.getHostname() + ":" + node.getJmxPort();

        JMXConnector connector = null;

        try {
            MBeanServerConnection mbsc;

            try {
                connector = connectViaJmx(address);
                mbsc = connector.getMBeanServerConnection();
            } catch (IOException e) {
                LoggingUtils.logException(LOGGER, "Cannot connect to the remote node {} at {}", e, nodeName, address);
                result.recordFatalError("Cannot connect to the remote node " + nodeName + " at " + address + ": " + e.getMessage(), e);
                return;
            }

            try {
                QuartzSchedulerMBean mbeanProxy = getMBeanProxy(nodeName, mbsc);
                if (mbeanProxy != null) {
                    mbeanProxy.standby();
                    result.recordSuccess();
                } else {
                    result.recordWarning("Cannot stop the scheduler on node " + nodeName + " at " + address + " because the JMX object for scheduler cannot be found on that node.");
                }
                return;
            }
            catch (Exception e) {
                LoggingUtils.logException(LOGGER, "Cannot put remote scheduler into standby mode; remote node {} at {}", e, nodeName, address);
                result.recordFatalError("Cannot put remote scheduler " + nodeName + " at " + address + " into standby mode: " + e.getMessage());
                return;
            }

        }
        finally {
            try {
                if (connector != null) {
                    connector.close();
                }
            } catch (IOException e) {
                LoggingUtils.logException(LOGGER, "Cannot close JMX connection to {}", e, address);
            }
        }

    }

    void startRemoteScheduler(String nodeIdentifier, OperationResult result) {

        NodeType node = getNode(nodeIdentifier, result);
        if (node == null) {
            return;
        }

        String nodeName = node.getNodeIdentifier();
        String address = node.getHostname() + ":" + node.getJmxPort();

        JMXConnector connector = null;

        try {
            MBeanServerConnection mbsc;

            try {
                connector = connectViaJmx(address);
                mbsc = connector.getMBeanServerConnection();
            } catch (IOException e) {
                LoggingUtils.logException(LOGGER, "Cannot connect to the remote node {} at {}", e, nodeName, address);
                result.recordFatalError("Cannot connect to the remote node " + nodeName + " at " + address + ": " + e.getMessage(), e);
                return;
            }

            try {
                QuartzSchedulerMBean mbeanProxy = getMBeanProxy(nodeName, mbsc);
                if (mbeanProxy != null) {
                    mbeanProxy.start();
                    result.recordSuccessIfUnknown();
                } else {
                    result.recordFatalError("Cannot start remote scheduler " + nodeName + " at " + address + " because it cannot be found on that node.");
                }
                return;
            }
            catch (Exception e) {
                LoggingUtils.logException(LOGGER, "Cannot start remote scheduler; remote node {} at {}", e, nodeName, address);
                result.recordFatalError("Cannot start remote scheduler " + nodeName + " at " + address + ": " + e.getMessage());
                return;
            }

        }
        finally {
            try {
                if (connector != null) {
                    connector.close();
                }
            } catch (IOException e) {
                LoggingUtils.logException(LOGGER, "Cannot close JMX connection to {}", e, address);
            }
        }

    }

    private QuartzSchedulerMBean getMBeanProxy(String nodeName, MBeanServerConnection mbsc) throws MalformedObjectNameException {
        String mbeanNameAsString = "quartz:type=QuartzScheduler,name=midPointScheduler,instance=" + nodeName;
        ObjectName mbeanName = new ObjectName(mbeanNameAsString);

        try {
            if (mbsc.isRegistered(mbeanName)) {
                return JMX.newMBeanProxy(mbsc, mbeanName, QuartzSchedulerMBean.class, true);
            } else {
                LOGGER.trace("MBean " + mbeanNameAsString + " is not registered at " + nodeName);
                return null;
            }
        } catch (IOException e) {
            LoggingUtils.logException(LOGGER, "Cannot communicate with remote node via JMX", e);
            return null;
        }
    }

    private JMXConnector connectViaJmx(String address) throws IOException {

        JMXServiceURL url =
                new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + address + "/jmxrmi");

        Map<String,Object> env = new HashMap<String,Object>();
        String[] creds = {taskManager.getConfiguration().getJmxUsername(), taskManager.getConfiguration().getJmxPassword()};
        env.put(JMXConnector.CREDENTIALS, creds);
        return JmxClient.connectWithTimeout(url, env,
                taskManager.getConfiguration().getJmxConnectTimeout(), TimeUnit.SECONDS);
    }

    private TaskManagerConfiguration getConfiguration() {
        return taskManager.getConfiguration();
    }

    private ExecutionManager getGlobalExecutionManager() {
        return taskManager.getExecutionManager();
    }

    // the task should be really running
    void stopRemoteTaskRun(String oid, Node nodeInfo, OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(RemoteNodesManager.class.getName() + ".stopRemoteTaskRun");
        result.addParam("oid", oid);
        result.addParam("nodeInfo", nodeInfo);

        LOGGER.debug("Interrupting task " + oid + " running at " + getClusterManager().dumpNodeInfo(nodeInfo));

//        if (taskManager.isCurrentNode(nodeInfo.getNodeType())) {
//            taskManager.signalShutdownToTaskLocally(oid);
//            return;
//        }

//        if (!taskManager.isClustered()) {       // here we should not come
//            LOGGER.warn("Remote task interruption is applicable in clustered mode only; doing nothing.");
//            return;
//        }

        NodeType node = nodeInfo.getNodeType().asObjectable();
        String nodeName = node.getNodeIdentifier();
        String address = node.getHostname() + ":" + node.getJmxPort();

        JMXConnector connector = null;

        try {
            MBeanServerConnection mbsc;

            try {
                connector = connectViaJmx(address);
                mbsc = connector.getMBeanServerConnection();
            } catch (IOException e) {
                LoggingUtils.logException(LOGGER, "Cannot connect to the remote node {} at {}", e, nodeName, address);
                result.recordFatalError("Cannot connect to the remote node " + nodeName + " at " + address + ": " + e.getMessage(), e);
                return;
            }

            try {
                QuartzSchedulerMBean mbeanProxy = getMBeanProxy(nodeName, mbsc);
                if (mbeanProxy != null) {
                    mbeanProxy.interruptJob(oid, Scheduler.DEFAULT_GROUP);
                    result.recordSuccessIfUnknown();
                } else {
                    result.recordFatalError("Cannot interrupt job at " + nodeName + " at " + address + " because the JMX object for scheduler cannot be found on that node.");
                }
                LOGGER.debug("Successfully signalled shutdown to task " + oid + " running at " + getClusterManager().dumpNodeInfo(nodeInfo));
            }
            catch (Exception e) {
                LoggingUtils.logException(LOGGER, "Cannot signal task {} interruption to remote node {} at {}", e, oid, nodeName, address);
            }
        }
        finally {
            try {
                if (connector != null) {
                    connector.close();
                }
            } catch (IOException e) {
                LoggingUtils.logException(LOGGER, "Cannot close JMX connection to {}", e, address);
            }
        }
    }

    private ClusterManager getClusterManager() {
        return taskManager.getClusterManager();
    }

//    private OperationResult createOperationResult(String methodName) {
//        return new OperationResult(RemoteNodesManager.class.getName() + "." + methodName);
//    }


}
