/*
 * Copyright (c) 2010-2013 Evolveum
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
import com.evolveum.midpoint.repo.sql.data.common.ObjectReference;
import com.evolveum.midpoint.repo.sql.data.common.RObjectReference;
import com.evolveum.midpoint.repo.sql.data.common.id.RCObjectReferenceId;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.midpoint.repo.sql.util.ClassMapper;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import org.apache.commons.lang.Validate;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;

import javax.persistence.*;

/**
 * @author lazyman
 */
@JaxbType(type = ObjectReferenceType.class)
@Entity
@IdClass(RCObjectReferenceId.class)
@Table(name = "m_assignment_reference")
@org.hibernate.annotations.Table(appliesTo = "m_assignment_reference",
        indexes = {@Index(name = "iAssignmentReferenceTargetOid", columnNames = "targetOid")})
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = RObjectReference.REFERENCE_TYPE, discriminatorType = DiscriminatorType.INTEGER)
public class RAssignmentReference implements ObjectReference {

    public static final String REFERENCE_TYPE = "reference_type";

    public static final String F_OWNER = "owner";

    //owner
    private RAssignment owner;
    private String ownerOid;
    private Short ownerId;
    //other primary key fields
    private String targetOid;
    private String relation;
    private RObjectType type;

    public RAssignmentReference() {
    }

    @ForeignKey(name = "fk_a_reference")
    @MapsId("owner")
    @ManyToOne(fetch = FetchType.LAZY)
    public RAssignment getOwner() {
        return owner;
    }

    @Id
    @Column(name = "owner_oid", length = RUtil.COLUMN_LENGTH_OID)
    public String getOwnerOid() {
        if (ownerOid == null && owner != null) {
            ownerOid = owner.getOwnerOid();
        }
        return ownerOid;
    }


    @Id
    @Column(name = "owner_id")
    public Short getOwnerId() {
        if (ownerId == null && owner != null) {
            ownerId = owner.getId();
        }
        return ownerId;
    }

    @Id
    @Column(name = "targetOid", length = RUtil.COLUMN_LENGTH_OID)
    @Override
    public String getTargetOid() {
        return targetOid;
    }

    @Id
    @Column(length = RUtil.COLUMN_LENGTH_QNAME)
    public String getRelation() {
        return relation;
    }

    /**
     * Represents {@link javax.xml.namespace.QName} type attribute in reference e.g.
     * {@link com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType} represented
     * as enum {@link com.evolveum.midpoint.repo.sql.data.common.other.RObjectType#USER}
     *
     * @return null if not defined, otherwise value from {@link com.evolveum.midpoint.repo.sql.data.common.other.RObjectType} enum
     */
    @Column(name = "containerType")
    @Enumerated(EnumType.ORDINAL)
    @Override
    public RObjectType getType() {
        return type;
    }

    public void setOwner(RAssignment owner) {
        this.owner = owner;
    }

    public void setOwnerOid(String ownerOid) {
        this.ownerOid = ownerOid;
    }

    public void setOwnerId(Short ownerId) {
        this.ownerId = ownerId;
    }

    public void setRelation(String relation) {
        this.relation = relation;
    }

    public void setTargetOid(String targetOid) {
        this.targetOid = targetOid;
    }

    public void setType(RObjectType type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RAssignmentReference ref = (RAssignmentReference) o;

        if (targetOid != null ? !targetOid.equals(ref.targetOid) : ref.targetOid != null) return false;
        if (type != ref.type) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = targetOid != null ? targetOid.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (relation != null ? relation.hashCode() : 0);

        return result;
    }

    public static void copyToJAXB(RAssignmentReference repo, ObjectReferenceType jaxb, PrismContext prismContext) {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

        jaxb.setType(ClassMapper.getQNameForHQLType(repo.getType()));
        jaxb.setOid(repo.getTargetOid());
        jaxb.setRelation(RUtil.stringToQName(repo.getRelation()));
    }

    public static void copyFromJAXB(ObjectReferenceType jaxb, RAssignmentReference repo, PrismContext prismContext) {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");
        Validate.notEmpty(jaxb.getOid(), "Target oid must not be null.");

        repo.setType(ClassMapper.getHQLTypeForQName(jaxb.getType()));
        repo.setRelation(RUtil.qnameToString(jaxb.getRelation()));

        repo.setTargetOid(jaxb.getOid());
    }

    public ObjectReferenceType toJAXB(PrismContext prismContext) {
        ObjectReferenceType ref = new ObjectReferenceType();
        copyToJAXB(this, ref, prismContext);

        return ref;
    }
}
