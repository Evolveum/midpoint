/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.construction;

import com.evolveum.midpoint.model.api.context.ProjectionContextKey;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Objects;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;

/**
 * Specifies the target of resource object construction - i.e. to which projection context it should be applied.
 *
 * It is similar to {@link ProjectionContextKey} but
 *
 * 1. resource, kind, and intent are required - we can afford this, because constructions are always targeted at specific
 * resource object type,
 * 2. there is no order nor "gone" flag.
 *
 * Note: the name of this class is a shorted form of `ResourceObjectConstructionTargetKey` (that is just too long/clumsy).
 *
 * *FIXME* There is a slight ambiguity here: a {@link ConstructionTargetKey} may point to multiple projection contexts.
 *   While the ambiguity of "gone" flag (true/false) is trivial and can be eliminated by simply looking for "not gone" projections,
 *   the ambiguity of "order" is more problematic. I.e. what if we assign a construction of resource/account/default, and there
 *   are multiple contexts with these coordinates (e.g. with orders 0 and 10)? A naive attempt of using the first "not completed"
 *   one fails because of the fact that constructions may be re-evaluated, and - during re-evaluation - they are meant to be
 *   attached to projections that are already completed.  See
 *   {@link LensContext#findFirstNotCompletedProjectionContext(ConstructionTargetKey)} and
 *   {@link LensContext#findFirstProjectionContext(ConstructionTargetKey)}.
 *   But this ambiguity is here (perhaps) from the beginning.
 */
public class ConstructionTargetKey
        implements Serializable, DebugDumpable, ShortDumpable, HumanReadableDescribable, Cloneable {

    @NotNull private final String resourceOid;

    @NotNull private final ShadowKindType kind;

    @NotNull private final String intent;

    private final String tag;

    ConstructionTargetKey(
            @NotNull String resourceOid,
            @NotNull ShadowKindType kind,
            @NotNull String intent,
            String tag) {
        argCheck(ShadowUtil.isKnown(kind), "Unknown kind: %s", kind);
        argCheck(ShadowUtil.isKnown(intent), "Unknown intent: %s", intent);
        this.resourceOid = resourceOid;
        this.kind = kind;
        this.intent = intent;
        this.tag = tag;
    }

    public @NotNull String getResourceOid() {
        return resourceOid;
    }

    public @NotNull ShadowKindType getKind() {
        return kind;
    }

    public @NotNull String getIntent() {
        return intent;
    }

    public String getTag() {
        return tag;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ConstructionTargetKey that = (ConstructionTargetKey) o;
        return resourceOid.equals(that.resourceOid)
                && kind == that.kind
                && intent.equals(that.intent)
                && Objects.equals(tag, that.tag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceOid, kind, intent, tag);
    }

    @Override
    public String toString() {
        return toHumanReadableDescription();
    }

    @Override
    public void shortDump(StringBuilder sb) {
        sb.append(kind.value()).append(" (").append(intent);
        if (tag != null) {
            sb.append("/").append(tag);
        }
        sb.append(") @").append(resourceOid);
    }

    @Override
    public String toHumanReadableDescription() {
        StringBuilder sb = new StringBuilder("ConstructionTargetKey(");
        shortDump(sb);
        sb.append(")");
        return sb.toString();
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "resourceOid", resourceOid, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "kind", kind, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "intent", indent, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "tag", tag, indent + 1);
        return sb.toString();
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public ConstructionTargetKey clone() {
        return this; // immutable
    }

    public ProjectionContextKey toProjectionContextKey() {
        return ProjectionContextKey.classified(resourceOid, kind, intent, tag);
    }
}
