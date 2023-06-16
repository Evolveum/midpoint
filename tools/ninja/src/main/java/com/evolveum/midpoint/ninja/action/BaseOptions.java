/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action;

import java.io.File;
import java.nio.charset.StandardCharsets;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;

/**
 * Created by Viliam Repan (lazyman).
 */
@Parameters(resourceBundle = "messages")
public class BaseOptions {

    public static final String P_HELP = "-h";
    public static final String P_HELP_LONG = "--help";

    public static final String P_VERBOSE = "-v";
    public static final String P_VERBOSE_LONG = "--verbose";

    public static final String P_SILENT = "-s";
    public static final String P_SILENT_LONG = "--silent";

    public static final String P_CHARSET = "-c";
    public static final String P_CHARSET_LONG = "--charset";

    public static final String P_VERSION = "-V";
    public static final String P_VERSION_LONG = "--version";

    @Parameter(names = { P_HELP, P_HELP_LONG }, help = true, descriptionKey = "base.help")
    private boolean help = false;

    @Parameter(names = { P_VERBOSE, P_VERBOSE_LONG }, descriptionKey = "base.verbose")
    private boolean verbose = false;

    @Parameter(names = { P_SILENT, P_SILENT_LONG }, descriptionKey = "base.silent")
    private boolean silent = false;

    @Parameter(names = { P_CHARSET, P_CHARSET_LONG }, descriptionKey = "base.charset")
    private String charset = StandardCharsets.UTF_8.name();

    @Parameter(names = { P_VERSION, P_VERSION_LONG }, descriptionKey = "base.version")
    private Boolean version;

    @ParametersDelegate
    private PolyStringNormalizerOptions polyStringNormalizerOptions = new PolyStringNormalizerOptions();

    public boolean isHelp() {
        return help;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public boolean isSilent() {
        return silent;
    }

    public String getCharset() {
        return charset;
    }

    public Boolean isVersion() {
        return version;
    }

    public PolyStringNormalizerOptions getPolyStringNormalizerOptions() {
        return polyStringNormalizerOptions;
    }

    public void setHelp(boolean help) {
        this.help = help;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void setSilent(boolean silent) {
        this.silent = silent;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public void setVersion(Boolean version) {
        this.version = version;
    }

    public void setPolyStringNormalizerOptions(PolyStringNormalizerOptions polyStringNormalizerOptions) {
        this.polyStringNormalizerOptions = polyStringNormalizerOptions;
    }
}
