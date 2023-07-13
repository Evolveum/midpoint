/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja;

import com.evolveum.midpoint.ninja.action.BaseOptions;
import com.evolveum.midpoint.ninja.action.ConnectionOptions;
import com.evolveum.midpoint.ninja.action.upgrade.action.UpgradeInstallationAction;

import com.evolveum.midpoint.ninja.action.upgrade.action.UpgradeInstallationOptions;

import org.testng.annotations.Test;

import java.io.File;
import java.util.List;

public class UpgradeInstallationTest implements NinjaTestMixin {

    @Test
    public void testCopyFiles() throws Exception{
        BaseOptions base = new BaseOptions();
        base.setVerbose(true);

        ConnectionOptions connection = new ConnectionOptions();

        UpgradeInstallationOptions options = new UpgradeInstallationOptions();
        options.setBackup(true);
        options.setInstallationDirectory(new File("../../_mess/upgrade/upgrade-installation/midpoint-4.4.5"));
        options.setDistributionDirectory(new File("../../_mess/upgrade/upgrade-installation/midpoint-4.8-SNAPSHOT"));

        executeAction(UpgradeInstallationAction.class, options, List.of(options, base, connection));
    }
}
