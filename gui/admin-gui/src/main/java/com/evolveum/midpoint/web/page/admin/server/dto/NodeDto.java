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

package com.evolveum.midpoint.web.page.admin.server.dto;

import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.task.api.Node;
import com.evolveum.midpoint.task.api.NodeErrorStatus;
import com.evolveum.midpoint.task.api.NodeExecutionStatus;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.NodeType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;

/**
 * @author lazyman
 */
public class NodeDto extends Selectable {

    private String oid;
    private String name;
    private String nodeIdentifier;
    private String managementPort;
    private Long lastCheckInTime;
    private boolean clustered;

    private NodeExecutionStatus executionStatus;
    private NodeErrorStatus errorStatus;

    private String statusMessage;

    public NodeDto(Node node) {
        Validate.notNull(node, "Node must not be null.");

        PrismObject<NodeType> prismNode = node.getNodeType();
        oid = prismNode.getOid();
        //name = prismNode.getPropertyRealValue(ObjectType.F_NAME, String.class);
        name = prismNode.asObjectable().getName().getOrig();

        XMLGregorianCalendar calendar = node.getLastCheckInTime();
        if (calendar != null) {
            lastCheckInTime = MiscUtil.asDate(calendar).getTime();
        }

        nodeIdentifier = node.getNodeIdentifier();
        clustered = node.isClustered();
        managementPort = node.getHostname() + ":" + node.getJmxPort();

        executionStatus = node.getNodeExecutionStatus();
        errorStatus = node.getNodeErrorStatus();

        if (StringUtils.isNotEmpty(node.getConnectionError())) {
            statusMessage = node.getConnectionError();
        } else if (errorStatus != null && errorStatus != NodeErrorStatus.OK) {
            statusMessage = errorStatus.toString();         // TODO: explain and localize this
        } else if (executionStatus == NodeExecutionStatus.ERROR) {      // error status not specified
            statusMessage = "Unspecified error (or the node is just starting or shutting down)";
        } else {
            statusMessage = "";
        }
    }

    public boolean isClustered() {
        return clustered;
    }

    public Long getLastCheckInTime() {
        return lastCheckInTime;
    }

    public String getName() {
        return name;
    }

    public String getNodeIdentifier() {
        return nodeIdentifier;
    }

    public String getOid() {
        return oid;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public NodeErrorStatus getErrorStatus() {
        return errorStatus;
    }

    public NodeExecutionStatus getExecutionStatus() {
        return executionStatus;
    }

    public String getManagementPort() {
        return managementPort;
    }
}
