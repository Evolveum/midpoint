/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger.threeway;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;

/**
 * When there are two deltas available:
 * - if conflict is true, then one of two deltas have to be picked by user to be applied
 * - if conflict is false, then only one delta should be applied (they should be equivalent)
 *
 * @param left
 * @param right
 * @param conflict
 */
public record MergeFragment(
        @NotNull List<ItemDelta<?, ?>> left, @NotNull List<ItemDelta<?, ?>> right, boolean conflict)
        implements DebugDumpable {

    public MergeFragment(@NotNull ItemDelta<?, ?> left, @NotNull ItemDelta<?, ?> right, boolean conflict) {
        this(List.of(left), List.of(right), conflict);
    }

    public static MergeFragment conflict(@NotNull ItemDelta<?, ?> left, @NotNull ItemDelta<?, ?> right) {
        return new MergeFragment(List.of(left), List.of(right), true);
    }

    public static MergeFragment noConflict(@NotNull ItemDelta<?, ?> left, @NotNull ItemDelta<?, ?> right) {
        return new MergeFragment(List.of(left), List.of(right), true);
    }

    public static MergeFragment leftOnlyChange(@NotNull ItemDelta<?, ?> left) {
        return new MergeFragment(List.of(left), List.of(), false);
    }

    public static MergeFragment rightOnlyChange(@NotNull ItemDelta<?, ?> right) {
        return new MergeFragment(List.of(), List.of(right), false);
    }

    @Override
    public String toString() {
        return debugDump();
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpLabelLn(sb, "MF (conflict: " + conflict + "):", indent);
        DebugUtil.debugDumpWithLabelLn(sb, "left", left, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "right", right, indent + 1);

        return sb.toString();
    }
}
