/*
 * Copyright (c) 2016 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.util;

import java.io.Serializable;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author semancik
 *
 */
public class MergeDeltas<O extends ObjectType> implements DebugDumpable, Serializable {
    private static final long serialVersionUID = 1L;

    private ObjectDelta<O> leftObjectDelta;
    private ObjectDelta<O> leftLinkDelta;
    private ObjectDelta<O> rightLinkDelta;

    public MergeDeltas(ObjectDelta<O> leftObjectDelta, ObjectDelta<O> leftLinkDelta,
            ObjectDelta<O> rightLinkDelta) {
        super();
        this.leftObjectDelta = leftObjectDelta;
        this.leftLinkDelta = leftLinkDelta;
        this.rightLinkDelta = rightLinkDelta;
    }

    public ObjectDelta<O> getLeftObjectDelta() {
        return leftObjectDelta;
    }

    public ObjectDelta<O> getLeftLinkDelta() {
        return leftLinkDelta;
    }

    public ObjectDelta<O> getRightLinkDelta() {
        return rightLinkDelta;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((leftLinkDelta == null) ? 0 : leftLinkDelta.hashCode());
        result = prime * result + ((leftObjectDelta == null) ? 0 : leftObjectDelta.hashCode());
        result = prime * result + ((rightLinkDelta == null) ? 0 : rightLinkDelta.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        MergeDeltas other = (MergeDeltas) obj;
        if (leftLinkDelta == null) {
            if (other.leftLinkDelta != null) {
                return false;
            }
        } else if (!leftLinkDelta.equals(other.leftLinkDelta)) {
            return false;
        }
        if (leftObjectDelta == null) {
            if (other.leftObjectDelta != null) {
                return false;
            }
        } else if (!leftObjectDelta.equals(other.leftObjectDelta)) {
            return false;
        }
        if (rightLinkDelta == null) {
            if (other.rightLinkDelta != null) {
                return false;
            }
        } else if (!rightLinkDelta.equals(other.rightLinkDelta)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "MergeDeltas(leftObjectDelta=" + leftObjectDelta + ", leftLinkDelta=" + leftLinkDelta
                + ", rightLinkDelta=" + rightLinkDelta + ")";
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("MergeDeltas\n");
        DebugUtil.debugDumpWithLabelLn(sb, "leftObjectDelta", leftObjectDelta, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "leftLinkDelta", leftLinkDelta, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "rightLinkDelta", rightLinkDelta, indent + 1);
        return sb.toString();
    }

}
