/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.common.id;

import java.io.Serializable;
import java.util.Objects;

public class RContainerId implements Serializable {

    private String ownerOid;
    private Integer id;

    @SuppressWarnings("unused")
    public RContainerId() {
    }

    public RContainerId(Integer id, String oid) {
        this.id = id;
        this.ownerOid = oid;
    }

    public String getOwnerOid() {
        return ownerOid;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setOwnerOid(String oid) {
        this.ownerOid = oid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RContainerId)) {
            return false;
        }

        RContainerId that = (RContainerId) o;
        return Objects.equals(ownerOid, that.ownerOid)
                && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ownerOid, id);
    }

    @Override
    public String toString() {
        return "RContainerId{" + ownerOid + ", " + id + "}";
    }
}
