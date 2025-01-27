/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.xml.ns._public.common.common_3.PropertyAccessType;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * Speed-optimized version of {@link PropertyAccessType}.
 *
 * It is used quite often in resource attributes.
 *
 * Do not use outside {@link PropertyLimitations}.
 */
public class PropertyAccess implements Serializable, Cloneable {

    private boolean read;
    private boolean add;
    private boolean modify;

    @Serial private static final long serialVersionUID = 0L;

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public boolean isAdd() {
        return add;
    }

    public void setAdd(boolean add) {
        this.add = add;
    }

    public boolean isModify() {
        return modify;
    }

    public void setModify(boolean modify) {
        this.modify = modify;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PropertyAccess propertyAccess)) {
            return false;
        }
        return read == propertyAccess.read && add == propertyAccess.add && modify == propertyAccess.modify;
    }

    @Override
    public int hashCode() {
        return Objects.hash(read, add, modify);
    }

    @Override
    public PropertyAccess clone() {
        try {
            return (PropertyAccess) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
