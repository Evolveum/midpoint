/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.annotation.Experimental;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Maintains information on processing of a given item: a resource object (for tasks like import or reconciliation),
 * a repository object (e.g. for recomputation tasks), a change (for livesync or async update), or basically whatever.
 */
@Experimental
public class ItemProcessing implements Serializable {

    /**
     * When the processing started (a result of System.nanoTime()).
     * This is used to determine the exact duration of individual parts of the processing.
     */
    private long startTimeNanos;

    /**
     * When the processing started (a result of System.currentTimeMillis()).
     * This is used to know the real calendar start time.
     */
    private long startTimeMillis;

    private final List<Milestone> milestones = new ArrayList<>();

    /**
     * Operation result related to the processing of the item.
     */
    private OperationResult result;

    /**
     * Milestone reached during processing of an item.
     */
    private static class Milestone implements Serializable {

        /** What milestone is this. */
        private final String label;

        /** Time (in nanos). Useful to determine exact durations. */
        private final long reachedTimeNanos;

        /** Time (in millis). Useful to know the real time. */
        private final long reachedTimeMillis;

        private Milestone(String label, long reachedTimeNanos, long reachedTimeMillis) {
            this.label = label;
            this.reachedTimeNanos = reachedTimeNanos;
            this.reachedTimeMillis = reachedTimeMillis;
        }

        public String getLabel() {
            return label;
        }

        public long getReachedTimeNanos() {
            return reachedTimeNanos;
        }

        public long getReachedTimeMillis() {
            return reachedTimeMillis;
        }
    }

    public long getStartTimeNanos() {
        return startTimeNanos;
    }

    public long getStartTimeMillis() {
        return startTimeMillis;
    }

    public List<Milestone> getMilestones() {
        return milestones;
    }

    public OperationResult getResult() {
        return result;
    }
}
