/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.data.common;

import static com.evolveum.midpoint.schema.util.CertCampaignTypeUtil.norm;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.*;
import javax.xml.datatype.XMLGregorianCalendar;

import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.*;

import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.container.RAccessCertificationCase;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RSimpleEmbeddedReference;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.data.common.enums.RAccessCertificationCampaignState;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbName;
import com.evolveum.midpoint.repo.sql.query.definition.NeverNull;
import com.evolveum.midpoint.repo.sql.type.XMLGregorianCalendarType;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.IdGeneratorResult;
import com.evolveum.midpoint.repo.sql.util.MidPointJoinedPersister;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.type.descriptor.jdbc.IntegerJdbcType;

@Entity
@Table(name = RAccessCertificationCampaign.TABLE_NAME,
        uniqueConstraints = @UniqueConstraint(name = "uc_acc_cert_campaign_name", columnNames = { "name_norm" }),
        indexes = {
                @Index(name = "iCertCampaignNameOrig", columnList = "name_orig")
        }
)
@Persister(impl = MidPointJoinedPersister.class)
@ForeignKey(name = "fk_acc_cert_campaign")
@DynamicUpdate
public class RAccessCertificationCampaign extends RObject {

    public static final String TABLE_NAME = "m_acc_cert_campaign";

    private RPolyString nameCopy;
    private RSimpleEmbeddedReference definitionRef;
    private Set<RAccessCertificationCase> cases;

    private RSimpleEmbeddedReference ownerRefCampaign;
    private String handlerUri;
    private XMLGregorianCalendar start;
    private XMLGregorianCalendar end;
    private RAccessCertificationCampaignState state;
    private Integer iteration;
    private Integer stageNumber;

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

    @Embedded
    public RSimpleEmbeddedReference getDefinitionRef() {
        return definitionRef;
    }

    @Transient
    public Set<RAccessCertificationCase> getCase() {
        if (cases == null) {
            cases = new HashSet<>();
        }
        return cases;
    }

    @JaxbName(localPart = "ownerRef")
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "relation", column = @Column(name = "ownerRef_relation", length = RUtil.COLUMN_LENGTH_QNAME)),
            @AttributeOverride(name = "targetOid", column = @Column(name = "ownerRef_targetOid", length = RUtil.COLUMN_LENGTH_OID)),
            @AttributeOverride(name = "targetType", column = @Column(name = "ownerRef_targetType"))
    })
    public RSimpleEmbeddedReference getOwnerRefCampaign() {       // name changed because of collision with RAbstractRole.ownerRef
        return ownerRefCampaign;
    }

    public String getHandlerUri() {
        return handlerUri;
    }

    @JaxbName(localPart = "startTimestamp")
    @Column(name = "startTimestamp")
    @Type(XMLGregorianCalendarType.class)
    public XMLGregorianCalendar getStart() {
        return start;
    }

    @JaxbName(localPart = "endTimestamp")
    @Column(name = "endTimestamp")
    @Type(XMLGregorianCalendarType.class)
    public XMLGregorianCalendar getEnd() {
        return end;
    }

    @JdbcType(IntegerJdbcType.class)
    public RAccessCertificationCampaignState getState() {
        return state;
    }

    @Column(nullable = false)
    public Integer getIteration() {
        return iteration;
    }

    public Integer getStageNumber() {
        return stageNumber;
    }

    public void setDefinitionRef(RSimpleEmbeddedReference definitionRef) {
        this.definitionRef = definitionRef;
    }

    public void setCase(Set<RAccessCertificationCase> cases) {
        this.cases = cases;
    }

    public void setOwnerRefCampaign(RSimpleEmbeddedReference ownerRefCampaign) {
        this.ownerRefCampaign = ownerRefCampaign;
    }

    public void setHandlerUri(String handlerUri) {
        this.handlerUri = handlerUri;
    }

    public void setStart(XMLGregorianCalendar start) {
        this.start = start;
    }

    public void setEnd(XMLGregorianCalendar end) {
        this.end = end;
    }

    public void setState(RAccessCertificationCampaignState state) {
        this.state = state;
    }

    public void setIteration(Integer iteration) {
        this.iteration = iteration;
    }

    public void setStageNumber(Integer stageNumber) {
        this.stageNumber = stageNumber;
    }

    // dynamically called
    public static void copyFromJAXB(AccessCertificationCampaignType jaxb, RAccessCertificationCampaign repo,
            RepositoryContext repositoryContext, IdGeneratorResult generatorResult)
            throws DtoTranslationException {

        copyAssignmentHolderInformationFromJAXB(jaxb, repo, repositoryContext, generatorResult);
        repo.setNameCopy(RPolyString.copyFromJAXB(jaxb.getName()));
        repo.setDefinitionRef(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getDefinitionRef(), repositoryContext.relationRegistry));

        List<AccessCertificationCaseType> cases = jaxb.getCase();
        if (!cases.isEmpty()) {
            for (AccessCertificationCaseType case1 : cases) {
                RAccessCertificationCase rCase = RAccessCertificationCase.toRepo(repo, case1, repositoryContext);
                rCase.setTransient(generatorResult.isTransient(case1.asPrismContainerValue()));     // redundant?
                repo.getCase().add(rCase);
            }
        }

        repo.setOwnerRefCampaign(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getOwnerRef(), repositoryContext.relationRegistry));
        repo.setHandlerUri(jaxb.getHandlerUri());
        repo.setStart(jaxb.getStartTimestamp());
        repo.setEnd(jaxb.getEndTimestamp());
        repo.setState(RUtil.getRepoEnumValue(jaxb.getState(), RAccessCertificationCampaignState.class));
        repo.setIteration(norm(jaxb.getIteration()));
        repo.setStageNumber(jaxb.getStageNumber());
    }
}
