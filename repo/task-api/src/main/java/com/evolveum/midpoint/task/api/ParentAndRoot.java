/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.api;

import com.evolveum.midpoint.util.annotation.Experimental;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A container for task parent and root.
 *
 * Used to return both parent and root from a single "get" operation.
 * (They are determined together.)
 */
@Experimental
public class ParentAndRoot {
    @Nullable public final Task parent;
    @NotNull public final Task root;

    public ParentAndRoot(@Nullable Task parent, @NotNull Task root) {
        this.parent = parent;
        this.root = root;
    }

    public static ParentAndRoot fromPath(List<Task> pathToRoot) {
        assert !pathToRoot.isEmpty();
        return new ParentAndRoot(
                pathToRoot.size() > 1 ? pathToRoot.get(1) : null,
                pathToRoot.get(pathToRoot.size() - 1));
    }
}
