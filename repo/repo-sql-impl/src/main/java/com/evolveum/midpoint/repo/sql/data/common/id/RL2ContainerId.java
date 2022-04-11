/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.common.id;

import java.io.Serializable;

/**
 * ID for second-level container (container within container within object).
 */
public class RL2ContainerId implements Serializable {

    private String ownerOwnerOid;
    private Integer ownerId;
    private Integer id;

    public RL2ContainerId() {
    }

    public RL2ContainerId(String ownerOwnerOid, Integer ownerId, Integer id) {
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
        if (this == o) return true;
        if (!(o instanceof RL2ContainerId)) return false;

        RL2ContainerId that = (RL2ContainerId) o;

        if (ownerOwnerOid != null ? !ownerOwnerOid.equals(that.ownerOwnerOid) : that.ownerOwnerOid != null)
            return false;
        if (ownerId != null ? !ownerId.equals(that.ownerId) : that.ownerId != null) return false;
        return !(id != null ? !id.equals(that.id) : that.id != null);

    }

    @Override
    public int hashCode() {
        int result = ownerOwnerOid != null ? ownerOwnerOid.hashCode() : 0;
        result = 31 * result + (ownerId != null ? ownerId.hashCode() : 0);
        result = 31 * result + (id != null ? id.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "RL2ContainerId{" +
                "ownerOwnerOid='" + ownerOwnerOid + '\'' +
                ", ownerId=" + ownerId +
                ", id=" + id +
                '}';
    }
}
