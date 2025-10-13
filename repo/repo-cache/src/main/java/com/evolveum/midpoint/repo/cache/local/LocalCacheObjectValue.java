/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.cache.local;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.cache.values.CachedObjectValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class LocalCacheObjectValue<T extends ObjectType> implements CachedObjectValue<T> {

    @NotNull private final PrismObject<T> object;

    private final boolean complete;

    LocalCacheObjectValue(@NotNull PrismObject<T> object, boolean complete) {
        this.object = object;
        this.complete = complete;
    }

    @Override
    public @NotNull PrismObject<T> getObject() {
        return object;
    }

    @Override
    public boolean isComplete() {
        return complete;
    }

    @Override
    public String toString() {
        return "LocalCacheObjectValue{"
                + "object=" + object
                + " (version " + object.getVersion() + ")"
                + ", complete=" + complete
                + "}";
    }
}
