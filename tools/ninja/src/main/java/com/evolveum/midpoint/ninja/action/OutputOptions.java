package com.evolveum.midpoint.ninja.action;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import java.io.File;

@Parameters(resourceBundle = "messages")
public class OutputOptions {

    public static final String P_OUTPUT = "-o";
    public static final String P_OUTPUT_LONG = "--output";

    public static final String P_OVERWRITE = "-O";
    public static final String P_OVERWRITE_LONG = "--overwrite";
    public static final String P_ZIP = "-z";
    public static final String P_ZIP_LONG = "--zip";

    @Parameter(names = { P_OUTPUT, P_OUTPUT_LONG }, descriptionKey = "output.output")
    private File output;

    @Parameter(names = { P_OVERWRITE, P_OVERWRITE_LONG }, descriptionKey = "output.overwrite")
    private boolean overwrite;

    @Parameter(names = { P_ZIP, P_ZIP_LONG }, descriptionKey = "baseImportExport.zip")
    private boolean zip;

    public File getOutput() {
        return output;
    }

    public void setOutput(File output) {
        this.output = output;
    }

    public boolean isOverwrite() {
        return overwrite;
    }

    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    public boolean isZip() {
        return zip;
    }

    public void setZip(boolean zip) {
        this.zip = zip;
    }
}
