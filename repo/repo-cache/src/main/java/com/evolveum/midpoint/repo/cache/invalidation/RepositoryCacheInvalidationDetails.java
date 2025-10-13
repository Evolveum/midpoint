/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
