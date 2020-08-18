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

    @Test( expectedExceptions = { AssertionError.class } )
    public void test200ImportReportForUser() throws Exception {
        super.test200ImportReportForUser();
    }

    @Test( expectedExceptions = { AssertionError.class } )
    public void test202ImportReportWithImportScript() throws Exception {
        super.test202ImportReportWithImportScript();
    }
}
