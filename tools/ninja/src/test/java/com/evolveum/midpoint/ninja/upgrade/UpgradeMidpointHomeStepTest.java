/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.upgrade;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.testng.annotations.Test;

import com.evolveum.midpoint.ninja.action.upgrade.step.UpgradeMidpointHomeStep;
import com.evolveum.midpoint.ninja.action.upgrade.step.UpgradeMidpointHomeOptions;

public class UpgradeMidpointHomeStepTest {

    private static final File RESOURCES_UPGRADE_DIRECTORY = new File("./src/test/resources/upgrade/upgrade-midpoint-home");

    private static final File TEST_TARGET_DIRECTORY = new File("./target/upgrade-midpoint-home");

    @Test
    public void testUpgradeNoBackup() throws Exception {
        testUpgrade(false);
    }

    @Test
    public void testUpgradeWithBackup() throws Exception {
        testUpgrade(true);
    }

    private void testUpgrade(boolean withBackup) throws Exception {
        File distribution = copyResources("distribution");
        File midpointHome = copyResources("midpoint-home");

        UpgradeMidpointHomeOptions opts = new UpgradeMidpointHomeOptions();
        opts.setDistributionDirectory(distribution);
        opts.setMidpointHomeDirectory(midpointHome);
        opts.setBackupFiles(withBackup);

        UpgradeMidpointHomeStep step = new UpgradeMidpointHomeStep(opts);
        step.execute();
    }

    private File copyResources(String directory) throws IOException {
        File target = new File(TEST_TARGET_DIRECTORY, directory);
        if (target.exists()) {
            target.delete();
        }

        FileUtils.forceMkdir(target);

        FileUtils.copyDirectory(new File(RESOURCES_UPGRADE_DIRECTORY, directory), target);

        return target;
    }
}
