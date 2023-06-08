/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.upgrade;

public class UpgradeStepsTest {

//    @Test(enabled = false)
//    public void test100DownloadDistribution() throws Exception {
//        DownloadDistributionStep step = new DownloadDistributionStep(null);
////        step.setVersion("4.4.4");
////        step.execute();
//    }
//
//    @Test(enabled = false)
//    public void test200UpgradeDatabaseSchema() throws Exception {
//        // nasty initialization in test, this looks like an issue in code (messy code/architecture of ninja)
//        JCommander jc = NinjaUtils.setupCommandLineParser();
//        jc.parse("-m ../../_mess/midpoint-home upgrade".split(" "));
//
//        ConnectionOptions options = NinjaUtils.getOptions(jc, ConnectionOptions.class);
//        UpgradeOptions upgradeOptions = NinjaUtils.getOptions(jc, UpgradeOptions.class);
//
//        NinjaContext ninjaContext = new NinjaContext(null);
//        ninjaContext.setLog(new Log(LogTarget.SYSTEM_OUT, Log.LogLevel.DEFAULT));
//        ninjaContext.init(options);
//
//        UpgradeStepsContext ctx = new UpgradeStepsContext(ninjaContext, upgradeOptions);
//
//        DownloadDistributionStep distributionStep = new DownloadDistributionStep(ctx);
////        distributionStep.setVersion("4.4.4");
//        DownloadDistributionResult result = distributionStep.execute();
//
//        ctx.addResult(DownloadDistributionStep.class, result);
//
//        DatabaseSchemaStep schemaStep = new DatabaseSchemaStep(ctx);
//        schemaStep.execute();
//    }
}
