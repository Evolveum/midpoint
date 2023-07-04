package com.evolveum.midpoint.ninja.action.upgrade.action;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.ninja.action.ExportOptions;

/**
 * todo cleanup options, use OutputOptions, SearchOptions etc. Get rid of inheritance, replace with composition
 */
@Parameters(resourceBundle = "messages", commandDescriptionKey = "upgradeObjects")
public class UpgradeObjectsOptions extends ExportOptions {

    public static final String P_VERIFICATION_LONG = "--verification-file";
    public static final String P_FILES = "--files";

//    @ParametersDelegate
//    private OutputOptions outputOptions = new OutputOptions();
//
//    @ParametersDelegate
//    private SearchOptions searchOptions = new SearchOptions();

    @Parameter(names = { P_VERIFICATION_LONG }, descriptionKey = "upgradeObjects.verification")
    private File verification;

    @Parameter(names = { P_FILES }, descriptionKey = "upgradeObjects.files", variableArity = true)
    private List<File> files = new ArrayList<>();

    public File getVerification() {
        return verification;
    }

    public void setVerification(File verification) {
        this.verification = verification;
    }

//    public OutputOptions getOutputOptions() {
//        return outputOptions;
//    }
//
//    public void setOutputOptions(OutputOptions outputOptions) {
//        this.outputOptions = outputOptions;
//    }
//
//    public SearchOptions getSearchOptions() {
//        return searchOptions;
//    }
//
//    public void setSearchOptions(SearchOptions searchOptions) {
//        this.searchOptions = searchOptions;
//    }

    @NotNull
    public List<File> getFiles() {
        if (files == null) {
            files = new ArrayList<>();
        }
        return files;
    }

    public void setFiles(List<File> files) {
        this.files = files;
    }
}
