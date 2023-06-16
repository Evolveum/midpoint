package com.evolveum.midpoint.ninja.action;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(resourceBundle = "messages", commandDescriptionKey = "verifyFiles")
public class VerifyFilesOptions extends VerifyOptions {

    public static final String P_FILES = "--files";

    @Parameter(names = { P_FILES }, descriptionKey = "verifyFiles.files", variableArity = true)
    private List<File> files = new ArrayList<>();

    public List<File> getFiles() {
        return files;
    }

    public void setFiles(List<File> files) {
        this.files = files;
    }
}
