/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.ninja.util.EnumConverterValidator;

/**
 * // todo fix these options, extending Export options is messing up help messages (it's using export.* keys)
 *
 * @author semancik
 */
@Parameters(resourceBundle = "messages", commandDescriptionKey = "verify")
public class VerifyOptions extends ExportOptions {

    public enum ReportStyle {
        PLAIN,

        CSV
    }

    public static class ReportStyleConverter extends EnumConverterValidator<ReportStyle> {

        public ReportStyleConverter() {
            super(VerifyOptions.ReportStyle.class);
        }
    }

    public enum VerificationCategory {

        DEPRECATED,

        REMOVED,

        PLANNED_REMOVAL,

        INCORRECT_OIDS
    }

    public static class VerificationCategoryConverter extends EnumConverterValidator<VerificationCategory> {

        public VerificationCategoryConverter() {
            super(VerificationCategory.class);
        }
    }

    public static final String P_VERIFICATION_CATEGORY_LONG = "--verification-category";

    public static final String P_REPORT_STYLE = "--report-style";

    public static final String P_CONTINUE_VERIFICATION_ON_ERROR = "--continue-verification-on-error";

    public static final String P_FILES = "--files";
    public static final String P_PLANNED_REMOVAL_VERSION = "--planned-removal-version";

    @Parameter(names = { P_VERIFICATION_CATEGORY_LONG }, descriptionKey = "verify.verificationCategory",
            converter = VerificationCategoryConverter.class, validateWith = VerificationCategoryConverter.class,
            variableArity = true)
    private List<VerificationCategory> verificationCategories = new ArrayList<>();

    @Parameter(names = { P_REPORT_STYLE }, descriptionKey = "verify.reportStyle",
            converter = ReportStyleConverter.class, validateWith = ReportStyleConverter.class)
    private ReportStyle reportStyle = ReportStyle.PLAIN;

    @Parameter(names = { P_CONTINUE_VERIFICATION_ON_ERROR }, descriptionKey = "verify.continueVerificationOnError")
    private boolean continueVerificationOnError = true;

    @Parameter(names = { P_FILES }, descriptionKey = "verify.files", variableArity = true)
    private List<File> files = new ArrayList<>();

    @Parameter(names = { P_PLANNED_REMOVAL_VERSION }, descriptionKey = "verify.plannedRemovalVersion")
    private String plannedRemovalVersion;

    public List<VerificationCategory> getVerificationCategories() {
        return verificationCategories;
    }

    public void setVerificationCategories(List<VerificationCategory> verificationCategories) {
        this.verificationCategories = verificationCategories;
    }

    public ReportStyle getReportStyle() {
        return reportStyle;
    }

    public void setReportStyle(ReportStyle reportStyle) {
        this.reportStyle = reportStyle;
    }

    // todo make use of this
    public boolean isContinueVerificationOnError() {
        return continueVerificationOnError;
    }

    public void setContinueVerificationOnError(boolean continueVerificationOnError) {
        this.continueVerificationOnError = continueVerificationOnError;
    }

    @NotNull
    public List<File> getFiles() {
        if (files == null) {
            files = new ArrayList<>();
        }
        return files;
    }

    public void setFiles(List<File> files) {
        this.files = files;
    }

    public String getPlannedRemovalVersion() {
        return plannedRemovalVersion;
    }

    public void setPlannedRemovalVersion(String plannedRemovalVersion) {
        this.plannedRemovalVersion = plannedRemovalVersion;
    }
}
