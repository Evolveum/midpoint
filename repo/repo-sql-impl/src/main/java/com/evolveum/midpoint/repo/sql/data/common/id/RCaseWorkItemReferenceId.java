/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.common.id;

import com.evolveum.midpoint.repo.sql.data.common.other.RCaseWorkItemReferenceOwner;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author lazyman
 */
public class RCaseWorkItemReferenceId implements Serializable {

    private String ownerOwnerOid;
    private Integer ownerId;
    private String targetOid;
    private String relation;
    private RCaseWorkItemReferenceOwner referenceType;

    public RCaseWorkItemReferenceId() {
    }

    public String getOwnerOwnerOid() {
        return ownerOwnerOid;
    }

    public void setOwnerOwnerOid(String ownerOwnerOid) {
        this.ownerOwnerOid = ownerOwnerOid;
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

    public RCaseWorkItemReferenceOwner getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(RCaseWorkItemReferenceOwner referenceType) {
        this.referenceType = referenceType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof RCaseWorkItemReferenceId))
            return false;
        RCaseWorkItemReferenceId that = (RCaseWorkItemReferenceId) o;
        return Objects.equals(ownerOwnerOid, that.ownerOwnerOid) &&
                Objects.equals(ownerId, that.ownerId) &&
                Objects.equals(targetOid, that.targetOid) &&
                Objects.equals(relation, that.relation) &&
                Objects.equals(referenceType, that.referenceType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ownerOwnerOid, ownerId, targetOid, relation, referenceType);
    }

    @Override
    public String toString() {
        return "RCertWorkItemReferenceId{" +
                "ownerOwnerOid=" + ownerOwnerOid +
                ", ownerId=" + ownerId +
                ", targetOid='" + targetOid + '\'' +
                ", relation='" + relation + '\'' +
                ", referenceType='" + referenceType + '\'' +
                '}';
    }
}
