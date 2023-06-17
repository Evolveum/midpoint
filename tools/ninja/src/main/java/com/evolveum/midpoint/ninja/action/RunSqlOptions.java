package com.evolveum.midpoint.ninja.action;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import com.evolveum.midpoint.ninja.util.RunModeConverterValidator;

@Parameters(resourceBundle = "messages", commandDescriptionKey = "runSql")
public class RunSqlOptions {

    public enum Mode {

        RAW(new File("."),
                Collections.emptyList(),
                Collections.emptyList()
        ),

        REPOSITORY(new File("./doc/config/sql/native-new"),
                List.of(new File("postgres-new.sql"), new File("postgres-new-quartz.sql")),
                List.of(new File("postgres-new-upgrade.sql"))
        ),

        AUDIT(new File("./doc/config/sql/native-new"),
                List.of(new File("postgres-new-audit.sql")),
                List.of(new File("postgres-new-upgrade-audit.sql"))
        );

        final File scriptsDirectory;

        final List<File> createScripts;

        final List<File> updateScripts;

        Mode(File scriptsDirectory, List<File> createScripts, List<File> updateScripts) {
            this.scriptsDirectory = scriptsDirectory;
            this.createScripts = createScripts;
            this.updateScripts = updateScripts;
        }
    }

    public static final String P_SCRIPTS_DIRECTORY_LONG = "--scripts-directory";
    public static final String P_SCRIPTS_LONG = "--scripts";
    public static final String P_AUDIT_SCRIPTS_LONG = "--audit-scripts";
    public static final String P_NO_AUDIT_LONG = "--no-audit";
    public static final String P_NO_REPOSITORY_LONG = "--no-repository";
    public static final String P_JDBC_URL_LONG = "--jdbc-url";
    public static final String P_JDBC_USERNAME_LONG = "--jdbc-username";
    public static final String P_JDBC_PASSWORD_LONG = "--jdbc-password";
    public static final String P_MODE = "--mode";
    public static final String P_UPGRADE = "--upgrade";
    public static final String P_CREATE = "--create";
    public static final String P_RESULT = "--result";

    public static final String P_JDBC_ASK_PASSWORD_LONG = "--jdbc-ask-password";

    @Parameter(names = { P_SCRIPTS_DIRECTORY_LONG }, descriptionKey = "runSql.scriptsDirectory")
    private File scriptsDirectory;

    @Parameter(names = { P_SCRIPTS_LONG }, descriptionKey = "runSql.scripts", variableArity = true)
    private List<File> scripts = new ArrayList<>();

    @Parameter(names = { P_MODE }, descriptionKey = "runSql.mode", validateWith = RunModeConverterValidator.class, converter = RunModeConverterValidator.class)
    private Mode mode;

    @Parameter(names = { P_JDBC_URL_LONG }, descriptionKey = "runSql.jdbcUrl")
    private String jdbcUrl;

    @Parameter(names = { P_JDBC_USERNAME_LONG }, descriptionKey = "runSql.jdbcUsername")
    private String jdbcUsername;

    @Parameter(names = { P_JDBC_PASSWORD_LONG }, descriptionKey = "runSql.jdbcPassword")
    private String jdbcPassword;

    @Parameter(names = { P_JDBC_ASK_PASSWORD_LONG }, descriptionKey = "runSql.jdbcAskPassword", password = true)
    private String jdbcAskPassword;

    @Parameter(names = { P_UPGRADE }, descriptionKey = "runSql.upgrade")
    private boolean upgrade;

    @Parameter(names = { P_CREATE }, descriptionKey = "runSql.create")
    private boolean create;

    @Parameter(names = { P_RESULT }, descriptionKey = "runSql.result")
    private boolean result;

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

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public String getJdbcUsername() {
        return jdbcUsername;
    }

    public void setJdbcUsername(String jdbcUsername) {
        this.jdbcUsername = jdbcUsername;
    }

    public String getJdbcPassword() {
        return jdbcPassword;
    }

    public void setJdbcPassword(String jdbcPassword) {
        this.jdbcPassword = jdbcPassword;
    }

    public String getJdbcAskPassword() {
        return jdbcAskPassword;
    }

    public void setJdbcAskPassword(String jdbcAskPassword) {
        this.jdbcAskPassword = jdbcAskPassword;
    }

    public String getPassword() {
        if (jdbcPassword != null) {
            return jdbcPassword;
        }

        return jdbcAskPassword;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public boolean getUpgrade() {
        return upgrade;
    }

    public void setUpgrade(boolean upgrade) {
        this.upgrade = upgrade;
    }

    public boolean getCreate() {
        return create;
    }

    public void setCreate(boolean create) {
        this.create = create;
    }

    public boolean getResult() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }
}
