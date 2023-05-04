/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action.mining;

import java.io.File;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import com.evolveum.midpoint.ninja.opts.BasicExportOptions;

@Parameters(resourceBundle = "messages", commandDescriptionKey = "exportMining")
public class ExportMiningOptions extends BaseMiningOptions implements BasicExportOptions {

    public static final String P_OUTPUT = "-O";
    public static final String P_OUTPUT_LONG = "--output";

    public static final String P_OVERWRITE = "-ow";
    public static final String P_OVERWRITE_LONG = "--overwrite";

    public static final String P_PREFIX_APPLICATION = "-pa";

    @Parameter(names = { P_PREFIX_APPLICATION }, descriptionKey = "export.application.role.prefix")
    private String applicationRolePrefix;

    public static final String P_PREFIX_BUSINESS = "-pb";

    @Parameter(names = { P_PREFIX_BUSINESS }, descriptionKey = "export.business.role.prefix")
    private String businessRolePrefix;

    public static final String P_SUFFIX_APPLICATION = "-sa";

    @Parameter(names = { P_SUFFIX_APPLICATION }, descriptionKey = "export.application.role.suffix")
    private String applicationRoleSuffix;
    public static final String P_SUFFIX_BUSINESS = "-sb";

    @Parameter(names = { P_SUFFIX_BUSINESS }, descriptionKey = "export.business.role.suffix")
    private String businessRoleSuffix;

    @Parameter(names = { P_OUTPUT, P_OUTPUT_LONG }, descriptionKey = "export.output")
    private File output;

    @Parameter(names = { P_OVERWRITE, P_OVERWRITE_LONG }, descriptionKey = "export.overwrite")
    private boolean overwrite;

    public static final String P_ORG = "-org";

    @Parameter(names = { P_ORG }, descriptionKey = "export.prevent.org")
    private boolean notIncludeOrg;

    public boolean isNotIncludeOrg() {
        return notIncludeOrg;
    }

    public static final String P_NAME_OPTIONS = "-nop";

    @Parameter(names = { P_NAME_OPTIONS }, descriptionKey = "export.name.options")
    private String nameMode;

    public String getNameMode() {
        return nameMode;
    }

    public File getOutput() {
        return output;
    }

    public boolean isOverwrite() {
        return overwrite;
    }

    public String getApplicationRolePrefix() {
        return applicationRolePrefix;
    }

    public String getBusinessRolePrefix() {
        return businessRolePrefix;
    }

    public String getApplicationRoleSuffix() {
        return applicationRoleSuffix;
    }

    public String getBusinessRoleSuffix() {
        return businessRoleSuffix;
    }
}
