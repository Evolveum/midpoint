/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class ReportDownloadHelper implements Serializable {

    private static final Trace LOGGER = TraceManager.getTrace(ReportDownloadHelper.class);
    private static final String DOT_CLASS = ReportDownloadHelper.class.getName() + ".";
    private static final String OPERATION_DOWNLOAD_REPORT = DOT_CLASS + ".downloadReport";
    private static final String ZIP_EXTENSION = "zip";

    private static final Map<FileFormatTypeType, String> REPORT_EXPORT_TYPE_MAP = new HashMap<>();
    static {
        REPORT_EXPORT_TYPE_MAP.put(FileFormatTypeType.CSV, "text/csv; charset=UTF-8");
        REPORT_EXPORT_TYPE_MAP.put(FileFormatTypeType.HTML, "text/html; charset=UTF-8");

    }

    public static AjaxDownloadBehaviorFromStream createAjaxDownloadBehaviorFromStream(ReportDataType reportObj, PageBase pageBase) {
        return new AjaxDownloadBehaviorFromStream() {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected InputStream getInputStream() {
                return createReport(reportObj, this, pageBase);
            }

            @Override
            public String getFileName() {
                return getReportFileName(reportObj);
            }
        };

    }

    public static String getReportFileName(ReportDataType currentReport) {
        String name = WebComponentUtil.getName(currentReport);
        if (StringUtils.isEmpty(name)) {
            name = "report"; // A fallback - this should not really occur
        }

        // Check if the file name contains the extension
        String extension = getReportExtension(currentReport);
        String dotExtension = extension != null ? "." + extension : null;
        if (StringUtils.isNotEmpty(dotExtension) && !name.endsWith(dotExtension)) {
            name = name + dotExtension;
        }

        // Sanitize to remove any path components for defense in depth
        // (browsers also ignore path components, but better to be safe)
        String sanitizedName = FilenameUtils.getName(name);
        if (isZipReport(currentReport) && !hasZipExtension(sanitizedName)) {
            return sanitizedName + "." + ZIP_EXTENSION;
        }
        return sanitizedName;
    }

    private static boolean isZipReport(ReportDataType report) {
        // ReportDataType has no ZIP file-format enum value, tracing stores ZIP outputs as files with .zip suffix.
        return report != null
                && report.getFilePath() != null
                && hasZipExtension(report.getFilePath());
    }

    private static boolean hasZipExtension(String fileName) {
        return ZIP_EXTENSION.equalsIgnoreCase(FilenameUtils.getExtension(fileName));
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

    private static String getReportExtension(ReportDataType report) {
        String filePath = report.getFilePath();
        FileFormatTypeType fileFormat = report.getFileFormat();

        String extension;
        if (fileFormat != null) {
            extension = fileFormat.value().toLowerCase();
        } else {
            extension = FilenameUtils.getExtension(filePath);
        }
        return extension;
    }

}
