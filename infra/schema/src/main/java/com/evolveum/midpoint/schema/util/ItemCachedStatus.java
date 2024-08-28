/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public enum ItemCachedStatus {

    NULL_OBJECT(false),
    CACHING_DISABLED(false),
    NO_TTL(false),
    NO_SHADOW_CACHING_METADATA(false),
    NO_SHADOW_RETRIEVAL_TIMESTAMP(false),
    INVALIDATED_GLOBALLY(false),
    SHADOW_EXPIRED(false),
    SHADOW_FRESH(true),
    ITEM_NOT_CACHED(false),
    ITEM_CACHED_AND_FRESH(true);

    private final boolean fresh;

    ItemCachedStatus(boolean fresh) {
        this.fresh = fresh;
    }

    public boolean isFresh() {
        return fresh;
    }

    public @NotNull ItemCachedStatus item(@NotNull Supplier<Boolean> itemCached) {
        if (!fresh) {
            return this;
        } else {
            return item(itemCached.get());
        }
    }

    public static @NotNull ItemCachedStatus item(boolean itemCached) {
        if (itemCached) {
            return ITEM_CACHED_AND_FRESH;
        } else {
            return ITEM_NOT_CACHED;
        }
    }
}
