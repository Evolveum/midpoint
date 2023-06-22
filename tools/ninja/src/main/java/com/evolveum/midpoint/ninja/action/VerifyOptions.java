/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action;

import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import com.evolveum.midpoint.ninja.util.EnumConverterValidator;

/**
 * // todo fix these options, extending Export options is messing up help messages (it's using export.* keys)
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

    @Parameter(names = { P_VERIFICATION_CATEGORY_LONG }, descriptionKey = "verify.verificationCategory",
            converter = VerificationCategoryConverter.class, validateWith = VerificationCategoryConverter.class,
            variableArity = true)
    private List<VerificationCategory> verificationCategories = new ArrayList<>();

    @Parameter(names = { P_REPORT_STYLE }, descriptionKey = "verify.reportStyle",
            converter = ReportStyleConverter.class, validateWith = ReportStyleConverter.class)
    private ReportStyle reportStyle = ReportStyle.PLAIN;

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
}
