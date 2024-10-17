package com.evolveum.midpoint.ninja.action.upgrade;

import java.io.File;

public class UpgradeConstants {

    /**
     * This is the version we are upgrading from.
     * It's current version.
     */
    public static final String SUPPORTED_VERSION = "4.8.5";

    /**
     * This is the version we are upgrading to.
     * For "master" branch it is null - there's nothing newer than master - nothing to upgrade to.
     */
    public static final String SUPPORTED_VERSION_TARGET = "4.9";

    public static final String UPGRADE_TEMP_DIRECTORY = ".upgrade";

    /**
     * Directory within distribution where SQL scripts for native PostgreSQL repository are located.
     */
    public static final File SCRIPTS_DIRECTORY = new File("./doc/config/sql/native");
}
