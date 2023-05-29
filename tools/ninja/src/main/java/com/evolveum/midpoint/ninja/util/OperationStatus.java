/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.util;

import java.util.concurrent.atomic.AtomicInteger;

import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.impl.NinjaException;
import com.evolveum.midpoint.schema.result.OperationResult;

/**
 * Created by Viliam Repan (lazyman).
 */
public class OperationStatus {

    public enum State {
        NOT_STARTED, STARTED, PRODUCER_FINISHED, FINISHED
    }

    private final NinjaContext context;

    private State state = State.NOT_STARTED;

    private final AtomicInteger totalCount = new AtomicInteger(0);
    private final AtomicInteger errorCount = new AtomicInteger(0);
    private final AtomicInteger skippedCount = new AtomicInteger(0);

    private long startTime;
    private long finishTime;

    private int lastPrintoutCount;

    private final OperationResult result;

    public OperationStatus(NinjaContext context, OperationResult result) {
        this.context = context;
        this.result = result;
    }

    public OperationResult getResult() {
        return result;
    }

    public State getState() {
        return state;
    }

    public void start() {
        if (state != State.NOT_STARTED) {
            throw new NinjaException("Can't start operation, previous state is was not " + State.NOT_STARTED);
        }

        debug("Operation: started");

        startTime = System.currentTimeMillis();

        state = State.STARTED;
    }

    public void finish() {
        if (state == State.NOT_STARTED) {
            throw new NinjaException("Can't finish operation, previous state was " + State.NOT_STARTED);
        }

        debug("Operation: finished");

        finishTime = System.currentTimeMillis();

        state = State.FINISHED;
    }

    public void producerFinish() {
        if (state != State.STARTED) {
            throw new NinjaException("Can't set state " + State.PRODUCER_FINISHED + " for operation, previous state is was not " + State.STARTED);
        }

        debug("Operation: producer finished");

        state = State.PRODUCER_FINISHED;
    }

    public boolean isStarted() {
        return State.STARTED == state;
    }

    public boolean isFinished() {
        return State.FINISHED == state;
    }

    public boolean isProducerFinished() {
        return State.PRODUCER_FINISHED == state;
    }

    public int getTotalCount() {
        return totalCount.get();
    }

    public int getErrorCount() {
        return errorCount.get();
    }

    public int getSkippedCount() {
        return skippedCount.get();
    }

    public void incrementTotal() {
        totalCount.incrementAndGet();
    }

    public void incrementError() {
        errorCount.incrementAndGet();
    }

    public void incrementSkipped() {
        skippedCount.incrementAndGet();
    }

    public double getTotalTime() {
        return (finishTime - startTime) / 1000d;
    }

    public double getAvgRequestPerSecond() {
        if (totalCount.get() == 0) {
            return 0d;
        }

        double span = state == State.FINISHED
                ? (finishTime - startTime) / 1000d
                : (System.currentTimeMillis() - startTime) / 1000d;

        return totalCount.get() / span;
    }

    public String print() {
        return "Processed: "
                + totalCount.get()
                + ", error: "
                + errorCount.get()
                + ", skipped: "
                + skippedCount.get()
                + ", avg processed: "
                + NinjaUtils.DECIMAL_FORMAT.format(getAvgRequestPerSecond())
                + "obj/s";
    }

    public void print(Log log) {
        log.info(print());

        lastPrintoutNow();
    }

    public void lastPrintoutNow() {
        this.lastPrintoutCount = totalCount.get() - lastPrintoutCount;
    }

    private void debug(String message) {
        Log log = context.getLog();
        log.debug(message);
    }
}
