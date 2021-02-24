/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

import com.evolveum.midpoint.util.annotation.Experimental;

/**
 * Represents data about iterative operation that starts.
 *
 * TODO better name
 */
@Experimental
public class IterativeOperation {

    private final IterationItemInformation item;
    private final long startTimestamp;
    private final Integer taskPartNumber;

    public IterativeOperation(IterationItemInformation item) {
        this(item, null);
    }

    public IterativeOperation(IterationItemInformation item, Integer taskPartNumber) {
        this(item, System.currentTimeMillis(), taskPartNumber);
    }

    public IterativeOperation(IterationItemInformation item, long startTimestamp, Integer taskPartNumber) {
        this.item = item;
        this.startTimestamp = startTimestamp;
        this.taskPartNumber = taskPartNumber;
    }

    public IterationItemInformation getItem() {
        return item;
    }

    public long getStartTimestamp() {
        return startTimestamp;
    }

    public Integer getTaskPartNumber() {
        return taskPartNumber;
    }

    @Override
    public String toString() {
        return "IterativeOperation{" +
                "item=" + item +
                ", startTimestamp=" + startTimestamp +
                ", taskPartNumber=" + taskPartNumber +
                '}';
    }
}
