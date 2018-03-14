package com.evolveum.midpoint.ninja.opts;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import java.io.File;

/**
 * Created by Viliam Repan (lazyman).
 */
@Parameters(resourceBundle = "messages", commandDescriptionKey = "export")
public class ExportOptions extends BaseImportExportOptions {

    public static final String P_OUTPUT = "-O";
    public static final String P_OUTPUT_LONG = "--output";

    public static final String P_SPLIT = "-n";
    public static final String P_SPLIT_LONG = "-split";

    @Parameter(names = {P_OUTPUT, P_OUTPUT_LONG}, descriptionKey = "export.output")
    private File output;

    @Parameter(names = {P_SPLIT, P_SPLIT_LONG}, descriptionKey = "export.split")
    private boolean split;

    public File getOutput() {
        return output;
    }

    public boolean isSplit() {
        return split;
    }
}
