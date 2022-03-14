/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task;

import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.jetbrains.annotations.NotNull;

/**
 * Used to resolve tasks (mainly subtasks) in various utility methods in this package.
 */
@Experimental
public interface TaskResolver {

    /**
     * @throws UnsupportedOperationException if this resolver does not support resolution of tasks
     */
    @NotNull TaskType resolve(String oid) throws SchemaException, ObjectNotFoundException, UnsupportedOperationException;

    /**
     * Does nothing: in its typical use it assumes that all children are pre-resolved.
     */
    static TaskResolver empty() {
        return oid -> {
            throw new UnsupportedOperationException("Found unresolved subtask " + oid);
        };
    }
}
