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
import com.evolveum.midpoint.repo.sql.data.common.id.RCaseWorkItemReferenceId;
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
 * Reference contained in a case work item.
 *
 */
@JaxbType(type = ObjectReferenceType.class)
@Entity
@IdClass(RCaseWorkItemReferenceId.class)
@Table(name = RCaseWorkItemReference.TABLE, indexes = {
        @Index(name = "iCaseWorkItemRefTargetOid", columnList = "targetOid")
})
@Persister(impl = MidPointSingleTablePersister.class)
public class RCaseWorkItemReference extends RReference {

    public static final String TABLE = "m_case_wi_reference";

    private RCaseWorkItem owner;
	private String ownerOwnerOid;						// case OID
	private Integer ownerId;							// work item ID

	@ForeignKey(name = "fk_case_wi_reference_owner")
    @MapsId("workItem")
    @ManyToOne(fetch = FetchType.LAZY)
    @NotQueryable
    public RCaseWorkItem getOwner() {
        return owner;
    }

	public void setOwner(RCaseWorkItem owner) {
		this.owner = owner;
		if (owner != null) {
			this.ownerOwnerOid = owner.getOwnerOid();
			this.ownerId = owner.getId();
		}
	}

	@Column(name = "owner_owner_oid", length = RUtil.COLUMN_LENGTH_OID)
    @NotQueryable
    public String getOwnerOwnerOid() {
        return ownerOwnerOid;
    }

	public void setOwnerOwnerOid(String ownerOwnerOid) {
		this.ownerOwnerOid = ownerOwnerOid;
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

	@Column(name = "targetType")
	@Enumerated(EnumType.ORDINAL)
	@Override
	public RObjectType getType() {
		return super.getType();
	}

	public static Set<RCaseWorkItemReference> safeListReferenceToSet(List<ObjectReferenceType> list, PrismContext prismContext,
			RCaseWorkItem owner) {
        Set<RCaseWorkItemReference> set = new HashSet<>();
        if (list == null || list.isEmpty()) {
            return set;
        }

        for (ObjectReferenceType ref : list) {
            RCaseWorkItemReference rRef = jaxbRefToRepo(ref, prismContext, owner);
            if (rRef != null) {
                set.add(rRef);
            }
        }
        return set;
    }

    public static RCaseWorkItemReference jaxbRefToRepo(ObjectReferenceType reference, PrismContext prismContext,
			RCaseWorkItem owner) {
        if (reference == null) {
            return null;
        }
        Validate.notNull(owner, "Owner of reference must not be null.");
        Validate.notEmpty(reference.getOid(), "Target oid reference must not be null.");

        RCaseWorkItemReference repoRef = new RCaseWorkItemReference();
        repoRef.setOwner(owner);
        RCaseWorkItemReference.copyFromJAXB(reference, repoRef);
        return repoRef;
    }
}
