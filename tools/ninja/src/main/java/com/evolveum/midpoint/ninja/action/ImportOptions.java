/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action;

import java.io.File;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Created by Viliam Repan (lazyman).
 */
@Parameters(resourceBundle = "messages", commandDescriptionKey = "import")
public class ImportOptions extends BaseImportExportOptions implements BasicImportOptions {

    public static final String P_INPUT = "-i";
    public static final String P_INPUT_LONG = "--input";

    public static final String P_OVERWRITE = "-O";
    public static final String P_OVERWRITE_LONG = "--overwrite";

    public static final String P_ALLOW_UNENCRYPTED_VALUES = "-e";
    public static final String P_ALLOW_UNENCRYPTED_VALUES_LONG = "--allow-unencrypted-values";

    public static final String P_CONTINUE_ON_INPUT_ERROR_LONG = "--continue-on-input-error";

    @Parameter(names = { P_INPUT, P_INPUT_LONG }, descriptionKey = "import.input")
    private File input;

    @Parameter(names = { P_OVERWRITE, P_OVERWRITE_LONG }, descriptionKey = "import.overwrite")
    private boolean overwrite;

    @Parameter(names = { P_ALLOW_UNENCRYPTED_VALUES, P_ALLOW_UNENCRYPTED_VALUES_LONG },
            descriptionKey = "import.allowUnencryptedValues")
    private boolean allowUnencryptedValues;

    @Parameter(names = { P_CONTINUE_ON_INPUT_ERROR_LONG }, descriptionKey = "import.continueOnInputError")
    private boolean continueOnInputError;

    @Override
    public File getInput() {
        return input;
    }

    @Override
    public boolean isOverwrite() {
        return overwrite;
    }

    public boolean isAllowUnencryptedValues() {
        return allowUnencryptedValues;
    }

    public boolean isContinueOnInputError() {
        return continueOnInputError;
    }

}
