/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.lens;

import com.evolveum.midpoint.schema.util.ItemCachedStatus;

import org.jetbrains.annotations.NotNull;

public enum ItemLoadedStatus {

    FULL_SHADOW(true),

    /** The item may or may not be cached; but we must use fresh version anyway (because of the configuration). */
    USE_OF_CACHED_NOT_ALLOWED(false),

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

    private final boolean loaded;

    static ItemLoadedStatus fromItemCachedStatus(@NotNull ItemCachedStatus cachedStatus) {
        return switch (cachedStatus) {
            case NULL_OBJECT -> NULL_OBJECT;
            case CACHING_DISABLED -> CACHING_DISABLED;
            case NO_TTL -> NO_TTL;
            case NO_SHADOW_CACHING_METADATA -> NO_SHADOW_CACHING_METADATA;
            case NO_SHADOW_RETRIEVAL_TIMESTAMP -> NO_SHADOW_RETRIEVAL_TIMESTAMP;
            case INVALIDATED_GLOBALLY -> INVALIDATED_GLOBALLY;
            case SHADOW_EXPIRED -> SHADOW_EXPIRED;
            case SHADOW_FRESH -> SHADOW_FRESH;
            case ITEM_NOT_CACHED -> ITEM_NOT_CACHED;
            case ITEM_CACHED_AND_FRESH -> ITEM_CACHED_AND_FRESH;
        };
    }

    ItemLoadedStatus(boolean loaded) {
        this.loaded = loaded;
    }

    public boolean isLoaded() {
        return loaded;
    }
}
