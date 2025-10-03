package com.evolveum.midpoint.ninja.action;

import com.beust.jcommander.Parameter;

import com.evolveum.midpoint.ninja.util.FileReference;
import com.evolveum.midpoint.ninja.util.FileReferenceConverter;

import java.io.File;
import java.io.Serializable;

public class ExportShadowStatisticsOptions implements BasicExportOptions {


    public static final String P_OUTPUT = "-o";
    public static final String P_OUTPUT_LONG = "--output";

    public static final String P_OVERWRITE = "-O";
    public static final String P_OVERWRITE_LONG = "--overwrite";


    public static final String P_FILTER = "-f";
    public static final String P_FILTER_LONG = "--filter";

    @Parameter(names = { P_OUTPUT, P_OUTPUT_LONG }, descriptionKey = "export.output")
    private File output;

    @Parameter(names = { P_OVERWRITE, P_OVERWRITE_LONG }, descriptionKey = "export.overwrite")
    private boolean overwrite;


    @Parameter(names = {P_FILTER, P_FILTER_LONG}, descriptionKey = "base.filter",
            converter = FileReferenceConverter.class, validateWith = FileReferenceConverter.class)
    private FileReference filter;


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

    public FileReference getFilter() {
        return filter;
    }

    public ExportShadowStatisticsOptions setFilter(FileReference filter) {
        this.filter = filter;
        return this;
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
