/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.data.common;

import java.util.Set;
import javax.persistence.*;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Persister;

import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbName;
import com.evolveum.midpoint.repo.sql.query.definition.NeverNull;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.IdGeneratorResult;
import com.evolveum.midpoint.repo.sql.util.MidPointJoinedPersister;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ServiceType;

/**
 * @author Viliam Repan (lazyman)
 */
@Entity
@ForeignKey(name = "fk_service")
@Persister(impl = MidPointJoinedPersister.class)
@Table(uniqueConstraints = @UniqueConstraint(name = "uc_service_name", columnNames = { "name_norm" }),
        indexes = {
                @Index(name = "iServiceNameOrig", columnList = "name_orig"),
                @Index(name = "iServiceNameNorm", columnList = "name_norm") })
public class RService extends RAbstractRole {

    private RPolyString nameCopy;
    @Deprecated //todo remove collection in 3.9
    private Set<String> serviceType;
    private Integer displayOrder;

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

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    @ElementCollection
    @ForeignKey(name = "fk_service_type")
    @CollectionTable(name = "m_service_type", joinColumns = {
            @JoinColumn(name = "service_oid", referencedColumnName = "oid")
    })
    @Cascade({ org.hibernate.annotations.CascadeType.ALL })
    public Set<String> getServiceType() {
        return serviceType;
    }

    public void setServiceType(Set<String> serviceType) {
        this.serviceType = serviceType;
    }

    // dynamically called
    public static void copyFromJAXB(ServiceType jaxb, RService repo, RepositoryContext repositoryContext,
            IdGeneratorResult generatorResult) throws DtoTranslationException {
        RAbstractRole.copyFromJAXB(jaxb, repo, repositoryContext, generatorResult);

        repo.setDisplayOrder(jaxb.getDisplayOrder());
        repo.setServiceType(RUtil.listToSet(jaxb.getServiceType()));
        repo.setNameCopy(RPolyString.copyFromJAXB(jaxb.getName()));
    }
}
