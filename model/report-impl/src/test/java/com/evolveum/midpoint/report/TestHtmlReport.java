/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

import java.io.File;

/**
 * @author skublik
 */

public class TestHtmlReport extends BasicNewReportTest {

    public static final File REPORT_DASHBOARD_WITH_DEFAULT_COLUMN_FILE = new File(TEST_REPOSTS_DIR, "report-dashboard-with-default-column.xml");
    public static final File REPORT_DASHBOARD_WITH_VIEW_FILE = new File(TEST_REPOSTS_DIR, "report-dashboard-with-view.xml");
    public static final File REPORT_DASHBOARD_WITH_TRIPLE_VIEW_FILE = new File(TEST_REPOSTS_DIR, "report-dashboard-with-triple-view.xml");

    public static final String REPORT_DASHBOARD_WITH_DEFAULT_COLUMN_OID = "2b44aa2e-dd86-4842-bcf5-762c8a9a8582";
    public static final String REPORT_DASHBOARD_WITH_VIEW_OID = "2b44aa2e-dd86-4842-bcf5-762c8a9a8533";
    public static final String REPORT_DASHBOARD_WITH_TRIPLE_VIEW_OID = "2b87aa2e-dd86-4842-bcf5-76200a9a8533";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        importObjectFromFile(REPORT_DASHBOARD_WITH_DEFAULT_COLUMN_FILE, initResult);
        importObjectFromFile(REPORT_DASHBOARD_WITH_VIEW_FILE, initResult);
        importObjectFromFile(REPORT_DASHBOARD_WITH_TRIPLE_VIEW_FILE, initResult);
    }

    @Override
    protected String getDashboardReportWithTripleViewOid() {
        return REPORT_DASHBOARD_WITH_TRIPLE_VIEW_OID;
    }

    @Override
    protected String getDashboardReportWithViewOid() {
        return REPORT_DASHBOARD_WITH_VIEW_OID;
    }

    @Override
    protected String getDashboardReportWithDefaultColumnOid() {
        return REPORT_DASHBOARD_WITH_DEFAULT_COLUMN_OID;
    }
}
