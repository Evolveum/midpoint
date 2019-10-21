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
public class RCertWorkItemId implements Serializable {

    private String ownerOwnerOid;
    private Integer ownerId;
    private Integer id;

    public RCertWorkItemId() {
    }

    public RCertWorkItemId(String ownerOwnerOid, Integer ownerId, Integer id) {
        this.ownerOwnerOid = ownerOwnerOid;
        this.ownerId = ownerId;
        this.id = id;
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

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof RCertWorkItemId))
            return false;
        RCertWorkItemId that = (RCertWorkItemId) o;
        return Objects.equals(ownerOwnerOid, that.ownerOwnerOid) &&
                Objects.equals(ownerId, that.ownerId) &&
                Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ownerOwnerOid, ownerId, id);
    }

    @Override
    public String toString() {
        return "RCertWorkItemId{" +
                "ownerOwnerOid='" + ownerOwnerOid + '\'' +
                ", ownerId=" + ownerId +
                ", id=" + id +
                '}';
    }
}
