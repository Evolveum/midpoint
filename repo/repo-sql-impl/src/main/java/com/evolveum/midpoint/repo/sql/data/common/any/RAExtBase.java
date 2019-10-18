/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.common.any;

import com.evolveum.midpoint.repo.sql.data.common.id.RAExtBaseId;
import com.evolveum.midpoint.repo.sql.data.common.type.RAssignmentExtensionType;

import java.util.Objects;

abstract public class RAExtBase<T> extends RAnyBase<T> implements RAExtValue<T> {

    private Boolean trans;

    //owner entity
    private RAssignmentExtension anyContainer;
    private String ownerOid;
    private Integer ownerId;

    private RAssignmentExtensionType extensionType;

    public String getOwnerOid() {
        if (ownerOid == null && anyContainer != null) {
            ownerOid = anyContainer.getOwnerOid();
        }
        return ownerOid;
    }

    public void setOwnerOid(String ownerOid) {
        this.ownerOid = ownerOid;
    }

    public Integer getOwnerId() {
        if (ownerId == null && anyContainer != null) {
            ownerId = anyContainer.getOwnerId();
        }
        return ownerId;
    }

    public void setOwnerId(Integer ownerId) {
        this.ownerId = ownerId;
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
    public RAssignmentExtension getAnyContainer() {
        return anyContainer;
    }

//    @Transient
//    public String getAssignmentOwnerOid() {
//        return anyContainer != null ? anyContainer.getAssignmentOwnerOid() : null;
//    }

//    @Transient
//    public Integer getAssignmentId() {
//        return anyContainer != null ? anyContainer.getAssignmentId() : null;
//    }

    @Override
    public void setAnyContainer(RAssignmentExtension anyContainer) {
        this.anyContainer = anyContainer;
    }

    @Override
    public RAssignmentExtensionType getExtensionType() {
        return extensionType;
    }

    @Override
    public void setExtensionType(RAssignmentExtensionType extensionType) {
        this.extensionType = extensionType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof RAExtBase))
            return false;
        if (!super.equals(o))
            return false;
        RAExtBase<?> raExtBase = (RAExtBase<?>) o;
        return Objects.equals(getOwnerOid(), raExtBase.getOwnerOid()) &&
                Objects.equals(getOwnerId(), raExtBase.getOwnerId());
                /*getExtensionType() == raExtBase.getExtensionType() */
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getOwnerOid(), getOwnerId());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "item id=" + getItemId() +
                ", value=" + getValue() +
                '}';
    }

    public abstract RAExtBaseId createId();
}
