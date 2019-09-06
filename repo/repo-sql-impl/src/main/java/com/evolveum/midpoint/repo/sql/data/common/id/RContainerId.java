/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.common.id;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class RContainerId implements Serializable {

    private String ownerOid;
    private Integer id;

    public RContainerId() {
    }

    public RContainerId(String oid) {
        this(0, oid);
    }

    public RContainerId(Integer id, String oid) {
        this.id = id;
        this.ownerOid = oid;
    }

    public Integer getId() {
        return id;
    }

    public String getOwnerOid() {
        return ownerOid;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setOwnerOid(String oid) {
        this.ownerOid = oid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RContainerId that = (RContainerId) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (ownerOid != null ? !ownerOid.equals(that.ownerOid) : that.ownerOid != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = ownerOid != null ? ownerOid.hashCode() : 0;
        result = 31 * result + (id != null ? id.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "RContainerId{" + ownerOid + ", " + id + "}";
    }
}
