/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story.sysperf;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.IntStream;

import static com.evolveum.midpoint.testing.story.sysperf.SourceInitializer.ACCOUNT_NAME_DIGITS;

record TaskDistribution(int threads, int workerTasks, int requiredBuckets, int levels) {

    private static final int DEFAULT_BUCKETS_PER_TASK = 2;

    private static final String THREADS_SUFFIX = ".threads";
    private static final String WORKER_TASKS_SUFFIX = ".worker-tasks";
    private static final String BUCKETS = ".buckets";

    static final int BUCKET_FACTOR_FOR_ACCOUNTS = 10;
    static final int BUCKET_FACTOR_FOR_OIDS = 16;

    static @NotNull TaskDistribution fromSystemProperties(String propPrefix, int bucketFactor) {
        int workerThreads = Math.max(0, Integer.parseInt(System.getProperty(propPrefix + THREADS_SUFFIX, "0")));
        int workerTasks = Math.max(Integer.parseInt(System.getProperty(propPrefix + WORKER_TASKS_SUFFIX, "0")), 0);
        int requiredBuckets = Integer.parseInt(
                System.getProperty(propPrefix + BUCKETS, determineDefaultBuckets(workerTasks, bucketFactor)));
        int levels = determineLevels(requiredBuckets, bucketFactor);
        return new TaskDistribution(workerThreads, workerTasks, requiredBuckets, levels);
    }

    private static String determineDefaultBuckets(int workerTasks, int bucketFactor) {
        if (workerTasks == 0) {
            return "0";
        }
        int minBuckets = workerTasks * DEFAULT_BUCKETS_PER_TASK;
        int buckets = 1;
        while (buckets < minBuckets) {
            buckets *= bucketFactor;
        }
        return String.valueOf(buckets);
    }

    private static int determineLevels(int requiredBuckets, int bucketFactor) {
        int level = 0, realBuckets = 1;
        while (realBuckets < requiredBuckets) {
            realBuckets *= bucketFactor;
            level++;
        }
        return level;
    }

    boolean isBucketing() {
        return levels > 0;
    }

    List<Integer> getFixedCharactersPositions() {
        var positions = CharacterPositions.determine(levels);
        return IntStream.range(3, 3 + positions.fixedZeros())
                .boxed()
                .toList();
    }

    List<Integer> getVaryingCharactersPositions() {
        var positions = CharacterPositions.determine(levels);
        return IntStream.range(3 + positions.fixedZeros(), 3 + positions.fixedZeros() + positions.varyingDigits())
                .boxed()
                .toList();
    }

    /**
     * An account name is in the form of u-0000NNNN, where NNNN are significant digits (depending on the number of accounts,
     * e.g. four of them for 1001 to 10000 accounts).
     *
     * We require {@link #levels} bucketing levels. Normally, the number of levels should be less than or equal to the number
     * of significant digits (if it's equal, the buckets will contain only a single account). However, it's possible to have
     * more levels than significant digits. In that case, some buckets will be empty.
     *
     * Let's assume 4 significant numbers (1001 to 10000 accounts) and 3 levels. Then:
     *
     * - first `fixedZeros` digits are fixed (0000); here `fixedZeros` = 4;
     * - the next `levels` digits are varying (NNN); here `varyingDigits` = 3;
     *
     * These positions start at 0, and are related to the first digit of the account number.
     */
    record CharacterPositions(int fixedZeros, int varyingDigits) {

        static CharacterPositions determine(int levels) {
            if (levels > ACCOUNT_NAME_DIGITS) {
                throw new IllegalStateException("Too many levels: " + levels);
            }
            int significantDigits = getSignificantDigits();
            if (levels <= significantDigits) {
                return new CharacterPositions(ACCOUNT_NAME_DIGITS - significantDigits, levels);
            } else {
                return new CharacterPositions(ACCOUNT_NAME_DIGITS - levels, levels);
            }
        }

        private static int getSignificantDigits() {
            var accounts = TestSystemPerformance.SOURCES_CONFIGURATION.getNumberOfAccounts();
            return (int) Math.ceil(Math.log10(accounts));
        }
    }

    @Override
    public String toString() {
        return "{threads=%d, workerTasks=%d, requiredBuckets=%d, levels=%d}".formatted(
                threads, workerTasks, requiredBuckets, levels);
    }
}
