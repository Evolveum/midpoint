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
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.query.QueryAttribute;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ConnectorHostType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ProtectedStringType;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Type;

import javax.persistence.*;

/**
 * @author lazyman
 */
@Entity
@ForeignKey(name = "fk_connector_host")
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"name_norm"}))
public class RConnectorHost extends RObject {

    @QueryAttribute(polyString = true)
    private RPolyString name;
    private String hostname;
    private String port;
    private String sharedSecret;
    private Boolean protectConnection;
    private Integer timeout;

    @Lob
    @Type(type = RUtil.LOB_STRING_TYPE)
    public String getSharedSecret() {
        return sharedSecret;
    }

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

    @Column(nullable = true)
    public Boolean isProtectConnection() {
        return protectConnection;
    }

    @Embedded
    public RPolyString getName() {
        return name;
    }

    public void setName(RPolyString name) {
        this.name = name;
    }

    public void setProtectConnection(Boolean protectConnection) {
        this.protectConnection = protectConnection;
    }

    @Column(nullable = true)
    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public void setSharedSecret(String sharedSecret) {
        this.sharedSecret = sharedSecret;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        RConnectorHost that = (RConnectorHost) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (hostname != null ? !hostname.equals(that.hostname) : that.hostname != null) return false;
        if (port != null ? !port.equals(that.port) : that.port != null) return false;
        if (protectConnection != null ? !protectConnection.equals(that.protectConnection) : that.protectConnection != null)
            return false;
        if (sharedSecret != null ? !sharedSecret.equals(that.sharedSecret) : that.sharedSecret != null) return false;
        if (timeout != null ? !timeout.equals(that.timeout) : that.timeout != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (hostname != null ? hostname.hashCode() : 0);
        result = 31 * result + (port != null ? port.hashCode() : 0);
        result = 31 * result + (sharedSecret != null ? sharedSecret.hashCode() : 0);
        result = 31 * result + (protectConnection != null ? protectConnection.hashCode() : 0);
        result = 31 * result + (timeout != null ? timeout.hashCode() : 0);
        return result;
    }

    public static void copyToJAXB(RConnectorHost repo, ConnectorHostType jaxb, PrismContext prismContext) throws
            DtoTranslationException {
        RObject.copyToJAXB(repo, jaxb, prismContext);

        jaxb.setName(RPolyString.copyToJAXB(repo.getName()));
        jaxb.setHostname(repo.getHostname());
        jaxb.setPort(repo.getPort());
        jaxb.setProtectConnection(repo.isProtectConnection());
        jaxb.setTimeout(repo.getTimeout());

        try {
            jaxb.setSharedSecret(RUtil.toJAXB(ConnectorHostType.class, new ItemPath(ConnectorHostType.F_SHARED_SECRET),
                    repo.getSharedSecret(), ProtectedStringType.class, prismContext));
        } catch (Exception ex) {
            new DtoTranslationException(ex.getMessage(), ex);
        }
    }

    public static void copyFromJAXB(ConnectorHostType jaxb, RConnectorHost repo, PrismContext prismContext) throws
            DtoTranslationException {
        RObject.copyFromJAXB(jaxb, repo, prismContext);

        repo.setName(RPolyString.copyFromJAXB(jaxb.getName()));
        repo.setHostname(jaxb.getHostname());
        repo.setPort(jaxb.getPort());
        repo.setTimeout(jaxb.getTimeout());
        repo.setProtectConnection(jaxb.isProtectConnection());

        try {
            repo.setSharedSecret(RUtil.toRepo(jaxb.getSharedSecret(), prismContext));
        } catch (Exception ex) {
            new DtoTranslationException(ex.getMessage(), ex);
        }
    }

    @Override
    public ConnectorHostType toJAXB(PrismContext prismContext) throws DtoTranslationException {
        ConnectorHostType object = new ConnectorHostType();
        RUtil.revive(object, prismContext);
        RConnectorHost.copyToJAXB(this, object, prismContext);

        return object;
    }
}
