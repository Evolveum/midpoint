/*
 * Copyright (c) 2010-2015 Evolveum
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
import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.embedded.REmbeddedReference;
import com.evolveum.midpoint.repo.sql.data.common.enums.RExclusionPolicy;
import com.evolveum.midpoint.repo.sql.data.common.id.RContainerId;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.midpoint.repo.sql.query.definition.OwnerIdGetter;
import com.evolveum.midpoint.repo.sql.query2.definition.IdQueryProperty;
import com.evolveum.midpoint.repo.sql.query2.definition.NotQueryable;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.IdGeneratorResult;
import com.evolveum.midpoint.repo.sql.util.MidPointSingleTablePersister;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExclusionPolicyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.commons.lang.Validate;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Persister;

import javax.persistence.*;

/**
 * @author lazyman
 * 
 * DEPRECATED. This does not need to be stored in the database any more.
 * 
 */
@JaxbType(type = ExclusionPolicyConstraintType.class)
@Entity
@IdClass(RContainerId.class)
@ForeignKey(name = "fk_exclusion")
@Deprecated
@Persister(impl = MidPointSingleTablePersister.class)
public class RExclusion implements Container {

    public static final String F_OWNER = "owner";

    private Boolean trans;

    //owner
    private RObject owner;
    private String ownerOid;
    private Integer id;

    //exclusion
    private REmbeddedReference targetRef;
    private RExclusionPolicy policy;

    public RExclusion() {
        this(null);
    }

    public RExclusion(RObject owner) {
        setOwner(owner);
    }

    @Id
    @ForeignKey(name = "fk_exclusion_owner")
    @MapsId("owner")
    @ManyToOne(fetch = FetchType.LAZY)
    @NotQueryable
    public RObject getOwner() {
        return owner;
    }

    @Column(name = "owner_oid", length = RUtil.COLUMN_LENGTH_OID, nullable = false)
    @OwnerIdGetter()
    public String getOwnerOid() {
        if (owner != null && ownerOid == null) {
            ownerOid = owner.getOid();
        }
        return ownerOid;
    }

    @Id
    @GeneratedValue(generator = "ContainerIdGenerator")
    @GenericGenerator(name = "ContainerIdGenerator", strategy = "com.evolveum.midpoint.repo.sql.util.ContainerIdGenerator")
    @Column(name = "id")
    @IdQueryProperty
    public Integer getId() {
        return id;
    }

    @Enumerated(EnumType.ORDINAL)
    public RExclusionPolicy getPolicy() {
        return policy;
    }

    @Embedded
    public REmbeddedReference getTargetRef() {
        return targetRef;
    }

    @Transient
    @Override
    public Boolean isTransient() {
        return trans;
    }

    @Override
    public void setTransient(Boolean trans) {
        this.trans = trans;
    }

    public void setOwner(RObject owner) {
        this.owner = owner;
    }

    public void setOwnerOid(String ownerOid) {
        this.ownerOid = ownerOid;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setPolicy(RExclusionPolicy policy) {
        this.policy = policy;
    }

    public void setTargetRef(REmbeddedReference targetRef) {
        this.targetRef = targetRef;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        RExclusion that = (RExclusion) o;

        if (policy != that.policy) return false;
        if (targetRef != null ? !targetRef.equals(that.targetRef) : that.targetRef != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (policy != null ? policy.hashCode() : 0);
        return result;
    }

    public static void copyToJAXB(RExclusion repo, ExclusionPolicyConstraintType jaxb, PrismContext prismContext) throws
            DtoTranslationException {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

        jaxb.setId(RUtil.toLong(repo.getId()));
        if (repo.getPolicy() != null) {
            jaxb.setPolicy(repo.getPolicy().getSchemaValue());
        }

        if (repo.getTargetRef() != null) {
            jaxb.setTargetRef(repo.getTargetRef().toJAXB(prismContext));
        }
    }

    public static void copyFromJAXB(ExclusionPolicyConstraintType jaxb, RExclusion repo, ObjectType parent,
            RepositoryContext repositoryContext, IdGeneratorResult generatorResult) throws DtoTranslationException {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

        repo.setTransient(generatorResult.isTransient(jaxb.asPrismContainerValue()));
        repo.setOwnerOid(parent.getOid());
        repo.setId(RUtil.toInteger(jaxb.getId()));

        repo.setPolicy(RUtil.getRepoEnumValue(jaxb.getPolicy(), RExclusionPolicy.class));
        repo.setTargetRef(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getTargetRef(), repositoryContext.prismContext));
    }

    public ExclusionPolicyConstraintType toJAXB(PrismContext prismContext) throws DtoTranslationException {
        ExclusionPolicyConstraintType object = new ExclusionPolicyConstraintType();
        RExclusion.copyToJAXB(this, object, prismContext);
        return object;
    }
}
