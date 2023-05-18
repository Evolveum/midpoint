/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action.upgrade;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import java.io.File;

@Parameters(resourceBundle = "messages", commandDescriptionKey = "upgrade")
public class UpgradeOptions {

    public static final String P_TEMP_DIR_LONG = "--temp-dir";

    public static final String P_ABORT_LONG = "--abort";

    public static final String P_SKIP_STEP = "--skip-step";

    public static final String P_STEP_LONG = "--step";

    @Parameter(names = {P_ABORT_LONG}, descriptionKey = "upgrade.abort")
    private Boolean abort;

    @Parameter(names = {P_TEMP_DIR_LONG}, descriptionKey = "upgrade.tempDir")
    private File tempDirectory;

    @Parameter(names = {P_STEP_LONG}, descriptionKey = "upgrade.step")
    private String step;

    public Boolean getAbort() {
        return abort;
    }
}
