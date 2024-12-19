/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.data.common.container;

import static com.evolveum.midpoint.schema.util.CertCampaignTypeUtil.norm;
import static com.evolveum.midpoint.util.MiscUtil.or0;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.repo.sql.data.common.embedded.RSimpleActivation;

import jakarta.persistence.CascadeType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.*;
import org.hibernate.annotations.*;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.RAccessCertificationCampaign;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RSimpleEmbeddedReference;
import com.evolveum.midpoint.repo.sql.data.common.id.RContainerId;
import com.evolveum.midpoint.repo.sql.query.definition.*;
import com.evolveum.midpoint.repo.sql.type.XMLGregorianCalendarType;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.MidPointSingleTablePersister;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationWorkItemType;

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
@DynamicUpdate
public class RAccessCertificationCase implements Container<RAccessCertificationCampaign> {

    private static final Trace LOGGER = TraceManager.getTrace(RAccessCertificationCase.class);

    private Boolean trans;

    private byte[] fullObject;

    private RAccessCertificationCampaign owner;
    private String ownerOid;
    private Integer id;

    private Set<RAccessCertificationWorkItem> workItems = new HashSet<>();
    private RSimpleEmbeddedReference objectRef;
    private RSimpleEmbeddedReference targetRef;
    private RSimpleEmbeddedReference tenantRef;
    private RSimpleEmbeddedReference orgRef;
    // we need mainly validFrom + validTo + maybe adminStatus; for simplicity we added whole ActivationType here
    private RSimpleActivation activation;

    private XMLGregorianCalendar reviewRequestedTimestamp;
    private XMLGregorianCalendar reviewDeadline;
    private XMLGregorianCalendar remediedTimestamp;
    private String currentStageOutcome;
    private Integer iteration;
    private Integer stageNumber;
    private String outcome;

    public RAccessCertificationCase() {
    }

    @Override
    @JoinColumn(name = "owner_oid", referencedColumnName = "oid", foreignKey = @ForeignKey(name = "fk_acc_cert_case_owner"))
    @MapsId
    @ManyToOne(fetch = FetchType.LAZY)
    @OwnerGetter(ownerClass = RAccessCertificationCampaign.class)
    public RAccessCertificationCampaign getOwner() {
        return owner;
    }

    @Id
    @Override
    @Column(name = "owner_oid", length = RUtil.COLUMN_LENGTH_OID, nullable = false)
    @OwnerIdGetter()
    public String getOwnerOid() {
        if (owner != null && ownerOid == null) {
            ownerOid = owner.getOid();
        }
        return ownerOid;
    }

    @Override
    @Id
    @GeneratedValue(generator = "ContainerIdGenerator")
    @GenericGenerator(name = "ContainerIdGenerator", strategy = "com.evolveum.midpoint.repo.sql.util.ContainerIdGenerator")
    @Column(name = "id")
    @IdQueryProperty
    public Integer getId() {
        return id;
    }

    @JaxbName(localPart = "workItem")
    @OneToMany(mappedBy = "owner", orphanRemoval = true, cascade = CascadeType.ALL)
    public Set<RAccessCertificationWorkItem> getWorkItems() {
        return workItems;
    }

    public void setWorkItems(Set<RAccessCertificationWorkItem> workItems) {
        this.workItems = workItems != null ? workItems : new HashSet<>();
    }

    @Embedded
    public RSimpleEmbeddedReference getTargetRef() {
        return targetRef;
    }

    @Embedded
    public RSimpleEmbeddedReference getObjectRef() {
        return objectRef;
    }

    @Embedded
    public RSimpleEmbeddedReference getTenantRef() {
        return tenantRef;
    }

    @Embedded
    public RSimpleEmbeddedReference getOrgRef() {
        return orgRef;
    }

    @Embedded
    public RSimpleActivation getActivation() {
        return activation;
    }

    @JaxbName(localPart = "currentStageCreateTimestamp")
    @Type(XMLGregorianCalendarType.class)
    public XMLGregorianCalendar getReviewRequestedTimestamp() {
        return reviewRequestedTimestamp;
    }

    @JaxbName(localPart = "currentStageDeadline")
    @Type(XMLGregorianCalendarType.class)
    public XMLGregorianCalendar getReviewDeadline() {
        return reviewDeadline;
    }

    @Type(XMLGregorianCalendarType.class)
    public XMLGregorianCalendar getRemediedTimestamp() {
        return remediedTimestamp;
    }

    public String getCurrentStageOutcome() {
        return currentStageOutcome;
    }

    @Column(nullable = false)
    public Integer getIteration() {
        return iteration;
    }

    public Integer getStageNumber() {
        return stageNumber;
    }

    public String getOutcome() {
        return outcome;
    }

    @Override
    public void setOwner(RAccessCertificationCampaign owner) {
        this.owner = owner;
        if (owner != null) {
            setOwnerOid(owner.getOid());
        }
    }

    @Override
    public void setOwnerOid(String ownerOid) {
        this.ownerOid = ownerOid;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public void setTargetRef(RSimpleEmbeddedReference targetRef) {
        this.targetRef = targetRef;
    }

    public void setObjectRef(RSimpleEmbeddedReference objectRef) {
        this.objectRef = objectRef;
    }

    public void setTenantRef(RSimpleEmbeddedReference tenantRef) {
        this.tenantRef = tenantRef;
    }

    public void setOrgRef(RSimpleEmbeddedReference orgRef) {
        this.orgRef = orgRef;
    }

    public void setActivation(RSimpleActivation activation) {
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

    public void setIteration(Integer iteration) {
        this.iteration = iteration;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RAccessCertificationCase)) {
            return false;
        }

        RAccessCertificationCase that = (RAccessCertificationCase) o;
        return Objects.equals(getOwnerOid(), that.getOwnerOid())
                && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getOwnerOid(), id);
    }

    @Override
    public String toString() {
        return "RAccessCertificationCase{" +
                "id=" + id +
                ", ownerOid='" + getOwnerOid() + '\'' +
                ", targetRef=" + targetRef +
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

    private static void toRepo(RAccessCertificationCase rCase, AccessCertificationCaseType case1,
            RepositoryContext context) throws DtoTranslationException {
        rCase.setTransient(null); // we don't try to advise hibernate - let it do its work, even if it would cost some SELECTs
        rCase.setId(RUtil.toInteger(case1.getId()));
        rCase.setObjectRef(RUtil.jaxbRefToEmbeddedRepoRef(case1.getObjectRef(), context.relationRegistry));
        rCase.setTargetRef(RUtil.jaxbRefToEmbeddedRepoRef(case1.getTargetRef(), context.relationRegistry));
        rCase.setTenantRef(RUtil.jaxbRefToEmbeddedRepoRef(case1.getTenantRef(), context.relationRegistry));
        rCase.setOrgRef(RUtil.jaxbRefToEmbeddedRepoRef(case1.getOrgRef(), context.relationRegistry));
        if (case1.getActivation() != null) {
            RSimpleActivation activation = new RSimpleActivation();
            RSimpleActivation.fromJaxb(case1.getActivation(), activation);
            rCase.setActivation(activation);
        }
        for (AccessCertificationWorkItemType workItem : case1.getWorkItem()) {
            rCase.getWorkItems().add(RAccessCertificationWorkItem.toRepo(rCase, workItem, context));
        }
        rCase.setReviewRequestedTimestamp(case1.getCurrentStageCreateTimestamp());
        rCase.setReviewDeadline(case1.getCurrentStageDeadline());
        rCase.setRemediedTimestamp(case1.getRemediedTimestamp());
        rCase.setCurrentStageOutcome(case1.getCurrentStageOutcome());
        rCase.setIteration(norm(case1.getIteration()));
        rCase.setStageNumber(or0(case1.getStageNumber()));
        rCase.setOutcome(case1.getOutcome());
        //noinspection unchecked
        PrismContainerValue<AccessCertificationCaseType> cvalue = case1.asPrismContainerValue();
        String serializedForm;
        try {
            serializedForm = context.prismContext
                    .serializerFor(context.configuration.getFullObjectFormat())
                    .serialize(cvalue, SchemaConstantsGenerated.C_VALUE);
        } catch (SchemaException e) {
            throw new IllegalStateException("Couldn't serialize certification case to string", e);
        }
        LOGGER.trace("RAccessCertificationCase full object\n{}", serializedForm);
        byte[] fullObject = RUtil.getBytesFromSerializedForm(serializedForm, false);
        rCase.setFullObject(fullObject);
    }

    public AccessCertificationCaseType toJAXB() throws SchemaException {
        return createJaxb(fullObject);
    }

    public static AccessCertificationCaseType createJaxb(byte[] fullObject) throws SchemaException {
        String serializedFrom = RUtil.getSerializedFormFromBytes(fullObject);
        LOGGER.trace("RAccessCertificationCase full object to be parsed\n{}", serializedFrom);
        try {
            return PrismContext.get().parserFor(serializedFrom)
                    .compat()
                    .fastAddOperations()
                    .parseRealValue(AccessCertificationCaseType.class);
        } catch (SchemaException e) {
            LOGGER.debug("Couldn't parse certification case because of schema exception ({}):\nData: {}", e, serializedFrom);
            throw e;
        } catch (RuntimeException e) {
            LOGGER.debug("Couldn't parse certification case because of unexpected exception ({}):\nData: {}", e, serializedFrom);
            throw e;
        }
    }
}
