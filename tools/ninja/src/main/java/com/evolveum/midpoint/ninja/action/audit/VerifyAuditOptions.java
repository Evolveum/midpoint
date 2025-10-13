/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.ninja.action.audit;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import com.evolveum.midpoint.ninja.action.BasicExportOptions;
import com.evolveum.midpoint.ninja.action.VerifyOptions;
import com.evolveum.midpoint.ninja.util.EnumConverterValidator;

import java.io.File;

@Parameters(resourceBundle = "messages", commandDescriptionKey = "verifyAudit")
public class VerifyAuditOptions extends BaseAuditImportExportOptions implements BasicExportOptions {

    public static final String P_OUTPUT = "-o";
    public static final String P_OUTPUT_LONG = "--output";

    public static final String P_OVERWRITE = "-O";
    public static final String P_OVERWRITE_LONG = "--overwrite";

    public static final String P_REPORT_STYLE = "--report-style";



    public enum ReportStyle {
        PLAIN,

        CSV
    }

    public static class ReportStyleConverter extends EnumConverterValidator<ReportStyle> {

        public ReportStyleConverter() {
            super(ReportStyle.class);
        }
    }

    @Parameter(names = { P_OUTPUT, P_OUTPUT_LONG }, descriptionKey = "export.output")
    private File output;

    @Parameter(names = { P_OVERWRITE, P_OVERWRITE_LONG }, descriptionKey = "export.overwrite")
    private boolean overwrite;

    @Parameter(names = { P_REPORT_STYLE }, descriptionKey = "verify.reportStyle",
            converter = ReportStyleConverter.class, validateWith = ReportStyleConverter.class)
    private ReportStyle reportStyle;

    public ReportStyle getReportStyle() {
        return reportStyle;
    }

    public File getOutput() {
        return output;
    }

    public boolean isOverwrite() {
        return overwrite;
    }

}
