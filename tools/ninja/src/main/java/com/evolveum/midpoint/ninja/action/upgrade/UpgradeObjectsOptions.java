package com.evolveum.midpoint.ninja.action.upgrade;

import java.io.File;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;

import com.evolveum.midpoint.ninja.action.ExportOptions;
import com.evolveum.midpoint.ninja.action.OutputOptions;
import com.evolveum.midpoint.ninja.action.SearchOptions;

/**
 * todo cleanup options, use OutputOptions, SearchOptions etc. Get rid of inheritance, replace with composition
 */
@Parameters(resourceBundle = "messages", commandDescriptionKey = "upgradeObjects")
public class UpgradeObjectsOptions extends ExportOptions {

    public static final String P_VERIFICATION_LONG = "--verification-file";

    @ParametersDelegate
    private OutputOptions outputOptions = new OutputOptions();

    @ParametersDelegate
    private SearchOptions searchOptions = new SearchOptions();

    @Parameter(names = { P_VERIFICATION_LONG }, descriptionKey = "upgradeObjects.verification")
    private File verification;

    public File getVerification() {
        return verification;
    }

    public void setVerification(File verification) {
        this.verification = verification;
    }

    public OutputOptions getOutputOptions() {
        return outputOptions;
    }

    public void setOutputOptions(OutputOptions outputOptions) {
        this.outputOptions = outputOptions;
    }

    public SearchOptions getSearchOptions() {
        return searchOptions;
    }

    public void setSearchOptions(SearchOptions searchOptions) {
        this.searchOptions = searchOptions;
    }
}
