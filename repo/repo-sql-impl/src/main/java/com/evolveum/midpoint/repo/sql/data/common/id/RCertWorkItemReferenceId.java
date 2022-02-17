/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.common.id;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author lazyman
 */
public class RCertWorkItemReferenceId implements Serializable {

    private String ownerOwnerOwnerOid;
    private Integer ownerOwnerId;
    private Integer ownerId;
    private String targetOid;
    private String relation;

    public RCertWorkItemReferenceId() {
    }

    public String getOwnerOwnerOwnerOid() {
        return ownerOwnerOwnerOid;
    }

    public void setOwnerOwnerOwnerOid(String ownerOwnerOwnerOid) {
        this.ownerOwnerOwnerOid = ownerOwnerOwnerOid;
    }

    public Integer getOwnerOwnerId() {
        return ownerOwnerId;
    }

    public void setOwnerOwnerId(Integer ownerOwnerId) {
        this.ownerOwnerId = ownerOwnerId;
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

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof RCertWorkItemReferenceId))
            return false;
        RCertWorkItemReferenceId that = (RCertWorkItemReferenceId) o;
        return Objects.equals(ownerOwnerOwnerOid, that.ownerOwnerOwnerOid) &&
                Objects.equals(ownerOwnerId, that.ownerOwnerId) &&
                Objects.equals(ownerId, that.ownerId) &&
                Objects.equals(targetOid, that.targetOid) &&
                Objects.equals(relation, that.relation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ownerOwnerOwnerOid, ownerOwnerId, ownerId, targetOid, relation);
    }

    @Override
    public String toString() {
        return "RCertWorkItemReferenceId{" +
                "ownerOwnerOwnerOid='" + ownerOwnerOwnerOid + '\'' +
                ", ownerOwnerId=" + ownerOwnerId +
                ", ownerId=" + ownerId +
                ", targetOid='" + targetOid + '\'' +
                ", relation='" + relation + '\'' +
                '}';
    }
}
