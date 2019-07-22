/*
 * Copyright (c) 2010-2019 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.perf;

import com.evolveum.midpoint.repo.api.perf.OperationRecord;
import com.evolveum.midpoint.repo.sql.SqlBaseService;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 *
 */
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
	    final int MAX_ATTEMPTS = SqlBaseService.LOCKING_MAX_RETRIES + 1;
	    StatEntry[] perAttempts = new StatEntry[MAX_ATTEMPTS];

	    for (int i = 0; i < MAX_ATTEMPTS; i++) {
	        perAttempts[i] = new StatEntry();
	    }

	    for (OperationRecord operation : finishedOperations) {
	        all.process(operation);
	        if (operation.getAttempts() >= 1 && operation.getAttempts() <= MAX_ATTEMPTS) {
	            perAttempts[operation.getAttempts() -1].process(operation);
	        } else if (operation.getAttempts() < 0) {
	            unfinished.process(operation);
	        }
	    }

	    StringBuilder retval = new StringBuilder();
	    retval.append("Overall: ").append(all.dump()).append("\n");
	    for (int i = 0; i < MAX_ATTEMPTS; i++) {
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
	               "Total time (avg/sum): " + ((float) totalTime/records) + "/" + totalTime + ", " +
	               "Wasted time (avg/sum): " + ((float) wastedTime/records) + "/" + wastedTime + " (" + (wastedTime*100.0f/totalTime) + "%), " +
	               "Attempts (avg): " + ((float) attempts/records);
	    }
	}
}
