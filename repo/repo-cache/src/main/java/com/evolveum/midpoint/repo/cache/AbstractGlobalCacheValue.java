/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.cache;

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
