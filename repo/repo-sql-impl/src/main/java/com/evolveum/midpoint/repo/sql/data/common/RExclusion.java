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

package com.evolveum.midpoint.repo.sql.data.common;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.common.embedded.REmbeddedReference;
import com.evolveum.midpoint.repo.sql.data.common.enums.RExclusionPolicy;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ExclusionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import org.apache.commons.lang.Validate;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Type;

import javax.persistence.*;

/**
 * @author lazyman
 */
@Entity
@ForeignKey(name = "fk_exclusion")
public class RExclusion extends RContainer implements ROwnable {

    //owner
    private RObject owner;
    private String ownerOid;
    private Long ownerId;
    //exclusion
    private String description;
    private REmbeddedReference targetRef;
    private RExclusionPolicy policy;

    public RExclusion() {
        this(null);
    }

    public RExclusion(RObject owner) {
        this.owner = owner;
    }

    @ForeignKey(name = "fk_exclusion_owner")
    @MapsId("owner")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "owner_oid", referencedColumnName = "oid"),
            @JoinColumn(name = "owner_id", referencedColumnName = "id")
    })
    public RObject getOwner() {
        return owner;
    }

    @Column(name = "owner_id", nullable = false)
    public Long getOwnerId() {
        if (ownerId == null && owner != null) {
            ownerId = owner.getId();
        }
        return ownerId;
    }

    @Column(name = "owner_oid", length = 36, nullable = false)
    public String getOwnerOid() {
        if (ownerOid == null && owner != null) {
            ownerOid = owner.getOid();
        }
        return ownerOid;
    }

    @Lob
    @Type(type = RUtil.LOB_STRING_TYPE)
    public String getDescription() {
        return description;
    }

    @Enumerated(EnumType.ORDINAL)
    public RExclusionPolicy getPolicy() {
        return policy;
    }

    @Embedded
    public REmbeddedReference getTargetRef() {
        return targetRef;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPolicy(RExclusionPolicy policy) {
        this.policy = policy;
    }

    public void setTargetRef(REmbeddedReference targetRef) {
        this.targetRef = targetRef;
    }

    public void setOwner(RObject owner) {
        this.owner = owner;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public void setOwnerOid(String ownerOid) {
        this.ownerOid = ownerOid;
    }

    @Transient
    @Override
    public RContainer getContainerOwner() {
        return getOwner();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        RExclusion that = (RExclusion) o;

        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        if (policy != that.policy) return false;
        if (targetRef != null ? !targetRef.equals(that.targetRef) : that.targetRef != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (policy != null ? policy.hashCode() : 0);
        return result;
    }

    public static void copyToJAXB(RExclusion repo, ExclusionType jaxb, PrismContext prismContext) throws
            DtoTranslationException {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

        jaxb.setDescription(repo.getDescription());
        jaxb.setId(repo.getId());
        if (repo.getPolicy() != null) {
            jaxb.setPolicy(repo.getPolicy().getSchemaValue());
        }

        if (repo.getTargetRef() != null) {
            jaxb.setTargetRef(repo.getTargetRef().toJAXB(prismContext));
        }
    }

    public static void copyFromJAXB(ExclusionType jaxb, RExclusion repo, ObjectType parent, PrismContext prismContext)
            throws DtoTranslationException {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

        repo.setOid(parent.getOid());
        repo.setId(jaxb.getId());

        repo.setDescription(jaxb.getDescription());
        repo.setPolicy(RUtil.getRepoEnumValue(jaxb.getPolicy(), RExclusionPolicy.class));
        repo.setTargetRef(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getTargetRef(), prismContext));
    }

    public ExclusionType toJAXB(PrismContext prismContext) throws DtoTranslationException {
        ExclusionType object = new ExclusionType();
        RExclusion.copyToJAXB(this, object, prismContext);
        return object;
    }
}
