/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.common.id;

import com.evolveum.midpoint.repo.sql.data.common.any.ROExtString;
import com.evolveum.midpoint.repo.sql.data.common.type.RObjectExtensionType;

import java.util.Objects;

/**
 * @author lazyman
 */
public class ROExtStringId extends ROExtBaseId {

    private String value;

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

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ROExtStringId))
            return false;
        if (!super.equals(o))
            return false;
        ROExtStringId that = (ROExtStringId) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), value);
    }

    @Override
    public String toString() {
        return "ROExtStringId[" + ownerOid + "," + ownerType + "," + itemId + "," + value + "]";
    }

    public static ROExtStringId createFromValue(ROExtString value) {
        ROExtStringId rv = new ROExtStringId();
        rv.value = value.getValue();
        rv.fillInFromValue(value);
        return rv;
    }
}
