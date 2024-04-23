/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger.threeway;

import java.util.List;

import org.jetbrains.annotations.NotNull;

/**
 * TODO DOC
 */
public record MergeResult(@NotNull List<MergeFragment> fragments) {

    public MergeResult() {
        this(List.of());
    }

    public MergeResult(@NotNull MergeFragment fragment) {
        this(List.of(fragment));
    }

    public boolean isEmpty() {
        return fragments.isEmpty();
    }

    public boolean hasConflict() {
        return fragments.stream().anyMatch(MergeFragment::conflict);
    }
}
