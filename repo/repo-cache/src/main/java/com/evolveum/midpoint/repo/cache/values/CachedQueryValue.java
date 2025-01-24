/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.cache.values;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.SearchResultList;

import static com.evolveum.midpoint.repo.cache.handlers.SearchOpHandler.QUERY_RESULT_SIZE_LIMIT;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

/** Value stored in both local and global query caches. */
public interface CachedQueryValue {

    @NotNull SearchResultList<String> getOidOnlyResult();

    default int size() {
        return getOidOnlyResult().size();
    }

    static void checkConsistency(SearchResultList<String> oidOnlyResult) {
        oidOnlyResult.checkImmutable();
        stateCheck(oidOnlyResult.size() <= QUERY_RESULT_SIZE_LIMIT,
                "Trying to cache result list greater than %s: %s", QUERY_RESULT_SIZE_LIMIT, oidOnlyResult.size());
    }
}
