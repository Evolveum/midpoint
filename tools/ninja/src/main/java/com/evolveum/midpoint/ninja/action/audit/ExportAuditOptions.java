/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action.audit;

import java.io.File;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import com.evolveum.midpoint.ninja.opts.BasicExportOptions;

@Parameters(resourceBundle = "messages", commandDescriptionKey = "exportAudit")
public class ExportAuditOptions extends BaseAuditImportExportOptions implements BasicExportOptions {

    public static final String P_OUTPUT = "-o";
    public static final String P_OUTPUT_LONG = "--output";

    public static final String P_OVERWRITE = "-O";
    public static final String P_OVERWRITE_LONG = "--overwrite";

    @Parameter(names = { P_OUTPUT, P_OUTPUT_LONG }, descriptionKey = "export.output")
    private File output;

    @Parameter(names = { P_OVERWRITE, P_OVERWRITE_LONG }, descriptionKey = "export.overwrite")
    private boolean overwrite;

    public File getOutput() {
        return output;
    }

    public boolean isOverwrite() {
        return overwrite;
    }
}
