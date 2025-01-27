/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.processor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

import com.evolveum.midpoint.prism.ItemProcessing;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;

/**
 * @author semancik
 *
 */
public class PropertyLimitations implements DebugDumpable, Serializable {
    @Serial private static final long serialVersionUID = 1L;

    private ItemProcessing processing;
    private int minOccurs;
    private int maxOccurs;
    private PropertyAccess access = new PropertyAccess();

    public ItemProcessing getProcessing() {
        return processing;
    }

    public void setProcessing(ItemProcessing processing) {
        this.processing = processing;
    }

    public int getMinOccurs() {
        return minOccurs;
    }

    public void setMinOccurs(int minOccurs) {
        this.minOccurs = minOccurs;
    }

    public int getMaxOccurs() {
        return maxOccurs;
    }

    public void setMaxOccurs(int maxOccurs) {
        this.maxOccurs = maxOccurs;
    }

    public PropertyAccess getAccess() {
        return access;
    }

    private void setAccess(PropertyAccess access) {
        this.access = access;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append(this);
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(minOccurs).append(",").append(maxOccurs).append("]");
        sb.append(",");
        if (canRead()) {
            sb.append("R");
        } else {
            sb.append("-");
        }
        if (canAdd()) {
            sb.append("A");
        } else {
            sb.append("-");
        }
        if (canModify()) {
            sb.append("M");
        } else {
            sb.append("-");
        }
        if (processing != null) {
            sb.append(",").append(processing);
        }
        return sb.toString();
    }

    // Note that the defaults here may look strange but it is exactly as existing clients use the data.

    // TODO maybe the names should be thought out once more...

    /** Returns `true` if the `modify` operation is allowed. */
    public boolean canModify() {
        return access == null || access.isModify();
    }

    /** Returns `true` if the `add` operation is allowed. */
    public boolean canAdd() {
        return access == null || access.isAdd();
    }

    /** Returns `true` if the `read` operation is allowed. */
    public boolean canRead() {
        return access == null || access.isRead();
    }

    PropertyLimitations cloneWithNewCardinality(int newMinOccurs, int newMaxOccurs) {
        var clone = new PropertyLimitations();
        clone.setProcessing(processing);
        clone.setMinOccurs(newMinOccurs);
        clone.setMaxOccurs(newMaxOccurs);
        clone.setAccess(access.clone());
        return clone;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PropertyLimitations that = (PropertyLimitations) o;
        return minOccurs == that.minOccurs
                && maxOccurs == that.maxOccurs
                && processing == that.processing
                && Objects.equals(access, that.access);
    }

    @Override
    public int hashCode() {
        return Objects.hash(processing, minOccurs, maxOccurs, access);
    }
}
