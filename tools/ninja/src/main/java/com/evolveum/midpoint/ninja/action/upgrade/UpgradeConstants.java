package com.evolveum.midpoint.ninja.action.upgrade;

public class UpgradeConstants {

    /**
     * This is the version we are upgrading from.
     * It's current version.
     */
    public static final String SUPPORTED_VERSION = "4.4.6";

    /**
     * This is the version we are upgrading to.
     * For "master" branch it is null - there's nothing newer than master - nothing to upgrade to.
     */
    public static final String SUPPORTED_VERSION_TARGET = "4.8";

    public static final String UPGRADE_TEMP_DIRECTORY = ".upgrade";
}
