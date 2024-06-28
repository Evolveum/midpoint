/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.reports;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.report.api.ReportManager;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxDownloadBehaviorFromStream;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FileFormatTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportDataType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class ReportDownloadHelper implements Serializable {

    private static final Trace LOGGER = TraceManager.getTrace(ReportDownloadHelper.class);
    private static final String DOT_CLASS = ReportDownloadHelper.class.getName() + ".";
    private static final String OPERATION_DOWNLOAD_REPORT = DOT_CLASS + ".downloadReport";

    private static final Map<FileFormatTypeType, String> REPORT_EXPORT_TYPE_MAP = new HashMap<>();
    static {
        REPORT_EXPORT_TYPE_MAP.put(FileFormatTypeType.CSV, "text/csv; charset=UTF-8");
        REPORT_EXPORT_TYPE_MAP.put(FileFormatTypeType.HTML, "text/html; charset=UTF-8");

    }

    public static AjaxDownloadBehaviorFromStream createAjaxDownloadBehaviorFromStream(ReportDataType reportObj, PageBase pageBase) {
        return new AjaxDownloadBehaviorFromStream() {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected InputStream initStream() {
                return createReport(reportObj, this, pageBase);
            }

            @Override
            public String getFileName() {
                return getReportFileName(reportObj);
            }
        };

    }

    public static String getReportFileName(ReportDataType currentReport) {
        try {
            String filePath = currentReport.getFilePath();
            if (filePath != null) {
                var fileName = new File(filePath).getName();
                if (StringUtils.isNotEmpty(fileName)) {
                    return fileName;
                }
            }
        } catch (RuntimeException ex) {
            // ignored
        }
        return "report"; // A fallback - this should not really occur
    }

    public static InputStream createReport(ReportDataType report,
            AjaxDownloadBehaviorFromStream ajaxDownloadBehaviorFromStream, PageBase pageBase) {
        OperationResult result = new OperationResult(OPERATION_DOWNLOAD_REPORT);
        ReportManager reportManager = pageBase.getReportManager();

        if (report == null) {
            return null;
        }

        String contentType = REPORT_EXPORT_TYPE_MAP.get(report.getFileFormat());
        if (StringUtils.isEmpty(contentType)) {
            contentType = "multipart/mixed; charset=UTF-8";
        }
        ajaxDownloadBehaviorFromStream.setContentType(contentType);

        InputStream input = null;
        try {
            input = reportManager.getReportDataStream(report.getOid(), result);
        } catch (IOException ex) {
            LOGGER.error("Report {} is not accessible.", WebComponentUtil.getName(report));
            result.recordPartialError("Report " + WebComponentUtil.getName(report) + " is not accessible.");
        } catch (Exception e) {
            pageBase.error(pageBase.getString("pageCreatedReports.message.downloadError") + " " + e.getMessage());
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't download report.", e);
            LOGGER.trace(result.debugDump());
        } finally {
            result.computeStatusIfUnknown();
        }

        if (WebComponentUtil.showResultInPage(result)) {
            pageBase.showResult(result);
        }

        return input;
    }

    public static void downloadPerformed(
            AjaxRequestTarget target, AjaxDownloadBehaviorFromStream ajaxDownloadBehavior) {
        ajaxDownloadBehavior.initiate(target);
    }

}
