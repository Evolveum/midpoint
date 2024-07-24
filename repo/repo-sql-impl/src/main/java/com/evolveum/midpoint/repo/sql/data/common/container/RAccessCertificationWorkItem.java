/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.data.common.container;

import static com.evolveum.midpoint.repo.sql.data.common.container.RAccessCertificationWorkItem.TABLE;
import static com.evolveum.midpoint.schema.util.CertCampaignTypeUtil.norm;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Table;
import jakarta.persistence.*;
import javax.xml.datatype.XMLGregorianCalendar;

import org.hibernate.annotations.*;

import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.embedded.REmbeddedReference;
import com.evolveum.midpoint.repo.sql.data.common.id.RCertWorkItemId;
import com.evolveum.midpoint.repo.sql.query.definition.*;
import com.evolveum.midpoint.repo.sql.type.XMLGregorianCalendarType;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.MidPointSingleTablePersister;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.util.cases.WorkItemTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationWorkItemType;

@JaxbType(type = AccessCertificationWorkItemType.class)
@Entity
@IdClass(RCertWorkItemId.class)
@Table(name = TABLE)
@Persister(impl = MidPointSingleTablePersister.class)
@DynamicUpdate
public class RAccessCertificationWorkItem implements L2Container<RAccessCertificationCase> {

    public static final String TABLE = "m_acc_cert_wi";

    private Boolean trans;

    private String ownerOwnerOid; // campaign OID
    private RAccessCertificationCase owner;
    private Integer ownerId;
    private Integer id;

    private Integer iteration;
    private Integer stageNumber;
    private Set<RCertWorkItemReference> assigneeRef = new HashSet<>();
    private REmbeddedReference performerRef;
    private String outcome;
    private XMLGregorianCalendar outputChangeTimestamp;
    private XMLGregorianCalendar closeTimestamp;

    public RAccessCertificationWorkItem() {
    }

    // ridiculous name, but needed in order to match case.owner_oid
    @Id
    @Override
    @Column(name = "owner_owner_oid", length = RUtil.COLUMN_LENGTH_OID, nullable = false)
    //@OwnerIdGetter()            // this is not a single-valued owner id
    public String getOwnerOwnerOid() {
        return ownerOwnerOid;
    }

    @Override
    public void setOwnerOwnerOid(String ownerOwnerOid) {
        this.ownerOwnerOid = ownerOwnerOid;
    }

    @Override
    @MapsId
    @ManyToOne(fetch = FetchType.LAZY)
    @OwnerGetter(ownerClass = RAccessCertificationCase.class)
    @JoinColumns(
            value = {
                    @JoinColumn(name = "owner_owner_oid", referencedColumnName = "owner_oid"),
                    @JoinColumn(name = "owner_id", referencedColumnName = "id")
            },
            foreignKey = @ForeignKey(name = "fk_acc_cert_wi_owner")
    )
    public RAccessCertificationCase getOwner() {
        return owner;
    }

    @Override
    public void setOwner(RAccessCertificationCase _case) {
        this.owner = _case;
        if (_case != null) {            // sometimes we are called with null _case but non-null IDs
            this.ownerId = _case.getId();
            this.ownerOwnerOid = _case.getOwnerOid();
        }
    }

    @Id
    @Override
    @Column(name = "owner_id", length = RUtil.COLUMN_LENGTH_OID, nullable = false)
    //@OwnerIdGetter()            // this is not a single-valued owner id
    public Integer getOwnerId() {
        return ownerId;
    }

    @Override
    public void setOwnerId(Integer ownerId) {
        this.ownerId = ownerId;
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

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    @Column(nullable = false)
    public Integer getIteration() {
        return iteration;
    }

    @Column
    public Integer getStageNumber() {
        return stageNumber;
    }

    public void setIteration(Integer iteration) {
        this.iteration = iteration;
    }

    public void setStageNumber(Integer stageNumber) {
        this.stageNumber = stageNumber;
    }

    @JaxbName(localPart = "assigneeRef")
    @OneToMany(mappedBy = "owner", orphanRemoval = true, cascade = CascadeType.ALL)
    @org.hibernate.annotations.ForeignKey(name = "none")
    public Set<RCertWorkItemReference> getAssigneeRef() {
        return assigneeRef;
    }

    public void setAssigneeRef(Set<RCertWorkItemReference> assigneeRef) {
        this.assigneeRef = assigneeRef;
    }

    @Column
    public REmbeddedReference getPerformerRef() {
        return performerRef;
    }

    public void setPerformerRef(REmbeddedReference performerRef) {
        this.performerRef = performerRef;
    }

    @JaxbPath(itemPath = { @JaxbName(localPart = "output"), @JaxbName(localPart = "outcome") })
    @Column
    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    @Column
    @Type(XMLGregorianCalendarType.class)
    public XMLGregorianCalendar getOutputChangeTimestamp() {
        return outputChangeTimestamp;
    }

    public void setOutputChangeTimestamp(XMLGregorianCalendar outputChangeTimestamp) {
        this.outputChangeTimestamp = outputChangeTimestamp;
    }

    @Column
    @Type(XMLGregorianCalendarType.class)
    public XMLGregorianCalendar getCloseTimestamp() {
        return closeTimestamp;
    }

    public void setCloseTimestamp(XMLGregorianCalendar closeTimestamp) {
        this.closeTimestamp = closeTimestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (!(o instanceof RAccessCertificationWorkItem)) { return false; }
        RAccessCertificationWorkItem that = (RAccessCertificationWorkItem) o;
        return Objects.equals(ownerOwnerOid, that.ownerOwnerOid) &&
                Objects.equals(ownerId, that.ownerId) &&
                Objects.equals(id, that.id) &&
                Objects.equals(iteration, that.iteration) &&
                Objects.equals(stageNumber, that.stageNumber) &&
                Objects.equals(assigneeRef, that.assigneeRef) &&
                Objects.equals(performerRef, that.performerRef) &&
                Objects.equals(outcome, that.outcome) &&
                Objects.equals(outputChangeTimestamp, that.outputChangeTimestamp) &&
                Objects.equals(closeTimestamp, that.closeTimestamp);
    }

    @Override
    public int hashCode() {
        return Objects
                .hash(ownerOwnerOid, ownerId, id, iteration, stageNumber, assigneeRef, performerRef, outcome, outputChangeTimestamp, closeTimestamp);
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

    public static RAccessCertificationWorkItem toRepo(RAccessCertificationCase _case,
            AccessCertificationWorkItemType workItem, RepositoryContext context) {

        RAccessCertificationWorkItem rWorkItem = new RAccessCertificationWorkItem();
        rWorkItem.setOwner(_case);
        toRepo(rWorkItem, workItem, context);
        return rWorkItem;
    }

    public static RAccessCertificationWorkItem toRepo(String campaignOid, Integer caseId, AccessCertificationWorkItemType workItem,
            RepositoryContext context) throws DtoTranslationException {
        RAccessCertificationWorkItem rWorkItem = new RAccessCertificationWorkItem();
        rWorkItem.setOwnerOwnerOid(campaignOid);
        rWorkItem.setOwnerId(caseId);
        toRepo(rWorkItem, workItem, context);
        return rWorkItem;
    }

    private static void toRepo(RAccessCertificationWorkItem rWorkItem,
            AccessCertificationWorkItemType workItem, RepositoryContext context) {
        // we don't try to advise hibernate - let it do its work, even if it would cost some SELECTs
        rWorkItem.setTransient(null);
        Integer idInt = RUtil.toInteger(workItem.getId());
        if (idInt == null) {
            throw new IllegalArgumentException("No ID for access certification work item: " + workItem);
        }
        rWorkItem.setId(idInt);
        rWorkItem.setIteration(norm(workItem.getIteration()));
        rWorkItem.setStageNumber(workItem.getStageNumber());
        rWorkItem.getAssigneeRef().addAll(RCertWorkItemReference.safeListReferenceToSet(
                workItem.getAssigneeRef(), rWorkItem, context.relationRegistry));
        rWorkItem.setPerformerRef(RUtil.jaxbRefToEmbeddedRepoRef(workItem.getPerformerRef(), context.relationRegistry));
        rWorkItem.setOutcome(WorkItemTypeUtil.getOutcome(workItem));
        rWorkItem.setOutputChangeTimestamp(workItem.getOutputChangeTimestamp());
        rWorkItem.setCloseTimestamp(workItem.getCloseTimestamp());
    }
}
