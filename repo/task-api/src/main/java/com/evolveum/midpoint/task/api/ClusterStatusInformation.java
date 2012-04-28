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
 * Portions Copyrighted 2011 [name of copyright owner]
 */

package com.evolveum.midpoint.task.api;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_1.NodeType;

import java.util.*;

/**
 * Provides information about tasks currently executing at particular nodes in the cluster.
 *
 * @author Pavol Mederly
 */
public class ClusterStatusInformation {

    public static class TaskInfo {
        private String oid;

        public TaskInfo(String taskOid) {
            oid = taskOid;
        }

        public String getOid() {
            return oid;
        }

        public void setOid(String oid) {
            this.oid = oid;
        }
    }

    public static class NodeInfo {
        private PrismObject<NodeType> nodeType;
        private boolean schedulerRunning;
        private String connectionError;

        public NodeInfo(PrismObject<NodeType> node) {
            nodeType = node;
        }

        public PrismObject<NodeType> getNodeType() {
            return nodeType;
        }

        public void setNodeType(PrismObject<NodeType> nodeType) {
            this.nodeType = nodeType;
        }

        public boolean isSchedulerRunning() {
            return schedulerRunning;
        }

        public void setSchedulerRunning(boolean schedulerRunning) {
            this.schedulerRunning = schedulerRunning;
        }

        public boolean isConnectionError() {
            return connectionError != null;
        }

        public String getConnectionError() {
            return connectionError;
        }

        public void setConnectionError(String connectionError) {
            this.connectionError = connectionError;
        }
    }

    private Map<NodeInfo,List<TaskInfo>> tasks = new HashMap<NodeInfo,List<TaskInfo>>();

    public Set<TaskInfo> getTasks() {
        Set<TaskInfo> retval = new HashSet<TaskInfo>();
        for (List<TaskInfo> tasksOnNode : tasks.values()) {
            retval.addAll(tasksOnNode);
        }
        return retval;
    }

    public Map<NodeInfo, List<TaskInfo>> getTasksOnNodes() {
        return tasks;
    }

    public List<TaskInfo> getTasksOnNode(NodeInfo nodeInfo) {
        return tasks.get(nodeInfo);
    }



    // assumes the task is executing at one node only
    public NodeInfo findNodeInfoForTask(String oid) {
        for (Map.Entry<NodeInfo,List<TaskInfo>> entry : tasks.entrySet()) {
            for (TaskInfo ti : entry.getValue()) {
                if (oid.equals(ti.getOid())) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    public Set<NodeInfo> getNodes() {
        return tasks.keySet();
    }

    public void addNodeInfo(NodeInfo nodeInfo) {
        tasks.put(nodeInfo, new ArrayList<TaskInfo>());       // TODO: or null? this is safer...
    }

    public void addNodeAndTaskInfo(NodeInfo nodeInfo, List<TaskInfo> taskInfoList) {
        tasks.put(nodeInfo, taskInfoList);
    }



}
