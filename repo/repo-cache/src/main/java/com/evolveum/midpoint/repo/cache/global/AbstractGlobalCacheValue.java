/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.cache.global;

class AbstractGlobalCacheValue {

    /**
     * When the value was crated. Used only for diagnostic purposes.
     * (Cache eviction is managed by cache2k itself!)
     */
    private final long createdAt = System.currentTimeMillis();

    long getAge() {
        return System.currentTimeMillis() - createdAt;
    }
}
