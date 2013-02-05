package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created with IntelliJ IDEA.
 * User: Pavol
 * Date: 31.1.2013
 * Time: 18:43
 * To change this template use File | Settings | File Templates.
 */
public class SqlPerformanceMonitor {

    private static final Trace LOGGER = TraceManager.getTrace(SqlPerformanceMonitor.class);

    private AtomicLong currentHandle = new AtomicLong();

    private ConcurrentMap<Long,OperationRecord> outstandingOperations = new ConcurrentHashMap<Long, OperationRecord>();
    private List<OperationRecord> finishedOperations = Collections.synchronizedList(new ArrayList<OperationRecord>());

    class OperationRecord {
        String kind;
        long handle;
        int attempts;
        long startTime;
        long startCpuTime;
        long totalTime;
        long totalCpuTime;
        long wastedTime;
        long wastedCpuTime;

        public OperationRecord(String kind, long handle) {
            this.kind = kind;
            this.handle = handle;
            this.startTime = System.currentTimeMillis();
        }

        @Override
        public String toString() {
            return "OperationRecord{" +
                    "kind='" + kind + '\'' +
                    ", handle=" + handle +
                    ", attempts=" + attempts +
                    ", startTime=" + new Date(startTime) +
                    ", totalTime=" + totalTime +
//                    ", totalCpuTime=" + totalCpuTime +
                    ", wastedTime=" + wastedTime +
//                    ", wastedCpuTime=" + wastedCpuTime +
                    '}';
        }
    }

    public void initialize() {
        outstandingOperations.clear();
        finishedOperations.clear();
        LOGGER.info("SQL Performance Monitor initialized.");
    }

    public void shutdown() {
        LOGGER.info("SQL Performance Monitor shutting down.");
        LOGGER.info("Statistics:\n" + getFormattedStatistics());
    }

    class StatEntry {
        long totalTime, wastedTime;
        int attempts;
        int records;

        public void process(OperationRecord operation) {
            totalTime += operation.totalTime;
            wastedTime += operation.wastedTime;
            attempts += operation.attempts;
            records++;
        }

        public String dump() {
            if (records == 0) {
                return "no records";
            }
            return "Records: " + records + ", " +
                   "Total time (avg/sum): " + ((float) totalTime/records) + "/" + totalTime + ", " +
                   "Wasted time (avg/sum): " + ((float) wastedTime/records) + "/" + wastedTime + ", " +
                   "Attempts (avg): " + ((float) attempts/records);
        }
    }

    private String getFormattedStatistics() {
        StatEntry all = new StatEntry();
        StatEntry unfinished = new StatEntry();
        StatEntry[] perAttempts = new StatEntry[SqlBaseService.LOCKING_MAX_ATTEMPTS+1];         // "+1" is a safety margin

        for (int i = 0; i < SqlBaseService.LOCKING_MAX_ATTEMPTS+1; i++) {
            perAttempts[i] = new StatEntry();
        }

        synchronized (finishedOperations) {
            for (OperationRecord operation : finishedOperations) {
                all.process(operation);
                if (operation.attempts >= 1 && operation.attempts <= SqlBaseService.LOCKING_MAX_ATTEMPTS+1) {
                    perAttempts[operation.attempts-1].process(operation);
                } else if (operation.attempts < 0) {
                    unfinished.process(operation);
                }
            }
        }

        StringBuilder retval = new StringBuilder();
        retval.append("Overall: " + all.dump() + "\n");
        for (int i = 0; i < SqlBaseService.LOCKING_MAX_ATTEMPTS+1; i++) {
            retval.append((i+1) + " attempt(s): " + perAttempts[i].dump() + "\n");
        }
        retval.append("Unfinished: " + unfinished.dump() + "\n");
        retval.append("Outstanding: " + outstandingOperations.toString());
        return retval.toString();
    }

    private String dump() {
        return "Finished operations: " + finishedOperations + "\nOutstanding operations: " + outstandingOperations;
    }


    public long registerOperationStart(String kind) {
        long handle = currentHandle.getAndIncrement();
        Long threadId = Thread.currentThread().getId();
        if (outstandingOperations.containsKey(threadId)) {
            OperationRecord unfinishedOperation = outstandingOperations.get(threadId);
            LOGGER.warn("Unfinished operation: " + unfinishedOperation);
            registerOperationFinishRaw(threadId, unfinishedOperation, -1);
        }
        outstandingOperations.put(threadId, new OperationRecord(kind, handle));
        return handle;
    }

    public void registerOperationFinish(long opHandle, int attempt) {
        Long threadId = Thread.currentThread().getId();
        OperationRecord operation = outstandingOperations.get(threadId);

        if (operation == null) {
            LOGGER.warn("Attempted to record finish event for unregistered operation: handle = " + opHandle + ", attempt = " + attempt + ", ignoring the request.");
            return;
        }
        if (operation.handle != opHandle) {
            LOGGER.error("Attempted to record finish event with unexpected operation handle: handle = " + opHandle + ", stored outstanding operation for this thread = " + operation);
            outstandingOperations.remove(threadId);
            return;
        }
        registerOperationFinishRaw(threadId, operation, attempt);
    }

    private void registerOperationFinishRaw(Long threadId, OperationRecord operation, int attempt) {
        operation.totalTime = System.currentTimeMillis() - operation.startTime;
        operation.attempts = attempt;
        finishedOperations.add(operation);
        outstandingOperations.remove(threadId);
    }

    public void registerOperationNewTrial(long opHandle, int attempt) {
        Long threadId = Thread.currentThread().getId();
        OperationRecord operation = outstandingOperations.get(threadId);

        if (operation == null) {
            LOGGER.warn("Attempted to record new trial event for unregistered operation: handle = " + opHandle + ", attempt = " + attempt + ", ignoring the request.");
            return;
        }
        if (operation.handle != opHandle) {
            LOGGER.error("Attempted to record new trial event with unexpected operation handle: handle = " + opHandle + ", stored outstanding operation for this thread = " + operation);
            outstandingOperations.remove(threadId);
            return;
        }
        operation.wastedTime = System.currentTimeMillis() - operation.startTime;
        operation.attempts = attempt;
    }

}
