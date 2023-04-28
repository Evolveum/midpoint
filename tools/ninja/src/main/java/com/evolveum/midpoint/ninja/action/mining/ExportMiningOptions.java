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

    public static final String P_KEY = "-k";

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
    public static final String P_KEY_LONG = "--key";

    public static final String P_GENERATE_KEY = "-rk";

    @Parameter(names = { P_OUTPUT, P_OUTPUT_LONG }, descriptionKey = "export.output")
    private File output;

    @Parameter(names = { P_OVERWRITE, P_OVERWRITE_LONG }, descriptionKey = "export.overwrite")
    private boolean overwrite;

    @Parameter(names = { P_KEY, P_KEY_LONG }, descriptionKey = "export.key")
    private String key;

    @Parameter(names = { P_GENERATE_KEY }, descriptionKey = "export.randomKey")
    private boolean randomKey;

    public String getKey() {
        return key;
    }

    public File getOutput() {
        return output;
    }

    public boolean isOverwrite() {
        return overwrite;
    }

    public boolean isRandomKey() {
        return randomKey;
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
