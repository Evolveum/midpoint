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

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.id.RCertWorkItemReferenceId;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.midpoint.repo.sql.query2.definition.NotQueryable;
import com.evolveum.midpoint.repo.sql.util.MidPointSingleTablePersister;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import org.apache.commons.lang.Validate;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.Persister;

import javax.persistence.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author lazyman
 * @author mederly
 *
 * Reference contained in a certification work item.
 *
 */
@JaxbType(type = ObjectReferenceType.class)
@Entity
@IdClass(RCertWorkItemReferenceId.class)
@Table(name = RCertWorkItemReference.TABLE, indexes = {
        @javax.persistence.Index(name = "iCertWorkItemRefTargetOid", columnList = "targetOid")
})
@Persister(impl = MidPointSingleTablePersister.class)
public class RCertWorkItemReference extends RReference {

    public static final String TABLE = "m_acc_cert_wi_reference";

    private RAccessCertificationWorkItem owner;
	private String ownerOwnerOwnerOid;					// campaign OID
	private Integer ownerOwnerId;						// case ID
	private Integer ownerId;							// work item ID

	@ForeignKey(name = "fk_acc_cert_wi_ref_owner")      // max. 30 chars (Oracle)
    @MapsId("workItem")
    @ManyToOne(fetch = FetchType.LAZY)
    @NotQueryable
    public RAccessCertificationWorkItem getOwner() {
        return owner;
    }

	public void setOwner(RAccessCertificationWorkItem owner) {
		this.owner = owner;
		if (owner != null) {
			this.ownerOwnerOwnerOid = owner.getOwnerOwnerOid();
			this.ownerOwnerId = owner.getOwnerId();
			this.ownerId = owner.getId();
		}
	}

	@Column(name = "owner_owner_owner_oid", length = RUtil.COLUMN_LENGTH_OID)
    @NotQueryable
    public String getOwnerOwnerOwnerOid() {
        return ownerOwnerOwnerOid;
    }

	public void setOwnerOwnerOwnerOid(String ownerOwnerOwnerOid) {
		this.ownerOwnerOwnerOid = ownerOwnerOwnerOid;
	}

	@Column(name = "owner_owner_id", length = RUtil.COLUMN_LENGTH_OID)
    @NotQueryable
    public Integer getOwnerOwnerId() {
        return ownerOwnerId;
    }

	public void setOwnerOwnerId(Integer ownerOwnerId) {
		this.ownerOwnerId = ownerOwnerId;
	}

	@Column(name = "owner_id")
    @NotQueryable
    public Integer getOwnerId() {
        return ownerId;
    }

	public void setOwnerId(Integer ownerId) {
		this.ownerId = ownerId;
	}

	//@MapsId("target")
    @ForeignKey(name="none")
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(referencedColumnName = "oid", updatable = false, insertable = false, nullable = true)
    @NotFound(action = NotFoundAction.IGNORE)
    @NotQueryable
    // only for HQL use
    public RObject getTarget() {
        return null;
    }

    @Id
    @Column(name = "targetOid", length = RUtil.COLUMN_LENGTH_OID)
    public String getTargetOid() {
        return super.getTargetOid();
    }

    @Id
    @Column(name="relation", length = RUtil.COLUMN_LENGTH_QNAME)
    public String getRelation() {
        return super.getRelation();
    }

	@Column(name = "containerType")
	@Enumerated(EnumType.ORDINAL)
	@Override
	public RObjectType getType() {
		return super.getType();
	}

	public static Set<RCertWorkItemReference> safeListReferenceToSet(List<ObjectReferenceType> list, PrismContext prismContext,
			RAccessCertificationWorkItem owner) {
        Set<RCertWorkItemReference> set = new HashSet<>();
        if (list == null || list.isEmpty()) {
            return set;
        }

        for (ObjectReferenceType ref : list) {
            RCertWorkItemReference rRef = jaxbRefToRepo(ref, prismContext, owner);
            if (rRef != null) {
                set.add(rRef);
            }
        }
        return set;
    }

    public static RCertWorkItemReference jaxbRefToRepo(ObjectReferenceType reference, PrismContext prismContext,
			RAccessCertificationWorkItem owner) {
        if (reference == null) {
            return null;
        }
        Validate.notNull(owner, "Owner of reference must not be null.");
        Validate.notEmpty(reference.getOid(), "Target oid reference must not be null.");

        RCertWorkItemReference repoRef = new RCertWorkItemReference();
        repoRef.setOwner(owner);
        RCertWorkItemReference.copyFromJAXB(reference, repoRef);
        return repoRef;
    }
}
