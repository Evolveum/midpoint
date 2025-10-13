/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.cache.handlers;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.repo.api.RepositoryOperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.jetbrains.annotations.NotNull;

/**
 * Result of the addObject operation. It is analogous to {@link com.evolveum.midpoint.repo.api.ModifyObjectResult}
 * and {@link com.evolveum.midpoint.repo.api.DeleteObjectResult} but cannot be returned by addObject,
 * because this operation returns String. This long-time convention cannot be changed easily, so we use
 * this workaround.
 */
public class AddObjectResult<T extends ObjectType> implements RepositoryOperationResult {

    @NotNull private final PrismObject<T> object;

    AddObjectResult(@NotNull PrismObject<T> object) {
        this.object = object;
    }

    @NotNull
    public PrismObject<T> getObject() {
        return object;
    }

    @Override
    public String toString() {
        return object.toString();
    }

    @Override
    public ChangeType getChangeType() {
        return ChangeType.ADD;
    }
}
