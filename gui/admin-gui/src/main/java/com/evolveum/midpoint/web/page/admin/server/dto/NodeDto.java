/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.server.dto;

import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.web.component.data.column.InlineMenuable;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeErrorStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import javax.xml.datatype.XMLGregorianCalendar;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class NodeDto extends Selectable implements InlineMenuable {

    public static final String F_CONTACT = "contact";
    public static final String F_NAME = "name";
    public static final String F_EXECUTION_STATUS = "executionStatus";
    public static final String F_CLUSTERED = "clustered";
    public static final String F_STATUS_MESSAGE = "statusMessage";
    private String oid;
    private String name;
    private String nodeIdentifier;
    private String contact;
    private Long lastCheckInTime;
    private boolean clustered;

    private NodeExecutionStatusType executionStatus;
    private NodeErrorStatusType errorStatus;

    private String statusMessage;

    private List<InlineMenuItem> menuItems;

    public NodeDto(NodeType node) {
        Validate.notNull(node, "Node must not be null.");

        oid = node.getOid();
        name = node.getName().getOrig();

        XMLGregorianCalendar calendar = node.getLastCheckInTime();
        if (calendar != null) {
            lastCheckInTime = MiscUtil.asDate(calendar).getTime();
        }

        nodeIdentifier = node.getNodeIdentifier();
        clustered = node.isClustered();
        contact = node.getUrl() != null ? node.getUrl() : node.getHostname();

        executionStatus = node.getExecutionStatus();
        errorStatus = node.getErrorStatus();

        if (node.getConnectionResult() != null && node.getConnectionResult().getStatus() != OperationResultStatusType.SUCCESS &&
                StringUtils.isNotEmpty(node.getConnectionResult().getMessage())) {
            statusMessage = node.getConnectionResult().getMessage();
        } else if (errorStatus != null && errorStatus != NodeErrorStatusType.OK) {
            statusMessage = errorStatus.toString();         // TODO: explain and localize this
        } else if (executionStatus == NodeExecutionStatusType.ERROR) {      // error status not specified
            statusMessage = "Unspecified error (or the node is just starting or shutting down)";
        } else {
            statusMessage = "";
        }
    }

    @Override
    public List<InlineMenuItem> getMenuItems() {
        if (menuItems == null) {
            menuItems = new ArrayList<>();
        }
        return menuItems;
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

    public NodeErrorStatusType getErrorStatus() {
        return errorStatus;
    }

    public NodeExecutionStatusType getExecutionStatus() {
        return executionStatus;
    }

    public String getContact() {
        return contact;
    }

    public static List<String> getNodeIdentifiers(List<NodeDto> nodeDtoList) {
        List<String> nodeList = new ArrayList<>();
        for (NodeDto nodeDto : nodeDtoList) {
            nodeList.add(nodeDto.getNodeIdentifier());
        }
        return nodeList;
    }

}
