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

import com.evolveum.midpoint.repo.sql.DtoTranslationException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorType;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;

import javax.persistence.*;
import java.util.List;

/**
 * @author lazyman
 */
@Entity
@Table(name = "connector")
public class RConnectorType extends RExtensibleObjectType {

    private String framework;
    private String connectorType;
    private String connectorVersion;
    private String connectorBundle;
    private List<String> targetSystemType;
    private String namespace;
    private RConnectorHostType connectorHost;
//    private ObjectReferenceType connectorHostRef;     //todo mapping
//    private XmlSchemaType schema;         //todo mapping

    public String getConnectorBundle() {
        return connectorBundle;
    }

    @ManyToOne
    @Cascade({CascadeType.SAVE_UPDATE}) //todo cascade check
    public RConnectorHostType getConnectorHost() {
        return connectorHost;
    }

    public String getConnectorType() {
        return connectorType;
    }

    public String getConnectorVersion() {
        return connectorVersion;
    }

    public String getFramework() {
        return framework;
    }

    public String getNamespace() {
        return namespace;
    }

    @ElementCollection//(fetch = FetchType.EAGER)
    @CollectionTable(name = "connector_target_system", joinColumns =
            {@JoinColumn(name = "connectorId")})
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public List<String> getTargetSystemType() {
        return targetSystemType;
    }

    public void setConnectorBundle(String connectorBundle) {
        this.connectorBundle = connectorBundle;
    }

    public void setConnectorHost(RConnectorHostType connectorHost) {
        this.connectorHost = connectorHost;
    }

    public void setConnectorType(String connectorType) {
        this.connectorType = connectorType;
    }

    public void setConnectorVersion(String connectorVersion) {
        this.connectorVersion = connectorVersion;
    }

    public void setFramework(String framework) {
        this.framework = framework;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public void setTargetSystemType(List<String> targetSystemType) {
        this.targetSystemType = targetSystemType;
    }

    public static void copyToJAXB(RConnectorType repo, ConnectorType jaxb) throws DtoTranslationException {
        RExtensibleObjectType.copyToJAXB(repo, jaxb);

        //todo implement
    }

    public static void copyFromJAXB(ConnectorType jaxb, RConnectorType repo) throws DtoTranslationException {
        RExtensibleObjectType.copyFromJAXB(jaxb, repo);

        //todo implement
    }

    @Override
    public ConnectorType toJAXB() throws DtoTranslationException {
        ConnectorType object = new ConnectorType();
        RConnectorType.copyToJAXB(this, object);
        return object;
    }
}
