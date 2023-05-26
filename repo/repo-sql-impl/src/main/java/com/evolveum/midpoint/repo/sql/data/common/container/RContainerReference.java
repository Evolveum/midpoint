/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.common.container;

import java.util.Objects;
import jakarta.persistence.Transient;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.other.RCReferenceType;
import com.evolveum.midpoint.repo.sql.query.definition.NotQueryable;
import com.evolveum.midpoint.repo.sql.util.ClassMapper;
import com.evolveum.midpoint.repo.sql.util.EntityState;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * @author lazyman
 * <p>
 * This is a reference that is contained in (any) container. Its owner is identified by OID, container value ID,
 * and owner type.
 * <p>
 * It is created as a superclass for both RAssignmentReference and RCertCaseReference (now non-existent) because they share
 * almost all the code.
 */
public abstract class RContainerReference extends RReference implements EntityState {

    public static final String REFERENCE_TYPE = "reference_type";

    public static final String F_OWNER = "owner";

    private Boolean trans;

    private RCReferenceType referenceType;

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

    protected RCReferenceType getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(RCReferenceType referenceType) {
        this.referenceType = referenceType;
    }

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
        if (this == o) {
            return true;
        }
        if (!(o instanceof RContainerReference)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        RContainerReference that = (RContainerReference) o;
        // TODO: what about ownerOid and ownerId?
        return referenceType == that.referenceType;
    }

    @Override
    public int hashCode() {
        // TODO: before june 2020 super was used, but this didn't match equals
//        return super.hashCode();
        return Objects.hash(referenceType);
    }

    public static void copyToJAXB(RContainerReference repo, ObjectReferenceType jaxb, PrismContext prismContext) {
        Objects.requireNonNull(repo, "Repo object must not be null.");
        Objects.requireNonNull(jaxb, "JAXB object must not be null.");

        jaxb.setType(ClassMapper.getQNameForHQLType(repo.getTargetType()));
        jaxb.setOid(repo.getTargetOid());
        jaxb.setRelation(RUtil.stringToQName(repo.getRelation()));
    }

    public ObjectReferenceType toJAXB(PrismContext prismContext) {
        ObjectReferenceType ref = new ObjectReferenceType();
        copyToJAXB(this, ref, prismContext);

        return ref;
    }
}
