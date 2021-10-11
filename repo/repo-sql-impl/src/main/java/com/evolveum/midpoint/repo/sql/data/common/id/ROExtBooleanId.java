/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.common.id;

import com.evolveum.midpoint.repo.sql.data.common.any.ROExtBoolean;
import com.evolveum.midpoint.repo.sql.data.common.type.RObjectExtensionType;

import java.util.Objects;

/**
 * @author lazyman
 */
public class ROExtBooleanId extends ROExtBaseId {

    private Boolean value;

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

    public Boolean getValue() {
        return value;
    }

    public void setValue(Boolean value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ROExtBooleanId))
            return false;
        if (!super.equals(o))
            return false;
        ROExtBooleanId that = (ROExtBooleanId) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), value);
    }

    @Override
    public String toString() {
        return "ROExtBooleanId[" + ownerOid + "," + ownerType + "," + itemId + "," + value + "]";
    }

    public static ROExtBooleanId createFromValue(ROExtBoolean value) {
        ROExtBooleanId rv = new ROExtBooleanId();
        rv.value = value.getValue();
        rv.fillInFromValue(value);
        return rv;
    }
}
