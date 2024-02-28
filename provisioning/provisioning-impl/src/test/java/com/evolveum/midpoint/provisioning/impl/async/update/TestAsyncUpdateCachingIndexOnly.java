/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.async.update;

import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.sql.SqlRepositoryServiceImpl;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

public class TestAsyncUpdateCachingIndexOnly extends TestAsyncUpdateCaching {

    static final File RESOURCE_ASYNC_CACHING_INDEX_ONLY_FILE =
            new File(TEST_DIR, "resource-async-caching-index-only.xml");

    @Autowired
    @Qualifier("repositoryService") // we want repo implementation, not cache
    private RepositoryService repositoryService;

    @Override
    protected File getResourceFile() {
        return RESOURCE_ASYNC_CACHING_INDEX_ONLY_FILE;
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        if (repositoryService instanceof SqlRepositoryServiceImpl sqlRepositoryService) {
            // These are experimental features, so they need to be explicitly enabled.
            // This will be eliminated later, when we make them enabled by default.
            sqlRepositoryService.sqlConfiguration().setEnableIndexOnlyItems(true);
            sqlRepositoryService.sqlConfiguration().setEnableNoFetchExtensionValuesInsertion(true);
            sqlRepositoryService.sqlConfiguration().setEnableNoFetchExtensionValuesDeletion(true);
        } else {
            // It's Sqale repo and it has no explicit switch for index only.
        }

        super.initSystem(initTask, initResult);
    }

}
