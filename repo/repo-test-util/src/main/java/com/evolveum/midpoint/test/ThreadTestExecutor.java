/*
 * Copyright (C) 2019-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.evolveum.midpoint.util.CheckedRunnable;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * EXPERIMENTAL
 */
public class ThreadTestExecutor {

    private static final Trace LOGGER = TraceManager.getTrace(ThreadTestExecutor.class);

    private static final long WAIT_STEP = 100;

    private final int threadsCount;
    private final long timeout;

    private List<Thread> threads;
    private List<Thread> failedThreads;
    private boolean lastExecutionFinished;

    public ThreadTestExecutor(int threadsCount, long timeout) {
        this.threadsCount = threadsCount;
        this.timeout = timeout;
    }

    public void execute(CheckedRunnable runnable) throws InterruptedException {
        threads = new ArrayList<>();
        failedThreads = new ArrayList<>();

        for (int i = 0; i < threadsCount; i++) {
            threads.add(new Thread(() -> {
                try {
                    runnable.run();
                } catch (Throwable t) {
                    failedThreads.add(Thread.currentThread());
                    LOGGER.error("Thread got an exception: " + t.getMessage(), t);
                    throw new AssertionError("Thread got an exception: " + t.getMessage(), t);
                }
            }));
        }
        for (int i = 0; i < threadsCount; i++) {
            threads.get(i).start();
        }

        long start = System.currentTimeMillis();
        lastExecutionFinished = false;
        while (System.currentTimeMillis() - start < timeout) {
            Optional<Thread> firstAlive = threads.stream().filter(Thread::isAlive).findFirst();
            if (firstAlive.isPresent()) {
                firstAlive.get().join(WAIT_STEP);
            } else {
                lastExecutionFinished = true;
                break;
            }
        }

        if (!lastExecutionFinished) {
            throw new AssertionError("Some of the threads have not finished in time: " +
                    threads.stream().filter(Thread::isAlive).collect(Collectors.toList()));
        }
    }

    public List<Thread> getThreads() {
        return threads;
    }

    public List<Thread> getFailedThreads() {
        return failedThreads;
    }

    public boolean isLastExecutionFinished() {
        return lastExecutionFinished;
    }
}
