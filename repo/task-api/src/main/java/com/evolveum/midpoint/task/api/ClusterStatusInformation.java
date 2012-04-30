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

    private Map<Node,List<TaskInfo>> tasks = new HashMap<Node,List<TaskInfo>>();

    public Set<TaskInfo> getTasks() {
        Set<TaskInfo> retval = new HashSet<TaskInfo>();
        for (List<TaskInfo> tasksOnNode : tasks.values()) {
            retval.addAll(tasksOnNode);
        }
        return retval;
    }

    public Map<Node, List<TaskInfo>> getTasksOnNodes() {
        return tasks;
    }

    public List<TaskInfo> getTasksOnNode(Node node) {
        return tasks.get(node);
    }



    // assumes the task is executing at one node only
    public Node findNodeInfoForTask(String oid) {
        for (Map.Entry<Node,List<TaskInfo>> entry : tasks.entrySet()) {
            for (TaskInfo ti : entry.getValue()) {
                if (oid.equals(ti.getOid())) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    public Set<Node> getNodes() {
        return tasks.keySet();
    }

    public void addNodeInfo(Node node) {
        tasks.put(node, new ArrayList<TaskInfo>());       // TODO: or null? this is safer...
    }

    public void addNodeAndTaskInfo(Node node, List<TaskInfo> taskInfoList) {
        tasks.put(node, taskInfoList);
    }

    public Node findNodeById(String nodeIdentifier) {
        for (Node node : tasks.keySet()) {
            if (node.getNodeIdentifier().equals(nodeIdentifier)) {
                return node;
            }
        }
        return null;
    }


}
