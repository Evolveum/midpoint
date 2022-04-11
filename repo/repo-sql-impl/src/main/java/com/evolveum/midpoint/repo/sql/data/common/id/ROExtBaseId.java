/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.common.id;

import com.evolveum.midpoint.repo.sql.data.common.any.ROExtBase;
import com.evolveum.midpoint.repo.sql.data.common.type.RObjectExtensionType;

import java.io.Serializable;
import java.util.Objects;

public class ROExtBaseId implements Serializable {

    protected String ownerOid;
    protected RObjectExtensionType ownerType;
    protected Integer itemId;

    void fillInFromValue(ROExtBase value) {
        ownerOid = value.getOwnerOid();
        ownerType = value.getOwnerType();
        itemId = value.getItemId();
    }

    public String getOwnerOid() {
        return ownerOid;
    }

    public void setOwnerOid(String ownerOid) {
        this.ownerOid = ownerOid;
    }

    public RObjectExtensionType getOwnerType() {
        return ownerType;
    }

    public void setOwnerType(RObjectExtensionType ownerType) {
        this.ownerType = ownerType;
    }

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ROExtBaseId))
            return false;
        ROExtBaseId that = (ROExtBaseId) o;
        return itemId.equals(that.itemId) &&
                Objects.equals(ownerOid, that.ownerOid) &&
                ownerType == that.ownerType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ownerOid, ownerType, itemId);
    }
}
