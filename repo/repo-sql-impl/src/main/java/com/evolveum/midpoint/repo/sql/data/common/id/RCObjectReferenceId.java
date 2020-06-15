/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.common.id;

import com.evolveum.midpoint.repo.sql.data.common.other.RCReferenceType;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class RCObjectReferenceId implements Serializable {

    private String ownerOid;
    private Integer ownerId;
    private String targetOid;
    private String relation;
    private RCReferenceType referenceType;

    public RCObjectReferenceId() {
    }

    public String getOwnerOid() {
        return ownerOid;
    }

    public void setOwnerOid(String ownerOid) {
        this.ownerOid = ownerOid;
    }

    public Integer getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Integer ownerId) {
        this.ownerId = ownerId;
    }

    public String getTargetOid() {
        return targetOid;
    }

    public void setTargetOid(String targetOid) {
        this.targetOid = targetOid;
    }

    public String getRelation() {
        return relation;
    }

    public void setRelation(String relation) {
        this.relation = relation;
    }

    public RCReferenceType getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(RCReferenceType referenceType) {
        this.referenceType = referenceType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RCObjectReferenceId that = (RCObjectReferenceId) o;

        if (ownerOid != null ? !ownerOid.equals(that.ownerOid) : that.ownerOid != null) return false;
        if (ownerId != null ? !ownerId.equals(that.ownerId) : that.ownerId != null) return false;
        if (targetOid != null ? !targetOid.equals(that.targetOid) : that.targetOid != null) return false;
        if (relation != null ? !relation.equals(that.relation) : that.relation != null) return false;
        if (referenceType != null ? !referenceType.equals(that.referenceType) : that.referenceType != null)
            return false;

        return true;
    }


    @Override
    public int hashCode() {
        int result = ownerOid != null ? ownerOid.hashCode() : 0;
        result = 31 * result + (ownerId != null ? ownerId.hashCode() : 0);
        result = 31 * result + (targetOid != null ? targetOid.hashCode() : 0);
        result = 31 * result + (relation != null ? relation.hashCode() : 0);
        result = 31 * result + (referenceType != null ? referenceType.hashCode() : 0);

        return result;
    }

    @Override
    public String toString() {
        return "RObjectReferenceId[" + ownerOid + "," + ownerId + ","
                + targetOid + "," + relation + "," + referenceType + ']';
    }
}
