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

package com.evolveum.midpoint.repo.sql.data.common;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.embedded.REmbeddedReference;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.IdGeneratorResult;
import com.evolveum.midpoint.repo.sql.util.MidPointJoinedPersister;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Persister;

import javax.persistence.*;
import java.util.Collection;
import java.util.Set;

/**
 * @author lazyman
 */
@Entity
@ForeignKey(name = "fk_connector")
@Persister(impl = MidPointJoinedPersister.class)
public class RConnector extends RObject<ConnectorType> {

    private static final Trace LOGGER = TraceManager.getTrace(RConnector.class);
    private RPolyString nameCopy;
    private String framework;
    private REmbeddedReference connectorHostRef;
    private String connectorType;
    private String connectorVersion;
    private String connectorBundle;
    private Set<String> targetSystemType;

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

    @ElementCollection
    @ForeignKey(name = "fk_connector_target_system")
    @CollectionTable(name = "m_connector_target_system", joinColumns = {
            @JoinColumn(name = "connector_oid", referencedColumnName = "oid")
    })
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<String> getTargetSystemType() {
        return targetSystemType;
    }

    public String getFramework() {
        return framework;
    }

    @AttributeOverrides({
            @AttributeOverride(name = "orig", column = @Column(name = "name_orig")),
            @AttributeOverride(name = "norm", column = @Column(name = "name_norm"))
    })
    @Embedded
    public RPolyString getNameCopy() {
        return nameCopy;
    }

    public void setNameCopy(RPolyString nameCopy) {
        this.nameCopy = nameCopy;
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

    public void setTargetSystemType(Set<String> targetSystemType) {
        this.targetSystemType = targetSystemType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        RConnector that = (RConnector) o;

        if (nameCopy != null ? !nameCopy.equals(that.nameCopy) : that.nameCopy != null) return false;
        if (connectorBundle != null ? !connectorBundle.equals(that.connectorBundle) : that.connectorBundle != null)
            return false;
        if (connectorHostRef != null ? !connectorHostRef.equals(that.connectorHostRef) : that.connectorHostRef != null)
            return false;
        if (connectorType != null ? !connectorType.equals(that.connectorType) : that.connectorType != null)
            return false;
        if (connectorVersion != null ? !connectorVersion.equals(that.connectorVersion) : that.connectorVersion != null)
            return false;
        if (framework != null ? !framework.equals(that.framework) : that.framework != null) return false;
        if (targetSystemType != null ? !targetSystemType.equals(that.targetSystemType) : that.targetSystemType != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (nameCopy != null ? nameCopy.hashCode() : 0);
        result = 31 * result + (framework != null ? framework.hashCode() : 0);
        result = 31 * result + (connectorType != null ? connectorType.hashCode() : 0);
        result = 31 * result + (connectorVersion != null ? connectorVersion.hashCode() : 0);
        result = 31 * result + (connectorBundle != null ? connectorBundle.hashCode() : 0);

        return result;
    }

    public static void copyFromJAXB(ConnectorType jaxb, RConnector repo, RepositoryContext repositoryContext,
            IdGeneratorResult generatorResult) throws DtoTranslationException {
        RObject.copyFromJAXB(jaxb, repo, repositoryContext, generatorResult);

        repo.setNameCopy(RPolyString.copyFromJAXB(jaxb.getName()));
        repo.setConnectorBundle(jaxb.getConnectorBundle());
        repo.setConnectorType(jaxb.getConnectorType());
        repo.setConnectorVersion(jaxb.getConnectorVersion());
        repo.setFramework(jaxb.getFramework());
        repo.setConnectorHostRef(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getConnectorHostRef(), repositoryContext.prismContext));

        if (jaxb.getConnectorHost() != null) {
            LOGGER.warn("Connector host from connector type won't be saved. It should be " +
                    "translated to connector host reference.");
        }

        try {
            repo.setTargetSystemType(RUtil.listToSet(jaxb.getTargetSystemType()));
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }

    @Override
    public ConnectorType toJAXB(PrismContext prismContext, Collection<SelectorOptions<GetOperationOptions>> options)
            throws DtoTranslationException {
        ConnectorType object = new ConnectorType();
        RUtil.revive(object, prismContext);
        RConnector.copyToJAXB(this, object, prismContext, options);

        return object;
    }
}
