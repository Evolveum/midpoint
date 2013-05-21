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

package com.evolveum.midpoint.task.api;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.NodeType;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 * @author Pavol Mederly
 */
public class Node {
    private PrismObject<NodeType> nodeTypePrismObject;

    private String connectionError;
    private NodeExecutionStatus nodeExecutionStatus;
    private NodeErrorStatus nodeErrorStatus;


    public Node(PrismObject<NodeType> node) {
        nodeTypePrismObject = node;
    }

    public PrismObject<NodeType> getNodeType() {
        return nodeTypePrismObject;
    }

    public boolean isSchedulerRunning() {
        return nodeExecutionStatus == NodeExecutionStatus.RUNNING;
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

    public String getHostname() {
        return nodeTypePrismObject.asObjectable().getHostname();
    }

    public String getInternalNodeIdentifier() {
        return nodeTypePrismObject.asObjectable().getInternalNodeIdentifier();
    }

    public Integer getJmxPort() {
        return nodeTypePrismObject.asObjectable().getJmxPort();
    }

    public XMLGregorianCalendar getLastCheckInTime() {
        return nodeTypePrismObject.asObjectable().getLastCheckInTime();
    }

    public String getNodeIdentifier() {
        return nodeTypePrismObject.asObjectable().getNodeIdentifier();
    }

    public Boolean isClustered() {
        return nodeTypePrismObject.asObjectable().isClustered();
    }

    public Boolean isRunning() {
        return nodeTypePrismObject.asObjectable().isRunning();
    }

    public NodeErrorStatus getNodeErrorStatus() {
        return nodeErrorStatus;
    }

    public NodeExecutionStatus getNodeExecutionStatus() {
        return nodeExecutionStatus;
    }

    public void setNodeErrorStatus(NodeErrorStatus nodeErrorStatus) {
        this.nodeErrorStatus = nodeErrorStatus;
    }

    public void setNodeExecutionStatus(NodeExecutionStatus nodeExecutionStatus) {
        this.nodeExecutionStatus = nodeExecutionStatus;
    }

    @Override
    public String toString() {
        return "Node[id=" + getNodeIdentifier() + ", executionStatus=" + getNodeExecutionStatus() + ", errorStatus=" + getNodeErrorStatus() + ", mgmtPort=" + getHostname() + ":" + getJmxPort() + "]";
    }
}
