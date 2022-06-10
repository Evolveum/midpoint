/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.context;

import java.io.Serializable;

import com.evolveum.midpoint.util.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

/**
 * Used to find a matching projection context.
 *
 * Originally, `ResourceShadowDiscriminator` (now removed) was used for this purpose. Also, {@link ProjectionContextKey} can
 * be used; but this class provides more explicit filtering behavior.
 *
 * Null values here mean "does not matter". An exception is {@link #tag} where the null value meaning is determined
 * by {@link #nullTagMeansAny}.
 */
@Experimental
public class ProjectionContextFilter
        implements Serializable, DebugDumpable, ShortDumpable, HumanReadableDescribable, Cloneable {

    @Nullable private final String resourceOid;
    @Nullable private final ShadowKindType kind;
    @Nullable private final String intent;
    @Nullable private final String tag;
    private final boolean nullTagMeansAny;
    @Nullable private final Boolean gone;

    private ProjectionContextFilter(
            @Nullable String resourceOid,
            @Nullable ShadowKindType kind,
            @Nullable String intent,
            @Nullable String tag,
            boolean nullTagMeansAny,
            @Nullable Boolean gone) {
        this.resourceOid = resourceOid;
        this.kind = kind;
        this.intent = intent;
        this.tag = tag;
        this.nullTagMeansAny = nullTagMeansAny;
        this.gone = gone;
    }

    /**
     * By specifying the tag the caller indicates it wants the exact match.
     */
    public ProjectionContextFilter(
            @Nullable String resourceOid,
            @Nullable ShadowKindType kind,
            @Nullable String intent,
            @Nullable String tag) {
        this(resourceOid, kind, intent, tag, false, null);
    }

    /**
     * By not specifying the tag the caller says it does not matter.
     */
    public ProjectionContextFilter(
            @Nullable String resourceOid,
            @Nullable ShadowKindType kind,
            @Nullable String intent) {
        this(resourceOid, kind, intent, null, true, null);
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

    public @Nullable Boolean getGone() {
        return gone;
    }

    public ProjectionContextFilter gone(Boolean gone) {
        return new ProjectionContextFilter(resourceOid, kind, intent, tag, nullTagMeansAny, gone);
    }

    public boolean matches(@NotNull ProjectionContextKey key) {
        return (resourceOid == null || resourceOid.equals(key.getResourceOid()))
                && (kind == null || kind == key.getKind())
                && (intent == null || intent.equals(key.getIntent()))
                && doesTagMatch(key)
                && (gone == null || key.isGone() == gone); // this "==" is OK: Boolean vs. boolean
    }

    private boolean doesTagMatch(@NotNull ProjectionContextKey key) {
        if (tag != null) {
            return tag.equals(key.getTag());
        } else {
            return nullTagMeansAny || key.getTag() == null;
        }
    }

    @Override
    public String toString() {
        return toHumanReadableDescription();
    }

    @Override
    public void shortDump(StringBuilder sb) {
        shortDump(sb, true);
    }

    public void shortDump(StringBuilder sb, boolean writeOid) {
        sb.append(kind).append("/").append(intent).append(":");
        if (tag == null) {
            if (nullTagMeansAny) {
                sb.append("*");
            } else {
                sb.append("null");
            }
        } else {
            sb.append(tag);
        }
        if (writeOid) {
            sb.append(" @");
            sb.append(resourceOid);
        }
        if (gone != null) {
            sb.append(" gone: ").append(gone);
        }
    }

    @Override
    public String toHumanReadableDescription() {
        return toHumanReadableDescription(true);
    }

    public String toHumanReadableDescription(boolean writeOid) {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append("[");
        shortDump(sb, writeOid);
        sb.append("]");
        return sb.toString();
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "resourceOid", resourceOid, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "kind", kind, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "intent", intent, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "tag", tag, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "nullTagMeansAny", nullTagMeansAny, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "gone", gone, indent + 1);
        return sb.toString();
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public ProjectionContextFilter clone() {
        return this; // immutable
    }
}
