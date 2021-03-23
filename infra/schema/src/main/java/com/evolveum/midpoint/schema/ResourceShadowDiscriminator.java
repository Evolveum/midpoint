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
import com.evolveum.midpoint.util.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowDiscriminatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;

/**
 * Aggregate bean containing resource OID, intent and tombstone flag.
 * It uniquely identifies an shadow projection (usually account) for a specific user regardless whether it has OID, does not have
 * OID yet, it exists of was deleted.
 *
 * This is used mostly as a key in hashes and for searches.
 *
 * TODO: split to two objects:
 * 1: ResourceShadowCoordinates which will stay in common
 * 2: ResourceShadowDiscriminator (subclass) which will go to model. This will contains tombstone and order.
 *
 * @author Radovan Semancik
 */
public class ResourceShadowDiscriminator implements Serializable, DebugDumpable, ShortDumpable, HumanReadableDescribable {
    private static final long serialVersionUID = 346600684011645741L;

    private String resourceOid;
    private ShadowKindType kind = ShadowKindType.ACCOUNT;
    private String intent;
    private String tag;
    private QName objectClass;
    private boolean tombstone;
    private int order = 0;

    public ResourceShadowDiscriminator(String resourceOid, ShadowKindType kind, String intent, String tag, boolean tombstone) {
        this.resourceOid = resourceOid;
        this.tombstone = tombstone;
        this.tag = tag;
        setIntent(intent);
        setKind(kind);
    }

    public ResourceShadowDiscriminator(ShadowDiscriminatorType accRefType) {
        this(accRefType.getResourceRef().getOid(), accRefType.getKind(), accRefType.getIntent(), accRefType.getTag(), false);
    }

    public ResourceShadowDiscriminator(ShadowDiscriminatorType accRefType, String defaultResourceOid, ShadowKindType defaultKind) {
        ShadowKindType kind = accRefType.getKind();
        if (kind == null) {
            kind = defaultKind;
        }
        if (accRefType.getResourceRef() == null) {
            this.resourceOid = defaultResourceOid;
        } else {
            this.resourceOid = accRefType.getResourceRef().getOid();
        }
        this.tombstone = false;
        setIntent(accRefType.getIntent());
        setKind(kind);
    }

    public ResourceShadowDiscriminator(String resourceOid) {
        this.resourceOid = resourceOid;
    }

    public ResourceShadowDiscriminator(String resourceOid, QName objectClass) {
        this.resourceOid = resourceOid;
        this.objectClass = objectClass;
        this.kind = null;
    }

    public String getResourceOid() {
        return resourceOid;
    }

    public void setResourceOid(String resourceOid) {
        this.resourceOid = resourceOid;
    }

    public ShadowKindType getKind() {
        return kind;
    }

    public void setKind(ShadowKindType kind) {
        this.kind = kind;
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
//        if (intent == null) {
//            intent = SchemaConstants.INTENT_DEFAULT;
//        }
        this.intent = intent;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public QName getObjectClass() {
        return objectClass;
    }

    public void setObjectClass(QName objectClass) {
        this.objectClass = objectClass;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    /**
     * Tombstone flag is true: the account no longer exists. The data we have are the latest metadata we were able to get.
     * The projection will be marked as tombstone if we discover that the associated resource object is gone. Or the shadow
     * is gone and we can no longer associate the resource object. In any way the tombstoned projection is marked for removal.
     * It will be eventually unlinked and the shadow will be deleted. The shadow may stay around in the "dead" state for
     * some time for reporting purposes.
     */
    public boolean isTombstone() {
        return tombstone;
    }

    public void setTombstone(boolean tombstone) {
        this.tombstone = tombstone;
    }

    public boolean isWildcard() {
        return kind == null && objectClass == null;
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
        bean.setTombstone(tombstone);
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
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((intent == null) ? 0 : intent.hashCode());
        result = prime * result + ((kind == null) ? 0 : kind.hashCode());
        result = prime * result + ((objectClass == null) ? 0 : objectClass.hashCode());
        result = prime * result + order;
        result = prime * result + ((resourceOid == null) ? 0 : resourceOid.hashCode());
        result = prime * result + ((tag == null) ? 0 : tag.hashCode());
        result = prime * result + (tombstone ? 1231 : 1237);
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
        ResourceShadowDiscriminator other = (ResourceShadowDiscriminator) obj;
        if (intent == null) {
            if (other.intent != null) {
                return false;
            }
        } else if (!intent.equals(other.intent)) {
            return false;
        }
        if (kind != other.kind) {
            return false;
        }
        if (objectClass == null) {
            if (other.objectClass != null) {
                return false;
            }
        } else if (!objectClass.equals(other.objectClass)) {
            return false;
        }
        if (order != other.order) {
            return false;
        }
        if (resourceOid == null) {
            if (other.resourceOid != null) {
                return false;
            }
        } else if (!resourceOid.equals(other.resourceOid)) {
            return false;
        }
        if (tag == null) {
            if (other.tag != null) {
                return false;
            }
        } else if (!tag.equals(other.tag)) {
            return false;
        }
        if (tombstone != other.tombstone) {
            return false;
        }
        return true;
    }

    /**
     * Similar to equals but ignores the order.
     */
    public boolean equivalent(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        ResourceShadowDiscriminator other = (ResourceShadowDiscriminator) obj;
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
        if (tombstone != other.tombstone) return false;
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
        if (order != 0) {
            sb.append(" order=");
            sb.append(order);
        }
        if (tombstone) {
            sb.append(" TOMBSTONE");
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
        DebugUtil.debugDumpWithLabelLn(sb, "tombstone", tombstone, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "order", order, indent + 1);
        return sb.toString();
    }
}
