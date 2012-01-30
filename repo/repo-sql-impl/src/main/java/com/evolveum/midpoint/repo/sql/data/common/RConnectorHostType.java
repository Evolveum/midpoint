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
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorHostType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author lazyman
 */
@Entity
@Table(name = "connector_host")
public class RConnectorHostType extends RExtensibleObjectType {

    private String hostname;
    private String port;
    //    private ProtectedStringType sharedSecret; //todo what with this?
    private boolean protectConnection;
    private Integer timeout; //todo default timeout value???

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public boolean isProtectConnection() {
        return protectConnection;
    }

    public void setProtectConnection(boolean protectConnection) {
        this.protectConnection = protectConnection;
    }

    @Column(nullable = true)
    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public static void copyToJAXB(RConnectorHostType repo, ConnectorHostType jaxb) throws DtoTranslationException {
        RExtensibleObjectType.copyToJAXB(repo, jaxb);

        jaxb.setHostname(repo.getHostname());
        jaxb.setPort(repo.getPort());
        jaxb.setProtectConnection(repo.isProtectConnection());
//        jaxb.setSharedSecret(); //todo implement
        jaxb.setTimeout(repo.getTimeout());
    }

    public static void copyFromJAXB(ConnectorHostType jaxb, RConnectorHostType repo) throws DtoTranslationException {
        RExtensibleObjectType.copyFromJAXB(jaxb, repo);

        repo.setHostname(jaxb.getHostname());
        repo.setPort(jaxb.getPort());
        repo.setTimeout(jaxb.getTimeout());

//        jaxb.getSharedSecret() //todo implement

        boolean protectConnection = jaxb.isProtectConnection() != null ? jaxb.isProtectConnection() : false;
        repo.setProtectConnection(protectConnection);

    }

    @Override
    public ConnectorHostType toJAXB() throws DtoTranslationException {
        ConnectorHostType object = new ConnectorHostType();
        RConnectorHostType.copyToJAXB(this, object);
        return object;
    }
}
