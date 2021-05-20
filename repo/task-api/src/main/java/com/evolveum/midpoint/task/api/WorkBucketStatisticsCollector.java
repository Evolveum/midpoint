/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.api;

/**
 *
 */
public interface WorkBucketStatisticsCollector {

    void register(String situation, long totalTime, int conflictCount, long conflictWastedTime, int bucketWaitCount, long bucketWaitTime, int bucketsReclaimed);
}
