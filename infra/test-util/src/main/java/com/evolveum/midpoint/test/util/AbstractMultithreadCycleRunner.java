/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test.util;

/**
 * Code to be run in multiple threads and multiple cycles until given time.
 * To be used in {@link ParallelTestThread}.
 *
 * @see MultithreadRunner
 * @see ParallelTestThread
 * @see TestUtil#multithread(MultithreadRunner, int, Integer)
 */
public abstract class AbstractMultithreadCycleRunner implements MultithreadRunner {

    private final long stopTimeMillis;

    /**
     * @param runTimeMillis For how long should this runner run. (Starting from now.)
     */
    public AbstractMultithreadCycleRunner(long runTimeMillis) {
        this.stopTimeMillis = System.currentTimeMillis() + runTimeMillis;
    }

    /**
     * Code that should be executed on start. Typical usage: login.
     */
    public void init(int threadIndex) throws Exception {
        // default: do nothing
    }

    /**
     * Code that should be executed periodically.
     */
    public abstract void run(int threadIndex, int cycleNumber) throws Exception;

    @Override
    public void run(int threadIndex) throws Exception {
        init(threadIndex);
        int cycleNumber = 0;
        while (System.currentTimeMillis() < stopTimeMillis) {
            run(threadIndex, cycleNumber++);
        }
        System.out.printf("Thread %d finished after %d cycles%n", threadIndex, cycleNumber);
    }
}
