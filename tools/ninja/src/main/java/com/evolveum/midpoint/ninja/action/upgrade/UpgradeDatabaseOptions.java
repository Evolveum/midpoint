package com.evolveum.midpoint.ninja.action.upgrade;

import java.io.File;
import java.util.List;

import com.beust.jcommander.Parameters;

import com.evolveum.midpoint.ninja.opts.DataSourceOptions;

@Parameters(resourceBundle = "messages", commandDescriptionKey = "upgradeDatabase")
public class UpgradeDatabaseOptions extends DataSourceOptions {

    public UpgradeDatabaseOptions() {
        setScriptsDirectory(new File("./doc/config/sql/native-new"));
        setScripts(List.of(
                new File("postgres-new-upgrade.sql")
        ));
        setAuditScripts(List.of(
                new File("postgres-new-upgrade-audit.sql")
        ));
    }
}
