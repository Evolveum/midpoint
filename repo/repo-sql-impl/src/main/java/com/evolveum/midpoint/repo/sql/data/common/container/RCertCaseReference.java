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

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.id.RCObjectReferenceId;
import com.evolveum.midpoint.repo.sql.data.common.other.RCReferenceOwner;
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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.Table;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author lazyman
 * @author mederly
 *
 * Reference contained in a certification case.
 *
 */
@JaxbType(type = ObjectReferenceType.class)
@Entity
@IdClass(RCObjectReferenceId.class)
@Table(name = RCertCaseReference.TABLE, indexes = {
        @javax.persistence.Index(name = "iCaseReferenceTargetOid", columnList = "targetOid")
})
@Persister(impl = MidPointSingleTablePersister.class)
public class RCertCaseReference extends RContainerReference {

    public static final String TABLE = "m_acc_cert_case_reference";

    private RAccessCertificationCase owner;

//    private RPolyString targetName;
//
//    public RPolyString getTargetName() {
//        return targetName;
//    }

    @ForeignKey(name = "fk_acc_cert_case_ref_owner")
    @MapsId("owner")
    @ManyToOne(fetch = FetchType.LAZY)
    @NotQueryable
    public RAccessCertificationCase getOwner() {
        return owner;
    }

    @Id
    @Column(name = "owner_owner_oid", length = RUtil.COLUMN_LENGTH_OID)
    @NotQueryable
    public String getOwnerOid() {
        return super.getOwnerOid();
    }


    @Id
    @Column(name = "owner_id")
    @NotQueryable
    public Integer getOwnerId() {
        return super.getOwnerId();
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
    @Override
    public String getTargetOid() {
        return super.getTargetOid();
    }

    @Id
    @Column(name="relation", length = RUtil.COLUMN_LENGTH_QNAME)
    public String getRelation() {
        return super.getRelation();
    }

    /**
     * Represents {@link javax.xml.namespace.QName} type attribute in reference e.g.
     * {@link com.evolveum.midpoint.xml.ns._public.common.common_3.UserType} represented
     * as enum {@link RObjectType#USER}
     *
     * @return null if not defined, otherwise value from {@link RObjectType} enum
     */
    @Column(name = "containerType")
    @Enumerated(EnumType.ORDINAL)
    @Override
    public RObjectType getType() {
        return super.getType();
    }

    @Id
    @Column(name = REFERENCE_TYPE, nullable = false)
    public RCReferenceOwner getReferenceType() {
        return super.getReferenceType();
    }

    public void setOwner(RAccessCertificationCase owner) {
        this.owner = owner;
    }

//    public void setTargetName(RPolyString targetName) {
//        this.targetName = targetName;
//    }

    public static Set<RCertCaseReference> safeListReferenceToSet(List<ObjectReferenceType> list, PrismContext prismContext,
                                                                 RAccessCertificationCase owner, RCReferenceOwner refOwner) {
        Set<RCertCaseReference> set = new HashSet<>();
        if (list == null || list.isEmpty()) {
            return set;
        }

        for (ObjectReferenceType ref : list) {
            RCertCaseReference rRef = jaxbRefToRepo(ref, prismContext, owner, refOwner);
            if (rRef != null) {
                set.add(rRef);
            }
        }
        return set;
    }

    public static RCertCaseReference jaxbRefToRepo(ObjectReferenceType reference, PrismContext prismContext,
                                                   RAccessCertificationCase owner, RCReferenceOwner refOwner) {
        if (reference == null) {
            return null;
        }
        Validate.notNull(owner, "Owner of reference must not be null.");
        Validate.notNull(refOwner, "Reference owner of reference must not be null.");
        Validate.notEmpty(reference.getOid(), "Target oid reference must not be null.");

        RCertCaseReference repoRef = new RCertCaseReference();
        repoRef.setReferenceType(refOwner);
        repoRef.setOwner(owner);
        RCertCaseReference.copyFromJAXB(reference, repoRef);
//        repoRef.setTargetName(RPolyString.toRepo(reference.asReferenceValue().getTargetName()));

        return repoRef;
    }
}
