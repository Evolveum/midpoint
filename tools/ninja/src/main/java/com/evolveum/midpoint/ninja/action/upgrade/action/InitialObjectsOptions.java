/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action.upgrade.action;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(resourceBundle = "messages", commandDescriptionKey = "initialObjects")
public class InitialObjectsOptions {

    private static final String P_FILES = "-f";
    private static final String P_FILES_LONG = "--files";
    private static final String P_DRY_RUN = "--dry-run";
    public static final String P_OUTPUT = "-o";
    public static final String P_OUTPUT_LONG = "--output";
    public static final String P_REPORT = "-r";
    public static final String P_REPORT_LONG = "--report";

    @Parameter(names = { P_FILES, P_FILES_LONG }, descriptionKey = "initialObjects.files")
    private List<File> files;

    @Parameter(names = { P_DRY_RUN }, descriptionKey = "initialObjects.dryRun")
    private boolean dryRun;

    @Parameter(names = { P_OUTPUT, P_OUTPUT_LONG }, descriptionKey = "initialObjects.output")
    private File output;

    @Parameter(names = { P_REPORT, P_REPORT_LONG }, descriptionKey = "initialObjects.report")
    private boolean report;

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
}
