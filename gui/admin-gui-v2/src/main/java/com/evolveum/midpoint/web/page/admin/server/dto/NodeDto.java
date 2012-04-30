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

package com.evolveum.midpoint.web.page.admin.server.dto;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.task.api.Node;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.web.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.NodeType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import org.apache.commons.lang.Validate;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 * @author lazyman
 */
public class NodeDto extends Selectable {

    private String oid;
    private String name;
    private String nodeIdentifier;
    private String hostname;
    private Long lastCheckInTime;
    private boolean clustered;
    private boolean running;
    private String statusMessage;

    public NodeDto(Node node) {
        Validate.notNull(node, "Node must not be null.");
        //todo status message

        PrismObject<NodeType> prismNode = node.getNodeType();
        oid = prismNode.getOid();
        name = prismNode.getPropertyRealValue(ObjectType.F_NAME, String.class);

        XMLGregorianCalendar calendar = node.getLastCheckInTime();
        if (calendar != null) {
            lastCheckInTime = MiscUtil.asDate(calendar).getTime();
        }

        nodeIdentifier = node.getNodeIdentifier();
        running = node.isRunning();
        clustered = node.isClustered();
        hostname = node.getHostname() + ":" + node.getJmxPort();
    }

    public boolean isClustered() {
        return clustered;
    }

    public String getHostname() {
        return hostname;
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

    public boolean isRunning() {
        return running;
    }

    public String getStatusMessage() {
        return statusMessage;
    }
}
