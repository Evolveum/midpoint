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

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.util.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowDiscriminatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;

/**
 * TODO Review this
 *
 * Aggregate bean containing resource OID, intent and tombstone flag.
 * It uniquely identifies an shadow projection (usually account) for a specific user regardless whether it has OID, does not have
 * OID yet, it exists of was deleted.
 *
 * This is used mostly as a key in hashes and for searches.
 *
 * @author Radovan Semancik
 */
public class ResourceShadowDiscriminator extends ResourceShadowCoordinates
        implements Serializable, DebugDumpable, ShortDumpable, HumanReadableDescribable, Cloneable {

    private static final long serialVersionUID = 346600684011645741L;

    private boolean gone;
    private int order = 0;

    public ResourceShadowDiscriminator(String resourceOid, ResourceObjectDefinition definition, String tag, boolean gone) {
        super(resourceOid, definition, tag);
        this.gone = gone;
    }

    public ResourceShadowDiscriminator(String resourceOid, ShadowKindType kind, String intent, String tag, boolean gone) {
        super(resourceOid, kind, intent, tag);
        this.gone = gone;
    }

    public ResourceShadowDiscriminator(ShadowDiscriminatorType accRefType) {
        this(accRefType.getResourceRef().getOid(), accRefType.getKind(), accRefType.getIntent(), accRefType.getTag(), false);
    }

    public ResourceShadowDiscriminator(ShadowDiscriminatorType accRefType, String defaultResourceOid, ShadowKindType defaultKind) {
        super(accRefType, defaultResourceOid, defaultKind);
        this.gone = false;
    }

    public ResourceShadowDiscriminator(String resourceOid) {
        super(resourceOid);
    }

    public ResourceShadowDiscriminator(String resourceOid, QName objectClass) {
        super(resourceOid, objectClass);
    }

    public ResourceShadowDiscriminator(String resourceOid, ShadowKindType kind, String intent, QName objectClass) {
        super(resourceOid, kind, intent, objectClass);
    }

    public ResourceShadowDiscriminator(@NotNull ResourceShadowCoordinates coordinates) {
        super(coordinates);
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    /**
     * "Gone" flag is true: the account no longer exists. The data we have are the latest metadata we were able to get.
     * The projection will be marked as gone if we discover that the associated resource object is gone. Or the shadow
     * is gone and we can no longer associate the resource object. In any way the "gone" projection is marked for removal.
     * It will be eventually unlinked and the shadow will be deleted. The shadow may stay around in the "dead" state for
     * some time for reporting purposes.
     *
     * In the terms of shadow lifecycle state, this covers corpse and tombstone states.
     */
    public boolean isGone() {
        return gone;
    }

    public void setGone(boolean gone) {
        this.gone = gone;
    }

    public ShadowDiscriminatorType toResourceShadowDiscriminatorType() {
        ShadowDiscriminatorType bean = new ShadowDiscriminatorType();
        bean.setIntent(intent);
        bean.setKind(kind);
        bean.setTag(tag);
        ObjectReferenceType resourceRef = new ObjectReferenceType();
        resourceRef.setOid(resourceOid);
        resourceRef.setType(ResourceType.COMPLEX_TYPE);
        bean.setResourceRef(resourceRef);

        bean.setObjectClassName(objectClass);
        bean.setTombstone(gone);
        bean.setDiscriminatorOrder(order);
        return bean;
    }

    public static ResourceShadowDiscriminator fromResourceShadowDiscriminatorType(
            ShadowDiscriminatorType bean, boolean provideDefaultIntent) {
        if (bean == null) {
            return null;
        }

        // For compatibility. Otherwise the kind should be explicitly serialized.
        ShadowKindType kind = ObjectUtils.defaultIfNull(bean.getKind(), ShadowKindType.ACCOUNT);
        String intent = bean.getIntent() != null || !provideDefaultIntent ?
                bean.getIntent() : SchemaConstants.INTENT_DEFAULT;

        ResourceShadowDiscriminator rsd = new ResourceShadowDiscriminator(
                bean.getResourceRef() != null ? bean.getResourceRef().getOid() : null,
                kind, intent, bean.getTag(),
                BooleanUtils.isTrue(bean.isTombstone()));
        rsd.setObjectClass(bean.getObjectClassName());
        if (bean.getDiscriminatorOrder() != null) {
            rsd.setOrder(bean.getDiscriminatorOrder());
        }
        return rsd;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        ResourceShadowDiscriminator that = (ResourceShadowDiscriminator) o;
        return gone == that.gone && order == that.order;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), gone, order);
    }


    /**
     * Similar to equals but ignores the order.
     */
    public boolean equivalent(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        ResourceShadowDiscriminator other = (ResourceShadowDiscriminator) obj;
        if (!super.equivalent(obj)) {
            return false;
        }
        return gone == other.gone;
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
        if (order != 0) {
            sb.append(" order=");
            sb.append(order);
        }
        if (gone) {
            sb.append(" GONE");
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
        sb.append("ResourceShadowDiscriminator\n");
        DebugUtil.debugDumpWithLabelLn(sb, "resourceOid", resourceOid, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "kind", kind, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "intent", indent, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "tag", tag, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "objectClass", objectClass, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "gone", gone, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "order", order, indent + 1);
        return sb.toString();
    }

    @Override
    public ResourceShadowDiscriminator clone() {
        return (ResourceShadowDiscriminator) super.clone();
    }

    /** Copies everything except for object class name and order. */
    public ResourceShadowDiscriminator cloneBasic() {
        ResourceShadowDiscriminator clone = clone();
        clone.objectClass = null;
        clone.order = 0;
        return clone;
    }
}
