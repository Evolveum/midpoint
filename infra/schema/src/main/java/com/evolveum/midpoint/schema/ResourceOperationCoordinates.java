/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.io.Serializable;

/**
 * Specifies the "coordinates" of a resource operation covering multiple objects, like search, or live sync.
 *
 * Differences from {@link ResourceShadowCoordinates}:
 *
 * . no `tag` information,
 * . resource is obligatory,
 * . categorized into {@link TypeScoped}, {@link ObjectClassScoped}, and {@link ResourceScoped} subtypes,
 * to indicate the scope of the operation.
 *
 * @see ResourceShadowCoordinates
 */
public abstract class ResourceOperationCoordinates
        implements Serializable, DebugDumpable, ShortDumpable, HumanReadableDescribable, Cloneable {

    @NotNull final String resourceOid;

    ResourceOperationCoordinates(@NotNull String resourceOid) {
        this.resourceOid = resourceOid;
    }

    public static ResourceOperationCoordinates of(
            @NotNull String resourceOid,
            @Nullable ShadowKindType kind,
            @Nullable String intent,
            @Nullable QName objectClassName) {
        if (kind == null) {
            return of(resourceOid, objectClassName);
        } else {
            return ofType(resourceOid, kind, intent, objectClassName);
        }
    }

    public static ResourceOperationCoordinates of(
            @NotNull String resourceOid,
            @Nullable QName objectClassName) {
        if (objectClassName == null) {
            return ofResource(resourceOid);
        } else {
            return ofObjectClass(resourceOid, objectClassName);
        }
    }

    public static ResourceOperationCoordinates ofResource(@NotNull String resourceOid) {
        return new ResourceScoped(resourceOid);
    }

    public static ResourceOperationCoordinates ofObjectClass(@NotNull String resourceOid, @NotNull QName objectClassName) {
        return new ObjectClassScoped(resourceOid, objectClassName);
    }

    public static ResourceOperationCoordinates ofType(
            @NotNull String resourceOid,
            @NotNull ShadowKindType kind,
            @Nullable String intent,
            @Nullable QName objectClassName) {
        return new TypeScoped(resourceOid, kind, intent, objectClassName);
    }

    public @NotNull String getResourceOid() {
        return resourceOid;
    }

    public @Nullable ShadowKindType getKind() {
        return null;
    }

    public @Nullable String getIntent() {
        return null;
    }

    public @Nullable QName getObjectClassName() {
        return null;
    }

    private boolean haveUnknownValuesPresent() {
        return getKind() == ShadowKindType.UNKNOWN
                || SchemaConstants.INTENT_UNKNOWN.equals(getIntent());
    }

    public void checkNotUnknown() {
        if (haveUnknownValuesPresent()) {
            throw new IllegalStateException(
                    "Unknown kind/intent values are not expected here: " + toHumanReadableDescription());
        }
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public ResourceOperationCoordinates clone() {
        return this; // The object is immutable anyway
    }

    @Override
    public String toString() {
        return toHumanReadableDescription();
    }

    @Override
    public void shortDump(StringBuilder sb) {
        shortDump(sb, true);
    }

    abstract void shortDump(StringBuilder sb, boolean writeOid);

    @Override
    public String toHumanReadableDescription() {
        return toHumanReadableDescription(true);
    }

    public String toHumanReadableDescription(boolean writeOid) {
        StringBuilder sb = new StringBuilder("ROC(");
        shortDump(sb, writeOid);
        sb.append(")");
        return sb.toString();
    }

    void appendResourceOidIfNeeded(StringBuilder sb, boolean writeOid) {
        if (writeOid) {
            sb.append(" @");
            sb.append(resourceOid);
        }
    }

    public void checkNotResourceScoped() {
        if (areResourceScoped()) {
            throw new IllegalArgumentException("This operation cannot be applied to the whole resource. Object type "
                    + "or object class must be specified: " + this);
        }
    }

    public boolean areObjectTypeScoped() {
        return this instanceof TypeScoped;
    }

    public boolean areObjectClassScoped() {
        return this instanceof ObjectClassScoped;
    }

    private boolean areResourceScoped() {
        return this instanceof ResourceScoped;
    }

    public static class TypeScoped extends ResourceOperationCoordinates {

        @NotNull private final ShadowKindType kind;
        @Nullable private final String intent;
        @Nullable private final QName objectClassName;

        TypeScoped(
                @NotNull String resourceOid,
                @NotNull ShadowKindType kind,
                @Nullable String intent,
                @Nullable QName objectClassName) {
            super(resourceOid);
            this.kind = kind;
            this.intent = intent;
            this.objectClassName = objectClassName;
        }

        @Override
        public @NotNull ShadowKindType getKind() {
            return kind;
        }

        @Override
        public @Nullable String getIntent() {
            return intent;
        }

        @Override
        public @Nullable QName getObjectClassName() {
            return objectClassName;
        }

        @Override
        public String debugDump(int indent) {
            StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
            DebugUtil.debugDumpWithLabelLn(sb, "resourceOid", resourceOid, indent + 1);
            DebugUtil.debugDumpWithLabelToStringLn(sb, "kind", kind, indent + 1);
            DebugUtil.debugDumpWithLabelLn(sb, "intent", intent, indent + 1);
            DebugUtil.debugDumpWithLabel(sb, "objectClassName", objectClassName, indent + 1);
            return sb.toString();
        }

        @Override
        void shortDump(StringBuilder sb, boolean writeOid) {
            sb.append(kind.value());
            sb.append(" (").append(intent);
            sb.append(")");
            if (objectClassName != null) {
                sb.append(": ").append(PrettyPrinter.prettyPrint(objectClassName));
            }
            appendResourceOidIfNeeded(sb, writeOid);
        }
    }

    public static class ObjectClassScoped extends ResourceOperationCoordinates {

        @NotNull private final QName objectClassName;

        ObjectClassScoped(@NotNull String resourceOid, @NotNull QName objectClassName) {
            super(resourceOid);
            this.objectClassName = objectClassName;
        }

        @Override
        public @NotNull QName getObjectClassName() {
            return objectClassName;
        }

        @Override
        public String debugDump(int indent) {
            StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
            DebugUtil.debugDumpWithLabelLn(sb, "resourceOid", resourceOid, indent + 1);
            DebugUtil.debugDumpWithLabel(sb, "objectClassName", objectClassName, indent + 1);
            return sb.toString();
        }

        @Override
        void shortDump(StringBuilder sb, boolean writeOid) {
            sb.append(PrettyPrinter.prettyPrint(objectClassName));
            appendResourceOidIfNeeded(sb, writeOid);
        }
    }

    public static class ResourceScoped extends ResourceOperationCoordinates {

        ResourceScoped(@NotNull String resourceOid) {
            super(resourceOid);
        }

        @Override
        public String debugDump(int indent) {
            StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
            DebugUtil.debugDumpWithLabel(sb, "resourceOid", resourceOid, indent + 1);
            return sb.toString();
        }

        @Override
        void shortDump(StringBuilder sb, boolean writeOid) {
            sb.append("all object classes");
            appendResourceOidIfNeeded(sb, writeOid);
        }
    }
}
