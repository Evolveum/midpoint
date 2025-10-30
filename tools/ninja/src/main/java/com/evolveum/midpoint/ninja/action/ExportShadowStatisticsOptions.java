package com.evolveum.midpoint.ninja.action;

import java.io.File;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(resourceBundle = "messages", commandDescriptionKey = "exportShadowStatistics")
public class ExportShadowStatisticsOptions implements BasicExportOptions {

    public static final String P_OUTPUT = "-o";
    public static final String P_OUTPUT_LONG = "--output";

    public static final String P_OVERWRITE = "-O";
    public static final String P_OVERWRITE_LONG = "--overwrite";

    @Parameter(names = { P_OUTPUT, P_OUTPUT_LONG }, descriptionKey = "export.output")
    private File output;

    @Parameter(names = { P_OVERWRITE, P_OVERWRITE_LONG }, descriptionKey = "export.overwrite")
    private boolean overwrite;


    @Override
    public File getOutput() {
        return output;
    }

    @Override
    public boolean isOverwrite() {
        return overwrite;
    }

    @Override
    public boolean isZip() {
        return false;
    }

    public ExportShadowStatisticsOptions setOutput(File output) {
        this.output = output;
        return this;
    }

    public ExportShadowStatisticsOptions setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
        return this;
    }

}
