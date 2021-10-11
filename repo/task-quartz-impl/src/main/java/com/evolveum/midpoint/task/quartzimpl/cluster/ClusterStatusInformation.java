/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl.cluster;

import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchedulerInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import java.io.Serializable;
import java.util.*;

/**
 * Provides information about tasks currently executing at particular nodes in the cluster.
 *
 * @author Pavol Mederly
 */
public class ClusterStatusInformation implements Serializable {

    private static final long serialVersionUID = -2955916510215061664L;

    private long timestamp = System.currentTimeMillis();

    public boolean isFresh(long allowance) {
        return System.currentTimeMillis() - timestamp <= allowance;
    }

    public static class TaskInfo implements Serializable {

        private static final long serialVersionUID = -6863271365758398279L;

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

        @Override
        public String toString() {
            return oid;
        }
    }

    private Map<NodeType,List<TaskInfo>> tasks = new HashMap<>();

    public Set<TaskInfo> getTasks() {
        Set<TaskInfo> retval = new HashSet<>();
        for (List<TaskInfo> tasksOnNode : tasks.values()) {
            retval.addAll(tasksOnNode);
        }
        return retval;
    }

    public Map<NodeType, List<TaskInfo>> getTasksOnNodes() {
        return tasks;
    }

    public Set<TaskInfo> getTasksOnNodes(Collection<String> nodeIdList) {
        Set<TaskInfo> retval = new HashSet<>();
        for (String nodeId : nodeIdList) {
            retval.addAll(getTasksOnNode(nodeId));
        }
        return retval;
    }


    public List<TaskInfo> getTasksOnNode(NodeType node) {
        return tasks.get(node);
    }

    public List<TaskInfo> getTasksOnNode(String nodeId) {
        return getTasksOnNode(findNodeById(nodeId));
    }

    // assumes the task is executing at one node only
    public NodeType findNodeInfoForTask(String oid) {
        for (Map.Entry<NodeType,List<TaskInfo>> entry : tasks.entrySet()) {
            for (TaskInfo ti : entry.getValue()) {
                if (oid.equals(ti.getOid())) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    public Set<NodeType> getNodes() {
        return tasks.keySet();
    }

    public void addNodeInfo(NodeType node) {
        tasks.put(node, new ArrayList<>());       // TODO: or null? this is safer...
    }

    public void addNodeAndTaskInfo(NodeType node, List<TaskInfo> taskInfoList) {
        tasks.put(node, taskInfoList);
    }

    public void addNodeAndTaskInfo(SchedulerInformationType info) {
        tasks.put(info.getNode(), getTaskInfoList(info));
    }

    private List<TaskInfo> getTaskInfoList(SchedulerInformationType info) {
        List<TaskInfo> rv = new ArrayList<>();
        for (TaskType taskBean : info.getExecutingTask()) {
            rv.add(new TaskInfo(taskBean.getOid()));
        }
        return rv;
    }

    public NodeType findNodeById(String nodeIdentifier) {
        for (NodeType node : tasks.keySet()) {
            if (node.getNodeIdentifier().equals(nodeIdentifier)) {
                return node;
            }
        }
        return null;
    }

    public String dump() {
        StringBuffer retval = new StringBuffer();
        for (Map.Entry<NodeType,List<TaskInfo>> nodeListEntry : tasks.entrySet()) {
            retval.append(nodeListEntry.getKey().toString());
            retval.append(": ");
            retval.append(nodeListEntry.getValue().toString());
        }
        return retval.toString();
    }
}
