/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action.upgrade.action;

import java.io.File;
import java.util.List;
import java.util.Set;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import com.evolveum.midpoint.ninja.util.EnumConverterValidator;
import com.evolveum.midpoint.ninja.util.ObjectTypesConverter;
import com.evolveum.midpoint.schema.constants.ObjectTypes;

@Parameters(resourceBundle = "messages", commandDescriptionKey = "initialObjects")
public class InitialObjectsOptions {

    public enum ReportStyle {
        DELTA,

        FULL_OBJECT,
    }

    public static class ReportStyleConverter extends EnumConverterValidator<ReportStyle> {

        public ReportStyleConverter() {
            super(ReportStyle.class);
        }
    }

    private static final String P_FILE_LONG = "--file";
    private static final String P_DRY_RUN = "--dry-run";
    public static final String P_OUTPUT = "-o";
    public static final String P_OUTPUT_LONG = "--output";
    public static final String P_OVERWRITE = "-O";
    public static final String P_OVERWRITE_LONG = "--overwrite";
    public static final String P_ZIP = "-z";
    public static final String P_ZIP_LONG = "--zip";
    public static final String P_REPORT = "-r";
    public static final String P_REPORT_LONG = "--report";
    public static final String P_FORCE_ADD_LONG = "--force-add";
    public static final String P_NO_MERGE = "--no-merge";
    public static final String P_REPORT_STYLE = "--report-style";
    public static final String P_TYPE_LONG = "--type";
    public static final String P_OID_LONG = "--oid";
    public static final String P_REVERSE_TYPE_FILTER_LONG = "--reverse-type-filter";
    public static final String P_REVERSE_OID_FILTIER_LONG = "--reverse-oid-filter";

    @Parameter(names = { P_FORCE_ADD_LONG }, descriptionKey = "initialObjects.forceAdd")
    private boolean forceAdd;

    @Parameter(names = { P_NO_MERGE }, descriptionKey = "initialObjects.noMerge")
    private boolean noMerge;

    @Parameter(names = { P_FILE_LONG }, descriptionKey = "initialObjects.file", variableArity = true)
    private List<File> files;

    @Parameter(names = { P_DRY_RUN }, descriptionKey = "initialObjects.dryRun")
    private boolean dryRun;

    @Parameter(names = { P_OUTPUT, P_OUTPUT_LONG }, descriptionKey = "initialObjects.output")
    private File output;

    @Parameter(names = { P_REPORT, P_REPORT_LONG }, descriptionKey = "initialObjects.report")
    private boolean report;

    @Parameter(names = { P_OVERWRITE, P_OVERWRITE_LONG }, descriptionKey = "initialObjects.overwrite")
    private boolean overwrite;

    @Parameter(names = { P_ZIP, P_ZIP_LONG }, descriptionKey = "initialObjects.zip")
    private boolean zip;

    @Parameter(names = { P_REPORT_STYLE }, descriptionKey = "initialObjects.reportStyle",
            converter = ReportStyleConverter.class, validateWith = ReportStyleConverter.class)
    private ReportStyle reportStyle = ReportStyle.DELTA;

    @Parameter(names = { P_TYPE_LONG }, descriptionKey = "initialObjects.type",
            validateWith = ObjectTypesConverter.class, converter = ObjectTypesConverter.class, variableArity = true)
    private Set<ObjectTypes> type;

    @Parameter(names = { P_OID_LONG }, descriptionKey = "initialObjects.oid", variableArity = true)
    private Set<String> oid;

    @Parameter(names = { P_REVERSE_TYPE_FILTER_LONG }, descriptionKey = "initialObjects.reverseTypeFilter")
    private boolean reverseTypeFilter;

    @Parameter(names = { P_REVERSE_OID_FILTIER_LONG }, descriptionKey = "initialObjects.reverseOidFilter")
    private boolean reverseOidFilter;

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public File getOutput() {
        return output;
    }

    public void setOutput(File output) {
        this.output = output;
    }

    public boolean isReport() {
        return report;
    }

    public void setReport(boolean report) {
        this.report = report;
    }

    public List<File> getFiles() {
        return files;
    }

    public void setFiles(List<File> files) {
        this.files = files;
    }

    public boolean isForceAdd() {
        return forceAdd;
    }

    public void setForceAdd(boolean forceAdd) {
        this.forceAdd = forceAdd;
    }

    public boolean isOverwrite() {
        return overwrite;
    }

    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    public boolean isZip() {
        return zip;
    }

    public void setZip(boolean zip) {
        this.zip = zip;
    }

    public boolean isNoMerge() {
        return noMerge;
    }

    public void setNoMerge(boolean noMerge) {
        this.noMerge = noMerge;
    }

    public ReportStyle getReportStyle() {
        return reportStyle;
    }

    public void setReportStyle(ReportStyle reportStyle) {
        this.reportStyle = reportStyle;
    }

    public Set<ObjectTypes> getType() {
        return type;
    }

    public void setType(Set<ObjectTypes> type) {
        this.type = type;
    }

    public Set<String> getOid() {
        return oid;
    }

    public void setOid(Set<String> oid) {
        this.oid = oid;
    }

    public boolean isReverseTypeFilter() {
        return reverseTypeFilter;
    }

    public void setReverseTypeFilter(boolean reverseTypeFilter) {
        this.reverseTypeFilter = reverseTypeFilter;
    }

    public boolean isReverseOidFilter() {
        return reverseOidFilter;
    }

    public void setReverseOidFilter(boolean reverseOidFilter) {
        this.reverseOidFilter = reverseOidFilter;
    }
}
