/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.api;

import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;

/**
 * TODO
 */
@Experimental
public class AddObjectResult<T extends ObjectType> implements RepoUpdateOperationResult {

    @NotNull private final T objectAdded;

    public AddObjectResult(@NotNull T objectAdded) {
        this.objectAdded = objectAdded;
    }

    public T getObjectAdded() {
        return objectAdded;
    }
}
