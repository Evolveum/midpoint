/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.context;

import static com.evolveum.midpoint.prism.Referencable.getOid;
import static com.evolveum.midpoint.util.MiscUtil.*;

import java.io.Serializable;
import java.util.Objects;

import com.evolveum.midpoint.schema.ResourceShadowCoordinates;

import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * Uniquely identifies {@link ModelProjectionContext}.
 *
 * (Originally, `ResourceShadowDiscriminator` was used as a key. However, it was overloaded with other duties,
 * so it was better to create a specialized class for this.)
 *
 * There are three flavors of these objects:
 *
 * 1. {@link Classified}: this is the most wanted state - we have resource and object type information;
 * 2. {@link Unclassified}: knowing the resource, but not the type (kind/intent) - should be avoided if at all possible;
 * 3. {@link WithoutResource}: almost no information; such a context is basically just a placeholder for broken links.
 *
 * == Construction
 *
 * === When you exactly know the flavor
 *
 * You can then use methods like:
 *
 * . {@link #classified(String, ResourceObjectTypeIdentification, String, int, boolean)}
 * . {@link #missing()}
 *
 * === When uncertain about the flavor (classified/unclassified)
 *
 * Then there are "dispatching" creation methods like {@link #forKnownResource(String, ResourceObjectTypeIdentification, String,
 * int, boolean)} that create both classified and unclassified instances.
 *
 * === When using specific source
 *
 * Use custom methods like
 *
 * - {@link #fromBean(ShadowDiscriminatorType)},
 * - {@link #fromCoordinates(ResourceShadowCoordinates)},
 * - {@link #fromClassifiedShadow(ShadowType)}.
 *
 * === For potentially unclassified shadows
 *
 * Because keys for unclassified shadows are so dangerous, the best approach here is to use
 * {@link ProjectionContextKeyFactory#createKey(ShadowType, Task, OperationResult)} method that tries
 * regular or emergency classification before creating the key.
 *
 * @see ProjectionContextKeyFactory
 */
public abstract class ProjectionContextKey
        implements Serializable, DebugDumpable, ShortDumpable, HumanReadableDescribable, Cloneable {

    final String resourceOid;
    final ResourceObjectTypeIdentification typeIdentification;
    private final String tag;
    private final int order;
    private final boolean gone;

    protected ProjectionContextKey(
            String resourceOid,
            ResourceObjectTypeIdentification typeIdentification,
            String tag,
            int order,
            boolean gone) {
        this.resourceOid = resourceOid;
        this.typeIdentification = typeIdentification;
        this.tag = tag;
        this.order = order;
        this.gone = gone;
    }

    /** Intentionally private to make clients think for a while when creating instances of this class. */
    private static ProjectionContextKey create(
            String resourceOid,
            ResourceObjectTypeIdentification typeIdentification,
            String tag,
            int order,
            boolean gone) {
        if (resourceOid != null) {
            if (typeIdentification != null) {
                return new Classified(resourceOid, typeIdentification, tag, order, gone);
            } else {
                return new Unclassified(resourceOid, tag, order, gone);
            }
        } else {
            return new WithoutResource(typeIdentification, tag, order, gone);
        }
    }

    public static Classified classified(
            @NotNull String resourceOid,
            @NotNull ResourceObjectTypeIdentification typeIdentification,
            @Nullable String tag,
            int order,
            boolean gone) {
        return new Classified(resourceOid, typeIdentification, tag, order, gone);
    }

    /** Kind and intent must not be "unknown". */
    public static ProjectionContextKey classified(
            @NotNull String resourceOid,
            @NotNull ShadowKindType kind,
            @NotNull String intent,
            @Nullable String tag) {
        return classified(resourceOid, kind, intent, tag, 0, false);
    }

    /** Kind and intent must not be "unknown". */
    public static ProjectionContextKey classified(
            @NotNull String resourceOid,
            @NotNull ShadowKindType kind,
            @NotNull String intent,
            @Nullable String tag,
            int order,
            boolean gone) {
        return new Classified(
                resourceOid,
                ResourceObjectTypeIdentification.of(kind, intent),
                tag,
                order,
                gone);
    }

    @VisibleForTesting
    public static ProjectionContextKey forKnownResource(
            @NotNull String resourceOid,
            @Nullable ResourceObjectTypeIdentification typeIdentification,
            @Nullable String tag) {
        return forKnownResource(resourceOid, typeIdentification, tag, 0, false);
    }

    public static ProjectionContextKey forKnownResource(
            @NotNull String resourceOid,
            @Nullable ResourceObjectTypeIdentification typeIdentification,
            @Nullable String tag,
            int order,
            boolean gone) {
        if (typeIdentification != null) {
            return new Classified(resourceOid, typeIdentification, tag, order, gone);
        } else {
            return new Unclassified(resourceOid, tag, order, gone);
        }
    }

    public static ProjectionContextKey missing() {
        return new WithoutResource(null, null, 0, true);
    }

    public static ProjectionContextKey fromCoordinates(@NotNull ResourceShadowCoordinates coordinates) {
        return forKnownResource(
                MiscUtil.argNonNull(coordinates.getResourceOid(), () -> "No resource OID in " + coordinates),
                ResourceObjectTypeIdentification.createIfKnown(coordinates),
                coordinates.getTag(),
                0,
                false);
    }

    public static ProjectionContextKey fromBean(ShadowDiscriminatorType bean) {
        if (bean == null) {
            return null;
        } else {
            return create(
                    getOid(bean.getResourceRef()),
                    ResourceObjectTypeIdentification.createIfKnown(bean.getKind(), bean.getIntent()),
                    bean.getTag(),
                    or0(bean.getDiscriminatorOrder()),
                    BooleanUtils.isTrue(bean.isTombstone()));
        }
    }

    public static ProjectionContextKey fromClassifiedShadow(@NotNull ShadowType shadow) {
        if (ShadowUtil.isClassified(shadow)) {
            return fromShadow(shadow, ResourceObjectTypeIdentification.createIfKnown(shadow));
        } else {
            throw new IllegalStateException(
                    "Shadow %s is not classified. Its kind is %s and intent is %s".formatted(
                            shadow, shadow.getKind(), shadow.getIntent()));
        }
    }

    public static ProjectionContextKey fromShadow(
            @NotNull ShadowType shadow, @Nullable ResourceObjectTypeIdentification typeIdentification) {
        return ProjectionContextKey.forKnownResource(
                ShadowUtil.getResourceOidRequired(shadow),
                typeIdentification,
                shadow.getTag(),
                0,
                ShadowUtil.isGoneApproximate(shadow));
    }

    public String getResourceOid() {
        return resourceOid;
    }

    public ResourceObjectTypeIdentification getTypeIdentification() {
        return typeIdentification;
    }

    public @Nullable ShadowKindType getKind() {
        return typeIdentification != null ? typeIdentification.getKind() : null;
    }

    public @Nullable String getIntent() {
        return typeIdentification != null ? typeIdentification.getIntent() : null;
    }

    public String getTag() {
        return tag;
    }

    public int getOrder() {
        return order;
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

    public ShadowDiscriminatorType toResourceShadowDiscriminatorType() {
        ShadowDiscriminatorType bean = new ShadowDiscriminatorType();
        bean.setKind(getKind());
        bean.setIntent(getIntent());
        bean.setTag(getTag());
        String resourceOid = getResourceOid();
        if (resourceOid != null) {
            bean.setResourceRef(
                    ObjectTypeUtil.createObjectRef(resourceOid, ObjectTypes.RESOURCE));
        }
        bean.setTombstone(gone);
        bean.setDiscriminatorOrder(getOrder());
        return bean;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ProjectionContextKey that = (ProjectionContextKey) o;
        return order == that.order
                && gone == that.gone
                && Objects.equals(resourceOid, that.resourceOid)
                && Objects.equals(typeIdentification, that.typeIdentification)
                && Objects.equals(tag, that.tag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceOid, typeIdentification, tag, order, gone);
    }

    /**
     * Similar to {@link #equals(Object)} but ignores the order.
     *
     * TODO what about comparing irregular keys (no resource? no kind/intent?)
     */
    public boolean equivalent(ProjectionContextKey other) {
        return gone == other.gone
                && Objects.equals(getResourceOid(), other.getResourceOid())
                && Objects.equals(getTypeIdentification(), other.getTypeIdentification())
                && Objects.equals(getTag(), other.getTag());
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
        ResourceObjectTypeIdentification typeIdentification = getTypeIdentification();
        if (typeIdentification != null) {
            sb.append(typeIdentification);
        } else {
            sb.append("(no type)");
        }
        String tag = getTag();
        if (tag != null) {
            sb.append(":").append(tag);
        }
        if (writeOid) {
            sb.append(" @");
            sb.append(getResourceOid());
        }
        int order = getOrder();
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
        StringBuilder sb = new StringBuilder("[");
        shortDump(sb, writeOid);
        sb.append("]");
        return sb.toString();
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "resourceOid", getResourceOid(), indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "typeIdentification", getTypeIdentification(), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "tag", getTag(), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "gone", gone, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "order", getOrder(), indent + 1);
        return sb.toString();
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public ProjectionContextKey clone() {
        return this; // immutable
    }

    public ProjectionContextKey withResourceOid(@NotNull String resourceOid) {
        return ProjectionContextKey.forKnownResource(resourceOid, getTypeIdentification(), getTag(), getOrder(), gone);
    }

    public ProjectionContextKey checkOrUpdateResourceOid(@NotNull String newValue) {
        if (resourceOid != null) {
            stateCheck(resourceOid.equals(newValue),
                    "Not allowed to change resource OID from %s to %s in %s", resourceOid, newValue, this);
            return this;
        } else {
            return withResourceOid(newValue);
        }
    }

    public ProjectionContextKey checkOrUpdateTypeIdentification(@NotNull ResourceObjectTypeIdentification newValue) {
        if (typeIdentification != null) {
            // We should not allow changing type identification. This is quite a harsh change (at least when done during
            // projector/clockwork execution).
            stateCheck(typeIdentification.equals(newValue),
                    "Not allowed to change type identification from %s to %s in %s",
                    typeIdentification, newValue, this);
            return this;
        } else {
            return create(resourceOid, newValue, tag, order, gone);
        }
    }

    /**
     * The usefulness of this method is not obvious; but - in fact - it is used e.g. in `TestMultiAccount.test350`, when
     * tag is changed from `Ix` to `ix`, when treating conflicting accounts in `ProjectionValuesProcessor`.
     *
     * Maybe we should reconsider if/when the tag should be updated.
     */
    public ProjectionContextKey updateTagIfChanged(String newValue) {
        if (!Objects.equals(tag, newValue)) {
            return create(resourceOid, typeIdentification, newValue, order, gone);
        } else {
            return this;
        }
    }

    public ProjectionContextKey withOrder(int order) {
        return create(resourceOid, typeIdentification, tag, order, gone);
    }

    public ProjectionContextKey gone() {
        return create(resourceOid, typeIdentification, tag, order, true);
    }

    public boolean isClassified() {
        return resourceOid != null && typeIdentification != null;
    }

    public abstract static class WithResource extends ProjectionContextKey {

        WithResource(
                @NotNull String resourceOid,
                ResourceObjectTypeIdentification typeIdentification,
                String tag,
                int order,
                boolean gone) {
            super(resourceOid, typeIdentification, tag, order, gone);
        }

        @Override
        public @NotNull String getResourceOid() {
            return resourceOid;
        }
    }

    public static class Classified extends WithResource {

        Classified(String resourceOid, ResourceObjectTypeIdentification typeIdentification, String tag, int order, boolean gone) {
            super(resourceOid, typeIdentification, tag, order, gone);
        }

        public @NotNull ResourceObjectTypeIdentification getTypeIdentification() {
            return typeIdentification;
        }
    }

    private static class Unclassified extends WithResource {

        private Unclassified(
                @NotNull String resourceOid,
                @Nullable String tag,
                int order,
                boolean gone) {
            super(resourceOid, null, tag, order, gone);
        }
    }

    private static class WithoutResource extends ProjectionContextKey {

        WithoutResource(
                ResourceObjectTypeIdentification typeIdentification,
                String tag,
                int order,
                boolean gone) {
            super(null, typeIdentification, tag, order, gone);
        }
    }
}
