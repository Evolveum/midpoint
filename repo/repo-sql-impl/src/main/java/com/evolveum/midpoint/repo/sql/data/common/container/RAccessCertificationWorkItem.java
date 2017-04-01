/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import com.evolveum.midpoint.repo.sql.data.common.id.RCertWorkItemId;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbName;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.midpoint.repo.sql.query.definition.OwnerGetter;
import com.evolveum.midpoint.repo.sql.query.definition.OwnerIdGetter;
import com.evolveum.midpoint.repo.sql.query2.definition.IdQueryProperty;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.MidPointSingleTablePersister;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationWorkItemType;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Persister;

import javax.persistence.*;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static com.evolveum.midpoint.repo.sql.data.common.container.RAccessCertificationWorkItem.TABLE;

/**
 * @author mederly
 */

@JaxbType(type = AccessCertificationWorkItemType.class)
@Entity
@IdClass(RCertWorkItemId.class)
@Table(name = TABLE, indexes = {
})
@Persister(impl = MidPointSingleTablePersister.class)
public class RAccessCertificationWorkItem implements L2Container<RAccessCertificationCase> {

    private static final Trace LOGGER = TraceManager.getTrace(RAccessCertificationWorkItem.class);

	public static final String TABLE = "m_acc_cert_wi";
    public static final String F_OWNER = "owner";

    private Boolean trans;

    private String ownerOwnerOid;						// campaign OID
    private RAccessCertificationCase owner;
    private Integer ownerId;
    private Integer id;

    private Integer stageNumber;
    private Set<RCertWorkItemReference> reviewerRef = new HashSet<>();
    private REmbeddedReference responderRef;
    private RAccessCertificationResponse response;
    private XMLGregorianCalendar timestamp;
    private XMLGregorianCalendar closedTimestamp;

    public RAccessCertificationWorkItem() {
    }

    // ridiculous name, but needed in order to match case.owner_oid
    @Column(name = "owner_owner_oid", length = RUtil.COLUMN_LENGTH_OID, nullable = false)
    //@OwnerIdGetter()			// this is not a single-valued owner id
    public String getOwnerOwnerOid() {
        return ownerOwnerOid;
    }

	public void setOwnerOwnerOid(String ownerOwnerOid) {
		this.ownerOwnerOid = ownerOwnerOid;
	}

	@Id
    @ForeignKey(name = "fk_acc_cert_wi_owner")
    @MapsId("owner")
    @ManyToOne(fetch = FetchType.LAZY)
    @OwnerGetter(ownerClass = RAccessCertificationCase.class)
    public RAccessCertificationCase getOwner() {
        return owner;
    }

	public void setOwner(RAccessCertificationCase _case) {
		this.owner = _case;
		if (_case != null) {            // sometimes we are called with null _case but non-null IDs
			this.ownerId = _case.getId();
			this.ownerOwnerOid = _case.getOwnerOid();
		}
	}

	@Column(name = "owner_id", length = RUtil.COLUMN_LENGTH_OID, nullable = false)
	//@OwnerIdGetter()			// this is not a single-valued owner id
    public Integer getOwnerId() {
        return ownerId;
    }

	public void setOwnerId(Integer ownerId) {
		this.ownerId = ownerId;
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

	@JaxbName(localPart = "reviewerRef")
    @OneToMany(mappedBy = "owner", orphanRemoval = true)
    @ForeignKey(name = "none")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RCertWorkItemReference> getReviewerRef() {
        return reviewerRef;
    }

    public void setReviewerRef(Set<RCertWorkItemReference> reviewerRef) {
        this.reviewerRef = reviewerRef;
    }

    @Column
	public REmbeddedReference getResponderRef() {
		return responderRef;
	}

	public void setResponderRef(REmbeddedReference responderRef) {
		this.responderRef = responderRef;
	}

	@Column
	public RAccessCertificationResponse getResponse() {
		return response;
	}

	public void setResponse(RAccessCertificationResponse response) {
		this.response = response;
	}

	@Column
	public XMLGregorianCalendar getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(XMLGregorianCalendar timestamp) {
		this.timestamp = timestamp;
	}

	@Column
	public XMLGregorianCalendar getClosedTimestamp() {
		return closedTimestamp;
	}

	public void setClosedTimestamp(XMLGregorianCalendar closedTimestamp) {
		this.closedTimestamp = closedTimestamp;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof RAccessCertificationWorkItem))
			return false;
		RAccessCertificationWorkItem that = (RAccessCertificationWorkItem) o;
		return Objects.equals(ownerOwnerOid, that.ownerOwnerOid) &&
				Objects.equals(ownerId, that.ownerId) &&
				Objects.equals(id, that.id) &&
				Objects.equals(stageNumber, that.stageNumber) &&
				Objects.equals(reviewerRef, that.reviewerRef) &&
				Objects.equals(responderRef, that.responderRef) &&
				response == that.response &&
				Objects.equals(timestamp, that.timestamp) &&
				Objects.equals(closedTimestamp, that.closedTimestamp);
	}

	@Override
	public int hashCode() {
		return Objects
				.hash(ownerOwnerOid, ownerId, id, stageNumber, reviewerRef, responderRef, response, timestamp, closedTimestamp);
	}

	@Transient
    public Boolean isTransient() {
        return trans;
    }

    public void setTransient(Boolean trans) {
        this.trans = trans;
    }

    public static RAccessCertificationWorkItem toRepo(RAccessCertificationCase _case, AccessCertificationWorkItemType workItem, RepositoryContext context) throws DtoTranslationException {
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
			AccessCertificationWorkItemType workItem, RepositoryContext context) throws DtoTranslationException {
        rWorkItem.setTransient(null);       // we don't try to advise hibernate - let it do its work, even if it would cost some SELECTs
		Integer idInt = RUtil.toInteger(workItem.getId());
		if (idInt == null) {
			throw new IllegalArgumentException("No ID for access certification work item: " + workItem);
		}
		rWorkItem.setId(idInt);
		rWorkItem.setStageNumber(workItem.getStageNumber());
        rWorkItem.getReviewerRef().addAll(RCertWorkItemReference.safeListReferenceToSet(
                workItem.getReviewerRef(), context.prismContext, rWorkItem));
        rWorkItem.setResponderRef(RUtil.jaxbRefToEmbeddedRepoRef(workItem.getResponderRef(), context.prismContext));
        rWorkItem.setResponse(RUtil.getRepoEnumValue(workItem.getResponse(), RAccessCertificationResponse.class));
        rWorkItem.setTimestamp(workItem.getTimestamp());
        rWorkItem.setClosedTimestamp(workItem.getClosedTimestamp());
    }
}