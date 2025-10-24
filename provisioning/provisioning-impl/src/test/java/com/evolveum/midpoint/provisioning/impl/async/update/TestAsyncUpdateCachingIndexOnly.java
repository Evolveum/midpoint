/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.impl.async.update;

import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.evolveum.midpoint.repo.api.RepositoryService;

public class TestAsyncUpdateCachingIndexOnly extends TestAsyncUpdateCaching {

    private static final File RESOURCE_ASYNC_CACHING_INDEX_ONLY_FILE =
            new File(TEST_DIR, "resource-async-caching-index-only.xml");

    @Autowired
    @Qualifier("repositoryService") // we want repo implementation, not cache
    private RepositoryService repositoryService;

    @Override
    protected File getResourceFile() {
        return RESOURCE_ASYNC_CACHING_INDEX_ONLY_FILE;
    }
}
