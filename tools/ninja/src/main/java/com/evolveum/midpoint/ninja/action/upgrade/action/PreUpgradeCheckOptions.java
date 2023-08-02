package com.evolveum.midpoint.ninja.action.upgrade.action;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(resourceBundle = "messages", commandDescriptionKey = "preUpgradeCheck")
public class PreUpgradeCheckOptions {

    public static final String P_SKIP_NODES_VERSION_CHECK = "--skip-nodes-version-check";

    public static final String P_SKIP_DATABASE_VERSION_CHECK = "--skip-database-version-check";

    @Parameter(names = { P_SKIP_NODES_VERSION_CHECK }, descriptionKey = "preUpgradeCheck.skipNodesVersionCheck")
    private boolean skipNodesVersionCheck;

    @Parameter(names = { P_SKIP_DATABASE_VERSION_CHECK }, descriptionKey = "preUpgradeCheck.skipDatabaseVersionCheck")
    private boolean skipDatabaseVersionCheck;

    public boolean isSkipNodesVersionCheck() {
        return skipNodesVersionCheck;
    }

    public void setSkipNodesVersionCheck(boolean skipNodesVersionCheck) {
        this.skipNodesVersionCheck = skipNodesVersionCheck;
    }

    public boolean isSkipDatabaseVersionCheck() {
        return skipDatabaseVersionCheck;
    }

    public void setSkipDatabaseVersionCheck(boolean skipDatabaseVersionCheck) {
        this.skipDatabaseVersionCheck = skipDatabaseVersionCheck;
    }
}
