package com.evolveum.midpoint.task.quartzimpl;/*
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
import com.evolveum.midpoint.task.api.ClusterStatusInformation;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.NodeType;
import org.apache.commons.lang.Validate;
import org.quartz.Scheduler;
import org.quartz.core.jmx.QuartzSchedulerMBean;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
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
     * @see GlobalExecutionManager#getClusterStatusInformation(boolean)
     * @param info A structure to which information should be added
     * @param node Node which to query
     */
    void addNodeStatusFromRemoteNode(ClusterStatusInformation info, PrismObject<NodeType> node) {

        String nodeName = node.asObjectable().getNodeIdentifier();
        String address = node.asObjectable().getHostname() + ":" + node.asObjectable().getJmxPort();
        ClusterStatusInformation.NodeInfo nodeInfo = new ClusterStatusInformation.NodeInfo(node);

        JMXConnector connector = null;

        try {
            MBeanServerConnection mbsc;

            try {
                connector = connectViaJmx(address);
                mbsc = connector.getMBeanServerConnection();
            } catch (IOException e) {
                LoggingUtils.logException(LOGGER, "Cannot connect to the remote node {} at {}", e, nodeName, address);
                nodeInfo.setConnectionError("Cannot connect to the remote node: " + e.getMessage());
                info.addNodeInfo(nodeInfo);
                return;
            }

            try {
                QuartzSchedulerMBean mbeanProxy = getMBeanProxy(nodeName, mbsc);

                boolean running = mbeanProxy.isStarted() && !mbeanProxy.isShutdown() && !mbeanProxy.isStandbyMode();
                LOGGER.trace(" - scheduler running = " + running);
                nodeInfo.setSchedulerRunning(running);

                List<ClusterStatusInformation.TaskInfo> taskInfoList = new ArrayList<ClusterStatusInformation.TaskInfo>();
                TabularData jobs = mbeanProxy.getCurrentlyExecutingJobs();
                for (CompositeData job : (Collection<CompositeData>) jobs.values()) {
                    String oid = (String) job.get("jobName");
                    LOGGER.trace(" - task oid = " + oid);
                    taskInfoList.add(new ClusterStatusInformation.TaskInfo(oid));
                }

                info.addNodeAndTaskInfo(nodeInfo, taskInfoList);

            }
            catch (Exception e) {
                LoggingUtils.logException(LOGGER, "Cannot get information from the remote node {} at {}", e, nodeName, address);
                nodeInfo.setConnectionError("Cannot get information from the remote node: " + e.getMessage());
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
        }
    }

    private QuartzSchedulerMBean getMBeanProxy(String nodeName, MBeanServerConnection mbsc) throws MalformedObjectNameException {
        ObjectName mbeanName = new ObjectName("quartz:type=QuartzScheduler,name=midPointScheduler,instance=" + nodeName);

        return JMX.newMBeanProxy(mbsc, mbeanName, QuartzSchedulerMBean.class, true);
    }

    private JMXConnector connectViaJmx(String address) throws IOException {

        Validate.isTrue(getConfiguration().isClustered(), "This method is applicable in clustered mode only.");

        JMXServiceURL url =
                new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + address + "/jmxrmi");

        Map<String,Object> env = new HashMap<String,Object>();
        String[] creds = {"midpoint", "secret"};
        env.put(JMXConnector.CREDENTIALS, creds);
        return JmxClient.connectWithTimeout(url, env,
                taskManager.getConfiguration().getJmxConnectTimeout(), TimeUnit.SECONDS);
    }

    private TaskManagerConfiguration getConfiguration() {
        return taskManager.getConfiguration();
    }

    private GlobalExecutionManager getGlobalExecutionManager() {
        return taskManager.getGlobalExecutionManager();
    }

    void stopRemoteTask(String oid, ClusterStatusInformation.NodeInfo nodeInfo) {
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
                return;
            }

            try {
                QuartzSchedulerMBean mbeanProxy = getMBeanProxy(nodeName, mbsc);
                mbeanProxy.interruptJob(oid, Scheduler.DEFAULT_GROUP);
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
