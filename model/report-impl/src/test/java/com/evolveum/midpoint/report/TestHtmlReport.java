/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author skublik
 */

public class TestHtmlReport extends BasicNewReportTest {

    private final static File TEST_REPORTS_HTML_DIR = new File(TEST_REPORTS_DIR,"html");

    public static final File REPORT_DASHBOARD_WITH_DEFAULT_COLUMN_FILE = new File(TEST_REPORTS_HTML_DIR, "report-dashboard-with-default-column.xml");
    public static final File REPORT_DASHBOARD_WITH_VIEW_FILE = new File(TEST_REPORTS_HTML_DIR, "report-dashboard-with-view.xml");
    public static final File REPORT_DASHBOARD_WITH_TRIPLE_VIEW_FILE = new File(TEST_REPORTS_HTML_DIR, "report-dashboard-with-triple-view.xml");
    public static final File REPORT_OBJECT_COLLECTION_WITH_DEFAULT_COLUMN_FILE = new File(TEST_REPORTS_HTML_DIR, "report-object-collection-with-default-column.xml");
    public static final File REPORT_OBJECT_COLLECTION_WITH_VIEW_FILE = new File(TEST_REPORTS_HTML_DIR, "report-object-collection-with-view.xml");
    public static final File REPORT_OBJECT_COLLECTION_WITH_DOUBLE_VIEW_FILE = new File(TEST_REPORTS_HTML_DIR, "report-object-collection-with-double-view.xml");
    public static final File REPORT_AUDIT_COLLECTION_WITH_DEFAULT_COLUMN_FILE = new File(TEST_REPORTS_HTML_DIR, "report-audit-collection-with-default-column.xml");
    public static final File REPORT_AUDIT_COLLECTION_WITH_VIEW_FILE = new File(TEST_REPORTS_HTML_DIR, "report-audit-collection-with-view.xml");
    public static final File REPORT_AUDIT_COLLECTION_WITH_DOUBLE_VIEW_FILE = new File(TEST_REPORTS_HTML_DIR, "report-audit-collection-with-double-view.xml");
    public static final File REPORT_OBJECT_COLLECTION_WITH_FILTER_FILE = new File(TEST_REPORTS_HTML_DIR, "report-object-collection-with-filter.xml");

    public static final String REPORT_DASHBOARD_WITH_DEFAULT_COLUMN_OID = "2b44aa2e-dd86-4842-bcf5-762c8a9a8582";
    public static final String REPORT_DASHBOARD_WITH_VIEW_OID = "2b44aa2e-dd86-4842-bcf5-762c8a9a8533";
    public static final String REPORT_DASHBOARD_WITH_TRIPLE_VIEW_OID = "2b87aa2e-dd86-4842-bcf5-76200a9a8533";
    public static final String REPORT_OBJECT_COLLECTION_WITH_DEFAULT_COLUMN_OID = "2b44aa2e-dd86-4842-bcf5-762c8a9a85ab";
    public static final String REPORT_OBJECT_COLLECTION_WITH_VIEW_OID = "2b44aa2e-dd86-4842-bcf5-762c8a9a85de";
    public static final String REPORT_OBJECT_COLLECTION_WITH_DOUBLE_VIEW_OID = "2b44aa2e-dd86-4842-bcf5-762c8a9a85ef";
    public static final String REPORT_AUDIT_COLLECTION_WITH_DEFAULT_COLUMN_OID = "2b44aa2e-dd86-4842-bcf5-762c8a9a85bc";
    public static final String REPORT_AUDIT_COLLECTION_WITH_VIEW_OID = "2b44aa2e-dd86-4842-bcf5-762c8a9a85cd";
    public static final String REPORT_AUDIT_COLLECTION_WITH_DOUBLE_VIEW_OID = "2b44aa2e-dd86-4842-bcf5-762c8a9a85fg";
    public static final String REPORT_OBJECT_COLLECTION_WITH_FILTER_OID = "2b44aa2e-dd86-4842-bcf5-762c8a9a85gh";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        importObjectFromFile(REPORT_DASHBOARD_WITH_DEFAULT_COLUMN_FILE, initResult);
        importObjectFromFile(REPORT_DASHBOARD_WITH_VIEW_FILE, initResult);
        importObjectFromFile(REPORT_DASHBOARD_WITH_TRIPLE_VIEW_FILE, initResult);
        importObjectFromFile(REPORT_OBJECT_COLLECTION_WITH_DEFAULT_COLUMN_FILE, initResult);
        importObjectFromFile(REPORT_OBJECT_COLLECTION_WITH_VIEW_FILE, initResult);
        importObjectFromFile(REPORT_OBJECT_COLLECTION_WITH_DOUBLE_VIEW_FILE, initResult);
        importObjectFromFile(REPORT_AUDIT_COLLECTION_WITH_DEFAULT_COLUMN_FILE, initResult);
        importObjectFromFile(REPORT_AUDIT_COLLECTION_WITH_VIEW_FILE, initResult);
        importObjectFromFile(REPORT_AUDIT_COLLECTION_WITH_DOUBLE_VIEW_FILE, initResult);
        importObjectFromFile(REPORT_OBJECT_COLLECTION_WITH_FILTER_FILE, initResult);
    }

    @Override
    protected String getDashboardReportWithTripleViewOid() {
        return REPORT_DASHBOARD_WITH_TRIPLE_VIEW_OID;
    }

    @Override
    protected String getDashboardReportWithViewOid() {
        return REPORT_DASHBOARD_WITH_VIEW_OID;
    }

    protected String getObjectCollectionReportWithDefaultColumnOid() {
        return REPORT_OBJECT_COLLECTION_WITH_DEFAULT_COLUMN_OID;
    }
    protected String getObjectCollectionReportWithViewOid() {
        return REPORT_OBJECT_COLLECTION_WITH_VIEW_OID;
    }
    protected String getObjectCollectionReportWithDoubleViewOid() {
        return REPORT_OBJECT_COLLECTION_WITH_DOUBLE_VIEW_OID;
    }

    protected String getAuditCollectionReportWithDefaultColumnOid() {
        return REPORT_AUDIT_COLLECTION_WITH_DEFAULT_COLUMN_OID;
    }
    protected String getAuditCollectionReportWithViewOid() {
        return REPORT_AUDIT_COLLECTION_WITH_VIEW_OID;
    }
    protected String getAuditCollectionReportWithDoubleViewOid() {
        return REPORT_AUDIT_COLLECTION_WITH_DOUBLE_VIEW_OID;
    }

    protected String getObjectCollectionReportWithFilterOid() {
        return REPORT_OBJECT_COLLECTION_WITH_FILTER_OID;
    }

    protected String getObjectCollectionReportWithFilterAndBasicCollectionOid() {
        return "";
    }

    @Override
    protected String getDashboardReportWithDefaultColumnOid() {
        return REPORT_DASHBOARD_WITH_DEFAULT_COLUMN_OID;
    }

    protected List<String> basicCheckOutputFile(PrismObject<ReportType> report) throws IOException, SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        List<String> lines = super.basicCheckOutputFile(report);

        if (lines.size() < 10) {
            fail("Html report CSV too short ("+lines.size()+" lines)");
        }
        return lines;
    }
}
