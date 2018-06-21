package com.evolveum.midpoint.ninja.opts;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import java.io.File;

/**
 * Created by Viliam Repan (lazyman).
 */
@Parameters(resourceBundle = "messages", commandDescriptionKey = "import")
public class ImportOptions extends BaseImportExportOptions {

    public static final String P_INPUT = "-i";
    public static final String P_INPUT_LONG = "--input";

    public static final String P_OVERWRITE = "-O";
    public static final String P_OVERWRITE_LONG = "--overwrite";

    public static final String P_ALLOW_UNENCRYPTED_VALUES = "-e";
    public static final String P_ALLOW_UNENCRYPTED_VALUES_LONG = "--allowUnencryptedValues";

    @Parameter(names = {P_INPUT, P_INPUT_LONG}, descriptionKey = "import.input")
    private File input;

    @Parameter(names = {P_OVERWRITE, P_OVERWRITE_LONG}, descriptionKey = "import.overwrite")
    private boolean overwrite;

    @Parameter(names = {P_ALLOW_UNENCRYPTED_VALUES, P_ALLOW_UNENCRYPTED_VALUES_LONG},
            descriptionKey = "import.allowUnencryptedValues")
    private boolean allowUnencryptedValues;

    public File getInput() {
        return input;
    }

    public boolean isOverwrite() {
        return overwrite;
    }

    public boolean isAllowUnencryptedValues() {
        return allowUnencryptedValues;
    }
}
