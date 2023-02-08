/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report;

import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;

import org.testng.annotations.Test;

import java.io.File;

/**
 * @author skublik
 */

public class TestCsvReportExportClassicSafe extends TestCsvReportExportClassic {

    protected static final File SYSTEM_CONFIGURATION_SAFE_FILE = new File(TEST_DIR_COMMON, "system-configuration-safe.xml");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        try {
            repoAddObjectFromFile(SYSTEM_CONFIGURATION_SAFE_FILE, RepoAddOptions.createOverwrite(), false, initResult);
        } catch (ObjectAlreadyExistsException e) {
            throw new ObjectAlreadyExistsException("System configuration already exists in repository;" +
                    "looks like the previous test haven't cleaned it up", e);
        }
    }

    // TODO check for specific error message like
    //  "Access to Groovy method java.lang.System#setProperty denied (applied expression profile 'safe')"
    @Test(expectedExceptions = { AssertionError.class })
    public void test101AuditCollectionReportWithView() throws Exception {
        super.test101AuditCollectionReportWithView();
    }

    @Test(expectedExceptions = { AssertionError.class })
    public void test102AuditCollectionReportWithDoubleView() throws Exception {
        super.test102AuditCollectionReportWithDoubleView();
    }

    @Test(expectedExceptions = { AssertionError.class })
    @Override
    public void test130ExportUsersWithAssignments() throws Exception {
        super.test130ExportUsersWithAssignments();
    }

    @Test(expectedExceptions = { AssertionError.class })
    @Override
    public void test140ExportAuditRecords() throws Exception {
        super.test140ExportAuditRecords();
    }
}
