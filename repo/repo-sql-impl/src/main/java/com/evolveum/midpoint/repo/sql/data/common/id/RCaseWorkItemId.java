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
public class RCaseWorkItemId implements Serializable {

    private String ownerOid;         // case OID
    private Integer id;

    public RCaseWorkItemId() {
    }

    public RCaseWorkItemId(String ownerOid, Integer id) {
        this.ownerOid = ownerOid;
        this.id = id;
    }

    public String getOwnerOid() {
        return ownerOid;
    }

    public void setOwnerOid(String ownerOid) {
        this.ownerOid = ownerOid;
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
        if (!(o instanceof RCaseWorkItemId))
            return false;
        RCaseWorkItemId that = (RCaseWorkItemId) o;
        return Objects.equals(ownerOid, that.ownerOid) &&
                Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ownerOid, id);
    }

    @Override
    public String toString() {
        return "RCaseWorkItemId{" +
                "oid=" + ownerOid +
                ", id=" + id +
                '}';
    }
}
