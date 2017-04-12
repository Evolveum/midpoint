/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.data.common.container;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.RAccessCertificationCampaign;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RActivation;
import com.evolveum.midpoint.repo.sql.data.common.embedded.REmbeddedReference;
import com.evolveum.midpoint.repo.sql.data.common.id.RContainerId;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbName;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.midpoint.repo.sql.query.definition.OwnerGetter;
import com.evolveum.midpoint.repo.sql.query.definition.OwnerIdGetter;
import com.evolveum.midpoint.repo.sql.query2.definition.IdQueryProperty;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.MidPointSingleTablePersister;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationWorkItemType;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Persister;
import org.jetbrains.annotations.NotNull;

import javax.persistence.*;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * @author lazyman
 * @author mederly
 */

@JaxbType(type = AccessCertificationCaseType.class)
@Entity
@IdClass(RContainerId.class)
@Table(name = "m_acc_cert_case", indexes = {
        @Index(name = "iCaseObjectRefTargetOid", columnList = "objectRef_targetOid"),
        @Index(name = "iCaseTargetRefTargetOid", columnList = "targetRef_targetOid"),
        @Index(name = "iCaseTenantRefTargetOid", columnList = "tenantRef_targetOid"),
        @Index(name = "iCaseOrgRefTargetOid", columnList = "orgRef_targetOid")
})
@Persister(impl = MidPointSingleTablePersister.class)
public class RAccessCertificationCase implements Container<RAccessCertificationCampaign> {

    private static final Trace LOGGER = TraceManager.getTrace(RAccessCertificationCase.class);

    public static final String F_OWNER = "owner";

    private Boolean trans;

    private byte[] fullObject;

    private RAccessCertificationCampaign owner;
    private String ownerOid;
    private Integer id;

    private Set<RAccessCertificationWorkItem> workItems = new HashSet<>();
    private REmbeddedReference objectRef;
    private REmbeddedReference targetRef;
    private REmbeddedReference tenantRef;
    private REmbeddedReference orgRef;
    private RActivation activation;                 // we need mainly validFrom + validTo + maybe adminStatus; for simplicity we added whole ActivationType here

    private XMLGregorianCalendar reviewRequestedTimestamp;
    private XMLGregorianCalendar reviewDeadline;
    private XMLGregorianCalendar remediedTimestamp;
    private String currentStageOutcome;
    private Integer stageNumber;
    private String outcome;

    public RAccessCertificationCase() {
    }

    @Id
    @org.hibernate.annotations.ForeignKey(name = "fk_acc_cert_case_owner")
    @MapsId("owner")
    @ManyToOne(fetch = FetchType.LAZY)
    @OwnerGetter(ownerClass = RAccessCertificationCampaign.class)
    public RAccessCertificationCampaign getOwner() {
        return owner;
    }

    @Column(name = "owner_oid", length = RUtil.COLUMN_LENGTH_OID, nullable = false)
    @OwnerIdGetter()
    public String getOwnerOid() {
        return ownerOid;
    }

    @Id
    @GeneratedValue(generator = "ContainerIdGenerator")
    @GenericGenerator(name = "ContainerIdGenerator", strategy = "com.evolveum.midpoint.repo.sql.util.ContainerIdGenerator")
    @Column(name = "id")
    @IdQueryProperty
    public Integer getId() {
        return id;
    }

    @JaxbName(localPart = "workItem")
    @OneToMany(mappedBy = "owner", orphanRemoval = true)
    @org.hibernate.annotations.ForeignKey(name = "none")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RAccessCertificationWorkItem> getWorkItems() {
        return workItems;
    }

    public void setWorkItems(Set<RAccessCertificationWorkItem> workItems) {
        this.workItems = workItems != null ? workItems : new HashSet<>();
    }

    @Embedded
    public REmbeddedReference getTargetRef() {
        return targetRef;
    }

    @Embedded
    public REmbeddedReference getObjectRef() {
        return objectRef;
    }

    @Embedded
    public REmbeddedReference getTenantRef() {
        return tenantRef;
    }

    @Embedded
    public REmbeddedReference getOrgRef() {
        return orgRef;
    }

    @Embedded
    public RActivation getActivation() {
        return activation;
    }

    @JaxbName(localPart = "currentStageCreateTimestamp")
    public XMLGregorianCalendar getReviewRequestedTimestamp() {
        return reviewRequestedTimestamp;
    }

    @JaxbName(localPart = "currentStageDeadline")
    public XMLGregorianCalendar getReviewDeadline() {
        return reviewDeadline;
    }

    public XMLGregorianCalendar getRemediedTimestamp() {
        return remediedTimestamp;
    }

    public String getCurrentStageOutcome() {
        return currentStageOutcome;
    }

    public Integer getStageNumber() {
        return stageNumber;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOwner(RAccessCertificationCampaign owner) {
        this.owner = owner;
        if (owner != null) {        // sometimes we are called with null owner but non-null ownerOid
            this.ownerOid = owner.getOid();
        }
    }

    public void setOwnerOid(String ownerOid) {
        this.ownerOid = ownerOid;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setTargetRef(REmbeddedReference targetRef) {
        this.targetRef = targetRef;
    }

    public void setObjectRef(REmbeddedReference objectRef) {
        this.objectRef = objectRef;
    }

    public void setTenantRef(REmbeddedReference tenantRef) {
        this.tenantRef = tenantRef;
    }

    public void setOrgRef(REmbeddedReference orgRef) {
        this.orgRef = orgRef;
    }

    public void setActivation(RActivation activation) {
        this.activation = activation;
    }

    public void setReviewRequestedTimestamp(XMLGregorianCalendar reviewRequestedTimestamp) {
        this.reviewRequestedTimestamp = reviewRequestedTimestamp;
    }

    public void setReviewDeadline(XMLGregorianCalendar reviewDeadline) {
        this.reviewDeadline = reviewDeadline;
    }

    public void setRemediedTimestamp(XMLGregorianCalendar remediedTimestamp) {
        this.remediedTimestamp = remediedTimestamp;
    }

    public void setCurrentStageOutcome(String currentStageOutcome) {
        this.currentStageOutcome = currentStageOutcome;
    }

    public void setStageNumber(Integer stageNumber) {
        this.stageNumber = stageNumber;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    @Lob
    public byte[] getFullObject() {
        return fullObject;
    }

    public void setFullObject(byte[] fullObject) {
        this.fullObject = fullObject;
    }

    // Notes to equals/hashCode: don't include trans nor owner
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof RAccessCertificationCase))
            return false;
        RAccessCertificationCase that = (RAccessCertificationCase) o;
        return Arrays.equals(fullObject, that.fullObject) &&
                Objects.equals(ownerOid, that.ownerOid) &&
                Objects.equals(id, that.id) &&
                Objects.equals(workItems, that.workItems) &&
                Objects.equals(objectRef, that.objectRef) &&
                Objects.equals(targetRef, that.targetRef) &&
                Objects.equals(tenantRef, that.tenantRef) &&
                Objects.equals(orgRef, that.orgRef) &&
                Objects.equals(activation, that.activation) &&
                Objects.equals(reviewRequestedTimestamp, that.reviewRequestedTimestamp) &&
                Objects.equals(reviewDeadline, that.reviewDeadline) &&
                Objects.equals(remediedTimestamp, that.remediedTimestamp) &&
                currentStageOutcome == that.currentStageOutcome &&
                Objects.equals(stageNumber, that.stageNumber) &&
                Objects.equals(outcome, that.outcome);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fullObject, ownerOid, id, workItems, objectRef, targetRef, tenantRef, orgRef, activation,
                reviewRequestedTimestamp, reviewDeadline, remediedTimestamp, currentStageOutcome, stageNumber,
                outcome);
    }

    @Override
    public String toString() {
        return "RAccessCertificationCase{" +
                "id=" + id +
                ", ownerOid='" + ownerOid + '\'' +
                ", owner=" + owner +
                ", targetRef=" + targetRef +
                ", objectRef=" + objectRef +
                '}';
    }

    @Override
    @Transient
    public Boolean isTransient() {
        return trans;
    }

    @Override
    public void setTransient(Boolean trans) {
        this.trans = trans;
    }

    public static RAccessCertificationCase toRepo(@NotNull RAccessCertificationCampaign owner, AccessCertificationCaseType case1, RepositoryContext context) throws DtoTranslationException {
		RAccessCertificationCase rCase = new RAccessCertificationCase();
		rCase.setOwner(owner);
        toRepo(rCase, case1, context);
        return rCase;
    }

    public static RAccessCertificationCase toRepo(String ownerOid, AccessCertificationCaseType case1, RepositoryContext context) throws DtoTranslationException {
		RAccessCertificationCase rCase = new RAccessCertificationCase();
		rCase.setOwnerOid(ownerOid);
        toRepo(rCase, case1, context);
        return rCase;
    }

    private static RAccessCertificationCase toRepo(RAccessCertificationCase rCase, AccessCertificationCaseType case1,
			RepositoryContext context) throws DtoTranslationException {
        rCase.setTransient(null);       // we don't try to advise hibernate - let it do its work, even if it would cost some SELECTs
        rCase.setId(RUtil.toInteger(case1.getId()));
        rCase.setObjectRef(RUtil.jaxbRefToEmbeddedRepoRef(case1.getObjectRef(), context.prismContext));
        rCase.setTargetRef(RUtil.jaxbRefToEmbeddedRepoRef(case1.getTargetRef(), context.prismContext));
        rCase.setTenantRef(RUtil.jaxbRefToEmbeddedRepoRef(case1.getTenantRef(), context.prismContext));
        rCase.setOrgRef(RUtil.jaxbRefToEmbeddedRepoRef(case1.getOrgRef(), context.prismContext));
        if (case1.getActivation() != null) {
            RActivation activation = new RActivation();
            RActivation.copyFromJAXB(case1.getActivation(), activation, context);
            rCase.setActivation(activation);
        }
		for (AccessCertificationWorkItemType workItem : case1.getWorkItem()) {
			rCase.getWorkItems().add(RAccessCertificationWorkItem.toRepo(rCase, workItem, context));
		}
        rCase.setReviewRequestedTimestamp(case1.getCurrentStageCreateTimestamp());
        rCase.setReviewDeadline(case1.getCurrentStageDeadline());
        rCase.setRemediedTimestamp(case1.getRemediedTimestamp());
        rCase.setCurrentStageOutcome(case1.getCurrentStageOutcome());
        rCase.setStageNumber(case1.getStageNumber());
        rCase.setOutcome(case1.getOutcome());
        PrismContainerValue<AccessCertificationCaseType> cvalue = case1.asPrismContainerValue();
        String xml;
        try {
            xml = context.prismContext.xmlSerializer().serialize(cvalue, SchemaConstantsGenerated.C_VALUE);
        } catch (SchemaException e) {
            throw new IllegalStateException("Couldn't serialize certification case to string", e);
        }
        LOGGER.trace("RAccessCertificationCase full object\n{}", xml);
        byte[] fullObject = RUtil.getByteArrayFromXml(xml, false);
        rCase.setFullObject(fullObject);

        return rCase;
    }

    public AccessCertificationCaseType toJAXB(PrismContext prismContext) throws SchemaException {
        return createJaxb(fullObject, prismContext, true);
    }

    // TODO find appropriate name
    public static AccessCertificationCaseType createJaxb(byte[] fullObject, PrismContext prismContext, boolean removeCampaignRef) throws SchemaException {
        String xml = RUtil.getXmlFromByteArray(fullObject, false);
        LOGGER.trace("RAccessCertificationCase full object to be parsed\n{}", xml);
        try {
            return prismContext.parserFor(xml).xml().compat().parseRealValue(AccessCertificationCaseType.class);
        } catch (SchemaException e) {
            LOGGER.debug("Couldn't parse certification case because of schema exception ({}):\nData: {}", e, xml);
            throw e;
        } catch (RuntimeException e) {
            LOGGER.debug("Couldn't parse certification case because of unexpected exception ({}):\nData: {}", e, xml);
            throw e;
        }
        //aCase.asPrismContainerValue().removeReference(AccessCertificationCaseType.F_CAMPAIGN_REF);
    }
}
