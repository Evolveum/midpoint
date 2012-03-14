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
import com.evolveum.midpoint.repo.sql.ClassMapper;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Type;
import org.w3c.dom.Element;

import javax.persistence.*;
import java.io.Serializable;

/**
 * @author lazyman
 */
@Entity
@Table(name = "reference")
public class RObjectReferenceType implements Serializable {

    //owner
    private RContainer owner;
    private String ownerOid;
    private Long ownerId;
    //target
    private RContainer target;
    private String targetOid;
    private Long targetId;
    //other fields
    private String description;
    private String filter;
    private RContainerType type;

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

    @ForeignKey(name = "none")  //todo disabled target oid FK check
//    @ForeignKey(name = "fk_reference_target")
    @MapsId("target")
    @ManyToOne(fetch = FetchType.LAZY)
    @PrimaryKeyJoinColumns({
            @PrimaryKeyJoinColumn(name = "target_oid", referencedColumnName = "oid"),
            @PrimaryKeyJoinColumn(name = "target_id", referencedColumnName = "id")
    })
    public RContainer getTarget() {
        return target;
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
    @Column(name = "target_id")
    public Long getTargetId() {
        if (targetId == null && target != null) {
            targetId = target.getId();
        }
        return targetId;
    }

    @Id
    @Column(name = "target_oid", length = 36)
    public String getTargetOid() {
        if (targetOid == null && target != null) {
            targetOid = target.getOid();
        }
        return targetOid;
    }

    @Type(type = "org.hibernate.type.TextType")
    public String getDescription() {
        return description;
    }

    @Enumerated(EnumType.ORDINAL)
    public RContainerType getType() {
        return type;
    }

    public String getFilter() {
        return filter;
    }

    public void setType(RContainerType type) {
        this.type = type;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public void setTarget(RContainer target) {
        this.target = target;
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

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public void setTargetOid(String targetOid) {
        this.targetOid = targetOid;
    }

    //todo hash and equals
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RObjectReferenceType reference = (RObjectReferenceType) o;

//        if (ownerId != null ? !ownerId.equals(reference.ownerId) : reference.ownerId != null) return false;
//        if (ownerOid != null ? !ownerOid.equals(reference.ownerOid) : reference.ownerOid != null) return false;
//        if (targetId != null ? !targetId.equals(reference.targetId) : reference.targetId != null) return false;
//        if (targetOid != null ? !targetOid.equals(reference.targetOid) : reference.targetOid != null) return false;
//
//        return true;
        return false;
    }

    @Override
    public int hashCode() {
//        int result = ownerOid != null ? ownerOid.hashCode() : 0;
//        result = 31 * result + (ownerId != null ? ownerId.hashCode() : 0);
//        result = 31 * result + (targetOid != null ? targetOid.hashCode() : 0);
//        result = 31 * result + (targetId != null ? targetId.hashCode() : 0);
//        return result;
        return 31;
    }

    public static void copyToJAXB(RObjectReferenceType repo, ObjectReferenceType jaxb, PrismContext prismContext) {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

        jaxb.setDescription(repo.getDescription());
        jaxb.setType(ClassMapper.getQNameForHQLType(repo.getType()));
        jaxb.setOid(repo.getTargetOid());

        String filter = repo.getFilter();
        if (StringUtils.isNotEmpty(filter)) {
            Element element = DOMUtil.parseDocument(filter).getDocumentElement();
            jaxb.setFilter(element);
        }
    }

    public static void copyFromJAXB(ObjectReferenceType jaxb, RObjectReferenceType repo, PrismContext prismContext) {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

        repo.setDescription(jaxb.getDescription());
        repo.setType(ClassMapper.getHQLTypeForQName(jaxb.getType()));

        repo.setTargetId(0L);
        repo.setTargetOid(jaxb.getOid());

        if (jaxb.getFilter() != null) {
            repo.setFilter(DOMUtil.printDom(jaxb.getFilter()).toString());
        }
    }

    public ObjectReferenceType toJAXB(PrismContext prismContext) {
        ObjectReferenceType ref = new ObjectReferenceType();
        copyToJAXB(this, ref, prismContext);

        return ref;
    }
}
