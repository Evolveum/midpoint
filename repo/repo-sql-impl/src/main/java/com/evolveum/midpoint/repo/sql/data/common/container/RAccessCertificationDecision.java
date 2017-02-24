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

import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.embedded.REmbeddedReference;
import com.evolveum.midpoint.repo.sql.data.common.enums.RAccessCertificationResponse;
import com.evolveum.midpoint.repo.sql.data.common.id.RL2ContainerId;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.midpoint.repo.sql.query2.definition.NotQueryable;
import com.evolveum.midpoint.repo.sql.util.MidPointSingleTablePersister;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDecisionType;
import org.hibernate.annotations.*;

import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.xml.datatype.XMLGregorianCalendar;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDecisionType.F_STAGE_NUMBER;

/**
 * Although being an image of a Containerable, this class is not a container in the sense of Container class:
 * its owner is not a RObject.
 *
 * @author mederly
 */

@JaxbType(type = AccessCertificationDecisionType.class)
@Entity
@IdClass(RL2ContainerId.class)
@Table(name = "m_acc_cert_decision",
        uniqueConstraints =
            @UniqueConstraint(
                    name = "uc_case_stage_reviewer",
                    columnNames = {"owner_owner_oid", "owner_id", "stageNumber", "reviewerRef_targetOid" }),

        indexes = {
//        @Index(name = "iObjectRefTargetOid", columnList = "objectRef_targetOid"),
//        @Index(name = "iTargetRefTargetOid", columnList = "targetRef_targetOid")
})
@Persister(impl = MidPointSingleTablePersister.class)
public class RAccessCertificationDecision implements L2Container<RAccessCertificationCase> {

    private static final Trace LOGGER = TraceManager.getTrace(RAccessCertificationDecision.class);

    public static final String F_OWNER = "owner";

    private Boolean trans;

    private RAccessCertificationCase owner;
    private String ownerOwnerOid;
    private Integer ownerId;
    private Integer id;

    private int stageNumber;
    private REmbeddedReference reviewerRef;
    private RAccessCertificationResponse response;
    private String comment;
    private XMLGregorianCalendar timestamp;

    public RAccessCertificationDecision() {
    }

    @Id
    @org.hibernate.annotations.ForeignKey(name = "fk_acc_cert_decision_owner")
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("owner")
    @NotQueryable
    public RAccessCertificationCase getOwner() {
        return owner;
    }

    @Column(name = "owner_owner_oid", length = RUtil.COLUMN_LENGTH_OID, nullable = false)
    @NotQueryable
    public String getOwnerOwnerOid() {
        if (owner != null && ownerOwnerOid == null) {
            ownerOwnerOid = owner.getOwnerOid();
        }
        return ownerOwnerOid;
    }

    @Id
    @NotQueryable
    @Column(name = "owner_id")
    public Integer getOwnerId() {
        if (owner != null && ownerId == null) {
            ownerId = owner.getId();
        }
        return ownerId;
    }

    @Id
    @GeneratedValue(generator = "ContainerIdGenerator")
    @GenericGenerator(name = "ContainerIdGenerator", strategy = "com.evolveum.midpoint.repo.sql.util.ContainerIdGenerator")
    @Column(name = "id")
    @NotQueryable
    public Integer getId() {
        return id;
    }

    public int getStageNumber() {
        return stageNumber;
    }

    @Embedded
    public REmbeddedReference getReviewerRef() {
        return reviewerRef;
    }

    public RAccessCertificationResponse getResponse() {
        return response;
    }

    @Column(name = "reviewerComment")           // in Oracle, 'comment' is a reserved word
    public String getComment() {
        return comment;
    }

    public XMLGregorianCalendar getTimestamp() {
        return timestamp;
    }

    public void setOwner(RAccessCertificationCase owner) {
        this.owner = owner;
    }

    public void setOwnerOwnerOid(String campaignOid) {
        this.ownerOwnerOid = campaignOid;
    }

    public void setOwnerId(Integer caseId) {
        this.ownerId = caseId;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setStageNumber(int stageNumber) {
        this.stageNumber = stageNumber;
    }

    public void setReviewerRef(REmbeddedReference reviewerRef) {
        this.reviewerRef = reviewerRef;
    }

    public void setResponse(RAccessCertificationResponse response) {
        this.response = response;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setTimestamp(XMLGregorianCalendar timestamp) {
        this.timestamp = timestamp;
    }

    @Transient
    public Boolean isTransient() {
        return trans;
    }

    public void setTransient(Boolean trans) {
        this.trans = trans;
    }

    public static RAccessCertificationDecision toRepo(RAccessCertificationCase owningCase, AccessCertificationDecisionType decision, RepositoryContext context) {
        RAccessCertificationDecision rDecision = toRepo(decision, context);
        rDecision.setOwner(owningCase);
        return rDecision;
    }

    private static RAccessCertificationDecision toRepo(AccessCertificationDecisionType decision, RepositoryContext context) {
        RAccessCertificationDecision rDecision = new RAccessCertificationDecision();
        rDecision.setTransient(null);       // we don't try to advise hibernate - let it do its work, even if it would cost some SELECTs
        Integer idInt = RUtil.toInteger(decision.getId());
        if (idInt == null) {
            throw new IllegalArgumentException("No ID for access certification decision: " + decision);
        }
        rDecision.setId(idInt);
        if (!decision.asPrismContainerValue().contains(F_STAGE_NUMBER)) {       // this check should be part of prism container maybe
            throw new IllegalArgumentException("No stage number for access certification decision: " + decision);
        }
        rDecision.setStageNumber(decision.getStageNumber());
        rDecision.setReviewerRef(RUtil.jaxbRefToEmbeddedRepoRef(decision.getReviewerRef(), context.prismContext));
        rDecision.setResponse(RUtil.getRepoEnumValue(decision.getResponse(), RAccessCertificationResponse.class));
        rDecision.setComment(decision.getComment());
        rDecision.setTimestamp(decision.getTimestamp());
        return rDecision;
    }

}
