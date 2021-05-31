/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report;

import org.testng.annotations.Test;

import java.io.File;

/**
 * @author skublik
 */

public class TestCsvReportSafe extends TestCsvReport{

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_SAFE_FILE;
    }

    @Test( expectedExceptions = { AssertionError.class }, priority = 102)
    public void test102CreateAuditCollectionReportWithView() throws Exception {
        super.test102CreateAuditCollectionReportWithView();
    }
    @Test( expectedExceptions = { AssertionError.class }, priority = 103)
    public void test103CreateAuditCollectionReportWithDoubleView() throws Exception {
        super.test103CreateAuditCollectionReportWithDoubleView();
    }

    @Test( expectedExceptions = { AssertionError.class }, priority = 200)
    public void test200ImportReportForUser() throws Exception {
        super.test200ImportReportForUser();
    }

    @Test( expectedExceptions = { AssertionError.class }, priority = 202)
    public void test202ImportReportWithImportScript() throws Exception {
        super.test202ImportReportWithImportScript();
    }
}
