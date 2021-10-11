/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.common.id;

import com.evolveum.midpoint.repo.sql.data.common.any.ROExtDate;
import com.evolveum.midpoint.repo.sql.data.common.type.RObjectExtensionType;

import java.sql.Timestamp;
import java.util.Objects;

/**
 * @author lazyman
 */
public class ROExtDateId extends ROExtBaseId {

    private Timestamp value;

    @Override
    public String getOwnerOid() {
        return super.getOwnerOid();
    }

    @Override
    public RObjectExtensionType getOwnerType() {
        return super.getOwnerType();
    }

    @Override
    public Integer getItemId() {
        return super.getItemId();
    }

    public Timestamp getValue() {
        return value;
    }

    public void setValue(Timestamp value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ROExtDateId))
            return false;
        if (!super.equals(o))
            return false;
        ROExtDateId that = (ROExtDateId) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), value);
    }

    @Override
    public String toString() {
        return "ROExtDateId[" + ownerOid + "," + ownerType + "," + itemId + "," + value + "]";
    }

    public static ROExtDateId createFromValue(ROExtDate value) {
        ROExtDateId rv = new ROExtDateId();
        rv.value = value.getValue();
        rv.fillInFromValue(value);
        return rv;
    }
}
