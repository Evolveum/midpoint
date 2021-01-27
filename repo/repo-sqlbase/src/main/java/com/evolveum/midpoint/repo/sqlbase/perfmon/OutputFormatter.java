/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase.perfmon;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.evolveum.midpoint.repo.api.perf.OperationRecord;
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
        Map<Integer, StatEntry> attemptStats = new TreeMap<>();

        for (OperationRecord operation : finishedOperations) {
            all.process(operation);
            StatEntry statEntry = attemptStats.computeIfAbsent(
                    operation.getAttempts(),
                    ix -> new StatEntry());
            statEntry.process(operation);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Overall: ").append(all.dump()).append("\n");
        attemptStats.forEach((ix, stat) ->
                sb.append(ix).append(" attempt(s): ").append(stat.dump()).append("\n"));
        sb.append("Outstanding: ").append(outstandingOperations.toString());
        return sb.toString();
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
