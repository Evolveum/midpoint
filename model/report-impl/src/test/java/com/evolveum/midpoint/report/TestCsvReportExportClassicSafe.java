/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.report;

import java.io.File;

import org.testng.annotations.Test;

import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;

/**
 * @author skublik
 */
public class TestCsvReportExportClassicSafe extends TestCsvReportExportClassic {

    private static final File SYSTEM_CONFIGURATION_SAFE_FILE = new File(TEST_DIR_COMMON, "system-configuration-safe.xml");

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
    public void test140ExportAuditRecords() throws Exception {
        super.test140ExportAuditRecords();
    }
}
