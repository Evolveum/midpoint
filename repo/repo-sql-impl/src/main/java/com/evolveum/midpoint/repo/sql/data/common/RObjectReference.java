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
import org.hibernate.annotations.*;
import org.hibernate.annotations.CascadeType;
import org.w3c.dom.Element;

import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * @author lazyman
 */
@Entity
@IdClass(RObjectReferenceId.class)
@Table(name = "reference")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "reference_type", discriminatorType = DiscriminatorType.INTEGER)
@DiscriminatorValue(value = "0")
public class RObjectReference implements Serializable {

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

    @ForeignKey(name = "none")
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
    @Column(name = "target_id", insertable = true,updatable = true,nullable = false)
    public Long getTargetId() {
        if (targetId == null && target != null) {
            targetId = target.getId();
        }
        if (targetId == null) {
            targetId = 0L;
        }
        return targetId;
    }

    @Id
    @Column(name = "target_oid", length = 36, insertable = true,updatable = true,nullable = false)
    public String getTargetOid() {
        if (targetOid == null && target != null) {
            targetOid = target.getOid();
        }
        if (targetOid == null) {
            targetOid = "";
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
//        if (target == null) {
//            return;
//        }
//        setTargetId(target.getId());
//        setTargetOid(target.getOid());
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RObjectReference that = (RObjectReference) o;

        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        if (filter != null ? !filter.equals(that.filter) : that.filter != null) return false;
        if (getTargetId() != null ? !getTargetId().equals(that.getTargetId()) : that.getTargetId() != null) return false;
        if (getTargetOid() != null ? !getTargetOid().equals(that.getTargetOid()) : that.getTargetOid() != null) return false;
        if (type != that.type) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = description != null ? description.hashCode() : 0;
        result = 31 * result + (filter != null ? filter.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }

    public static void copyToJAXB(RObjectReference repo, ObjectReferenceType jaxb, PrismContext prismContext) {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

        jaxb.setDescription(repo.getDescription());
        jaxb.setType(ClassMapper.getQNameForHQLType(repo.getType()));
        if (StringUtils.isNotEmpty(repo.getTargetOid())) {
            jaxb.setOid(repo.getTargetOid());
        }

        String filter = repo.getFilter();
        if (StringUtils.isNotEmpty(filter)) {
            Element element = DOMUtil.parseDocument(filter).getDocumentElement();
            ObjectReferenceType.Filter jaxbFilter = new ObjectReferenceType.Filter();
            jaxbFilter.setFilter(element);
            jaxb.setFilter(jaxbFilter);
        }
    }

    public static void copyFromJAXB(ObjectReferenceType jaxb, RObjectReference repo, PrismContext prismContext) {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

        repo.setDescription(jaxb.getDescription());
        repo.setType(ClassMapper.getHQLTypeForQName(jaxb.getType()));

        repo.setTargetId(0L);
        repo.setTargetOid(jaxb.getOid());

        if (jaxb.getFilter() != null) {
            ObjectReferenceType.Filter filter = jaxb.getFilter();
            repo.setFilter(DOMUtil.printDom(filter.getFilter()).toString());
        }
    }

    public ObjectReferenceType toJAXB(PrismContext prismContext) {
        ObjectReferenceType ref = new ObjectReferenceType();
        copyToJAXB(this, ref, prismContext);

        return ref;
    }
}
