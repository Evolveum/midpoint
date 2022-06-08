/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import java.io.Serializable;
import java.util.Objects;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

import org.jetbrains.annotations.Nullable;

/**
 * Aggregate bean containing resource OID, kind, intent, object class and tag flags.
 *
 * For general use where a combination of (some of) these information bits is needed.
 *
 * @see ResourceOperationCoordinates
 *
 * @author Radovan Semancik
 */
public class ResourceShadowCoordinates
        implements Serializable, DebugDumpable, ShortDumpable, HumanReadableDescribable, Cloneable {

    @Nullable protected final String resourceOid;
    @Nullable protected final ShadowKindType kind;
    @Nullable protected final String intent;
    @Nullable protected final String tag;
    @Nullable protected final QName objectClass;

    public ResourceShadowCoordinates(
            @Nullable String resourceOid,
            @Nullable ShadowKindType kind,
            @Nullable String intent,
            @Nullable String tag,
            @Nullable QName objectClass) {
        this.resourceOid = resourceOid;
        this.kind = kind;
        this.intent = intent;
        this.tag = tag;
        this.objectClass = objectClass;
    }

    public ResourceShadowCoordinates(String resourceOid, ShadowKindType kind, String intent, QName objectClass) {
        this(resourceOid, kind, intent, null, objectClass);
    }

    public ResourceShadowCoordinates(String resourceOid, ShadowKindType kind, String intent) {
        this(resourceOid, kind, intent, null, null);
    }

    public @Nullable String getResourceOid() {
        return resourceOid;
    }

    public @Nullable ShadowKindType getKind() {
        return kind;
    }

    public @Nullable String getIntent() {
        return intent;
    }

    public @Nullable String getTag() {
        return tag;
    }

    public @Nullable QName getObjectClass() {
        return objectClass;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ResourceShadowCoordinates that = (ResourceShadowCoordinates) o;
        return Objects.equals(resourceOid, that.resourceOid)
                && kind == that.kind
                && Objects.equals(intent, that.intent)
                && Objects.equals(tag, that.tag)
                && Objects.equals(objectClass, that.objectClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceOid, kind, intent, tag, objectClass);
    }

    @Override
    public String toString() {
        return toHumanReadableDescription();
    }

    @Override
    public void shortDump(StringBuilder sb) {
        shortDump(sb, true);
    }

    private void shortDump(StringBuilder sb, boolean writeOid) {
        sb.append(kind==null?"null":kind.value());
        sb.append(" (").append(intent);
        if (tag != null) {
            sb.append("/").append(tag);
        }
        sb.append(")");
        if (objectClass != null) {
            sb.append(": ").append(PrettyPrinter.prettyPrint(objectClass));
        }
        if (writeOid) {
            sb.append(" @");
            sb.append(resourceOid);
        }
    }

    @Override
    public String toHumanReadableDescription() {
        return toHumanReadableDescription(true);
    }

    public String toHumanReadableDescription(boolean writeOid) {
        StringBuilder sb = new StringBuilder("RSD(");
        shortDump(sb, writeOid);
        sb.append(")");
        return sb.toString();
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append(getClass().getName()).append("\n");
        DebugUtil.debugDumpWithLabelLn(sb, "resourceOid", resourceOid, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "kind", kind, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "intent", intent, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "tag", tag, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "objectClass", objectClass, indent + 1);
        return sb.toString();
    }

    @Override
    public ResourceShadowCoordinates clone() {
        try {
            return (ResourceShadowCoordinates) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    public boolean isTypeSpecified() {
        return ShadowUtil.isKnown(kind) && ShadowUtil.isKnown(intent);
    }
}
