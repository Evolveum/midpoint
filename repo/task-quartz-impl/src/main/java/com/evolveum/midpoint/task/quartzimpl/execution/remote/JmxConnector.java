/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.task.quartzimpl.execution.remote;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerConfiguration;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerQuartzImpl;
import com.evolveum.midpoint.task.quartzimpl.cluster.ClusterManager;
import com.evolveum.midpoint.task.quartzimpl.cluster.ClusterStatusInformation;
import com.evolveum.midpoint.task.quartzimpl.execution.ExecutionManager;
import com.evolveum.midpoint.task.quartzimpl.execution.JmxClient;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;
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
 * Manages remote nodes using JMX.
 *
 * @author Pavol Mederly
 */
public class JmxConnector {

    private static final Trace LOGGER = TraceManager.getTrace(JmxConnector.class);

    private TaskManagerQuartzImpl taskManager;

    public JmxConnector(TaskManagerQuartzImpl taskManager) {
        this.taskManager = taskManager;
    }

    public void addNodeStatusUsingJmx(ClusterStatusInformation info, NodeType nodeInfo, OperationResult result) {
        String nodeIdentifier = nodeInfo.getNodeIdentifier();
        String address = nodeInfo.getHostname() + ":" + nodeInfo.getJmxPort();
        JMXConnector connector = null;

        try {
            MBeanServerConnection mbsc;

            try {
                connector = connectViaJmx(address);
                mbsc = connector.getMBeanServerConnection();
            } catch (IOException e) {
                LoggingUtils
                        .logUnexpectedException(LOGGER, "Cannot connect to the remote node {} at {}", e, nodeIdentifier, address);
                result.recordWarning("Cannot connect to the remote node " + nodeIdentifier + " at " + address + ": " + e.getMessage(), e);
                nodeInfo.setExecutionStatus(NodeExecutionStatusType.COMMUNICATION_ERROR);
                nodeInfo.setConnectionResult(result.createOperationResultType());
                info.addNodeInfo(nodeInfo);
                return;
            }

            try {

                QuartzSchedulerMBean mbeanProxy = getMBeanProxy(nodeIdentifier, mbsc);

                boolean running = false, down = true;
                if (mbeanProxy != null) {
                    try {
                        running = mbeanProxy.isStarted() && !mbeanProxy.isShutdown() && !mbeanProxy.isStandbyMode();
                        down = mbeanProxy.isShutdown();
                    } catch (Exception e) {     // was: InstanceNotFoundException but it does not seem to work
                        String message = "Cannot get information from scheduler " + nodeIdentifier + " because it does not exist or is shut down.";
                        LoggingUtils.logUnexpectedException(LOGGER, message, e);
                        result.recordWarning(message, e);
                        nodeInfo.setConnectionResult(result.createOperationResultType());
                    }
                } else {
                    result.recordWarning("Cannot get information from node " + nodeIdentifier + " at " + address + " because the JMX object for scheduler cannot be found on that node.");
                    nodeInfo.setConnectionResult(result.createOperationResultType());
                }

                LOGGER.trace(" - scheduler found = " + (mbeanProxy != null) + ", running = " + running + ", shutdown = " + down);

                if (down) {
                    nodeInfo.setExecutionStatus(NodeExecutionStatusType.ERROR);         // this is a mark of error situation (we expect that during ordinary shutdown the node quickly goes down so there is little probability of getting this status on that occasion)
                } else if (running) {
                    nodeInfo.setExecutionStatus(NodeExecutionStatusType.RUNNING);
                } else {
                    nodeInfo.setExecutionStatus(NodeExecutionStatusType.PAUSED);
                }

                List<ClusterStatusInformation.TaskInfo> taskInfoList = new ArrayList<>();
                if (mbeanProxy != null) {
                    TabularData jobs = mbeanProxy.getCurrentlyExecutingJobs();
                    for (CompositeData job : (Collection<CompositeData>) jobs.values()) {
                        String oid = (String) job.get("jobName");
                        LOGGER.trace(" - task oid = " + oid);
                        taskInfoList.add(new ClusterStatusInformation.TaskInfo(oid));
                    }
                }

                if (result.isUnknown()) {
                    result.recordStatus(OperationResultStatus.SUCCESS, "Node " + nodeIdentifier + ": status = " + nodeInfo.getExecutionStatus() + ", # of running tasks: " + taskInfoList.size());
                }
                info.addNodeAndTaskInfo(nodeInfo, taskInfoList);
            } catch (Exception e) {   // unfortunately, mbeanProxy.getCurrentlyExecutingJobs is declared to throw an Exception
                LoggingUtils.logUnexpectedException(LOGGER, "Cannot get information from the remote node {} at {}", e, nodeIdentifier, address);
                result.recordWarning("Cannot get information from the remote node " + nodeIdentifier + " at " + address + ": " + e.getMessage(), e);
                nodeInfo.setExecutionStatus(NodeExecutionStatusType.COMMUNICATION_ERROR);
                nodeInfo.setConnectionResult(result.createOperationResultType());
                info.addNodeInfo(nodeInfo);
            }
        }
        finally {
            try {
                if (connector != null) {
                    connector.close();
                }
            } catch (IOException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Cannot close JMX connection to {}", e, address);
            }
            result.recordSuccessIfUnknown();    // TODO - ok?
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

    public void stopRemoteScheduler(NodeType node, OperationResult result) {

        String nodeName = node.getNodeIdentifier();
        String address = node.getHostname() + ":" + node.getJmxPort();

        JMXConnector connector = null;

        try {
            MBeanServerConnection mbsc;

            try {
                connector = connectViaJmx(address);
                mbsc = connector.getMBeanServerConnection();
            } catch (IOException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Cannot connect to the remote node {} at {}", e, nodeName, address);
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
            }
            catch (Exception e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Cannot put remote scheduler into standby mode; remote node {} at {}", e, nodeName, address);
                result.recordFatalError("Cannot put remote scheduler " + nodeName + " at " + address + " into standby mode: " + e.getMessage());
            }
        }
        finally {
            try {
                if (connector != null) {
                    connector.close();
                }
            } catch (IOException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Cannot close JMX connection to {}", e, address);
            }
        }

    }

    public void startRemoteScheduler(NodeType node, OperationResult result) {

        String nodeName = node.getNodeIdentifier();
        String address = node.getHostname() + ":" + node.getJmxPort();

        JMXConnector connector = null;

        try {
            MBeanServerConnection mbsc;

            try {
                connector = connectViaJmx(address);
                mbsc = connector.getMBeanServerConnection();
            } catch (IOException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Cannot connect to the remote node {} at {}", e, nodeName, address);
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
            }
            catch (Exception e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Cannot start remote scheduler; remote node {} at {}", e, nodeName, address);
                result.recordFatalError("Cannot start remote scheduler " + nodeName + " at " + address + ": " + e.getMessage());
            }

        }
        finally {
            try {
                if (connector != null) {
                    connector.close();
                }
            } catch (IOException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Cannot close JMX connection to {}", e, address);
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
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot communicate with remote node via JMX", e);
            return null;
        }
    }

    private JMXConnector connectViaJmx(String address) throws IOException {

        JMXServiceURL url =
                new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + address + "/jmxrmi");

        Map<String,Object> env = new HashMap<>();
        String jmxUsername = taskManager.getConfiguration().getJmxUsername();
        String jmxPassword = taskManager.getConfiguration().getJmxPassword();
        if (jmxUsername != null || jmxPassword != null) {
            String[] creds = { jmxUsername, jmxPassword };
            env.put(JMXConnector.CREDENTIALS, creds);
        }
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
    public void stopRemoteTaskRun(String oid, NodeType node, OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(JmxConnector.class.getName() + ".stopRemoteTaskRun");
        result.addParam("oid", oid);
        result.addParam("node", node.toString());

        LOGGER.debug("Interrupting task " + oid + " running at " + getClusterManager().dumpNodeInfo(node));

        String nodeName = node.getNodeIdentifier();
        String address = node.getHostname() + ":" + node.getJmxPort();

        Holder<JMXConnector> connectorHolder = new Holder<>();

        try {
            QuartzSchedulerMBean mbeanProxy = getSchedulerBean(node, connectorHolder, result);
            if (mbeanProxy != null) {
                try {
                    mbeanProxy.interruptJob(oid, Scheduler.DEFAULT_GROUP);
                    LOGGER.debug("Successfully signalled shutdown to task " + oid + " running at " + getClusterManager().dumpNodeInfo(node));
                    result.recordSuccessIfUnknown();
                } catch (Exception e) {   // necessary because of mbeanProxy
                    String message = "Cannot signal task "+oid+" interruption to remote node "+nodeName+" at "+address;
                    LoggingUtils.logUnexpectedException(LOGGER, message, e);
                    result.recordFatalError(message + ":" + e.getMessage(), e);
                }
            }
        } finally {
            closeJmxConnection(connectorHolder, address);
        }
    }

    private void closeJmxConnection(Holder<JMXConnector> connectorHolder, String nodeInfo) {
        try {
            if (!connectorHolder.isEmpty()) {
                connectorHolder.getValue().close();
            }
        } catch (IOException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot close JMX connection to {}", e, nodeInfo);
        }
    }

    private QuartzSchedulerMBean getSchedulerBean(NodeType node, Holder<JMXConnector> connectorHolder,
            OperationResult result) {
        String nodeName = node.getNodeIdentifier();
        String address = node.getHostname() + ":" + node.getJmxPort();
        try {
            JMXConnector connector = connectViaJmx(address);
            connectorHolder.setValue(connector);
            MBeanServerConnection serverConnection = connector.getMBeanServerConnection();
            QuartzSchedulerMBean bean = getMBeanProxy(nodeName, serverConnection);
            if (bean == null) {
                String message = "Cannot connect to the Quartz Scheduler bean at remote node " + nodeName + " at "
                        + address + " because the JMX object for scheduler cannot be found on that node.";
                LOGGER.warn("{}", message);
                result.recordFatalError(message);
            }
            return bean;
        } catch (IOException|MalformedObjectNameException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot connect to the quartz scheduler bean at remote node {} at {}", e, nodeName, address);
            result.recordFatalError("Cannot connect to the quartz scheduler bean at remote node " + nodeName + " at " + address + ": " + e.getMessage(), e);
            return null;
        }
    }

    private ClusterManager getClusterManager() {
        return taskManager.getClusterManager();
    }

}
