/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja;

import java.io.File;

import org.fusesource.jansi.AnsiConsole;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.ninja.action.upgrade.DistributionManager;
import com.evolveum.midpoint.ninja.action.upgrade.ProgressListener;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

public class DistributionManagerTest {

    private static final Trace LOGGER = TraceManager.getTrace(DistributionManagerTest.class);

    @BeforeClass
    public void beforeClass() {
        AnsiConsole.systemInstall();
    }

    @AfterClass
    public void afterClass() {
        AnsiConsole.systemUninstall();
    }

    @Test(enabled = false)
    public void downloadSpecificVersion() throws Exception {
        downloadAndAssert("4.7");
    }

    @Test(enabled = false)
    public void downloadLatest() throws Exception {
        downloadAndAssert(DistributionManager.LATEST_VERSION);
    }

    private void downloadAndAssert(String version) throws Exception {
        final TestProgressListener listener = new TestProgressListener();

        File file = new DistributionManager(new File("./target")).downloadDistribution(version, listener);
        AssertJUnit.assertTrue(file.exists());
        AssertJUnit.assertTrue(file.length() > 0);
        AssertJUnit.assertEquals(listener.contentLength, file.length());

        LOGGER.info("File size: " + file.length());
        LOGGER.info("File path: " + file.getAbsolutePath());
    }

    private static class TestProgressListener implements ProgressListener {

        private long contentLength;

        private boolean done;

        @Override
        public void update(long bytesRead, long contentLength, boolean done) {
            AssertJUnit.assertFalse("Already done, shouldn't happen", this.done);

            if (this.contentLength == 0) {
                AssertJUnit.assertTrue("Content length is zero", contentLength > 0);
                this.contentLength = contentLength;
                return;
            }

            if (!this.done && done) {
                this.done = true;
            }

            AssertJUnit.assertEquals("Content length doesn't match", this.contentLength, contentLength);
            this.done = done;
        }
    }
}
