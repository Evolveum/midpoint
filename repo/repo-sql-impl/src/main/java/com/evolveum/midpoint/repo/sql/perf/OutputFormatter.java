/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.perf;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.evolveum.midpoint.repo.api.perf.OperationRecord;
import com.evolveum.midpoint.repo.sql.helpers.BaseHelper;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

class OutputFormatter {

    private static final Trace LOGGER = TraceManager.getTrace(OutputFormatter.class);

    static void writeStatisticsToFile(String file, List<OperationRecord> finishedOperations, Map<Long, OperationRecord> outstandingOperations) {
        try {
            PrintWriter pw = new PrintWriter(new FileWriter(file, true));
            for (OperationRecord or : finishedOperations) {
                pw.println(new Date(or.getStartTime()) + "\t" + or.getKind() + "\t" + or.getObjectTypeName() + "\t" + or.getAttempts() + "\t" + or.getTotalTime()
                        + "\t" + or.getWastedTime());
            }
            for (OperationRecord or : outstandingOperations.values()) {
                pw.println(new Date(or.getStartTime()) + "\t" + or.getKind() + "\t" + or.getObjectTypeName() + "\t" + "?" + "\t" + "?" + "\t" + or
                        .getWastedTime());
            }
            pw.close();
            LOGGER.trace("{} record(s) written to file {}", finishedOperations.size() + outstandingOperations.size(), file);
        } catch (IOException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't write repository performance statistics to file " + file, e);
        }

    }

    static String getFormattedStatistics(List<OperationRecord> finishedOperations, Map<Long, OperationRecord> outstandingOperations) {
        StatEntry all = new StatEntry();
        StatEntry unfinished = new StatEntry();
        final int maxAttempts = BaseHelper.LOCKING_MAX_RETRIES + 1;
        StatEntry[] perAttempts = new StatEntry[maxAttempts];

        for (int i = 0; i < maxAttempts; i++) {
            perAttempts[i] = new StatEntry();
        }

        for (OperationRecord operation : finishedOperations) {
            all.process(operation);
            if (operation.getAttempts() >= 1 && operation.getAttempts() <= maxAttempts) {
                perAttempts[operation.getAttempts() - 1].process(operation);
            } else if (operation.getAttempts() < 0) {
                unfinished.process(operation);
            }
        }

        StringBuilder retval = new StringBuilder();
        retval.append("Overall: ").append(all.dump()).append("\n");
        for (int i = 0; i < maxAttempts; i++) {
            retval.append(i + 1).append(" attempt(s): ").append(perAttempts[i].dump()).append("\n");
        }
        retval.append("Unfinished: ").append(unfinished.dump()).append("\n");
        retval.append("Outstanding: ").append(outstandingOperations.toString());
        return retval.toString();
    }

    private static class StatEntry {
        long totalTime, wastedTime;
        int attempts;
        int records;

        public void process(OperationRecord operation) {
            totalTime += operation.getTotalTime();
            wastedTime += operation.getWastedTime();
            attempts += operation.getAttempts();
            records++;
        }

        public String dump() {
            if (records == 0) {
                return "no records";
            }
            return "Records: " + records + ", " +
                    "Total time (avg/sum): " + ((float) totalTime / records) + "/" + totalTime + ", " +
                    "Wasted time (avg/sum): " + ((float) wastedTime / records) + "/" + wastedTime + " (" + (wastedTime * 100.0f / totalTime) + "%), " +
                    "Attempts (avg): " + ((float) attempts / records);
        }
    }
}
