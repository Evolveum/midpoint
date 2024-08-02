/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.data.common;

import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.*;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.*;

import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.embedded.REmbeddedReference;
import com.evolveum.midpoint.repo.sql.data.common.embedded.ROperationalState;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.data.common.enums.RResourceAdministrativeState;
import com.evolveum.midpoint.repo.sql.data.common.other.RReferenceType;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbName;
import com.evolveum.midpoint.repo.sql.query.definition.NeverNull;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.IdGeneratorResult;
import com.evolveum.midpoint.repo.sql.util.MidPointJoinedPersister;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceBusinessConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.hibernate.type.descriptor.jdbc.IntegerJdbcType;

@Entity
@ForeignKey(name = "fk_resource")
@Table(uniqueConstraints = @UniqueConstraint(name = "uc_resource_name", columnNames = { "name_norm" }),
        indexes = {
                @Index(name = "iResourceNameOrig", columnList = "name_orig"),
        }
)
@Persister(impl = MidPointJoinedPersister.class)
@DynamicUpdate
public class RResource extends RObject {

    private RPolyString nameCopy;
    private REmbeddedReference connectorRef;
    private ROperationalState operationalState;
    //resource business configuration, embedded component can't be used, because then it couldn't use
    //non embedded approverRef relationship
    private RResourceAdministrativeState administrativeState;
    private Set<RObjectReference<RFocus>> approverRef;
    //end of resource business configuration
    // administrativeOperationalStateAdministrativeAvailabilityStatus works only in new repo by design
    private Boolean template;

    @Enumerated(EnumType.ORDINAL)
    @JdbcType(IntegerJdbcType.class)
    public RResourceAdministrativeState getAdministrativeState() {
        return administrativeState;
    }

    @Where(clause = RObjectReference.REFERENCE_TYPE + "= 2")
    @OneToMany(mappedBy = "owner", orphanRemoval = true)
    @ForeignKey(name = "none")
    @Cascade({ org.hibernate.annotations.CascadeType.ALL })
    public Set<RObjectReference<RFocus>> getApproverRef() {
        if (approverRef == null) {
            approverRef = new HashSet<>();
        }
        return approverRef;
    }

    @Embedded
    public REmbeddedReference getConnectorRef() {
        return connectorRef;
    }

    @Embedded
    public ROperationalState getOperationalState() {
        return operationalState;
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

    @Column
    public Boolean getTemplate() {
        return template;
    }

    public void setNameCopy(RPolyString nameCopy) {
        this.nameCopy = nameCopy;
    }

    public void setAdministrativeState(RResourceAdministrativeState administrativeState) {
        this.administrativeState = administrativeState;
    }

    public void setApproverRef(Set<RObjectReference<RFocus>> approverRef) {
        this.approverRef = approverRef;
    }

    public void setOperationalState(ROperationalState operationalState) {
        this.operationalState = operationalState;
    }

    public void setConnectorRef(REmbeddedReference connectorRef) {
        this.connectorRef = connectorRef;
    }

    public void setTemplate(Boolean template) {
        this.template = template;
    }

    // dynamically called
    public static void copyFromJAXB(ResourceType jaxb, RResource repo, RepositoryContext repositoryContext,
            IdGeneratorResult generatorResult) throws DtoTranslationException {
        copyAssignmentHolderInformationFromJAXB(jaxb, repo, repositoryContext, generatorResult);

        repo.setNameCopy(RPolyString.copyFromJAXB(jaxb.getName()));
        repo.setConnectorRef(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getConnectorRef(), repositoryContext.relationRegistry));
        repo.setTemplate(jaxb.isTemplate());

        try {
            if (jaxb.getBusiness() != null) {
                ResourceBusinessConfigurationType business = jaxb.getBusiness();
                repo.getApproverRef().addAll(RUtil.toRObjectReferenceSet(business.getApproverRef(),
                        repo, RReferenceType.RESOURCE_BUSINESS_CONFIGURATION_APPROVER, repositoryContext.relationRegistry));
                repo.setAdministrativeState(RUtil.getRepoEnumValue(business.getAdministrativeState(),
                        RResourceAdministrativeState.class));
            }
            if (jaxb.getOperationalState() != null) {
                ROperationalState repoOpState = new ROperationalState();
                ROperationalState.fromJaxb(jaxb.getOperationalState(), repoOpState);
                repo.setOperationalState(repoOpState);
            }
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }
}
