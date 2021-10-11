/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.async;

import com.evolveum.midpoint.repo.sql.SqlRepositoryServiceImpl;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.io.File;

/**
 *
 */
public class TestAsyncUpdateCachingIndexOnly extends TestAsyncUpdateCaching {

    @Autowired
    @Qualifier("sqlRepositoryServiceImpl")
    private SqlRepositoryServiceImpl sqlRepositoryService;

    @Override
    protected File getResourceFile() {
        return RESOURCE_ASYNC_CACHING_INDEX_ONLY_FILE;
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        // These are experimental features, so they need to be explicitly enabled. This will be eliminated later,
        // when we make them enabled by default.
        sqlRepositoryService.getConfiguration().setEnableIndexOnlyItems(true);
        sqlRepositoryService.getConfiguration().setEnableNoFetchExtensionValuesInsertion(true);
        sqlRepositoryService.getConfiguration().setEnableNoFetchExtensionValuesDeletion(true);

        super.initSystem(initTask, initResult);
    }

}
