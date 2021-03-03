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
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Persister;

import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbName;
import com.evolveum.midpoint.repo.sql.query.definition.NeverNull;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.IdGeneratorResult;
import com.evolveum.midpoint.repo.sql.util.MidPointJoinedPersister;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

/**
 * @author lazyman
 */
@Entity
@ForeignKey(name = "fk_org")
@Table(uniqueConstraints = @UniqueConstraint(name = "uc_org_name", columnNames = { "name_norm" }),
        indexes = {
                @javax.persistence.Index(name = "iOrgNameOrig", columnList = "name_orig"),
        }
)
@Persister(impl = MidPointJoinedPersister.class)
public class ROrg extends RAbstractRole {

    private RPolyString nameCopy;
    @Deprecated //todo remove collection in 3.9
    private Set<String> orgType;
    private Boolean tenant;
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

    @ElementCollection
    @CollectionTable(name = "m_org_org_type", joinColumns = {
            @JoinColumn(name = "org_oid", referencedColumnName = "oid",
                    foreignKey = @javax.persistence.ForeignKey(name = "fk_org_org_type"))
    })
    @Cascade({ org.hibernate.annotations.CascadeType.ALL })
    public Set<String> getOrgType() {
        return orgType;
    }

    @Index(name = "iDisplayOrder")
    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public void setOrgType(Set<String> orgType) {
        this.orgType = orgType;
    }

    public Boolean getTenant() {
        return tenant;
    }

    public void setTenant(Boolean tenant) {
        this.tenant = tenant;
    }

    // dynamically called
    public static void copyFromJAXB(OrgType jaxb, ROrg repo, RepositoryContext repositoryContext,
            IdGeneratorResult generatorResult) throws DtoTranslationException {
        RAbstractRole.copyFromJAXB(jaxb, repo, repositoryContext, generatorResult);

        repo.setNameCopy(RPolyString.copyFromJAXB(jaxb.getName()));
        repo.setOrgType(RUtil.listToSet(jaxb.getOrgType()));
        repo.setTenant(jaxb.isTenant());
    }
}
