package com.evolveum.midpoint.ninja.opts;

import com.beust.jcommander.Parameter;

import java.io.File;
import java.util.List;

public class SetupDatabaseSchemaOptions {

    public static final String P_SCRIPTS_DIRECTORY_LONG = "--scripts-directory";
    public static final String P_SCRIPTS_LONG = "--script";
    public static final String P_NO_AUDIT_LONG = "--no-audit";

    @Parameter(names = { P_SCRIPTS_DIRECTORY_LONG }, descriptionKey = "setupDatabaseSchema.scriptsDirectory")
    private File scriptsDirectory;

    @Parameter(names = { P_SCRIPTS_LONG }, descriptionKey = "setupDatabaseSchema.scripts", variableArity = true)
    private List<File> scripts;

    @Parameter(names = { P_NO_AUDIT_LONG }, descriptionKey = "setupDatabaseSchema.noAudit")
    private boolean noAudit;

    public File getScriptsDirectory() {
        return scriptsDirectory;
    }

    public void setScriptsDirectory(File scriptsDirectory) {
        this.scriptsDirectory = scriptsDirectory;
    }

    public List<File> getScripts() {
        return scripts;
    }

    public void setScripts(List<File> scripts) {
        this.scripts = scripts;
    }

    public boolean isNoAudit() {
        return noAudit;
    }

    public void setNoAudit(boolean noAudit) {
        this.noAudit = noAudit;
    }
}
