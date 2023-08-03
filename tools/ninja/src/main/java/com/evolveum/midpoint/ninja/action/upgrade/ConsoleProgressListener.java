/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action.upgrade;

import java.text.DecimalFormat;

import org.apache.commons.io.FileUtils;
import org.fusesource.jansi.Ansi;

import com.evolveum.midpoint.ninja.impl.Log;
import com.evolveum.midpoint.ninja.impl.LogLevel;
import com.evolveum.midpoint.ninja.util.ConsoleFormat;

public class ConsoleProgressListener implements ProgressListener {

    private static final DecimalFormat FORMAT = new DecimalFormat("###");

    private final Log log;

    private boolean firstUpdate = true;

    private double progress = 0;

    public ConsoleProgressListener(Log log) {
        this.log = log;
    }

    @Override
    public void update(long bytesRead, long contentLength, boolean done) {
        if (done) {
            log.info(ConsoleFormat.formatSuccessMessage("Download complete"));
            return;
        }

        if (firstUpdate) {
            firstUpdate = false;

            String size = contentLength == -1 ? "unknown" : FileUtils.byteCountToDisplaySize(contentLength);

            log.info(ConsoleFormat.formatMessageWithInfoParameters("Download size: {}", size));

            // this empty line will be removed with progress update
            log.info("");
        }

        double newProgress = (double) (100 * bytesRead) / contentLength;
        if (newProgress - progress > 1) {
            logProgress(newProgress);

            progress = newProgress;
        }
    }

    private void logProgress(double newProgress) {
        log.info(Ansi.ansi()
                .cursorToColumn(1)
                .cursorUpLine().eraseLine(Ansi.Erase.ALL)
                .a(
                        ConsoleFormat.formatLogMessage(LogLevel.INFO,
                                ConsoleFormat.formatMessageWithInfoParameters("Progress: {}%", FORMAT.format(newProgress)))
                ).toString());
    }
}
