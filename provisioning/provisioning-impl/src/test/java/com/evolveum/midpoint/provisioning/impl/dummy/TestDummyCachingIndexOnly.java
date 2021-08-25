/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.dummy;

import com.evolveum.midpoint.repo.sql.SqlRepositoryServiceImpl;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.io.File;

public class TestDummyCachingIndexOnly extends TestDummyCaching {

    public static final File RESOURCE_DUMMY_FILE = new File(TestDummyCaching.TEST_DIR, "resource-dummy-index-only.xml");

    @Autowired
    @Qualifier("repositoryService") // we're downcasting here to known subtype
    private SqlRepositoryServiceImpl sqlRepositoryService;

    @Override
    protected File getResourceDummyFile() {
        return RESOURCE_DUMMY_FILE;
    }

    @Override
    protected boolean isWeaponIndexOnly() {
        return true;
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        // These are experimental features, so they need to be explicitly enabled. This will be eliminated later,
        // when we make them enabled by default.
        sqlRepositoryService.sqlConfiguration().setEnableIndexOnlyItems(true);
        sqlRepositoryService.sqlConfiguration().setEnableNoFetchExtensionValuesInsertion(true);
        sqlRepositoryService.sqlConfiguration().setEnableNoFetchExtensionValuesDeletion(true);
    }
}
