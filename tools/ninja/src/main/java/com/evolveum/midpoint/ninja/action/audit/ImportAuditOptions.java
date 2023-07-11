/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action.audit;

import java.io.File;
import java.util.Set;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import com.evolveum.midpoint.ninja.action.BasicImportOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;

@Parameters(resourceBundle = "messages", commandDescriptionKey = "importAudit")
public class ImportAuditOptions extends BaseAuditImportExportOptions implements BasicImportOptions {

    public static final String P_INPUT = "-i";
    public static final String P_INPUT_LONG = "--input";

    public static final String P_OVERWRITE = "-O";
    public static final String P_OVERWRITE_LONG = "--overwrite";

    @Parameter(names = { P_INPUT, P_INPUT_LONG }, descriptionKey = "import.input")
    private File input;

    @Parameter(names = { P_OVERWRITE, P_OVERWRITE_LONG }, descriptionKey = "import.overwrite")
    private boolean overwrite;

    public File getInput() {
        return input;
    }

    public boolean isOverwrite() {
        return overwrite;
    }

    @Override
    public Set<ObjectTypes> getType() {
        return Set.of();
    }
}
