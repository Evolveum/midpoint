/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.helpers;

import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.SystemException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

/**
 * Works around the inability of H2 database to correctly serialize concurrent object modifications
 * by providing explicit per-OID locking.
 *
 * Assumes that there is only a single midPoint instance running, therefore local locking is sufficient.
 *
 * NOT TO BE USED IN PRODUCTION.
 * CALLERS: MAKE SURE YOU USE IT ONLY WITH CONNECTION TO H2 DATABASE.
 */
@Experimental
class ExplicitAccessLock {

    private static final Map<String, Semaphore> SEMAPHORE_MAP = new ConcurrentHashMap<>();

    private final Semaphore semaphore;

    private ExplicitAccessLock(Semaphore semaphore) {
        this.semaphore = semaphore;
    }

    static ExplicitAccessLock acquireFor(String oid) {
        Semaphore semaphore = SEMAPHORE_MAP.computeIfAbsent(oid, s -> new Semaphore(1));
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            throw new SystemException("Unexpected InterruptedException", e);
        }
        return new ExplicitAccessLock(semaphore);
    }

    void release() {
        semaphore.release();
    }
}
