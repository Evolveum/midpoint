/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.cache.handlers;

import com.evolveum.midpoint.schema.util.ShadowUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.repo.api.RepositoryOperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import javax.xml.namespace.QName;

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

    @Override
    public @Nullable QName getShadowObjectClassName() {
        return ShadowUtil.getObjectClassName(object);
    }
}
