package com.evolveum.midpoint.ninja.action;

import java.io.File;
import java.util.List;

import com.beust.jcommander.Parameters;

@Parameters(resourceBundle = "messages", commandDescriptionKey = "setupDatabase")
public class SetupDatabaseOptions extends DataSourceOptions {

    public SetupDatabaseOptions() {
        setScriptsDirectory(new File("./doc/config/sql/native-new"));
        setScripts(List.of(
                new File("postgres-new.sql"),
                new File("postgres-new-quartz.sql")
        ));
        setAuditScripts(List.of(
                new File("postgres-new-audit.sql")
        ));
    }
}
