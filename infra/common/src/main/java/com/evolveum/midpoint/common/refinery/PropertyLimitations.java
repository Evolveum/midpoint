/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.common.refinery;

import java.io.Serializable;

import com.evolveum.midpoint.prism.ItemProcessing;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PropertyAccessType;

/**
 * @author semancik
 *
 */
public class PropertyLimitations implements DebugDumpable, Serializable {
    private static final long serialVersionUID = 1L;

    private ItemProcessing processing;
    private int minOccurs;
    private int maxOccurs;
    private PropertyAccessType access = new PropertyAccessType();

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

    public PropertyAccessType getAccess() {
        return access;
    }

    public void setAccess(PropertyAccessType access) {
        this.access = access;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append(toString());
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(minOccurs).append(",").append(maxOccurs).append("]");
        sb.append(",");
        if (getAccess().isRead()) {
            sb.append("R");
        } else {
            sb.append("-");
        }
        if (getAccess().isAdd()) {
            sb.append("A");
        } else {
            sb.append("-");
        }
        if (getAccess().isModify()) {
            sb.append("M");
        } else {
            sb.append("-");
        }
        if (processing != null) {
            sb.append(",").append(processing);
        }
        return sb.toString();
    }

}
