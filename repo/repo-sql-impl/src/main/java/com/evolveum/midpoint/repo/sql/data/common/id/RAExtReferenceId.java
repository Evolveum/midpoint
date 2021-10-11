/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.common.id;

import com.evolveum.midpoint.repo.sql.data.common.any.RAExtReference;

import java.util.Objects;

/**
 * @author lazyman
 */
public class RAExtReferenceId extends RAExtBaseId {

    private String value;

    @Override
    public String getOwnerOid() {
        return super.getOwnerOid();
    }

    @Override
    public Integer getOwnerId() {
        return super.getOwnerId();
    }

    @Override
    public Integer getItemId() {
        return super.getItemId();
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RAExtReferenceId))
            return false;
        if (!super.equals(o))
            return false;
        RAExtReferenceId that = (RAExtReferenceId) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), value);
    }

    @Override
    public String toString() {
        return "RAReferenceId{" +
                "ownerOid='" + ownerOid + '\'' +
                ", ownerId=" + ownerId +
                ", itemId=" + itemId +
                ", value='" + value + '\'' +
                '}';
    }

    public static RAExtReferenceId createFromValue(RAExtReference value) {
        RAExtReferenceId rv = new RAExtReferenceId();
        rv.value = value.getValue();
        rv.fillInFromValue(value);
        return rv;
    }
}
