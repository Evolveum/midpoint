/*
 * Copyright (c) 2016 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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

    private int i;
    private MultithreadRunner target;
    private Throwable exception;

    public ParallelTestThread(int i, MultithreadRunner target) {
        super();
        this.i = i;
        this.target = target;
    }

    @Override
    public void run() {
        try {
            target.run(i);
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
