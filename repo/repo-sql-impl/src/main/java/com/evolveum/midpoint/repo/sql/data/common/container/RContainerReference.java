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
import com.evolveum.midpoint.repo.sql.data.common.ObjectReference;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.RObjectReference;
import com.evolveum.midpoint.repo.sql.data.common.other.RCReferenceOwner;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sql.query2.definition.NotQueryable;
import com.evolveum.midpoint.repo.sql.util.ClassMapper;
import com.evolveum.midpoint.repo.sql.util.EntityState;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import org.apache.commons.lang.Validate;

import javax.persistence.Transient;
import java.util.Objects;

/**
 * @author lazyman
 * @author mederly
 *
 * This is a reference that is contained in (any) container. Its owner is identified by OID, container value ID,
 * and owner type.
 *
 * It is created as a superclass for both RAssignmentReference and RCertCaseReference (now non-existent) because they share
 * almost all the code.
 *
 */
public abstract class RContainerReference extends RReference implements ObjectReference, EntityState {

    public static final String REFERENCE_TYPE = "reference_type";

    public static final String F_OWNER = "owner";

    private Boolean trans;

    private RCReferenceOwner referenceType;

    //owner
    private String ownerOid;
    private Integer ownerId;

    public RContainerReference() {
    }

    @NotQueryable
    public abstract Container getOwner();

    @NotQueryable
    protected String getOwnerOid() {
        if (ownerOid == null && getOwner() != null) {
            ownerOid = getOwner().getOwnerOid();
        }
        return ownerOid;
    }

    @NotQueryable
    protected Integer getOwnerId() {
        if (ownerId == null && getOwner() != null) {
            ownerId = getOwner().getId();
        }
        return ownerId;
    }

    public RObject getTarget() {        // for HQL use only
        return null;
    }

    protected RCReferenceOwner getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(RCReferenceOwner referenceType) { this.referenceType = referenceType; }

    public void setOwnerOid(String ownerOid) {
        this.ownerOid = ownerOid;
    }

    public void setOwnerId(Integer ownerId) {
        this.ownerId = ownerId;
    }

    @Transient
    public Boolean isTransient() {
        return trans;
    }

    @Override
    public void setTransient(Boolean trans) {
        this.trans = trans;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof RContainerReference))
            return false;
        if (!super.equals(o))
            return false;
        RContainerReference that = (RContainerReference) o;
        return referenceType == that.referenceType;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    public static void copyToJAXB(RContainerReference repo, ObjectReferenceType jaxb, PrismContext prismContext) {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

        jaxb.setType(ClassMapper.getQNameForHQLType(repo.getType()));
        jaxb.setOid(repo.getTargetOid());
        jaxb.setRelation(RUtil.stringToQName(repo.getRelation()));
    }

    public static void copyFromJAXB(ObjectReferenceType jaxb, RContainerReference repo) {
        RObjectReference.copyFromJAXB(jaxb, repo);
    }

    public ObjectReferenceType toJAXB(PrismContext prismContext) {
        ObjectReferenceType ref = new ObjectReferenceType();
        copyToJAXB(this, ref, prismContext);

        return ref;
    }
}
