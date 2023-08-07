/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action.upgrade;

import static com.evolveum.midpoint.ninja.util.ConsoleFormat.*;

import java.text.DecimalFormat;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;

import com.evolveum.midpoint.ninja.impl.Log;
import com.evolveum.midpoint.ninja.impl.LogLevel;
import com.evolveum.midpoint.ninja.util.NinjaUtils;

public class ConsoleProgressListener implements ProgressListener {

    private static final DecimalFormat PROGRESS_FORMAT = new DecimalFormat("###");

    private static final DecimalFormat SPEED_FORMAT = new DecimalFormat("####.##");

    private final Log log;

    private boolean firstUpdate = true;

    private long startTime;

    private long lastReportTime;

    public ConsoleProgressListener(Log log) {
        this.log = log;
    }

    @Override
    public void update(long bytesRead, long contentLength, boolean done) {
        if (done) {
            log.info(
                    rewriteConsoleLine(
                            formatLogMessage(LogLevel.INFO,
                                    formatSuccessMessage("Download complete"))));
            return;
        }

        if (firstUpdate) {
            firstUpdate = false;

            startTime = System.currentTimeMillis();

            String size = contentLength == -1 ? "unknown" : FileUtils.byteCountToDisplaySize(contentLength);

            log.info(formatMessageWithInfoParameters("Download size: {}", size));

            // this empty line will be removed with progress update
            log.info(logProgress(bytesRead, contentLength));
            lastReportTime = System.currentTimeMillis();
        }

        if (System.currentTimeMillis() - lastReportTime < NinjaUtils.COUNT_STATUS_LOG_INTERVAL) {
            return;
        }

        log.info(
                rewriteConsoleLine(
                        logProgress(bytesRead, contentLength)));
        lastReportTime = System.currentTimeMillis();
    }

    private String logProgress(long bytesRead, long contentLength) {
        double speed = bytesRead / ((System.currentTimeMillis() - startTime) / 1000.0);
        double speedMB = speed / 1024 / 1024;
        long estimatedDuration = (long) ((contentLength - bytesRead) / speed * 1000);

        double progress = (double) (100 * bytesRead) / contentLength;

        return formatLogMessage(LogLevel.INFO,
                NinjaUtils.printFormatted(
                        "Progress: {}\t{} MB/s\tETA: {}",
                        formatMessage(PROGRESS_FORMAT.format(progress) + "%", Color.INFO),
                        SPEED_FORMAT.format(speedMB),
                        DurationFormatUtils.formatDurationWords(estimatedDuration, true, true)));
    }
}
