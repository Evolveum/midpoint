/*
 * Copyright (c) 2016 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.test.util;

import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author semancik
 */
public class ParallelTestThread extends Thread {

    private static final Trace LOGGER = TraceManager.getTrace(ParallelTestThread.class);

    private final int threadIndex;
    private final MultithreadRunner runner;
    private Throwable exception;

    ParallelTestThread(int threadIndex, MultithreadRunner runner) {
        super();
        this.threadIndex = threadIndex;
        this.runner = runner;
    }

    @Override
    public void run() {
        try {
            runner.run(threadIndex);
        } catch (RuntimeException | Error e) {
            recordException(e);
            throw e;
        } catch (Throwable e) {
            recordException(e);
            throw new SystemException(e.getMessage(), e);
        }
    }

    public Throwable getException() {
        return exception;
    }

    public void recordException(Throwable e) {
        LOGGER.error("Test thread failed: {}", e.getMessage(), e);
        this.exception = e;
    }

}
