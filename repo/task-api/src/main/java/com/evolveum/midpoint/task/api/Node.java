/*
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
