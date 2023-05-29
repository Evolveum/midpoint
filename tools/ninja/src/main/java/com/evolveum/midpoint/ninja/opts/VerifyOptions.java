/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.opts;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import java.io.File;

/**
 * @author semancik
 */
@Parameters(resourceBundle = "messages", commandDescriptionKey = "verify")
public class VerifyOptions extends ExportOptions {

    public static final String P_WARN = "-w";
    public static final String P_WARN_LONG = "--warn";

    public static final String P_CREATE_REPORT = "--create-report";

    @Parameter(names = {P_WARN, P_WARN_LONG}, descriptionKey = "verify.warn")
    private String warn;

    @Parameter(names = {P_CREATE_REPORT}, descriptionKey = "verify.createReport")
    private Boolean createReport;

    public String getWarn() {
        return warn;
    }

    public Boolean isCreateReport() {
        return createReport;
    }

    public VerifyOptions setWarn(String warn) {
        this.warn = warn;
        return this;
    }

    public VerifyOptions setCreateReport(Boolean createReport) {
        this.createReport = createReport;
        return this;
    }
}
