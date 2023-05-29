/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.upgrade;

import java.io.File;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.ninja.action.upgrade.ConsoleProgressListener;
import com.evolveum.midpoint.ninja.action.upgrade.DistributionManager;
import com.evolveum.midpoint.ninja.action.upgrade.ProgressListener;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

public class DistributionManagerTest {

    private static final Trace LOGGER = TraceManager.getTrace(DistributionManagerTest.class);

    @Test(enabled = false)
    public void downloadLTS() throws Exception {
        AnsiConsole.systemInstall();

        final ProgressListener listener = new ConsoleProgressListener();

        File file = new DistributionManager(new File("./target")).downloadDistribution("4.4.4", listener);
        AssertJUnit.assertTrue(file.exists());
        LOGGER.info("File size: " + file.length());
        LOGGER.info("File path: " + file.getAbsolutePath());

        AnsiConsole.systemUninstall();
    }

    @Test(enabled = false)
    public void testJANSI() {
        AnsiConsole.systemInstall();

        System.out.println(Ansi.ansi().fgBlue().a("Start").reset());
        for (int i = 0; i < 10; i++) {
            System.out.println(Ansi.ansi().eraseLine(Ansi.Erase.ALL).fgGreen().a(i).reset());
        }
        System.out.println(Ansi.ansi().fgRed().a("Complete").reset());

        AnsiConsole.systemUninstall();
    }
}
