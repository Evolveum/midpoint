/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.cache.invalidation;

import com.evolveum.midpoint.repo.api.CacheInvalidationDetails;
import com.evolveum.midpoint.repo.api.RepositoryOperationResult;
import com.evolveum.midpoint.util.annotation.Experimental;

/**
 * TODO
 */
@Experimental
public final class RepositoryCacheInvalidationDetails implements CacheInvalidationDetails {
    private final RepositoryOperationResult details;

    RepositoryCacheInvalidationDetails(RepositoryOperationResult details) {
        this.details = details;
    }

    @Deprecated
    public Object getObject() {
        return details;
    }

    public RepositoryOperationResult getResult() {
        return details;
    }
}
