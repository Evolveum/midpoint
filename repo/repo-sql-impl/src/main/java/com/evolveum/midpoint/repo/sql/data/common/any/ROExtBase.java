/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.common.any;

import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.id.ROExtBaseId;
import com.evolveum.midpoint.repo.sql.data.common.type.RObjectExtensionType;

import java.util.Objects;

abstract public class ROExtBase<T> extends RAnyBase<T> implements ROExtValue<T> {

    private Boolean trans;

    //owner entity
    private RObject owner;
    private String ownerOid;
    private RObjectExtensionType ownerType;

    public RObject getOwner() {
        return owner;
    }

    public String getOwnerOid() {
        if (ownerOid == null && owner != null) {
            ownerOid = owner.getOid();
        }
        return ownerOid;
    }

    public RObjectExtensionType getOwnerType() {
        return ownerType;
    }

    public void setOwner(RObject owner) {
        this.owner = owner;
    }

    public void setOwnerOid(String ownerOid) {
        this.ownerOid = ownerOid;
    }

    public void setOwnerType(RObjectExtensionType ownerType) {
        this.ownerType = ownerType;
    }

    @Override
    public Boolean isTransient() {
        return trans;
    }

    @Override
    public void setTransient(Boolean trans) {
        this.trans = trans;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ROExtBase))
            return false;
        if (!super.equals(o))
            return false;
        ROExtBase roExtBase = (ROExtBase) o;
        return Objects.equals(getOwnerOid(), roExtBase.getOwnerOid()) && getOwnerType() == roExtBase.getOwnerType();
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getOwnerOid(), getOwnerType());
    }

    public abstract ROExtBaseId createId();
}
