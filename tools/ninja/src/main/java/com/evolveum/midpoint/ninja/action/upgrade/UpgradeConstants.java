package com.evolveum.midpoint.ninja.action.upgrade;

import com.evolveum.midpoint.ninja.Main;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class UpgradeConstants {

    private static final String UNKNOWN_VERSION = "unknown";

    /**
     * This is the version we are upgrading from.
     * It's current version.
     */
    public static final String SUPPORTED_VERSION;

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

    static {
        URL url = Main.class.getResource("/version");

        String version;
        if (url == null) {
            version = UNKNOWN_VERSION;
        } else {
            try (InputStream is = url.openStream()) {
                version = IOUtils.toString(is, StandardCharsets.UTF_8).trim();
                if (StringUtils.isEmpty(version)) {
                    version = UNKNOWN_VERSION;
                }
            } catch (Exception ex) {
                // This should not happen
                version = UNKNOWN_VERSION;
            }
        }

        SUPPORTED_VERSION = version;
    }
}
