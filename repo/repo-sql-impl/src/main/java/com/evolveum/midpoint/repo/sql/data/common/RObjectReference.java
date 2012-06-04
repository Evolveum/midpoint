/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.sql.data.common;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectReferenceType;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.hibernate.annotations.ForeignKey;

import javax.persistence.*;
import java.io.Serializable;

/**
 * @author lazyman
 */
@Entity
@IdClass(RObjectReferenceId.class)
@Table(name = "m_reference")
public class RObjectReference implements Serializable {

    //owner
    private RContainer owner;
    private String ownerOid;
    private Long ownerId;
    //reference
    private String targetOid;
    private REmbeddedReference reference = new REmbeddedReference();

    @ForeignKey(name = "fk_reference_owner")
    @MapsId("owner")
    @ManyToOne(fetch = FetchType.LAZY)
    @PrimaryKeyJoinColumns({
            @PrimaryKeyJoinColumn(name = "owner_oid", referencedColumnName = "oid"),
            @PrimaryKeyJoinColumn(name = "owner_id", referencedColumnName = "id")
    })
    public RContainer getOwner() {
        return owner;
    }

    @Id
    @Column(name = "owner_id")
    public Long getOwnerId() {
        if (ownerId == null && owner != null) {
            ownerId = owner.getId();
        }
        return ownerId;
    }

    @Id
    @Column(name = "owner_oid", length = 36)
    public String getOwnerOid() {
        if (ownerOid == null && owner != null) {
            ownerOid = owner.getOid();
        }
        return ownerOid;
    }

    @Id
    @Column(name = "targetOid", length = 36)
    public String getTargetOid() {
        if (targetOid == null) {
            targetOid = getReference().getTargetOid();
        }
        if (targetOid == null) {
            targetOid = "";
        }
        return targetOid;
    }

    @AttributeOverrides({
            @AttributeOverride(name = "type", column = @Column(name = "type")),
            @AttributeOverride(name = "targetOid", column = @Column(name = "targetOid",
                    insertable = false, updatable = false)),
            @AttributeOverride(name = "description", column = @Column(name = "description")),
            @AttributeOverride(name = "filter", column = @Column(name = "filter"))
    })
    @Embedded
    public REmbeddedReference getReference() {
        return reference;
    }

    public void setTargetOid(String targetOid) {
        getReference().setTargetOid(targetOid);
        this.targetOid = targetOid;
    }

    public void setReference(REmbeddedReference reference) {
        Validate.notNull(reference, "Embedded reference must not be null.");
        this.reference = reference;
        this.targetOid = reference.getTargetOid();
    }

    public void setOwner(RContainer owner) {
        this.owner = owner;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public void setOwnerOid(String ownerOid) {
        this.ownerOid = ownerOid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RObjectReference that = (RObjectReference) o;

        if (reference != null ? !reference.equals(that.reference) : that.reference != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return reference != null ? reference.hashCode() : 0;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

    public static void copyToJAXB(RObjectReference repo, ObjectReferenceType jaxb, PrismContext prismContext) {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

        REmbeddedReference.copyToJAXB(repo.getReference(), jaxb, prismContext);
    }

    public static void copyFromJAXB(ObjectReferenceType jaxb, RObjectReference repo, PrismContext prismContext) {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

        REmbeddedReference ref = new REmbeddedReference();
        REmbeddedReference.copyFromJAXB(jaxb, ref, prismContext);
        repo.setReference(ref);
    }

    public ObjectReferenceType toJAXB(PrismContext prismContext) {
        ObjectReferenceType ref = new ObjectReferenceType();
        copyToJAXB(this, ref, prismContext);

        return ref;
    }
}
