/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.opts;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import java.io.File;

/**
 * @author semancik
 */
@Parameters(resourceBundle = "messages", commandDescriptionKey = "verify")
public class VerifyOptions extends ExportOptions {

    public static final String P_WARN = "-w";
    public static final String P_WARN_LONG = "--warn";

    @Parameter(names = {P_WARN, P_WARN_LONG}, descriptionKey = "verify.warn")
    private String warn;

    public String getWarn() {
        return warn;
    }

}
