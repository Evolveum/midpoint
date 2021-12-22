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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectClassDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.util.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowDiscriminatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

/**
 * Aggregate bean containing resource OID, kind, intent, object class and tag flags.
 * It uniquely identifies an shadow projection (usually account) for a specific user regardless whether it has OID, does not have
 * OID yet, it exists of was deleted.
 *
 * This is used mostly as a key in hashes and for searches.
 *
 * @author Radovan Semancik
 */
public class ResourceShadowCoordinates
        implements Serializable, DebugDumpable, ShortDumpable, HumanReadableDescribable, Cloneable {

    private static final long serialVersionUID = 346600684011645741L;

    protected final String resourceOid;
    protected final ShadowKindType kind;
    protected final String intent;
    protected final String tag;
    protected QName objectClass;

    public ResourceShadowCoordinates(
            String resourceOid,
            @NotNull ResourceObjectDefinition definition,
            @Nullable String tag) {
        this.resourceOid = resourceOid;
        if (definition instanceof ResourceObjectTypeDefinition) {
            this.kind = ((ResourceObjectTypeDefinition) definition).getKind();
            this.intent = ((ResourceObjectTypeDefinition) definition).getIntent();
            this.objectClass = null;
        } else if (definition instanceof ResourceObjectClassDefinition) {
            this.kind = null;
            this.intent = null;
            this.objectClass = definition.getObjectClassName();
        } else {
            throw new AssertionError(definition);
        }
        this.tag = tag;
    }

    public ResourceShadowCoordinates(String resourceOid, ShadowKindType kind, String intent, String tag) {
        this.resourceOid = resourceOid;
        this.kind = kind;
        this.intent = intent;
        this.tag = tag;
    }

    public ResourceShadowCoordinates(@NotNull ResourceShadowCoordinates prototype) {
        this.resourceOid = prototype.resourceOid;
        this.kind = prototype.kind;
        this.intent = prototype.intent;
        this.tag = prototype.tag;
        this.objectClass = prototype.objectClass;
    }

    public ResourceShadowCoordinates(ShadowDiscriminatorType accRefType) {
        this(accRefType.getResourceRef().getOid(), accRefType.getKind(), accRefType.getIntent(), accRefType.getTag());
    }

    public ResourceShadowCoordinates(ShadowDiscriminatorType accRefType, String defaultResourceOid, ShadowKindType defaultKind) {
        if (accRefType.getResourceRef() == null) {
            this.resourceOid = defaultResourceOid;
        } else {
            this.resourceOid = accRefType.getResourceRef().getOid();
        }
        this.kind = Objects.requireNonNullElse(accRefType.getKind(), defaultKind);
        this.intent = accRefType.getIntent();
        this.tag = null;
    }

    public ResourceShadowCoordinates(String resourceOid) {
        this.resourceOid = resourceOid;
        this.kind = ShadowKindType.ACCOUNT; // TODO???
        this.intent = null;
        this.tag = null;
    }

    public ResourceShadowCoordinates(String resourceOid, QName objectClass) {
        this.resourceOid = resourceOid;
        this.objectClass = objectClass;
        this.kind = null;
        this.intent = null;
        this.tag = null;
    }

    public ResourceShadowCoordinates(String resourceOid, ShadowKindType kind, String intent, QName objectClass) {
        this.resourceOid = resourceOid;
        this.objectClass = objectClass;
        this.kind = kind;
        this.intent = intent;
        this.tag = null;
    }

    public String getResourceOid() {
        return resourceOid;
    }

    public ShadowKindType getKind() {
        return kind;
    }

    public String getIntent() {
        return intent;
    }

    public String getTag() {
        return tag;
    }

    public QName getObjectClass() {
        return objectClass;
    }

    public void setObjectClass(QName objectClass) {
        this.objectClass = objectClass;
    }

    public boolean isWildcard() {
        return kind == null && objectClass == null;
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

    /**
     * Similar to equals but ignores the order.
     */
    @SuppressWarnings("RedundantIfStatement")
    public boolean equivalent(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        ResourceShadowCoordinates other = (ResourceShadowCoordinates) obj;
        if (intent == null) {
            if (other.intent != null) return false;
        } else if (!equalsIntent(this.intent, other.intent)) {
            return false;
        }
        if (!Objects.equals(this.tag, other.tag)) {
            return false;
        }
        if (resourceOid == null) {
            if (other.resourceOid != null) return false;
        } else if (!resourceOid.equals(other.resourceOid)) {
            return false;
        }
        return true;
    }

    // FIXME what if a == b == null ? The method should (most probably) return true in such case.
    public static boolean equalsIntent(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return a.equals(b);
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
        DebugUtil.debugDumpWithLabelLn(sb, "intent", indent, indent + 1);
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

    /** Copies everything except for object class name and order. */
    public ResourceShadowCoordinates cloneBasic() {
        ResourceShadowCoordinates clone = clone();
        clone.objectClass = null;
        return clone;
    }
}
