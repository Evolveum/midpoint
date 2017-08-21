/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.task.quartzimpl.execution;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.quartzimpl.cluster.ClusterStatusInformation;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerConfiguration;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerQuartzImpl;
import com.evolveum.midpoint.task.quartzimpl.cluster.ClusterManager;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;

import org.jetbrains.annotations.NotNull;
import org.quartz.*;
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

    public static final JobKey STARTER_JOB_KEY = JobKey.jobKey("STARTER JOB");

    private TaskManagerQuartzImpl taskManager;

    public RemoteNodesManager(TaskManagerQuartzImpl taskManager) {
        this.taskManager = taskManager;
    }

    /**
     * Used exclusively for collecting running task information.
     *
     * @param info A structure to which information should be added
     * @param node Node which to query
     */
    void addNodeStatusFromRemoteNode(ClusterStatusInformation info, PrismObject<NodeType> node, OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(RemoteNodesManager.class.getName() + ".addNodeStatusFromRemoteNode");
        result.addParam("node", node);

        NodeType nodeInfo = node.asObjectable();
        String nodeIdentifier = nodeInfo.getNodeIdentifier();
        String address = nodeInfo.getHostname() + ":" + nodeInfo.getJmxPort();

        if (!taskManager.getClusterManager().isUp(nodeInfo)) {
            nodeInfo.setExecutionStatus(NodeExecutionStatusType.DOWN);
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
                LoggingUtils.logUnexpectedException(LOGGER, "Cannot connect to the remote node {} at {}", e, nodeIdentifier, address);
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
                    result.recordStatus(OperationResultStatus.SUCCESS, "Node " + nodeIdentifier + ": status = " + nodeInfo.getExecutionStatus() + ", # of running tasks: " + taskInfoList.size());
                }
                info.addNodeAndTaskInfo(nodeInfo, taskInfoList);
            } catch (Exception e) {   // unfortunately, mbeanProxy.getCurrentlyExecutingJobs is declared to throw an Exception
                LoggingUtils.logUnexpectedException(LOGGER, "Cannot get information from the remote node {} at {}", e, nodeIdentifier, address);
                result.recordWarning("Cannot get information from the remote node " + nodeIdentifier + " at " + address + ": " + e.getMessage(), e);
                nodeInfo.setExecutionStatus(NodeExecutionStatusType.COMMUNICATION_ERROR);
                nodeInfo.setConnectionResult(result.createOperationResultType());
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
                LoggingUtils.logUnexpectedException(LOGGER, "Cannot close JMX connection to {}", e, address);
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
                return;
            }
            catch (Exception e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Cannot put remote scheduler into standby mode; remote node {} at {}", e, nodeName, address);
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
                LoggingUtils.logUnexpectedException(LOGGER, "Cannot close JMX connection to {}", e, address);
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
                return;
            }
            catch (Exception e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Cannot start remote scheduler; remote node {} at {}", e, nodeName, address);
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

        Map<String,Object> env = new HashMap<String,Object>();
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
    void stopRemoteTaskRun(String oid, NodeType node, OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(RemoteNodesManager.class.getName() + ".stopRemoteTaskRun");
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

	public void redirectTaskToNode(@NotNull Task task, @NotNull NodeType node, @NotNull OperationResult result) {
    	LOGGER.trace("Trying to schedule task {} on {}", task, node.getNodeIdentifier());
		Holder<JMXConnector> connectorHolder = new Holder<>();
		try {
			QuartzSchedulerMBean mbeanProxy = getSchedulerBean(node, connectorHolder, result);
			if (mbeanProxy != null) {
				try {
					createStarterJobIfNeeded();
					mbeanProxy.triggerJob(STARTER_JOB_KEY.getName(), STARTER_JOB_KEY.getGroup(),
							Collections.singletonMap(JobStarter.TASK_OID, task.getOid()));
					LOGGER.debug("Successfully requested start of " + task + " at " + getClusterManager().dumpNodeInfo(node));
					result.recordSuccessIfUnknown();
				} catch (Exception e) {   // necessary because of mbeanProxy
					String message = "Cannot schedule " + task + " at " + getClusterManager().dumpNodeInfo(node);
					LoggingUtils.logUnexpectedException(LOGGER, message, e);
					result.recordFatalError(message + ":" + e.getMessage(), e);
				}
			} else {
				LOGGER.warn("Couldn't obtain Quartz MBean so couldn't reschedule task {} on {}", task, node.getNodeIdentifier());
			}
		} finally {
			closeJmxConnection(connectorHolder, getClusterManager().dumpNodeInfo(node));
		}
    }

	private void createStarterJobIfNeeded() {
		Scheduler scheduler = taskManager.getExecutionManager().getQuartzScheduler();
		try {
			if (!scheduler.checkExists(STARTER_JOB_KEY)) {
				JobDetail starterJob = JobBuilder.newJob(JobStarter.class)
						.withIdentity(STARTER_JOB_KEY)
						.storeDurably()
						.build();
				scheduler.addJob(starterJob, true);
			}
		} catch (SchedulerException e) {
			throw new SystemException("Starter job couldn't be created", e);
		}
	}

}
