/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.ninja.util;

import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.impl.NinjaException;
import com.evolveum.midpoint.schema.result.OperationResult;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Viliam Repan (lazyman).
 */
public class OperationStatus {

    public enum State {
        NOT_STARTED, STARTED, PRODUCER_FINISHED, FINISHED;
    }

    private NinjaContext context;

    private State state = State.NOT_STARTED;

    private AtomicInteger totalCount = new AtomicInteger(0);
    private AtomicInteger errorCount = new AtomicInteger(0);
    private AtomicInteger skippedCount = new AtomicInteger(0);

    private long startTime;
    private long finishTime;

    private long lastPrintoutTime;
    private int lastPrintoutCount;

    private OperationResult result;

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
        return ((double) (finishTime - startTime)) / 1000;
    }

    public double getAvgRequestPerSecond() {
        if (totalCount.get() == 0) {
            return 0d;
        }

        long span = (System.currentTimeMillis() - startTime) / 1000;

        return ((double) totalCount.get()) / span;
    }

    public String print() {
        StringBuilder sb = new StringBuilder();
        sb.append("Processed: ");
        sb.append(totalCount.get());
        sb.append(", error: ");
        sb.append(errorCount.get());
        sb.append(", skipped: ");
        sb.append(skippedCount.get());
        sb.append(", avg: ");
        sb.append(NinjaUtils.DECIMAL_FORMAT.format(getAvgRequestPerSecond()));
        sb.append("obj/s");

        return sb.toString();
    }

    public void print(Log log) {
        log.info(print());

        lastPrintoutNow();
    }

    public void lastPrintoutNow() {
        this.lastPrintoutTime = System.currentTimeMillis();
        this.lastPrintoutCount = totalCount.get() - lastPrintoutCount;
    }

    private void debug(String message) {
        Log log = context.getLog();
        log.debug(message);
    }
}
