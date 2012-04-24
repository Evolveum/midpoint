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

package com.evolveum.midpoint.repo.sql.data.common;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.DtoTranslationException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.NodeType;
import org.hibernate.annotations.ForeignKey;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author lazyman
 */
@Entity
@Table(name = "node")
@ForeignKey(name = "fk_node")
public class RNode extends RObject {

    private String nodeIdentifier;
    private String hostname;

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getNodeIdentifier() {
        return nodeIdentifier;
    }

    public void setNodeIdentifier(String nodeIdentifier) {
        this.nodeIdentifier = nodeIdentifier;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        RNode rNode = (RNode) o;

        if (hostname != null ? !hostname.equals(rNode.hostname) : rNode.hostname != null) return false;
        if (nodeIdentifier != null ? !nodeIdentifier.equals(rNode.nodeIdentifier) : rNode.nodeIdentifier != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (nodeIdentifier != null ? nodeIdentifier.hashCode() : 0);
        result = 31 * result + (hostname != null ? hostname.hashCode() : 0);
        return result;
    }

    public static void copyToJAXB(RNode repo, NodeType jaxb, PrismContext prismContext) throws
            DtoTranslationException {
        RObject.copyToJAXB(repo, jaxb, prismContext);

        jaxb.setHostname(repo.getHostname());
        jaxb.setNodeIdentifier(repo.getNodeIdentifier());
    }

    public static void copyFromJAXB(NodeType jaxb, RNode repo, PrismContext prismContext) throws
            DtoTranslationException {
        RObject.copyFromJAXB(jaxb, repo, prismContext);

        repo.setHostname(jaxb.getHostname());
        repo.setNodeIdentifier(jaxb.getNodeIdentifier());
    }

    @Override
    public NodeType toJAXB(PrismContext prismContext) throws DtoTranslationException {
        NodeType object = new NodeType();
        RUtil.revive(object, prismContext);
        RNode.copyToJAXB(this, object, prismContext);

        return object;
    }
}
