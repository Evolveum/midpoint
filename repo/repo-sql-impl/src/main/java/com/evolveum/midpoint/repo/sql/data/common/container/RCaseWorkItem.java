/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.common.container;

import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.RCase;
import com.evolveum.midpoint.repo.sql.data.common.embedded.REmbeddedReference;
import com.evolveum.midpoint.repo.sql.data.common.id.RCaseWorkItemId;
import com.evolveum.midpoint.repo.sql.data.common.other.RCaseWorkItemReferenceOwner;
import com.evolveum.midpoint.repo.sql.query.definition.*;
import com.evolveum.midpoint.repo.sql.query2.definition.IdQueryProperty;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.MidPointSingleTablePersister;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.util.WorkItemTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.*;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.*;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static com.evolveum.midpoint.repo.sql.data.common.container.RCaseWorkItem.TABLE;

/**
 * @author mederly
 */

@JaxbType(type = CaseWorkItemType.class)
@Entity
@IdClass(RCaseWorkItemId.class)
@Table(name = TABLE, indexes = {
})
@Persister(impl = MidPointSingleTablePersister.class)
public class RCaseWorkItem implements Container<RCase> {

    public static final String TABLE = "m_case_wi";
    public static final String F_OWNER = "owner";

    private Boolean trans;

    private RCase owner;
    private String ownerOid;
    private Integer id;

    private Integer stageNumber;
    private REmbeddedReference originalAssigneeRef;
    private Set<RCaseWorkItemReference> assigneeRef = new HashSet<>();
    private Set<RCaseWorkItemReference> candidateRef = new HashSet<>();
    private REmbeddedReference performerRef;
    private String outcome;
    private XMLGregorianCalendar createTimestamp;
    private XMLGregorianCalendar closeTimestamp;
    private XMLGregorianCalendar deadline;

    public RCaseWorkItem() {
    }

    @Id
    @ForeignKey(name = "fk_case_wi_owner")
    @MapsId("owner")
    @ManyToOne(fetch = FetchType.LAZY)
    @OwnerGetter(ownerClass = RCase.class)
    public RCase getOwner() {
        return owner;
    }

    public void setOwner(RCase _case) {
        this.owner = _case;
        if (_case != null) {            // sometimes we are called with null _case but non-null IDs
            this.ownerOid = _case.getOid();
        }
    }

    @Column(name = "owner_oid", length = RUtil.COLUMN_LENGTH_OID, nullable = false)
    @OwnerIdGetter()
    public String getOwnerOid() {
        return ownerOid;
    }

    public void setOwnerOid(String ownerOid) {
        this.ownerOid = ownerOid;
    }

    @Id
    @GeneratedValue(generator = "ContainerIdGenerator")
    @GenericGenerator(name = "ContainerIdGenerator", strategy = "com.evolveum.midpoint.repo.sql.util.ContainerIdGenerator")
    @Column(name = "id")
    @IdQueryProperty
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Column
    public Integer getStageNumber() {
        return stageNumber;
    }

    public void setStageNumber(Integer stageNumber) {
        this.stageNumber = stageNumber;
    }

    @Embedded
    public REmbeddedReference getOriginalAssigneeRef() {
        return originalAssigneeRef;
    }

    public void setOriginalAssigneeRef(REmbeddedReference originalAssigneeRef) {
        this.originalAssigneeRef = originalAssigneeRef;
    }

    @Where(clause = RCaseWorkItemReference.REFERENCE_TYPE + "= 0")
    @JaxbName(localPart = "assigneeRef")
    @OneToMany(mappedBy = "owner", orphanRemoval = true)
    @ForeignKey(name = "none")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RCaseWorkItemReference> getAssigneeRef() {
        return assigneeRef;
    }

    public void setAssigneeRef(Set<RCaseWorkItemReference> assigneeRef) {
        this.assigneeRef = assigneeRef;
    }

    @Where(clause = RCaseWorkItemReference.REFERENCE_TYPE + "= 1")
    @JaxbName(localPart = "candidateRef")
    @OneToMany(mappedBy = "owner", orphanRemoval = true)
    @ForeignKey(name = "none")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RCaseWorkItemReference> getCandidateRef() {
        return candidateRef;
    }

    public void setCandidateRef(Set<RCaseWorkItemReference> candidateRef) {
        this.candidateRef = candidateRef;
    }

    @Column
    public REmbeddedReference getPerformerRef() {
        return performerRef;
    }

    public void setPerformerRef(REmbeddedReference performerRef) {
        this.performerRef = performerRef;
    }

    @JaxbPath(itemPath = {@JaxbName(localPart = "output"), @JaxbName(localPart = "outcome")})
    @Column
    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    @Column
    public XMLGregorianCalendar getCloseTimestamp() {
        return closeTimestamp;
    }

    public void setCloseTimestamp(XMLGregorianCalendar closeTimestamp) {
        this.closeTimestamp = closeTimestamp;
    }

    @Column
    public XMLGregorianCalendar getCreateTimestamp() {
        return createTimestamp;
    }

    public void setCreateTimestamp(XMLGregorianCalendar createTimestamp) {
        this.createTimestamp = createTimestamp;
    }

    @Column
    public XMLGregorianCalendar getDeadline() {
        return deadline;
    }

    public void setDeadline(XMLGregorianCalendar deadline) {
        this.deadline = deadline;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof RCaseWorkItem))
            return false;
        RCaseWorkItem that = (RCaseWorkItem) o;
        return Objects.equals(ownerOid, that.ownerOid) &&
                Objects.equals(id, that.id) &&
                Objects.equals(stageNumber, that.stageNumber) &&
                Objects.equals(assigneeRef, that.assigneeRef) &&
                Objects.equals(performerRef, that.performerRef) &&
                Objects.equals(outcome, that.outcome) &&
                Objects.equals(closeTimestamp, that.closeTimestamp) &&
                Objects.equals(createTimestamp, that.createTimestamp) &&
                Objects.equals(deadline, that.deadline);
    }

    @Override
    public int hashCode() {
        return Objects
                .hash(ownerOid, id, stageNumber, assigneeRef, performerRef, outcome, closeTimestamp, createTimestamp, deadline);
    }

    @Transient
    public Boolean isTransient() {
        return trans;
    }

    public void setTransient(Boolean trans) {
        this.trans = trans;
    }

    public static RCaseWorkItem toRepo(RCase _case, CaseWorkItemType workItem, RepositoryContext context) throws DtoTranslationException {
        RCaseWorkItem rWorkItem = new RCaseWorkItem();
        rWorkItem.setOwner(_case);
        toRepo(rWorkItem, workItem, context);
        return rWorkItem;
    }

    public static RCaseWorkItem toRepo(String caseOid, CaseWorkItemType workItem,
                                       RepositoryContext context) throws DtoTranslationException {
        RCaseWorkItem rWorkItem = new RCaseWorkItem();
        rWorkItem.setOwnerOid(caseOid);
        toRepo(rWorkItem, workItem, context);
        return rWorkItem;
    }

    private static void toRepo(RCaseWorkItem rWorkItem, CaseWorkItemType workItem, RepositoryContext context) throws DtoTranslationException {
        rWorkItem.setTransient(null);       // we don't try to advise hibernate - let it do its work, even if it would cost some SELECTs
        Integer idInt = RUtil.toInteger(workItem.getId());
        rWorkItem.setId(idInt);
        rWorkItem.setStageNumber(workItem.getStageNumber());
        rWorkItem.setOriginalAssigneeRef(RUtil.jaxbRefToEmbeddedRepoRef(workItem.getOriginalAssigneeRef(), context.relationRegistry));
        rWorkItem.getAssigneeRef().addAll(RCaseWorkItemReference.safeListReferenceToSet(
                workItem.getAssigneeRef(), rWorkItem, context.relationRegistry, RCaseWorkItemReferenceOwner.ASSIGNEE));
        rWorkItem.getCandidateRef().addAll(RCaseWorkItemReference.safeListReferenceToSet(
                workItem.getCandidateRef(), rWorkItem, context.relationRegistry, RCaseWorkItemReferenceOwner.CANDIDATE));
        rWorkItem.setPerformerRef(RUtil.jaxbRefToEmbeddedRepoRef(workItem.getPerformerRef(), context.relationRegistry));
        rWorkItem.setOutcome(WorkItemTypeUtil.getOutcome(workItem));
        rWorkItem.setCloseTimestamp(workItem.getCloseTimestamp());
        rWorkItem.setCreateTimestamp(workItem.getCreateTimestamp());
        rWorkItem.setDeadline(workItem.getDeadline());
    }
}
