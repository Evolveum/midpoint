/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.conntool;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import java.io.File;

/**
 * Options common to all functionalities of this tool.
 * (Currently only "generate" command is implemented.)
 */
@Parameters(resourceBundle = "messages")
public class CommonOptions {

    private static final String P_HELP = "-h";
    private static final String P_HELP_LONG = "--help";

    static final String P_VERBOSE = "-v";
    private static final String P_VERBOSE_LONG = "--verbose";

    static final String P_SILENT = "-s";
    private static final String P_SILENT_LONG = "--silent";

    private static final String P_CONNECTOR_FILE = "-c";
    private static final String P_CONNECTOR_FILE_LONG = "--connector-file";

    private static final String P_VERSION = "-V";
    private static final String P_VERSION_LONG = "--version";

    // TODO generalize to "connector directory" to process multiple JARs in a single run
    // TODO an option to select individual connectors from the bundle (e.g. LDAP connector from connector-ldap-x.y.jar bundle)

    @Parameter(names = { P_CONNECTOR_FILE, P_CONNECTOR_FILE_LONG }, descriptionKey = "common.connectorFile")
    private File connectorFile;

    @Parameter(names = { P_HELP, P_HELP_LONG }, help = true, descriptionKey = "common.help")
    private boolean help;

    @Parameter(names = { P_VERBOSE, P_VERBOSE_LONG }, descriptionKey = "common.verbose")
    private boolean verbose;

    @Parameter(names = { P_SILENT, P_SILENT_LONG }, descriptionKey = "common.silent")
    private boolean silent;

    @Parameter(names = { P_VERSION, P_VERSION_LONG }, descriptionKey = "common.version")
    private boolean version;

    File getConnectorFile() {
        return connectorFile;
    }

    boolean isHelp() {
        return help;
    }

    boolean isVerbose() {
        return verbose;
    }

    boolean isSilent() {
        return silent;
    }

    boolean isVersion() {
        return version;
    }
}
