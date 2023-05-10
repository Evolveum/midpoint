/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action.upgrade;

import org.apache.commons.io.FileUtils;

import java.io.File;

public class UpgradeConstants {

    public static final File TEMP_UPGRADE_FOLDER = new File(FileUtils.getTempDirectory(), "midpoint-upgrade/");
}
