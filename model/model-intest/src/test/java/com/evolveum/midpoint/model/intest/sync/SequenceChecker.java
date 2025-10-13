/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.intest.sync;

/**
 * Checks that related changes arrive in the correct order and in the single thread.
 */
public class SequenceChecker {

    public static final SequenceChecker INSTANCE = new SequenceChecker();

    private Long threadId;
    private Integer lastChangeSeen;

    public void reset() {
        threadId = null;
        lastChangeSeen = null;
    }

    public void checkChange(int currentChange) {
        long currentThreadId = Thread.currentThread().getId();
        if (threadId != null) {
            if (currentThreadId != threadId) {
                throw new IllegalStateException("Current thread ID " + currentThreadId + " is different from the ID "
                        + "of thread that processed previous requests: " + threadId);
            } else {
                System.out.println("Thread ID OK: " + threadId);
            }
        } else {
            threadId = currentThreadId;
        }

        if (lastChangeSeen != null) {
            if (currentChange < lastChangeSeen) {
                throw new IllegalStateException("Current change # (" + currentChange + ") is under last processed change # (" + lastChangeSeen + ")");
            } else {
                System.out.println("Change # OK: " + lastChangeSeen + "->" + currentChange);
            }
        }
        lastChangeSeen = currentChange;
    }
}
