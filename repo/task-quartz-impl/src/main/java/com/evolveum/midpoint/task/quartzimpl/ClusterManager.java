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
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTasksInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.NodeType;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;
import org.apache.commons.lang.Validate;
import org.quartz.Scheduler;
import org.quartz.core.jmx.QuartzSchedulerMBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.*;

/**
 * Copyright (c) 2011 Evolveum
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
@Component
public class ClusterManager {

    private static final transient Trace LOGGER = TraceManager.getTrace(ClusterManager.class);

    @Autowired(required=true)
    private RepositoryService repositoryService;

    @Autowired(required=true)
    private PrismContext prismContext;

    @Autowired(required=true)
    private TaskManagerQuartzImpl taskManager;


    PrismObject<NodeType> createNodePrism(String instanceId, int port) {

        PrismObjectDefinition<NodeType> nodeTypeDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(NodeType.class);
        PrismObject<NodeType> nodePrism = nodeTypeDef.instantiate();

        NodeType node = nodePrism.asObjectable();

        node.setNodeIdentifier(instanceId);
        node.setName(instanceId);
        node.setHostname(getMyAddress() + ":" + port);

        return nodePrism;
    }

    void registerNode(PrismObject<NodeType> nodePrism, OperationResult result) {

        NodeType node = nodePrism.asObjectable();

        LOGGER.info("Registering this node in the repository as " + node.getNodeIdentifier() + " at " + node.getHostname());

        QueryType q;
        try {
            q = QueryUtil.createNameQuery(node);
        } catch (SchemaException e) {
            throw new SystemException("Cannot register this node, because the query for NodeType name cannot be created.", e);
        }

        List<PrismObject<NodeType>> nodes;
        try {
            nodes = repositoryService.searchObjects(NodeType.class, q, new PagingType(), result);
        } catch (SchemaException e) {
            throw new SystemException("Cannot register this node, because the query for NodeType name cannot be executed.", e);
        }

        for (PrismObject<NodeType> n : nodes) {
            LOGGER.trace("Removing existing NodeType with oid = {}, name = {}", n.getOid(), n.getName());
            try {
                repositoryService.deleteObject(NodeType.class, n.getOid(), result);
            } catch (ObjectNotFoundException e) {
                LoggingUtils.logException(LOGGER, "Cannot remove NodeType with oid = {}, name = {}, because it does not exist.", e, n.getOid(), n.getName());
            }
        }

        try {
            repositoryService.addObject(nodePrism, result);
        } catch (ObjectAlreadyExistsException e) {
            throw new SystemException("Cannot register this node, because it already exists (this should not happen, as nodes with such a name were just removed)", e);
        } catch (SchemaException e) {
            throw new SystemException("Cannot register this node, because of schema exception", e);
        }

        LOGGER.trace("Node was successfully registered in the repository.");
    }

    void unregisterNode(OperationResult result) {

        String oid = taskManager.getNodePrism().getOid();
        String name = taskManager.getNodePrism().asObjectable().getNodeIdentifier();

        LOGGER.trace("Unregistering this node from the repository (name {}, oid {})", name, oid);
        try {
            repositoryService.deleteObject(NodeType.class, oid, result);
            LOGGER.trace("Node successfully unregistered.");
        } catch (ObjectNotFoundException e) {
            LoggingUtils.logException(LOGGER, "Cannot unregister this node (name {}, oid {}), because it does not exist.", e,
                    name, oid);
        }

    }


    private String getMyAddress() {
        try {
            InetAddress addr = InetAddress.getLocalHost();
            return addr.getHostAddress();
        } catch (UnknownHostException e) {
            LoggingUtils.logException(LOGGER, "Cannot get local IP address", e);
            return "unknown-host";
        }
    }


    List<PrismObject<NodeType>> getAllNodes(OperationResult result) {
        try {
            return repositoryService.searchObjects(NodeType.class, QueryUtil.createAllObjectsQuery(), new PagingType(), result);
        } catch (SchemaException e) {       // should not occur
            throw new SystemException("Cannot get the list of nodes from the repository", e);
        }
    }


    public RunningTasksInfo getRunningTasks() {
        return getRunningTasks(true);
    }

    public RunningTasksInfo getRunningTasks(boolean clusterwide) {

        OperationResult result = createOperationResult("getRunningTasks");

        RunningTasksInfo retval = new RunningTasksInfo();

        if (taskManager.isClustered() && clusterwide) {
            for (PrismObject<NodeType> node : getAllNodes(result)) {
                addNodeAndTaskInfo(retval, node);
            }
        } else {
            addNodeAndTaskInfo(retval, taskManager.getNodePrism());
        }
        return retval;
    }

    private void addNodeAndTaskInfo(RunningTasksInfo info, PrismObject<NodeType> node) {

        if (taskManager.isCurrentNode(node)) {

            LOGGER.trace("Getting node and task info from the current node ({})", node.asObjectable().getNodeIdentifier());

            List<RunningTasksInfo.TaskInfo> taskInfoList = new ArrayList<RunningTasksInfo.TaskInfo>();
            Set<Task> tasks = taskManager.getRunningTasks();
            for (Task task : tasks) {
                taskInfoList.add(new RunningTasksInfo.TaskInfo(task.getOid()));
            }
            RunningTasksInfo.NodeInfo nodeInfo = new RunningTasksInfo.NodeInfo(node);
            nodeInfo.setSchedulerRunning(taskManager.getServiceThreadsActivationState());

            info.addNodeAndTaskInfo(nodeInfo, taskInfoList);

        } else {    // if remote (cannot occur if !isClustered)

            LOGGER.debug("Getting running task info from remote node ({}, {})", node.asObjectable().getNodeIdentifier(), node.asObjectable().getHostname());
            addNodeAndTaskInfoFromRemoteNode(info, node);
        }
    }

    private void addNodeAndTaskInfoFromRemoteNode(RunningTasksInfo info, PrismObject<NodeType> node) {

        Validate.isTrue(taskManager.isClustered(), "This method is applicable in clustered mode only.");

        String nodeName = node.asObjectable().getNodeIdentifier();
        String address = node.asObjectable().getHostname();
        RunningTasksInfo.NodeInfo nodeInfo = new RunningTasksInfo.NodeInfo(node);

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

                List<RunningTasksInfo.TaskInfo> taskInfoList = new ArrayList<RunningTasksInfo.TaskInfo>();
                TabularData jobs = mbeanProxy.getCurrentlyExecutingJobs();
                for (CompositeData job : (Collection<CompositeData>) jobs.values()) {
                    String oid = (String) job.get("jobName");
                    LOGGER.trace(" - task oid = " + oid);
                    taskInfoList.add(new RunningTasksInfo.TaskInfo(oid));
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

        Validate.isTrue(taskManager.isClustered(), "This method is applicable in clustered mode only.");

        JMXServiceURL url =
            new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + address + "/jmxrmi");

        Map<String,Object> env = new HashMap<String,Object>();
        String[] creds = {"midpoint", "secret"};
        env.put(JMXConnector.CREDENTIALS, creds);
        return JMXConnectorFactory.connect(url, env);
    }

    public void interruptTaskClusterwide(String oid, RunningTasksInfo.NodeInfo nodeInfo) {
        LOGGER.debug("Interrupting task " + oid + " running at " + dumpNodeInfo(nodeInfo));

        if (taskManager.isCurrentNode(nodeInfo.getNodeType())) {
            taskManager.signalShutdownToTaskLocally(oid);
            return;
        }

        if (!taskManager.isClustered()) {       // here we should not come
            LOGGER.warn("Remote task interruption is applicable in clustered mode only; doing nothing.");
            return;
        }

        String nodeName = nodeInfo.getNodeType().asObjectable().getNodeIdentifier();
        String address = nodeInfo.getNodeType().asObjectable().getHostname();

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
                LOGGER.debug("Successfully signalled shutdown to task " + oid + " running at " + dumpNodeInfo(nodeInfo));
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

    private String dumpNodeInfo(RunningTasksInfo.NodeInfo nodeInfo) {
        NodeType node = nodeInfo.getNodeType().asObjectable();
        return node.getNodeIdentifier() + " (" + node.getHostname() + ")";
    }

    private OperationResult createOperationResult(String methodName) {
        return new OperationResult(ClusterManager.class.getName() + "." + methodName);
    }



}

