package com.evolveum.midpoint.ninja.action.upgrade.action;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.ninja.action.ExportOptions;
import com.evolveum.midpoint.schema.validator.UpgradePhase;
import com.evolveum.midpoint.schema.validator.UpgradePriority;
import com.evolveum.midpoint.schema.validator.UpgradeType;

/**
 * todo cleanup options, use OutputOptions, SearchOptions etc. Get rid of inheritance, replace with composition
 */
@Parameters(resourceBundle = "messages", commandDescriptionKey = "upgradeObjects")
public class UpgradeObjectsOptions extends ExportOptions {

    public static final String P_VERIFICATION_LONG = "--verification-file";
    public static final String P_FILE = "--file";
    public static final String P_IDENTIFIER = "--upgrade-identifier";
    public static final String P_PHASE = "--upgrade-phase";
    public static final String P_TYPE = "--upgrade-type";
    public static final String P_PRIORITY = "--upgrade-priority";
    public static final String P_SKIP_UPGRADE_WARNING = "--skip-upgrade-warning";

//    @ParametersDelegate
//    private OutputOptions outputOptions = new OutputOptions();
//
//    @ParametersDelegate
//    private SearchOptions searchOptions = new SearchOptions();

    @Parameter(names = { P_VERIFICATION_LONG }, descriptionKey = "upgradeObjects.verification")
    private File verification;

    @Parameter(names = { P_FILE }, descriptionKey = "upgradeObjects.files", variableArity = true)
    private List<File> files = new ArrayList<>();

    @Parameter(names = { P_IDENTIFIER }, descriptionKey = "upgradeObjects.identifiers", variableArity = true)
    private List<String> identifiers = new ArrayList<>();

    @Parameter(names = { P_PHASE }, descriptionKey = "upgradeObjects.phases", variableArity = true)
    private List<UpgradePhase> phases = new ArrayList<>();

    @Parameter(names = { P_TYPE }, descriptionKey = "upgradeObjects.types", variableArity = true)
    private List<UpgradeType> types = new ArrayList<>();

    @Parameter(names = { P_PRIORITY }, descriptionKey = "upgradeObjects.priorities", variableArity = true)
    private List<UpgradePriority> priorities = new ArrayList<>();

    @Parameter(names = { P_SKIP_UPGRADE_WARNING }, descriptionKey = "upgradeObjects.skipUpgradeWarning")
    private boolean skipUpgradeWarning;

    public File getVerification() {
        return verification;
    }

    public void setVerification(File verification) {
        this.verification = verification;
    }

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

    public List<String> getIdentifiers() {
        return identifiers;
    }

    public void setIdentifiers(List<String> identifiers) {
        this.identifiers = identifiers;
    }

    public List<UpgradePhase> getPhases() {
        return phases;
    }

    public void setPhases(List<UpgradePhase> phases) {
        this.phases = phases;
    }

    public List<UpgradeType> getTypes() {
        return types;
    }

    public void setTypes(List<UpgradeType> types) {
        this.types = types;
    }

    public List<UpgradePriority> getPriorities() {
        return priorities;
    }

    public void setPriorities(List<UpgradePriority> priorities) {
        this.priorities = priorities;
    }

    public boolean isSkipUpgradeWarning() {
        return skipUpgradeWarning;
    }

    public void setSkipUpgradeWarning(boolean skipUpgradeWarning) {
        this.skipUpgradeWarning = skipUpgradeWarning;
    }
}
