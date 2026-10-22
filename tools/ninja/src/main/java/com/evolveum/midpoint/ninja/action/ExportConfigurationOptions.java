package com.evolveum.midpoint.ninja.action;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(resourceBundle = "messages", commandDescriptionKey = "exportConfiguration")
public class ExportConfigurationOptions extends ExportOptions {

    public static final String P_SPLIT_FILES = "-sf";
    public static final String P_SPLIT_FILES_LONG = "--split-files";

    @Parameter(names = { P_SPLIT_FILES, P_SPLIT_FILES_LONG }, descriptionKey = "split.files")
    private boolean splitFiles;

    public boolean isSplitFiles() { return splitFiles; }

    public ExportOptions setSplitFiles(boolean splitFiles) {
        this.splitFiles = splitFiles;
        return this;
    }

}
