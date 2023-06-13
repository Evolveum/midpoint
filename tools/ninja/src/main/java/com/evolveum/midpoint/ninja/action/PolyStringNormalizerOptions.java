/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Created by Viliam Repan (lazyman).
 */
@Parameters(resourceBundle = "messages")
public class PolyStringNormalizerOptions {

    public static final String P_PSN_CLASS_NAME = "--psn-class-name";

    public static final String P_PSN_TRIM = "--psn-trim";

    public static final String P_PSN_NFKD = "--psn-nfkd";

    public static final String P_PSN_TRIM_WHITESPACE = "--psn-trim-whitespace";

    public static final String P_PSN_LOWERCASE = "--psn-lowercase";

    @Parameter(names = { P_PSN_CLASS_NAME }, descriptionKey = "base.psn.className")
    private String psnClassName;

    @Parameter(names = { P_PSN_TRIM }, descriptionKey = "base.psn.trim")
    private Boolean psnTrim;

    @Parameter(names = { P_PSN_NFKD }, descriptionKey = "base.psn.nfkd")
    private Boolean psnNfkd;

    @Parameter(names = { P_PSN_TRIM_WHITESPACE }, descriptionKey = "base.psn.trimWhitespace")
    private Boolean psnTrimWhitespace;

    @Parameter(names = { P_PSN_LOWERCASE }, descriptionKey = "base.psn.lowercase")
    private Boolean psnLowercase;

    public String getPsnClassName() {
        return psnClassName;
    }

    public Boolean isPsnTrim() {
        return psnTrim;
    }

    public Boolean isPsnNfkd() {
        return psnNfkd;
    }

    public Boolean isPsnTrimWhitespace() {
        return psnTrimWhitespace;
    }

    public Boolean isPsnLowercase() {
        return psnLowercase;
    }
}
