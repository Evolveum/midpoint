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
import com.evolveum.midpoint.repo.sql.data.common.embedded.REmbeddedReference;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.query.QueryAttribute;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.XmlSchemaType;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.List;
import java.util.Set;

/**
 * @author lazyman
 */
@Entity
@ForeignKey(name = "fk_connector")
@org.hibernate.annotations.Table(appliesTo = "m_connector",
        indexes = {@Index(name = "iConnectorName", columnNames = "name_norm")})
public class RConnector extends RObject {

    private static final Trace LOGGER = TraceManager.getTrace(RConnector.class);
    @QueryAttribute(polyString = true)
    private RPolyString name;
    @QueryAttribute
    private String framework;
    @QueryAttribute(reference = true)
    private REmbeddedReference connectorHostRef;
    @QueryAttribute
    private String connectorType;
    @QueryAttribute
    private String connectorVersion;
    @QueryAttribute
    private String connectorBundle;
    private Set<String> targetSystemType;
    @QueryAttribute
    private String namespace;
    private String xmlSchema;

    @Embedded
    public REmbeddedReference getConnectorHostRef() {
        return connectorHostRef;
    }

    public String getConnectorBundle() {
        return connectorBundle;
    }

    public String getConnectorType() {
        return connectorType;
    }

    public String getConnectorVersion() {
        return connectorVersion;
    }

    public String getNamespace() {
        return namespace;
    }

    @ElementCollection
    @ForeignKey(name = "fk_connector_target_system")
    @CollectionTable(name = "m_connector_target_system", joinColumns = {
            @JoinColumn(name = "connector_oid", referencedColumnName = "oid"),
            @JoinColumn(name = "connector_id", referencedColumnName = "id")
    })
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<String> getTargetSystemType() {
        return targetSystemType;
    }

    @Lob
    @Type(type = RUtil.LOB_STRING_TYPE)
    public String getXmlSchema() {
        return xmlSchema;
    }

    public String getFramework() {
        return framework;
    }

    @Embedded
    public RPolyString getName() {
        return name;
    }

    public void setName(RPolyString name) {
        this.name = name;
    }

    public void setFramework(String framework) {
        this.framework = framework;
    }

    public void setConnectorHostRef(REmbeddedReference connectorHostRef) {
        this.connectorHostRef = connectorHostRef;
    }

    public void setConnectorBundle(String connectorBundle) {
        this.connectorBundle = connectorBundle;
    }

    public void setConnectorType(String connectorType) {
        this.connectorType = connectorType;
    }

    public void setConnectorVersion(String connectorVersion) {
        this.connectorVersion = connectorVersion;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public void setTargetSystemType(Set<String> targetSystemType) {
        this.targetSystemType = targetSystemType;
    }

    public void setXmlSchema(String xmlSchema) {
        this.xmlSchema = xmlSchema;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        RConnector that = (RConnector) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (connectorBundle != null ? !connectorBundle.equals(that.connectorBundle) : that.connectorBundle != null)
            return false;
        if (connectorHostRef != null ? !connectorHostRef.equals(that.connectorHostRef) : that.connectorHostRef != null)
            return false;
        if (connectorType != null ? !connectorType.equals(that.connectorType) : that.connectorType != null)
            return false;
        if (connectorVersion != null ? !connectorVersion.equals(that.connectorVersion) : that.connectorVersion != null)
            return false;
        if (framework != null ? !framework.equals(that.framework) : that.framework != null) return false;
        if (namespace != null ? !namespace.equals(that.namespace) : that.namespace != null) return false;
        if (targetSystemType != null ? !targetSystemType.equals(that.targetSystemType) : that.targetSystemType != null)
            return false;
        if (xmlSchema != null ? !xmlSchema.equals(that.xmlSchema) : that.xmlSchema != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (framework != null ? framework.hashCode() : 0);
        result = 31 * result + (connectorType != null ? connectorType.hashCode() : 0);
        result = 31 * result + (connectorVersion != null ? connectorVersion.hashCode() : 0);
        result = 31 * result + (connectorBundle != null ? connectorBundle.hashCode() : 0);
        result = 31 * result + (namespace != null ? namespace.hashCode() : 0);
        result = 31 * result + (xmlSchema != null ? xmlSchema.hashCode() : 0);
        return result;
    }

    public static void copyToJAXB(RConnector repo, ConnectorType jaxb, PrismContext prismContext) throws
            DtoTranslationException {
        RObject.copyToJAXB(repo, jaxb, prismContext);

        jaxb.setName(RPolyString.copyToJAXB(repo.getName()));
        jaxb.setConnectorBundle(repo.getConnectorBundle());
        jaxb.setConnectorType(repo.getConnectorType());
        jaxb.setConnectorVersion(repo.getConnectorVersion());
        jaxb.setFramework(repo.getFramework());
        jaxb.setNamespace(repo.getNamespace());

        try {
            jaxb.setSchema(RUtil.toJAXB(ConnectorType.class, new ItemPath(ConnectorType.F_SCHEMA),
                    repo.getXmlSchema(), XmlSchemaType.class, prismContext));

            if (repo.getConnectorHostRef() != null) {
                jaxb.setConnectorHostRef(repo.getConnectorHostRef().toJAXB(prismContext));
            }

            List types = RUtil.safeSetToList(repo.getTargetSystemType());
            if (!types.isEmpty()) {
                jaxb.getTargetSystemType().addAll(types);
            }
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }

    public static void copyFromJAXB(ConnectorType jaxb, RConnector repo, PrismContext prismContext) throws
            DtoTranslationException {
        RObject.copyFromJAXB(jaxb, repo, prismContext);

        repo.setName(RPolyString.copyFromJAXB(jaxb.getName()));
        repo.setConnectorBundle(jaxb.getConnectorBundle());
        repo.setConnectorType(jaxb.getConnectorType());
        repo.setConnectorVersion(jaxb.getConnectorVersion());
        repo.setFramework(jaxb.getFramework());
        repo.setNamespace(jaxb.getNamespace());
        repo.setConnectorHostRef(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getConnectorHostRef(), prismContext));

        if (jaxb.getConnectorHost() != null) {
            LOGGER.warn("Connector host from connector type won't be saved. It should be " +
                    "translated to connector host reference.");
        }

        try {
            repo.setXmlSchema(RUtil.toRepo(jaxb.getSchema(), prismContext));
            repo.setTargetSystemType(RUtil.listToSet(jaxb.getTargetSystemType()));
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }

    @Override
    public ConnectorType toJAXB(PrismContext prismContext) throws DtoTranslationException {
        ConnectorType object = new ConnectorType();
        RUtil.revive(object, prismContext);
        RConnector.copyToJAXB(this, object, prismContext);

        return object;
    }
}
