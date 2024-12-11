/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.data.common;

import java.util.Set;
import jakarta.persistence.*;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Persister;

import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RSimpleEmbeddedReference;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbName;
import com.evolveum.midpoint.repo.sql.query.definition.NeverNull;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.IdGeneratorResult;
import com.evolveum.midpoint.repo.sql.util.MidPointJoinedPersister;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;

@Entity
@ForeignKey(name = "fk_connector")
@Persister(impl = MidPointJoinedPersister.class)
@Table(indexes = {
        @Index(name = "iConnectorNameOrig", columnList = "name_orig"),
        @Index(name = "iConnectorNameNorm", columnList = "name_norm") })
@DynamicUpdate
public class RConnector extends RObject {

    private RPolyString nameCopy;
    private String framework;
    private RSimpleEmbeddedReference connectorHostRef;
    private String connectorType;
    private String connectorVersion;
    private String connectorBundle;
    private Set<String> targetSystemType;
    private Boolean available;

    @Embedded
    public RSimpleEmbeddedReference getConnectorHostRef() {
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

    @Column
    public Boolean getAvailable() {
        return available;
    }

    @ElementCollection
    @ForeignKey(name = "fk_connector_target_system")
    @CollectionTable(name = "m_connector_target_system", joinColumns = {
            @JoinColumn(name = "connector_oid", referencedColumnName = "oid")
    })
    @Cascade({ org.hibernate.annotations.CascadeType.ALL })
    public Set<String> getTargetSystemType() {
        return targetSystemType;
    }

    public String getFramework() {
        return framework;
    }

    @JaxbName(localPart = "name")
    @AttributeOverrides({
            @AttributeOverride(name = "orig", column = @Column(name = "name_orig")),
            @AttributeOverride(name = "norm", column = @Column(name = "name_norm"))
    })
    @Embedded
    @NeverNull
    public RPolyString getNameCopy() {
        return nameCopy;
    }

    public void setNameCopy(RPolyString nameCopy) {
        this.nameCopy = nameCopy;
    }

    public void setFramework(String framework) {
        this.framework = framework;
    }

    public void setConnectorHostRef(RSimpleEmbeddedReference connectorHostRef) {
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

    public void setAvailable(Boolean available) {
        this.available = available;
    }

    // dynamically called
    public static void copyFromJAXB(ConnectorType jaxb, RConnector repo, RepositoryContext repositoryContext,
            IdGeneratorResult generatorResult) throws DtoTranslationException {
        copyAssignmentHolderInformationFromJAXB(jaxb, repo, repositoryContext, generatorResult);

        repo.setNameCopy(RPolyString.copyFromJAXB(jaxb.getName()));
        repo.setConnectorBundle(jaxb.getConnectorBundle());
        repo.setConnectorType(jaxb.getConnectorType());
        repo.setConnectorVersion(jaxb.getConnectorVersion());
        repo.setFramework(jaxb.getFramework());
        repo.setConnectorHostRef(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getConnectorHostRef(), repositoryContext.relationRegistry));
        repo.setAvailable(jaxb.isAvailable());

        try {
            repo.setTargetSystemType(RUtil.listToSet(jaxb.getTargetSystemType()));
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }
}
