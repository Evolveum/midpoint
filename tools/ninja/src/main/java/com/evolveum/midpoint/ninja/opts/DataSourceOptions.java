package com.evolveum.midpoint.ninja.opts;

import java.io.File;
import java.util.List;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(resourceBundle = "messages", commandDescriptionKey = "dataSource")
public class DataSourceOptions {

    public static final String P_SCRIPTS_DIRECTORY_LONG = "--scripts-directory";
    public static final String P_SCRIPTS_LONG = "--scripts";
    public static final String P_AUDIT_SCRIPTS_LONG = "--audit-scripts";
    public static final String P_NO_AUDIT_LONG = "--no-audit";
    public static final String P_AUDIT_ONLY_LONG = "--audit-only";

    @Parameter(names = { P_SCRIPTS_DIRECTORY_LONG }, descriptionKey = "dataSource.scriptsDirectory")
    private File scriptsDirectory;

    @Parameter(names = { P_SCRIPTS_LONG }, descriptionKey = "dataSource.scripts", variableArity = true)
    private List<File> scripts;

    @Parameter(names = { P_AUDIT_SCRIPTS_LONG }, descriptionKey = "dataSource.auditScripts", variableArity = true)
    private List<File> auditScripts;

    @Parameter(names = { P_NO_AUDIT_LONG }, descriptionKey = "dataSource.noAudit")
    private boolean noAudit;

    @Parameter(names = { P_AUDIT_ONLY_LONG }, descriptionKey = "dataSource.auditOnly")
    private boolean auditOnly;

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

    public List<File> getAuditScripts() {
        return auditScripts;
    }

    public void setAuditScripts(List<File> auditScripts) {
        this.auditScripts = auditScripts;
    }

    public boolean isAuditOnly() {
        return auditOnly;
    }

    public void setAuditOnly(boolean auditOnly) {
        this.auditOnly = auditOnly;
    }
}
